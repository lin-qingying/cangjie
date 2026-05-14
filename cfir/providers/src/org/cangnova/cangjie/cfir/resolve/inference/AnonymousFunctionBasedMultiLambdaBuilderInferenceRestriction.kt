package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.resolve.calls.inference.model.MultiLambdaBuilderInferenceRestriction
import org.cangnova.cangjie.type.model.TypeParameterMarker

/**
 * 对齐 Kotlin FIR 的匿名函数专用 builder inference restriction transport type。
 */
class AnonymousFunctionBasedMultiLambdaBuilderInferenceRestriction(
    anonymous: CfirAnonymousFunction,
    typeParameter: TypeParameterMarker,
) : MultiLambdaBuilderInferenceRestriction<CfirAnonymousFunction>(anonymous, typeParameter)
