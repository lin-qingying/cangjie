package org.cangnova.cangjie.name

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** 与 cjc 1.1.3 对 Unicode 15 组合字符的名称比较结果对照。 */
class CangJieUnicodeTest {
    @Test
    fun `Unicode 15 combining class reorders before composition`() {
        val nagMundariMark = "\uD839\uDCEC" // U+1E4EC
        assertEquals("à$nagMundariMark", CangJieUnicode.normalizeIdentifier("a$nagMundariMark\u0300"))
        assertEquals("à$nagMundariMark", CangJieUnicode.normalizeIdentifier("a\u0300$nagMundariMark"))
    }
}
