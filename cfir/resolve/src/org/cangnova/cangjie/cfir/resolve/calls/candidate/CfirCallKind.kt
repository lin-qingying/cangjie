package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionStage

/**
 * 调用种类，决定候选验证管线需要执行哪些阶段。
 * 每种调用都会携带一组 [resolutionSequence]，由解析管线按顺序执行。
 * 对齐 K2 `CallKind`，但简化为 3 类，去掉 DelegatingConstructorCall / CustomForIde 等分支。
 */
sealed class CfirCallKind {

    /** 当前调用种类所需的验证阶段序列。 */
    abstract val resolutionSequence: List<CfirResolutionStage>

    /** 函数调用。 */
    class Function(
        override val resolutionSequence: List<CfirResolutionStage>,
    ) : CfirCallKind()

    /** 变量或属性访问。 */
    class VariableAccess(
        override val resolutionSequence: List<CfirResolutionStage>,
    ) : CfirCallKind()

    /** 构造器调用。 */
    class ConstructorCall(
        override val resolutionSequence: List<CfirResolutionStage>,
    ) : CfirCallKind()
}

