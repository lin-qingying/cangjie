package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallForestBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceDecl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** LightTree 宏表面只暴露 delimiters 内部的 attr/input token。 */
@RunWith(JUnit3RunnerWithInners::class)
class MacroSurfacePayloadTest : AbstractLightTree2CfirConverterTestCase() {
    fun testExpressionMacroSurfaceCapturesOnlyBracketContentsAndPreservesHostOffsets() {
        val source = """
            package nestedmacros

            func payloadProbe(): Int64 {
                let value: Int64 = @Echo[  tag  ]( 1 + 2 )
                return value
            }
        """.trimIndent()
        val sourceFile = CjInMemoryTextSourceFile("macroExpressionPayload.cj", null, source)
        val session = createTestSession()
        val (_, surfaces) = LightTree2Cfir(
            session = session,
            scopeProvider = session.cangjieScopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL,
        ).buildCfirFileWithSurfaces(
            source,
            sourceFile,
            source.toSourceLinesMapping(),
        )
        val surface = surfaces.filterIsInstance<MacroSurfaceExpr>().single()

        assertTrue(surface.hasAttr)
        assertTrue(surface.hasParenthesis)
        assertEquals("  tag  ", surface.attrTokens.joinToString(separator = "") { it.text })
        assertEquals(" 1 + 2 ", surface.inputTokens.joinToString(separator = "") { it.text })
        for (token in surface.attrTokens + surface.inputTokens) {
            assertEquals(
                "Macro payload token offsets must stay in the physical host file.",
                token.text,
                source.substring(token.startOffset, token.endOffset),
            )
        }
    }

    fun testLightTreeMacroSurfaceKeepsDelimiterShapeAndDoesNotSendPartialPayloads() {
        val source = """
            package nestedmacros

            func payloadProbe(): Int64 {
                let emptyInput: Int64 = @EmptyInput()
                let emptyAttribute: Int64 = @EmptyAttribute[]()
                let incomplete: Int64 = @Incomplete( 1 + 2
                return emptyInput
            }
        """.trimIndent()
        val sourceFile = CjInMemoryTextSourceFile("macroExpressionPayloadShapes.cj", null, source)
        val session = createTestSession()
        val (_, allSurfaces) = LightTree2Cfir(
            session = session,
            scopeProvider = session.cangjieScopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL,
        ).buildCfirFileWithSurfaces(
            source,
            sourceFile,
            source.toSourceLinesMapping(),
        )
        val surfaces = allSurfaces.filterIsInstance<MacroSurfaceExpr>()
            .associateBy { it.qualifiedName?.shortName()?.asString() }
        val emptyInput = surfaces.getValue("EmptyInput")
        val emptyAttribute = surfaces.getValue("EmptyAttribute")
        val incomplete = surfaces.getValue("Incomplete")

        assertFalse(emptyInput.hasAttr)
        assertTrue(emptyInput.hasParenthesis)
        assertTrue(emptyInput.inputTokens.isEmpty())
        assertTrue(emptyAttribute.hasAttr)
        assertTrue(emptyAttribute.hasParenthesis)
        assertTrue(emptyAttribute.attrTokens.isEmpty())
        assertFalse(incomplete.hasParenthesis)
        assertTrue(incomplete.inputTokens.isEmpty())
    }

    fun testLightTreeMacroSurfaceRejectsInvalidEscapeAndKeepsOnlyAcceptedInputPrefix() {
        val source = """
            package nestedmacros

            func payloadProbe(): Int64 {
                let value: Int64 = @InvalidEscape[  tag \( 2 ]( 1 + \[ 2 )
                return value
            }
        """.trimIndent()
        val sourceFile = CjInMemoryTextSourceFile("macroExpressionInvalidEscape.cj", null, source)
        val session = createTestSession()
        val (_, allSurfaces) = LightTree2Cfir(
            session = session,
            scopeProvider = session.cangjieScopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL,
        ).buildCfirFileWithSurfaces(
            source,
            sourceFile,
            source.toSourceLinesMapping(),
        )
        val surface = allSurfaces.filterIsInstance<MacroSurfaceExpr>().single()

        assertTrue(surface.hasAttr)
        assertEquals("  tag ", surface.attrTokens.joinToString(separator = "") { it.text })
        assertFalse(surface.hasParenthesis)
        assertEquals(" 1 + ", surface.inputTokens.joinToString(separator = "") { it.text })
        assertTrue(surface.inputTokens.all { token ->
            source.substring(token.startOffset, token.endOffset) == token.text
        })
    }

