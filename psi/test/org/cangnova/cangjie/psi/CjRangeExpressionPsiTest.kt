package org.cangnova.cangjie.psi

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** 范围表达式保持独立的 AST PSI 契约，不进入普通二元表达式 Stub 层级。 */
class CjRangeExpressionPsiTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun rangePsiUsesItsAstElementTypeAndKeepsRangeEndpoints() {
        val cases = listOf(
            RangeCase("1..10:3", "1", "10", "3", CjTokens.RANGE, hasParserError = false),
            RangeCase("1..=10:3", "1", "10", "3", CjTokens.RANGEEQ, hasParserError = false),
            RangeCase("1..:3", "1", null, "3", CjTokens.RANGE, hasParserError = true),
        )

        cases.forEach { case ->
            val expression = CjPsiFactory(project).createExpression(case.text)
            val range = expression as CjRangeExpression

            assertEquals(case.text, range.text)
            assertSame(CjNodeTypes.RANGE_EXPRESSION, range.elementType)
            assertEquals(range.elementType, range.node.elementType)
            assertFalse(expression is StubBasedPsiElement<*>)
            assertFalse(expression is CjBinaryExpression)
            assertSame(case.operator, range.operationToken)
            assertEquals(case.operator == CjTokens.RANGEEQ, range.isInclusive)
            assertEquals(case.start, range.startExpression?.text)
            assertEquals(case.stop, range.stopExpression?.text)
            assertEquals(case.step, range.step?.text)
            assertEquals(listOfNotNull(case.start, case.stop, case.step), range.getInnerExpressions().map { it.text })
            assertEquals(
                case.hasParserError,
                PsiTreeUtil.findChildrenOfType(expression, PsiErrorElement::class.java).isNotEmpty(),
            )
        }
    }

    @Test
    fun rangePsiRemainsAstBasedInsideAValueArgument() {
        val call = CjPsiFactory(project).createExpression("consume(1..=10:3)")
        val range = assertNotNull(PsiTreeUtil.findChildOfType(call, CjRangeExpression::class.java))

        assertSame(CjNodeTypes.RANGE_EXPRESSION, range.elementType)
        val rangeExpression: CjExpression = range
        assertFalse(rangeExpression is StubBasedPsiElement<*>)
        assertFalse(rangeExpression is CjBinaryExpression)
        assertEquals("1", range.startExpression?.text)
        assertEquals("10", range.stopExpression?.text)
        assertEquals("3", range.step?.text)
        assertTrue(PsiTreeUtil.findChildrenOfType(call, PsiErrorElement::class.java).isEmpty())
    }

    @Test
    fun indexSlicesUseTheSharedAstRangeStructureWithOptionalEndpoints() {
        val cases = listOf(
            SliceCase("array[..]", null, null, CjTokens.RANGE),
            SliceCase("array[..10]", null, "10", CjTokens.RANGE),
            SliceCase("array[1..]", "1", null, CjTokens.RANGE),
            SliceCase("array[..=10]", null, "10", CjTokens.RANGEEQ),
        )

        cases.forEach { case ->
            val expression = CjPsiFactory(project).createExpression(case.text)
            val slice = assertNotNull(PsiTreeUtil.findChildOfType(expression, CjSliceExpression::class.java))

            assertEquals(case.text.substringAfter('[').substringBeforeLast(']'), slice.text)
            assertSame(CjNodeTypes.SLICE_EXPRESSION, slice.elementType)
            assertEquals(slice.elementType, slice.node.elementType)
            val sliceExpression: CjExpression = slice
            assertFalse(sliceExpression is StubBasedPsiElement<*>)
            assertFalse(sliceExpression is CjUnaryExpression)
            assertSame(case.operator, slice.rangeOperatorToken)
            assertEquals(case.expectedStart, slice.startExpression?.text)
            assertEquals(case.expectedStop, slice.stopExpression?.text)
            assertEquals(listOfNotNull(case.expectedStart, case.expectedStop), slice.getInnerExpressions().map { it.text })
            assertTrue(PsiTreeUtil.findChildrenOfType(expression, PsiErrorElement::class.java).isEmpty())
        }
    }

    @Test
    fun indexedRangesKeepTheRangePsiAndDoNotTreatStepAsStop() {
        val cases = listOf(
            RangeCase("array[1..=2]", "1", "2", null, CjTokens.RANGEEQ, hasParserError = false),
        )

        cases.forEach { case ->
            val expression = CjPsiFactory(project).createExpression(case.text)
            val range = assertNotNull(PsiTreeUtil.findChildOfType(expression, CjRangeExpression::class.java))

            assertSame(CjNodeTypes.RANGE_EXPRESSION, range.elementType)
            assertEquals(case.operator == CjTokens.RANGEEQ, range.isInclusive)
            assertEquals(case.start, range.startExpression?.text)
            assertEquals(case.stop, range.stopExpression?.text)
            assertEquals(case.step, range.step?.text)
            assertEquals(
                case.hasParserError,
                PsiTreeUtil.findChildrenOfType(expression, PsiErrorElement::class.java).isNotEmpty(),
            )
        }
    }

    @Test
    fun closedIndexSlicesRequireAnEndExpression() {
        listOf("array[..=]", "array[1..=]").forEach { text ->
            val expression = CjPsiFactory(project).createExpression(text)
            val slice = assertNotNull(PsiTreeUtil.findChildOfType(expression, CjSliceExpression::class.java))

            assertSame(CjTokens.RANGEEQ, slice.rangeOperatorToken)
            assertTrue(PsiTreeUtil.findChildrenOfType(expression, PsiErrorElement::class.java).isNotEmpty())
        }
    }

    @Test
    fun realCjFileKeepsRangeAndSlicePsiTypesDuringTreeTraversal() {
        val file = createPsiFile(
            "rangeAndSlicePsi",
            """
            func rangeAndSlice(array: Array<Int64>): Unit {
                let range = 1..10:3
                let slice = array[..10]
            }
            """.trimIndent(),
        ) as CjFile
        val range = PsiTreeUtil.findChildrenOfType(file, CjRangeExpression::class.java).single()
        val slice = PsiTreeUtil.findChildrenOfType(file, CjSliceExpression::class.java).single()

        assertSame(CjNodeTypes.RANGE_EXPRESSION, range.elementType)
        assertSame(CjNodeTypes.SLICE_EXPRESSION, slice.elementType)
        assertEquals("1", range.startExpression?.text)
        assertEquals("10", range.stopExpression?.text)
        assertEquals("3", range.step?.text)
        assertEquals("10", slice.stopExpression?.text)
        assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).isEmpty())
    }

    @Test
    fun ordinaryBinaryPsiKeepsItsStubElementType() {
        val binary = CjPsiFactory(project).createExpression("1 + 2") as CjBinaryExpression

        assertTrue(binary is StubBasedPsiElement<*>)
        assertSame(CjStubElementTypes.BINARY_EXPRESSION, binary.elementType)
        assertEquals("1", binary.left?.text)
        assertEquals("2", binary.right?.text)
    }

    private data class SliceCase(
        val text: String,
        val expectedStart: String?,
        val expectedStop: String?,
        val operator: IElementType,
    )

    private data class RangeCase(
        val text: String,
        val start: String?,
        val stop: String?,
        val step: String?,
        val operator: IElementType,
        val hasParserError: Boolean,
    )
}
