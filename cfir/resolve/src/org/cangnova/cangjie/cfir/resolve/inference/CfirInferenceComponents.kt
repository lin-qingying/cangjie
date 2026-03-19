package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker

/**
 * Factory for inference components used in call resolution.
 */
class CfirInferenceComponents(
    val subtypeChecker: ConeSubtypeChecker,
    val inferenceLogger: FirInferenceLogger? = null,
) {
    fun createConstraintSystem(): CfirConstraintSystemImpl = CfirConstraintSystemImpl(
        subtypeChecker = subtypeChecker,
        inferenceLogger = inferenceLogger,
    )
}


