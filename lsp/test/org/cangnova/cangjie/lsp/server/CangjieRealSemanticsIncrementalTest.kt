package org.cangnova.cangjie.lsp.server

import java.nio.file.Files
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.framework.LspIntegrationTestSession
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.LspTestData
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.FileChangeType
import org.eclipse.lsp4j.FileEvent
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.SelectionRange
import org.eclipse.lsp4j.SelectionRangeParams
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import org.eclipse.lsp4j.WorkspaceSymbolParams
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 真实语义下的**增量可见性**。
 *
 * 覆盖三类只有跨请求/跨文件系统事件才暴露的路径：
 * 1. 磁盘上新建/删除的源码能否进入工作区模型（`workspace/didChangeWatchedFiles`）；
 * 2. 选择范围一次请求多个位置时是否按序返回多条链；
 * 3. pull 诊断是否按当前文档状态重算，而不是返回缓存。
 */
class CangjieRealSemanticsIncrementalTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：每个用例需要自己的临时工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 磁盘新建源码 + watched files 通知后，新类型必须可检索。
     *
     * 真实客户端新增文件只发 `workspace/didChangeWatchedFiles`，不会重开会话；
     * 若此时工作区模型仍看不到新文件，用户会看到「新文件里的符号搜不到、跳不过去」。
     */
    @Test
    fun `watched file creation becomes visible to workspace symbols`() {
        incrementalWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val newFile = fixture.path("src/watched.cj")
                LspTestData.copyTo("realSemantics/incremental/watched.cj", newFile)
                session.didChangeWatchedFiles(
                    DidChangeWatchedFilesParams(
                        listOf(FileEvent(fixture.uri("src/watched.cj"), FileChangeType.Created)),
                    ),
                )

                val names = workspaceSymbolNames(session, "WatchedType")
                assertTrue(
                    names.contains("WatchedType"),
                    "新建文件里的 WatchedType 必须在 watched files 通知后可检索，实际 $names",
                )
            }
        }
    }

    /**
     * 磁盘删除源码 + watched files 通知后，旧类型必须从检索结果消失。
     */
    @Test
    fun `watched file deletion removes symbols from workspace`() {
        incrementalWorkspace(includeDoomed = true).use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val doomed = fixture.path("src/doomed.cj")
                assertTrue(Files.isRegularFile(doomed), "夹具前提：doomed.cj 必须存在")

                val before = workspaceSymbolNames(session, "DoomedType")
                assertTrue(before.contains("DoomedType"), "删除前必须能检索到 DoomedType，实际 $before")

                Files.delete(doomed)
                session.didChangeWatchedFiles(
                    DidChangeWatchedFilesParams(
                        listOf(FileEvent(fixture.uri("src/doomed.cj"), FileChangeType.Deleted)),
                    ),
                )

                val after = workspaceSymbolNames(session, "DoomedType")
                assertTrue(
                    !after.contains("DoomedType"),
                    "已删除文件的 DoomedType 不得继续出现在检索结果里，实际 $after",
                )
            }
        }
    }

    /**
     * 一次请求多个位置时，选择范围必须按序返回同样数量的链，且每条链都覆盖自己的位置。
     */
    @Test
    fun `selection range answers one chain per requested position`() {
        incrementalWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/main.cj")
                val text = fixture.text("src/main.cj")
                session.openDocument(uri, text)

                val positions = listOf(
                    positionOf(text, "return 1", delta = 7),
                    positionOf(text, "ping()", delta = 1),
                    positionOf(text, "func use", delta = 5),
                )

                val chains = session.selectionRange(
                    SelectionRangeParams(TextDocumentIdentifier(uri), positions),
                )

                assertEquals(
                    positions.size,
                    chains.size,
                    "每个请求位置必须对应一条链，请求 ${positions.size} 个位置，实际 ${chains.size} 条",
                )
                chains.forEachIndexed { index, chain ->
                    assertTrue(
                        covers(chain.range, positions[index]),
                        "第 $index 条链必须覆盖对应请求位置 ${positions[index]}，实际 ${chain.range}",
                    )
                    assertNested(chain, index)
                }
            }
        }
    }

    /**
     * pull 诊断必须反映当前文档状态：修好之后立即变空，不需要等 push。
     */
    @Test
    fun `pull diagnostics follow the current document state`() {
        val broken = LspTestData.text("realSemantics/incremental/pull-broken.cj")
        val fixed = LspTestData.text("realSemantics/incremental/pull-fixed.cj")

        incrementalWorkspace(mainTestData = "realSemantics/incremental/pull-broken.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/main.cj")
                session.openDocument(uri, broken)

                val before = session.documentDiagnostic(session.documentDiagnosticParams(uri))
                assertTrue(
                    before.relatedFullDocumentDiagnosticReport.items.any {
                        it.severity == DiagnosticSeverity.Error
                    },
                    "打开有错的文档时 pull 诊断必须报告错误，实际 ${before.relatedFullDocumentDiagnosticReport.items}",
                )

                session.changeDocument(
                    DidChangeTextDocumentParams(
                        VersionedTextDocumentIdentifier(uri, 2),
                        listOf(TextDocumentContentChangeEvent(fixed)),
                    ),
                )

                val after = session.documentDiagnostic(session.documentDiagnosticParams(uri))
                assertTrue(
                    after.relatedFullDocumentDiagnosticReport.items.none {
                        it.severity == DiagnosticSeverity.Error
                    },
                    "修好之后 pull 诊断不得继续报告错误（说明返回了缓存），实际 " +
                        "${after.relatedFullDocumentDiagnosticReport.items}",
                )
            }
        }
    }

    // ---------------------------------------------------------------- 夹具

    /**
     * 单模块工作区；主文件与可选「待删除文件」都来自 testData。
     */
    private fun incrementalWorkspace(
        includeDoomed: Boolean = false,
        mainTestData: String = "realSemantics/incremental/main.cj",
    ): LspWorkspaceFixture {
        val builder = LspWorkspaceFixtureBuilder()
            .sourceFromTestData("main.cj", mainTestData)
        if (includeDoomed) {
            builder.sourceFromTestData("doomed.cj", "realSemantics/incremental/doomed.cj")
        }
        return builder.build()
    }

    // ---------------------------------------------------------- 断言辅助

    /**
     * 工作区符号查询，返回两种结果形态的名字列表。
     */
    private fun workspaceSymbolNames(
        session: LspIntegrationTestSession,
        query: String,
    ): List<String> {
        val symbols = session.workspaceSymbol(WorkspaceSymbolParams(query))
        return when {
            symbols.isRight -> symbols.right.orEmpty().map { it.name }
            else -> symbols.left.orEmpty().map { it.name }
        }
    }

    /**
     * 判断范围是否覆盖指定位置。
     */
    private fun covers(range: org.eclipse.lsp4j.Range, position: Position): Boolean =
        !after(range.start, position) && !after(position, range.end)

    /**
     * 位置先后比较。
     */
    private fun after(left: Position, right: Position): Boolean =
        left.line > right.line || (left.line == right.line && left.character > right.character)

    /**
     * 选择范围链必须逐级包含。
     */
    private fun assertNested(chain: SelectionRange, index: Int) {
        var parent = chain.parent
        var child = chain
        while (parent != null) {
            assertTrue(
                covers(parent.range, child.range.start) && covers(parent.range, child.range.end),
                "第 $index 条链必须逐级包含：${parent.range} 不含 ${child.range}",
            )
            child = parent
            parent = parent.parent
        }
    }

    /**
     * 文本中指定 token 首次出现处（可加 [delta]）的位置。
     */
    private fun positionOf(text: String, token: String, delta: Int = 0): Position {
        val offset = text.indexOf(token)
        check(offset >= 0) { "Cannot find `$token`" }
        return LspTextDocument("file:///incremental.cj", "cangjie", 1, text).positionAt(offset + delta)
    }

}
