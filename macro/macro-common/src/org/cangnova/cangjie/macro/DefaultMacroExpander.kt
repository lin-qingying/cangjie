package org.cangnova.cangjie.macro

import org.cangnova.cangjie.cfir.declarations.CfirFile

/**
 * 旧 collect -> execute -> text replacement 宏展开编排器。
 *
 * 该编排器依赖 [MacroReplacer] 将 [MacroExpansionResult.Success.expandedText]
 * 写回源码并重建 CFIR，属于 PLAN Batch 10 明确禁止的旧语义路径。
 * 仅保留源码以便审计旧实现，不允许生产代码继续装配或调用。
 */
@Deprecated(
    message = "Use MacroConstructionService construction flow instead. " +
        "DefaultMacroExpander still orchestrates the forbidden text replacement semantic path.",
    level = DeprecationLevel.ERROR,
)
class DefaultMacroExpander(
    private val collector: MacroCollector,
    private val executor: MacroExecutor,
    private val replacer: MacroReplacer,
) : MacroExpander {
    override fun expandAll(files: List<CfirFile>, maxIterations: Int): MacroExpansionOutput {
        require(maxIterations > 0) { "maxIterations must be greater than 0" }

        if (files.isEmpty()) {
            return MacroExpansionOutput(
                files = files,
                diagnostics = emptyList(),
                expandedCount = 0,
                iterations = 0,
            )
        }

        if (!executor.isAvailable()) {
            return MacroExpansionOutput(
                files = files,
                diagnostics = listOf(
                    MacroDiagnosticInfo(
                        severity = MacroDiagnosticSeverity.ERROR,
                        message = "Macro executor is unavailable",
                    )
                ),
                expandedCount = 0,
                iterations = 0,
            )
        }

        val diagnostics = mutableListOf<MacroDiagnosticInfo>()
        var currentFiles = files
        var expandedCount = 0
        var iterations = 0

        executor.reset()
        try {
            while (iterations < maxIterations) {
                val callSites = collector.collect(currentFiles)
                if (callSites.isEmpty()) {
                    break
                }

                val libPaths = callSites
                    .map { it.callInfo.libPath }
                    .filter { it.isNotBlank() }
                    .distinct()

                val loadLibrariesResult = runCatching {
                    executor.loadLibraries(libPaths)
                }
                if (loadLibrariesResult.isFailure) {
                    val throwable = loadLibrariesResult.exceptionOrNull()
                    diagnostics += MacroDiagnosticInfo(
                        severity = MacroDiagnosticSeverity.ERROR,
                        message = "Failed to load macro libraries: ${throwable?.message ?: throwable?.javaClass?.simpleName ?: "unknown error"}",
                    )
                    break
                }

                val executionResultsResult = runCatching {
                    executor.execute(callSites.map { it.callInfo })
                }
                if (executionResultsResult.isFailure) {
                    val throwable = executionResultsResult.exceptionOrNull()
                    diagnostics += MacroDiagnosticInfo(
                        severity = MacroDiagnosticSeverity.ERROR,
                        message = "Failed to execute macros: ${throwable?.message ?: throwable?.javaClass?.simpleName ?: "unknown error"}",
                    )
                    break
                }
                val executionResults = executionResultsResult.getOrThrow()

                if (executionResults.size != callSites.size) {
                    diagnostics += MacroDiagnosticInfo(
                        severity = MacroDiagnosticSeverity.ERROR,
                        message = "Macro executor returned ${executionResults.size} results for ${callSites.size} calls",
                    )
                    break
                }

                val expansions = buildMap {
                    callSites.zip(executionResults).forEach { (site, result) ->
                        put(site, result)
                    }
                }

                val replacementOutput = replacer.replace(currentFiles, expansions)
                diagnostics += replacementOutput.diagnostics
                currentFiles = replacementOutput.files
                expandedCount += replacementOutput.replacedCount
                iterations++

                if (replacementOutput.replacedCount == 0) {
                    diagnostics += MacroDiagnosticInfo(
                        severity = MacroDiagnosticSeverity.WARNING,
                        message = "Macro expansion stopped because no replacements were applied in iteration $iterations",
                    )
                    break
                }
            }
        } finally {
            executor.reset()
        }

        if (iterations == maxIterations && collector.collect(currentFiles).isNotEmpty()) {
            diagnostics += MacroDiagnosticInfo(
                severity = MacroDiagnosticSeverity.ERROR,
                message = "Macro expansion stopped after reaching the iteration limit of $maxIterations",
            )
        }

        return MacroExpansionOutput(
            files = currentFiles,
            diagnostics = diagnostics,
            expandedCount = expandedCount,
            iterations = iterations,
        )
    }
}
