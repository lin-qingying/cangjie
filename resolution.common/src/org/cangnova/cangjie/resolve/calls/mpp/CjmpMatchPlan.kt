package org.cangnova.cangjie.resolve.calls.mpp

import java.util.Collections

/** 参数默认状态分开记录语言标志、真实 desugar helper 与可读穿资格。 */
data class CjmpParameterDefaultState<D : CjmpDeclarationSymbolMarker>(
    val hasInitial: Boolean,
    val desugaredDefaultSource: D?,
    /** 无 helper 的反序列化默认值 marker 仍可供 specific 调用做默认参数适用性判断。 */
    val canBeReadThrough: Boolean = desugaredDefaultSource != null,
)

/** 单个参数按官方顺序检查名字、默认值冲突，并按需转移 common 默认来源。 */
data class CjmpParameterMatch<D : CjmpDeclarationSymbolMarker>(
    val index: Int,
    val namesMatch: Boolean,
    val commonDefault: CjmpParameterDefaultState<D>,
)

/** 一次实现关系写入的独立输入，不读取或改变声明对象。 */
data class CjmpBindingRequest<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker>(
    val specific: D,
    val common: D,
    val typeParameterMapping: Map<T, T>,
    val requiresImplementation: Boolean,
)

/**
 * 声明种类规定的绑定规则。
 *
 * 官方 function/enum 使用占用优先的 TrySetSpecificImpl；property 依次处理本体和访问器；
 * extend 将同 key 的多个 common owner 合并到 specific；static/global variable 直接替换实现，
 * 实例字段则消费 nominal 合并已经建立的关系。
 */
sealed interface CjmpBindingRule<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker> {
    data class Callable<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker>(
        val request: CjmpBindingRequest<D, T>,
    ) : CjmpBindingRule<D, T>

    data class Property<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker>(
        val property: CjmpBindingRequest<D, T>,
        val getter: CjmpBindingRequest<D, T>?,
        val setter: CjmpBindingRequest<D, T>?,
    ) : CjmpBindingRule<D, T>

    data class VariableAssignment<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker>(
        val request: CjmpBindingRequest<D, T>,
        val kind: CjmpVariableAssignmentKind,
    ) : CjmpBindingRule<D, T>

    data class MergedMemberVariable<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker>(
        val request: CjmpBindingRequest<D, T>,
    ) : CjmpBindingRule<D, T>

    /** nominal/extend 的后期验证只消费实际合并结果，不能重新执行 callable 占用或重复建立绑定。 */
    data class MergedContainer<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker>(
        val request: CjmpBindingRequest<D, T>,
    ) : CjmpBindingRule<D, T>
}

/** 变量赋实现时，只有 static/global 分支同时禁止导出 common；静态性不一致分支只赋实现。 */
enum class CjmpVariableAssignmentKind {
    STATIC_OR_GLOBAL,
    STATIC_MISMATCH,
}

/**
 * 一个候选的不可变执行输入；类型、泛型约束和诊断参数均在进入求值器之前取得。
 *
 * 不使用可执行闭包或字符串指令。参数前后的检查边界对应官方 MatchCJMPFunction；
 * Diagnostic 只承载前端提供的诊断数据，求值器不会调用 reporter 或重新解析类型。
 */
data class CjmpCandidatePlan<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic>(
    val common: D,
    val eligibility: CjmpMatchResult,
    val beforeSignatureDiagnostics: List<Diagnostic>,
    val signatureCompatibility: CjmpMatchResult?, // 资格拒绝时类型签名未计算。
    val parameters: List<CjmpParameterMatch<D>>,
    val afterParameterDiagnostics: List<Diagnostic>,
    val afterParameters: CjmpMatchResult,
    val beforeBindingDiagnostics: List<Diagnostic>,
    val binding: CjmpBindingRule<D, T>,
)

/** 候选停止规则由语言版本策略提供；求值器不按 parent 分组或重排输入。 */
enum class CjmpCandidateTraversal {
    FIRST_SUCCESS,
    ALL_CANDIDATES,
}

/** 声明匹配输入；一个模式的各绑定仍是独立声明，不压成单个 common 对应物。 */
sealed interface CjmpMatchPlan<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic> {
    val specific: D

    data class Declaration<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic>(
        override val specific: D,
        val initialDefaults: List<CjmpParameterDefaultState<D>>,
        val candidates: List<CjmpCandidatePlan<D, T, Diagnostic>>,
        val traversal: CjmpCandidateTraversal,
        /** 由声明所属语言版本冻结；纯求值器不能自行推断版本策略。 */
        val checkParameterDefaultValueConflict: Boolean,
    ) : CjmpMatchPlan<D, T, Diagnostic>

