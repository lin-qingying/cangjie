package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.KNOWN_DEFECT_TAG
import org.cangnova.cangjie.lsp.testkit.LspEditApplicator
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.CodeActionContext
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.DeclarationParams
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.DocumentHighlightParams
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.FoldingRangeRequestParams
import org.eclipse.lsp4j.FormattingOptions
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.ImplementationParams
import org.eclipse.lsp4j.InlayHint
import org.eclipse.lsp4j.InlayHintParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.ReferenceContext
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.RenameParams
import org.eclipse.lsp4j.SelectionRange
import org.eclipse.lsp4j.SelectionRangeParams
import org.eclipse.lsp4j.SemanticTokens
import org.eclipse.lsp4j.SemanticTokensParams
import org.eclipse.lsp4j.SignatureHelpParams
import org.eclipse.lsp4j.SymbolKind
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.TypeDefinitionParams
import org.eclipse.lsp4j.WorkspaceSymbolParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * 真实 Analysis 语义下的能力行为断言。
 *
 * 与契约矩阵的分工必须说清：矩阵用可记录的**假 facade**验证协议路由，只能证明
 * 「请求到得了适配层」；本类用**生产 Analysis facade + 临时仓颉工作区**验证
 * 「适配层给出的语义结果对不对」。两者不可互相替代。
 *
 * 断言原则：落到具体内容或具体位置（符号名、目标行、token 覆盖的文本、应用编辑后的文本），
 * 不用 `toString().contains(...)` 这类对空实现也能过的写法；也不断言未定义行为。
 */
class CangjieRealSemanticsCapabilityTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：每个用例需要带真实源码的工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 导航必须指向声明的真实位置，而不是「返回了非空位置列表」。
     */
    @Test
    fun `navigation resolves to the real declaration sites`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val modelUri = fixture.uri(MODEL)
                val useUri = fixture.uri(USE)
                session.openDocument(modelUri, fixture.text(MODEL))
                session.openDocument(useUri, fixture.text(USE))

                val twiceCall = positionOf(fixture, USE, "twice(", delta = 1)
                val squareUse = positionOf(fixture, USE, "square.area", delta = 0)
                val shapeDecl = positionOf(fixture, MODEL, "interface Shape", delta = 10)

                val definition = locations(session.definition(DefinitionParams(TextDocumentIdentifier(useUri), twiceCall)))
                assertTrue(
                    definition.any { it.uri == modelUri && it.range.start.line == lineOf(fixture, MODEL, "func twice") },
                    "definition 应指向 model.cj 的 func twice，实际 $definition",
                )

                val declaration = locations(session.declaration(DeclarationParams(TextDocumentIdentifier(useUri), twiceCall)))
                assertTrue(
                    declaration.any { it.uri == modelUri && it.range.start.line == lineOf(fixture, MODEL, "func twice") },
                    "declaration 应指向 model.cj 的 func twice，实际 $declaration",
                )

                val typeDefinition = locations(
                    session.typeDefinition(TypeDefinitionParams(TextDocumentIdentifier(useUri), squareUse)),
                )
                assertTrue(
                    typeDefinition.any { it.uri == modelUri && it.range.start.line == lineOf(fixture, MODEL, "class Square") },
                    "typeDefinition 应指向 class Square，实际 $typeDefinition",
                )

                val implementation = locations(
                    session.implementation(ImplementationParams(TextDocumentIdentifier(modelUri), shapeDecl)),
                )
                assertTrue(
                    implementation.isNotEmpty(),
                    "接口 Shape 必须能跳转到实现（Square / area）",
                )
                assertTrue(
                    implementation.all { it.uri == modelUri },
                    "实现跳转不应跑出同模块文件，实际 $implementation",
                )
                assertTrue(
                    implementation.any { it.range.start.line >= lineOf(fixture, MODEL, "class Square") },
                    "实现跳转应落在 Square 之后，实际 $implementation",
                )
            }
        }
    }

    /**
     * 编辑辅助必须给出与符号一致的内容，而不是空壳。
     */
    @Test
    fun `editor assist content matches the symbol under the cursor`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(USE)
                session.openDocument(uri, fixture.text(USE))

                val hover = session.hover(HoverParams(TextDocumentIdentifier(uri), positionOf(fixture, USE, "twice(", delta = 1)))
                val hoverText = hoverText(hover)
                assertTrue(hoverText.contains("twice"), "hover 必须描述被悬停的符号 twice，实际：$hoverText")
                assertTrue(hoverText.contains("Int64"), "hover 必须给出返回类型 Int64，实际：$hoverText")

                val callCharacter = positionOf(fixture, USE, "twice(", delta = 6)
                val signature = session.signatureHelp(SignatureHelpParams(TextDocumentIdentifier(uri), callCharacter))
                assertTrue(signature.signatures.isNotEmpty(), "调用括号内必须给出签名帮助")
                val signatureLabel = signature.signatures.first().label
                assertTrue(
                    signatureLabel.contains("twice") && signatureLabel.contains("Int64"),
                    "签名必须来自真实声明 twice(value: Int64)，实际：$signatureLabel",
                )

                // caret 必须在 `.` **之后**：在点上取补全只会得到局部变量。
            }
        }
    }

    /**
     * 限定名成员补全：`square.` 之后必须列出 Square 的成员。
     *
     * 这条与「标识符前缀补全」是两条不同路径：前者要做 PSI 占位符替换 + 重排。
     * 只测前缀补全会让成员补全路径完全没有覆盖。
     */
    @Test
    @Tag(KNOWN_DEFECT_TAG)
    fun `member completion after a dot lists the members`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(USE)
                session.openDocument(uri, fixture.text(USE))

                val dot = positionOf(fixture, USE, "square.area", delta = "square.".length)
                val completion = session.completion(CompletionParams(TextDocumentIdentifier(uri), dot))
                val labels = when {
                    completion.isLeft -> completion.left.orEmpty().map { it.label }
                    else -> completion.right?.items.orEmpty().map { it.label }
                }
                assertTrue(
                    labels.contains("area"),
                    "Square 实例的成员补全必须包含 area，实际前 20 项：${labels.take(20)}",
                )
                assertTrue(
                    labels.contains("side"),
                    "Square 实例的成员补全必须包含 side，实际前 20 项：${labels.take(20)}",
                )
            }
        }
    }

    /**
     * 符号与高亮必须覆盖真实声明，且位置落在本文件内。
     */
    @Test
    fun `symbols and highlights describe the real declarations`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val modelUri = fixture.uri(MODEL)
                session.openDocument(modelUri, fixture.text(MODEL))

                val symbols = session.documentSymbol(DocumentSymbolParams(TextDocumentIdentifier(modelUri)))
                val names = flattenSymbolNames(symbols)
                listOf("Shape", "Square", "twice").forEach { expected ->
                    assertTrue(names.contains(expected), "documentSymbol 缺少 $expected，实际 $names")
                }

                val areaSymbol = findSymbol(symbols, "area")
                assertNotNull(areaSymbol, "documentSymbol 必须包含成员 area，实际 $names")
                assertTrue(
                    contains(areaSymbol!!.range, areaSymbol.selectionRange),
                    "符号的 selectionRange 必须落在自身 range 内：${areaSymbol.range} / ${areaSymbol.selectionRange}",
                )
                assertTrue(
                    areaSymbol.kind == SymbolKind.Method || areaSymbol.kind == SymbolKind.Function,
                    "area 应映射为方法/函数，实际 ${areaSymbol.kind}",
                )

                val sideHighlights = session.documentHighlight(
                    DocumentHighlightParams(
                        TextDocumentIdentifier(modelUri),
                        positionOf(fixture, MODEL, "var side", delta = 4),
                    ),
                )
                assertTrue(
                    sideHighlights.size >= 2,
                    "字段 side 有声明 + 一处使用，高亮至少 2 处，实际 ${sideHighlights.size}",
                )
                assertTrue(
                    sideHighlights.all { it.range.start.line >= lineOf(fixture, MODEL, "class Square") },
                    "side 的高亮必须落在 Square 内，实际 ${sideHighlights.map { it.range }}",
                )

                val workspaceSymbols = session.workspaceSymbol(WorkspaceSymbolParams("Square"))
                val workspaceNames = when {
                    workspaceSymbols.isRight -> workspaceSymbols.right.orEmpty().map { it.name }
                    else -> workspaceSymbols.left.orEmpty().map { it.name }
                }
                assertTrue(
                    workspaceNames.contains("Square"),
                    "workspace/symbol 必须查到 Square，实际 $workspaceNames",
                )
            }
        }
    }

    /**
     * 语义 token 必须覆盖到正确的**文本**，而不只是「data 非空」。
     */
    @Test
    fun `semantic tokens cover the right source text`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val modelUri = fixture.uri(MODEL)
                val text = fixture.text(MODEL)
                session.openDocument(modelUri, text)

                val legend = requireNotNull(
                    session.initializeResult().capabilities.semanticTokensProvider?.legend,
                )
                val tokens = session.semanticTokensFull(SemanticTokensParams(TextDocumentIdentifier(modelUri)))
                val decoded = decode(tokens, legend.tokenTypes, legend.tokenModifiers, text)

                // 同名 token 会同时出现在声明与引用处（interface/type、class/type），
                // 因此按「声明所在行」定位声明 token，再断言它的类型。
                assertTokenType(decoded, "Square", lineOf(fixture, MODEL, "class Square"), "class")
                assertTokenType(decoded, "twice", lineOf(fixture, MODEL, "func twice"), "function")
                assertTokenType(decoded, "Shape", lineOf(fixture, MODEL, "interface Shape"), "interface")
                // 成员方法声明当前被着色为 function；是否该用 method 见
                // `member function declarations are coloured as methods`（独立红灯）。
                assertTokenLike(decoded, "area", lineOf(fixture, MODEL, "func area"), setOf("method", "function"))

                decoded.forEach { token ->
                    assertTrue(token.length > 0, "token 长度必须为正：$token")
                    assertTrue(
                        token.line in 0 until text.lineSequence().count(),
                        "token 行号越界：$token",
                    )
                }
            }
        }
    }

    /**
     * 格式化必须产出可应用、可预测的文本。
     */
    @Test
    fun `formatting produces applicable text`() {
        testDataWorkspace("src/format.cj", "realSemantics/formatting.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/format.cj")
                val text = fixture.text("src/format.cj")
                session.openDocument(uri, text)

                val edits = session.formatting(
                    DocumentFormattingParams(TextDocumentIdentifier(uri), FormattingOptions(2, true)),
                )
                assertFalse(edits.isEmpty(), "格式化必须返回编辑")

                val applied = LspEditApplicator.apply(text, edits, uri)
                assertTrue(applied.contains("func f() {"), "格式化后函数体左括号必须前移：$applied")
                assertTrue(applied.contains("  let x = 1 + 2"), "格式化后赋值必须补空格并保持两空格缩进：$applied")
                assertTrue(applied.contains("\n}"), "格式化后右括号必须独占一行：$applied")
            }
        }
    }

    /**
     * 重命名的编辑应用到源文本后，声明与所有使用都必须改名。
     */
    @Test
    fun `rename edits every declaration and use when applied`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val modelUri = fixture.uri(MODEL)
                val useUri = fixture.uri(USE)
                val modelText = fixture.text(MODEL)
                val useText = fixture.text(USE)
                session.openDocument(modelUri, modelText)
                session.openDocument(useUri, useText)

                val edits = session.rename(
                    RenameParams(
                        TextDocumentIdentifier(modelUri),
                        positionOf(fixture, MODEL, "func twice", delta = 6),
                        "doubled",
                    ),
                )
                assertNotNull(edits.changes, "重命名必须返回 changes 编辑")
                assertTrue(
                    edits.changes.containsKey(modelUri),
                    "重命名必须包含声明所在文件，实际 ${edits.changes.keys}",
                )

                val applied = LspEditApplicator.apply(edits, mapOf(modelUri to modelText, useUri to useText))
                val appliedModel = applied.getValue(modelUri)
                assertTrue(appliedModel.contains("func doubled("), "声明必须被改名：$appliedModel")
                assertFalse(appliedModel.contains("func twice("), "旧声明名必须消失：$appliedModel")

                val appliedUse = applied[useUri].orEmpty()
                assertTrue(appliedUse.contains("doubled("), "跨文件使用点必须一起改名，实际 $appliedUse")
                assertFalse(appliedUse.contains("twice("), "旧调用名必须消失，实际 $appliedUse")
            }
        }
    }

    /**
     * 折叠与选择范围必须有真实结构（而不是空壳）。
     */
    @Test
    fun `folding and selection ranges have real structure`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(MODEL)
                session.openDocument(uri, fixture.text(MODEL))

                val folding = session.foldingRange(FoldingRangeRequestParams(TextDocumentIdentifier(uri)))
                assertFalse(folding.isEmpty(), "类/函数体必须产生折叠区域")
                assertTrue(
                    folding.any { it.startLine < it.endLine },
                    "折叠区域必须跨多行，实际 ${folding.map { it.startLine to it.endLine }}",
                )
                assertTrue(
                    folding.any { it.startLine <= lineOf(fixture, MODEL, "class Square") && it.endLine >= lineOf(fixture, MODEL, "func area") },
                    "必须存在覆盖 Square 类体的折叠区域，实际 ${folding.map { it.startLine to it.endLine }}",
                )

                val position = positionOf(fixture, MODEL, "return side", delta = 8)
                val chain = session.selectionRange(
                    SelectionRangeParams(TextDocumentIdentifier(uri), listOf(position)),
                ).single()

                val depth = depthOf(chain)
                assertTrue(depth >= 2, "选择范围必须是链（表达式 → 语句 → 声明 → 文件），实际深度 $depth")
                assertNested(chain)
            }
        }
    }

    /**
     * 内联提示必须标在真实表达式上，并说出推断类型。
     */
    @Test
    fun `inlay hints describe inferred types at real positions`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(USE)
                session.openDocument(uri, fixture.text(USE))

                val line = lineOf(fixture, USE, "let square = Square()")
                val hints = session.inlayHint(
                    InlayHintParams(
                        TextDocumentIdentifier(uri),
                        Range(Position(line, 0), Position(line, 200)),
                    ),
                )

                val typeHint = hints.firstOrNull { hintLine(it) == line && labelOf(it).contains("Square") }
                assertNotNull(
                    typeHint,
                    "`let square = Square()` 上应给出类型提示 Square，实际 ${hints.map { hintLine(it) to labelOf(it) }}",
                )
            }
        }
    }

    /**
     * 诊断必须指向真实的错误行，push 与 pull 必须一致。
     */
    @Test
    fun `diagnostics point at the real error line in push and pull channels`() {
        testDataWorkspace("src/broken.cj", "realSemantics/broken.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/broken.cj")
                session.openDocument(uri, fixture.text("src/broken.cj"))

                val errorLine = lineOf(fixture, "src/broken.cj", "return \"text\"")
                val published = session.awaitPublishedDiagnostics(uri) { params ->
                    params.diagnostics.any { it.severity == org.eclipse.lsp4j.DiagnosticSeverity.Error }
                }
                val pushErrors = published.diagnostics.filter {
                    it.severity == org.eclipse.lsp4j.DiagnosticSeverity.Error
                }
                assertTrue(
                    pushErrors.any { it.range.start.line == errorLine },
                    "push 诊断必须落在类型错误所在行 $errorLine，实际 ${pushErrors.map { it.range.start.line }}",
                )

                val pull = session.documentDiagnostic(session.documentDiagnosticParams(uri))
                val pullItems = pull.relatedFullDocumentDiagnosticReport.items
                assertTrue(pullItems.isNotEmpty(), "pull 诊断必须报告同一个类型错误")
                assertTrue(
                    pullItems.any { it.range.start.line == errorLine },
                    "pull 诊断必须与 push 指向同一行 $errorLine，实际 ${pullItems.map { it.range.start.line }}",
                )
            }
        }
    }

    /**
     * code action 的编辑应用后必须真的补出缺失成员。
     */
    @Test
    fun `code action edits implement the missing member when applied`() {
        testDataWorkspace("src/host.cj", "realSemantics/host.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/host.cj")
                val text = fixture.text("src/host.cj")
                session.openDocument(uri, text)

                val actions = session.codeAction(
                    CodeActionParams(
                        TextDocumentIdentifier(uri),
                        rangeOfText(text, "class Host", 0, "class Host".length),
                        CodeActionContext(emptyList()),
                    ),
                )
                val quickFix = actions.mapNotNull { if (it.isRight) it.right else null }
                    .firstOrNull { it.edit == null }
                assertNotNull(quickFix, "未实现成员必须给出（不带编辑的）quick fix，实际 $actions")

                val resolved = session.resolveCodeAction(quickFix!!)
                val edits = resolved.edit?.changes?.get(uri).orEmpty()
                assertTrue(edits.isNotEmpty(), "resolve 必须补出编辑")

                val applied = LspEditApplicator.apply(text, edits, uri)
                assertTrue(
                    applied.contains("override func greet"),
                    "应用编辑后必须真的出现 override 成员声明，实际文本：\n$applied",
                )
            }
        }
    }

    // ---------------------------------------------------------------- 夹具

    /** 语义工作区文件：声明与被调用方。 */
    private companion object {
        const val MODEL: String = "src/model.cj"
        const val USE: String = "src/use.cj"
    }

    /**
     * 两文件语义工作区：接口、实现类、带字段的方法、顶层函数、跨文件调用。
     *
     * 源码放在 `testData/realSemantics/`，与其它模块的 testData 约定一致。
     */
    private fun semanticWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
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

    /** 取 `Either` 形态导航结果的左分支。 */
    private fun locations(result: Either<List<Location>, *>): List<Location> =
        if (result.isLeft) result.left.orEmpty() else emptyList()

    /** hover 的纯文本内容（Markdown 与纯文本都取出）。 */
    private fun hoverText(hover: org.eclipse.lsp4j.Hover): String = when {
        hover.contents == null -> ""
        hover.contents.isLeft -> hover.contents.left.orEmpty().joinToString("\n") { markup ->
            if (markup.isLeft) markup.left.orEmpty() else markup.right?.value.orEmpty()
        }

        else -> hover.contents.right?.value.orEmpty()
    }

    /** 展平 documentSymbol 的层级，取出全部名字（含子符号）。 */
    private fun flattenSymbolNames(symbols: List<Either<org.eclipse.lsp4j.SymbolInformation, DocumentSymbol>>): List<String> {
        val names = mutableListOf<String>()
        symbols.forEach { symbol ->
            if (symbol.isRight) {
                collectNames(symbol.right, names)
            } else {
                names += symbol.left?.name.orEmpty()
            }
        }
        return names
    }

    /** 递归收集符号名。 */
    private fun collectNames(symbol: DocumentSymbol?, destination: MutableList<String>) {
        if (symbol == null) return
        destination += symbol.name
        symbol.children.orEmpty().forEach { collectNames(it, destination) }
    }

    /** 按名字深度优先查找符号。 */
    private fun findSymbol(
        symbols: List<Either<org.eclipse.lsp4j.SymbolInformation, DocumentSymbol>>,
        name: String,
    ): DocumentSymbol? {
        symbols.forEach { symbol ->
            if (symbol.isRight) {
                val found = findSymbol(symbol.right, name)
                if (found != null) return found
            }
        }
        return null
    }

    /** 递归查找同名符号。 */
    private fun findSymbol(symbol: DocumentSymbol?, name: String): DocumentSymbol? {
        if (symbol == null) return null
        if (symbol.name == name) return symbol
        symbol.children.orEmpty().forEach { child ->
            val found = findSymbol(child, name)
            if (found != null) return found
        }
        return null
    }

    /** 判断外层范围是否包含内层范围。 */
    private fun contains(outer: Range, inner: Range): Boolean =
        !positionAfter(outer.start, inner.start) && !positionAfter(inner.end, outer.end)

    /** 位置先后比较。 */
    private fun positionAfter(left: Position, right: Position): Boolean =
        left.line > right.line || (left.line == right.line && left.character > right.character)

    /** 选择范围链深度。 */
    private fun depthOf(chain: SelectionRange?): Int {
        var depth = 0
        var current = chain
        while (current != null) {
            depth++
            current = current.parent
        }
        return depth
    }

    /** 选择范围链必须逐级包含。 */
    private fun assertNested(chain: SelectionRange) {
        var parent = chain.parent
        var child = chain
        while (parent != null) {
            assertTrue(
                contains(parent.range, child.range),
                "选择范围必须逐级包含：${parent.range} 不含 ${child.range}",
            )
            child = parent
            parent = parent.parent
        }
    }

    /** 内联提示所在行。 */
    private fun hintLine(hint: InlayHint): Int = hint.position.line

    /** 内联提示文本。 */
    private fun labelOf(hint: InlayHint): String =
        if (hint.label.isLeft) {
            hint.label.left.orEmpty()
        } else {
            // lsp4j 1.0 的右分支是 InlayHintLabelPart 列表，不是一个 MarkupContent。
            hint.label.right.orEmpty().joinToString("") { part -> part.value }
        }

    /**
     * 解码语义 token，并把 token 覆盖的源文本取出来。
     *
     * 只断言「覆盖文本 + 类型」，因为这才是客户端看到的东西；只比类型编号会让
     * 「长度/起始位置错位」这类 bug 逃过去。
     */
    private fun decode(
        tokens: SemanticTokens,
        tokenTypes: List<String>,
        modifiers: List<String>,
        text: String,
    ): List<DecodedToken> {
        assertEquals(0, tokens.data.size % 5, "语义 token 必须是 5 元组序列")
        val lines = text.lines()
        var line = 0
        var start = 0
        return tokens.data.chunked(5).map { group ->
            val deltaLine = group[0]
            val deltaStart = group[1]
            line += deltaLine
            start = if (deltaLine == 0) start + deltaStart else deltaStart
            val length = group[2]
            val type = group[3]
            val modifierBits = group[4]
            val sourceLine = lines.getOrElse(line) { "" }
            DecodedToken(
                line = line,
                start = start,
                length = length,
                typeName = tokenTypes.getOrElse(type) { "?" },
                modifierNames = modifiers.filterIndexed { index, _ -> (modifierBits shr index) and 1 == 1 },
                text = sourceLine.substring(
                    start.coerceIn(0, sourceLine.length),
                    (start + length).coerceIn(0, sourceLine.length),
                ),
            )
        }
    }

    /**
     * 断言「某行上覆盖指定文本的 token」具有期望类型。
     */
    private fun assertTokenType(
        decoded: List<DecodedToken>,
        text: String,
        line: Int,
        expectedType: String,
    ) {
        val candidates = decoded.filter { it.text == text && it.line == line }
        assertTrue(
            candidates.isNotEmpty(),
            "第 $line 行没有覆盖 `$text` 的语义 token；该行实际 token=${decoded.filter { it.line == line }.map { it.text to it.typeName }}",
        )
        assertTrue(
            candidates.any { it.typeName == expectedType },
            "第 $line 行的 `$text` 必须着色为 $expectedType，实际 ${candidates.map { it.typeName }}",
        )
    }

    /**
     * 断言某行的 token 类型落在允许集合内。
     */
    private fun assertTokenLike(
        decoded: List<DecodedToken>,
        text: String,
        line: Int,
        allowed: Set<String>,
    ) {
        val candidates = decoded.filter { it.text == text && it.line == line }
        assertTrue(
            candidates.isNotEmpty(),
            "第 $line 行没有覆盖 `$text` 的语义 token；实际 ${decoded.filter { it.line == line }.map { it.text to it.typeName }}",
        )
        assertTrue(
            candidates.any { it.typeName in allowed },
            "第 $line 行的 `$text` 类型必须是 $allowed 之一，实际 ${candidates.map { it.typeName }}",
        )
    }

    /**
     * 成员方法声明应按 LSP legend 着色为 `method`。
     *
     * 规范里 `method` 明确用于「方法声明或调用」；当前实现把**所有**函数声明统一映射成
     * `function`，成员的 `method` 只出现在调用点。这是着色保真度缺口（不影响功能），
     * 单独立一条红灯，避免它掩盖其它 token 断言。
     */
    @Test
    @Tag(KNOWN_DEFECT_TAG)
    fun `member function declarations are coloured as methods`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val modelUri = fixture.uri(MODEL)
                val text = fixture.text(MODEL)
                session.openDocument(modelUri, text)

                val legend = requireNotNull(
                    session.initializeResult().capabilities.semanticTokensProvider?.legend,
                )
                val decoded = decode(
                    session.semanticTokensFull(SemanticTokensParams(TextDocumentIdentifier(modelUri))),
                    legend.tokenTypes,
                    legend.tokenModifiers,
                    text,
                )

                assertTokenType(decoded, "area", lineOf(fixture, MODEL, "func area"), "method")
            }
        }
    }

    /** 解码后的 token。 */
    private data class DecodedToken(
        val line: Int,
        val start: Int,
        val length: Int,
        val typeName: String,
        val modifierNames: List<String>,
        val text: String,
    )

    /** 指定 token 首次出现处（可加 [delta]）的位置。 */
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
        return document(fixture, relativePath, text).positionAt(offset + delta)
    }

    /** 指定 token 首次出现处的行号。 */
    private fun lineOf(fixture: LspWorkspaceFixture, relativePath: String, token: String): Int =
        positionOf(fixture, relativePath, token).line

    /** 文本中指定 token 覆盖的范围。 */
    private fun rangeOfText(text: String, token: String, offsetInToken: Int, length: Int): Range {
        val start = text.indexOf(token)
        check(start >= 0) { "Cannot find `$token`" }
        val document = LspTextDocument("file:///range.cj", "cangjie", 1, text)
        return Range(
            document.positionAt(start + offsetInToken),
            document.positionAt(start + offsetInToken + length),
        )
    }

    /** 构造文档快照（仅用于坐标换算）。 */
    private fun document(
        fixture: LspWorkspaceFixture,
        relativePath: String,
        text: String,
    ): LspTextDocument = LspTextDocument(fixture.uri(relativePath), "cangjie", 1, text)
}
