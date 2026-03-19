package org.cangnova.cangjie.cfir.analysis.tests.services

import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.session.diagnosticCollector
import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.services.artifactsProvider
import org.cangnova.cangjie.test.services.moduleStructure
import org.cangnova.cangjie.test.services.sourceFileProvider
import org.cangnova.cangjie.test.services.TestServices
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import java.io.File
import java.nio.charset.StandardCharsets

class CfirInlineDiagnosticsChecker(
    testServices: TestServices,
) : AfterAnalysisChecker(testServices) {
    override val order: Order
        get() = Order.P1

    override fun check(failedAssertions: List<org.cangnova.cangjie.test.WrappedException>) {
        val mismatches = buildList {
            for (module in testServices.moduleStructure.modules) {
                val artifact = testServices.artifactsProvider.getArtifactSafe(module, FrontendKinds.CFIR) ?: continue
                for (file in module.files) {
                    if (file.isAdditional) continue

                    val actualDiagnostics = diagnosticsForFile(artifact, file)
                    val mismatch = CfirInlineDiagnosticsDiff.compare(
                        filePath = file.originalFile.canonicalPath,
                        originalText = file.originalContent,
                        actualDiagnostics = actualDiagnostics,
                    )
                    if (mismatch != null) {
                        add(mismatch)
                    }
                }
            }
        }

        if (mismatches.isEmpty()) return
        throw StructuredInlineDiagnosticsAssertionError(mismatches)
    }

    private fun diagnosticsForFile(
        artifact: org.cangnova.cangjie.test.frontend.CfirOutputArtifact,
        file: org.cangnova.cangjie.test.model.TestFile,
    ): List<org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic> {
        val normalizedRealPath = normalizePath(testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(file).canonicalPath)
        val normalizedOriginalPath = normalizePath(file.originalFile.canonicalPath)

        return artifact.partsForDependsOnModules
            .flatMap { part ->
                val collector = runCatching { part.session.diagnosticCollector }.getOrNull() ?: return@flatMap emptyList()
                collector.rawDiagnostics
            }
            .filter { diagnostic ->
                val filePath = (diagnostic.context as? DiagnosticContext)?.containingFilePath?.let(::normalizePath)
                filePath == null || filePath == normalizedRealPath || filePath == normalizedOriginalPath
            }
    }

    private fun normalizePath(path: String): String {
        return File(path).canonicalPath.replace('\\', '/')
    }
}

class StructuredInlineDiagnosticsAssertionError(
    private val mismatches: List<CfirInlineDiagnosticsDiff.StructuredDiagnosticMismatch>,
) : AssertionFailedError(
    render(mismatches),
    FileInfo(
        mismatches.first().filePath,
        mismatches.first().expectedContent.toByteArray(StandardCharsets.UTF_8),
    ),
    mismatches.first().actualContent,
) {
    companion object {
        private fun render(mismatches: List<CfirInlineDiagnosticsDiff.StructuredDiagnosticMismatch>): String {
            return buildString {
                appendLine("Inline diagnostic assertion failed: ${mismatches.size} file(s) differ.")
                for ((index, mismatch) in mismatches.withIndex()) {
                    if (index > 0) appendLine()
                    appendLine("File: ${mismatch.filePath}")
                    if (mismatch.missing.isNotEmpty()) {
                        appendLine("Expected but not found (${mismatch.missing.size}):")
                        mismatch.missing.forEach { appendLine("  ${it.renderSummary("expected")}") }
                    }
                    if (mismatch.unexpected.isNotEmpty()) {
                        appendLine("Unexpected actual diagnostics (${mismatch.unexpected.size}):")
                        mismatch.unexpected.forEach { appendLine("  ${it.renderSummary("actual")}") }
                    }
                }
            }.trimEnd()
        }
    }
}
