package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjExpression

interface CaEvaluator : CaLifetimeOwner {
    fun CjExpression.evaluate(): CaCompileTimeValue?
}
