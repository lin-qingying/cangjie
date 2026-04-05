package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.analysis.AbstractCangjieAnalysisFacade
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisRequestContext
import org.cangnova.cangjie.lsp.capabilities.CangjieLspFeatureSet
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CangjieLanguageServerTest : AbstractLspIntegrationTest() {
    @Test
    fun `default server keeps diagnostics on notification channel over real lsp connection`() {
        val result = session.initializeResult()

        assertNotNull(result.capabilities.textDocumentSync)
        assertNotNull(result.capabilities.completionProvider)
        assertNotNull(result.capabilities.hoverProvider)
        assertNull(result.capabilities.diagnosticProvider)
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

    @Test
    fun `default analysis facade refreshes diagnostics after document changes`() {
        val uri = "file:///workspace/main.cj"
        session.openDocument(
            uri = uri,
            text = """
                import ghost.pkg.MissingSymbol

                func useGreeting() {
                    let answer = 42
                }
            """.trimIndent(),
        )
        session.awaitDiagnosticsCount(1)

        val firstPublish = session.publishedDiagnostics().last()
        assertEquals(uri, firstPublish.uri)
        assertFalse(firstPublish.diagnostics.isEmpty(), "未收到真实 Analysis API 诊断")

        session.changeDocument(
            uri = uri,
            version = 2,
            newText = """
                func useGreeting() {
                    let answer = 42
                }
            """.trimIndent(),
        )
        session.awaitDiagnosticsCount(2)

        val secondPublish = session.publishedDiagnostics().last()
        assertEquals(uri, secondPublish.uri)
        assertTrue(secondPublish.diagnostics.isEmpty(), "文档修复后诊断没有刷新为空")
    }

    @Test
    fun `default analysis facade clears diagnostics after document closed`() {
        val uri = "file:///workspace/main.cj"
        session.openDocument(
            uri = uri,
            text = "import missing.symbol",
        )
        session.awaitDiagnosticsCount(1)

        session.closeDocument(uri)
        session.awaitDiagnosticsCount(2)

        val secondPublish = session.publishedDiagnostics().last()
        assertEquals(uri, secondPublish.uri)
        assertTrue(secondPublish.diagnostics.isEmpty(), "文档关闭后诊断没有刷新为空")
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
