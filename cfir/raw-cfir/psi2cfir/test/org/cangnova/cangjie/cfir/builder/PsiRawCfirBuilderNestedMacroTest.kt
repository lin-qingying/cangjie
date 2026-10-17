package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.ObsoleteTestInfrastructure
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallForestBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroPayloadChannel
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceDecl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 声明宏嵌套成员宏时，Raw CFIR 必须按 PSI 声明边界构建一次。 */
@RunWith(JUnit3RunnerWithInners::class)
@OptIn(ObsoleteTestInfrastructure::class)
@ObsoleteTestInfrastructure
class PsiRawCfirBuilderNestedMacroTest : AbstractRawCfirBuilderTestCase() {
    fun testExpressionMacroSurfaceCapturesOnlyBracketContentsAndPreservesHostOffsets() {
        val file = createCjFile(
            "macroExpressionPayload",
            """
            package nestedmacros

            func payloadProbe(): Int64 {
                let value: Int64 = @Echo[  tag  ]( 1 + 2 )
                return value
            }
            """.trimIndent(),
        )
        val builder = PsiRawCfirBuilder(createTestSession(), BodyBuildingMode.NORMAL)
        builder.buildCfirFile(file)

        val surface = builder.consumeCollectedMacroSurfaces()
            .filterIsInstance<MacroSurfaceExpr>()
            .single()

        assertTrue(surface.hasAttr)
        assertTrue(surface.hasParenthesis)
        assertEquals("  tag  ", surface.attrTokens.joinToString(separator = "") { it.text })
        assertEquals(" 1 + 2 ", surface.inputTokens.joinToString(separator = "") { it.text })
        for (token in surface.attrTokens + surface.inputTokens) {
            assertEquals(
                "Macro payload token offsets must stay in the physical host file.",
                token.text,
                file.text.substring(token.startOffset, token.endOffset),
            )
        }
    }

