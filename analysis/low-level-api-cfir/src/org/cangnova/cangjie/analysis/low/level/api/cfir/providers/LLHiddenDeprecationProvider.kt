

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.declarations.CfirHiddenDeprecationProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

/**
 * 仓颉 low-level API 保持统一的隐藏弃用判定，不再为 Java 互操作保留特殊分支。
 */
class LLHiddenDeprecationProvider(session: CfirSession) : CfirHiddenDeprecationProvider(session) {
    /**
     * 判断 [symbol] 的弃用等级是否为隐藏。
     *
     * low-level API 直接复用主干 CFIR 判定，不增加 IDE 特殊分支。
     */
    override fun isDeprecationLevelHidden(symbol: CfirBasedSymbol<*>): Boolean = super.isDeprecationLevelHidden(symbol)
}
