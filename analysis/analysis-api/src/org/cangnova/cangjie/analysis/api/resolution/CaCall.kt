package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name

/**
 * 调用解析结果。
 *
 * Analysis API 需要对外暴露“调用点最终看到了哪些候选、最终选择了哪个候选”，
 * 但不能把底层 CFIR 的候选对象直接泄漏到上层。
 *
 * 因此这里稳定公开两层信息：
 * 1. [successfulCall] 表示无错误的最终选中调用
 * 2. [calls] 表示当前调用点可观察到的调用视图集合，允许包含带错误的已选候选
 */
interface CaCallInfo : CaLifetimeOwner {
    /**
     * 最终无错误解析成功的调用。
     *
     * 如果调用未成功解析，或最终选择的是“带错误但仍被保留”的候选，则返回 `null`。
     */
    val successfulCall: CaCall?

    /**
     * 当前调用点可观察到的调用视图集合。
     *
     * 该集合面向稳定语义建模，而不是底层 resolver 的瞬时内部对象。
     */
    val calls: List<CaCall>
}

/**
 * 调用种类。
 */
enum class CaCallKind {
    FUNCTION,
}

/**
 * 调用来源。
 *
 * 它描述的是源码层面的语义来源，而不是底层 CFIR 节点实现细节。
 */
enum class CaCallOrigin {
    REGULAR,
    OPERATOR,
}

/**
 * 候选调用适用性。
 *
 * 该枚举与底层候选适用性一一对应，但命名和语义稳定在 Analysis API 中，
 * 使 IDE、Standalone、LSP 可以共享同一套公开调用结果模型。
 */
enum class CaCallApplicability {
    HIDDEN,
    INAPPLICABLE_WRONG_RECEIVER,
    INAPPLICABLE_ARGUMENTS_MAPPING_ERROR,
    INAPPLICABLE,
    VISIBILITY_ERROR,
    UNSAFE_CALL,
    UNSTABLE_SMARTCAST,
    CONVENTION_ERROR,
    RESOLVED_LOW_PRIORITY,
    RESOLVED_NEED_PRESERVE_COMPATIBILITY,
    RESOLVED_WITH_ERROR,
    RESOLVED,
}

/**
 * 单个源码实参与形参的映射结果。
 *
 * 该结构只描述“源码参数列表中的显式实参”。
 * 隐式接收者、上下文参数等额外参与解析的信息通过 [CaCall] 的专门字段暴露，
 * 避免把底层 candidate 内部顺序直接泄漏给上层。
 */
interface CaCallArgumentMapping : CaLifetimeOwner {
    /**
     * 实参在源码参数列表中的位置。
     */
    val argumentIndex: Int

    /**
     * 解析后映射到的形参名。
     *
     * 当参数映射尚未建立，或当前实参未成功映射到某个形参时返回 `null`。
     */
    val parameterName: Name?

    /**
     * 解析后映射到的形参类型。
     *
     * 当形参不存在或其类型尚不可用时返回 `null`。
     */
    val parameterType: CaType?
}

/**
 * 单个调用的公开语义视图。
 *
 * 该视图稳定暴露调用目标、适用性、接收者拆分、显式类型参数以及实参与形参映射，
 * 供 Analysis API 测试框架、引用服务、渲染器和上层工具共享。
 */
interface CaCall : CaLifetimeOwner {
    /**
     * 调用种类。
     */
    val kind: CaCallKind

    /**
     * 调用来源。
     */
    val origin: CaCallOrigin

    /**
     * 当前调用候选的适用性。
     */
    val applicability: CaCallApplicability

    /**
     * 当前调用是否来自隐式 `invoke` 展开。
     */
    val isImplicitInvoke: Boolean

    /**
     * 调用点上参与解析的被调用名。
     *
     * 对于普通调用通常等于源码中的 callee 名；特殊场景下允许为 `null`。
     */
    val calleeName: Name?

    /**
     * 成功解析到的目标可调用符号。
     *
     * 若当前调用尚未绑定到稳定的可调用目标，则返回 `null`。
     */
    val target: CaCallableSymbol?

    /**
     * 源码中显式接收者的类型。
     *
     * 例如 `counter.add(1)` 中返回 `counter` 的类型。
     */
    val explicitReceiverType: CaType?

    /**
     * 解析后选择的 dispatch receiver 类型。
     */
    val dispatchReceiverType: CaType?

    /**
     * 解析后选择的扩展接收者类型。
     */
    val extensionReceiverType: CaType?

    /**
     * 参与解析的上下文参数类型列表。
     */
    val contextArgumentTypes: List<CaType?>

    /**
     * 源码显式实参的解析后类型列表。
     *
     * 与源码参数顺序保持一致；某个实参尚未推导出类型时对应位置为 `null`。
     */
    val argumentTypes: List<CaType?>

    /**
     * 源码显式类型实参的解析后类型列表。
     */
    val typeArguments: List<CaType?>

    /**
     * 源码显式实参与形参的映射结果。
     *
     * 该列表与 [argumentTypes] 使用同一实参顺序。
     */
    val argumentMapping: List<CaCallArgumentMapping>

    /**
     * 源码层显式提供的类型参数个数。
     */
    val typeArgumentCount: Int
        get() = typeArguments.size
}

/**
 * 读取当前解析结果最终选中的可调用目标。
 */
val CaCallInfo.target: CaCallableSymbol?
    get() = successfulCall?.target

/**
 * 当且仅当调用集合中存在唯一调用时返回该调用。
 */
fun CaCallInfo.singleCallOrNull(): CaCall? = calls.singleOrNull()

/**
 * 读取当前唯一的成功函数调用。
 */
fun CaCallInfo.successfulFunctionCallOrNull(): CaCall? =
    successfulCall?.takeIf { it.kind == CaCallKind.FUNCTION }

/**
 * 判断适用性是否表示“无错误成功”。
 *
 * 注意：`RESOLVED_WITH_ERROR` 虽然代表 resolver 已经选中了候选，
 * 但它仍然表示带错误的结果，因此这里返回 `false`。
 */
val CaCallApplicability.isSuccess: Boolean
    get() = this >= CaCallApplicability.RESOLVED_LOW_PRIORITY &&
        this != CaCallApplicability.RESOLVED_WITH_ERROR

/**
 * 判断调用是否属于无错误成功结果。
 */
val CaCall.isSuccessful: Boolean
    get() = applicability.isSuccess
