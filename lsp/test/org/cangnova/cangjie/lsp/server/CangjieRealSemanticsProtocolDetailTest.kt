package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.KNOWN_DEFECT_TAG
import org.cangnova.cangjie.lsp.testkit.LspClientCapabilitiesBuilder
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.CodeActionContext
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.InlayHint
import org.eclipse.lsp4j.InlayHintParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.ReferenceContext
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.SemanticTokens
import org.eclipse.lsp4j.SemanticTokensParams
import org.eclipse.lsp4j.SemanticTokensRangeParams
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * 协议语义细节：能力协商之后的**结果形态**必须随客户端能力变化，而不是「总是返回同一种形状」。
 *
 * 这一类比「请求有没有结果」更容易出问题：客户端拿到错误形状时通常不报错，
 * 只是静默丢掉结果（例如老客户端不认识 `DocumentSymbol` 的 children 字段）。
 * 因此每条都断言**形状**或**范围收敛**，不是断言「非空」。
 */
class CangjieRealSemanticsProtocolDetailTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：每个用例都要按自己的客户端能力初始化。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 客户端没声明 `hierarchicalDocumentSymbolSupport` 时，服务端必须降级成 `SymbolInformation`。
     *
     * LSP 3.17 `DocumentSymbolClientCapabilities.hierarchicalDocumentSymbolSupport`：
     * 客户端不支持时，服务端只能返回扁平的 `SymbolInformation`。返回 `DocumentSymbol`
     * 会让客户端把整棵层级当成一个没有名字的符号——用户看到空的「大纲」。
     */
    @Test
    @Tag(KNOWN_DEFECT_TAG)
    fun `document symbols degrade to flat symbols when the client has no hierarchy support`() {
        structureWorkspace().use { fixture ->
            // `core()` 的 documentSymbol 能力没有打开 hierarchicalDocumentSymbolSupport。
            createSession(fixture.initializeParams(capabilities = LspClientCapabilitiesBuilder.core()))
                .use { session ->
                    val uri = fixture.uri(STRUCTURE)
                    session.openDocument(uri, fixture.text(STRUCTURE))

                    val symbols = session.documentSymbol(DocumentSymbolParams(TextDocumentIdentifier(uri)))
                    assertTrue(symbols.isNotEmpty(), "结构文件必须给出符号，实际为空")

                    val hierarchical = symbols.filterIsInstance<Either<SymbolInformation, DocumentSymbol>>()
                        .count { it.isRight }
                    assertEquals(
                        0,
                        hierarchical,
                        "客户端未声明层级符号支持时不得返回 DocumentSymbol（实际 $hierarchical 个）；" +
                            "客户端会把它们当成无名符号，大纲视图将变为空白。实际结果：$symbols",
                    )

                    val names = symbols.mapNotNull { it.left?.name }
                    assertTrue(
                        names.containsAll(listOf("Shape", "Square")),
                        "降级后仍必须给出类型符号，实际 $names",
                    )
                }
        }
    }

    /**
     * 客户端声明了层级符号支持时，必须给出带 children 的 `DocumentSymbol`。
     *
     * 与上一条成对：一条断言降级、一条断言不降级。只测其中一条会把「永远扁平」或
     * 「永远层级」都放过。
     */
    @Test
    fun `document symbols keep their hierarchy when the client supports it`() {
        structureWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(STRUCTURE)
                session.openDocument(uri, fixture.text(STRUCTURE))

                val symbols = session.documentSymbol(DocumentSymbolParams(TextDocumentIdentifier(uri)))
                val square = symbols.mapNotNull { it.right }
                    .firstOrNull { it.name == "Square" }
                assertTrue(square != null, "支持层级时 Square 必须是 DocumentSymbol，实际 $symbols")
                assertTrue(
                    square!!.children.orEmpty().any { it.name == "area" },
                    "Square 的成员必须挂在 children 下，实际 ${square.children?.map { it.name }}",
                )
            }
        }
    }

    /**
     * `includeDeclaration` 必须真的影响结果，而不是被忽略。
     *
     * 客户端据此区分「查找引用」和「查找所有出现」：编辑器里「Find Usages」通常不含声明，
     * 「Highlight All」需要含声明。忽略该字段会让用户的引用列表多出一条声明项。
     */
    @Test
    fun `references honour the include declaration flag`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val modelUri = fixture.uri(MODEL)
                val useUri = fixture.uri(USE)
                session.openDocument(modelUri, fixture.text(MODEL))
                session.openDocument(useUri, fixture.text(USE))

                val declaration = declarationRangeOf(fixture, MODEL, "func twice", "twice")
                val call = positionOf(fixture, USE, "twice(", delta = 0)

                val withDeclaration = session.references(
                    ReferenceParams(TextDocumentIdentifier(useUri), call, ReferenceContext(true)),
                )
                val withoutDeclaration = session.references(
                    ReferenceParams(TextDocumentIdentifier(useUri), call, ReferenceContext(false)),
                )

                assertTrue(
                    withDeclaration.any { it.uri == modelUri && it.range == declaration },
                    "includeDeclaration=true 必须包含 func twice 的声明，实际 $withDeclaration",
                )
                assertFalse(
                    withoutDeclaration.any { it.uri == modelUri && it.range == declaration },
                    "includeDeclaration=false 不得包含声明本身，实际 $withoutDeclaration",
                )
                assertTrue(
                    withoutDeclaration.any { it.uri == useUri },
                    "去掉声明后仍必须保留 use.cj 的调用点，实际 $withoutDeclaration",
                )
            }
        }
    }

    /** 类型模式的名称子节点属于声明，两个分支内用法才是引用。 */
    @Test
    fun `type pattern references contain exactly the binder uses`() {
        patternReferencesWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                session.openDocument(fixture.uri(PATTERNS), fixture.text(PATTERNS))
                assertExactPatternReferences(
                    session, fixture,
                    declaration = tokenLocation(fixture, PATTERNS, "case typeBound", "typeBound"),
                    uses = listOf(
                        tokenLocation(fixture, PATTERNS, "=> typeBound +", "typeBound"),
                        tokenLocation(fixture, PATTERNS, "+ typeBound", "typeBound"),
                    ),
                )
            }
        }
    }

    /** VarOrEnum 决议为绑定后，查询声明或用法都必须获得同一个精确引用集合。 */
    @Test
    fun `bare binder references contain exactly the binder uses`() {
        patternReferencesWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                session.openDocument(fixture.uri(PATTERNS), fixture.text(PATTERNS))
                assertExactPatternReferences(
                    session, fixture,
                    declaration = tokenLocation(fixture, PATTERNS, "case bound", "bound"),
                    uses = listOf(
                        tokenLocation(fixture, PATTERNS, "=> bound +", "bound"),
                        tokenLocation(fixture, PATTERNS, "+ bound", "bound"),
                    ),
                )
            }
        }
    }

    /** 全局 tuple 叶子独立发布声明身份；原名及多个导入别名都保持相同目标。 */
    @Test
    fun `global tuple leaf references include all aliases and exactly one declaration`() {
        patternReferencesWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                listOf(PATTERN_PROVIDER, PATTERN_CONSUMER).forEach { path ->
                    session.openDocument(fixture.uri(path), fixture.text(path))
                }
                assertExactPatternReferences(
                    session, fixture,
                    declaration = tokenLocation(fixture, PATTERN_PROVIDER, "(globalLeaf,", "globalLeaf"),
                    uses = listOf(
                        tokenLocation(fixture, PATTERN_PROVIDER, "return globalLeaf", "globalLeaf"),
                        tokenLocation(fixture, PATTERN_CONSUMER, "globalLeaf as firstAlias", "globalLeaf"),
                        tokenLocation(fixture, PATTERN_CONSUMER, "globalLeaf as secondAlias", "globalLeaf"),
                        tokenLocation(fixture, PATTERN_CONSUMER, "import sample.patternprovider.globalLeaf\n", "globalLeaf"),
                        tokenLocation(fixture, PATTERN_CONSUMER, "return firstAlias +", "firstAlias"),
                        tokenLocation(fixture, PATTERN_CONSUMER, "+ secondAlias", "secondAlias"),
                        tokenLocation(fixture, PATTERN_CONSUMER, "+ globalLeaf", "globalLeaf"),
                    ),
                )
                assertExactPatternReferences(
                    session, fixture,
                    declaration = tokenLocation(fixture, PATTERN_PROVIDER, ", sibling)", "sibling"),
                    uses = listOf(
                        tokenLocation(fixture, PATTERN_CONSUMER, "import sample.patternprovider.sibling", "sibling"),
                        tokenLocation(fixture, PATTERN_CONSUMER, "return sibling", "sibling"),
                    ),
                )
            }
        }
    }

    /**
     * 引用列表不得出现重复位置。
     *
     * 客户端把每个 `Location` 渲染成一行（VS Code 的 Peek 视图、IntelliJ 的 Find Usages）；
     * 同一位置出现两次会让用户看到重复条目，并且在「全部替换」时重复应用同一次编辑。
     */
    @Test
    fun `references never repeat the same location`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val modelUri = fixture.uri(MODEL)
                val useUri = fixture.uri(USE)
                session.openDocument(modelUri, fixture.text(MODEL))
                session.openDocument(useUri, fixture.text(USE))

                val call = positionOf(fixture, USE, "twice(", delta = 0)
                val references = session.references(
                    ReferenceParams(TextDocumentIdentifier(useUri), call, ReferenceContext(true)),
                )
                assertTrue(references.isNotEmpty(), "`twice` 至少有一处调用与一处声明，实际为空")

                val keys = references.map { "${it.uri}:${it.range.start.line}:${it.range.start.character}" }
                assertEquals(
                    keys.size,
                    keys.toSet().size,
                    "引用结果不得重复，实际重复项：${keys.groupingBy { it }.eachCount().filterValues { it > 1 }}",
                )
            }
        }
    }

    /**
     * `semanticTokens/range` 必须收敛到请求范围，且与全量结果在该范围内一致。
     *
     * 服务端在 initialize 里声明了 `range = true`，客户端只会对可视区域发范围请求；
     * 若服务端忽略范围，等于把整文件的 token 编码换成局部编码塞回去——着色会整体错位。
     */
    @Test
    fun `semantic tokens range matches the full result inside the range`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val modelUri = fixture.uri(MODEL)
                val text = fixture.text(MODEL)
                session.openDocument(modelUri, text)

                val legend = requireNotNull(
                    session.initializeResult().capabilities.semanticTokensProvider?.legend,
                ) { "语义 token 能力已宣告，legend 不得为空" }

                val declarationLine = lineOf(fixture, MODEL, "func twice")
                // `func twice` 三行：签名、return、右括号。
                val range = Range(Position(declarationLine, 0), Position(declarationLine + 2, 1))

                val full = decode(
                    session.semanticTokensFull(SemanticTokensParams(TextDocumentIdentifier(modelUri))),
                    legend.tokenTypes,
                    text,
                )
                val ranged = decode(
                    session.semanticTokensRange(
                        SemanticTokensRangeParams(TextDocumentIdentifier(modelUri), range),
                    ),
                    legend.tokenTypes,
                    text,
                )

                assertTrue(ranged.isNotEmpty(), "范围内必须返回 token，实际为空（范围 $range）")
                ranged.forEach { token ->
                    assertTrue(
                        token.line in declarationLine..(declarationLine + 2),
                        "范围请求返回了范围外的 token：$token（范围 $range）",
                    )
                }

                fun fingerprint(tokens: List<DecodedToken>): List<Triple<Int, Int, String>> =
                    tokens.map { Triple(it.line, it.start, it.typeName) }.sortedBy { it.first * 1000 + it.second }

                val expected = fingerprint(full.filter { it.line in declarationLine..(declarationLine + 2) })
                assertEquals(
                    expected,
                    fingerprint(ranged),
                    "范围请求在 [$declarationLine, ${declarationLine + 2}] 内必须与全量结果一致",
                )
            }
        }
    }

    /**
     * `codeAction` 必须按请求范围筛选诊断，不能把全文的修复都塞回来。
     *
     * 客户端只在光标处或诊断处请求动作；返回无关动作会让用户在接口声明处看到
     * 「实现未实现成员」这种和当前行毫无关系的条目。
     */
    @Test
    fun `code actions stay scoped to the requested range`() {
        testDataWorkspace(HOST, "realSemantics/host.cj").use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(HOST)
                val text = fixture.text(HOST)
                session.openDocument(uri, text)

                fun actionsAt(token: String): List<String> = session.codeAction(
                    CodeActionParams(
                        TextDocumentIdentifier(uri),
                        rangeOfToken(text, token),
                        CodeActionContext(emptyList()),
                    ),
                ).mapNotNull { action -> action.right?.title ?: action.left?.title }

                val atClass = actionsAt("class Host")
                assertTrue(
                    atClass.isNotEmpty(),
                    "未实现成员所在的类声明处必须给出 quick fix，实际为空",
                )

                val atInterface = actionsAt("interface Greeter")
                assertTrue(
                    atInterface.isEmpty(),
                    "接口声明处与「未实现成员」诊断不重叠，不得返回针对 class Host 的修复，实际 $atInterface",
                )
            }
        }
    }

    /**
     * `inlayHint` 必须只返回请求范围内的提示。
     *
     * 编辑器按可视区域分块请求；忽略范围会让每块都返回全文提示，滚动时提示重复跳动。
     */
    @Test
    fun `inlay hints stay inside the requested range`() {
        semanticWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri(USE)
                session.openDocument(uri, fixture.text(USE))

                val line = lineOf(fixture, USE, "let square = Square()")
                val wide = session.inlayHint(
                    InlayHintParams(TextDocumentIdentifier(uri), Range(Position(line, 0), Position(line, 200))),
                )
                assertTrue(
                    wide.any { hintLine(it) == line && labelOf(it).contains("Square") },
                    "范围内必须给出 Square 的类型提示，实际 ${wide.map { hintLine(it) to labelOf(it) }}",
                )

                val narrow = session.inlayHint(
                    InlayHintParams(TextDocumentIdentifier(uri), Range(Position(0, 0), Position(0, 10))),
                )
                assertTrue(
                    narrow.isEmpty(),
                    "包声明行没有任何可推断的位置，不得返回提示，实际 ${narrow.map { hintLine(it) to labelOf(it) }}",
                )
            }
        }
    }

    // ---------------------------------------------------------------- 夹具

    private companion object {
        const val MODEL: String = "src/model.cj"
        const val USE: String = "src/use.cj"
        const val STRUCTURE: String = "src/structure.cj"
        const val HOST: String = "src/host.cj"
        const val PATTERNS: String = "src/patterns.cj"
        const val PATTERN_PROVIDER: String = "src/provider.cj"
        const val PATTERN_CONSUMER: String = "src/consumer.cj"
    }

    /** 两文件语义工作区（与其它真实语义用例共用同一份 testData）。 */
    private fun semanticWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .sourceFromTestData("model.cj", "realSemantics/model.cj")
        .sourceFromTestData("use.cj", "realSemantics/use.cj")
        .build()

    /** 单文件工作区。 */
    private fun structureWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .sourceFromTestData("structure.cj", "realSemantics/structure.cj")
        .build()

    /** 模式引用协议专用工作区，源码均来自已用官方编译器核实的 testData。 */
    private fun patternReferencesWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .sourceFromTestData("patterns.cj", "realSemantics/protocolDetail/patterns.cj")
        .sourceFromTestData("provider.cj", "realSemantics/protocolDetail/provider.cj")
        .sourceFromTestData("consumer.cj", "realSemantics/protocolDetail/consumer.cj")
        .build()

    /** 由 testData 单文件构造的工作区。 */
    private fun testDataWorkspace(targetRelativePath: String, testDataPath: String): LspWorkspaceFixture =
        LspWorkspaceFixtureBuilder()
            .fileFromTestData(targetRelativePath, testDataPath)
            .build()

    // ---------------------------------------------------------------- 断言辅助

    /** 用完整 URI/Range 集合校验每个查询入口；计数另行锁住重复位置。 */
    private fun assertExactPatternReferences(
        session: org.cangnova.cangjie.lsp.framework.LspIntegrationTestSession,
        fixture: LspWorkspaceFixture,
        declaration: Location,
        uses: List<Location>,
    ) {
        (listOf(declaration) + uses).forEach { query ->
            listOf(false, true).forEach { includeDeclaration ->
                val expected = if (includeDeclaration) uses + declaration else uses
                val actual = session.references(
                    ReferenceParams(TextDocumentIdentifier(query.uri), query.range.start, ReferenceContext(includeDeclaration)),
                )
                val context = "query=$query, includeDeclaration=$includeDeclaration, workspace=${fixture.workspaceRoot}"
                assertEquals(expected.size, actual.size, "声明或引用数量不正确：$context")
                assertEquals(expected.toSet(), actual.toSet(), "必须精确匹配规范叶子的全部位置：$context")
            }
        }
    }

    /** 用唯一源码片段定位其中的标识符，不依赖 PSI 或产品侧 target key 算法生成期望。 */
    private fun tokenLocation(fixture: LspWorkspaceFixture, path: String, anchor: String, token: String): Location {
        val text = fixture.text(path).replace("\r\n", "\n")
        val anchorOffset = text.indexOf(anchor)
        check(anchorOffset >= 0 && text.indexOf(anchor, anchorOffset + 1) < 0) { "源码片段必须唯一：$path: $anchor" }
        val tokenOffset = anchor.indexOf(token)
        check(tokenOffset >= 0) { "源码片段缺少标识符：$anchor: $token" }
        val document = LspTextDocument(fixture.uri(path), "cangjie", 1, text)
        val start = anchorOffset + tokenOffset
        return Location(fixture.uri(path), Range(document.positionAt(start), document.positionAt(start + token.length)))
    }

    /** 解码后的 token。 */
    private data class DecodedToken(
        val line: Int,
        val start: Int,
        val length: Int,
        val typeName: String,
    )

    /** 按 5 元组增量编码解出 token 的绝对位置。 */
    private fun decode(tokens: SemanticTokens, tokenTypes: List<String>, text: String): List<DecodedToken> {
        assertEquals(0, tokens.data.size % 5, "语义 token 必须是 5 元组序列")
        val lineCount = text.lineSequence().count()
        var line = 0
        var start = 0
        return tokens.data.chunked(5).map { group ->
            val deltaLine = group[0]
            val deltaStart = group[1]
            line += deltaLine
            start = if (deltaLine == 0) start + deltaStart else deltaStart
            assertTrue(line in 0 until lineCount, "token 行号越界：line=$line，文件行数=$lineCount")
            DecodedToken(
                line = line,
                start = start,
                length = group[2],
                typeName = tokenTypes.getOrElse(group[3]) { "unknown(${group[3]})" },
            )
        }
    }

    /** 提示所在行。 */
    private fun hintLine(hint: InlayHint): Int = hint.position.line

    /** 提示文本。 */
    private fun labelOf(hint: InlayHint): String =
        if (hint.label.isLeft) {
            hint.label.left.orEmpty()
        } else {
            hint.label.right.orEmpty().joinToString("") { part -> part.value }
        }

    /** 声明标识符的范围（用于和 references 结果比对）。 */
    private fun declarationRangeOf(
        fixture: LspWorkspaceFixture,
        relativePath: String,
        declarationPrefix: String,
        identifier: String,
    ): Range {
        val text = fixture.text(relativePath)
        val offset = text.indexOf(declarationPrefix) + declarationPrefix.length - identifier.length
        check(offset >= identifier.length) { "在 $relativePath 中找不到 `$declarationPrefix`" }
        val document = LspTextDocument(fixture.uri(relativePath), "cangjie", 1, text)
        return Range(document.positionAt(offset), document.positionAt(offset + identifier.length))
    }

    /** 文本里某段字面量覆盖的范围。 */
    private fun rangeOfToken(text: String, token: String): Range {
        val start = text.indexOf(token)
        check(start >= 0) { "找不到 `$token`" }
        val document = LspTextDocument("file:///range.cj", "cangjie", 1, text)
        return Range(document.positionAt(start), document.positionAt(start + token.length))
    }

    /** 第 `occurrence` 次出现的 token 的起始位置。 */
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
            check(offset >= 0) { "在 $relativePath 中找不到第 $occurrence 个 `$token`" }
            searchFrom = offset + token.length
        }
        val document = LspTextDocument(fixture.uri(relativePath), "cangjie", 1, text)
        return document.positionAt(offset + delta)
    }

    /** token 所在行号。 */
    private fun lineOf(fixture: LspWorkspaceFixture, relativePath: String, token: String): Int =
        positionOf(fixture, relativePath, token).line
}
