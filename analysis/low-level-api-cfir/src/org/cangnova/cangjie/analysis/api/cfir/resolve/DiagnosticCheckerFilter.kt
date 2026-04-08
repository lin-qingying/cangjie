package org.cangnova.cangjie.analysis.api.cfir.resolve

/**
 * 低层诊断检查器过滤器。
 *
 * 它位于低层 facade 一侧，负责描述本次解析要启用哪些检查器分组。
 * 当前仓库只有 common checkers 真正接入，因此 extra/experimental 先保留语义位，
 * 便于后续在不破坏 API 的前提下补齐完整矩阵。
 */
data class DiagnosticCheckerFilter(
    val runDefaultCheckers: Boolean,
    val runExtraCheckers: Boolean,
    val runExperimentalCheckers: Boolean,
) {
    /**
     * 组合两组低层诊断检查器配置。
     *
     * 组合语义属于诊断过滤值对象本身，而不是调用侧的临时规则。
     * 这样 Analysis API、low-level facade 和后续平台实现都可以稳定复用同一套“并集”语义。
     */
    operator fun plus(other: DiagnosticCheckerFilter): DiagnosticCheckerFilter =
        DiagnosticCheckerFilter(
            runDefaultCheckers = runDefaultCheckers || other.runDefaultCheckers,
            runExtraCheckers = runExtraCheckers || other.runExtraCheckers,
            runExperimentalCheckers = runExperimentalCheckers || other.runExperimentalCheckers,
        )

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
