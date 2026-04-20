package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope

/**
 * class-like 自身 declared-member 的公开作用域视图。
 */
internal class CaCfirDeclaredMemberScope(
    declaredMemberScope: CfirContainingNamesAwareScope,
    analysisSession: CaCfirSession,
    token: CaLifetimeToken,
) : CaCfirBasedScope<CfirContainingNamesAwareScope>(declaredMemberScope, analysisSession, token)
