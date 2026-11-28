package org.cangnova.cangjie.cfir.resolve.source

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperationKind
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperator
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.types.ConePrimitiveType

/** 固定 v113 IsExpr/AsExpr：选择器一次求值，TypePattern/Some 参数共享真实新 binding。 */
internal class CfirSourceTypeOperationCanonicalizer(private val context: CfirSourceGraphProducer.Context) {
    fun produce(operation: CfirTypeOperator, owner: CfirDeclaration): CfirSourceExpressionPayload.Match {
        val selector = context.expression(operation.argument, owner)
        val selectorType = context.typeOf(operation.argument)
        val targetType = context.typeOf(operation.typeRef)
        val isTest = operation.operation == CfirTypeOperationKind.IS
        val resultType = context.typeOf(operation)
        val inner: CfirSourcePattern
        val success: CfirSourceExpressionIdentity
        val failure: CfirSourceExpressionIdentity
        if (isTest) {
            check(resultType == ConePrimitiveType.BOOLEAN)
            inner = context.wildcardPattern(targetType)
            success = context.boolean(owner, true)
            failure = context.boolean(owner, false)
        } else {
            val variable = context.freshVariable("v-compiler", targetType, false, owner, operation)
            inner = context.variablePattern(variable, targetType)
            success = context.enumCall(resultType, "Some", listOf(context.reference(variable, owner)), owner)
            failure = context.enumReference(resultType, "None", owner)
        }
        val cases = listOf(
            context.typePattern(selectorType, targetType, inner, owner) to success,
            context.wildcardPattern(selectorType) to failure,
        ).map { (pattern, value) -> context.generated(owner, resultType,
            CfirSourceExpressionPayload.MatchCase(listOf(pattern), null, context.expressionBlock(owner, value))) }
        return CfirSourceExpressionPayload.Match(selector, cases)
    }
}
