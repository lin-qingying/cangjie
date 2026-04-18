package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaExpressionTypeProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjExpression

/**
 * 对齐 Kotlin `KaFirExpressionTypeProvider` 的组件落位。
 *
 * 这里只负责把 CFIR 已经求出的表达式/声明返回类型投影到公开 `CaType`，
 * 不混入类型关系、类型构造等其他职责。
 */
internal class CaCfirExpressionTypeProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaExpressionTypeProvider, CaCfirSessionComponent {
    override val CjExpression.expressionType: CaType?
        get() = withValidityAssertion {
            analysisSession.typeQueries.queryExpressionType(this@expressionType)?.asPublicType()
        }

    override val CjCallableDeclaration.returnType: CaType?
        get() = withValidityAssertion {
            analysisSession.typeQueries.queryDeclarationReturnType(this@returnType)?.asPublicType()
        }
}