    fun testExpressionMacroSurfaceDistinguishesEmptyPayloadsAndIncompleteParentheses() {
        val file = createCjFile(
            "macroExpressionPayloadShapes",
            """
            package nestedmacros

            func payloadProbe(): Int64 {
                let emptyInput: Int64 = @EmptyInput()
                let emptyAttribute: Int64 = @EmptyAttribute[]()
                let incomplete: Int64 = @Incomplete( 1 + 2
                return emptyInput
            }
            """.trimIndent(),
        )
        val builder = PsiRawCfirBuilder(createTestSession(), BodyBuildingMode.NORMAL)
        builder.buildCfirFile(file)

        val surfaces = builder.consumeCollectedMacroSurfaces().filterIsInstance<MacroSurfaceExpr>()
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

    fun testExpressionMacroSurfaceRejectsInvalidEscapeAndKeepsOnlyAcceptedInputPrefix() {
        val file = createCjFile(
            "macroExpressionInvalidEscape",
            """
            package nestedmacros

            func payloadProbe(): Int64 {
                let value: Int64 = @InvalidEscape[  tag \( 2 ]( 1 + \[ 2 )
                return value
            }
            """.trimIndent(),
        )
        val builder = PsiRawCfirBuilder(createTestSession(), BodyBuildingMode.NORMAL)
        builder.buildCfirFile(file)

        val surface = builder.consumeCollectedMacroSurfaces().filterIsInstance<MacroSurfaceExpr>().single()

        assertTrue(surface.hasAttr)
        assertEquals("  tag ", surface.attrTokens.joinToString(separator = "") { it.text })
        assertFalse(surface.hasParenthesis)
        assertEquals(" 1 + ", surface.inputTokens.joinToString(separator = "") { it.text })
        assertTrue(surface.inputTokens.all { token ->
            file.text.substring(token.startOffset, token.endOffset) == token.text
        })
    }

    fun testDeclarationMacroSurfaceCapturesPayloadWithoutWrapperDelimiters() {
        val file = createCjFile(
            "macroDeclarationPayload",
            """
            package nestedmacros

            @Outer[  tag  ]
            class OuterType {}
            """.trimIndent(),
        )
        val builder = PsiRawCfirBuilder(createTestSession(), BodyBuildingMode.NORMAL)
        builder.buildCfirFile(file)

        val surface = builder.consumeCollectedMacroSurfaces().filterIsInstance<MacroSurfaceDecl>().single()
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
                file.text.substring(token.startOffset, token.endOffset),
            )
        }
    }

    fun testForcedAnnotationMacroSurfaceCapturesBracketArgumentContentsOnly() {
        val file = createCjFile(
            "forcedAnnotationMacroPayload",
            """
            package nestedmacros

            @!Outer[  tag  ]
            class OuterType {}
            """.trimIndent(),
        )
        val builder = PsiRawCfirBuilder(createTestSession(), BodyBuildingMode.NORMAL)
        builder.buildCfirFile(file)

        val surface = builder.consumeCollectedMacroSurfaces().filterIsInstance<MacroSurfaceDecl>().single()
        val attrText = surface.attrTokens.joinToString(separator = "") { it.text }

        assertTrue(surface.hasAttr)
        assertEquals("  tag  ", attrText)
        assertFalse(attrText.contains("[") || attrText.contains("]"))
        for (token in surface.attrTokens) {
            assertEquals(token.text, file.text.substring(token.startOffset, token.endOffset))
        }
    }

    fun testForcedAnnotationMacroSurfaceStopsAtInvalidArgumentSyntax() {
        val file = createCjFile(
            "forcedAnnotationMacroInvalidPayload",
            """
            package nestedmacros

            @!Outer[ 1 /* keep */ 2 ]
            class OuterType {}
            """.trimIndent(),
        )
        val builder = PsiRawCfirBuilder(createTestSession(), BodyBuildingMode.NORMAL)
        builder.buildCfirFile(file)

        val surface = builder.consumeCollectedMacroSurfaces().filterIsInstance<MacroSurfaceDecl>().single()
        val attrText = surface.attrTokens.joinToString(separator = "") { it.text }

        assertTrue(surface.hasAttr)
        assertEquals(" 1 /* keep */ ", attrText)
    }

    fun testNestedCustomAnnotationSurfaceUsesPhysicalAttrPayloadEdge() {
        val file = createCjFile(
            "nestedCustomAnnotationPayload",
            """
            package nestedmacros

            @Annotation
            public class A { const init(_: Int64) {} }

            @A[{ =>
                @A[0] let x = 0
                x
            }()]
            class B {}
            """.trimIndent(),
        )
        val builder = PsiRawCfirBuilder(createTestSession(), BodyBuildingMode.NORMAL)
        builder.buildCfirFile(file)

        val annotationSurfaces = builder.consumeCollectedMacroSurfaces()
            .filterIsInstance<MacroSurfaceDecl>()
            .filter { it.qualifiedName?.shortName()?.asString() == "A" }
            .sortedBy { it.sourceRange?.startOffset }
        assertEquals(2, annotationSurfaces.size)

        val (outer, inner) = annotationSurfaces
        val outerRange = checkNotNull(outer.sourceRange)
        val innerRange = checkNotNull(inner.sourceRange)
        val attrStart = outer.attrTokens.minOf { it.startOffset }
        val attrEnd = outer.attrTokens.maxOf { it.endOffset }
        assertTrue(
            innerRange.startOffset >= attrStart && innerRange.endOffset <= attrEnd,
            "The nested annotation range must be inside the outer attr tokens in physical file coordinates: " +
                    "inner=${innerRange.startOffset}..${innerRange.endOffset} " +
                    "outerAttr=$attrStart..$attrEnd " +
                    "innerText=${inner.capturedRawSyntax} " +
                    "outerAttrText=${file.text.substring(attrStart, attrEnd)}",
        )
        assertTrue(
            outer.attrTokens.all { token ->
                token.text == file.text.substring(token.startOffset, token.endOffset)
            },
            "Every outer attr token must retain its exact physical file slice.",
        )
        assertEquals(
            "The token stream must preserve the complete physical attr payload, including trivia.",
            file.text.substring(attrStart, attrEnd),
            outer.attrTokens.joinToString(separator = "") { it.text },
        )
        assertTrue(
            outer.attrTokens.zipWithNext().all { (left, right) -> left.endOffset == right.startOffset },
            "Adjacent attr tokens must cover the payload without gaps.",
        )

        val forest = MacroCallForestBuilder.build(annotationSurfaces)
        val innerNode = forest.allNodes.single { it.surface === inner }
        val outerNode = forest.allNodes.single { it.surface === outer }
        assertSame(outerNode, innerNode.parent)
        assertEquals(
            MacroPayloadChannel.ATTR,
            outerNode.childEdges.single { it.child === innerNode }.channel,
        )
        assertEquals(innerRange, outerNode.childEdges.single { it.child === innerNode }.replaceRange)
        assertEquals(outerRange, outerNode.surface.sourceRange)
    }

    fun testNestedClassMemberMacrosBuildOneClassAndAllMacroSurfaces() {
        val file = createCjFile(
            "nestedClassMemberMacros",
            """
            package nestedmacros

            @ContextOuter
            class ContextMacroProbe {
                @ContextInner
                var firstCount: Int64 = 40

                @ContextInner
                var secondCount: Int64 = 2
            }
            """.trimIndent(),
        )
        val builder = PsiRawCfirBuilder(createTestSession(), BodyBuildingMode.NORMAL)

        val cfirFile = builder.buildCfirFile(file)
        val macroSurfaces = builder.consumeCollectedMacroSurfaces()

        assertEquals(1, cfirFile.declarations.size)
        assertEquals(3, macroSurfaces.size)
        val declarationSurfaces = macroSurfaces.filterIsInstance<MacroSurfaceDecl>()
        assertEquals(3, declarationSurfaces.size)
        assertTrue(declarationSurfaces.all { !it.hasParenthesis && it.inputTokens.isNotEmpty() })
    }
}
