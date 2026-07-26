package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.checkTypeMismatch
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 从 return、函数体尾位置或变量 initializer 上下文中推导 target-typed 表达式的目标类型。
 */
internal fun CfirExpression.expectedTypeFromTargetContext(context: CheckerContext): ConeCangJieType? {
    val parentStatement = context.containingStatements.asReversed().drop(1).firstOrNull()

    val returnExpression = parentStatement as? CfirReturnExpression
    if (returnExpression?.result === this) {
        val containingFunction = returnExpression.target.labeledElement as? CfirFunction ?: return null
        return (containingFunction.returnTypeRef as? CfirResolvedTypeRef)?.coneType
    }

    val parentBlock = parentStatement as? CfirBlock
    if (parentBlock != null && parentBlock.statements.lastOrNull() === this) {
        val containingFunction = context.findClosestDeclaration<CfirFunction> { it.body === parentBlock } ?: return null
        // 构造器体没有尾表达式返回语义，不能用构造器 returnTypeRef 反向约束 body 尾表达式。
        if (containingFunction is CfirConstructor) return null
        return (containingFunction.returnTypeRef as? CfirResolvedTypeRef)?.coneType
    }

    val containingVariable = context.findClosestDeclaration<CfirVariable> { it.initializer === this } ?: return null
    return (containingVariable.returnTypeRef as? CfirResolvedTypeRef)?.coneType
}

/**
 * 检查 block 的尾表达式是否满足 target-typed 语境给出的期望类型。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkTargetTypedBlockTail(block: CfirBlock, expectedType: ConeCangJieType) {
    val tailExpression = block.statements.lastOrNull() as? CfirExpression ?: return
    checkTargetTypedTailExpression(tailExpression, expectedType)
}

/**
 * 递归下钻 block/if/match 的尾位置，并对真实结果表达式执行类型匹配检查。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkTargetTypedTailExpression(expression: CfirExpression, expectedType: ConeCangJieType) {
    when (expression) {
        is CfirBlock -> {
            checkTargetTypedBlockTail(expression, expectedType)
            return
        }

        is CfirIfExpression -> {
            checkTargetTypedBlockTail(expression.thenBranch, expectedType)
            expression.elseBranch?.let { checkTargetTypedTailExpression(it, expectedType) }
            return
        }

        is CfirMatchExpression -> {
            expression.branches.forEach { branch ->
                checkTargetTypedBlockTail(branch.body, expectedType)
            }
            return
        }
    }

    val source = expression.source as? AbstractCjSourceElement ?: return
    val actualType = expression.coneTypeOrNull?.normalizeForTargetTyping(expectedType) ?: return
    if (actualType is ConeErrorType || expectedType is ConeErrorType) return

    checkTypeMismatch(
        expectedType = expectedType,
        actualType = actualType,
        expression = expression,
        source = source,
        preferredSpecializedSource = source,
        diagnosticFactory = CfirErrors.TYPE_MISMATCH,
    )
}

/**
 * target typing 下的 ideal literal 使用期望类型参与比较，避免把整数/浮点字面量误判为不匹配。
 */
private fun ConeCangJieType.normalizeForTargetTyping(expectedType: ConeCangJieType): ConeCangJieType {
    return when (this) {
        is ConeIdealLiteralType -> expectedType
        else -> this
    }
}
