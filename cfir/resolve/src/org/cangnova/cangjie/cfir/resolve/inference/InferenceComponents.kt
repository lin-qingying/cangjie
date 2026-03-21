package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker

/**
 * K2/FIR `InferenceComponents` 的 CFIR 对位实现入口。
 */
class InferenceComponents(
    val subtypeChecker: ConeSubtypeChecker,
    val inferenceLogger: FirInferenceLogger? = null,
    val constraintSystemFactory: ConstraintSystemFactory = ConstraintSystemFactory {
        CfirConstraintSystemImpl(
            subtypeChecker = subtypeChecker,
            inferenceLogger = inferenceLogger,
        )
    },
) {
    fun interface ConstraintSystemFactory {
        fun createConstraintSystem(): NewConstraintSystemImpl
    }

    fun createConstraintSystem(): NewConstraintSystemImpl = constraintSystemFactory.createConstraintSystem()
}

typealias CfirInferenceComponents = InferenceComponents

