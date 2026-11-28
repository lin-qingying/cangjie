package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider

import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.mpp.*

/**
 * CLI 与 LL 共用的规范组聚合。session 的 lazy resolver 决定如何准备原声明；派生 cell 只执行纯程序。
 * BODY 默认值可请求实际候选所在的组，不请求 specific BODY，也不借 checker 完成整个包。
 */
class CfirCjmpBatchFamilyAggregate(
    private val session: CfirSession,
    private val declarationFactsPreparer: CfirCjmpDeclarationFactsPreparer,
) : CfirCjmpFamilyAggregate {
    private val structures = CfirCjmpSingleFlightCache<List<CfirBasedSymbol<*>>, CfirCjmpStructureSnapshot>()
    private val groups = CfirCjmpSingleFlightCache<CfirCjmpGroupCacheKey, CfirCjmpFamilySnapshot>()
    private val families = CfirCjmpSingleFlightCache<FqName, CfirCjmpFamilySnapshot>()

    override fun structureFor(declaration: CfirDeclaration): CfirCjmpStructureSnapshot {
        val original = (declaration as? CfirCallableDeclaration)?.unwrapSubstitutionOverrides() ?: declaration
        val packageName = original.cjmpPackageFqName()
        val topLevel = (original as? CfirCallableDeclaration)?.getContainingCjmpDeclaration() ?: original
        val group = discoverGroup(topLevel, packageName)
        val key = group.map { it.symbol }
        return structures.getOrCompute(key) {
            prepareGroup(packageName, group, null, null, includeMatchFacts = false).evaluateStructure()
        }
    }

    override fun matchingGroupFor(
        declaration: CfirDeclaration,
        declarationFacts: CfirCjmpDeclarationMatchFacts?,
        matchingMode: CfirCjmpMatchingMode,
    ): CfirCjmpFamilySnapshot {
        val original = (declaration as? CfirCallableDeclaration)?.unwrapSubstitutionOverrides() ?: declaration
        require(declarationFacts == null || declarationFacts.rootSymbol === original.symbol) {
            "Prepared CJMP facts must belong to the declaration's phase root"
        }
        val packageName = original.cjmpPackageFqName()
        val topLevel = (original as? CfirCallableDeclaration)?.getContainingCjmpDeclaration() ?: original
        val group = discoverGroup(topLevel, packageName)
        val key = CfirCjmpGroupCacheKey(group.map { it.symbol }, matchingMode)
        return groups.getOrCompute(key) {
            prepareGroup(
                packageName,
                group,
                declarationFacts?.rootSymbol,
                declarationFacts,
                matchingMode = matchingMode,
            ).evaluate()
        }
    }

    override fun defaultParameterStatesForBootstrap(declaration: CfirDeclaration): List<CjmpParameterDefaultState<CfirBasedSymbol<*>>> {
        val original = (declaration as? CfirCallableDeclaration)?.unwrapSubstitutionOverrides() ?: declaration
        val packageName = original.cjmpPackageFqName()
        val topLevel = (original as? CfirCallableDeclaration)?.getContainingCjmpDeclaration() ?: original
        val provisional = prepareGroup(
            packageName = packageName,
            topLevels = discoverGroup(topLevel, packageName),
            preparedRootSymbol = null,
            preparedRootFacts = null,
            includeMatchFacts = true,
            matchingMode = CfirCjmpMatchingMode.DEFAULT_PARAMETER_BOOTSTRAP,
        ).evaluate()
        return checkNotNull(provisional.defaults[original.symbol]) {
            "A provisional CJMP group must publish defaults for its requested declaration"
        }
    }

    override fun ensureFamilyComplete(packageFqName: FqName): CfirCjmpFamilySnapshot {
        return families.getOrCompute(packageFqName) {
            val snapshots = ArrayList<CfirCjmpFamilySnapshot>()
            val packageDeclarations = packageTopLevelDeclarations(packageFqName)
            for (declaration in packageDeclarations) {
                if (declaration !is CfirMemberDeclaration) continue
                if (!declaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.COMMON) &&
                    !declaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC) &&
                    declaration !is CfirClassLikeDeclaration && declaration !is CfirExtend
                ) continue
                val snapshot = matchingGroupFor(declaration)
                if (snapshots.none { it === snapshot }) snapshots += snapshot
            }
            CfirCjmpFamilySnapshot.combine(packageFqName, snapshots, packageDeclarations)
        }
    }

    private fun discoverGroup(target: CfirDeclaration, packageName: FqName): List<CfirDeclaration> {
        val refinementModules = session.moduleData.allRefinementDependencies.toSet()
        val declarations = when (target) {
            is CfirExtend -> {
                target.lazyResolveToPhase(CfirResolvePhase.TYPES)
                val matchingExtends = session.extendProvider.getCjmpMatchingExtends(target, packageName).filter { candidate ->
                    candidate.moduleData == session.moduleData || candidate.moduleData in refinementModules
                }
                // extend 候选顺序决定重复 specific 的归属；目标只补入，不能移到规范顺序之前。
                matchingExtends + target
            }
            else -> packageTopLevelDeclarations(packageName, includeExtends = false) + target
        }
        val commonCandidates = when (target) {
            is CfirClassLikeDeclaration -> session.dependenciesSymbolProvider
                .getClassLikeSymbolsByClassId(target.symbol.classId).map { it.cfir }
            is CfirPatternVariable -> declarationNames(target).flatMap { name ->
                session.dependenciesSymbolProvider.getTopLevelCallableSymbols(packageName, name).map { it.cfir }
            }
            is CfirCallableDeclaration -> session.dependenciesSymbolProvider
                .getTopLevelCallableSymbols(packageName, target.symbol.name).map { it.cfir }
            else -> emptyList()
        }.filter { candidate ->
            candidate.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.COMMON) && candidate.moduleData in refinementModules
        }
        val topLevel = (declarations + commonCandidates).distinctBy { it.symbol }
        return when (target) {
            is CfirClassLikeDeclaration -> topLevel.filterIsInstance<CfirClassLikeDeclaration>()
                .filter { it.name == target.name }
            is CfirExtend -> topLevel.filterIsInstance<CfirExtend>()
            is CfirCallableDeclaration -> {
                // tuple 的多个真实绑定名可把同名组连接起来；固定点保证从任意 root 请求都得到同一键。
                val names = declarationNames(target).toMutableSet()
                val selected = LinkedHashSet<CfirDeclaration>()
                var changed: Boolean
                do {
                    changed = false
                    for (candidate in topLevel.filterIsInstance<CfirCallableDeclaration>()) {
                        val candidateNames = declarationNames(candidate)
                        if (candidateNames.any { it in names } && selected.add(candidate)) {
                            names.addAll(candidateNames)
                            changed = true
                        }
                    }
                } while (changed)
                topLevel.filter { it in selected }
            }
            else -> listOf(target)
        }.also { group -> check(group.any { it === target }) { "A CJMP group must contain its original declaration" } }
    }

    private fun declarationNames(declaration: CfirCallableDeclaration): Set<Name> = when (declaration) {
        is CfirPatternVariable -> declaration.pattern.bindingVariables().mapTo(linkedSetOf()) { it.name }
        else -> setOf(declaration.symbol.name)
    }

    /** 这里只枚举原始结构；没有推进无关 callable 的签名或函数体。 */
    private fun packageTopLevelDeclarations(
        packageName: FqName,
        includeExtends: Boolean = true,
    ): List<CfirDeclaration> {
        val refinementModules = session.moduleData.allRefinementDependencies.toSet()
        val declarations = buildList {
            session.cjmpFilesInPackage(packageName).forEach { addAll(it.declarations) }
            val provider = session.dependenciesSymbolProvider
            val names = provider.symbolNamesProvider
            for (name in names.getTopLevelClassifierNamesInPackage(packageName).orEmpty()) {
                addAll(provider.getClassLikeSymbolsByClassId(ClassId(packageName, name)).map { it.cfir })
            }
            for (name in names.getTopLevelCallableNamesInPackage(packageName).orEmpty()) {
                addAll(provider.getTopLevelCallableSymbols(packageName, name).map { it.cfir })
            }
            if (includeExtends) addAll(session.extendProvider.getExtendsInPackage(packageName))
        }.distinctBy { it.symbol }.filter {
            it.moduleData == session.moduleData || it.moduleData in refinementModules
        }
        return declarations.sortedWith(compareBy(
            { it.cjmpSourcePosition()?.fileName },
            { it.cjmpSourcePosition()?.line },
            { it.cjmpSourcePosition()?.column },
        ))
    }

    private fun prepareGroup(
        packageName: FqName,
        topLevels: List<CfirDeclaration>,
        preparedRootSymbol: CfirBasedSymbol<*>?,
        preparedRootFacts: CfirCjmpDeclarationMatchFacts?,
        includeMatchFacts: Boolean = true,
        matchingMode: CfirCjmpMatchingMode = CfirCjmpMatchingMode.FINAL,
    ): CfirCjmpPreparedGroup {
        val declarations = ArrayList<CfirDeclaration>()
        val children = linkedMapOf<CfirBasedSymbol<*>, List<CfirBasedSymbol<*>>>()
        fun collect(declaration: CfirDeclaration) {
            declarations += declaration
            val nested: List<CfirDeclaration> = when (declaration) {
                is CfirClassLikeDeclaration -> declaration.declarations
                is CfirExtend -> declaration.declarations
                is CfirProperty -> listOfNotNull(declaration.getter, declaration.setter)
                is CfirPatternVariable -> declaration.pattern.bindingVariables()
                else -> emptyList()
            }
            children[declaration.symbol] = nested.map { it.symbol }
            nested.forEach(::collect)
        }
        topLevels.forEach(::collect)
        val roots = if (!includeMatchFacts) emptyList() else declarations.filterIsInstance<CfirMemberDeclaration>().filter { declaration ->
            declaration.moduleData == session.moduleData &&
                    declaration !is CfirPropertyAccessor && declaration !is CfirPatternBindingVariable &&
                    (declaration is CfirClassLikeDeclaration || declaration is CfirExtend ||
                            declaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC))
        }
        val factsByRoot = LinkedHashMap<CfirBasedSymbol<*>, CfirCjmpDeclarationMatchFacts>()
        for (root in roots) {
            val facts = when {
                root.symbol === preparedRootSymbol -> checkNotNull(preparedRootFacts) {
                    "A prepared root symbol requires its immutable CJMP facts"
                }
                matchingMode == CfirCjmpMatchingMode.FINAL && root.cjmpMatchFacts != null -> root.cjmpMatchFacts!!
                else -> declarationFactsPreparer.prepare(
                    root,
                    (root as? CfirCallableDeclaration)?.getContainingCjmpDeclaration()
                        ?.let { factsByRoot[it.symbol] ?: it.cjmpMatchFacts },
                    matchingMode,
                )
            }
            factsByRoot[root.symbol] = facts
        }
        val facts = roots.map { root -> factsByRoot.getValue(root.symbol) }
        val containers = declarations.mapNotNull { declaration ->
            if (declaration is CfirClassLikeDeclaration || declaration is CfirExtend) {
                if (declaration.moduleData == session.moduleData) {
                    declaration.lazyResolveToPhase(CfirResolvePhase.TYPES)
                }
                declaration.cjmpMatchFacts?.container ?: declaration.cjmpOriginalContainerInput()
            } else null
        }
        val positions = containers.flatMap { it.members }.mapNotNull { it.sourcePosition }
            .distinct().sortedWith(compareBy({ it.line }, { it.column }))
        val positionRanks = positions.withIndex().associate { it.value to it.index }
        fun sharedInput(container: CfirCjmpContainerInput) = CjmpMergeContainer(
            container.symbol, container.name, container.kind, container.isCommon, container.isSpecific,
            container.hasGenericNode, container.typeParameters, container.isNonExhaustive, container.originalDoNotExport,
            container.members.map { member ->
                CjmpMergeMember(
                    member.symbol, member.name, member.isInstanceVariable, member.isMemberParameter,
                    member.isCommon, member.isSpecific, member.hasCommonDefault, member.originalDoNotExport,
                    // 初始化依赖的发布晚于成员图准备；family 快照不能固化独立 lifecycle 的 live 属性。
                    CjmpInitializationDependencies.NotCollected, member.isFromCommonPart,
                    member.sourcePosition?.let(positionRanks::getValue) ?: positions.size,
                )
            },
        )
        // 同 key 的 extend 在 discovery 中已完成类型等价比较；纯 evaluator 使用显式稳定键。
        val extensions = containers.filter { it.kind == CjmpContainerKind.EXTEND }.map { container ->
            CjmpExtendMergeInput(sharedInput(container), container.extendKey?.let { 0 })
        }
        return CfirCjmpPreparedGroup(
            packageName, facts, topLevels.map { it.symbol }, children,
            declarations.filter { it.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.COMMON) }.map { it.symbol },
            containers.filter { it.kind != CjmpContainerKind.EXTEND }.map(::sharedInput), extensions,
            extensions.filter { it.key != null && it.container.isCommon }.map { it.container.symbol },
            declarations.filter { it.originalDoNotExport }.mapTo(linkedSetOf()) { it.symbol },
            classifierDomain = CfirCjmpClassifierDomain.capture(session, declarations,
                declarations.filterIsInstance<CfirClassLikeDeclaration>().mapNotNull {
                    it.moduleData.session.cfirProvider.getContainingFile(it.symbol)
                }),
        )
    }
}

