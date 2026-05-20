package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspClientCapabilitiesBuilder
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams
import org.eclipse.lsp4j.WorkspaceDiagnosticParams
import org.eclipse.lsp4j.WorkspaceFoldersChangeEvent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CangjieSemanticDiagnosticsIntegrationTest : AbstractLspIntegrationTest() {
    override val autoCreateDefaultSession: Boolean = false

    @Test
    fun `stdlib default imports resolve String through configured stdlib path`() {
        LspWorkspaceFixtureBuilder()
            .source(
                "stdlib.cj",
                """
                    package sample.lsp

                    func echo(value: String): String {
                        return value
                    }
                """.trimIndent(),
            )
            .build()
            .use { fixture ->
                createSession(fixture.initializeParams()).use { testSession ->
                    val uri = fixture.uri("src/stdlib.cj")
                    testSession.openDocument(uri, fixture.text("src/stdlib.cj"))
                    testSession.awaitDiagnosticsCount(1)

                    assertTrue(diagnosticsFor(testSession, uri).isEmpty())
                }
            }
    }

    @Test
    fun `overlay dependency changes refresh push and pull diagnostics for dependent document`() {
        overlayWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { testSession ->
                val mainUri = fixture.uri("src/main.cj")
                val dependencyUri = fixture.uri("src/a/helper.cj")

                testSession.openDocument(mainUri, fixture.text("src/main.cj"))
                testSession.openDocument(dependencyUri, fixture.text("src/a/helper.cj"))
                testSession.awaitDiagnosticsCount(2)

                testSession.clearPublishedDiagnostics()
                testSession.changeDocument(
                    dependencyUri,
                    """
                        package sample.overlay.a

                        public func renamedValue(): Int64 {
                            return 1
                        }
                    """.trimIndent(),
                    2,
                )
                testSession.awaitPublishedDiagnostics(mainUri) { published ->
                    published.diagnostics.any { diagnostic ->
                        diagnostic.message.left?.contains("Unresolved reference") == true
                    }
                }

                assertTrue(
                    diagnosticsFor(testSession, mainUri).any { diagnostic ->
                        diagnostic.message.left?.contains("Unresolved reference") == true
                    },
                )
                assertTrue(
                    testSession.documentDiagnostic(testSession.documentDiagnosticParams(mainUri))
                        .toString()
                        .contains("Unresolved reference"),
                )

                testSession.clearPublishedDiagnostics()
                testSession.closeDocument(dependencyUri)
                testSession.awaitDiagnosticsCount(2)

                assertTrue(diagnosticsFor(testSession, dependencyUri).isEmpty())
                assertTrue(diagnosticsFor(testSession, mainUri).isEmpty())
            }
        }
    }

    @Test
    fun `workspace diagnostics include unopened disk files from newly added workspace folders`() {
        LspWorkspaceFixtureBuilder()
            .withoutStdlib()
            .addModule(name = "root", sourceRoots = listOf(""))
            .file(
                "main.cj",
                """
                    package sample.root

                    func ok(): Int64 {
                        return 1
                    }
                """.trimIndent(),
            )
            .build()
            .use { primaryFixture ->
                LspWorkspaceFixtureBuilder()
                    .withoutStdlib()
                    .addModule(name = "added", sourceRoots = listOf(""))
                    .file(
                        "broken.cj",
                        """
                            package sample.added

                            import ghost.pkg.MissingSymbol

                            func broken(): Int64 {
                                return 1
                            }
                        """.trimIndent(),
                    )
                    .build()
                    .use { addedFixture ->
                        createSession(
                            primaryFixture.initializeParams(
                                capabilities = LspClientCapabilitiesBuilder.fullFeatured(),
                                includeMultiModuleOption = false,
                            ),
                        ).use { testSession ->
                            val before = testSession.workspaceDiagnostic(WorkspaceDiagnosticParams()).toString()

                            testSession.didChangeWorkspaceFolders(
                                DidChangeWorkspaceFoldersParams(
                                    WorkspaceFoldersChangeEvent(
                                        listOf(addedFixture.workspaceFolder()),
                                        emptyList(),
                                    ),
                                ),
                            )

                            val after = testSession.workspaceDiagnostic(WorkspaceDiagnosticParams()).toString()
                            assertFalse(before.contains("broken.cj"))
                            assertTrue(after.contains("broken.cj"))
                        }
                    }
            }
    }

    private fun overlayWorkspace(): LspWorkspaceFixture {
        return LspWorkspaceFixtureBuilder()
            .source(
                "main.cj",
                """
                    package sample.overlay

                    import sample.overlay.a.*

                    func useValue(): Int64 {
                        return makeValue()
                    }
                """.trimIndent(),
            )
            .source(
                "a/helper.cj",
                """
                    package sample.overlay.a

                    public func makeValue(): Int64 {
                        return 1
                    }
                """.trimIndent(),
            )
            .build()
    }

    private fun diagnosticsFor(
        session: org.cangnova.cangjie.lsp.framework.LspIntegrationTestSession,
        uri: String,
    ) = session.publishedDiagnostics()
        .lastOrNull { published -> published.uri == uri }
        ?.diagnostics
        .orEmpty()
}
