package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.cfir.CaCfirLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.CaInvalidLifetimeOwnerAccessException
import org.cangnova.cangjie.analysis.api.lifetime.assertIsValidAndAccessible
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Analysis API CFIR 层的基础可用性与接口可达性测试。
 */
class AnalysisApiSurfaceTest {
    @Test
    fun `cfir lifetime token starts valid and accessible`() {
        val token = CaCfirLifetimeToken()

        assertTrue(token.isValid())
        assertTrue(token.isAccessible())
        assertDoesNotThrow { token.assertIsValidAndAccessible() }
    }

    @Test
    fun `cfir lifetime token invalidation is observable through lifetime checks`() {
        val token = CaCfirLifetimeToken()

        token.invalidate("test invalidation")

        assertFalse(token.isValid())
        assertEquals("test invalidation", token.getInvalidationReason())

        val exception = assertThrows(CaInvalidLifetimeOwnerAccessException::class.java) {
            token.assertIsValidAndAccessible()
        }
        assertEquals("test invalidation", exception.message)
    }
}
