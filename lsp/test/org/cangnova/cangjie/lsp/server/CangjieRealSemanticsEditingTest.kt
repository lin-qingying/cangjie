package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.LspEditApplicator
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.FormattingOptions
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 编辑与生命周期：**增量同步的坐标**、**格式化的幂等性**、**关闭文档后的收尾**。
 *
 * 这三条都是「一次请求对了、连续操作就错」的典型位置：
 * - 增量变更要按 UTF-16 码元算范围，文件里有非 BMP 字符时按字节算会写错位置；
 * - 格式化不幂等会让「保存时格式化」每次保存都改一次文件，产生幽灵 diff；
 * - `didClose` 不清理会让客户端的波浪线停留在已关闭的文件上。
 */
class CangjieRealSemanticsEditingTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：每个用例自带工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 非 BMP 字符之后的增量编辑必须落在正确位置。
     *
     * 文件前几行有 emoji（每个占 2 个 UTF-16 码元、4 个 UTF-8 字节）。若服务端按字节
     * 解释 `Range`，每一行都会偏移若干字符，编辑会把代码改成别的样子；这里用
     * 「改完还能解析、还能跳到声明」来暴露这类偏移错误——错位的编辑会直接破坏语法。
     */
    @Test
    fun `incremental edits after a non BMP line keep coordinates correct`() {
        testDataWorkspace(EMOJI, "realSemantics/emoji.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(EMOJI)
                val text = fixture.text(EMOJI)
                session.openDocument(uri, text, version = 1)

                val document = LspTextDocument(uri, "cangjie", 1, text)
                val callOffset = text.indexOf("return twice(2)")
                check(callOffset >= 0) { "测试数据里找不到 `return twice(2)`" }
                val bodyRange = Range(
                    document.positionAt(callOffset),
                    document.positionAt(callOffset + "return twice(2)".length),
                )

                session.changeDocument(
                    DidChangeTextDocumentParams(
                        VersionedTextDocumentIdentifier(uri, 2),
                        listOf(TextDocumentContentChangeEvent(bodyRange, "return twice(3) + 1")),
                    ),
                )

                val diagnostics = session.documentDiagnostic(session.documentDiagnosticParams(uri))
                val errors = diagnostics.relatedFullDocumentDiagnosticReport.items
                    .filter { it.severity == DiagnosticSeverity.Error }
                assertTrue(
                    errors.isEmpty(),
                    "编辑必须落在正确偏移，改完不应出现语法错误，实际 $errors",
                )

                val twicePosition = document.positionAt(callOffset + "return ".length)
                val definition = session.definition(
                    DefinitionParams(TextDocumentIdentifier(uri), twicePosition),
                ).let { if (it.isLeft) it.left.orEmpty() else emptyList() }
                val declarationLine = lineOf(fixture, EMOJI, "func twice")
                assertTrue(
                    definition.any { location ->
                        // 声明元素的 range 会带上前置注释行，因此判「覆盖声明行」而不是「起始行相等」。
                        location.uri == uri &&
                            location.range.start.line <= declarationLine &&
                            location.range.end.line >= declarationLine
                    },
                    "增量编辑后 definition 必须落在 func twice 的声明上（第 $declarationLine 行），实际 $definition",
                )
            }
        }
    }

    /**
     * 连续多次增量编辑（模拟逐个字符输入）后，坐标仍必须正确。
     *
     * 真实客户端每次按键发一次 `didChange`，只带一个很小的 range。这里把
     * `return twi` 分两步补成 `return twice(2)`：先插入 `ce`，再插入 `(2)`。
     * 第二次编辑的坐标建立在**第一次已应用**的文本上——偏移算错会累积。
     *
     * 断言落在「能否解析」上：补全成合法调用之后，definition 必须命中声明。
     * （注意 `twice` 单独出现是函数值而非引用，本用例刻意补成调用，避免把
     * 「不带括号的函数名不算引用」误判成增量同步的缺陷。）
     */
    @Test
    fun `consecutive incremental changes keep the symbol resolvable`() {
        completionWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(COMPLETION)
                val text = fixture.text(COMPLETION)
                session.openDocument(uri, text, version = 1)

                // 客户端每敲一段就更新本地缓冲，下一次编辑的坐标基于**更新后**的文本。
                var current = text
                val prefixStart = current.indexOf("return twi") + "return ".length

                fun insertAt(offset: Int, inserted: String, version: Int) {
                    val buffer = LspTextDocument(uri, "cangjie", version, current)
                    val caret = buffer.positionAt(offset)
                    session.changeDocument(
                        DidChangeTextDocumentParams(
                            VersionedTextDocumentIdentifier(uri, version),
                            listOf(TextDocumentContentChangeEvent(Range(caret, caret), inserted)),
                        ),
                    )
                    current = current.substring(0, offset) + inserted + current.substring(offset)
                }

                insertAt(prefixStart + "twi".length, "ce", version = 2)
                insertAt(prefixStart + "twice".length, "(2)", version = 3)

                val diagnostics = session.documentDiagnostic(session.documentDiagnosticParams(uri))
                    .relatedFullDocumentDiagnosticReport.items
                    .filter { it.severity == DiagnosticSeverity.Error }
                assertTrue(
                    diagnostics.isEmpty(),
                    "补成 `return twice(2)` 之后不应有错误诊断，实际 ${diagnostics.map { it.message.left }}",
                )

                val declarationLine = lineOf(fixture, COMPLETION, "func twice")
                val caret = LspTextDocument(uri, "cangjie", 3, current).positionAt(prefixStart)
                val definition = session.definition(
                    DefinitionParams(TextDocumentIdentifier(uri), caret),
                ).let { if (it.isLeft) it.left.orEmpty() else emptyList() }
                assertTrue(
                    definition.any { location ->
                        location.uri == uri &&
                            location.range.start.line <= declarationLine &&
                            location.range.end.line >= declarationLine
                    },
                    "两次增量编辑之后 definition 必须仍落在 func twice（第 $declarationLine 行）：" +
                        "definition=$definition，caret=$caret，当前行=" +
                        "`${current.lineSequence().last { it.contains("return") }}`",
                )
            }
        }
    }

    /**
     * 格式化必须幂等：对已格式化的文本再格式化不得再产生编辑。
     *
     * 「保存时格式化」是编辑器的常规配置；不幂等意味着用户每保存一次文件就被改一次，
     * 版本控制里持续出现无意义 diff。
     */
    @Test
    fun `formatting is idempotent and keeps the document parseable`() {
        testDataWorkspace(FORMATTING, "realSemantics/formatting.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(FORMATTING)
                val text = fixture.text(FORMATTING)
                session.openDocument(uri, text, version = 1)

                val firstPass = session.formatting(
                    DocumentFormattingParams(TextDocumentIdentifier(uri), FormattingOptions(4, true)),
                )
                assertTrue(firstPass.isNotEmpty(), "未格式化的文本必须产生编辑")
                val formatted = LspEditApplicator.apply(text, firstPass, uri)
                assertTrue(
                    formatted != text,
                    "格式化结果必须与原文不同；若相同，说明格式化是空实现",
                )

                session.changeDocument(uri, formatted, version = 2)
                val secondPass = session.formatting(
                    DocumentFormattingParams(TextDocumentIdentifier(uri), FormattingOptions(4, true)),
                )
                assertTrue(
                    secondPass.isEmpty(),
                    "格式化必须幂等：第二次不得再产生编辑，实际 ${secondPass.size} 条——" +
                        "这会让「保存时格式化」每次都改一次文件",
                )

                // 幂等不等于正确：格式化后的文本必须仍能被解析出同一个符号。
                val symbols = session.documentSymbol(DocumentSymbolParams(TextDocumentIdentifier(uri)))
                val names = symbols.mapNotNull { symbol -> symbol.right?.name ?: symbol.left?.name }
                assertTrue(
                    names.contains("f"),
                    "格式化后必须仍能解析出函数 f，实际符号 $names",
                )
            }
        }
    }

    /**
     * 关闭文档必须清空它的诊断，且后续请求不得变成协议错误。
     *
     * 客户端不会为已关闭的文件保留诊断容器，服务端不清空就会留下「幽灵波浪线」；
     * 关闭后仍到达的请求（关闭与请求并发是常态）必须有正常响应，不能把服务端打挂。
     */
    @Test
    fun `closing a document clears its diagnostics and keeps serving requests`() {
        testDataWorkspace(BROKEN, "realSemantics/broken.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(BROKEN)
                session.openDocument(uri, fixture.text(BROKEN))

                val published = session.awaitPublishedDiagnostics(uri) { params ->
                    params.diagnostics.any { it.severity == DiagnosticSeverity.Error }
                }
                assertTrue(published.diagnostics.isNotEmpty(), "打开有错的文件后必须先发布错误诊断")

                session.clearPublishedDiagnostics()
                session.closeDocument(uri)

                val cleared = session.awaitPublishedDiagnostics(uri) { params -> params.diagnostics.isEmpty() }
                assertEquals(uri, cleared.uri)
                assertTrue(
                    cleared.diagnostics.isEmpty(),
                    "关闭文档必须发布空诊断，否则客户端会保留已关闭文件的波浪线",
                )

                // 关闭后到达的请求：允许返回空结果，不允许返回协议错误。
                val hover = runCatching { session.hover(HoverParams(TextDocumentIdentifier(uri), Position(0, 0))) }
                assertTrue(
                    hover.isSuccess,
                    "关闭文档后的 hover 不得变成协议错误：${hover.exceptionOrNull()}",
                )

                val definition = runCatching {
                    session.definition(DefinitionParams(TextDocumentIdentifier(uri), Position(0, 0)))
                }
                assertTrue(
                    definition.isSuccess,
                    "关闭文档后的 definition 不得变成协议错误：${definition.exceptionOrNull()}",
                )
                val locations: List<Location> = definition.getOrNull()?.let { result ->
                    if (result.isLeft) result.left.orEmpty() else emptyList()
                }.orEmpty()
                assertTrue(
                    locations.all { it.uri == uri },
                    "关闭文档后若仍给出位置，必须属于同一文件，实际 $locations",
                )
            }
        }
    }

    // ---------------------------------------------------------------- 夹具

    private companion object {
        const val EMOJI: String = "src/emoji.cj"
        const val FORMATTING: String = "src/format.cj"
        const val BROKEN: String = "src/broken.cj"
        const val COMPLETION: String = "src/doc.cj"
    }

    /** 带未完成标识符的单文件（`return twi`）。 */
    private fun completionWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .sourceFromTestData("doc.cj", "realSemantics/cdoc.cj")
        .build()

    /** 由 testData 单文件构造的工作区。 */
    private fun testDataWorkspace(targetRelativePath: String, testDataPath: String): LspWorkspaceFixture =
        LspWorkspaceFixtureBuilder()
            .fileFromTestData(targetRelativePath, testDataPath)
            .build()

    // ---------------------------------------------------------------- 断言辅助

    /** token 所在行号。 */
    private fun lineOf(fixture: LspWorkspaceFixture, relativePath: String, token: String): Int {
        val text = fixture.text(relativePath)
        val offset = text.indexOf(token)
        check(offset >= 0) { "在 $relativePath 中找不到 `$token`" }
        return LspTextDocument(fixture.uri(relativePath), "cangjie", 1, text).positionAt(offset).line
    }
}
