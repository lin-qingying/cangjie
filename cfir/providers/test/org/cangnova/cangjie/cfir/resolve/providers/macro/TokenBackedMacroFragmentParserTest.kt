package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.name.FqName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TokenBackedMacroFragmentParserTest {
    @Test
    fun `empty token stream returns failure without invoking reparse`() {
        var reparseCalls = 0
        val parser = TokenBackedMacroFragmentParser(
            reparse = { _, _, _ ->
                reparseCalls++
                Any()
            },
            reTokenize = MacroTokenReEvaluator::preserveTextTokens,
        )

        val result = parser.parse(
            node = node("Empty"),
            tokens = emptyList(),
            mode = MacroFragmentParser.Mode.EXPRESSION,
        )

        assertTrue(result is MacroFragmentResult.Failure)
        assertEquals("Macro fragment text is empty after token re-evaluation", (result as MacroFragmentResult.Failure).reason)
        assertEquals(0, reparseCalls)
    }

    @Test
    fun `successful expression parse returns construction-only success`() {
        val received = mutableListOf<Pair<String, MacroFragmentParser.Mode>>()
        val parser = TokenBackedMacroFragmentParser(
            reparse = { text, mode, _ ->
                received += text to mode
                Any()
            },
            reTokenize = { it },
        )
        val tokens = listOf(token("1"), token(" + "), token("2"))

        val result = parser.parse(
            node = node("ExprMacro"),
            tokens = tokens,
            mode = MacroFragmentParser.Mode.EXPRESSION,
        )

        assertTrue(result is MacroFragmentResult.Success)
        val success = result as MacroFragmentResult.Success
        assertEquals(MacroFragmentParser.Mode.EXPRESSION, success.mode)
        assertEquals(tokens, success.tokens)
        assertEquals(listOf("1 + 2" to MacroFragmentParser.Mode.EXPRESSION), received)
    }

    @Test
    fun `custom annotation mode returns CustomAnnotation without final CFIR payload`() {
        val parser = TokenBackedMacroFragmentParser(
            reparse = { text, mode, _ ->
                assertEquals("@Anno(value)", text)
                assertEquals(MacroFragmentParser.Mode.CUSTOM_ANNOTATION, mode)
                "raw-builder-payload-must-not-escape"
            },
            reTokenize = MacroTokenReEvaluator::preserveTextTokens,
        )

        val result = parser.parse(
            node = node("pkg.Anno"),
            tokens = listOf(token("@Anno"), token("("), token("value"), token(")")),
            mode = MacroFragmentParser.Mode.CUSTOM_ANNOTATION,
        )

        assertTrue(result is MacroFragmentResult.CustomAnnotation)
        val annotation = result as MacroFragmentResult.CustomAnnotation
        assertEquals("Anno", annotation.annotationName.asString())
        assertEquals("@Anno(value)", annotation.tokens.joinToString(separator = "") { it.text })
    }

    @Test
    fun `parser consumes token-stage re-evaluation output before reparse`() {
        val received = mutableListOf<String>()
        val parser = TokenBackedMacroFragmentParser(
            reparse = { text, _, _ ->
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

        val result = parser.parse(
            node = node("ExprMacro"),
            tokens = listOf(token("ignored text")),
            mode = MacroFragmentParser.Mode.EXPRESSION,
        )

        assertTrue(result is MacroFragmentResult.Success)
        assertEquals(listOf("normalized(42)"), received)
        assertEquals("normalized(42)", (result as MacroFragmentResult.Success).tokens.joinToString(separator = "") { it.text })
    }

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

    private fun token(text: String): MacroSurfaceToken = MacroSurfaceToken(
        text = text,
        startOffset = 0,
        endOffset = text.length,
        kindName = "TEST",
    )
}
