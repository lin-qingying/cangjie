package org.cangnova.cangjie.analysis.api.impl.base.util

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession

/**
 * 创建需要在构造期间互相引用 session component 的 Analysis API session。
 */
@CaImplementationDetail
fun <T : CaSession> createSession(block: CaSessionCreationContext<T>.() -> T): T {
    val box = CaSessionCreationContextImpl<T>()
    val session = block(box)
    box.initialize(session)
    return session
}

/**
 * 延迟提供 session 实例的创建上下文实现。
 */
@OptIn(CaImplementationDetail::class)
private class CaSessionCreationContextImpl<T : CaSession> : () -> T, CaSessionCreationContext<T> {
    /**
     * 创建完成后缓存的 session。
     */
    private var cachedSession: T? = null

    /**
     * 在 session 构造完成后初始化上下文。
     */
    fun initialize(session: T) {
        require(cachedSession == null) { "The session is already initialized" }
        cachedSession = session
    }

    /**
     * 提供给 session component 的 session provider。
     */
    override val analysisSessionProvider: () -> T
        get() = this

    /**
     * 返回已经初始化的 session。
     */
    override fun invoke(): T {
        return cachedSession
            ?: error(
                "Session is not yet initialized. " +
                        "If you are inside a session component, perhaps you will need to wrap your computation in 'lazy {}'"
            )
    }
}

/**
 * session 构造期间向组件暴露 session provider 的上下文。
 */
@CaImplementationDetail
interface CaSessionCreationContext<T : CaSession> {
    /**
     * 延迟返回当前正在创建的 session。
     */
    val analysisSessionProvider: () -> T
}
