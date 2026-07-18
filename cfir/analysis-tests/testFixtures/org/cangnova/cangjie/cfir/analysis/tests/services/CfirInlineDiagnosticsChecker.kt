package org.cangnova.cangjie.cfir.analysis.tests.services

import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.frontend.cfirDiagnosticCollectorService
import org.cangnova.cangjie.test.services.artifactsProvider
import org.cangnova.cangjie.test.services.moduleStructure
import org.cangnova.cangjie.test.services.TestServices
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import java.nio.charset.StandardCharsets

/**
 * after-analysis 阶段的 CFIR 内联诊断检查器。
 *
 * 该检查器读取 CFIR 前端诊断产物，并逐个源文件调用 [CfirInlineDiagnosticsDiff]
 * 生成结构化 mismatch。
 */
class CfirInlineDiagnosticsChecker(
    testServices: TestServices,
) : AfterAnalysisChecker(testServices) {
    /**
     * 检查器执行顺序。
     *
     * P1 让内联诊断 diff 尽早报告，避免被后续 suppressor 吞掉上下文。
     */
    override val order: Order
        get() = Order.P1

    /**
     * 检查所有非附加源文件的 CFIR 诊断是否与内联标记一致。
     */
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

    /**
     * 提取指定测试文件对应的 CFIR 前端诊断。
     *
     * 路径过滤同时接受真实临时文件路径和原始 testData 路径，以兼容测试框架的文件映射。
     */
    private fun diagnosticsForFile(
        artifact: org.cangnova.cangjie.test.frontend.CfirOutputArtifact,
        file: org.cangnova.cangjie.test.model.TestFile,
    ): List<org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic> {
        val frontendDiagnostics = testServices.cfirDiagnosticCollectorService.getFrontendDiagnosticsForModule(artifact)
        val cfirFiles = artifact.partsForDependsOnModules
            .asSequence()
            .mapNotNull { part -> part.firFilesByTestFile[file] }
            .toList()
        val diagnostics = cfirFiles
            .asSequence()
            .flatMap { cfirFile -> frontendDiagnostics[cfirFile].orEmpty().asSequence() }
            .toList()
        return diagnostics
    }
}

/**
 * 结构化内联诊断断言错误。
 *
 * 继承 [AssertionFailedError] 以便 IDE/test runner 能展示 expected/actual 文本差异。
 */
class StructuredInlineDiagnosticsAssertionError(
    /**
     * 当前断言错误包含的文件级诊断差异。
     */
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
