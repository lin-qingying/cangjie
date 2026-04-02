package org.cangnova.cangjie.lsp.analysis

import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.lsp.capabilities.CangjieLspFeatureSet
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.eclipse.lsp4j.Diagnostic

/**
 * 基于 Analysis API 入口的默认 LSP 分析适配器。
 *
 * 目前 Analysis API 已经明确给出 diagnostics 的直接入口，所以这一项做真实接入。
 * 其它 LSP 能力还没有对应稳定入口，继续继承基础类里的 TODO 占位。
 */
class AnalysisApiCangjieAnalysisFacade(
    lifecycleContext: CangjieAnalysisLifecycleContext,
) : AbstractCangjieAnalysisFacade() {
    private val psiDocumentFactory = AnalysisApiPsiDocumentFactory(lifecycleContext)

    override val supportedFeatures: CangjieLspFeatureSet = CangjieLspFeatureSet(
        diagnostics = true,
    )

    override fun collectDiagnostics(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
    ): List<Diagnostic> {
        // 每次都基于最新 LSP 文本重建 PSI 快照，保证分析看到的是当前文档内容。
        val psiFile = psiDocumentFactory.createAnalyzableFile(document)
        val diagnostics = analyze(psiFile) {
            psiFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }
        return AnalysisApiDiagnostics.toLspDiagnostics(
            document = document,
            source = context.descriptor.diagnosticIdentifier,
            diagnostics = diagnostics,
        )
    }
}
