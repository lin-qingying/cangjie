package org.cangnova.cangjie.codeinsight.folding

import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.psi.util.PsiTreeUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class CangJieFoldingRangeCollectorTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    @Test
    fun testMultipleImportsProduceSingleImportsRegion() {
        val regions = collect(
            """
            import std.collection.ArrayList
            import std.collection.HashMap

            func main() {}
            """.trimIndent(),
        )

        val imports = regions.filter { it.kind == CangJieFoldingKind.IMPORTS }
        assertEquals(1, imports.size)
        assertEquals("...", imports.single().placeholderText)
        assertTrue(imports.single().canBeRemovedWhenCollapsed)
    }

    @Test
    fun testBlockClassBodyFunctionLiteralAndMatchProduceRegion() {
        val regions = collect(
            """
            class Box {
                func value(): Int64 {
                    let mapper = { value =>
                        value
                    }
                    return match (1) {
                        case 1 => 1
                        case _ => 0
                    }
                }
            }
            """.trimIndent(),
        )

        val regionTexts = regions
            .filter { it.kind == CangJieFoldingKind.REGION }
            .map { it.element.text }

        assertTrue(regionTexts.any { it.startsWith("{") && "func value" in it })
        assertTrue(regionTexts.any { "let mapper" in it && "return match" in it })
        assertTrue(regionTexts.any { it.startsWith("{ value =>") || it.startsWith("{value =>") })
        assertTrue(regionTexts.any { it.startsWith("match") })
    }

    @Test
    fun testSingleLineBlockAndCallDoNotProduceRegion() {
        val file = parse(
            """
            func single() { return singleLine(1, 2) }
            """.trimIndent(),
        )
        val document = DocumentImpl(file.text)
        val regions = CangJieFoldingRangeCollector.collect(file, document)

        assertFalse(regions.any { it.element is CjCallExpression })
        assertTrue(regions.isEmpty() || regions.all { "\n" in it.element.text })
    }

    @Test
    fun testMultilineCallProducesRegionWithParenthesesPlaceholder() {
        val file = parse(
            """
            func main() {
                consume(
                    1,
                    2
                )
            }
            """.trimIndent(),
        )
        val regions = CangJieFoldingRangeCollector.collect(file, DocumentImpl(file.text))
        val call = PsiTreeUtil.findChildOfType(file, CjCallExpression::class.java)
        val callRegion = regions.singleOrNull { it.element == call } ?: error("call folding region not found")

        assertEquals(CangJieFoldingKind.REGION, callRegion.kind)
        assertEquals("(...)", callRegion.placeholderText)
    }

    @Test
    fun testBlockCommentAndCDocProduceCommentRegion() {
        val regions = collect(
            """
            /*
             * block text
             */
            /**
             * doc text
             */
            func main() {}
            """.trimIndent(),
        )

        val comments = regions.filter { it.kind == CangJieFoldingKind.COMMENT }
        assertEquals(2, comments.size)
        assertTrue(comments.any { it.placeholderText == "/ block text .../" })
        assertTrue(comments.any { it.placeholderText == "/** doc text ...*/" })
    }

    private fun collect(text: String): List<CangJieFoldingRegion> {
        val file = parse(text)
        return CangJieFoldingRangeCollector.collect(file, DocumentImpl(file.text))
    }

    private fun parse(text: String): CjFile {
        return createPsiFile("folding", text) as CjFile
    }
}
