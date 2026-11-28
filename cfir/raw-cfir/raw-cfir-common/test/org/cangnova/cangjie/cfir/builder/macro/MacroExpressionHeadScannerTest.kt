package org.cangnova.cangjie.cfir.builder.macro

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/** 宏名称段和属性区间始终对应原始 UTF-16 源码。 */
class MacroExpressionHeadScannerTest {
    @Test
    fun `unicode and quoted names retain exact token boundaries`() {
        val text = "prefix @!pkg.`e\u0301.x`.\uD807\uDF02Name[attr: [1], text: \"a]b\", /* ] */ n: 2] class Item {}"
        val head = requireNotNull(MacroExpressionHeadScanner.scan(text, text.indexOf('@')))
        fun slice(range: MacroExpressionTextRange): String = text.substring(range.startOffset, range.endOffset)
        assertEquals(listOf("pkg", "`e\u0301.x`", "\uD807\uDF02Name"), head.segmentRanges.map(::slice))
        assertEquals("pkg.`e\u0301.x`.\uD807\uDF02Name", slice(head.nameRange))
        assertEquals("[attr: [1], text: \"a]b\", /* ] */ n: 2]", slice(requireNotNull(head.attrRange)))
        assertEquals(text.indexOf('@'), head.annotationStartOffset)
    }

    @Test
    fun `only the direct prefix is accepted and incomplete names are rejected`() {
        assertNull(MacroExpressionHeadScanner.scan("prefix @Foo", 0))
        assertNull(MacroExpressionHeadScanner.scan("@foo. class Item {}", 0))
        val head = requireNotNull(MacroExpressionHeadScanner.scan("@Foo[unfinished", 0))
        assertNull(head.attrRange)
    }

    @Test
    fun `escaped brackets in attributes do not change nesting`() {
        for (attribute in listOf("[\\] x]", "[\\[]", "[x: [\\] y]]")) {
            val source = "@Foo$attribute class Item {}"
            val range = requireNotNull(requireNotNull(MacroExpressionHeadScanner.scan(source, 0)).attrRange)
            assertEquals(attribute, source.substring(range.startOffset, range.endOffset))
        }
    }

    @Test
    fun `parenthesized input is determined by the next token after trivia`() {
        assertTrue(requireNotNull(MacroExpressionHeadScanner.scan("@Foo /* gap */ (input)", 0)).hasParenthesizedInput)
        assertTrue(requireNotNull(MacroExpressionHeadScanner.scan("@Foo[x] /* gap */ (input)", 0)).hasParenthesizedInput)
        assertFalse(requireNotNull(MacroExpressionHeadScanner.scan("@Foo[text: \"(x)\"] class Item {}", 0)).hasParenthesizedInput)
        assertFalse(requireNotNull(MacroExpressionHeadScanner.scan("@Foo[unfinished (input)", 0)).hasParenthesizedInput)
    }
}