    data class TuplePattern<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic>(
        override val specific: D,
        val elements: List<CjmpPatternMatchElement<D, T, Diagnostic>>,
    ) : CjmpMatchPlan<D, T, Diagnostic>

    data class NonTuplePattern<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic>(
        override val specific: D,
    ) : CjmpMatchPlan<D, T, Diagnostic>
}

/** 官方模式配对仅遍历变量项；遇到非变量项停止，之前成功的绑定仍保留。 */
sealed interface CjmpPatternMatchElement<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic> {
    data class Variable<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic>(
        val plan: CjmpMatchPlan.Declaration<D, T, Diagnostic>,
    ) : CjmpPatternMatchElement<D, T, Diagnostic>

    class NonVariable<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic> :
        CjmpPatternMatchElement<D, T, Diagnostic>
}

/**
 * 复制并冻结匹配计划的全部集合边界，供声明属性和并发派生缓存共同保存。
 *
 * 符号与诊断值由调用方提供；仓颉 CFIR 计划的诊断类型为 `Nothing`。这里冻结计划自身的列表、映射和嵌套
 * 规则，避免求值后原始 builder 集合被修改，或只读声明事实仍间接引用可变输入。
 */
fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic>
    CjmpMatchPlan<D, T, Diagnostic>.frozenCopy(): CjmpMatchPlan<D, T, Diagnostic> = when (this) {
    is CjmpMatchPlan.Declaration -> frozenDeclarationCopy()

    is CjmpMatchPlan.TuplePattern -> CjmpMatchPlan.TuplePattern(
        specific = specific,
        elements = immutableListCopy(elements.map { element ->
            when (element) {
                is CjmpPatternMatchElement.Variable -> CjmpPatternMatchElement.Variable(element.plan.frozenDeclarationCopy())
                is CjmpPatternMatchElement.NonVariable -> CjmpPatternMatchElement.NonVariable()
            }
        }),
    )

    is CjmpMatchPlan.NonTuplePattern -> CjmpMatchPlan.NonTuplePattern(specific)
}

private fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic>
    CjmpMatchPlan.Declaration<D, T, Diagnostic>.frozenDeclarationCopy(): CjmpMatchPlan.Declaration<D, T, Diagnostic> =
    CjmpMatchPlan.Declaration(
        specific = specific,
        initialDefaults = immutableListCopy(initialDefaults),
        candidates = immutableListCopy(candidates.map { it.frozenCopy() }),
        traversal = traversal,
        checkParameterDefaultValueConflict = checkParameterDefaultValueConflict,
    )

private fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic>
    CjmpCandidatePlan<D, T, Diagnostic>.frozenCopy(): CjmpCandidatePlan<D, T, Diagnostic> = copy(
    beforeSignatureDiagnostics = immutableListCopy(beforeSignatureDiagnostics),
    parameters = immutableListCopy(parameters),
    afterParameterDiagnostics = immutableListCopy(afterParameterDiagnostics),
    beforeBindingDiagnostics = immutableListCopy(beforeBindingDiagnostics),
    binding = when (val rule = binding) {
        is CjmpBindingRule.Callable -> CjmpBindingRule.Callable(rule.request.frozenCopy())
        is CjmpBindingRule.Property -> CjmpBindingRule.Property(
            property = rule.property.frozenCopy(),
            getter = rule.getter?.frozenCopy(),
            setter = rule.setter?.frozenCopy(),
        )
        is CjmpBindingRule.VariableAssignment -> CjmpBindingRule.VariableAssignment(
            request = rule.request.frozenCopy(),
            kind = rule.kind,
        )
        is CjmpBindingRule.MergedMemberVariable -> CjmpBindingRule.MergedMemberVariable(rule.request.frozenCopy())
        is CjmpBindingRule.MergedContainer -> CjmpBindingRule.MergedContainer(rule.request.frozenCopy())
    },
)

private fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker>
    CjmpBindingRequest<D, T>.frozenCopy(): CjmpBindingRequest<D, T> = copy(
    typeParameterMapping = Collections.unmodifiableMap(LinkedHashMap(typeParameterMapping)),
)

private fun <T> immutableListCopy(values: Collection<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))

/** 一次被接受的绑定；历史接受记录与聚合中的最终占用表分别保存。 */
data class CjmpAcceptedBinding<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker>(
    val specific: D,
    val common: D,
    val typeParameterMapping: Map<T, T>,
    val suppressesCommonExport: Boolean,
)

