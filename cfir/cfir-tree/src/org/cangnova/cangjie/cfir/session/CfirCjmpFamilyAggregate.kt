package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.resolve.calls.mpp.*
import java.util.Collections

/** 候选程序已经产生的诊断事实；后续候选成功不能撤销这些事件。 */
sealed interface CfirCjmpCandidateDiagnostic {
    data class MissingBody(val commonCandidate: CfirDeclaration) : CfirCjmpCandidateDiagnostic
    data class ParameterMismatch(val mismatch: CjmpParameterMismatch) : CfirCjmpCandidateDiagnostic
    data class Occupied(val commonCandidate: CfirDeclaration, val previousSpecific: CfirDeclaration) : CfirCjmpCandidateDiagnostic
}

/**
 * 缓存外完成的组输入。声明、成员、位置、类型键与依赖状态均已经冻结；求值期间不查询 CFIR/provider/阶段。
 * 原容器先纯合并，再按合并后的声明次序执行各 root 的独立计划。
 */
class CfirCjmpPreparedGroup(
    val packageFqName: FqName,
    roots: List<CfirCjmpDeclarationMatchFacts>,
    topLevelOrder: List<CfirBasedSymbol<*>>,
    originalChildren: Map<CfirBasedSymbol<*>, List<CfirBasedSymbol<*>>>,
    commonDeclarations: List<CfirBasedSymbol<*>>,
    containers: List<CjmpMergeContainer<CfirBasedSymbol<*>, CfirTypeParameterSymbol>>,
    extensions: List<CjmpExtendMergeInput<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Int>>,
    commonExtendOrder: List<CfirBasedSymbol<*>>,
    originalDoNotExport: Set<CfirBasedSymbol<*>>,
    val classifierDomain: CfirCjmpClassifierDomain,
) {
    val roots = frozenList(roots)
    val topLevelOrder = frozenList(topLevelOrder)
    val originalChildren = frozenMap(originalChildren.mapValues { frozenList(it.value) })
    val commonDeclarations = frozenList(commonDeclarations)
    val containers = frozenList(containers.map { it.frozenInput() })
    val extensions = frozenList(extensions.map { it.copy(container = it.container.frozenInput()) })
    val commonExtendOrder = frozenList(commonExtendOrder)
    val originalDoNotExport = frozenSet(originalDoNotExport)

    fun evaluateStructure(): CfirCjmpStructureSnapshot {
        val merge = combineContainerResults(listOf(
            CjmpContainerMergeEvaluator.evaluateNominals(containers),
            CjmpContainerMergeEvaluator.evaluateExtensions(extensions, commonExtendOrder),
        ))
        val orderedSymbols = ArrayList<CfirBasedSymbol<*>>()
        fun collect(symbol: CfirBasedSymbol<*>) {
            orderedSymbols += symbol
            for (child in merge.members[symbol] ?: originalChildren[symbol].orEmpty()) collect(child)
        }
        topLevelOrder.forEach(::collect)
        val physicalOwners = (containers + extensions.map { it.container }).flatMap { container ->
            container.members.map { it.symbol to container.symbol }
        }.toMap()

        return CfirCjmpStructureSnapshot(
            packageFqName,
            topLevelOrder,
            orderedSymbols,
            commonDeclarations,
            containers + extensions.map { it.container },
            physicalOwners,
            merge,
            originalDoNotExport,
            classifierDomain,
        )
    }

    fun evaluate(): CfirCjmpFamilySnapshot {
        val structure = evaluateStructure()
        val merge = structure.merge
        val order = structure.declarationOrder.withIndex().associate { it.value to it.index }
        val seeds = merge.initialBindings.associate { it.common to it.specific }
        val physicalOwners = structure.physicalOwners

        fun candidateIsInMergedScope(specific: CfirBasedSymbol<*>, candidate: CjmpCandidatePlan<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Nothing>): Boolean {
            if (candidate.binding is CjmpBindingRule.MergedContainer || candidate.binding is CjmpBindingRule.MergedMemberVariable) {
                return seeds[candidate.common] === specific ||
                        candidate.eligibility != CjmpMatchResult.Matched ||
                        candidate.signatureCompatibility?.let { it != CjmpMatchResult.Matched } == true
            }
            val specificOwner = merge.logicalOwners[specific] ?: physicalOwners[specific]
            val commonOwner = merge.logicalOwners[candidate.common] ?: physicalOwners[candidate.common]
            return specificOwner === commonOwner
        }
        fun preparePlan(plan: CjmpMatchPlan<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Nothing>): CjmpMatchPlan<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Nothing> = when (plan) {
            // 候选序由 resolver 的查找入口决定；成员图的遍历序是合并布局，不可覆盖 first-fit 次序。
            is CjmpMatchPlan.Declaration -> plan.copy(candidates = plan.candidates.filter { candidateIsInMergedScope(plan.specific, it) })
            is CjmpMatchPlan.TuplePattern -> plan.copy(elements = plan.elements.map { element ->
                when (element) {
                    is CjmpPatternMatchElement.Variable -> {
                        val prepared = preparePlan(element.plan) as CjmpMatchPlan.Declaration<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Nothing>
                        CjmpPatternMatchElement.Variable(prepared)
                    }
                    is CjmpPatternMatchElement.NonVariable -> element
                }
            })
            is CjmpMatchPlan.NonTuplePattern -> plan
        }
        val orderedRoots = roots.sortedBy { order.getValue(it.rootSymbol) }
        // nominal 的后期验证没有绑定副作用；真正的容器/字段关系已经由同一个 merge 产生。
        val plans = orderedRoots.flatMap { it.plans }.map(::preparePlan)
        val aggregate = CjmpMatchEvaluator.evaluate(plans, merge.initialBindings)
        return CfirCjmpFamilySnapshot(
            packageFqName, orderedRoots, structure.topLevelOrder, structure.declarationOrder, structure.commonDeclarations,
            containers + extensions.map { it.container }, physicalOwners, merge, aggregate, structure.originalDoNotExport,
        )
    }
}

