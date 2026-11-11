package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.DiagnosticCollectionFailures
import org.cangnova.cangjie.lsp.CangjieLspEnvironment
import org.cangnova.cangjie.lsp.analysis.AbstractCangjieAnalysisFacade
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisRequestContext
import org.cangnova.cangjie.lsp.capabilities.CangjieLanguageServerDescriptor
import org.cangnova.cangjie.lsp.capabilities.CangjieLspFeatureSet
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionContext
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** G7：生产 codeAction 与 resolveCodeAction 请求不得把隔离异常留在全局 collector。 */
class CangjieCodeActionFailureRetentionTest {
    /** 两条生产请求都经 server context 的 failure-retention 边界。 */
    @Test
    fun codeActionAndResolveDoNotRetainIsolatedFailures() {
        DiagnosticCollectionFailures.clearAll()
        val environment = CangjieLspEnvironment.create(CangJieCoreEnvironmentMode.UnitTest)
        val context = CangjieServerContext(
            descriptor = CangjieLanguageServerDescriptor(),
            environment = environment,
            retainDiagnosticCollectionFailures = false,
            analysisFacadeFactory = { RecordingFailureAnalysisFacade() },
        )

        try {
            context.use {
                val document = LspTextDocument(
                    uri = "file:///workspace/CodeAction.cj",
                    languageId = "cangjie",
                    version = 1,
                    text = "",
                )
                val params = CodeActionParams(
                    TextDocumentIdentifier(document.uri),
                    Range(Position(0, 0), Position(0, 0)),
                    CodeActionContext(emptyList()),
                )

                assertTrue(it.codeActions(document, params).isEmpty())

                val action = CodeAction()
                assertSame(action, it.resolveCodeAction(document, action))
                assertTrue(
                    DiagnosticCollectionFailures.drain().isEmpty(),
                    "生产 codeAction 和 resolveCodeAction 请求不得保留已隔离的 Throwable。",
                )
            }
        } finally {
            DiagnosticCollectionFailures.clearAll()
        }
    }
}

/** 在两个 code-action facade 入口中模拟被隔离并记录的诊断异常。 */
private class RecordingFailureAnalysisFacade : AbstractCangjieAnalysisFacade() {
    override val supportedFeatures: CangjieLspFeatureSet = CangjieLspFeatureSet(codeAction = true)

    override fun codeActions(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: CodeActionParams,
    ): List<Either<Command, CodeAction>> {
        DiagnosticCollectionFailures.record(IllegalStateException("isolated codeAction failure"))
        return emptyList()
    }

    override fun resolveCodeAction(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: CodeAction,
    ): CodeAction {
        DiagnosticCollectionFailures.record(IllegalStateException("isolated resolveCodeAction failure"))
        return params
    }
}
