package org.cangnova.cangjie.cfir.semantics

enum class CandidateApplicability {
    HIDDEN,
    /**
     * Candidate could be successful but arguments have wrong types (or other general inapplicability)
     */
    INAPPLICABLE,
    INAPPLICABLE_ARGUMENTS_MAPPING_ERROR,
    /**
     * Candidate could be successful but receiver (or argument?) nullability doesn't match
     */
    UNSAFE_CALL,

    /**
     * Candidate could be successful but does not obey conventions.
     * E.g. infix / operator / etc are missed (= no expected modifier).
     */
    CONVENTION_ERROR,
    /**
     * Candidate is successful but has low priority.
     * Tower resolve proceeds to next levels.
     */
    RESOLVED_LOW_PRIORITY,


    /**
     * Candidate has some error, but it is still successful from resolution perspective.
     * This means that tower resolve stops with this applicability.
     * However, error will be reported.
     * This is the only applicability that stops resolve but provokes an error.
     */
    RESOLVED_WITH_ERROR,

    /**
     * Candidate is successful or has uncompleted inference (so possibly successful).
     */
    RESOLVED,
}

val CandidateApplicability.isSuccess: Boolean
    get() = this >=  CandidateApplicability.RESOLVED_LOW_PRIORITY && this !=  CandidateApplicability.RESOLVED_WITH_ERROR

val CandidateApplicability.shouldStopResolve: Boolean
    get() = this >= CandidateApplicability.RESOLVED_LOW_PRIORITY
