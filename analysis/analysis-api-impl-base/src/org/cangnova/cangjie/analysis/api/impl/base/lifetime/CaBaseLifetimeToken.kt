package org.cangnova.cangjie.analysis.api.impl.base.lifetime

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.platform.lifetime.CaLifetimeTracker

/**
 * Analysis API 基础生命周期令牌。
 *
 * 该实现承担两类职责：
 * 1. 跟踪对象是否已失效
 * 2. 校验当前线程是否处于该 token 对应的分析上下文中
 *
 * 这与 Kotlin Analysis API 中 token 既管理 validity，
 * 又管理 analysis context accessibility 的职责保持一致。
 */
internal class CaBaseLifetimeToken(
    private val project: Project,
) : CaLifetimeToken() {
    @Volatile
    private var valid: Boolean = true

    @Volatile
    private var invalidationReason: String? = null

    override fun isValid(): Boolean = valid

    override fun getInvalidationReason(): String {
        return invalidationReason ?: error("Token is still valid")
    }

    override fun isAccessible(): Boolean {
        val currentToken = CaLifetimeTracker.getInstance(project).currentToken
        return currentToken === this
    }

    override fun getInaccessibilityReason(): String {
        return "The lifetime owner is accessed outside of its valid analysis context."
    }

    fun invalidate(reason: String) {
        invalidationReason = reason
        valid = false
    }
}
