package org.cangnova.cangjie.analysis.api.types

interface CaFunctionType : CaType {
    val parameterTypes: List<CaType>

    val returnType: CaType

    val isCFunction: Boolean

    val isClosureType: Boolean

    val hasVariableLengthArgument: Boolean
}