    fun testLightTreeDeclarationMacroPreservesNoParenthesisInputShape() {
        val source = """
            package nestedmacros

            @Outer
            class OuterType {
                @Inner
                var member: Int64 = 1
            }
        """.trimIndent()
        val sourceFile = CjInMemoryTextSourceFile("macroDeclarationPayload.cj", null, source)
        val session = createTestSession()
        val (_, surfaces) = LightTree2Cfir(
            session = session,
            scopeProvider = session.cangjieScopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL,
        ).buildCfirFileWithSurfaces(
            source,
            sourceFile,
            source.toSourceLinesMapping(),
        )
        val declarationSurfaces = surfaces.filterIsInstance<MacroSurfaceDecl>()

        assertEquals(2, declarationSurfaces.size)
        assertTrue(declarationSurfaces.all { !it.hasParenthesis && it.inputTokens.isNotEmpty() })
    }

    fun testLightTreeDeclarationMacroCapturesPayloadWithoutWrapperDelimiters() {
        val source = """
            package nestedmacros

            @Outer[  tag  ]
            class OuterType {}
        """.trimIndent()
        val sourceFile = CjInMemoryTextSourceFile("macroDeclarationPayload.cj", null, source)
        val session = createTestSession()
        val (_, allSurfaces) = LightTree2Cfir(
            session = session,
            scopeProvider = session.cangjieScopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL,
        ).buildCfirFileWithSurfaces(
            source,
            sourceFile,
            source.toSourceLinesMapping(),
        )
        val surface = allSurfaces.filterIsInstance<MacroSurfaceDecl>().single()
        val inputText = surface.inputTokens.joinToString(separator = "") { it.text }

        assertTrue(surface.hasAttr)
        assertFalse(surface.hasParenthesis)
        assertEquals("  tag  ", surface.attrTokens.joinToString(separator = "") { it.text })
        assertTrue(inputText.contains("class OuterType"))
        assertFalse(inputText.contains("[") || inputText.contains("]"))
        for (token in surface.attrTokens + surface.inputTokens) {
            assertEquals(
                "Declaration macro token offsets must stay in the physical host file.",
                token.text,
                source.substring(token.startOffset, token.endOffset),
            )
        }
    }

    fun testLightTreeForcedAnnotationMacroUsesOnlyBracketArgumentContents() {
        val source = """
            package nestedmacros

            @!Outer[  tag  ]
            class OuterType {}
        """.trimIndent()
        val sourceFile = CjInMemoryTextSourceFile("forcedAnnotationMacroPayload.cj", null, source)
        val session = createTestSession()
        val (_, allSurfaces) = LightTree2Cfir(
            session = session,
            scopeProvider = session.cangjieScopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL,
        ).buildCfirFileWithSurfaces(
            source,
            sourceFile,
            source.toSourceLinesMapping(),
        )
        val surface = allSurfaces.filterIsInstance<MacroSurfaceDecl>().single()
        val attrText = surface.attrTokens.joinToString(separator = "") { it.text }

        assertTrue(surface.hasAttr)
        assertEquals("  tag  ", attrText)
        assertFalse(attrText.contains("[") || attrText.contains("]"))
        for (token in surface.attrTokens) {
            assertEquals(token.text, source.substring(token.startOffset, token.endOffset))
        }
    }

    fun testLightTreeForcedAnnotationMacroStopsAtInvalidArgumentSyntax() {
        val source = """
            package nestedmacros

            @!Outer[ 1 /* keep */ 2 ]
            class OuterType {}
        """.trimIndent()
        val sourceFile = CjInMemoryTextSourceFile("forcedAnnotationMacroInvalidPayload.cj", null, source)
        val session = createTestSession()
        val (_, allSurfaces) = LightTree2Cfir(
            session = session,
            scopeProvider = session.cangjieScopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL,
        ).buildCfirFileWithSurfaces(
            source,
            sourceFile,
            source.toSourceLinesMapping(),
        )
        val surface = allSurfaces.filterIsInstance<MacroSurfaceDecl>().single()
        val attrText = surface.attrTokens.joinToString(separator = "") { it.text }

        assertTrue(surface.hasAttr)
        assertEquals(" 1 /* keep */ ", attrText)
    }

