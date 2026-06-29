package org.cangnova.cangjie.formatter

import kotlin.test.Test
import kotlin.test.assertEquals
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * 验证仓颉 formatter 的基础格式化输出。
 */
class CangJieFormatterTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    /**
     * 初始化 PSI 解析测试环境。
     */
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    /**
     * 释放 PSI 解析测试环境。
     */
    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /**
     * 函数体花括号和二元表达式空格应被规范化。
     */
    @Test
    fun testFormatsFunctionBodyAndBinarySpacing() {
        val file = createPsiFile(
            "formatting",
            """
            func main(){
            let value=1+2
            }
            """.trimIndent(),
        ) as CjFile

        val formatted = CangJieFormatter.format(file).trimEnd()
        assertEquals(
            """
            func main() {
                let value = 1 + 2
            }
            """.trimIndent(),
            formatted,
        )
    }
}
