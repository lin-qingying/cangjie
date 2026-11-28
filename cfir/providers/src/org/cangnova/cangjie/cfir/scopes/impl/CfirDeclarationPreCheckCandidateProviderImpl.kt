package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.ScopeSessionKey
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.patterns.visibleBindingVariables
import org.cangnova.cangjie.cfir.scopes.*
import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.session.CfirCjmpMemberSnapshot
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.cjmpSourcePosition
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.session.requiresDeferredCjmpFinalMatching
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import java.util.Collections

/**
 * 声明候选只从原声明表/未导出过滤的 merge 声明体构造。
 *
 * snapshot、输入视图、真实声明/来源及冻结自身位均参与 ScopeSession 键；具体名称、参数
 * 签名、implementation 指针不决定 COMMON 删除资格。继承与调用不复用此过滤表。
 */
class CfirDeclarationPreCheckCandidateProviderImpl(private val session: CfirSession) : CfirDeclarationPreCheckCandidateProvider {
    override fun inputFor(
        owner: CfirDeclaration,
        scopeSession: ScopeSession,
        view: CfirDeclarationPreCheckView,
    ): CfirDeclarationPreCheckInput {
        if (view != CfirDeclarationPreCheckView.RAW) {
            check(session.isCjmpSpecificCompilationEnabled()) { "CJMP PreCheck requires a specific-compilation session" }
        }
        val snapshot = snapshotFor(owner, view)
        val scope = when (owner) {
            is CfirFile -> CfirDeclarationPreCheckScopeIdentity.Package(session, owner.packageDirective.packageFqName)
            is CfirClassLikeDeclaration, is CfirExtend -> CfirDeclarationPreCheckScopeIdentity.Member(owner.symbol)
            else -> error("Declaration PreCheck requires a file, nominal declaration or extend owner")
        }
        val declarations = when (owner) {
            is CfirFile -> packageDeclarations(owner)
            is CfirClassLikeDeclaration -> snapshot?.mergedDeclarationsFor(owner) ?: owner.declarations
            is CfirExtend -> snapshot?.mergedDeclarationsFor(owner) ?: owner.declarations
            else -> error("Declaration PreCheck requires a declaration scope")
        }.distinctBy { it.symbol }
        val candidates = declarations.mapIndexed { index, declaration ->
            candidateFor(declaration, owner.takeUnless { it is CfirFile }, scope, snapshot, index)
        }
        return scopeSession.getOrBuild(InputKey(scope, view, snapshot, candidates), PRECHECK_INPUTS) {
            val groups = candidates.filter { it.functionKind != null }.groupBy { checkNotNull(it.names) }.map { (names, raw) ->
                val hasSpecific = view != CfirDeclarationPreCheckView.RAW && raw.any { it.attributes.isSpecific }
                val filtered = if (hasSpecific) raw.filterNot { it.attributes.isCommon } else raw
                CfirDeclarationPreCheckFunctionGroup(names, raw, filtered)
            }
            CfirDeclarationPreCheckInput(scope, view, snapshot, candidates, groups)
        }
    }

