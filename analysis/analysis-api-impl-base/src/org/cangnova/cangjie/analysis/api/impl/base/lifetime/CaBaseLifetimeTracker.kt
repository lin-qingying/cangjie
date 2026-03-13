package org.cangjie.analysis.api.impl.base.lifetime

import com.intellij.openapi.project.Project
import org.cangjie.analysis.api.CaSession
import org.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangjie.analysis.api.platform.lifetime.CaLifetimeTracker

/**
 * 基础生命周期追踪器（对齐 Kotlin 的 KaBaseLifetimeTracker）。
 *
 * 使用 ThreadLocal 维护每线程的令牌栈，追踪嵌套的 analyze() 调用。
 */
internal class CaBaseLifetimeTracker : CaLifetimeTracker {
    private val lifetimeOwnersStack = ThreadLocal.withInitial<ArrayDeque<CaLifetimeToken>> { ArrayDeque() }

    override val currentToken: CaLifetimeToken? get() = lifetimeOwnersStack.get().lastOrNull()

    fun beforeEnteringAnalysis(session: CaSession) {
        lifetimeOwnersStack.get().addLast(session.token)
    }

    fun afterLeavingAnalysis(session: CaSession) {
        val stack = lifetimeOwnersStack.get()
        val last = stack.lastOrNull()
        check(last == session.token) {
            "The last token on the stack should be the same as the one from the outgoing session."
        }
        stack.removeLast()
    }

    companion object {
        fun getInstance(project: Project): CaBaseLifetimeTracker =
            CaLifetimeTracker.getInstance(project) as? CaBaseLifetimeTracker
                ?: error("Expected ${CaBaseLifetimeTracker::class.simpleName} to be registered for ${CaLifetimeTracker::class.simpleName}.")
    }
}
