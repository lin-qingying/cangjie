package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.KNOWN_DEFECT_TAG
import org.cangnova.cangjie.lsp.testkit.LspEditApplicator
import org.cangnova.cangjie.lsp.testkit.LspTestData
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.FoldingRangeRequestParams
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.RenameParams
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * 真实语义下的健壮性：空文档、CRLF 坐标、重命名的边界。
 *
 * 这三类是最容易被「正常用例」掩盖的地方：
 * - 空文档会让「按位置取 PSI 元素」的代码拿到 null；
 * - CRLF 让「分析文本（LF）」与「协议文本（CRLF）」的坐标分叉；
 * - 重命名会碰到注释/字符串里的同名文本，以及与新名字冲突的情况。
 */
class CangjieRealSemanticsRobustnessTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：每个用例需要自己的临时工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 空文档上所有位置型请求都必须返回空结果，而不是抛错或给兜底内容。
     */
    @Test
    @Tag(KNOWN_DEFECT_TAG)
    fun `empty document answers every position based request`() {
        testDataWorkspace("src/empty.cj", "realSemantics/robustness/empty.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/empty.cj")
                session.openDocument(uri, "")

                val completion = session.completion(CompletionParams(TextDocumentIdentifier(uri), Position(0, 0)))
                val completionLabels = when {
                    completion.isLeft -> completion.left.orEmpty()
                    else -> completion.right?.items.orEmpty()
                }
                assertTrue(completionLabels.isEmpty(), "空文档不应给出候选，实际 ${completionLabels.map { it.label }}")

                val hover = session.hover(HoverParams(TextDocumentIdentifier(uri), Position(0, 0)))
                assertTrue(isEmptyHover(hover), "空文档 hover 必须为空，实际 $hover")

                val definition = session.definition(DefinitionParams(TextDocumentIdentifier(uri), Position(0, 0)))
                assertTrue(
                    definition.isLeft && definition.left.orEmpty().isEmpty(),
                    "空文档 definition 必须为空，实际 $definition",
                )

                val symbols = session.documentSymbol(DocumentSymbolParams(TextDocumentIdentifier(uri)))
                assertTrue(symbols.isEmpty(), "空文档不应有文档符号，实际 $symbols")

                val folding = session.foldingRange(FoldingRangeRequestParams(TextDocumentIdentifier(uri)))
                assertTrue(folding.isEmpty(), "空文档不应有折叠区域，实际 $folding")
            }
        }
    }

    /**
     * CRLF 文档：诊断行号与跳转位置必须按客户端原始文本（CRLF）计算，不能偏一行。
     *
     * 分析侧用 LF 规范化文本，协议侧用原始文本；两者换算错一格，整篇诊断就会错行。
     */
    @Test
    fun `crlf document keeps diagnostics and navigation on the same line`() {
        val source = LspTestData.text("realSemantics/robustness/crlf.cj")
        testDataWorkspace("src/crlf.cj", "realSemantics/robustness/crlf.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/crlf.cj")
                session.openDocument(uri, source)

                val brokenLine = LspTextDocument(uri, "cangjie", 1, source)
                    .positionAt(source.indexOf("return \"text\"")).line
                assertEquals(7, brokenLine, "测试前提：错误行应为 0 基第 7 行")

                val published = session.awaitPublishedDiagnostics(uri) { params ->
                    params.diagnostics.any { it.severity == DiagnosticSeverity.Error }
                }
                val error = published.diagnostics.first { it.severity == DiagnosticSeverity.Error }
                assertEquals(
                    brokenLine,
                    error.range.start.line,
                    "CRLF 文档的诊断必须落在原始文本的错误行（0 基 $brokenLine），实际 ${error.range.start.line}",
                )

                val anchorCall = positionOf(
                    source,
                    "return anchor()",
                    delta = "return ".length,
                )
                val definition = session.definition(DefinitionParams(TextDocumentIdentifier(uri), anchorCall))
                assertTrue(
                    definition.isLeft && definition.left.orEmpty().isNotEmpty(),
                    "CRLF 文档里的 definition 必须能解析，实际 $definition",
                )
                assertEquals(
                    2,
                    definition.left.first().range.start.line,
                    "definition 应指向 `func anchor` 所在行（0 基 2）",
                )
            }
        }
    }

    /**
     * 重命名不得改动注释与字符串里的同名文本（它们不是引用）。
     */
    @Test
    fun `rename leaves comments and string literals untouched`() {
        val source = LspTestData.text("realSemantics/robustness/rename.cj")
        testDataWorkspace("src/rename.cj", "realSemantics/robustness/rename.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/rename.cj")
                session.openDocument(uri, source)

                val edits = session.rename(
                    RenameParams(
                        TextDocumentIdentifier(uri),
                        positionOf(source, "func twice", delta = "func ".length),
                        "doubled",
                    ),
                )
                val applied = LspEditApplicator.apply(edits, mapOf(uri to source)).getValue(uri)

                assertTrue(applied.contains("func doubled("), "声明必须改名：$applied")
                assertTrue(applied.contains("return doubled(2)"), "调用点必须改名：$applied")
                assertTrue(
                    applied.contains("// twice appears in a comment"),
                    "注释里的同名文本不得被改：$applied",
                )
                assertTrue(
                    applied.contains("\"twice in a string\""),
                    "字符串字面量里的同名文本不得被改：$applied",
                )
            }
        }
    }

    /**
     * 重命名到已存在的符号名时，不得静默产出一份重复声明的代码。
     *
     * 允许两种正确行为：拒绝（请求失败或返回空编辑），或实现语义安全的冲突处理。
     * 唯一不允许的是「照改不误」——那会在用户文件里留下两个同名函数。
     */
    @Test
    @Tag(KNOWN_DEFECT_TAG)
    fun `rename to an existing symbol name never duplicates a declaration`() {
        val source = LspTestData.text("realSemantics/robustness/conflict.cj")
        testDataWorkspace("src/conflict.cj", "realSemantics/robustness/conflict.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/conflict.cj")
                session.openDocument(uri, source)

                val edits = runCatching {
                    session.rename(
                        RenameParams(
                            TextDocumentIdentifier(uri),
                            positionOf(source, "func first", delta = "func ".length),
                            "second",
                        ),
                    )
                }.getOrNull()

                val changes = edits?.changes.orEmpty()
                if (changes.isEmpty()) {
                    // 拒绝改名是正确行为之一。
                    return@use
                }

                val applied = LspEditApplicator.apply(edits!!, mapOf(uri to source)).getValue(uri)
                val duplicateDeclarations = Regex("""func second\(""").findAll(applied).count()
                assertEquals(
                    1,
                    duplicateDeclarations,
                    "改名后不得出现两个 `func second(`（文件已被破坏）：\n$applied",
                )
            }
        }
    }

    /**
     * 语法残缺的文件仍必须给出**已完整的**符号。
     *
     * 用户中途打字时文件常常是不完整的；此时若整份大纲消失，编辑器会表现为
     * 「结构树突然空掉」，比一个波浪线严重得多。这里用「少一个右括号」的文件验证：
     * 残缺位置之前的声明必须仍在符号列表里。
     */
    @Test
    fun `document symbols survive a syntax error`() {
        testDataWorkspace(PARTIAL, "realSemantics/robustness/syntax-error.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(PARTIAL)
                session.openDocument(uri, fixture.text(PARTIAL))

                val symbols = session.documentSymbol(DocumentSymbolParams(TextDocumentIdentifier(uri)))
                val names = flattenNames(symbols)
                assertTrue(
                    names.contains("Holder"),
                    "语法错误不得让已完整的类型声明从大纲里消失，实际 $names",
                )
                assertTrue(
                    names.contains("value"),
                    "语法错误不得让已完整的方法从大纲里消失，实际 $names",
                )
                assertTrue(
                    names.contains("dangling"),
                    "残缺位置之前的声明同样必须保留，实际 $names",
                )
            }
        }
    }

    /**
     * 语法错误必须作为诊断上报。
     *
     * 少一个右括号的文件在 `cjc` 里必然编译失败；服务端若一声不响，用户看到的是
     * 「编辑器全绿、构建报错」——这是最难自查的一类不一致。
     */
    @Test
    @Tag(KNOWN_DEFECT_TAG)
    fun `syntax errors surface as diagnostics`() {
        testDataWorkspace(PARTIAL, "realSemantics/robustness/syntax-error.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(PARTIAL)
                session.openDocument(uri, fixture.text(PARTIAL))

                val report = session.documentDiagnostic(session.documentDiagnosticParams(uri))
                    .relatedFullDocumentDiagnosticReport.items
                val rendered = report.map { "${it.severity} ${it.range.start.line}:${it.message.left}" }
                assertTrue(
                    report.any { it.severity == DiagnosticSeverity.Error },
                    "缺右括号的文件必须报语法错误，实际诊断 $rendered",
                )
            }
        }
    }

    // ---------------------------------------------------------------- 夹具

    private companion object {
        /** 语法残缺（缺右括号）的单文件相对路径。 */
        const val PARTIAL: String = "src/partial.cj"
    }

    /**
     * 由 testData 单文件构造的工作区。
     */
    private fun testDataWorkspace(
        targetRelativePath: String,
        testDataPath: String,
    ): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .fileFromTestData(targetRelativePath, testDataPath)
        .build()

    // ---------------------------------------------------------- 断言辅助

    /**
     * 展平符号层级（含 children）。
     */
    private fun flattenNames(symbols: List<org.eclipse.lsp4j.jsonrpc.messages.Either<org.eclipse.lsp4j.SymbolInformation, org.eclipse.lsp4j.DocumentSymbol>>): List<String> {
        val names = mutableListOf<String>()
        symbols.forEach { symbol ->
            if (symbol.isRight) {
                collectNames(symbol.right, names)
            } else {
                symbol.left?.name?.let(names::add)
            }
        }
        return names
    }

    /**
     * 递归收集符号名。
     */
    private fun collectNames(symbol: org.eclipse.lsp4j.DocumentSymbol?, destination: MutableList<String>) {
        if (symbol == null) return
        destination += symbol.name
        symbol.children.orEmpty().forEach { collectNames(it, destination) }
    }

    /**
     * hover 是否为空。
     */
    private fun isEmptyHover(hover: org.eclipse.lsp4j.Hover): Boolean = when {
        hover.contents == null -> true
        hover.contents.isLeft -> hover.contents.left.orEmpty().isEmpty()
        else -> hover.contents.right?.value.isNullOrBlank()
    }

    /**
     * 偏移转位置。
     */
    private fun positionOf(text: String, token: String, delta: Int = 0): Position {
        val offset = text.indexOf(token)
        check(offset >= 0) { "Cannot find `$token`" }
        return LspTextDocument("file:///robust.cj", "cangjie", 1, text).positionAt(offset + delta)
    }
}
