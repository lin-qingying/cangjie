package org.cangnova.cangjie.psi.stubs.impl

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.cangnova.cangjie.lexer.CjTokens

class ModifierMaskUtilsTest {
    @Test
    fun `platform modifier survives stub mask encoding`() {
        val mask = ModifierMaskUtils.computeMask(
            hasModifier = { false },
            hasAdditionalModifier = { it == CjTokens.PLATFORM_KEYWORD },
        )

        assertTrue(ModifierMaskUtils.maskHasModifier(mask, CjTokens.PLATFORM_KEYWORD))
        assertFalse(ModifierMaskUtils.maskHasModifier(mask, CjTokens.SPECIFIC_KEYWORD))
        assertTrue(ModifierMaskUtils.maskToString(mask).contains("platform"))
    }
}
