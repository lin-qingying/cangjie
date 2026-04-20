package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaDataFlowProvider
import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowInfo
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.psi.CjExpression

/**
 * 数据流快照入口。
 */
internal class CaCfirDataFlowProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaDataFlowProvider {
    override fun CjExpression.getDataFlowInfo(): CaDataFlowInfo = withValidityAssertion {
        analysisSession.getDataFlowInfo(this@getDataFlowInfo)
    }
}
