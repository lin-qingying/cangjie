

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.declarations.CfirHiddenDeprecationProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

/**
 * 仓颉 low-level API 保持统一的隐藏弃用判定，不再为 Java 互操作保留特殊分支。
 */
class LLHiddenDeprecationProvider(session: CfirSession) : CfirHiddenDeprecationProvider(session) {
    override fun isDeprecationLevelHidden(symbol: CfirBasedSymbol<*>): Boolean = super.isDeprecationLevelHidden(symbol)
}