/** 主体推断前即可发布的容器、extend 与实例字段结构关系；不包含 callable 最终匹配结果。 */
class CfirCjmpStructureSnapshot internal constructor(
    val packageFqName: FqName,
    topLevelDeclarations: List<CfirBasedSymbol<*>>,
    declarationOrder: List<CfirBasedSymbol<*>>,
    commonDeclarations: List<CfirBasedSymbol<*>>,
    containers: List<CjmpMergeContainer<CfirBasedSymbol<*>, CfirTypeParameterSymbol>>,
    internal val physicalOwners: Map<CfirBasedSymbol<*>, CfirBasedSymbol<*>>,
    val merge: CjmpContainerMergeResult<CfirBasedSymbol<*>, CfirTypeParameterSymbol>,
    internal val originalDoNotExport: Set<CfirBasedSymbol<*>>,
    val classifierDomain: CfirCjmpClassifierDomain,
) : CfirCjmpMemberSnapshot {
    internal val topLevelOrder = frozenList(topLevelDeclarations)
    internal val declarationOrder = frozenList(declarationOrder)
    internal val commonDeclarations = frozenList(commonDeclarations)
    private val containers = frozenMap(containers.associateBy { it.symbol })
    private val doNotExport: Set<CfirBasedSymbol<*>> = frozenSet(
        (originalDoNotExport - merge.memberFacts.keys - merge.members.keys) + merge.doNotExport,
    )

    fun familyCommonDeclarations(): Set<CfirDeclaration> = frozenSet(commonDeclarations.map { it.cfir })

    /** 只提取本域实际选择的顶层 nominal seed；没有结构边与没有解析证明是不同状态。 */
    fun classifierRelationFor(common: CfirClassLikeSymbol<*>, actualSession: CfirSession): CfirCjmpClassifierRelation? {
        classifierDomain.requireSession(actualSession)
        val commonDeclaration = classifierDomain.requireDeclaration(common)
        classifierDomain.requirePhysicalRoot(common)
        check(common.classId.packageFqName == packageFqName) { "Common classifier is outside the original package domain" }
        check(common in topLevelOrder) { "A nested classifier requires its independently proven physical owner domain" }
        check(classifierDomain.moduleOf(common) in classifierDomain.refinementModules) {
            "A common classifier must belong to this actual session's refinement input"
        }
        val commonInput = checkNotNull(containers[common]) { "Classifier has no original nominal container input" }
        check(commonInput.kind != CjmpContainerKind.EXTEND && commonInput.isCommon) {
            "Only an original common nominal can provide a classifier relation"
        }
        check(commonInput.kind == classifierContainerKind(commonDeclaration)) { "Common container kind does not describe its real classifier" }
        check(commonDeclaration.name.asString() == commonInput.name &&
            commonDeclaration.status.isCommon == commonInput.isCommon &&
            commonDeclaration.status.isSpecific == commonInput.isSpecific &&
            commonDeclaration.typeParameters.map { it.symbol } == commonInput.typeParameters) {
            "Common nominal input changed after structural preparation"
        }
        val selected = merge.initialBindings.filter { it.common === common }
        check(selected.size <= 1) { "A common nominal has conflicting structural selections" }
        val binding = selected.singleOrNull() ?: return null
        val specific = checkNotNull(binding.specific as? CfirClassLikeSymbol<*>) {
            "A nominal seed must select a real classifier symbol"
        }
        val specificDeclaration = classifierDomain.requireDeclaration(specific)
        classifierDomain.requirePhysicalRoot(specific)
        check(specific.classId.packageFqName == packageFqName) { "Specific classifier is outside the original package domain" }
        val specificInput = checkNotNull(containers[specific])
        check(specificInput.kind == classifierContainerKind(specificDeclaration)) { "Specific container kind does not describe its real classifier" }
        check(specific in topLevelOrder && classifierDomain.moduleOf(specific) === classifierDomain.actualModule &&
            specificInput.isSpecific && specificInput.kind == commonInput.kind) {
            "Selected classifier does not belong to this actual nominal domain"
        }
        check(specificDeclaration.name.asString() == specificInput.name &&
            specificDeclaration.status.isCommon == specificInput.isCommon &&
            specificDeclaration.status.isSpecific == specificInput.isSpecific &&
            specificDeclaration.typeParameters.map { it.symbol } == specificInput.typeParameters) {
            "Specific nominal input changed after structural preparation"
        }
        return CfirCjmpClassifierRelation(common, specific, this)
    }

    private fun classifierContainerKind(declaration: CfirClassLikeDeclaration): CjmpContainerKind = when (declaration) {
        is CfirClass -> CjmpContainerKind.CLASS
        is CfirInterface -> CjmpContainerKind.INTERFACE
        is CfirStruct -> CjmpContainerKind.STRUCT
        is CfirEnum -> CjmpContainerKind.ENUM
        else -> error("Only declared nominal kinds can provide structural classifier relations")
    }

    override fun commonCounterpartsFor(specific: CfirDeclaration): List<CfirDeclaration> = frozenList(
        merge.initialBindings.filter { it.specific === specific.symbol }.map { it.common.cfir }.distinct(),
    )

    override fun isCommonOwnedMember(member: CfirDeclaration): Boolean {
        if ((member as? CfirMemberDeclaration)?.status?.isCommon == true) return true
        if ((member as? CfirConstructor)?.isPrimary == true) return false
        return physicalOwners[member.symbol]?.let { containers[it]?.isCommon } == true
    }

    override fun isSpecificOwnedMember(member: CfirDeclaration): Boolean =
        (member as? CfirMemberDeclaration)?.status?.isSpecific == true

    /** 结构视图只应用源声明与容器合并的导出规则，不提前应用 callable 匹配裁决。 */
    fun packageDeclarationsForExport(): List<CfirDeclaration> = frozenList(
        topLevelOrder.distinctBy { it.cfir.symbol }.filterNot { it in doNotExport }.map { it.cfir },
    )

    /** 最终配对阶段要解析完整匹配组的 specific 签名，不能受包导出过滤影响。 */
    fun topLevelDeclarationsForMatching(): List<CfirDeclaration> = frozenList(
        topLevelOrder.distinctBy { it.cfir.symbol }.map { it.cfir },
    )

    override fun membersFor(owner: CfirDeclaration): List<CfirDeclaration> =
        merge.members.getValue(owner.symbol).filter { it !in doNotExport }.map { it.cfir }

    override fun mergedDeclarationsFor(owner: CfirDeclaration): List<CfirDeclaration> =
        frozenList(merge.members.getValue(owner.symbol).map { it.cfir })

    override fun physicalOwnerFor(member: CfirDeclaration): CfirDeclaration? =
        physicalOwners[member.symbol]?.cfir

    override fun ownerTypeParameterMappingFor(member: CfirDeclaration): Map<CfirTypeParameter, CfirTypeParameter> {
        val physical = physicalOwners[member.symbol] ?: return emptyMap()
        val logical = merge.logicalOwners.getValue(member.symbol)
        if (physical === logical) return emptyMap()
        return containers.getValue(physical).typeParameters.zip(containers.getValue(logical).typeParameters)
            .associate { (common, specific) -> common.cfir to specific.cfir }
    }

    override fun logicalOwnerFor(member: CfirDeclaration): CfirDeclaration? = merge.logicalOwners[member.symbol]?.cfir

}

