package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaDataFlowProvider
import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowInfo
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.psi.CjExpression

/**
 * 数据流快照入口。
 */
internal class CaCfirDataFlowProvider(
    /**
     * 延迟取得当前 CFIR Analysis session，数据流快照由 session 级实现计算。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaDataFlowProvider {
    /**
     * 返回表达式在当前 session 下的数据流信息快照。
     */
    override fun CjExpression.getDataFlowInfo(): CaDataFlowInfo = withValidityAssertion {
        analysisSession.getDataFlowInfo(this@getDataFlowInfo)
    }
}
