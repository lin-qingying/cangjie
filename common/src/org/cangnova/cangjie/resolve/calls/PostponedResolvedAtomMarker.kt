package org.cangnova.cangjie.resolve.calls

import org.cangnova.cangjie.type.model.CangJieTypeMarker

interface PostponedResolvedAtomMarker {
    /**
     * Generally, it's a collection of types that need to be "proper" to start the analysis of the atom (unless PCLA).
     * Used mostly to define if the atom is ready and to define the order among other atoms.
     *
     * Usually, it's just a list of receiver/value parameter types, but might be an expected type variable.
     * (see [org.jetbrains.kotlin.fir.resolve.calls.ConeLambdaWithTypeVariableAsExpectedTypeAtom])
     */
    val inputTypes: Collection<CangJieTypeMarker>

    /**
     * Type that might be refined after analysis of the given atom, i.e., some new type variable constraints found.
     * Currently, used to define dependencies between variables.
     * (see TypeVariableDependencyInformationProvider.computePostponeArgumentsEdges)
     *
     * Usually, it's a return type of lambda/reference.
     * Might be `null` if the return type is unknown or irrelevant.
     */
    val outputType: CangJieTypeMarker?
    val expectedType: CangJieTypeMarker?
    val analyzed: Boolean
}

interface CollectionLiteralAtomMarker : PostponedResolvedAtomMarker

interface PostponedAtomWithRevisableExpectedType : PostponedResolvedAtomMarker {
    val revisedExpectedType: CangJieTypeMarker?

    fun reviseExpectedType(expectedType: CangJieTypeMarker)
}

interface PostponedCallableReferenceMarker : PostponedAtomWithRevisableExpectedType {
    val needsResolution get() = !analyzed
}

interface LambdaWithTypeVariableAsExpectedTypeMarker : PostponedAtomWithRevisableExpectedType {
    val parameterTypesFromDeclaration: List<CangJieTypeMarker?>?

    fun updateParameterTypesFromDeclaration(types: List<CangJieTypeMarker?>?)
}