/**
 * 同一纯组求值的完整只读结果。匹配、默认值、诊断、实现关系和实际化成员图共享这一对象。
 * 快照不保存 live storage，也不向 common 声明发布依赖 specific session 的布尔属性。
 */
class CfirCjmpFamilySnapshot internal constructor(
    val packageFqName: FqName,
    roots: List<CfirCjmpDeclarationMatchFacts>,
    topLevelDeclarations: List<CfirBasedSymbol<*>>,
    declarationOrder: List<CfirBasedSymbol<*>>,
    commonDeclarations: List<CfirBasedSymbol<*>>,
    containers: List<CjmpMergeContainer<CfirBasedSymbol<*>, CfirTypeParameterSymbol>>,
    physicalOwners: Map<CfirBasedSymbol<*>, CfirBasedSymbol<*>>,
    val merge: CjmpContainerMergeResult<CfirBasedSymbol<*>, CfirTypeParameterSymbol>,
    val matching: CjmpMatchAggregate<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Nothing>,
    originalDoNotExport: Set<CfirBasedSymbol<*>>,
) : CfirCjmpMemberSnapshot {
    internal val roots = frozenList(roots)
    private val topLevelDeclarations = frozenList(topLevelDeclarations)
    internal val declarationOrder = frozenList(declarationOrder)
    private val commonDeclarations = frozenList(commonDeclarations)
    private val containers = frozenMap(containers.associateBy { it.symbol })
    private val physicalOwners = frozenMap(physicalOwners)
    private val rootsByOwnedSymbol = frozenMap(roots.flatMap { root -> root.ownedSymbols.map { it to root } }.toMap())
    private val originalDoNotExport = frozenSet(originalDoNotExport)
    val doNotExport: Set<CfirBasedSymbol<*>> = frozenSet(
        (originalDoNotExport - merge.memberFacts.keys - merge.members.keys) + merge.doNotExport + matching.doNotExport,
    )
    val specificDeclarations: List<CfirDeclaration> = frozenList(declarationOrder.filter { it in rootsByOwnedSymbol }.map { it.cfir })
    val matchedCommonForSpecific: Map<CfirDeclaration, List<CfirDeclaration>> = frozenMap(specificDeclarations.mapNotNull { specific ->
        commonCounterpartsFor(specific).takeIf { it.isNotEmpty() }?.let { specific to it }
    }.toMap())
    val defaults: Map<CfirBasedSymbol<*>, List<CjmpParameterDefaultState<CfirBasedSymbol<*>>>> get() = matching.defaults
    val hasSpecificDeclarations: Boolean get() = roots.any { it.plans.isNotEmpty() }

    fun commonFor(specific: CfirDeclaration): CfirDeclaration? {
        val counterparts = commonCounterpartsFor(specific)
        check(counterparts.size <= 1) { "A specific declaration has multiple common-parent counterparts" }
        return counterparts.singleOrNull()
    }

    /** 最终实现与历史接受记录分别保存；VAR 覆写之后只把最终关系暴露为 counterpart。 */
    override fun commonCounterpartsFor(specific: CfirDeclaration): List<CfirDeclaration> = frozenList(
        matching.acceptedBindings.filter { it.specific === specific.symbol && matching.implementations[it.common] === specific.symbol }
            .map { it.common.cfir }.distinct(),
    )

    /** 将稳定组级裁决投影为单声明属性，供惰性访问器在阶段发布后读取。 */
    fun declarationMatchResultFor(specific: CfirDeclaration): CfirCjmpDeclarationMatchResult =
        CfirCjmpDeclarationMatchResult(
            specific.symbol,
            commonCounterpartsFor(specific).map { common ->
                CfirCjmpCounterpartMatch(common.symbol, typeParameterMappingFor(specific, common))
            },
        )

    fun specificBindingsFor(common: CfirDeclaration): List<CfirDeclaration> =
        matching.implementations[common.symbol]?.let { listOf(it.cfir) }.orEmpty()

    fun typeParameterMappingsFor(
        specific: CfirDeclaration,
    ): List<Pair<CfirDeclaration, Map<CfirTypeParameter, CfirTypeParameter>>> =
        commonCounterpartsFor(specific).map { common -> common to typeParameterMappingFor(specific, common) }

    /** 单 parent 消费者的便捷入口；多 parent 必须按 counterpart 分别应用映射。 */
    fun typeParameterMappingFor(specific: CfirDeclaration): Map<CfirTypeParameter, CfirTypeParameter> {
        val mappings = typeParameterMappingsFor(specific)
        check(mappings.size <= 1) { "A specific declaration has multiple common-parent mappings" }
        return mappings.singleOrNull()?.second.orEmpty()
    }

    /** 多个 common extend owner 的映射分别查询，不能以 primary owner 覆盖其余映射。 */
    fun typeParameterMappingFor(specific: CfirDeclaration, common: CfirDeclaration): Map<CfirTypeParameter, CfirTypeParameter> =
        matching.acceptedBindings.lastOrNull { it.specific === specific.symbol && it.common === common.symbol }
            ?.typeParameterMapping?.map { (from, to) -> from.cfir to to.cfir }?.toMap().orEmpty()

    private fun resultSymbol(declaration: CfirDeclaration): CfirBasedSymbol<*> {
        val root = rootsByOwnedSymbol[declaration.symbol]
        return if (root?.rootSymbol === declaration.symbol && root.plans.size == 1) root.plans.single().specific else declaration.symbol
    }

    fun isUnmatched(specific: CfirDeclaration): Boolean = matching.matched[resultSymbol(specific)] == false

    fun hasResolutionResult(specific: CfirDeclaration): Boolean =
        rootsByOwnedSymbol[specific.symbol]?.rootSymbol === specific.symbol ||
                specific.symbol in matching.matched || matching.acceptedBindings.any { it.specific === specific.symbol }

    fun mismatchKindsFor(specific: CfirDeclaration): List<CjmpMismatchKind> {
        if (matching.matched[resultSymbol(specific)] == true) return emptyList()
        return matching.visits.filter { it.specific === resultSymbol(specific) }.mapNotNull { visit ->
            when (val result = visit.result) {
                is CjmpMatchResult.Mismatched -> result.kind
                is CjmpMatchResult.ParameterMismatched -> result.mismatch.kind
                CjmpMatchResult.TypeNotResolved -> CjmpMismatchKind.TYPE_NOT_RESOLVED
                CjmpMatchResult.Matched -> null
            }
        }
    }

    fun mismatchCandidatesFor(specific: CfirDeclaration): List<CfirDeclaration> =
        if (matching.matched[resultSymbol(specific)] == true) emptyList() else matching.visits
            .filter { it.specific === resultSymbol(specific) && it.result != CjmpMatchResult.Matched }.map { it.common.cfir }

    /** 只返回产生指定不匹配结论的 common 候选，供诊断参数读取真实对端声明。 */
    fun mismatchCandidatesFor(
        specific: CfirDeclaration,
        mismatchKind: CjmpMismatchKind,
    ): List<CfirDeclaration> = if (matching.matched[resultSymbol(specific)] == true) {
        emptyList()
    } else {
        matching.visits.filter { visit ->
            visit.specific === resultSymbol(specific) && when (val result = visit.result) {
                is CjmpMatchResult.Mismatched -> result.kind == mismatchKind
                is CjmpMatchResult.ParameterMismatched -> result.mismatch.kind == mismatchKind
                CjmpMatchResult.TypeNotResolved,
                CjmpMatchResult.Matched -> false
            }
        }.map { it.common.cfir }
    }

    fun candidateDiagnosticsFor(specific: CfirDeclaration): List<CfirCjmpCandidateDiagnostic> = matching.events.mapNotNull { event ->
        when (event) {
            is CjmpMatchEvent.MissingImplementation -> if (event.specific === specific.symbol) CfirCjmpCandidateDiagnostic.MissingBody(event.common.cfir) else null
            is CjmpMatchEvent.Occupied -> if (event.specific === specific.symbol) CfirCjmpCandidateDiagnostic.Occupied(event.common.cfir, event.previousSpecific.cfir) else null
            is CjmpMatchEvent.ParameterMismatch -> if (event.specific === specific.symbol) CfirCjmpCandidateDiagnostic.ParameterMismatch(event.mismatch) else null
            is CjmpMatchEvent.DiagnosticData -> error("CFIR match plans have no untyped diagnostic payload")
        }
    }

    fun isDuplicateSpecificExtend(specific: CfirDeclaration): Boolean = merge.events.any {
        it is CjmpContainerMergeEvent.DuplicateSpecificExtend && it.duplicate === specific.symbol
    }

    fun familyCommonDeclarations(): Set<CfirDeclaration> = frozenSet(commonDeclarations.map { it.cfir })

    /** P3 已裁决的包级 CJO roots；matched common roots 与 doNotExport 声明不进入 specific 导出。 */
    fun packageDeclarationsForExport(): List<CfirDeclaration> = frozenList(
        topLevelDeclarations.distinctBy { it.cfir.symbol }.filterNot { it in doNotExport }.map { it.cfir },
    )

    /** 实际化 declared 输入在继承归并之前取自此成员图；原始 CFIR declarations 不受影响。 */
    fun hasMemberOwner(owner: CfirDeclaration): Boolean = owner.symbol in merge.members

    override fun membersFor(owner: CfirDeclaration): List<CfirDeclaration> = merge.members.getValue(owner.symbol)
        .filter { it !in doNotExport }.map { it.cfir }

    override fun mergedDeclarationsFor(owner: CfirDeclaration): List<CfirDeclaration> =
        frozenList(merge.members.getValue(owner.symbol).map { it.cfir })

    override fun physicalOwnerFor(member: CfirDeclaration): CfirDeclaration? =
        physicalOwners[member.symbol]?.cfir

    /** 普通 common 成员也需先按 physical → logical owner 投影，再进行具体 receiver 替换。 */
    override fun ownerTypeParameterMappingFor(member: CfirDeclaration): Map<CfirTypeParameter, CfirTypeParameter> {
        val physical = physicalOwners[member.symbol] ?: return emptyMap()
        val logical = merge.logicalOwners.getValue(member.symbol)
        if (physical === logical) return emptyMap()
        return containers.getValue(physical).typeParameters.zip(containers.getValue(logical).typeParameters)
            .associate { (common, specific) -> common.cfir to specific.cfir }
    }

    /**
     * 返回声明或其最近物理容器成员的实际化 owner 映射。
     *
     * 函数参数、类型参数等声明节点本身不一定直接参与容器归并；调用方按声明树顺序传入
     * 物理祖先后，映射裁决仍由家族快照统一提供，序列化器不自行推导 common/specific 关系。
     */
    fun ownerTypeParameterMappingForDescendant(
        declaration: CfirDeclaration,
        physicalAncestors: Sequence<CfirDeclaration>,
    ): Map<CfirTypeParameter, CfirTypeParameter> =
        sequenceOf(declaration)
            .plus(physicalAncestors)
            .map(::ownerTypeParameterMappingFor)
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()

    override fun logicalOwnerFor(member: CfirDeclaration): CfirDeclaration? = merge.logicalOwners[member.symbol]?.cfir

    override fun isCommonOwnedMember(member: CfirDeclaration): Boolean {
        if ((member as? CfirMemberDeclaration)?.status?.isCommon == true) return true
        if ((member as? CfirConstructor)?.isPrimary == true) return false
        return physicalOwners[member.symbol]?.let { containers[it]?.isCommon } == true
    }

    override fun isSpecificOwnedMember(member: CfirDeclaration): Boolean =
        (member as? CfirMemberDeclaration)?.status?.isSpecific == true

    companion object {
        /** 家族只组合已经求值的独立组，不再次执行候选程序。 */
        fun combine(
            packageFqName: FqName,
            groups: List<CfirCjmpFamilySnapshot>,
            packageDeclarations: List<CfirDeclaration>,
        ): CfirCjmpFamilySnapshot {
            require(groups.all { it.packageFqName == packageFqName })
            val aggregates = groups.map { it.matching }
            return CfirCjmpFamilySnapshot(
                packageFqName,
                groups.flatMap { it.roots }, packageDeclarations.map { it.symbol },
                groups.flatMap { it.declarationOrder }, groups.flatMap { it.commonDeclarations },
                groups.flatMap { it.containers.values }, groups.flatMap { it.physicalOwners.entries }.associate { it.toPair() },
                combineContainerResults(groups.map { it.merge }),
                CjmpMatchAggregate(
                    frozenList(aggregates.flatMap { it.plans }), frozenList(aggregates.flatMap { it.initialBindings }),
                    frozenMap(aggregates.flatMap { it.matched.entries }.associate { it.toPair() }),
                    frozenMap(aggregates.flatMap { it.implementations.entries }.associate { it.toPair() }),
                    frozenSet(aggregates.flatMap { it.doNotExport }),
                    frozenMap(aggregates.flatMap { it.defaults.entries }.associate { it.toPair() }),
                    frozenList(aggregates.flatMap { it.acceptedBindings }), frozenList(aggregates.flatMap { it.visits }),
                    frozenList(aggregates.flatMap { it.events }),
                ),
                groups.flatMap { it.originalDoNotExport }.toSet(),
            )
        }
    }
}

