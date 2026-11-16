package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.KNOWN_DEFECT_TAG
import org.cangnova.cangjie.lsp.testkit.LspTestData
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.InlayHint
import org.eclipse.lsp4j.InlayHintParams
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior
import org.eclipse.lsp4j.PrepareRenameParams
import org.eclipse.lsp4j.PrepareRenameResult
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.RenameParams
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import org.eclipse.lsp4j.WorkspaceFoldersChangeEvent
import org.eclipse.lsp4j.WorkspaceSymbolParams
import org.eclipse.lsp4j.jsonrpc.messages.Either3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * 真实语义下的**边界与二次请求**行为。
 *
 * 与 `CangjieRealSemanticsCapabilityTest` 互补：那边验证「一次请求的结果对不对」，
 * 这里验证「延迟补全（resolve）、不可重命名、工作区后加目录、诊断增量」这些
 * 只有在真实 Analysis 上才会暴露的路径。
 */
class CangjieRealSemanticsBoundaryTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：每个用例需要自己的临时工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * `completionItem/resolve` 必须补齐真实 CDoc 文档。
     *
     * 这条覆盖的是「候选 → 会话 → 重进分析 → 取 CDoc → 渲染 Markdown」整条链路；
     * 只断言「resolve 返回了候选」会让整条链路没被验证。
     */
    @Test
    fun `completion resolve fills documentation from cdoc`() {
        testDataWorkspace("src/doc.cj", "realSemantics/cdoc.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/doc.cj")
                val text = fixture.text("src/doc.cj")
                session.openDocument(uri, text)

                val caret = positionAt(text, text.indexOf("return twi") + "return twi".length)
                val completion = session.completion(CompletionParams(TextDocumentIdentifier(uri), caret))
                val items = when {
                    completion.isLeft -> completion.left.orEmpty()
                    else -> completion.right?.items.orEmpty()
                }
                val twice = items.firstOrNull { it.label == "twice" }
                assertNotNull(twice, "补全必须给出 twice，实际 ${items.map { it.label }.take(20)}")

                val resolved = session.resolveCompletionItem(twice!!)
                val documentation = documentationOf(resolved)
                assertTrue(
                    documentation.contains("Doubles the given value"),
                    "resolve 必须把 CDoc 渲染进 documentation，实际：`$documentation`",
                )
                assertEquals("twice", resolved.label, "resolve 不得改动候选名")
            }
        }
    }

    /**
     * `inlayHint/resolve` 不能让提示变得不可用。
     *
     * 当前实现是「列表阶段就渲染完，resolve 原样返回」；这条用例固定该契约：
     * 无论是否补齐，label 必须保持非空、位置必须不变。
     */
    @Test
    fun `inlay hint resolve keeps the hint usable`() {
        renameWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(USE)
                val text = fixture.text(USE)
                session.openDocument(uri, text)

                val line = lineOf(text, "let square = Square()")
                val hints = session.inlayHint(
                    InlayHintParams(TextDocumentIdentifier(uri), Range(Position(line, 0), Position(line, 200))),
                )
                assertFalse(hints.isEmpty(), "`let square = Square()` 上应有类型提示")

                val hint = hints.first()
                val resolved = session.resolveInlayHint(hint)
                assertEquals(hint.position, resolved.position, "resolve 不得移动提示位置")
                assertTrue(labelOf(resolved).isNotBlank(), "resolve 后 label 必须仍然非空")
            }
        }
    }

    /**
     * `prepareRename` 只能给出「确实可改名的范围」，不能返回与请求位置无关的伪范围。
     *
     * LSP 规定不可重命名时结果为空（null）；服务端用 `Range(0,0)` 当哨兵会让客户端
     * 在一个完全无关的位置弹出重命名输入框。这条用例按协议语义断言。
     */
    @Test
    @Tag(KNOWN_DEFECT_TAG)
    fun `prepare rename only reports ranges that cover the request position`() {
        renameWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val modelUri = fixture.uri(MODEL)
                val modelText = fixture.text(MODEL)
                session.openDocument(modelUri, modelText)

                val identifier = positionOf(modelText, "func twice", delta = 5)
                val identifierResult = session.prepareRename(PrepareRenameParams(TextDocumentIdentifier(modelUri), identifier))
                val identifierRange = rangeOf(identifierResult)
                assertNotNull(
                    identifierRange,
                    "可重命名的标识符必须给出范围；实际分支 first=${identifierResult.isFirst} " +
                        "second=${identifierResult.isSecond} third=${identifierResult.isThird} 结果=$identifierResult",
                )
                assertTrue(
                    covers(identifierRange!!, identifier),
                    "可重命名范围必须覆盖请求位置 $identifier，实际 $identifierRange",
                )

                val keyword = positionOf(modelText, "class Square", delta = 1)
                val keywordRange = rangeOf(
                    session.prepareRename(PrepareRenameParams(TextDocumentIdentifier(modelUri), keyword)),
                )
                if (keywordRange != null) {
                    assertTrue(
                        covers(keywordRange, keyword),
                        "不可重命名位置必须返回空结果；当前返回了与请求无关的范围 $keywordRange" +
                            "（请求位置 $keyword），客户端会在此弹出重命名框",
                    )
                }
            }
        }
    }

    /**
     * 不可重命名位置上的 `rename` 必须不产生编辑。
     */
    @Test
    fun `rename on a non renamable position produces no edits`() {
        renameWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val modelUri = fixture.uri(MODEL)
                val modelText = fixture.text(MODEL)
                session.openDocument(modelUri, modelText)

                val keyword = positionOf(modelText, "class Square", delta = 1)
                val edit = session.rename(RenameParams(TextDocumentIdentifier(modelUri), keyword, "Renamed"))
                assertTrue(
                    edit.changes.isNullOrEmpty(),
                    "关键字位置不得产生重命名编辑，实际 ${edit.changes}",
                )
            }
        }
    }

    /**
     * 后加入的工作区目录必须能被工作区符号检索到。
     *
     * 这条覆盖「didChangeWorkspaceFolders → 项目结构重建 → 模块注册」整条链路；
     * 只断言「通知没报错」等于没测。
     */
    @Test
    @Tag(KNOWN_DEFECT_TAG)
    fun `workspace folder added later becomes searchable`() {
        val first = renameWorkspace()
        val second = testDataWorkspace("src/later.cj", "realSemantics/incremental/later.cj")

        try {
            createSession(
                first.initializeParams(workspaceFolders = listOf(first.workspaceFolder())),
            ).use { session ->
                session.didChangeWorkspaceFolders(
                    DidChangeWorkspaceFoldersParams(
                        WorkspaceFoldersChangeEvent(listOf(second.workspaceFolder()), emptyList()),
                    ),
                )

                val symbols = session.workspaceSymbol(WorkspaceSymbolParams("LaterType"))
                val names = when {
                    symbols.isRight -> symbols.right.orEmpty().map { it.name }
                    else -> symbols.left.orEmpty().map { it.name }
                }
                assertTrue(
                    names.contains("LaterType"),
                    "后加入的目录里的 LaterType 必须可被 workspace/symbol 检索到，实际 $names",
                )
            }
        } finally {
            first.close()
            second.close()
        }
    }

    /**
     * 未提供 multiModuleOption 时，后加入的目录必须按「目录即模块」降级后可用。
     *
     * 与上一条的区别：上一条初始化时给了 multiModuleOption（真实客户端的常态），
     * 这条不给——用来区分「新增目录完全不支持」和「只在多模块配置下不支持」。
     */
    @Test
    fun `workspace folder added later degrades to a module when no multi module option was given`() {
        val first = renameWorkspace()
        val second = testDataWorkspace("src/degraded.cj", "realSemantics/incremental/degraded.cj")

        try {
            createSession(
                first.initializeParams(
                    workspaceFolders = listOf(first.workspaceFolder()),
                    includeMultiModuleOption = false,
                ),
            ).use { session ->
                session.didChangeWorkspaceFolders(
                    DidChangeWorkspaceFoldersParams(
                        WorkspaceFoldersChangeEvent(listOf(second.workspaceFolder()), emptyList()),
                    ),
                )

                val symbols = session.workspaceSymbol(WorkspaceSymbolParams("DegradedType"))
                val names = when {
                    symbols.isRight -> symbols.right.orEmpty().map { it.name }
                    else -> symbols.left.orEmpty().map { it.name }
                }
                assertTrue(
                    names.contains("DegradedType"),
                    "无多模块配置时，新增目录应按目录即模块降级并参与检索，实际 $names",
                )
            }
        } finally {
            first.close()
            second.close()
        }
    }

    /**
     * 同一文档的诊断必须随内容增量更新，而不是只发一次。
     */
    @Test
    fun `diagnostics refresh incrementally on the same document`() {
        val broken = LspTestData.text("realSemantics/incremental/pull-broken.cj")
        val fixed = LspTestData.text("realSemantics/incremental/pull-fixed.cj")
        val brokenAgain = LspTestData.text("realSemantics/incremental/pull-broken-again.cj")

        testDataWorkspace("src/refresh.cj", "realSemantics/incremental/pull-broken.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/refresh.cj")
                session.openDocument(uri, broken)

                val firstErrorLine = LspTextDocument(uri, "cangjie", 1, broken)
                    .positionAt(broken.indexOf("return \"text\"")).line
                session.awaitPublishedDiagnostics(uri) { params ->
                    params.diagnostics.any { it.severity == DiagnosticSeverity.Error && it.range.start.line == firstErrorLine }
                }

                session.changeDocument(
                    DidChangeTextDocumentParams(
                        VersionedTextDocumentIdentifier(uri, 2),
                        listOf(TextDocumentContentChangeEvent(fixed)),
                    ),
                )
                session.clearPublishedDiagnostics()
                session.awaitPublishedDiagnostics(uri) { params -> params.diagnostics.isEmpty() }

                session.changeDocument(
                    DidChangeTextDocumentParams(
                        VersionedTextDocumentIdentifier(uri, 3),
                        listOf(TextDocumentContentChangeEvent(brokenAgain)),
                    ),
                )
                val secondErrorLine = LspTextDocument(uri, "cangjie", 3, brokenAgain)
                    .positionAt(brokenAgain.indexOf("return \"still wrong\"")).line
                val published = session.awaitPublishedDiagnostics(uri) { params ->
                    params.diagnostics.any {
                        it.severity == DiagnosticSeverity.Error && it.range.start.line == secondErrorLine
                    }
                }

                assertTrue(
                    published.diagnostics.none { it.range.start.line == firstErrorLine },
                    "修复后的旧错误必须消失，实际 ${published.diagnostics.map { it.range.start.line }}",
                )
            }
        }
    }

    // ---------------------------------------------------------------- 夹具

    private companion object {
        const val MODEL: String = "src/model.cj"
        const val USE: String = "src/use.cj"
    }

    /**
     * 两文件工作区（与能力用例共用同一份 testData，避免夹具各写一份后漂移）。
     */
    private fun renameWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .sourceFromTestData("model.cj", "realSemantics/model.cj")
        .sourceFromTestData("use.cj", "realSemantics/use.cj")
        .build()

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
     * 取出候选的文档文本（Markdown 与纯文本都还原成字符串）。
     */
    private fun documentationOf(item: CompletionItem): String {
        val documentation = item.documentation ?: return ""
        return if (documentation.isLeft) {
            documentation.left.orEmpty()
        } else {
            (documentation.right as? MarkupContent)?.value.orEmpty()
        }
    }

    /**
     * 取出 prepareRename 结果里的范围；不可重命名（空/默认行为）返回 null。
     */
    private fun rangeOf(
        result: Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>,
    ): Range? = when {
        result.isFirst -> result.first
        result.isSecond -> result.second?.range
        // third 是 `defaultBehavior`：规范允许客户端自行处理，视为「没有具体范围」。
        else -> null
    }

    /**
     * 判断范围是否覆盖指定位置。
     */
    private fun covers(range: Range, position: Position): Boolean =
        !after(range.start, position) && !after(position, range.end)

    /**
     * 位置先后比较。
     */
    private fun after(left: Position, right: Position): Boolean =
        left.line > right.line || (left.line == right.line && left.character > right.character)

    /**
     * 内联提示文本。
     */
    private fun labelOf(hint: InlayHint): String =
        if (hint.label.isLeft) {
            hint.label.left.orEmpty()
        } else {
            hint.label.right.orEmpty().joinToString("") { part -> part.value }
        }

    /**
     * 文本中指定 token 首次出现处（可加 [delta]）的位置。
     */
    private fun positionOf(
        text: String,
        token: String,
        delta: Int = 0,
    ): Position = positionAt(text, text.indexOf(token) + delta)

    /**
     * 文本中指定 token 首次出现处的行号。
     */
    private fun lineOf(text: String, token: String): Int = positionOf(text, token).line

    /**
     * 偏移转位置。
     */
    private fun positionAt(text: String, offset: Int): Position =
        LspTextDocument("file:///boundary.cj", "cangjie", 1, text).positionAt(offset)
}
