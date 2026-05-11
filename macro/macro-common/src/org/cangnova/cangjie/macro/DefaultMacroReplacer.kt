package org.cangnova.cangjie.macro

import org.cangnova.cangjie.cfir.declarations.CfirFile

/**
 * 旧 text-patch + 全文件重建路径（baseline 第 2 节硬性边界 #8 列出的禁止形态）。
 *
 * 仅作为 Batch 10 过渡期的兼容实现保留：
 * - 不进入 baseline `MacroConstructionService` 主流程；
 * - 任何新引用都视为对硬性边界的违反；
 * - 真实 splice 将在 Batch 8 fragment parser 接通后由
 *   `org.cangnova.cangjie.cfir.resolve.providers.macro.MacroStableSplicer`
 *   实现替代。
 */
@Deprecated(
    message = "Use MacroConstructionService + MacroStableSplicer instead. " +
        "Text-patch + full-file rebuild is forbidden as a semantic path " +
        "by macro construction baseline §2 / §9.",
    level = DeprecationLevel.WARNING,
)
class DefaultMacroReplacer(
    private val fileRebuilder: MacroFileRebuilder,
) : MacroReplacer {
    override fun replace(
        files: List<CfirFile>,
        expansions: Map<MacroCallSite, MacroExpansionResult>,
    ): MacroReplacementOutput {
        if (expansions.isEmpty()) {
            return MacroReplacementOutput(
                files = files,
                diagnostics = emptyList(),
                replacedCount = 0,
            )
        }

        val diagnostics = mutableListOf<MacroDiagnosticInfo>()
        val expansionsByFile = expansions.entries.groupBy { it.key.file }
        val replacedFiles = ArrayList<CfirFile>(files.size)
        var replacedCount = 0

        for (file in files) {
            val fileExpansions = expansionsByFile[file].orEmpty()
            if (fileExpansions.isEmpty()) {
                replacedFiles += file
                continue
            }

            val originalText = file.readSourceText()
            if (originalText == null) {
                diagnostics += MacroDiagnosticInfo(
                    severity = MacroDiagnosticSeverity.ERROR,
                    message = "Macro expansion skipped because source text is unavailable for ${file.name}",
                )
                diagnostics += file.collectResultDiagnostics(fileExpansions)
                replacedFiles += file
                continue
            }

            val successfulExpansions = fileExpansions.mapNotNull { entry ->
                when (val result = entry.value) {
                    is MacroExpansionResult.Success -> entry.key to result
                    is MacroExpansionResult.Failure -> {
                        diagnostics += result.toDiagnostic(file.name)
                        diagnostics += result.diagnostics
                        null
                    }
                }
            }

            if (successfulExpansions.isEmpty()) {
                replacedFiles += file
                continue
            }

            val patchedText = applyExpansions(
                originalText = originalText,
                fileName = file.name,
                expansions = successfulExpansions,
                diagnostics = diagnostics,
            )
            if (patchedText == null) {
                replacedFiles += file
                continue
            }

            val rebuiltFileResult = runCatching {
                fileRebuilder.rebuild(file, patchedText)
            }
            val rebuiltFile = rebuiltFileResult.getOrElse { throwable ->
                diagnostics += MacroDiagnosticInfo(
                    severity = MacroDiagnosticSeverity.ERROR,
                    message = "Failed to rebuild ${file.name} after macro expansion: ${throwable.message ?: throwable::class.simpleName}",
                )
                file
            }

            if (rebuiltFileResult.isSuccess) {
                replacedCount += successfulExpansions.size
            }
            successfulExpansions.forEach { (_, result) -> diagnostics += result.diagnostics }
            replacedFiles += rebuiltFile
        }

        return MacroReplacementOutput(
            files = replacedFiles,
            diagnostics = diagnostics,
            replacedCount = replacedCount,
        )
    }

    private fun applyExpansions(
        originalText: String,
        fileName: String,
        expansions: List<Pair<MacroCallSite, MacroExpansionResult.Success>>,
        diagnostics: MutableList<MacroDiagnosticInfo>,
    ): String? {
        val sortedExpansions = expansions
            .mapNotNull { (site, result) ->
                val source = site.expression.source
                if (source == null) {
                    diagnostics += MacroDiagnosticInfo(
                        severity = MacroDiagnosticSeverity.ERROR,
                        message = "Macro expansion skipped in $fileName because source range is unavailable",
                    )
                    null
                } else {
                    ExpansionPatch(
                        startOffset = source.startOffset,
                        endOffset = source.endOffset,
                        replacement = result.expandedText,
                    )
                }
            }
            .sortedByDescending { it.startOffset }

        if (sortedExpansions.isEmpty()) {
            return null
        }

        var previousStart = originalText.length
        val builder = StringBuilder(originalText)
        for (patch in sortedExpansions) {
            if (patch.startOffset < 0 || patch.endOffset > originalText.length || patch.startOffset > patch.endOffset) {
                diagnostics += MacroDiagnosticInfo(
                    severity = MacroDiagnosticSeverity.ERROR,
                    message = "Macro expansion skipped in $fileName because source range ${patch.startOffset}..${patch.endOffset} is invalid",
                )
                return null
            }
            if (patch.endOffset > previousStart) {
                diagnostics += MacroDiagnosticInfo(
                    severity = MacroDiagnosticSeverity.ERROR,
                    message = "Macro expansion skipped in $fileName because macro ranges overlap",
                )
                return null
            }
            builder.replace(patch.startOffset, patch.endOffset, patch.replacement)
            previousStart = patch.startOffset
        }
        return builder.toString()
    }

    private fun CfirFile.readSourceText(): String? {
        val sourceFile = sourceFile ?: return null
        return sourceFile.getContentsAsStream()
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }

    private fun CfirFile.collectResultDiagnostics(
        expansions: List<Map.Entry<MacroCallSite, MacroExpansionResult>>,
    ): List<MacroDiagnosticInfo> {
        return expansions.flatMap { entry ->
            when (val result = entry.value) {
                is MacroExpansionResult.Success -> result.diagnostics
                is MacroExpansionResult.Failure -> listOf(result.toDiagnostic(name)) + result.diagnostics
            }
        }
    }

    private fun MacroExpansionResult.Failure.toDiagnostic(fileName: String): MacroDiagnosticInfo {
        return MacroDiagnosticInfo(
            severity = MacroDiagnosticSeverity.ERROR,
            message = "Macro expansion failed in $fileName: $message",
        )
    }

    private data class ExpansionPatch(
        val startOffset: Int,
        val endOffset: Int,
        val replacement: String,
    )
}