/** 结构合并与最终匹配快照向成员 scope 暴露的共同只读契约。 */
interface CfirCjmpMemberSnapshot {
    fun membersFor(owner: CfirDeclaration): List<CfirDeclaration>
    /** 未应用导出/调用配对过滤的合并声明体；声明预检查与实例化成员建表保留原输入。 */
    fun mergedDeclarationsFor(owner: CfirDeclaration): List<CfirDeclaration>
    /** 原声明的物理容器；独立于 logicalOwnerFor，不把容器角色当作成员自身属性。 */
    fun physicalOwnerFor(member: CfirDeclaration): CfirDeclaration?
    fun ownerTypeParameterMappingFor(member: CfirDeclaration): Map<CfirTypeParameter, CfirTypeParameter>
    fun logicalOwnerFor(member: CfirDeclaration): CfirDeclaration?
    fun commonCounterpartsFor(specific: CfirDeclaration): List<CfirDeclaration>
    /** 按真实物理 owner 角色分类；未标记直接成员继承 common owner 的候选角色。 */
    fun isCommonOwnedMember(member: CfirDeclaration): Boolean
    /** 按真实物理 owner 角色分类；未标记直接成员继承 specific owner 的候选角色。 */
    fun isSpecificOwnedMember(member: CfirDeclaration): Boolean
}

