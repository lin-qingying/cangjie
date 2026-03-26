package org.cangnova.cangjie.cfir.resolve.inference.model

import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.resolve.calls.inference.model.LambdaArgumentConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.OnlyInputTypeConstraintPosition

class ConeRegularLambdaArgumentConstraintPosition(
    anonymousFunction: CfirAnonymousFunction,
    override val anonymousFunctionReturnExpression: CfirExpression,
) : ConeLambdaArgumentConstraintPosition(anonymousFunction), OnlyInputTypeConstraintPosition
sealed class ConeLambdaArgumentConstraintPosition(anonymousFunction: CfirAnonymousFunction) :
    LambdaArgumentConstraintPosition<CfirAnonymousFunction>(anonymousFunction) {
    abstract val anonymousFunctionReturnExpression: CfirExpression?

    override fun toString(): String = "LambdaArgument"
}
