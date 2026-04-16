package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope

/**
 * class-like use-site member / type scope 的公开作用域视图。
 */
internal class CaCfirMemberScope(
    memberScope: CfirTypeScope,
    analysisSession: CaCfirSession,
    token: CaLifetimeToken,
) : CaCfirBasedScope<CfirTypeScope>(memberScope, analysisSession, token)
