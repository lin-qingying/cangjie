package org.cangnova.cangjie.cfir.resolve.providers.macro

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.messages.CjParsingDiagnosticCollector
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.parsing.CjParserSourceContext
import org.cangnova.cangjie.source.CjLightSourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.util.concurrent.CancellationException

/**
 * 验证基于 token 的宏片段解析器在重新 token 化、重新解析和失败路径上的行为。
 */
class TokenBackedMacroFragmentParserTest {
    /** 内部不变量和取消不是源码语法失败，必须保持异常身份，不能变成可降级 fragment。 */
    @Test
    fun `reparse invariant failures and cancellation propagate unchanged`() {
        for (expected in listOf(IllegalStateException("metadata invariant"), CancellationException("cancel reparse"))) {
            var reparseCalls = 0
            val parser = TokenBackedMacroFragmentParser(
                reparse = { text, _ ->
                    reparseCalls++
                    assertEquals("1 + 2", text)
                    throw expected
                },
                reTokenize = { it },
            )
            val thrown = assertThrows(RuntimeException::class.java) {
                parser.parse(input(node("ExprMacro"), listOf(token("1 + 2")), MacroFragmentParser.Mode.EXPRESSION))
            }
            assertSame(expected, thrown)
            assertEquals(1, reparseCalls)
        }
    }

