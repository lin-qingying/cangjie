package org.cangnova.cangjie.analysis.api.cfir

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken

/**
 * CFIR 实现的生命周期令牌（对齐 Kotlin 的 KaFirLifetimeToken）。
 *
 * 追踪底层 CfirSession 的有效性。
 * 当源码或项目结构发生变化时，令牌失效。
 */
class CaCfirLifetimeToken : CaLifetimeToken() {

    /**
     * 当前令牌是否仍然允许访问 Analysis API 对象。
     */
    @Volatile
    private var valid = true

    /**
     * 令牌失效时记录的原因；令牌仍有效时保持为 null。
     */
    @Volatile
    private var invalidationReason: String? = null

    /**
     * 返回当前令牌是否有效。
     */
    override fun isValid(): Boolean = valid

    /**
     * 返回令牌失效原因。
     *
     * 只有令牌已经失效时才能调用；有效令牌调用会报告编程错误。
     */
    override fun getInvalidationReason(): String {
        return invalidationReason ?: error("Token is still valid")
    }

    /**
     * CFIR session token 当前不做线程/读写动作访问限制。
     */
    override fun isAccessible(): Boolean = true

    /**
     * 返回不可访问原因。
     *
     * 该实现始终可访问，因此调用该方法表示上层状态判断错误。
     */
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
