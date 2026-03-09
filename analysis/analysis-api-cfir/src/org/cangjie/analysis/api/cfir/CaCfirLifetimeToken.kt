package org.cangjie.analysis.api.cfir

import org.cangjie.analysis.api.lifetime.CaLifetimeToken

/**
 * CFIR 实现的生命周期令牌（对齐 Kotlin 的 KaFirLifetimeToken）。
 *
 * 追踪底层 CfirSession 的有效性。
 * 当源码或项目结构发生变化时，令牌失效。
 */
class CaCfirLifetimeToken : CaLifetimeToken() {

    @Volatile
    private var valid = true

    @Volatile
    private var invalidationReason: String? = null

    override fun isValid(): Boolean = valid

    override fun getInvalidationReason(): String {
        return invalidationReason ?: error("Token is still valid")
    }

    override fun isAccessible(): Boolean = true

    override fun getInaccessibilityReason(): String {
        error("Token is accessible")
    }

    /**
     * 使令牌失效。
     */
    fun invalidate(reason: String) {
        invalidationReason = reason
        valid = false
    }
}
