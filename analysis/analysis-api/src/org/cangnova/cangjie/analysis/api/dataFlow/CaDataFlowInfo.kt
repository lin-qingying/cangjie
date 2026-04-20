package org.cangnova.cangjie.analysis.api.dataFlow

import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.types.CaType

interface CaDataFlowInfo : CaLifetimeOwner {
    val expressionType: CaType?

    val compileTimeValue: CaCompileTimeValue?

    val isPureReference: Boolean

    val stability: CaDataFlowStability
}
