package org.cangnova.cangjie.analysis.api.impl.base.lifetime

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.platform.lifetime.CaLifetimeTracker

/**
 * 基础生命周期追踪器（对齐 Kotlin 的 KaBaseLifetimeTracker）。
 *
 * 使用 ThreadLocal 维护每线程的令牌栈，追踪嵌套的 analyze() 调用。
 */
internal class CaBaseLifetimeTracker : CaLifetimeTracker {
    /**
     * 当前线程进入分析上下文时压入的 lifetime token 栈。
     */
    private val lifetimeOwnersStack = ThreadLocal.withInitial<ArrayDeque<CaLifetimeToken>> { ArrayDeque() }

    /**
     * 当前线程最内层 analysis context 的 token。
     */
    override val currentToken: CaLifetimeToken? get() = lifetimeOwnersStack.get().lastOrNull()

    /**
     * 进入分析上下文前压入当前 session token。
     */
    fun beforeEnteringAnalysis(session: CaSession) {
        lifetimeOwnersStack.get().addLast(session.token)
    }

    /**
     * 离开分析上下文后弹出当前 session token。
     */
    fun afterLeavingAnalysis(session: CaSession) {
        val stack = lifetimeOwnersStack.get()
        val last = stack.lastOrNull()
        check(last == session.token) {
            "The last token on the stack should be the same as the one from the outgoing session."
        }
        stack.removeLast()
    }

    companion object {
        /**
         * 从 project 服务容器取得基础 lifetime tracker。
         */
        fun getInstance(project: Project): CaBaseLifetimeTracker =
            CaLifetimeTracker.getInstance(project) as? CaBaseLifetimeTracker
                ?: error("Expected ${CaBaseLifetimeTracker::class.simpleName} to be registered for ${CaLifetimeTracker::class.simpleName}.")
    }
}
