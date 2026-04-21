package org.cangnova.cangjie.cfir.analysis.tests.services

import org.cangnova.cangjie.cfir.analysis.tests.golden.CjcDiag
import org.cangnova.cangjie.cfir.analysis.tests.golden.CjcDiagnosticJsonParser
import org.cangnova.cangjie.cfir.analysis.tests.golden.CjcProcessRunner
import org.cangnova.cangjie.cfir.analysis.tests.golden.DiagnosticNameMapper
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.session.diagnosticCollector
import org.cangnova.cangjie.test.directives.CangjieTestDirectives.NO_PRELUDE
import org.cangnova.cangjie.test.WrappedException
import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.artifactsProvider
import org.cangnova.cangjie.test.services.moduleStructure
import org.cangnova.cangjie.test.services.sourceFileProvider
import java.io.File
import java.nio.file.Path
import kotlin.math.min

/**
 * LLT 场景下的 CFIR vs CJC 诊断一致性检查器。
 *
 * 约束策略（与需求保持一致）：
 * 1. 只比较 ERROR 级别诊断；
 * 2. 名称比较走 DiagnosticNameMapper（CFIR -> CJC kind）；
 * 3. 映射缺失（CFIR/CJC 任一侧）直接判失败；
 * 4. 主比对键：mappedKind + file + start(line/column)；
 * 5. end(line/column) 作为位置一致性附加检查（positionMismatch）。
 */
