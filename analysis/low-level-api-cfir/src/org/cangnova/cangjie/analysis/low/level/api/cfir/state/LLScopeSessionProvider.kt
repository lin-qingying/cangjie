

package org.cangnova.cangjie.analysis.low.level.api.cfir.state

import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.cfir.ScopeSession

interface LLScopeSessionProvider {
    fun getScopeSession(session: LLCfirSession): ScopeSession
}

internal object LLDefaultScopeSessionProvider : LLScopeSessionProvider {
    override fun getScopeSession(session: LLCfirSession): ScopeSession {
        return session.getScopeSession()
    }
}
