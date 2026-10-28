package org.cangnova.cangjie.psi

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * 平台 2025.3 起 stub-based PSI 必须通过 `getIElementType()` 暴露元素类型；
 * 由 AST 构造、节点类型又不是 stub 类型的元素（如 `MATCH_CONDITION_WITH_EXPRESSION`）
 * 不能走已废弃的 `getElementType()`，否则高亮/chameleon 遍历会抛 `ClassCastException`。
 */
class StubBasedElementTypeTest : CjParsingTestCase(
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
    fun testMatchConditionWithExpressionExposesItsNodeType() {
        val file = createPsiFile("matchConditionExpression", SOURCE) as CjFile

        val condition = PsiTreeUtil.findChildOfType(file, CjMatchConditionWithExpression::class.java)
            ?: error("CjMatchConditionWithExpression not found in parsed file")

        assertEquals(CjNodeTypes.MATCH_CONDITION_EXPRESSION, condition.iElementType)
    }

    private companion object {
        private val SOURCE = """
            func f(x: Int64): Int64 {
                match {
                    case x > 0 => 1
                    case _ => 0
                }
            }
        """.trimIndent()
    }
}
