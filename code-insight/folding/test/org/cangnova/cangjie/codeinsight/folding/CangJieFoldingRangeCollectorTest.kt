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

/**
 * 验证共享折叠区域收集器的语法覆盖和占位文本。
 */
class CangJieFoldingRangeCollectorTest : CjParsingTestCase(
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
     * 多个 import 应合并为一个 imports 折叠区域。
     */
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

    /**
     * 块、类体、函数 literal 和 match 表达式应产生普通折叠区域。
     */
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

    /**
     * 单行块和单行调用不应产生折叠区域。
     */
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

    /**
     * 多行调用应折叠参数括号范围并使用 `(...)` 占位符。
     */
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

    /**
     * 块注释与 CDoc 应产生 comment 类折叠区域。
     */
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

    /**
     * 解析源码并收集折叠区域。
     */
    private fun collect(text: String): List<CangJieFoldingRegion> {
        val file = parse(text)
        return CangJieFoldingRangeCollector.collect(file, DocumentImpl(file.text))
    }

    /**
     * 将内联仓颉源码解析为 PSI 文件。
     */
    private fun parse(text: String): CjFile {
        return createPsiFile("folding", text) as CjFile
    }
}