/** 组、实际化成员与 checker 的统一入口；准备发生在目标 CJMP 锁及派生缓存 monitor 外。 */
enum class CfirCjmpMatchingMode {
    FINAL,
    /** alpha.66 调用候选视图：保留当前可见签名的求值结果，与隐式类型完成后的 FINAL 快照隔离。 */
    ACTUALIZATION_LOOKUP,
    DEFAULT_PARAMETER_BOOTSTRAP,
}

interface CfirCjmpFamilyAggregate : CfirSessionComponent {
    fun structureFor(declaration: CfirDeclaration): CfirCjmpStructureSnapshot

    fun matchingGroupFor(
        declaration: CfirDeclaration,
        declarationFacts: CfirCjmpDeclarationMatchFacts? = null,
        matchingMode: CfirCjmpMatchingMode = CfirCjmpMatchingMode.FINAL,
    ): CfirCjmpFamilySnapshot

    /** alpha.66 返回类型匹配完成前，按临时匹配视图读取默认参数事实。 */
    fun defaultParameterStatesForBootstrap(declaration: CfirDeclaration): List<CjmpParameterDefaultState<CfirBasedSymbol<*>>>

    fun ensureFamilyComplete(packageFqName: FqName): CfirCjmpFamilySnapshot
}

/** 由 resolve 层提供计划准备器，保持 providers 对匹配计划构造的依赖方向。 */
fun interface CfirCjmpDeclarationFactsPreparer {
    fun prepare(
        declaration: CfirMemberDeclaration,
        ownerFacts: CfirCjmpDeclarationMatchFacts?,
        matchingMode: CfirCjmpMatchingMode,
    ): CfirCjmpDeclarationMatchFacts
}

