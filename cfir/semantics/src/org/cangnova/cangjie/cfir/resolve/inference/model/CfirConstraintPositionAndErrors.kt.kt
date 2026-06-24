package org.cangnova.cangjie.cfir.resolve.inference.model

import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.resolve.calls.inference.model.LambdaArgumentConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.OnlyInputTypeConstraintPosition

/**
 * 普通 lambda 实参产生的约束位置。
 *
 * @property anonymousFunctionReturnExpression lambda 返回表达式，用于把返回类型约束定位回具体表达式。
 */
class ConeRegularLambdaArgumentConstraintPosition(
    anonymousFunction: CfirAnonymousFunction,
    override val anonymousFunctionReturnExpression: CfirExpression,
) : ConeLambdaArgumentConstraintPosition(anonymousFunction), OnlyInputTypeConstraintPosition

/**
 * lambda 实参约束位置的 CFIR 基类。
 *
 * @param anonymousFunction 参与调用解析的匿名函数。
 */
sealed class ConeLambdaArgumentConstraintPosition(anonymousFunction: CfirAnonymousFunction) :
    LambdaArgumentConstraintPosition<CfirAnonymousFunction>(anonymousFunction) {
    /**
     * lambda 返回表达式；为空表示约束不对应某个具体返回表达式。
     */
    abstract val anonymousFunctionReturnExpression: CfirExpression?

    /**
     * 约束位置的调试文本。
     */
    override fun toString(): String = "LambdaArgument"
}
