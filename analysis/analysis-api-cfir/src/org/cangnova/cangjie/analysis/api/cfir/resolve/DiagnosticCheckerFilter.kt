package org.cangnova.cangjie.analysis.api.cfir.resolve

/**
 * 低级别诊断检查器过滤器（对齐 K2 的 DiagnosticCheckerFilter）。
 *
 * 控制哪些类别的检查器参与诊断收集。
 */
data class DiagnosticCheckerFilter(
    val runDefaultCheckers: Boolean,
    val runExtraCheckers: Boolean,
    val runExperimentalCheckers: Boolean,
) {
    companion object {
        val ONLY_DEFAULT_CHECKERS = DiagnosticCheckerFilter(
            runDefaultCheckers = true, runExtraCheckers = false, runExperimentalCheckers = false,
        )
        val ONLY_EXTRA_CHECKERS = DiagnosticCheckerFilter(
            runDefaultCheckers = false, runExtraCheckers = true, runExperimentalCheckers = false,
        )
        val ONLY_EXPERIMENTAL_CHECKERS = DiagnosticCheckerFilter(
            runDefaultCheckers = false, runExtraCheckers = false, runExperimentalCheckers = true,
        )
    }
}

operator fun DiagnosticCheckerFilter.plus(other: DiagnosticCheckerFilter) =
    DiagnosticCheckerFilter(
        runDefaultCheckers || other.runDefaultCheckers,
        runExtraCheckers || other.runExtraCheckers,
        runExperimentalCheckers || other.runExperimentalCheckers,
    )