    fun testNestedCustomAnnotationRemainsInsidePhysicalAttrPayload() {
        val source = """
            package nestedmacros

            @Annotation
            public class A { const init(_: Int64) {} }

            @A[{ =>
                @A[0] let x = 0
                x
            }()]
            class B {}
        """.trimIndent()
        val sourceFile = CjInMemoryTextSourceFile("nestedCustomAnnotationPayload.cj", null, source)
        val session = createTestSession()
        val (_, allSurfaces) = LightTree2Cfir(
            session = session,
            scopeProvider = session.cangjieScopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL,
        ).buildCfirFileWithSurfaces(
            source,
            sourceFile,
            source.toSourceLinesMapping(),
        )
        val annotationSurfaces = allSurfaces.filterIsInstance<MacroSurfaceDecl>()
            .filter { it.qualifiedName?.shortName()?.asString() == "A" }
            .sortedBy { it.sourceRange?.startOffset }
        assertEquals(1, annotationSurfaces.size)

        val outer = annotationSurfaces.single()
        val outerCall = source.indexOf("@A[")
        val outerAttrClose = source.indexOf("}()]", outerCall)
        assertTrue(outerCall >= 0)
        assertTrue(outerAttrClose >= 0)
        val attrStart = outerCall + "@A[".length
        val attrEnd = outerAttrClose + "}()".length
        assertEquals(attrStart, outer.attrTokens.first().startOffset)
        assertEquals(attrEnd, outer.attrTokens.last().endOffset)
        assertEquals(source.substring(attrStart, attrEnd), outer.attrTokens.joinToString(separator = "") { it.text })
        assertTrue(outer.attrTokens.zipWithNext().all { (left, right) -> left.endOffset == right.startOffset })
        assertTrue(outer.attrTokens.all { token ->
            token.text == source.substring(token.startOffset, token.endOffset)
        })

        val forest = MacroCallForestBuilder.build(annotationSurfaces)
        val outerNode = forest.allNodes.single { it.surface === outer }
        assertTrue(outerNode.childEdges.isEmpty())
    }

    /**
     * 宏 Tokens 输入必须包含移位运算符与字节字面量：
     * 旧实现的 quote token 集合缺少 `<<`、`b'0'` 对应 token，
     * 会让 `@NoFormat(...)`、`@assert(...)` 的输入在中途截断。
     */
    fun testMacroSurfaceKeepsShiftOperatorAndByteLiteralTokens() {
        val source = """
            package payloadprobe

            func payloadProbe(x: Int64, b: UInt8): Int64 {
                let shifted = @NoFormat(x << 1)
                @assert(b >= b'0' && b <= b'9')
                shifted
            }
        """.trimIndent()
        val sourceFile = CjInMemoryTextSourceFile("macroSurfaceTokens.cj", null, source)
        val session = createTestSession()
        val (_, allSurfaces) = LightTree2Cfir(
            session = session,
            scopeProvider = session.cangjieScopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL,
        ).buildCfirFileWithSurfaces(
            source,
            sourceFile,
            source.toSourceLinesMapping(),
        )
        val expressionSurfaces = allSurfaces.filterIsInstance<MacroSurfaceExpr>()
            .associateBy { it.qualifiedName?.shortName()?.asString() }

        val noFormat = expressionSurfaces.getValue("NoFormat")
        assertTrue(
            noFormat.inputTokens.joinToString(separator = "") { it.text }.contains("<< 1"),
            "NoFormat input must keep the shift operator: ${noFormat.inputTokens.joinToString(separator = "") { it.text }}",
        )

        val assertSurface = expressionSurfaces.getValue("assert")
        assertTrue(
            assertSurface.inputTokens.joinToString(separator = "") { it.text }.contains("b'0'"),
            "assert input must keep the byte literal: ${assertSurface.inputTokens.joinToString(separator = "") { it.text }}",
        )
    }
}
