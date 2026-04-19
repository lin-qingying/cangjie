
package org.cangnova.cangjie.analysis.low.level.api.cfir.compile

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.scopes.CfirLocalScope
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.psi.CjCodeFragment

@LLCfirInternals
val CfirSession.codeFragmentScopeProvider: CodeFragmentScopeProvider by CfirSession.sessionComponentAccessor()


val CfirPropertySymbol.isForeignValue: Boolean
    get() = false


class CodeFragmentScopeProvider(private val session: CfirSession) : CfirSessionComponent {
    /**
     * 仓颉不在 low-level API 中承担 Java/JVM 代码片段类型桥接语义。
     */
    fun getExtraScopes(codeFragment: CjCodeFragment): List<CfirLocalScope> = emptyList()
}
