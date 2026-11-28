package org.cangnova.cangjie.cfir.builder.macro

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** 宏 token 缓存需要保留新标识符边界以及宿主 UTF-16 偏移。 */
class MacroPayloadUnicodeTest {
    @Test
    fun `Unicode 15 identifiers remain single tokens at nonzero source offsets`() {
        val source = "cafe\u0301 \uD807\uDF02value"
        val tokens = MacroPayloadTokenizer.tokenize(source, baseOffset = 37)
        assertEquals(listOf("cafe\u0301", " ", "\uD807\uDF02value"), tokens.map { it.text })
        assertEquals(listOf(37 to 42, 42 to 43, 43 to 50), tokens.map { it.startOffset to it.endOffset })
    }
}
