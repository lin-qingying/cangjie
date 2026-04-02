package org.cangnova.cangjie.lsp.analysis

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.diagnostics.CaSeverity
import org.cangnova.cangjie.analysis.api.diagnostics.getDefaultMessageWithFactoryName
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.jsonrpc.messages.Either

/**
 * 把 Analysis API 诊断转换成协议层使用的 LSP 诊断。
 */
internal object AnalysisApiDiagnostics {
    fun toLspDiagnostics(
        document: LspTextDocument,
        source: String,
        diagnostics: Collection<CaDiagnosticWithPsi<*>>,
    ): List<Diagnostic> {
        return diagnostics.flatMap { diagnostic ->
            diagnostic.lspRanges(document).map { range ->
                Diagnostic().apply {
                    this.range = range
                    this.message = Either.forLeft(diagnostic.getDefaultMessageWithFactoryName())
                    this.severity = diagnostic.severity.toLspSeverity()
                    this.source = source
                    this.code = Either.forLeft(diagnostic.factoryName)
                }
            }
        }
    }

    private fun CaDiagnosticWithPsi<*>.lspRanges(document: LspTextDocument) =
        effectiveRanges().map { range -> document.rangeOf(range.startOffset, range.endOffset) }

    private fun CaDiagnosticWithPsi<*>.effectiveRanges(): List<TextRange> {
        val fromDiagnostic = textRanges.filterNot { it.isEmpty }
        if (fromDiagnostic.isNotEmpty()) return fromDiagnostic

        // 有些诊断只挂在 PSI 元素上，没有独立 text range；这里回退到元素范围，确保 LSP 侧仍可消费。
        val fallback = psi.textRange
        return if (fallback == null || fallback.isEmpty) {
            listOf(TextRange(0, 0))
        } else {
            listOf(fallback)
        }
    }

    private fun CaSeverity.toLspSeverity(): DiagnosticSeverity = when (this) {
        CaSeverity.ERROR -> DiagnosticSeverity.Error
        CaSeverity.WARNING -> DiagnosticSeverity.Warning
        CaSeverity.INFO -> DiagnosticSeverity.Information
    }
}
