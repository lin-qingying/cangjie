package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.type.model.CangJieTypeMarker

interface PostponedResolvedAtomMarker {
    /**
     * Types that should become proper before atom analysis can proceed (outside PCLA).
     */
    val inputTypes: Collection<CangJieTypeMarker>

    /**
     * Type that may be refined after analyzing this atom.
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
