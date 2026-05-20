package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.FoldingRangeKind
import org.eclipse.lsp4j.FoldingRangeRequestParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CangjieFoldingIntegrationTest : AbstractLspIntegrationTest() {
    override val autoCreateDefaultSession: Boolean = false

    @Test
    fun `folding range returns real imports comment and multiline call ranges`() {
        foldingWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { testSession ->
                val uri = fixture.uri("src/folding.cj")
                testSession.openDocument(uri, fixture.text("src/folding.cj"))

                val ranges = testSession.foldingRange(FoldingRangeRequestParams(TextDocumentIdentifier(uri)))
                val importsLine = lineOfToken(fixture, "src/folding.cj", "import std.collection.ArrayList")
                val commentLine = lineOfToken(fixture, "src/folding.cj", "/*")
                val callLine = lineOfToken(fixture, "src/folding.cj", "consume(")

                assertTrue(
                    ranges.any { range ->
                        range.kind == FoldingRangeKind.Imports &&
                            range.startLine == importsLine &&
                            range.endLine > range.startLine
                    },
                )
                assertTrue(
                    ranges.any { range ->
                        range.kind == FoldingRangeKind.Comment &&
                            range.startLine == commentLine &&
                            range.endLine > range.startLine
                    },
                )
                assertTrue(
                    ranges.any { range ->
                        range.kind == FoldingRangeKind.Region &&
                            range.startLine == callLine &&
                            range.endLine > range.startLine
                    },
                )
            }
        }
    }

    private fun foldingWorkspace(): LspWorkspaceFixture {
        return LspWorkspaceFixtureBuilder()
            .source(
                "folding.cj",
                """
                    package sample.lsp

                    import std.collection.ArrayList
                    import std.collection.HashMap

                    /*
                     * folded comment
                     */
                    func main() {
                        consume(
                            1,
                            2
                        )
                    }
                """.trimIndent(),
            )
            .build()
    }

    private fun lineOfToken(
        fixture: LspWorkspaceFixture,
        relativePath: String,
        token: String,
    ): Int {
        val text = fixture.text(relativePath)
        val offset = text.indexOf(token)
        check(offset >= 0) { "Cannot find `$token` in $relativePath" }
        val document = LspTextDocument(
            uri = fixture.uri(relativePath),
            languageId = "cangjie",
            version = 1,
            text = text,
        )
        return document.positionAt(offset).line
    }
}
