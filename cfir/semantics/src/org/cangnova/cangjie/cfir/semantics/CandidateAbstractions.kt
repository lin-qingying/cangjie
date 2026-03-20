package org.cangnova.cangjie.cfir.semantics




/**
 * Candidate applicability ordered from worst to best.
 */
enum class CandidateApplicability {
    HIDDEN,
    INAPPLICABLE_WRONG_RECEIVER,
    INAPPLICABLE_ARGUMENTS_MAPPING_ERROR,
    INAPPLICABLE,
    RESOLVED_LOW_PRIORITY,
    RESOLVED_WITH_ERROR,
    RESOLVED,
    ;

    val isSuccess: Boolean
        get() = this >= RESOLVED_LOW_PRIORITY
}



