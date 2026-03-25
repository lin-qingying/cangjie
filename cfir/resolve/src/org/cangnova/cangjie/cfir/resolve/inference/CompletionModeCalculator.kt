package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode

fun Candidate.computeCompletionMode(
    components: InferenceComponents,
    resolutionMode: ResolutionMode,
    currentReturnType: ConeCangJieType?
): ConstraintSystemCompletionMode {
    return when {
        // Expected type is present or call is being resolved in independent context
        resolutionMode.forceFullCompletion -> ConstraintSystemCompletionMode.FULL

        callInfo.isCollectionLiteralCall -> {
            error("Should not run completion for collection literal")
        }

        // This is questionable as null return type can be only for error call
        currentReturnType == null -> ConstraintSystemCompletionMode.PARTIAL

        // Full if return type for call has no type variables
        csBuilder.isProperType(currentReturnType) -> ConstraintSystemCompletionMode.FULL

        // Plugins need fully complete calls. Calls that cannot be completed should not be modified, forcing completion will produce type inference error
        currentReturnType.toRegularClassSymbol(components.session)?.fir?.originalCallDataForPluginRefinedCall != null -> ConstraintSystemCompletionMode.FULL

        else -> CalculatorForNestedCall(
            this, currentReturnType, csBuilder, components.trivialConstraintTypeInferenceOracle
        ).computeCompletionMode()
    }
}