val CfirSession.cjmpFamilyAggregate: CfirCjmpFamilyAggregate by CfirSession.sessionComponentAccessor()

private fun CjmpMergeContainer<CfirBasedSymbol<*>, CfirTypeParameterSymbol>.frozenInput() = copy(
    typeParameters = frozenList(typeParameters), members = frozenList(members),
)

private fun combineContainerResults(
    results: List<CjmpContainerMergeResult<CfirBasedSymbol<*>, CfirTypeParameterSymbol>>,
): CjmpContainerMergeResult<CfirBasedSymbol<*>, CfirTypeParameterSymbol> = CjmpContainerMergeResult(
    frozenMap(results.flatMap { it.members.entries }.associate { it.toPair() }),
    frozenMap(results.flatMap { it.memberFacts.entries }.associate { it.toPair() }),
    frozenMap(results.flatMap { it.logicalOwners.entries }.associate { it.toPair() }),
    frozenMap(results.flatMap { it.dependencies.entries }.associate { it.toPair() }),
    frozenList(results.flatMap { it.initialBindings }), frozenSet(results.flatMap { it.doNotExport }),
    frozenSet(results.flatMap { it.commonNonExhaustive }), frozenList(results.flatMap { it.merges }), frozenList(results.flatMap { it.events }),
    frozenList(results.flatMap { it.dependencyMergeSteps }),
)

private fun <T> frozenList(values: Collection<T>): List<T> = Collections.unmodifiableList(ArrayList(values))
private fun <T> frozenSet(values: Collection<T>): Set<T> = Collections.unmodifiableSet(LinkedHashSet(values))
private fun <K, V> frozenMap(values: Map<K, V>): Map<K, V> = Collections.unmodifiableMap(LinkedHashMap(values))
