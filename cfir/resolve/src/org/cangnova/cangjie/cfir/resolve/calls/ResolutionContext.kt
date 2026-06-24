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
/** 单次调用解析使用的上下文对象。 */
class ResolutionContext(
    /** 当前 CFIR session。 */
    override val session: CfirSession,
    /** body resolve 共享组件。 */
    val bodyResolveComponents: BodyResolveComponents,
    /** 当前 body resolve 语句/声明上下文。 */
    val bodyResolveContext: BodyResolveContext,
) : SessionHolder {
    /** 当前 session 的类型系统上下文。 */
    val typeContext: ConeInferenceContext
        get() = session.typeContext

    /** 当前 session 的类型推断组件集合。 */
    val inferenceComponents: InferenceComponents
        get() = session.inferenceComponents

    /** 当前 body resolve 使用的返回类型计算器。 */
    val returnTypeCalculator: ReturnTypeCalculator
        get() = bodyResolveComponents.returnTypeCalculator
}
