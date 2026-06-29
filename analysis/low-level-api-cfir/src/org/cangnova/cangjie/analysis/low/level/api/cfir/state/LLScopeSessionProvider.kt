

package org.cangnova.cangjie.analysis.low.level.api.cfir.state

import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.cfir.ScopeSession

/**
 * 为指定 low-level session 提供 [ScopeSession] 的抽象。
 */
interface LLScopeSessionProvider {
    /**
     * 返回 [session] 使用的 scope session。
     */
    fun getScopeSession(session: LLCfirSession): ScopeSession
}

/**
 * 默认 scope session provider，直接委托给 session 自身。
 */
internal object LLDefaultScopeSessionProvider : LLScopeSessionProvider {
    /**
     * 返回 [session] 自身提供的 scope session。
     */
    override fun getScopeSession(session: LLCfirSession): ScopeSession {
        return session.getScopeSession()
    }
}