    /** FINAL 按真实阶段先完成匹配；没有异常后选择结构视图的分支。 */
    private fun snapshotFor(owner: CfirDeclaration, view: CfirDeclarationPreCheckView): CfirCjmpMemberSnapshot? {
        if (view == CfirDeclarationPreCheckView.RAW || owner is CfirFile ||
            !owner.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)
        ) return null
        return when (view) {
            CfirDeclarationPreCheckView.RAW -> error("RAW PreCheck does not request a family snapshot")
            CfirDeclarationPreCheckView.CJMP_STRUCTURE -> {
                owner.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
                session.cjmpFamilyAggregate.structureFor(owner)
            }
            CfirDeclarationPreCheckView.CJMP_FINAL -> {
                owner.lazyResolveToPhase(
                    if (session.requiresDeferredCjmpFinalMatching()) CfirResolvePhase.CJMP_FINAL_MATCHING
                    else CfirResolvePhase.CJMP_MATCHING,
                )
                session.cjmpFamilyAggregate.matchingGroupFor(owner)
            }
        }
    }

    /** 同包跨文件声明共享作用域；普通依赖候选仍由 collector 的模块/可见性规则裁决。 */
    private fun packageDeclarations(file: CfirFile): List<CfirDeclaration> {
        val packageName = file.packageDirective.packageFqName
        // Platform checker 以 specific session 检查整个 refinement 图，当前 file 可以物理属于 common。
        // 原文件/其会话提供原始声明，消费会话提供本平台同包声明；两者共享逻辑 package 身份。
        val source = listOf(session, file.moduleData.session).distinct().flatMap { declarationSession ->
            declarationSession.cfirProvider.getCfirFilesByPackage(packageName).flatMap { it.declarations }
        }.distinctBy { it.symbol }
        val peers = buildList {
            val provider = session.dependenciesSymbolProvider
            val callableNames = source.filterIsInstance<CfirCallableDeclaration>().flatMap { declaration ->
                if (declaration is CfirPatternVariable) declaration.pattern.visibleBindingVariables().map { it.name }
                else listOfNotNull(normalizedName(declaration))
            }.distinct().sortedBy { it.asString() }
            for (name in callableNames) addAll(provider.getTopLevelCallableSymbols(packageName, name).map { it.cfir })
            for (name in source.filterIsInstance<CfirClassLikeDeclaration>().map { it.name }.distinct().sortedBy { it.asString() }) {
                addAll(provider.getClassLikeSymbolsByClassId(ClassId(packageName, name)).map { it.cfir })
            }
        }
        return (source + peers).distinctBy { it.symbol }.sortedWith(compareBy(
            { it.cjmpSourcePosition()?.fileName },
            { it.cjmpSourcePosition()?.line },
            { it.cjmpSourcePosition()?.column },
        ))
    }

    private fun candidateFor(
        declaration: CfirDeclaration,
        rawOwner: CfirDeclaration?,
        scope: CfirDeclarationPreCheckScopeIdentity,
        snapshot: CfirCjmpMemberSnapshot?,
        index: Int,
    ): CfirDeclarationPreCheckCandidate {
        val physical = if (snapshot == null) rawOwner else checkNotNull(snapshot.physicalOwnerFor(declaration)) {
            "A merged PreCheck member must retain its physical owner"
        }
        val logical = if (snapshot == null) rawOwner else checkNotNull(snapshot.logicalOwnerFor(declaration)) {
            "A merged PreCheck member must retain its logical owner"
        }
        val mapping = if (snapshot == null) emptyMap() else snapshot.ownerTypeParameterMappingFor(declaration)
        val status = (declaration as? CfirMemberDeclaration)?.status
        return CfirDeclarationPreCheckCandidate(
            declaration = declaration,
            names = normalizedName(declaration)?.let { CfirDeclarationPreCheckNames(it, scope) },
            functionKind = functionKind(declaration),
            attributes = CfirDeclarationPreCheckAttributes(
                status?.isCommon == true, status?.isSpecific == true, declaration.isFromCommonPart,
                status?.isStatic == true, status?.isForeign == true, status?.isC == true,
            ),
            physicalOwner = physical,
            logicalOwner = logical,
            ownerTypeParameterMapping = Collections.unmodifiableMap(LinkedHashMap(mapping)),
            sourceFileIdentity = sourceFileIdentity(declaration),
            origin = declaration.origin,
            declarationOrder = index,
            sourceStartOffset = declaration.source?.startOffset,
            sourceEndOffset = declaration.source?.endOffset,
        )
    }

    private fun sourceFileIdentity(declaration: CfirDeclaration): CfirDeclarationPreCheckSourceFileIdentity? {
        declaration.symbol.getContainingFile()?.let { return CfirDeclarationPreCheckSourceFileIdentity.Source(it) }
        return declaration.cjoDeclarationPosition?.declarationLocation?.filePath?.let { path ->
            CfirDeclarationPreCheckSourceFileIdentity.Binary(declaration.moduleData, path)
        }
    }

    private fun normalizedName(declaration: CfirDeclaration): Name? = when (declaration) {
        is CfirConstructor -> Name.identifier("init")
        is CfirFinalizer -> Name.identifier("~init")
        is CfirMainFunction -> Name.identifier("main")
        is CfirMacroDeclaration -> declaration.name
        is CfirNamedFunction -> declaration.name
        is CfirEnumConstructor -> declaration.name
        is CfirClassLikeDeclaration -> declaration.name
        is CfirCallableDeclaration -> declaration.symbol.name.takeUnless { it.isSpecial }
        else -> null
    }

    private fun functionKind(declaration: CfirDeclaration): CfirDeclarationPreCheckFunctionKind? = when (declaration) {
        is CfirConstructor -> CfirDeclarationPreCheckFunctionKind.CONSTRUCTOR
        is CfirEnumConstructor -> if (declaration.valueParameters.isEmpty()) null else CfirDeclarationPreCheckFunctionKind.ENUM_CONSTRUCTOR
        is CfirMainFunction -> CfirDeclarationPreCheckFunctionKind.MAIN
        is CfirMacroDeclaration -> CfirDeclarationPreCheckFunctionKind.MACRO
        is CfirFinalizer -> CfirDeclarationPreCheckFunctionKind.FINALIZER
        is CfirNamedFunction -> CfirDeclarationPreCheckFunctionKind.FUNCTION
        else -> null
    }

    private data class InputKey(
        val scope: CfirDeclarationPreCheckScopeIdentity,
        val view: CfirDeclarationPreCheckView,
        val snapshot: CfirCjmpMemberSnapshot?,
        val candidates: List<CfirDeclarationPreCheckCandidate>,
    )

    private object PRECHECK_INPUTS : ScopeSessionKey<InputKey, CfirDeclarationPreCheckInput>()
}
