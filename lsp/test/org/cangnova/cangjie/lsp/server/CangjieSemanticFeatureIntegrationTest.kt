package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CangjieSemanticFeatureIntegrationTest : AbstractLspIntegrationTest() {
    override val autoCreateDefaultSession: Boolean = false

    @Test
    fun `editor assist requests resolve against real analysis workspace`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { testSession ->
                val useUri = fixture.uri("src/use.cj")
                testSession.openDocument(useUri, fixture.text("src/use.cj"))

                val completion = testSession.completion(
                    CompletionParams(TextDocumentIdentifier(useUri), positionOf(fixture, "src/use.cj", "return current")),
                )
                val hover = testSession.hover(
                    HoverParams(TextDocumentIdentifier(useUri), positionOf(fixture, "src/use.cj", "buildUser", delta = 1)),
                )
                val signatureHelp = testSession.signatureHelp(
                    SignatureHelpParams(TextDocumentIdentifier(useUri), positionOf(fixture, "src/use.cj", "greet(user, 2)", delta = 12)),
                )
                val selectionRanges = testSession.selectionRange(
                    SelectionRangeParams(
                        TextDocumentIdentifier(useUri),
                        listOf(positionOf(fixture, "src/use.cj", "current", occurrence = 2, delta = 1)),
                    ),
                )

                val labels = completion.left.map(CompletionItem::getLabel)
                assertTrue(labels.contains("buildUser"))
                assertTrue(labels.contains("greet"))
                assertTrue(hover.toString().contains("buildUser"))
                assertEquals("greet(value: Base, times: Int64): Base", signatureHelp.signatures.single().label)
                assertEquals(1, signatureHelp.activeParameter)
                assertNotNull(selectionRanges.single().parent)
            }
        }
    }

    @Test
    fun `navigation and symbol requests resolve across workspace files`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { testSession ->
                val modelUri = fixture.uri("src/model.cj")
                val useUri = fixture.uri("src/use.cj")
                testSession.openDocument(modelUri, fixture.text("src/model.cj"))
                testSession.openDocument(useUri, fixture.text("src/use.cj"))

                val definition = testSession.definition(
                    DefinitionParams(TextDocumentIdentifier(useUri), positionOf(fixture, "src/use.cj", "buildUser", delta = 1)),
                )
                val declaration = testSession.declaration(
                    DeclarationParams(TextDocumentIdentifier(useUri), positionOf(fixture, "src/use.cj", "buildUser", delta = 1)),
                )
                val typeDefinition = testSession.typeDefinition(
                    TypeDefinitionParams(TextDocumentIdentifier(useUri), positionOf(fixture, "src/use.cj", "user, 2", delta = 1)),
                )
                val implementation = testSession.implementation(
                    ImplementationParams(TextDocumentIdentifier(modelUri), positionOf(fixture, "src/model.cj", "interface Base", delta = 10)),
                )
                val references = testSession.references(
                    ReferenceParams(
                        TextDocumentIdentifier(useUri),
                        positionOf(fixture, "src/use.cj", "buildUser", delta = 1),
                        ReferenceContext(true),
                    ),
                )
                val highlights = testSession.documentHighlight(
                    DocumentHighlightParams(
                        TextDocumentIdentifier(modelUri),
                        positionOf(fixture, "src/model.cj", "User()", delta = 1),
                    ),
                )
                val documentSymbols = testSession.documentSymbol(DocumentSymbolParams(TextDocumentIdentifier(modelUri)))
                val workspaceSymbols = testSession.workspaceSymbol(WorkspaceSymbolParams("User"))

                val buildUserLine = positionOf(fixture, "src/model.cj", "func buildUser").line
                val userLine = positionOf(fixture, "src/model.cj", "class User").line
                val symbolNames = documentSymbols.mapNotNull { symbol -> symbol.right?.name }
                val workspaceNames = workspaceSymbols.right.map(WorkspaceSymbol::getName)

                assertTrue(definition.left.any { location -> location.uri == modelUri && location.range.start.line == buildUserLine })
                assertTrue(declaration.left.any { location -> location.uri == modelUri && location.range.start.line == buildUserLine })
                assertTrue(typeDefinition.left.any { location -> location.uri == modelUri && location.range.start.line == userLine })
                assertTrue(implementation.left.any { location -> location.uri == modelUri && location.range.start.line == userLine })
                assertTrue(references.any { location -> location.uri == modelUri })
                assertTrue(references.any { location -> location.uri == useUri })
                assertFalse(highlights.isEmpty())
                assertEquals(listOf("Base", "User", "greet", "buildUser"), symbolNames)
                assertTrue(workspaceNames.contains("User"))
                assertFalse(workspaceSymbols.right.isEmpty())
            }
        }
    }

    private fun semanticWorkspace(): LspWorkspaceFixture {
        return LspWorkspaceFixtureBuilder()
            .source(
                "model.cj",
                """
                    package sample.lsp

                    interface Base {
                        func code(): Int64
                    }

                    class User <: Base {
                        override func code(): Int64 {
                            return 1
                        }
                    }

                    func greet(value: Base, times: Int64): Base {
                        return value
                    }

                    func buildUser(): User {
                        return User()
                    }
                """.trimIndent(),
            )
            .source(
                "use.cj",
                """
                    package sample.lsp

                    func consume(): Int64 {
                        let user = buildUser()
                        let base: Base = greet(user, 2)
                        let current = base.code()
                        return current
                    }
                """.trimIndent(),
            )
            .build()
    }

    private fun positionOf(
        fixture: LspWorkspaceFixture,
        relativePath: String,
        token: String,
        occurrence: Int = 1,
        delta: Int = 0,
    ): Position {
        val text = fixture.text(relativePath)
        var searchFrom = 0
        var offset = -1
        repeat(occurrence) {
            offset = text.indexOf(token, searchFrom)
            check(offset >= 0) { "Cannot find `$token` in $relativePath" }
            searchFrom = offset + token.length
        }
        val document = LspTextDocument(
            uri = fixture.uri(relativePath),
            languageId = "cangjie",
            version = 1,
            text = text,
        )
        return document.positionAt(offset + delta)
    }
}
