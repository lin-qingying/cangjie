package org.cangnova.cangjie.analysis.api.cfir.resolve

/**
 * 底层诊断检查器过滤器。
 *
 * 对齐 Kotlin `DiagnosticCheckerFilter`，但当前仓颉 CFIR 侧还没有把 extra / experimental checker
 * 做成独立集合，因此这里只保留统一的过滤协议，供 Analysis API surface 与低层 facade 传递语义。
 */
data class DiagnosticCheckerFilter(
    val runDefaultCheckers: Boolean,
    val runExtraCheckers: Boolean,
    val runExperimentalCheckers: Boolean,
) {
    val includesDefaultCheckers: Boolean
        get() = runDefaultCheckers

    companion object {
        val ONLY_DEFAULT_CHECKERS = DiagnosticCheckerFilter(
            runDefaultCheckers = true,
            runExtraCheckers = false,
            runExperimentalCheckers = false,
        )

        val ONLY_EXTRA_CHECKERS = DiagnosticCheckerFilter(
            runDefaultCheckers = false,
            runExtraCheckers = true,
            runExperimentalCheckers = false,
        )

        val ONLY_EXPERIMENTAL_CHECKERS = DiagnosticCheckerFilter(
            runDefaultCheckers = false,
            runExtraCheckers = false,
            runExperimentalCheckers = true,
        )
    }
}

operator fun DiagnosticCheckerFilter.plus(other: DiagnosticCheckerFilter): DiagnosticCheckerFilter {
    return DiagnosticCheckerFilter(
        runDefaultCheckers = runDefaultCheckers || other.runDefaultCheckers,
        runExtraCheckers = runExtraCheckers || other.runExtraCheckers,
        runExperimentalCheckers = runExperimentalCheckers || other.runExperimentalCheckers,
    )
}
