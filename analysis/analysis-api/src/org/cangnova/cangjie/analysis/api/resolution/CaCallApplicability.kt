package org.cangnova.cangjie.analysis.api.resolution

enum class CaCallApplicability {
    HIDDEN,
    INAPPLICABLE_WRONG_RECEIVER,
    INAPPLICABLE_ARGUMENTS_MAPPING_ERROR,
    INAPPLICABLE,
    VISIBILITY_ERROR,
    UNSAFE_CALL,
    UNSTABLE_SMARTCAST,
    CONVENTION_ERROR,
    RESOLVED_LOW_PRIORITY,
    RESOLVED_NEED_PRESERVE_COMPATIBILITY,
    RESOLVED_WITH_ERROR,
    RESOLVED,
}

val CaCallApplicability.isSuccess: Boolean
    get() = this >= CaCallApplicability.RESOLVED_LOW_PRIORITY &&
        this != CaCallApplicability.RESOLVED_WITH_ERROR
