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
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 对齐官方仓颉 `ChkTryExpr/ChkTryExprCatchesAndHandles + ChkBlock`：
 * target-typed `try` 会把外层目标类型分别下推到 try/catch block 的尾表达式，
 * 类型不匹配时主诊断落在具体尾表达式上，而不是落回整个 `return try`。
 */
object CfirTryTargetTypeMismatchChecker : CfirTryExpressionChecker() {
    /** 检查 target-typed try/catch block 尾表达式是否符合外层期望类型。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirTryExpression) {
        if (expression.resources.isNotEmpty()) return
        if (expression.handlers.isNotEmpty()) return

        val expectedType = expression.expectedTypeFromContext(context) ?: return
        checkBlockTail(expression.tryBlock, expectedType)
        expression.catches.forEach { catchClause ->
            checkBlockTail(catchClause.body, expectedType)
        }
    }

    /** 检查 block 的尾表达式是否满足指定期望类型。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkBlockTail(block: CfirBlock, expectedType: ConeCangJieType) {
        val tailExpression = block.statements.lastOrNull() as? CfirExpression ?: return
        checkTailExpression(tailExpression, expectedType)
    }

    /** 递归下钻 block/if/match 的尾位置，并对实际表达式执行类型匹配检查。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkTailExpression(expression: CfirExpression, expectedType: ConeCangJieType) {
        when (expression) {
            is CfirBlock -> {
                checkBlockTail(expression, expectedType)
                return
            }

            is CfirIfExpression -> {
                checkBlockTail(expression.thenBranch, expectedType)
                expression.elseBranch?.let { checkTailExpression(it, expectedType) }
                return
            }

            is CfirMatchExpression -> {
                expression.branches.forEach { branch ->
                    checkBlockTail(branch.body, expectedType)
                }
                return
            }
        }

        val source = expression.source as? AbstractCjSourceElement ?: return
        val actualType = expression.coneTypeOrNull?.normalizeForTryTarget(expectedType) ?: return
        if (actualType is ConeErrorType || expectedType is ConeErrorType) return

        checkTypeMismatch(
            expectedType = expectedType,
            actualType = actualType,
            source = source,
            preferredSpecializedSource = source,
            diagnosticFactory = CfirErrors.TYPE_MISMATCH,
        )
    }
}

/** 从 return、函数体尾位置或变量 initializer 上下文中推导 try 表达式的目标类型。 */
private fun CfirTryExpression.expectedTypeFromContext(context: CheckerContext): ConeCangJieType? {
    val parentStatement = context.containingStatements.asReversed().drop(1).firstOrNull()

    val returnExpression = parentStatement as? CfirReturnExpression
    if (returnExpression?.result === this) {
        val containingFunction = returnExpression.target.labeledElement as? CfirFunction ?: return null
        return (containingFunction.returnTypeRef as? CfirResolvedTypeRef)?.coneType
    }

    val parentBlock = parentStatement as? CfirBlock
    if (parentBlock != null && parentBlock.statements.lastOrNull() === this) {
        val containingFunction = context.findClosestDeclaration<CfirFunction> { it.body === parentBlock } ?: return null
        // 构造器体没有尾表达式返回语义，不能用构造器 returnTypeRef 反向约束 body 尾部 try。
        if (containingFunction is CfirConstructor) return null
        return (containingFunction.returnTypeRef as? CfirResolvedTypeRef)?.coneType
    }

    val containingVariable = context.findClosestDeclaration<CfirVariable> { it.initializer === this } ?: return null
    return (containingVariable.returnTypeRef as? CfirResolvedTypeRef)?.coneType
}

/** 对 try target typing 下的 ideal literal 类型做期望类型归一化。 */
private fun ConeCangJieType.normalizeForTryTarget(expectedType: ConeCangJieType): ConeCangJieType {
    return when (this) {
        is ConeIdealLiteralType -> expectedType
        else -> this
    }
}
