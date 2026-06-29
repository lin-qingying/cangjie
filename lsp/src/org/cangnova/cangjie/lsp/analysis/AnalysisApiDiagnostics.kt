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
    /**
     * 将 Analysis API 诊断集合转换为当前文档的 LSP 诊断列表。
     *
     * 每个诊断可能携带多个文本范围，因此转换结果按有效范围展开，并保留消息、严重级别、来源和诊断码。
     */
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

    /**
     * 将诊断有效范围映射为 LSP 文档范围。
     *
     * 映射使用文档快照的 analysis 文本偏移，保证 PSI 诊断位置与客户端可见文本一致。
     */
    private fun CaDiagnosticWithPsi<*>.lspRanges(document: LspTextDocument) =
        effectiveRanges().map { range -> document.analysisRangeOf(range.startOffset, range.endOffset) }

    /**
     * 计算诊断可呈现给客户端的文本范围。
     *
     * 优先使用诊断自身范围；缺失时回退到 PSI 元素范围；两者都不可用时返回零长度起点范围。
     */
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

    /**
     * 将 Analysis API 严重级别映射为 LSP 严重级别。
     */
    private fun CaSeverity.toLspSeverity(): DiagnosticSeverity = when (this) {
        CaSeverity.ERROR -> DiagnosticSeverity.Error
        CaSeverity.WARNING -> DiagnosticSeverity.Warning
        CaSeverity.INFO -> DiagnosticSeverity.Information
    }
}
