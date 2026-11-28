package org.cangnova.cangjie.analysis.api.completion

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.psi.CjElement

/**
 * extend 成员对某个接收者的适用性结论。
 *
 * 与 [CaCompletionCandidateStatus] 的三态可达性**不是超集关系**：
 * 三态回答“这个名字在使用点能不能直接用/需要导入/不可见”，
 * 本结论回答“这个 extend 在这个具体 receiver 上是否成立”，两者必须分别判断。
 * 一个候选可以完全可见但不适用于当前 receiver（例如泛型约束不满足），
 * 也可以适用于 receiver 但仍需导入。
 */
@CaExperimentalApi
sealed interface CaExtensionApplicabilityResult : CaLifetimeOwner {
    /**
     * extend 对当前接收者成立；[substitutor] 给出 extend 类型参数到实际类型的替换。
     *
     * 该替换已按 extend 声明的 where 约束校验，可直接用于渲染替换后的成员签名。
     */
    @CaExperimentalApi
    data class Applicable(
        /**
         * extend 类型参数到实际类型的替换器。
         */
        val substitutor: CaSubstitutor,
    ) : CaExtensionApplicabilityResult {
        /**
         * 本结论跟随所属会话的生命周期。
         */
        override val token: CaLifetimeToken get() = substitutor.token
    }

    /**
     * extend 与接收者类型不匹配：目标类型模式无法被当前接收者约束。
     *
     * 这是确定的否定结论，可用于直接过滤候选。
     */
    @CaExperimentalApi
    data class ReceiverMismatch(
        override val token: CaLifetimeToken,
    ) : CaExtensionApplicabilityResult

    /**
     * 目标类型可匹配，但 extend 自身的泛型/where 约束在当前 receiver 上不成立。
     */
    @CaExperimentalApi
    data class ConstraintUnsatisfied(
        override val token: CaLifetimeToken,
    ) : CaExtensionApplicabilityResult

    /**
     * 当前信息不足以判定：接收者类型未确定、extend 尚未解析或处于受限分析状态。
     *
     * 该结论**不等于不适用**：消费方不得据此过滤候选，否则会把“可判定为适用”的
     * extend 一起丢掉。
     */
    @CaExperimentalApi
    data class Unknown(
        override val token: CaLifetimeToken,
    ) : CaExtensionApplicabilityResult

    /**
     * 是否已确定该 extend 对当前接收者成立。
     */
    val isApplicable: Boolean
        get() = this is Applicable
}

/**
 * 补全场景下的 extend 适用性检查。
 *
 * 对齐 Kotlin `KaCompletionCandidateChecker.createExtensionCandidateChecker`：
 * 形状上绑定原文件、使用点位置与接收者，结果同时表达适用性与所需类型替换。
 *
 * 与 Kotlin 的偏差（仓颉语义）：
 * - Kotlin 的 extension function 在仓颉对应 `extend` 声明，其接收者是 extend 目标类型，
 *   适用性由目标类型匹配加 extend 自身约束共同决定；
 * - 完全实例化 extend 与带泛型/where 的 extend 走同一条判定路径，只在约束校验处分叉；
 * - 成员来源、跨包导出与 private extend 的可见性由访问组件另行判断，本接口不产出可见性结论。
 */
@CaExperimentalApi
interface CaCompletionExtensionCandidateChecker : CaLifetimeOwner {
    /**
     * 判断 [extendSymbol] 对 [receiver] 是否适用。
     *
     * [position] 是使用点；它决定 extend 的可见范围，而不是重新选择目标类型。
     * 无法判定时必须返回 [CaExtensionApplicabilityResult.Unknown]，不得返回否定结论。
     */
    fun checkExtensionApplicability(
        extendSymbol: CaSymbol,
        receiver: CaType,
        position: CjElement,
    ): CaExtensionApplicabilityResult
}