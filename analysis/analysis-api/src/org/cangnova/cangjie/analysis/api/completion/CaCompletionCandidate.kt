package org.cangnova.cangjie.analysis.api.completion

/**
 * Analysis API 对补全候选的稳定语义判断结果。
 */
enum class CaCompletionCandidateStatus {
    DIRECT,
    REQUIRES_IMPORT,
    HIDDEN,
}
