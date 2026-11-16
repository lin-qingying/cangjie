package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.KNOWN_DEFECT_TAG
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.CodeActionContext
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.CodeActionKind
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.SymbolKind
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * 展示层保真度：符号种类、空白位置的 hover、诊断与 code action 的关联信息。
 *
 * 这些字段不影响「能不能用」，但直接决定编辑器里看到的图标、分组和过滤：
 * 种类全错 → 大纲图标全一样；hover 在注释里也给内容 → 每个词都弹卡片；
 * code action 不带诊断 → 客户端无法把修复挂到对应波浪线上。
 */
class CangjieRealSemanticsPresentationTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：每个用例自带工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 类型符号的种类必须按声明形态区分，而不是一律 `Object`。
     */
    @Test
    fun `document symbols use the right kinds for types`() {
        structureWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(STRUCTURE)
                session.openDocument(uri, fixture.text(STRUCTURE))

                val symbols = session.documentSymbol(DocumentSymbolParams(TextDocumentIdentifier(uri)))
                val byName = symbols.mapNotNull { it.right }.associateBy { it.name }

                assertEquals(
                    SymbolKind.Interface,
                    byName["Shape"]?.kind,
                    "接口必须映射为 SymbolKind.Interface，实际 ${byName["Shape"]?.kind}（符号 ${byName.keys}）",
                )
                assertEquals(
                    SymbolKind.Class,
                    byName["Square"]?.kind,
                    "类必须映射为 SymbolKind.Class，实际 ${byName["Square"]?.kind}",
                )
            }
        }
    }

    /**
     * 类里的成员变量必须映射为 `SymbolKind.Field`，不是 `Variable`。
     *
     * `Variable` 的语义是局部/顶层变量；成员用 `Field`。客户端按 kind 过滤符号列表时
     * （例如「只看字段」），错误映射会让成员完全筛不出来。
     */
    @Test
    @Tag(KNOWN_DEFECT_TAG)
    fun `document symbols map class members to the field kind`() {
        structureWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(STRUCTURE)
                session.openDocument(uri, fixture.text(STRUCTURE))

                val symbols = session.documentSymbol(DocumentSymbolParams(TextDocumentIdentifier(uri)))
                val square = symbols.mapNotNull { it.right }.firstOrNull { it.name == "Square" }
                val side = square?.children?.firstOrNull { it.name == "side" }
                assertEquals(
                    SymbolKind.Field,
                    side?.kind,
                    "成员变量必须映射为 SymbolKind.Field，实际 ${square?.children?.map { it.name to it.kind }}",
                )
            }
        }
    }

    /**
     * 成员方法必须映射为 `SymbolKind.Method`，不是 `Function`。
     *
     * 与语义 token 的 D5 同源：成员与顶层函数在符号种类映射时没有区分。
     */
    @Test
    @Tag(KNOWN_DEFECT_TAG)
    fun `document symbols map member functions to the method kind`() {
        structureWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(STRUCTURE)
                session.openDocument(uri, fixture.text(STRUCTURE))

                val symbols = session.documentSymbol(DocumentSymbolParams(TextDocumentIdentifier(uri)))
                val square = symbols.mapNotNull { it.right }.firstOrNull { it.name == "Square" }
                val area = square?.children?.firstOrNull { it.name == "area" }
                assertEquals(
                    SymbolKind.Method,
                    area?.kind,
                    "成员方法必须映射为 SymbolKind.Method，实际 ${square?.children?.map { it.name to it.kind }}",
                )
            }
        }
    }

    /**
     * 顶层函数必须映射为 `SymbolKind.Function`。
     *
     * 与上一条分开：顶层函数与成员方法在 PSI 上是不同元素，映射错位通常只发生在其中一侧。
     */
    @Test
    fun `top level functions use the function kind`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(MODEL)
                session.openDocument(uri, fixture.text(MODEL))

                val symbols = session.documentSymbol(DocumentSymbolParams(TextDocumentIdentifier(uri)))
                val twice = symbols.mapNotNull { it.right }.firstOrNull { it.name == "twice" }
                assertEquals(
                    SymbolKind.Function,
                    twice?.kind,
                    "顶层函数必须映射为 SymbolKind.Function，实际 ${twice?.kind}",
                )
            }
        }
    }

    /**
     * 注释与字符串字面量里没有符号，hover 必须为空。
     *
     * 返回兜底内容（例如把所在行当成表达式）会让编辑器在鼠标划过每个词时都弹卡片。
     */
    @Test
    fun `hover inside comments and strings returns nothing`() {
        commentWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(FILE)
                val text = fixture.text(FILE)
                session.openDocument(uri, text)

                val comment = positionIn(text, uri, "// twice appears in a comment", "twice")
                val inString = positionIn(text, uri, "\"twice in a string\"", "twice")

                val commentHover = session.hover(HoverParams(TextDocumentIdentifier(uri), comment))
                assertTrue(
                    isEmptyHover(commentHover),
                    "注释里的 `twice` 不是符号引用，hover 必须为空，实际 ${render(commentHover)}",
                )

                val stringHover = session.hover(HoverParams(TextDocumentIdentifier(uri), inString))
                assertTrue(
                    isEmptyHover(stringHover),
                    "字符串字面量里的 `twice` 不是符号引用，hover 必须为空，实际 ${render(stringHover)}",
                )
            }
        }
    }

    /**
     * code action 必须带上触发它的诊断。
     *
     * 客户端（VS Code / IntelliJ）按 `CodeAction.diagnostics` 把修复挂到对应波浪线上、
     * 并据此过滤「只在有错的行上显示灯泡」。缺这个字段时，动作只能在「全文件动作」里出现，
     * 用户点不到。
     */
    @Test
    fun `code actions carry the diagnostic that triggered them`() {
        hostWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(HOST)
                val text = fixture.text(HOST)
                session.openDocument(uri, text)

                val requestRange = rangeOfToken(text, uri, "class Host")
                val actions = session.codeAction(
                    CodeActionParams(TextDocumentIdentifier(uri), requestRange, CodeActionContext(emptyList())),
                ).mapNotNull { it.right }

                assertTrue(actions.isNotEmpty(), "未实现成员必须给出 quick fix")
                actions.forEach { action ->
                    assertEquals(
                        CodeActionKind.QuickFix,
                        action.kind,
                        "该路径产出的动作必须声明为 quickfix，实际 ${action.kind}（${action.title}）",
                    )
                    val diagnostics = action.diagnostics.orEmpty()
                    assertTrue(
                        diagnostics.isNotEmpty(),
                        "code action `「${action.title}」` 必须带 diagnostics，否则客户端无法把它挂到波浪线上",
                    )
                    diagnostics.forEach { diagnostic ->
                        assertTrue(
                            overlaps(diagnostic.range, requestRange),
                            "动作携带的诊断必须与请求范围重叠，实际 ${diagnostic.range} vs $requestRange",
                        )
                    }
                }
            }
        }
    }

    /**
     * 候选的编辑**按设计**推迟到 `applyCompletion` 命令，因此 `textEdit` 必须是空操作。
     *
     * 这不是随便定的：服务端把「删掉已输入前缀 + 插入完整标识符 + 补 import」全部算在命令里
     * （`LspCompletionCommands` 的约定），客户端接受候选后回调命令再落盘。因此列表阶段给出的
     * `textEdit` 只能是 caret 处的空编辑。
     *
     * 这里断言这条契约本身，避免有人「顺手」把 `textEdit` 改成整行替换——那会和命令里的
     * 编辑叠加，产生重复插入。
     *
     * 兼容性代价（已知，写在这里以免被当成实现细节忽略）：只认 `textEdit`、不执行
     * `CompletionItem.command` 的客户端会「点了候选没反应」。真实插入的正确性由
     * `CangjieRealSemanticsCompletionApplyTest` 端到端覆盖。
     */
    @Test
    fun `completion candidates defer the edit to the apply command`() {
        completionWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(COMPLETION)
                val text = fixture.text(COMPLETION)
                session.openDocument(uri, text)

                val document = LspTextDocument(uri, "cangjie", 1, text)
                val prefixOffset = text.indexOf("return twi") + "return ".length
                val caret = document.positionAt(prefixOffset + "twi".length)

                val completion = session.completion(
                    org.eclipse.lsp4j.CompletionParams(TextDocumentIdentifier(uri), caret),
                )
                val items = if (completion.isLeft) completion.left.orEmpty() else completion.right?.items.orEmpty()
                val item = items.firstOrNull { it.label == "twice" }
                org.junit.jupiter.api.Assertions.assertNotNull(
                    item,
                    "候选里应有 `twice`，实际前 20 项：${items.take(20).map { it.label }}",
                )

                val textEdit = item!!.textEdit?.let { either ->
                    if (either.isLeft) {
                        either.left
                    } else {
                        either.right?.let { replace -> org.eclipse.lsp4j.TextEdit(replace.insert, replace.newText) }
                    }
                }
                org.junit.jupiter.api.Assertions.assertNotNull(
                    textEdit,
                    "候选必须给出 `textEdit`（协议要求二选一：textEdit / insertText）：item=$item",
                )
                assertEquals(
                    caret,
                    textEdit!!.range.start,
                    "空操作的 `textEdit` 必须落在 caret 上，实际 ${textEdit.range}",
                )
                assertEquals(
                    textEdit.range.start,
                    textEdit.range.end,
                    "`textEdit` 必须是零宽插入（真正的编辑在命令里），实际 ${textEdit.range}",
                )
                assertEquals(
                    "",
                    textEdit.newText,
                    "`textEdit.newText` 必须为空，否则会与命令里的编辑叠加",
                )
                assertEquals(
                    text,
                    org.cangnova.cangjie.lsp.testkit.LspEditApplicator.apply(text, listOf(textEdit), uri),
                    "只应用 `textEdit` 时文本不得改变（设计要求由命令落盘）",
                )

                val command = item.command
                org.junit.jupiter.api.Assertions.assertNotNull(
                    command,
                    "推迟编辑的前提是候选必须带 `applyCompletion` 命令，否则客户端无从插入：item=$item",
                )
                assertEquals(
                    org.cangnova.cangjie.lsp.completion.LspCompletionCommands.APPLY_COMPLETION,
                    command!!.command,
                    "命令必须是 `applyCompletion`",
                )
                org.junit.jupiter.api.Assertions.assertNotNull(
                    command.arguments,
                    "命令必须带 `[sessionKey, index]` 实参，否则服务端无法定位候选",
                )
            }
        }
    }

    /**
     * 折叠范围不得退化：`startLine == endLine == endCharacter` 这类范围会让客户端画出 0 行折叠区。
     */
    @Test
    fun `folding ranges are never degenerate`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(MODEL)
                session.openDocument(uri, fixture.text(MODEL))

                val ranges = session.foldingRange(
                    org.eclipse.lsp4j.FoldingRangeRequestParams(TextDocumentIdentifier(uri)),
                )
                assertTrue(ranges.isNotEmpty(), "语义工作区里应有可折叠区域，实际为空")
                ranges.forEach { range ->
                    val ordered = range.startLine < range.endLine ||
                        (range.startLine == range.endLine &&
                            (range.startCharacter ?: 0) < (range.endCharacter ?: 0))
                    assertTrue(
                        ordered,
                        "折叠范围必须非退化（起始严格在结束之前）：" +
                            "${range.startLine}:${range.startCharacter} - ${range.endLine}:${range.endCharacter}",
                    )
                }
            }
        }
    }

    /**
     * 单行注释不应产生折叠范围。
     */
    @Test
    fun `single line comments are not foldable`() {
        commentWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(FILE)
                val text = fixture.text(FILE)
                session.openDocument(uri, text)

                val commentLine = text.lineSequence()
                    .indexOfFirst { it.trimStart().startsWith("//") }
                check(commentLine >= 0) { "测试数据里应有单行注释" }

                val ranges = session.foldingRange(
                    org.eclipse.lsp4j.FoldingRangeRequestParams(TextDocumentIdentifier(uri)),
                )
                assertTrue(
                    ranges.none { it.startLine == commentLine || it.endLine == commentLine },
                    "单行注释（第 $commentLine 行）不得参与折叠，实际 " +
                        "${ranges.map { "${it.startLine}-${it.endLine}" }}",
                )
            }
        }
    }

    /**
     * 诊断必须带 `source`，客户端按它分组、按它决定「清除哪些旧诊断」。
     *
     * 没有 source 的诊断会在编辑器里堆成来源不明的条目，且无法在配置里按来源开关。
     */
    @Test
    fun `diagnostics carry the advertised source`() {
        brokenWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(BROKEN)
                session.openDocument(uri, fixture.text(BROKEN))

                val diagnostics = session.documentDiagnostic(session.documentDiagnosticParams(uri))
                    .relatedFullDocumentDiagnosticReport.items
                assertTrue(diagnostics.isNotEmpty(), "有错的文件必须给出诊断")

                val expectedSource = "cangjie"
                diagnostics.forEach { diagnostic ->
                    assertEquals(
                        expectedSource,
                        diagnostic.source,
                        "诊断的 `source` 必须是服务端宣告的标识符 `$expectedSource`，实际 ${diagnostic.source}",
                    )
                }
                assertTrue(
                    diagnostics.any { it.code != null },
                    "诊断必须带 `code`（客户端据此做「同类错误批量抑制」），实际全部为空",
                )
            }
        }
    }

    // ---------------------------------------------------------------- 夹具

    private companion object {
        const val MODEL: String = "src/model.cj"
        const val STRUCTURE: String = "src/structure.cj"
        const val HOST: String = "src/host.cj"
        const val FILE: String = "src/rename.cj"
        const val COMPLETION: String = "src/doc.cj"
        const val BROKEN: String = "src/broken.cj"
    }

    /** 接口 / 实现类 / 字段 / 成员方法的单文件。 */
    private fun structureWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .sourceFromTestData("structure.cj", "realSemantics/structure.cj")
        .build()

    /** 两文件语义工作区。 */
    private fun semanticWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .sourceFromTestData("model.cj", "realSemantics/model.cj")
        .sourceFromTestData("use.cj", "realSemantics/use.cj")
        .build()

    /** 带注释与字符串字面量的单文件。 */
    private fun commentWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .sourceFromTestData("rename.cj", "realSemantics/robustness/rename.cj")
        .build()

    /** 带未完成标识符的单文件（`return twi`）。 */
    private fun completionWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .sourceFromTestData("doc.cj", "realSemantics/cdoc.cj")
        .build()

    /** 带类型错误的单文件。 */
    private fun brokenWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .sourceFromTestData("broken.cj", "realSemantics/broken.cj")
        .build()

    /** 未实现成员（可修复）的单文件。 */
    private fun hostWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .sourceFromTestData("host.cj", "realSemantics/host.cj")
        .build()

    // ---------------------------------------------------------------- 断言辅助

    /** hover 是否为空。 */
    private fun isEmptyHover(hover: org.eclipse.lsp4j.Hover): Boolean = when {
        hover.contents == null -> true
        hover.contents.isLeft -> hover.contents.left.orEmpty().isEmpty() ||
            hover.contents.left.orEmpty().all { it.left.isNullOrBlank() && it.right == null }

        else -> hover.contents.right?.value.isNullOrBlank()
    }

    /** hover 的可读描述。 */
    private fun render(hover: org.eclipse.lsp4j.Hover): String = when {
        hover.contents == null -> "null"
        hover.contents.isLeft -> hover.contents.left.orEmpty().joinToString("|") {
            if (it.isLeft) it.left.orEmpty() else it.right?.value.orEmpty()
        }

        else -> hover.contents.right?.value.orEmpty()
    }

    /** 命中 `anchor` 所在行内 `token` 的位置。 */
    private fun positionIn(text: String, uri: String, anchor: String, token: String): Position {
        val anchorOffset = text.indexOf(anchor)
        check(anchorOffset >= 0) { "找不到 `$anchor`" }
        val offset = text.indexOf(token, anchorOffset)
        check(offset in anchorOffset until anchorOffset + anchor.length) { "`$anchor` 里找不到 `$token`" }
        return LspTextDocument(uri, "cangjie", 1, text).positionAt(offset)
    }

    /** 文本里某段字面量覆盖的范围。 */
    private fun rangeOfToken(text: String, uri: String, token: String): Range {
        val start = text.indexOf(token)
        check(start >= 0) { "找不到 `$token`" }
        val document = LspTextDocument(uri, "cangjie", 1, text)
        return Range(document.positionAt(start), document.positionAt(start + token.length))
    }

    /** 两个范围是否重叠。 */
    private fun overlaps(first: Range, second: Range): Boolean =
        !positionAfter(first.start, second.end) && !positionAfter(second.start, first.end)

    /** `first` 是否严格在 `second` 之后。 */
    private fun positionAfter(first: Position, second: Position): Boolean =
        first.line > second.line || (first.line == second.line && first.character > second.character)
}
