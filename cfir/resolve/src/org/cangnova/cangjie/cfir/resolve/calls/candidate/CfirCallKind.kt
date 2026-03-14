package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionStage

/**
 * 调用种类，决定候选验证管线使用哪些阶段。
 *
 * 每种调用种类携带一个验证阶段序列 [resolutionSequence]，
 * [CfirResolutionStageRunner] 按序执行这些阶段。
 *
 * 对齐 K2 CallKind，简化为 3 种（去掉 DelegatingConstructorCall/CustomForIde 等）。
 */
sealed class CfirCallKind {

    /** 该调用种类所需的验证阶段序列 */
    abstract val resolutionSequence: List<CfirResolutionStage>

    /** 函数调用 */
    class Function(
        override val resolutionSequence: List<CfirResolutionStage>,
    ) : CfirCallKind()

    /** 变量/属性访问 */
    class VariableAccess(
        override val resolutionSequence: List<CfirResolutionStage>,
    ) : CfirCallKind()

    /** 构造器调用 */
    class ConstructorCall(
        override val resolutionSequence: List<CfirResolutionStage>,
    ) : CfirCallKind()
}
