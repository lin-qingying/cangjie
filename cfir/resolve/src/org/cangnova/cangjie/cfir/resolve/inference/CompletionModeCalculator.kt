package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode

fun Candidate.computeCompletionMode(
    components: InferenceComponents,
    resolutionMode: ResolutionMode,
    currentReturnType: ConeCangJieType?,
): ConstraintSystemCompletionMode {
    return when {
        resolutionMode.forceFullCompletion -> ConstraintSystemCompletionMode.FULL
        callInfo.isCollectionLiteralCall -> error("Should not run completion for collection literal")
        currentReturnType == null -> ConstraintSystemCompletionMode.PARTIAL
        system.getBuilder().isProperType(currentReturnType) -> ConstraintSystemCompletionMode.FULL
        postponedAtoms.any { it is org.cangnova.cangjie.cfir.resolve.calls.ConeFunctionTypeRelatedPostponedResolvedAtom } ->
            ConstraintSystemCompletionMode.FULL
        else -> ConstraintSystemCompletionMode.PARTIAL
    }
}
