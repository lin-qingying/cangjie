package org.cangnova.cangjie.analysis.api.components

/**
 * [CaDiagnosticCheckerFilter] controls which kinds of diagnostics are included in the result of diagnostic collection.
 */
enum class CaDiagnosticCheckerFilter {
    /**
     * Includes diagnostics only from the compiler's common checkers.
     */
    ONLY_COMMON_CHECKERS,

    /**
     * Includes diagnostics from extended checkers (that typically run only in the IDE).
     */
    ONLY_EXTENDED_CHECKERS,

    /**
     * Includes diagnostics from experimental checkers.
     *
     * Their role is to be run in the IDE similar to [ONLY_EXTENDED_CHECKERS] with the following differences:
     * * They might have false positives
     * * They might be slow
     */
    ONLY_EXPERIMENTAL_CHECKERS,

    /**
     * Includes diagnostics from both common and extended checkers.
     */
    EXTENDED_AND_COMMON_CHECKERS,
}
