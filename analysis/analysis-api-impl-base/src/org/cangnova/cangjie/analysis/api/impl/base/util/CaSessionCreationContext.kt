package org.cangnova.cangjie.analysis.api.impl.base.util

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession

@CaImplementationDetail
fun <T : CaSession> createSession(block: CaSessionCreationContext<T>.() -> T): T {
    val box = CaSessionCreationContextImpl<T>()
    val session = block(box)
    box.initialize(session)
    return session
}

@OptIn(CaImplementationDetail::class)
private class CaSessionCreationContextImpl<T : CaSession> : () -> T, CaSessionCreationContext<T> {
    private var cachedSession: T? = null

    fun initialize(session: T) {
        require(cachedSession == null) { "The session is already initialized" }
        cachedSession = session
    }

    override val analysisSessionProvider: () -> T
        get() = this

    override fun invoke(): T {
        return cachedSession
            ?: error(
                "Session is not yet initialized. " +
                        "If you are inside a session component, perhaps you will need to wrap your computation in 'lazy {}'"
            )
    }
}

@CaImplementationDetail
interface CaSessionCreationContext<T : CaSession> {
    val analysisSessionProvider: () -> T
}
