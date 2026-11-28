package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.macro.MacroTokenKind
import org.cangnova.cangjie.macro.TokenInfo
import org.cangnova.cangjie.macro.protocol.MacroMsgCodec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * 官方 Lexer.cpp/TokenSerialization.cpp 的 wire value 与源码界定符分别保存。
 * Keep 返回同一 wire token 时只恢复一次界定符，不能把原源码再次作为 payload 包起来。
 */
class MacroTokenLiteralWireTest {
    @Test
    fun ordinaryStringPreservesPayloadAndProtocolDelimiter() {
        checkLiteral("STRING_LITERAL", "\"integer\"", "integer")
    }

    @Test
    fun escapedStringPreservesEscapes() {
        checkLiteral("STRING_LITERAL", "\"a\\\"b\\n\"", "a\\\"b\\n")
    }

    @Test
    fun interpolationPreservesInnerQuotes() {
        val value = "\${\"value\"}"
        checkLiteral("STRING_LITERAL", "\"$value\"", value)
    }

    @Test
    fun jStringPreservesPayload() {
        checkLiteral("JSTRING_LITERAL", "J\"value\"", "value")
    }

    @Test
    fun runePreservesUnicodeEscape() {
        checkLiteral("RUNE_LITERAL", "r'\\u{4f60}'", "\\u{4f60}")
    }

    @Test
    fun multilineStringDoesNotDuplicateInitialLineTerminator() {
        checkLiteral("MULTILINE_STRING", "\"\"\"\nfirst\nsecond\n\"\"\"", "first\nsecond\n")
    }

    @Test
    fun rawStringPreservesDelimiterCountAndInnerQuotes() {
        checkLiteral("MULTILINE_RAW_STRING", "###\"a\"#b\"###", "a\"#b", 3)
    }

    private fun checkLiteral(kind: String, source: String, value: String, delimiters: Int = 1) {
        val encoded = MacroTokenKind.fromLexerToken(kind, source)
        assertNotNull(encoded)
        val token = kotlin.checkNotNull(encoded)
        assertEquals(MacroTokenKind.kindOf(kind), token.kind)
        assertEquals(value, token.value)
        assertEquals(delimiters, token.delimiterNum)
        val rendered = MacroMsgCodec.rebuildExpandedText(listOf(TokenInfo(
            kind = token.kind, value = token.value, delimiterNum = token.delimiterNum,
        )))
        assertEquals(source.replace("\r\n", "\n"), rendered.replace("\r\n", "\n"))
    }
}
