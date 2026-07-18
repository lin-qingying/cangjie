package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.inference.InferenceComponents
import org.cangnova.cangjie.cfir.resolve.inference.inferenceComponents
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeInferenceContext
import org.cangnova.cangjie.cfir.types.typeContext

/** 候选阶段在一次解析中的执行边界。 */
internal enum class CandidateProcessingMode {
    /** 执行调用种类声明的完整候选阶段序列。 */
    FULL,

    /** 只执行到参数形态映射，用于在实参值解析前识别确定的绑定错误。 */
    ARGUMENT_SHAPE,
}

/** 单次调用解析使用的上下文对象。 */
class ResolutionContext(
    /** 当前 CFIR session。 */
    override val session: CfirSession,
    /** body resolve 共享组件。 */
    val bodyResolveComponents: BodyResolveComponents,
    /** 当前 body resolve 语句/声明上下文。 */
    val bodyResolveContext: BodyResolveContext,
) : SessionHolder {
    /** 当前解析使用的候选阶段边界。 */
    internal var candidateProcessingMode: CandidateProcessingMode = CandidateProcessingMode.FULL
        private set

    /** 当前 session 的类型系统上下文。 */
    val typeContext: ConeInferenceContext
        get() = session.typeContext

    /** 当前 session 的类型推断组件集合。 */
    val inferenceComponents: InferenceComponents
        get() = session.inferenceComponents

    /** 当前 body resolve 使用的返回类型计算器。 */
    val returnTypeCalculator: ReturnTypeCalculator
        get() = bodyResolveComponents.returnTypeCalculator

    /** 在指定候选阶段边界内执行解析，并在退出时恢复原模式。 */
    internal inline fun <T> withCandidateProcessingMode(
        mode: CandidateProcessingMode,
        action: () -> T,
    ): T {
        val previousMode = candidateProcessingMode
        candidateProcessingMode = mode
        return try {
            action()
        } finally {
            candidateProcessingMode = previousMode
        }
    }
}
