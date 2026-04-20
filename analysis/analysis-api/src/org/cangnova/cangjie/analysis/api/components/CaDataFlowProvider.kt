package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowInfo
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjExpression

interface CaDataFlowProvider : CaLifetimeOwner {
    fun CjExpression.getDataFlowInfo(): CaDataFlowInfo
}
