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

/**
 * 校验 LSP folding range 请求与真实工作区文档的集成行为。
 *
 * 该测试覆盖 imports、块注释和多行调用三类折叠范围，确保客户端可见位置来自实际文档快照。
 */
class CangjieFoldingIntegrationTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话，测试自行创建带夹具工作区的会话。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 校验折叠范围包含导入、注释和多行调用区域。
     *
     * 该用例根据源码 token 计算行号，避免硬编码行号掩盖工作区文本变化。
     */
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

    /**
     * 构造用于折叠测试的单文件工作区。
     *
     * 源文件同时包含连续导入、多行块注释和跨行函数调用，覆盖折叠提供器的主要输入形态。
     */
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

    /**
     * 返回指定 token 在夹具文件中的 LSP 行号。
     *
     * 该方法通过 `LspTextDocument` 统一执行偏移到位置转换，确保测试与协议坐标模型一致。
     */
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
