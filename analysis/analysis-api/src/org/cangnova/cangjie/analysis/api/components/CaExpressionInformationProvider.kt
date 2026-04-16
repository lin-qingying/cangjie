package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjExpression

interface CaExpressionInformationProvider : CaLifetimeOwner {
    val CjExpression.isStatementLike: Boolean

    val CjExpression.isCompileTimeConstant: Boolean
}
