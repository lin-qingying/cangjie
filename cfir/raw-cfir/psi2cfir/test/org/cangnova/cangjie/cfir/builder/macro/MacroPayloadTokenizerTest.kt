package org.cangnova.cangjie.cfir.builder.macro

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 验证 macro payload 拆词与 CJC token kind 的对齐。
 *
 * CangJie lexer 把字符串拆成 `OPEN_QUOTE` / 内容 / `CLOSING_QUOTE`，而 CJC 与宏协议
 * 只认整段字符串 token；这里锁定合并行为，避免 `OPEN_QUOTE` 缺少 kind 映射再次中断分析。
 */
class MacroPayloadTokenizerTest {

    @Test
    fun `single line string is merged into one string literal token`() {
        val tokens = MacroPayloadTokenizer.tokenize("\"hello\"")

        assertEquals(1, tokens.size)
        assertEquals("STRING_LITERAL", tokens.single().kindName)
        assertEquals("\"hello\"", tokens.single().text)
        assertEquals(0, tokens.single().startOffset)
        assertEquals(7, tokens.single().endOffset)
    }

    @Test
    fun `multiline string keeps raw text and uses multiline kind`() {
        val tokens = MacroPayloadTokenizer.tokenize("\"\"\"hello\nworld\"\"\"")

        assertEquals(1, tokens.size)
        assertEquals("MULTILINE_STRING", tokens.single().kindName)
        assertEquals("\"\"\"hello\nworld\"\"\"", tokens.single().text)
    }

    @Test
    fun `hash string is encoded as multiline raw string`() {
        val tokens = MacroPayloadTokenizer.tokenize("#\"hello\"#")

        assertEquals(1, tokens.size)
        assertEquals("MULTILINE_RAW_STRING", tokens.single().kindName)
        assertEquals("#\"hello\"#", tokens.single().text)
    }

    @Test
    fun `string interpolation is merged with surrounding parts`() {
        val tokens = MacroPayloadTokenizer.tokenize("\"a" + "\${x}" + "b\"")

        assertEquals(1, tokens.size)
        assertEquals("STRING_LITERAL", tokens.single().kindName)
        assertEquals("\"a" + "\${x}" + "b\"", tokens.single().text)
    }

    @Test
    fun `unterminated string collapses into a single illegal token`() {
        val tokens = MacroPayloadTokenizer.tokenize("\"abc")

        assertEquals(1, tokens.size)
        assertEquals("ILLEGAL", tokens.single().kindName)
        assertEquals("\"abc", tokens.single().text)
    }

    @Test
    fun `string token keeps host offsets and neighbor tokens`() {
        val tokens = MacroPayloadTokenizer.tokenize("x \"y\"", baseOffset = 10)

        assertEquals(3, tokens.size)
        assertEquals("IDENTIFIER", tokens[0].kindName)
        assertEquals(10, tokens[0].startOffset)
        assertEquals("STRING_LITERAL", tokens[2].kindName)
        assertEquals("\"y\"", tokens[2].text)
        assertEquals(12, tokens[2].startOffset)
        assertEquals(15, tokens[2].endOffset)
    }
}
