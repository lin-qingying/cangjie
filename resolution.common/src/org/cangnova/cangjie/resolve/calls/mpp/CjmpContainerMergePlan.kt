package org.cangnova.cangjie.resolve.calls.mpp

import java.util.Collections
import java.util.LinkedHashMap

/** 官方 nominal/extend 的声明种类；成员合并只接受相同种类。 */
enum class CjmpContainerKind {
    CLASS,
    STRUCT,
    INTERFACE,
    ENUM,
    EXTEND,
}

/**
 * 合并前的成员事实。isInstanceVariable 严格对应 VAR_DECL && !IsStaticOrGlobal，
 * 因而 property、函数、构造器和 enum 构造项都不能进入字段预绑定。
 */
data class CjmpMergeMember<D : CjmpDeclarationSymbolMarker>(
    val symbol: D,
    val name: String,
    val isInstanceVariable: Boolean,
    val isMemberParameter: Boolean,
    val isCommon: Boolean,
    val isSpecific: Boolean,
    val hasCommonDefault: Boolean,
    val doNotExport: Boolean,
    val dependencies: CjmpInitializationDependencies<D>,
    /** 来源位独立于 COMMON；common part 的普通字段也参与 common 初始化序列。 */
    val isFromCommonPart: Boolean,
    /** 前端按真实 begin 的 line/column 冻结的排序名次；官方比较不包含 fileID，也不是合并后的槽位。 */
    val sourceOrder: Int,
)

/**
 * 容器的独立原始快照。hasGeneric 表达 generic 节点是否存在，不能与参数数量比较混为一谈。
 * 所有成员来自真实声明列表；求值器不会从已建立的配对关系反推合并输入。
 */
data class CjmpMergeContainer<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker>(
    val symbol: D,
    val name: String,
    val kind: CjmpContainerKind,
    val isCommon: Boolean,
    val isSpecific: Boolean,
    val hasGeneric: Boolean,
    val typeParameters: List<T>,
    val isNonExhaustive: Boolean,
    val doNotExport: Boolean,
    val members: List<CjmpMergeMember<D>>,
)

/** extend 的键由前端类型层计算；null 明确表示无效类型，不参与官方的键分组。 */
data class CjmpExtendMergeInput<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, K : Any>(
    val container: CjmpMergeContainer<D, T>,
    val key: K?,
)

/** 合并诊断按真实动作顺序保留，checker 决定诊断源和呈现方式。 */
sealed interface CjmpContainerMergeEvent<D : CjmpDeclarationSymbolMarker> {
    data class DifferentKind<D : CjmpDeclarationSymbolMarker>(val common: D, val specific: D) : CjmpContainerMergeEvent<D>
    data class NonExhaustiveMismatch<D : CjmpDeclarationSymbolMarker>(val common: D, val specific: D) : CjmpContainerMergeEvent<D>
    data class UnmatchedSpecificField<D : CjmpDeclarationSymbolMarker>(val field: D) : CjmpContainerMergeEvent<D>
    data class UnmatchedCommonField<D : CjmpDeclarationSymbolMarker>(val field: D) : CjmpContainerMergeEvent<D>
    data class DuplicateSpecificExtend<D : CjmpDeclarationSymbolMarker>(val first: D, val duplicate: D) : CjmpContainerMergeEvent<D>
}

/** 一次实际合并及当时从 common 移入的成员，保留重复 nominal 合并的历史边界。 */
data class CjmpContainerMerge<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker>(
    val common: D,
    val specific: D,
    val typeParameterMapping: Map<T, T>,
    val commonMembers: List<D>,
)

/** 单次合并前已接受的实现边，以及该次官方依赖更新所遍历的有效成员。 */
data class CjmpInitializationDependencyMergeStep<D : CjmpDeclarationSymbolMarker>(
    val members: List<D>,
    val implementations: Map<D, D>,
)

/**
 * 合并后的不可变视图：有效成员与 common 中保留的旧字段分别由 members 给出。
 * logicalOwners 不改写原声明；initialBindings 直接作为 CjmpMatchEvaluator 的真实 merge seed。
 */
data class CjmpContainerMergeResult<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker>(
    val members: Map<D, List<D>>,
    /** 原成员资格和来源事实；依赖消费者读取下方合并后的 dependencies，而非原始边。 */
    val memberFacts: Map<D, CjmpMergeMember<D>>,
    val logicalOwners: Map<D, D>,
    val dependencies: Map<D, CjmpInitializationDependencies<D>>,
    val initialBindings: List<CjmpAcceptedBinding<D, T>>,
    val doNotExport: Set<D>,
    val commonNonExhaustive: Set<D>,
    val merges: List<CjmpContainerMerge<D, T>>,
    val events: List<CjmpContainerMergeEvent<D>>,
    /** 精确保留逐次合并时的依赖单边替换边界，供独立依赖生命周期晚于结构快照时重放。 */
    val dependencyMergeSteps: List<CjmpInitializationDependencyMergeStep<D>> = emptyList(),
) {
    /**
     * 将晚发布的原始依赖放进已冻结的实际化成员图，并重放每次合并发生时的单边替换。
     * 结构快照不读取依赖属性；该方法不会改写原图、成员事实或替换步骤。
     */
    fun withInitializationDependencies(
        rawDependencies: Map<D, CjmpInitializationDependencies<D>>,
    ): CjmpContainerMergeResult<D, T> {
        require(rawDependencies.keys.all(memberFacts::containsKey)) {
            "Initialization dependencies may only be supplied for members in the frozen graph"
        }
        val refreshed = LinkedHashMap<D, CjmpInitializationDependencies<D>>(dependencies)
        for ((symbol, facts) in memberFacts) {
            refreshed[symbol] = rawDependencies[symbol] ?: facts.dependencies
        }
        for (step in dependencyMergeSteps) {
            for (symbol in step.members) {
                refreshed[symbol] = refreshed.getValue(symbol).mapDeclarations { dependency ->
                    step.implementations[dependency] ?: dependency
                }
            }
        }
        return copy(dependencies = Collections.unmodifiableMap(refreshed))
    }
}

/** 官方 VarDeclWithPosition：初始化次序变化后仍保留原有效实例字段槽位。 */
data class CjmpVariableWithPosition<D : CjmpDeclarationSymbolMarker>(
    val symbol: D,
    val position: Int,
)