class CfirCjcLlTDiagnosticsChecker(
    testServices: TestServices,
) : AfterAnalysisChecker(testServices) {

    override val order: Order
        get() = Order.P1

    private val cjcPath: Path by lazy { CjcProcessRunner.findCjcPath() }

    override fun check(failedAssertions: List<WrappedException>) {
        check(cjcPath.toFile().exists()) {
            "cjc not found at $cjcPath. Set CANGJIE_HOME or cjc.home."
        }

        val mismatches = buildList {
            for (module in testServices.moduleStructure.modules) {
                val artifact = testServices.artifactsProvider.getArtifactSafe(module, FrontendKinds.CFIR) ?: continue
                for (testFile in module.files) {
                    if (testFile.isAdditional) continue

                    val realFile = testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(testFile)
                    val cfirDiagnostics = collectCfirDiagnostics(artifact, realFile, testFile.originalFile)
                    val cjcDiagnostics = collectCjcDiagnostics(realFile, testFile, module)
                    val report = compareDiagnostics(
                        originalFile = testFile.originalFile,
                        cfirDiagnostics = cfirDiagnostics,
                        cjcDiagnostics = cjcDiagnostics,
                    )
                    if (report.hasIssues()) add(report)
                }
            }
        }

        if (mismatches.isEmpty()) return
        throw AssertionError(renderMismatchReport(mismatches))
    }

    private fun collectCfirDiagnostics(
        artifact: org.cangnova.cangjie.test.frontend.CfirOutputArtifact,
        realFile: File,
        originalFile: File,
    ): List<CjDiagnostic> {
        val normalizedRealPath = normalizePath(realFile.canonicalPath)
        val normalizedOriginalPath = normalizePath(originalFile.canonicalPath)

        return artifact.partsForDependsOnModules
            .flatMap { part ->
                val collector = runCatching { part.session.diagnosticCollector }.getOrNull() ?: return@flatMap emptyList()
                collector.rawDiagnostics
            }
            .filter { diagnostic ->
                if (diagnostic.severity != Severity.ERROR) return@filter false
                val pathFromContext = (diagnostic.context as? DiagnosticContext)
                    ?.containingFilePath
                    ?.let(::normalizePath)
                pathFromContext == null || pathFromContext == normalizedRealPath || pathFromContext == normalizedOriginalPath
            }
    }

    private fun collectCjcDiagnostics(realFile: File, testFile: TestFile, module: TestModule): List<CjcDiag> {
        val result = CjcProcessRunner.compileSingleFile(
            cjcPath = cjcPath,
            sourceFile = realFile,
            noPrelude = NO_PRELUDE in testFile.directives || NO_PRELUDE in module.directives,
        )
        val parsed = parseJsonOutput(result.output)
            ?: error(
                "Failed to parse cjc diagnostic JSON for ${realFile.path}. " +
                    "Output(prefix): ${result.output.take(300)}"
            )

        return parsed.filter { it.severity.equals("error", ignoreCase = true) }
    }

    private fun parseJsonOutput(output: String): List<CjcDiag>? {
        val start = output.indexOf('{')
        if (start < 0) {
            if (output.isBlank()) return emptyList()
            return null
        }

        val candidate = output.substring(start)
        return runCatching { CjcDiagnosticJsonParser.parse(candidate).diags }
            .recoverCatching { CjcDiagnosticJsonParser.parseLenient(candidate).diags }
            .getOrNull()
    }

    private fun compareDiagnostics(
        originalFile: File,
        cfirDiagnostics: List<CjDiagnostic>,
        cjcDiagnostics: List<CjcDiag>,
    ): FileComparisonReport {
        val sourceText = originalFile.readText()
        val defaultFilePath = normalizePath(originalFile.canonicalPath)

        val mappedCfir = mutableListOf<MappedDiagnostic>()
        val mappedCjc = mutableListOf<MappedDiagnostic>()
        val unmapped = mutableListOf<UnmappedDiagnostic>()

        for (diagnostic in cfirDiagnostics) {
            val normalizedName = normalizeProjectDiagnosticName(diagnostic.factoryName)
            val mappedKind = DiagnosticNameMapper.projectToCjcKind(normalizedName)
            val contextFilePath = (diagnostic.context as? DiagnosticContext)?.containingFilePath
            val filePath = contextFilePath?.let(::normalizePath) ?: defaultFilePath
            val start = toLineColumn(sourceText, diagnostic.firstRange.startOffset)
            val end = toLineColumn(sourceText, diagnostic.firstRange.endOffset.coerceAtLeast(diagnostic.firstRange.startOffset))

            if (mappedKind == null) {
                unmapped += UnmappedDiagnostic(
                    source = DiagnosticSource.CFIR,
                    rawName = normalizedName,
                    filePath = filePath,
                    startLine = start.line,
                    startColumn = start.column,
                )
                continue
            }

            mappedCfir += MappedDiagnostic(
                source = DiagnosticSource.CFIR,
                rawName = normalizedName,
                mappedKind = mappedKind,
                filePath = filePath,
                startLine = start.line,
                startColumn = start.column,
                endLine = end.line,
                endColumn = end.column,
            )
        }

        for (diagnostic in cjcDiagnostics) {
            val rawKind = diagnostic.diagKind.trim()
            val projectNames = DiagnosticNameMapper.cjcKindToProjectNames(rawKind)
            val location = diagnostic.location
            val range = diagnostic.mainHint?.range
            val begin = range?.begin
            val end = range?.end
            val filePath = location?.file?.takeIf { it.isNotBlank() }?.let(::normalizePath) ?: defaultFilePath

            val startLine = (begin?.line ?: location?.line ?: 1).coerceAtLeast(1)
            val startColumn = (begin?.column ?: location?.column ?: 1).coerceAtLeast(1)
            val endLine = (end?.line ?: startLine).coerceAtLeast(1)
            val endColumn = (end?.column ?: startColumn).coerceAtLeast(1)

            if (projectNames.isEmpty()) {
                unmapped += UnmappedDiagnostic(
                    source = DiagnosticSource.CJC,
                    rawName = rawKind,
                    filePath = filePath,
                    startLine = startLine,
                    startColumn = startColumn,
                )
                continue
            }

            mappedCjc += MappedDiagnostic(
                source = DiagnosticSource.CJC,
                rawName = rawKind,
                mappedKind = rawKind,
                filePath = filePath,
                startLine = startLine,
                startColumn = startColumn,
                endLine = endLine,
                endColumn = endColumn,
            )
        }

        val missing = mutableListOf<MappedDiagnostic>()
        val unexpected = mutableListOf<MappedDiagnostic>()
        val positionMismatch = mutableListOf<PositionMismatch>()

        val cfirByStart = mappedCfir.groupBy { it.startKey() }
        val cjcByStart = mappedCjc.groupBy { it.startKey() }
        val allKeys = (cfirByStart.keys + cjcByStart.keys).toSortedSet()

        for (key in allKeys) {
            val expected = cfirByStart[key].orEmpty()
                .sortedWith(mappedDiagnosticComparator)
                .toMutableList()
            val actual = cjcByStart[key].orEmpty()
                .sortedWith(mappedDiagnosticComparator)
                .toMutableList()

            val pairedCount = min(expected.size, actual.size)
            repeat(pairedCount) { index ->
                val expectedDiagnostic = expected[index]
                val actualDiagnostic = actual[index]
                if (
                    expectedDiagnostic.endLine != actualDiagnostic.endLine ||
                    expectedDiagnostic.endColumn != actualDiagnostic.endColumn
                ) {
                    positionMismatch += PositionMismatch(expectedDiagnostic, actualDiagnostic)
                }
            }

            if (expected.size > pairedCount) {
                missing += expected.drop(pairedCount)
            }
            if (actual.size > pairedCount) {
                unexpected += actual.drop(pairedCount)
            }
        }

        return FileComparisonReport(
            filePath = normalizePath(originalFile.canonicalPath),
            missing = missing,
            unexpected = unexpected,
            positionMismatch = positionMismatch,
            unmapped = unmapped,
        )
    }

    private fun renderMismatchReport(reports: List<FileComparisonReport>): String = buildString {
        appendLine("CFIR vs CJC diagnostics mismatch in LLT tests: ${reports.size} file(s).")
        for ((index, report) in reports.withIndex()) {
            if (index > 0) appendLine()
            appendLine("File: ${report.filePath}")

            if (report.unmapped.isNotEmpty()) {
                appendLine("  [unmapped] ${report.unmapped.size}")
                report.unmapped
                    .sortedWith(compareBy<UnmappedDiagnostic> { it.source.name }.thenBy { it.startLine }.thenBy { it.startColumn }.thenBy { it.rawName })
                    .forEach { diag ->
                        appendLine("    - ${diag.source.name}.${diag.rawName} @ ${diag.filePath}:${diag.startLine}:${diag.startColumn}")
                    }
            }

            if (report.missing.isNotEmpty()) {
                appendLine("  [missing] ${report.missing.size}")
                report.missing
                    .sortedWith(mappedDiagnosticComparator)
                    .forEach { diag ->
                        appendLine("    - ${diag.mappedKind} (from CFIR:${diag.rawName}) @ ${diag.renderPosition()}")
                    }
            }

            if (report.unexpected.isNotEmpty()) {
                appendLine("  [unexpected] ${report.unexpected.size}")
                report.unexpected
                    .sortedWith(mappedDiagnosticComparator)
                    .forEach { diag ->
                        appendLine("    - ${diag.mappedKind} (from CJC:${diag.rawName}) @ ${diag.renderPosition()}")
                    }
            }

            if (report.positionMismatch.isNotEmpty()) {
                appendLine("  [positionMismatch] ${report.positionMismatch.size}")
                report.positionMismatch
                    .sortedWith(compareBy<PositionMismatch> { it.expected.startLine }.thenBy { it.expected.startColumn }.thenBy { it.expected.mappedKind })
                    .forEach { mismatch ->
                        appendLine(
                            "    - ${mismatch.expected.mappedKind} start ${mismatch.expected.filePath}:" +
                                "${mismatch.expected.startLine}:${mismatch.expected.startColumn} " +
                                "end(expected=${mismatch.expected.endLine}:${mismatch.expected.endColumn}, " +
                                "actual=${mismatch.actual.endLine}:${mismatch.actual.endColumn})"
                        )
                    }
            }
        }
    }.trimEnd()

    private fun normalizeProjectDiagnosticName(name: String): String {
        return name.removePrefix("CFIR_").trim()
    }

    private fun normalizePath(path: String): String {
        return runCatching { File(path).canonicalPath }
            .getOrDefault(path)
            .replace('\\', '/')
    }

    private fun toLineColumn(text: String, offset: Int): LineColumn {
        val safeOffset = offset.coerceIn(0, text.length)
        var line = 1
        var column = 1
        for (i in 0 until safeOffset) {
            if (text[i] == '\n') {
                line++
                column = 1
            } else {
                column++
            }
        }
        return LineColumn(line, column)
    }

    private data class LineColumn(
        val line: Int,
        val column: Int,
    )

    private data class FileComparisonReport(
        val filePath: String,
        val missing: List<MappedDiagnostic>,
        val unexpected: List<MappedDiagnostic>,
        val positionMismatch: List<PositionMismatch>,
        val unmapped: List<UnmappedDiagnostic>,
    ) {
        fun hasIssues(): Boolean {
            return missing.isNotEmpty() ||
                unexpected.isNotEmpty() ||
                positionMismatch.isNotEmpty() ||
                unmapped.isNotEmpty()
        }
    }

    private data class MappedDiagnostic(
        val source: DiagnosticSource,
        val rawName: String,
        val mappedKind: String,
        val filePath: String,
        val startLine: Int,
        val startColumn: Int,
        val endLine: Int,
        val endColumn: Int,
    ) {
        fun startKey(): StartKey {
            return StartKey(
                mappedKind = mappedKind,
                filePath = filePath,
                startLine = startLine,
                startColumn = startColumn,
            )
        }

        fun renderPosition(): String {
            return "$filePath:$startLine:$startColumn..$endLine:$endColumn"
        }
    }

    private data class PositionMismatch(
        val expected: MappedDiagnostic,
        val actual: MappedDiagnostic,
    )

    private data class UnmappedDiagnostic(
        val source: DiagnosticSource,
        val rawName: String,
        val filePath: String,
        val startLine: Int,
        val startColumn: Int,
    )

    private enum class DiagnosticSource {
        CFIR,
        CJC,
    }

    private data class StartKey(
        val mappedKind: String,
        val filePath: String,
        val startLine: Int,
        val startColumn: Int,
    ) : Comparable<StartKey> {
        override fun compareTo(other: StartKey): Int {
            return compareValuesBy(
                this,
                other,
                StartKey::filePath,
                StartKey::startLine,
                StartKey::startColumn,
                StartKey::mappedKind,
            )
        }
    }

    private companion object {
        val mappedDiagnosticComparator: Comparator<MappedDiagnostic> =
            compareBy<MappedDiagnostic> { it.filePath }
                .thenBy { it.startLine }
                .thenBy { it.startColumn }
                .thenBy { it.endLine }
                .thenBy { it.endColumn }
                .thenBy { it.mappedKind }
                .thenBy { it.rawName }
    }
}
