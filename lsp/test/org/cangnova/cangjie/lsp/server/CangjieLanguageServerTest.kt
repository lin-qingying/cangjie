package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.analysis.AbstractCangjieAnalysisFacade
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisRequestContext
import org.cangnova.cangjie.lsp.capabilities.CangjieLspFeatureSet
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CangjieLanguageServerTest : AbstractLspIntegrationTest() {
    @Test
    fun `default server advertises diagnostics over real lsp connection`() {
        val result = session.initializeResult()

        assertNotNull(result.capabilities.textDocumentSync)
        assertNull(result.capabilities.completionProvider)
        assertNull(result.capabilities.hoverProvider)
        assertNotNull(result.capabilities.diagnosticProvider)
    }

    @Test
    fun `publishes diagnostics through real lsp connection`() {
        val diagnosticsSession = createSession(
            defaultServerOptions().copy(
                analysisFacadeFactory = { RecordingDiagnosticsFacade() },
            ),
        )

        diagnosticsSession.use {
            it.openDocument(
                uri = "file:///workspace/main.cj",
                text = "let answer = 42",
            )
            it.awaitDiagnosticsCount(1)

            val published = it.publishedDiagnostics().single()
            val diagnostic = published.diagnostics.single()
            assertEquals("file:///workspace/main.cj", published.uri)
            assertEquals("framework diagnostic", diagnostic.message.left)
            assertEquals(DiagnosticSeverity.Warning, diagnostic.severity)
            assertEquals(Range(Position(0, 0), Position(0, 3)), diagnostic.range)
        }
    }

    private class RecordingDiagnosticsFacade : AbstractCangjieAnalysisFacade() {
        override val supportedFeatures = CangjieLspFeatureSet(diagnostics = true)

        override fun collectDiagnostics(
            context: CangjieAnalysisRequestContext,
            document: LspTextDocument,
        ): List<Diagnostic> {
            return listOf(
                Diagnostic(
                    Range(Position(0, 0), Position(0, 3)),
                    "framework diagnostic",
                    DiagnosticSeverity.Warning,
                    "cangjie",
                    "framework",
                ),
            )
        }
    }
}
