package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.types.ConeCangJieType

interface PostponedResolvedAtomMarker {
    /**
     * Types that should become proper before atom analysis can proceed (outside PCLA).
     */
    val inputTypes: Collection<ConeCangJieType>

    /**
     * Type that may be refined after analyzing this atom.
     */
    val outputType: ConeCangJieType?

    val expectedType: ConeCangJieType?
    val analyzed: Boolean
}

interface CollectionLiteralAtomMarker : PostponedResolvedAtomMarker

interface PostponedAtomWithRevisableExpectedType : PostponedResolvedAtomMarker {
    val revisedExpectedType: ConeCangJieType?

    fun reviseExpectedType(expectedType: ConeCangJieType)
}

interface PostponedCallableReferenceMarker : PostponedAtomWithRevisableExpectedType {
    val needsResolution get() = !analyzed
}

interface LambdaWithTypeVariableAsExpectedTypeMarker : PostponedAtomWithRevisableExpectedType {
    val parameterTypesFromDeclaration: List<ConeCangJieType?>?

    fun updateParameterTypesFromDeclaration(types: List<ConeCangJieType?>?)
}
