package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.framework.LspIntegrationTestSession
import org.cangnova.cangjie.lsp.testkit.LspEditApplicator
import org.cangnova.cangjie.lsp.testkit.LspTestData
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.FormattingOptions
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 客户端**配置**驱动的行为：缩进风格与诊断版本号。
 *
 * 这两项都不改变「有没有结果」，只改变「结果对不对得上客户端的设置」——
 * 一旦错，用户看到的是「我的缩进设置没生效」「波浪线停在上一次的内容上」，
 * 而且服务端日志里一切正常。
 */
class CangjieRealSemanticsClientSettingsTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：每个用例按自己的客户端能力初始化。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * `FormattingOptions.tabSize` 必须真的决定缩进宽度。
     *
     * 同一次会话里用两个 tabSize 请求格式化的结果必须不同；只测单个 tabSize 时，
     * 「完全忽略这个字段、永远用默认 4」也能通过。
     */
    @Test
    fun `formatting honours the client tab size`() {
        testDataWorkspace(FORMATTING, "realSemantics/formatting.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(FORMATTING)
                val text = fixture.text(FORMATTING)
                session.openDocument(uri, text)

                val twoSpaces = formattedText(session, uri, text, FormattingOptions(2, true))
                val fourSpaces = formattedText(session, uri, text, FormattingOptions(4, true))

                assertNotEquals(
                    twoSpaces,
                    fourSpaces,
                    "tabSize 不同却得到完全相同的文本，说明该字段被忽略：\n$twoSpaces",
                )
                assertEquals(
                    2,
                    indentWidth(twoSpaces, "let x"),
                    "tabSize=2 时函数体必须缩进 2 个空格，实际：\n$twoSpaces",
                )
                assertEquals(
                    4,
                    indentWidth(fourSpaces, "let x"),
                    "tabSize=4 时函数体必须缩进 4 个空格，实际：\n$fourSpaces",
                )
            }
        }
    }

    /**
     * `insertSpaces=false` 时缩进必须用制表符。
     *
     * 客户端的「缩进方式：Tab」是用户显式设置；服务端返回空格缩进会被编辑器原样写入文件，
     * 用户随后在团队里制造一行行 diff。
     */
    @Test
    fun `formatting honours tab indentation when the client asks for it`() {
        testDataWorkspace(FORMATTING, "realSemantics/formatting.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(FORMATTING)
                val text = fixture.text(FORMATTING)
                session.openDocument(uri, text)

                val tabbed = formattedText(session, uri, text, FormattingOptions(4, false))
                val bodyLine = tabbed.lineSequence().firstOrNull { it.contains("let x") }
                assertTrue(bodyLine != null, "格式化结果里找不到函数体，实际：\n$tabbed")
                assertTrue(
                    bodyLine!!.startsWith("\t"),
                    "insertSpaces=false 时函数体必须以制表符缩进，实际 `$bodyLine`——" +
                        "客户端会把这个空格缩进原样写进文件",
                )
            }
        }
    }

    /**
     * 发布诊断必须带上文档版本，并随文档更新。
     *
     * 客户端用 `version` 丢弃过期诊断：版本不变时，编辑器会把上一次的错误一直画在那里，
     * 即使文件已经改好（或相反：把旧错误覆盖到新内容上）。
     */
    @Test
    fun `published diagnostics carry the document version and follow changes`() {
        testDataWorkspace(DIAGNOSTICS, "realSemantics/incremental/pull-broken.cj").use { fixture ->
            // fullFeatured 声明了 publishDiagnostics.versionSupport。
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(DIAGNOSTICS)
                val broken = LspTestData.text("realSemantics/incremental/pull-broken.cj")
                val fixed = LspTestData.text("realSemantics/incremental/pull-fixed.cj")
                session.openDocument(uri, broken, version = 1)

                val firstPublish = session.awaitPublishedDiagnostics(uri) { params ->
                    params.diagnostics.any { it.severity == DiagnosticSeverity.Error }
                }
                assertEquals(
                    1,
                    firstPublish.version ?: -1,
                    "客户端声明 versionSupport 时，诊断必须带上文档版本（当前版本 1）",
                )

                session.changeDocument(uri, fixed, version = 2)
                val secondPublish = session.awaitPublishedDiagnostics(uri) { params ->
                    params.diagnostics.isEmpty()
                }
                assertEquals(
                    2,
                    secondPublish.version ?: -1,
                    "改好之后重发的空诊断必须带新版本 2，否则客户端会当成过期消息丢弃",
                )
            }
        }
    }

    // ---------------------------------------------------------------- 夹具

    private companion object {
        const val FORMATTING: String = "src/format.cj"
        const val DIAGNOSTICS: String = "src/pull.cj"
    }

    /** 由 testData 单文件构造的工作区。 */
    private fun testDataWorkspace(targetRelativePath: String, testDataPath: String): LspWorkspaceFixture =
        LspWorkspaceFixtureBuilder()
            .fileFromTestData(targetRelativePath, testDataPath)
            .build()

    // ---------------------------------------------------------------- 断言辅助

    /** 用给定选项格式化并应用编辑，返回最终文本。 */
    private fun formattedText(
        session: LspIntegrationTestSession,
        uri: String,
        text: String,
        options: FormattingOptions,
    ): String {
        val edits = session.formatting(DocumentFormattingParams(TextDocumentIdentifier(uri), options))
        assertTrue(edits.isNotEmpty(), "格式化必须返回编辑（options=$options）")
        return LspEditApplicator.apply(text, edits, uri)
    }

    /** 文本中包含指定片段的那一行的缩进宽度（只按空格计，制表符记为 -1）。 */
    private fun indentWidth(text: String, marker: String): Int {
        val line = text.lineSequence().firstOrNull { it.contains(marker) } ?: return -1
        if (line.startsWith("\t")) return -1
        return line.takeWhile { it == ' ' }.length
    }
}
