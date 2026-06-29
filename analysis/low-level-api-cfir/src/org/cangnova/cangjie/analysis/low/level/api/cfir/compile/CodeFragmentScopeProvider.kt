
package org.cangnova.cangjie.analysis.low.level.api.cfir.compile

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.psi.CjCodeFragment

@LLCfirInternals
/**
 * 从 CFIR session 取得 code fragment 额外作用域 provider。
 */
val CfirSession.codeFragmentScopeProvider: CodeFragmentScopeProvider by CfirSession.sessionComponentAccessor()


/**
 * 标记属性 symbol 是否表示外部环境值；仓颉当前没有 foreign value 语义。
 */
val CfirPropertySymbol.isForeignValue: Boolean
    get() = false


/**
 * 为 code fragment 提供额外 local scope 的 session component。
 */
class CodeFragmentScopeProvider(
    /**
     * 该 provider 所属的 CFIR session。
     */
    private val session: CfirSession,
) : CfirSessionComponent {
    /**
     * 仓颉不在 low-level API 中承担 Java/JVM 代码片段类型桥接语义。
     */
    fun getExtraScopes(codeFragment: CjCodeFragment): List<CfirLocalScope> = emptyList()
}
