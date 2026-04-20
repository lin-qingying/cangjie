package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjExpression

interface CaExpressionTypeProvider : CaLifetimeOwner {
    val CjExpression.expressionType: CaType?

    val CjCallableDeclaration.returnType: CaType?
}
