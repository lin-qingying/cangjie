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
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaExpressionInformationProvider {
    override val CjExpression.isStatementLike: Boolean
        get() = withValidityAssertion {
            this@isStatementLike.isStatementLikeExpression()
        }

    override val CjExpression.isCompileTimeConstant: Boolean
        get() = withValidityAssertion {
            analysisSession.evaluateCompileTimeValue(this@isCompileTimeConstant) != null
        }
}