/** 求值产生的有序事件，checker 负责把它们映射到实际诊断通道。 */
sealed interface CjmpMatchEvent<D : CjmpDeclarationSymbolMarker, Diagnostic> {
    data class DiagnosticData<D : CjmpDeclarationSymbolMarker, Diagnostic>(
        val specific: D,
        val common: D,
        val diagnostic: Diagnostic,
    ) : CjmpMatchEvent<D, Diagnostic>

    data class ParameterMismatch<D : CjmpDeclarationSymbolMarker, Diagnostic>(
        val specific: D,
        val common: D,
        val mismatch: CjmpParameterMismatch,
    ) : CjmpMatchEvent<D, Diagnostic>

    data class Occupied<D : CjmpDeclarationSymbolMarker, Diagnostic>(
        val specific: D,
        val common: D,
        val previousSpecific: D,
    ) : CjmpMatchEvent<D, Diagnostic>

    data class MissingImplementation<D : CjmpDeclarationSymbolMarker, Diagnostic>(
        val specific: D,
        val common: D,
    ) : CjmpMatchEvent<D, Diagnostic>
}

/** 实际访问过的候选及其结果；未访问的尾部候选不产生事件。 */
data class CjmpCandidateVisit<D : CjmpDeclarationSymbolMarker>(
    val specific: D,
    val common: D,
    val result: CjmpMatchResult,
)

/**
 * 完整声明族的一次纯求值快照。所有集合均由本次局部状态复制，不能继续写入。
 * 默认来源供 IMPLICIT/BODY 消费，诊断事件供 checker 消费，两者来自同一执行结果。
 */
data class CjmpMatchAggregate<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic>(
    /** 冻结的完整求值输入，供声明投影和后续有序 family 求值共享。 */
    val plans: List<CjmpMatchPlan<D, T, Diagnostic>>,
    /** 冻结的真实合并 seed；声明投影只保留该根拥有的 seed。 */
    val initialBindings: List<CjmpAcceptedBinding<D, T>>,
    val matched: Map<D, Boolean>,
    val implementations: Map<D, D>,
    val doNotExport: Set<D>,
    val defaults: Map<D, List<CjmpParameterDefaultState<D>>>,
    val acceptedBindings: List<CjmpAcceptedBinding<D, T>>,
    val visits: List<CjmpCandidateVisit<D>>,
    val events: List<CjmpMatchEvent<D, Diagnostic>>,
)

/**
 * 单个阶段根拥有的冻结匹配输入。
 *
 * 该属性只保存独立候选计划和真实合并 seed，不保存求值结果。候选访问、默认值转移、占用事件和最终实现
 * 都依赖调用方提供的规范顺序，必须由 [CjmpMatchEvaluator] 对多个根事实统一重放。
 */
class CjmpMatchFacts<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic> internal constructor(
    plans: List<CjmpMatchPlan<D, T, Diagnostic>>,
    initialBindings: List<CjmpAcceptedBinding<D, T>>,
) {
    /** 当前 root 及其真实子声明的有序兼容性计划。 */
    val plans: List<CjmpMatchPlan<D, T, Diagnostic>> = immutableListCopy(plans.map { it.frozenCopy() })

    /** 由真实合并产生、供有序组求值消费的初始绑定。 */
    val initialBindings: List<CjmpAcceptedBinding<D, T>> = immutableListCopy(initialBindings.map { binding ->
        binding.copy(typeParameterMapping = Collections.unmodifiableMap(LinkedHashMap(binding.typeParameterMapping)))
    })

    companion object {
        /**
         * 直接冻结 root 的独立输入，不运行候选程序，也不要求某个组的占用或合并结果已经求值。
         * 声明发布和完整组求值是两个入口；不能为了创建输入事实先执行一次 evaluator。
         */
        fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic> create(
            plans: List<CjmpMatchPlan<D, T, Diagnostic>>,
            initialBindings: List<CjmpAcceptedBinding<D, T>> = emptyList(),
        ): CjmpMatchFacts<D, T, Diagnostic> = CjmpMatchFacts(plans, initialBindings)
    }
}

/** 按具体阶段根及其拥有的子声明投影冻结输入，不携带任何求值结果或组级占用索引。 */
fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic>
    CjmpMatchAggregate<D, T, Diagnostic>.factsOwnedBy(ownedSpecifics: Collection<D>): CjmpMatchFacts<D, T, Diagnostic> {
    val owned = Collections.newSetFromMap(java.util.IdentityHashMap<D, Boolean>())
    owned.addAll(ownedSpecifics)
    return CjmpMatchFacts(
        plans = plans.filter { it.specific in owned },
        initialBindings = initialBindings.filter { it.specific in owned },
    )
}
