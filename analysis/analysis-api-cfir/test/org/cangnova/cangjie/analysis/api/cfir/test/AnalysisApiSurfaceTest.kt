package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.lifetime.CaInvalidLifetimeOwnerAccessException
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.assertIsValidAndAccessible
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Analysis API 生命周期契约的表面可用性测试。
 *
 * 这里刻意只依赖公开的 `CaLifetimeToken` 抽象，而不绑定到具体的 CFIR token 实现，
 * 以避免实现模块测试再次把实现细节当成对外稳定 API。
 */
class AnalysisApiSurfaceTest {
    @Test
    fun `lifetime token starts valid and accessible`() {
        val token = TestLifetimeToken()

        assertTrue(token.isValid())
        assertTrue(token.isAccessible())
        assertDoesNotThrow { token.assertIsValidAndAccessible() }
    }

    @Test
    fun `lifetime token invalidation is observable through lifetime checks`() {
        val token = TestLifetimeToken()

        token.invalidate("test invalidation")

        assertFalse(token.isValid())
        assertEquals("test invalidation", token.getInvalidationReason())

        val exception = assertThrows(CaInvalidLifetimeOwnerAccessException::class.java) {
            token.assertIsValidAndAccessible()
        }
        assertEquals("test invalidation", exception.message)
    }
}

/**
 * 测试专用 token，实现公开生命周期契约中的最小必要语义：
 * 1. 初始有效且可访问；
 * 2. 失效后能通过统一异常链被观察到。
 */
private class TestLifetimeToken : CaLifetimeToken() {
    private var valid: Boolean = true
    private var invalidationReason: String? = null

    override fun isValid(): Boolean = valid

    override fun getInvalidationReason(): String {
        return invalidationReason ?: error("Token is still valid")
    }

    override fun isAccessible(): Boolean = true

    override fun getInaccessibilityReason(): String {
        error("Token is accessible")
    }

    fun invalidate(reason: String) {
        invalidationReason = reason
        valid = false
    }
}
