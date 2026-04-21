package org.cangnova.cangjie.cfir.resolve.match.exhaustive

import org.cangnova.cangjie.cfir.resolve.match.CfirMatchPattern

sealed class ExhaustivenessResult {
    data object Exhaustive : ExhaustivenessResult()

    data class NonExhaustive(
        val missingPatterns: List<CfirMatchPattern>,
        val source: CheckSource = CheckSource.UNKNOWN,
    ) : ExhaustivenessResult() {
        fun getMissingPatternTexts(): List<String> = missingPatterns.map { it.text() }
    }

    data class Error(val reason: String) : ExhaustivenessResult()
    data object Skipped : ExhaustivenessResult()

    val isExhaustive: Boolean
        get() = this is Exhaustive
    val isNonExhaustive: Boolean
        get() = this is NonExhaustive
}

enum class CheckSource {
    UNKNOWN,
    TRIVIAL,
    BOOLEAN_FLAG,
    ENUM_BITVECTOR,
    INTEGER_INTERVAL,
    CHAR_INTERVAL,
    TUPLE_COMPONENT,
    NESTED_FLATTEN,
    MARANGET,
}
