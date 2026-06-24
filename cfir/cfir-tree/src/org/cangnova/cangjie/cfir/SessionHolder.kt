

package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.session.CfirSession


/**
 * context receiver 中提供 [CfirSession] 的持有者。
 */
interface SessionHolder {
    /**
     * 当前 CFIR session。
     */
    val session: CfirSession
}

/**
 * context receiver 中提供 [ScopeSession] 的持有者。
 */
interface ScopeSessionHolder {
    /**
     * 当前 scope session。
     */
    val scopeSession: ScopeSession
}

/**
 * 同时提供 [CfirSession] 与 [ScopeSession] 的 context receiver 持有者。
 */
interface SessionAndScopeSessionHolder : SessionHolder, ScopeSessionHolder

/**
 * 在 [block] 中提供 [session] context。
 */
inline fun <R> withSession(session: CfirSession, block: context(SessionHolder) () -> R): R {
    val holder = object : SessionHolder {
        override val session: CfirSession
            get() = session
    }
    return block(holder)
}

/**
 * 在 [block] 中提供 [session] 与 [scopeSession] context。
 */
inline fun <R> withSession(session: CfirSession, scopeSession: ScopeSession, block: context(SessionHolder) () -> R): R {
    val holder = object : SessionAndScopeSessionHolder {
        override val session: CfirSession
            get() = session

        override val scopeSession: ScopeSession
            get() = scopeSession
    }
    return block(holder)
}