private data class CfirCjmpGroupCacheKey(
    val declarations: List<CfirBasedSymbol<*>>,
    val matchingMode: CfirCjmpMatchingMode,
)

/** package 身份来自声明或其 provider，不由显示名称推断。 */
fun CfirDeclaration.cjmpPackageFqName(): FqName = when (this) {
    is CfirCallableDeclaration -> symbol.callableId.packageName
    is CfirClassLikeDeclaration -> symbol.classId.packageFqName
    is CfirExtend -> checkNotNull(moduleData.session.extendProvider.getPackageFqName(this))
    else -> checkNotNull(moduleData.session.cfirProvider.getContainingFile(symbol)).packageDirective.packageFqName
}

fun CfirSession.cjmpFilesInPackage(packageFqName: FqName): List<CfirFile> =
    cfirProvider.getCfirFilesByPackage(packageFqName).filter { it.moduleData == moduleData }

/** 包级消费者使用同一原始声明收集边界；成员不能因为尚未被调用而遗漏。 */
fun CfirSession.cjmpSpecificDeclarationsInPackage(packageFqName: FqName): List<CfirDeclaration> = buildList {
    fun collect(declaration: CfirDeclaration) {
        if (declaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)) add(declaration)
        when (declaration) {
            is CfirClassLikeDeclaration -> declaration.declarations.forEach(::collect)
            is CfirExtend -> declaration.declarations.forEach(::collect)
            else -> Unit
        }
    }
    cjmpFilesInPackage(packageFqName).forEach { file -> file.declarations.forEach(::collect) }
}
