

package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.session.CfirSession


interface SessionHolder {
    val session: CfirSession
}

interface ScopeSessionHolder {
    val scopeSession: ScopeSession
}

interface SessionAndScopeSessionHolder : SessionHolder, ScopeSessionHolder

inline fun <R> withSession(session: CfirSession, block: context(SessionHolder) () -> R): R {
    val holder = object : SessionHolder {
        override val session: CfirSession
            get() = session
    }
    return block(holder)
}

inline fun <R> withSession(session: CfirSession, scopeSession: ScopeSession, block: context(SessionHolder) () -> R): R {
    val holder = object : SessionAndScopeSessionHolder {
        override val session: CfirSession
            get() = session

        override val scopeSession: ScopeSession
            get() = scopeSession
    }
    return block(holder)
}
