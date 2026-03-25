package org.cangnova.cangjie.cfir.resolve.calls.candidate
import  org.cangnova.cangjie.cfir.resolve.calls.stages.   ResolutionStage
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckArguments
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckVisibility
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCreateFreshTypeVariableSubstitutorStage
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirInferTypeArguments
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirMapArguments
/**
 * 调用种类，决定候选验证管线需要执行哪些阶段。
 * 每种调用都会携带一组 [resolutionSequence]，由解析管线按顺序执行。
 *
 * 对齐 K2 `CallKind`：使用 object 单例模式，阶段序列在定义时固定。
 * 简化为 3 类，去掉 DelegatingConstructorCall / CustomForIde 等分支。
 */
sealed class  CallKind(
    vararg val resolutionSequence:  ResolutionStage,
    additionalStages: Array<ResolutionStage> = emptyArray(),

) {
    val resolutionSequenceWithAdditionalStages: Array<out ResolutionStage> = arrayOf(*resolutionSequence, *additionalStages)

    /**
     * 函数调用。
     * 对齐 K2 `CallKind.Function`。
     */
    data object Function : CallKind(
        CfirCheckVisibility,
        CfirCreateFreshTypeVariableSubstitutorStage,
        CfirMapArguments,
        CfirCheckArguments,
        CfirInferTypeArguments,
    )

    /**
     * 变量或属性访问（无参数调用，不含枚举构造器）。
     * 对齐 K2 `CallKind.VariableAccess`。
     */
    data object VariableAccess : CallKind(
        CfirCheckVisibility,
        CfirCreateFreshTypeVariableSubstitutorStage,
        CfirMapArguments,
    )


    /**
     * 枚举构造器调用。
     * 当函数/变量解析失败后，回退尝试枚举构造器解析时使用。
     * 阶段序列与 [Function] 相同，但语义上独立，便于后续扩展枚举特有阶段。
     */
    data object EnumConstructorCall : CallKind(
        CfirCheckVisibility,
        CfirCreateFreshTypeVariableSubstitutorStage,
        CfirMapArguments,
        CfirCheckArguments,
        CfirInferTypeArguments,
    )
}
