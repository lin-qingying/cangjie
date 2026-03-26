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
class ResolutionContext(
    override val session: CfirSession,
    val bodyResolveComponents: BodyResolveComponents,
    val bodyResolveContext: BodyResolveContext,
) : SessionHolder {
    val typeContext: ConeInferenceContext
        get() = session.typeContext

    val inferenceComponents: InferenceComponents
        get() = session.inferenceComponents

    val returnTypeCalculator: ReturnTypeCalculator
        get() = bodyResolveComponents.returnTypeCalculator
}
