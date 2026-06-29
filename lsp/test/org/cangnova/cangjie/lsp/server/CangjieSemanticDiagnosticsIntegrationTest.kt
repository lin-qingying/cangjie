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

/**
 * 校验 LSP 诊断能力与真实 Analysis API 工作区的集成。
 *
 * 该测试覆盖标准库路径、overlay 依赖刷新、push/pull diagnostics 和新增工作区目录诊断。
 */
class CangjieSemanticDiagnosticsIntegrationTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话，测试按不同工作区配置手动创建会话。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 校验通过 LSP 配置的标准库路径可以解析默认导入 String。
     */
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

    /**
     * 校验 overlay 依赖变更会刷新依赖文档的 push 和 pull diagnostics。
     */
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

    /**
     * 校验工作区诊断包含新加入 workspace folder 下未打开的磁盘源码文件。
     */
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

    /**
     * 构造 overlay 依赖诊断测试使用的两文件工作区。
     */
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

    /**
     * 返回指定 URI 最近一次发布的诊断列表。
     */
    private fun diagnosticsFor(
        session: org.cangnova.cangjie.lsp.framework.LspIntegrationTestSession,
        uri: String,
    ) = session.publishedDiagnostics()
        .lastOrNull { published -> published.uri == uri }
        ?.diagnostics
        .orEmpty()
}
