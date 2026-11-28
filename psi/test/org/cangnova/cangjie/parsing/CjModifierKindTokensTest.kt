package org.cangnova.cangjie.parsing

import org.cangnova.cangjie.lexer.CjTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class CjModifierKindTokensTest {
    @Test
    fun `raw modifier identities preserve historical spelling and token kind`() {
        val kinds = listOf(CjModifierKind.COMMON, CjModifierKind.SPECIFIC, CjModifierKind.PLATFORM,
            CjModifierKind.INTERNAL, CjModifierKind.OVERRIDE, CjModifierKind.INOUT)
        for (kind in kinds) assertEquals(kind, kind.token().cjModifierKindOrNull())
        assertNotEquals(CjModifierKind.SPECIFIC.token(), CjModifierKind.PLATFORM.token())
        assertNull(CjTokens.FUNC_KEYWORD.cjModifierKindOrNull())
    }
}