    /** 语法失败保留生成文本坐标，并且不能伪装成 CFIR 语义诊断。 */
    @Test
    fun `syntax failure remains in parser channel with exact generated ranges`() {
        val rawText = "  func generated( {}  "
        val parsedText = rawText.trim()
        val sourceRange = MacroSurfaceSourceRange(
            source = lightSourceElement(100, 100 + rawText.length),
            startOffset = 100,
            endOffset = 100 + rawText.length,
        )
        val diagnosticStart = parsedText.indexOf('{')
        val diagnostic = ParsingDiagnostic(diagnosticStart, diagnosticStart + 1, "Expecting parameter declaration")
        lateinit var diagnostics: List<ParsingDiagnostic>
        val parser = TokenBackedMacroFragmentParser(
            reparse = { text, _ ->
                assertEquals(parsedText, text)
                val collector = CjParsingDiagnosticCollector(CjParserSourceContext.complete(text))
                collector.report(diagnostic)
                diagnostics = collector.finish()
                MacroFragmentReparseResult.SyntaxFailure(text, collector.publication)
            },
            reTokenize = { it },
        )
        val origin = node("InvalidDeclaration", sourceRange)
        val result = parser.parse(
            input(
                origin,
                listOf(token(rawText, sourceRange.startOffset, sourceRange, isSourceToken = true)),
                MacroFragmentParser.Mode.DECLARATION,
            ),
        )
        assertTrue(result is MacroFragmentResult.SyntaxFailure)
        val failure = result as MacroFragmentResult.SyntaxFailure
        assertEquals(origin, failure.originNode)
        assertEquals(parsedText, failure.parsedText)
        assertEquals(diagnostics, failure.diagnostics)
        assertEquals(diagnostic, failure.diagnostics.single().copy(mainRange = null))
        val mappedRange = failure.sourceMap?.mapRange(diagnosticStart, diagnosticStart + 1)
        assertEquals(sourceRange.startOffset + rawText.indexOf('{'), mappedRange?.startOffset)
        assertEquals(sourceRange.startOffset + rawText.indexOf('{') + 1, mappedRange?.endOffset)
        val registry = MacroExpansionRegistry()
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException::class.java) { registry.addParsingFailure(failure) }
        assertFalse(registry.hasErrors)
        assertTrue(registry.diagnostics.isEmpty())
        assertTrue(registry.parsingFailures.isEmpty())
    }

    /** 生成文本偏移可能恰好落入调用范围；来源标记必须阻止伪造逐字符源码位置。 */
    @Test
    fun `generated coordinates overlapping the invocation remain expansion origins`() {
        val sourceRange = MacroSurfaceSourceRange(lightSourceElement(10, 80), 10, 80)
        val generatedText = "func generated() {}"
        val diagnostic = ParsingDiagnostic(5, 14, "Generated declaration is invalid")
        val parser = TokenBackedMacroFragmentParser(
            reparse = { text, _ ->
                val collector = CjParsingDiagnosticCollector(CjParserSourceContext.complete(text))
                collector.report(diagnostic)
                collector.finish()
                MacroFragmentReparseResult.SyntaxFailure(text, collector.publication)
            },
            reTokenize = { it },
        )
        val result = parser.parse(input(
            node("Generated", sourceRange),
            listOf(token(generatedText, 20, sourceRange)),
            MacroFragmentParser.Mode.DECLARATION,
        )) as MacroFragmentResult.SyntaxFailure
        val mapped = result.sourceMap?.mapRange(diagnostic.startOffset, diagnostic.endOffset)
        assertEquals(10, mapped?.startOffset)
        assertEquals(80, mapped?.endOffset)
    }

    /** 缺少 token 的 EOF 语法诊断仍保留原源码中的零宽锚点。 */
    @Test
    fun `source token EOF syntax failure keeps its original insertion point`() {
        val rawText = "  func missing()  "
        val sourceRange = MacroSurfaceSourceRange(lightSourceElement(100, 100 + rawText.length), 100, 100 + rawText.length)
        val parsedText = rawText.trim()
        val diagnostic = ParsingDiagnostic(parsedText.length, parsedText.length, "Expecting body")
        val parser = TokenBackedMacroFragmentParser(
            reparse = { text, _ ->
                val collector = CjParsingDiagnosticCollector(CjParserSourceContext.complete(text))
                collector.report(diagnostic)
                collector.finish()
                MacroFragmentReparseResult.SyntaxFailure(text, collector.publication)
            },
            reTokenize = { it },
        )
        val result = parser.parse(input(
            node("Source", sourceRange),
            listOf(token(rawText, 100, sourceRange, isSourceToken = true)),
            MacroFragmentParser.Mode.DECLARATION,
        )) as MacroFragmentResult.SyntaxFailure
        val mapped = result.sourceMap?.mapRange(diagnostic.startOffset, diagnostic.endOffset)
        assertEquals(100 + rawText.trimEnd().length, mapped?.startOffset)
        assertEquals(mapped?.startOffset, mapped?.endOffset)
    }

    /** 复扫器未满足稳定输入契约时，不得把最后一轮 token 当作成功片段继续解析。 */
    @Test
    fun `unstable token re-evaluation cannot publish a fragment`() {
        var reparseCalls = 0
        val parser = TokenBackedMacroFragmentParser(
            reparse = { _, _ ->
                reparseCalls++
                MacroFragmentReparseResult.Success(MacroFragmentPayload.EMPTY)
            },
            reTokenize = { tokens -> listOf(token(if (tokens.single().text == "left") "right" else "left")) },
        )
        assertThrows(IllegalStateException::class.java) {
            parser.parse(input(node("Unstable"), listOf(token("left")), MacroFragmentParser.Mode.EXPRESSION))
        }
        assertEquals(0, reparseCalls)
    }

    /**
     * 验证空 token 流会直接失败，并且不会触发重解析回调。
     */
    @Test
    fun `empty token stream returns failure without invoking reparse`() {
        var reparseCalls = 0
        val parser = TokenBackedMacroFragmentParser(
            reparse = { _, _ ->
                reparseCalls++
                MacroFragmentReparseResult.Success(MacroFragmentPayload.EMPTY)
            },
            reTokenize = MacroTokenReEvaluator::preserveTextTokens,
        )
        val node = node("Empty")

        val result = parser.parse(
            input(node, emptyList(), MacroFragmentParser.Mode.EXPRESSION),
        )

        assertTrue(result is MacroFragmentResult.Failure)
        assertEquals("Macro fragment text is empty after token re-evaluation", (result as MacroFragmentResult.Failure).reason)
        assertEquals(0, reparseCalls)
    }

    /** 声明宏允许删除 carrier，空 token 流必须成为零声明输出而不是展开失败。 */
    @Test
    fun `empty declaration fragment represents zero replacements`() {
        var reparseCalls = 0
        val parser = TokenBackedMacroFragmentParser(
            reparse = { _, _ ->
                reparseCalls++
                error("An empty declaration fragment does not require reparsing.")
            },
            reTokenize = MacroTokenReEvaluator::preserveTextTokens,
        )

        val result = parser.parse(
            input(node("RemoveDeclaration"), emptyList(), MacroFragmentParser.Mode.DECLARATION),
        )

        assertTrue(result is MacroFragmentResult.Success)
        val success = result as MacroFragmentResult.Success
        assertTrue((success.payload as MacroDeclarationFragment).declarations.isEmpty())
        assertEquals(0, reparseCalls)
    }

    /**
     * 验证表达式模式下成功重解析会返回 construction-only 的成功片段。
     */
    @Test
    fun `successful expression parse returns construction-only success`() {
        val received = mutableListOf<Pair<String, MacroFragmentParser.Mode>>()
        val parser = TokenBackedMacroFragmentParser(
            reparse = { text, input ->
                received += text to input.mode
                MacroFragmentReparseResult.Success(MacroFragmentPayload.EMPTY)
            },
            reTokenize = { it },
        )
        val tokens = listOf(token("1"), token(" + "), token("2"))
        val node = node("ExprMacro")

        val result = parser.parse(
            input(node, tokens, MacroFragmentParser.Mode.EXPRESSION),
        )

        assertTrue(result is MacroFragmentResult.Success)
        val success = result as MacroFragmentResult.Success
        assertEquals(MacroFragmentParser.Mode.EXPRESSION, success.mode)
        assertEquals(tokens, success.tokens)
        assertEquals(listOf("1 + 2" to MacroFragmentParser.Mode.EXPRESSION), received)
    }

    /**
     * 验证自定义注解模式必须携带完整注解槽位快照。
     */
    @Test
    fun `custom annotation mode requires full annotation slot snapshot`() {
        var reparseCalls = 0
        val parser = TokenBackedMacroFragmentParser(
            reparse = { _, _ ->
                reparseCalls++
                MacroFragmentReparseResult.Success(MacroFragmentPayload.EMPTY)
            },
            reTokenize = MacroTokenReEvaluator::preserveTextTokens,
        )
        val node = node("pkg.Anno")

        val result = parser.parse(
            input(
                node = node,
                tokens = listOf(token("@Anno"), token("["), token("value"), token("]")),
                mode = MacroFragmentParser.Mode.CUSTOM_ANNOTATION,
            ),
        )

        assertTrue(result is MacroFragmentResult.Failure)
        assertEquals(
            "Custom-annotation fragment requires a full annotation slot snapshot.",
            (result as MacroFragmentResult.Failure).reason,
        )
        assertEquals(0, reparseCalls)
    }

    /**
     * 验证解析器会先消费 token 阶段重新求值结果，再交给重解析回调。
     */
    @Test
    fun `parser consumes token-stage re-evaluation output before reparse`() {
        val received = mutableListOf<String>()
        val parser = TokenBackedMacroFragmentParser(
            reparse = { text, _ ->
                received += text
                MacroFragmentReparseResult.Success(MacroFragmentPayload.EMPTY)
            },
            reTokenize = {
                listOf(
                    token("normalized"),
                    token("("),
                    token("42"),
                    token(")"),
                )
            },
        )
        val node = node("ExprMacro")

        val result = parser.parse(
            input(node, listOf(token("ignored text")), MacroFragmentParser.Mode.EXPRESSION),
        )

        assertTrue(result is MacroFragmentResult.Success)
        assertEquals(listOf("normalized(42)"), received)
        assertEquals("normalized(42)", (result as MacroFragmentResult.Success).tokens.joinToString(separator = "") { it.text })
    }

    /**
     * 构造宏片段解析输入。
     */
    private fun input(
        node: MacroCallNode,
        tokens: List<MacroSurfaceToken>,
        mode: MacroFragmentParser.Mode,
    ): MacroFragmentInput {
        val decision = FinalMacroSurfaceDecision(
            surface = node.surface,
            callSite = MacroCallSite.EXPRESSION,
            slotType = when (mode) {
                MacroFragmentParser.Mode.CUSTOM_ANNOTATION -> MacroReplacementSlotType.ANNOTATION
                MacroFragmentParser.Mode.DECLARATION -> MacroReplacementSlotType.DECLARATION
                MacroFragmentParser.Mode.EXPRESSION -> MacroReplacementSlotType.EXPRESSION
            },
            annotationCarrier = null,
            resolution = when (mode) {
                MacroFragmentParser.Mode.CUSTOM_ANNOTATION -> MacroResolution.CustomAnnotation(Name.identifier("Anno"))
                else -> MacroResolution.Unresolved(Name.identifier("ExprMacro"))
            },
            parserMode = mode,
            localConstruction = true,
            executorRequired = false,
            externalPackageDemand = null,
            failurePolicy = MacroFailurePolicy.STRICT,
            blockedDiagnostic = null,
        )
        return MacroFragmentInput(
            node = node,
            tokens = tokens,
            decision = decision,
            annotationSnapshot = null,
            annotationMetadata = CfirAnnotationMetadataBuilder(),
        )
    }

    /**
     * 构造没有父子关系的测试宏调用节点。
     */
    private fun node(name: String, sourceRange: MacroSurfaceSourceRange? = null): MacroCallNode {
        return MacroCallNode(
            surface = MacroSurfaceExpr(
                surfaceId = name.hashCode().toLong(),
                qualifiedName = FqName(name),
                isQualifiedName = '.' in name,
                kind = MacroSurface.Kind.PLAIN,
                parsingFacts = org.cangnova.cangjie.parsing.MacroParsingFacts(
                    org.cangnova.cangjie.parsing.MacroParsingScope.Expression(
                        org.cangnova.cangjie.parsing.CjExprKind.UNKNOWN_EXPR,
                    ),
                    org.cangnova.cangjie.parsing.CjMacroFragmentParent.OTHER,
                    null,
                ),
                hasParenthesis = true,
                attrTokens = emptyList(),
                hasAttr = false,
                inputTokens = emptyList(),
                sourceRange = sourceRange ?: MacroSurfaceSourceRange(
                    source = null,
                    startOffset = 0,
                    endOffset = name.length,
                ),
                scopeContext = MacroSurfaceScopeContext(
                    packageFqName = FqName("test"),
                    sourceModuleName = BuiltInAnnotationRegistry.sourceModuleName(FqName("test")),
                    enclosingClassFqName = null,
                    enclosingFunctionName = null,
                ),
                modifiers = emptyList(),
                carriedAnnotations = emptyList(),
                capturedRawSyntax = null,
                containerContext = MacroSurfaceContainerContext(
                    outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.FUNCTION_BODY,
                    isInsidePrimaryConstructor = false,
                    isInsideEnumBody = false,
                    isInsideBlock = true,
                ),
                replaceHandle = CfirReplaceHandle(name.hashCode().toLong(), topLevelMacroOwnerSnapshot(FqName("test"))),
            ),
            parent = null,
            children = emptyList(),
        )
    }

    /**
     * 构造测试宏 surface token。
     */
    private fun token(
        text: String,
        startOffset: Int = 0,
        sourceRange: MacroSurfaceSourceRange? = null,
        isSourceToken: Boolean = false,
    ): MacroSurfaceToken = MacroSurfaceToken(
        text = text,
        startOffset = startOffset,
        endOffset = startOffset + text.length,
        kindName = "TEST",
        sourceRange = sourceRange,
        isSourceToken = isSourceToken,
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> proxy(type: Class<T>): T = Proxy.newProxyInstance(
        type.classLoader,
        arrayOf(type),
    ) { _, _, _ -> null } as T

    @Suppress("UNCHECKED_CAST")
    private fun lightSourceElement(startOffset: Int, endOffset: Int): CjLightSourceElement = CjLightSourceElement(
        lighterASTNode = proxy(LighterASTNode::class.java),
        startOffset = startOffset,
        endOffset = endOffset,
        treeStructure = Proxy.newProxyInstance(
            FlyweightCapableTreeStructure::class.java.classLoader,
            arrayOf(FlyweightCapableTreeStructure::class.java),
        ) { _, _, _ -> null } as FlyweightCapableTreeStructure<LighterASTNode>,
    )
}
