package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.types.ConeCangJieType

internal class CallableReferenceAdaptation(
    val argumentTypes: List<ConeCangJieType> = emptyList(),
    val defaults: Int,
    val conversionStrategy: CallableReferenceConversionStrategy = CallableReferenceConversionStrategy.NoConversion,
) {
    init {
        require(defaults >= 0) { "defaults must be non-negative" }
        require(defaults != 0 || hasFunctionKindConversion() || argumentTypes.isNotEmpty()) {
            "Adaptation must be non-trivial."
        }
    }

    fun hasFunctionKindConversion(): Boolean {
        return conversionStrategy != CallableReferenceConversionStrategy.NoConversion
    }
}

sealed class CallableReferenceConversionStrategy {
    data object NoConversion : CallableReferenceConversionStrategy()

    data object FunctionKindConversion : CallableReferenceConversionStrategy()
}
