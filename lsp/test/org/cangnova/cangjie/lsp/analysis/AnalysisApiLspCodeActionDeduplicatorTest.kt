package org.cangnova.cangjie.lsp.analysis

import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionKind
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** 验证逐成员诊断产生的同一实现动作会合并，其他动作仍保留。 */
class AnalysisApiLspCodeActionDeduplicatorTest {
    @Test
    fun `deduplicates repeated member actions and merges related diagnostics`() {
        val firstMemberAction = codeAction("missing greet")
        val secondMemberAction = codeAction("missing farewell")
        val differentAction = codeAction("other intent")

        val actions = LspCodeActionDeduplicator.deduplicate(
            listOf(
                "ImplementMembersQuickfix" to firstMemberAction,
                "ImplementMembersQuickfix" to secondMemberAction,
                "OtherQuickfix" to differentAction,
            ),
        ).mapNotNull { result -> if (result.isRight) result.right else null }

        assertEquals(2, actions.size)
        assertEquals(
            setOf("missing greet", "missing farewell"),
            actions.first().diagnostics.orEmpty().mapNotNull { diagnostic -> diagnostic.message.left }.toSet(),
        )
    }

    private fun codeAction(diagnosticMessage: String): CodeAction = CodeAction().apply {
        title = "Implement members"
        kind = CodeActionKind.QuickFix
        data = mapOf("documentUri" to "file:///host.cj", "factory" to "InterfaceMemberMustBeImplemented")
        setDiagnostics(
            listOf(
                Diagnostic().apply {
                    range = Range(Position(0, 0), Position(0, 4))
                    message = Either.forLeft(diagnosticMessage)
                },
            ),
        )
    }
}
