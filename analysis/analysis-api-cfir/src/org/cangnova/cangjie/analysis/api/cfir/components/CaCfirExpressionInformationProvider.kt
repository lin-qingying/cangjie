package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaExpressionInformationProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.psi.CjExpression

/**
 * 表达式结构与编译期常量信息入口。
 */
@OptIn(CaImplementationDetail::class)
internal class CaCfirExpressionInformationProvider(
    /**
     * 延迟取得当前 CFIR Analysis session。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaExpressionInformationProvider {
    /**
     * 判断表达式在当前位置是否以语句形态参与语义。
     */
    override val CjExpression.isStatementLike: Boolean
        get() = withValidityAssertion {
            this@isStatementLike.isStatementLikeExpression()
        }

    /**
     * 判断表达式是否可以稳定求得编译期常量值。
     */
    override val CjExpression.isCompileTimeConstant: Boolean
        get() = withValidityAssertion {
            analysisSession.evaluateCompileTimeValue(this@isCompileTimeConstant) != null
        }
}
