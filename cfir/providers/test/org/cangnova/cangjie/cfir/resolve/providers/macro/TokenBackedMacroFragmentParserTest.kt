package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 验证基于 token 的宏片段解析器在重新 token 化、重新解析和失败路径上的行为。
 */
class TokenBackedMacroFragmentParserTest {
    /**
     * 验证空 token 流会直接失败，并且不会触发重解析回调。
     */
    @Test
    fun `empty token stream returns failure without invoking reparse`() {
        var reparseCalls = 0
        val parser = TokenBackedMacroFragmentParser(
            reparse = { _, _ ->
                reparseCalls++
                Any()
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

    /**
     * 验证表达式模式下成功重解析会返回 construction-only 的成功片段。
     */
    @Test
    fun `successful expression parse returns construction-only success`() {
        val received = mutableListOf<Pair<String, MacroFragmentParser.Mode>>()
        val parser = TokenBackedMacroFragmentParser(
            reparse = { text, input ->
                received += text to input.mode
                Any()
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
                Any()
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
                Any()
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
        )
    }

    /**
     * 构造没有父子关系的测试宏调用节点。
     */
    private fun node(name: String): MacroCallNode {
        return MacroCallNode(
            surface = MacroSurfaceExpr(
                surfaceId = name.hashCode().toLong(),
                qualifiedName = FqName(name),
                kind = MacroSurface.Kind.PLAIN,
                hasParenthesis = true,
                attrTokens = emptyList(),
                inputTokens = emptyList(),
                sourceRange = MacroSurfaceSourceRange(
                    source = null,
                    startOffset = 0,
                    endOffset = name.length,
                ),
                scopeContext = MacroSurfaceScopeContext(
                    packageFqName = FqName("test"),
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
                replaceHandle = CfirReplaceHandle(name.hashCode().toLong()),
            ),
            parent = null,
            children = emptyList(),
        )
    }

    /**
     * 构造测试宏 surface token。
     */
    private fun token(text: String): MacroSurfaceToken = MacroSurfaceToken(
        text = text,
        startOffset = 0,
        endOffset = text.length,
        kindName = "TEST",
    )
}
