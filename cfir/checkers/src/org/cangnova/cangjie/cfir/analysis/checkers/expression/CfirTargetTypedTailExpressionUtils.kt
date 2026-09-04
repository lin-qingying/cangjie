package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.checkTypeMismatch
import org.cangnova.cangjie.cfir.analysis.checkers.functionTypeForLambdaShape
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * target-typed 复合表达式检查结果。
 *
 * [NotHandled] 表示当前表达式根不是需要下钻的复合表达式，调用方应继续保留
 * 自己原有的根级诊断分类；[Handled] 表示 target type 已经由真实结果位置消费，
 * 调用方不得再对组合表达式根追加 mismatch。
 */
internal sealed class TargetTypedCheckOutcome {
    data object NotHandled : TargetTypedCheckOutcome()
    data class Handled(val reported: Boolean) : TargetTypedCheckOutcome()
}

/** 判断 target-typed helper 是否已经接管了当前表达式。 */
internal val TargetTypedCheckOutcome.isHandled: Boolean
    get() = this is TargetTypedCheckOutcome.Handled

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
        val declaredReturnType = (containingFunction.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return null
        // 对齐官方 CheckFuncBody：显式 Unit 返回类型只综合分析函数体并插入 return ()，
        // 不把 body 尾表达式强制当作 Unit 返回值，与 CfirFunctionBodyTypeMismatchChecker 一致。
        if (declaredReturnType.isUnit) return null
        return declaredReturnType
    }

    val containingValueParameter = context.findClosestDeclaration<CfirValueParameter> { it.defaultValue === this }
    if (containingValueParameter != null) {
        return (containingValueParameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType
    }

    val containingVariable = context.findClosestDeclaration<CfirVariable> { it.initializer === this } ?: return null
    return containingVariable.explicitDeclaredTypeOrNull()
}

/**
 * 返回变量声明中真实写出的目标类型。
 *
 * 无显式类型的变量在 body resolve 结束时会把 `CfirImplicitTypeRef` 替换为一个
 * `source == null` 的 resolved type ref。这个 resolved 节点只记录 initializer 的推断结果，
 * 不能重新作为 initializer 的 target type；否则 `let value = match { ... }` 会把推断出的
 * match 类型反向施加到每个分支。源码类型引用则保留非空 source，只有它才是声明级 target。
 */
internal fun CfirVariable.explicitDeclaredTypeOrNull(): ConeCangJieType? {
    val typeRef = returnTypeRef as? CfirResolvedTypeRef ?: return null
    return typeRef.takeIf { it.source != null }?.coneType
}

/**
 * target-typed 检查的外部入口。
 *
 * 该入口只接管 block/双分支 if/match/普通 try 这类会把结果藏在子结构里的复合表达式。
 * 普通叶子表达式返回 [TargetTypedCheckOutcome.NotHandled]，让 return、assignment、
 * initializer 等调用方继续使用各自的根级诊断类型。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkTargetTypedExpression(
    expression: CfirExpression,
    expectedType: ConeCangJieType,
): TargetTypedCheckOutcome {
    val unwrapped = expression.unwrapForTargetTyping()
    if (expectedType is ConeErrorType || unwrapped.coneTypeOrNull is ConeErrorType) {
        return TargetTypedCheckOutcome.Handled(reported = false)
    }

    return when (unwrapped) {
        is CfirAnonymousFunctionExpression -> {
            // 官方 ChkLamExpr 在函数类型目标下直接把目标参数/返回类型下推到 lambda
            // 子树；lambda 本身不是一个已经定型的普通函数值，不能在声明初始化器层再做
            // 一次 `(P) -> Unit <: (P) -> Any` 的整体比较。参数与 body 的诊断分别由
            // lambda declaration/body checkers 负责。只有目标不是函数类型时，才让调用方
            // 按普通表达式报告 lambda 与目标类型不匹配。
            if (expectedType.functionTypeForLambdaShape(context) != null) {
                TargetTypedCheckOutcome.Handled(reported = false)
            } else {
                TargetTypedCheckOutcome.NotHandled
            }
        }
        is CfirBlock -> checkTargetTypedBlockTail(unwrapped, expectedType)
        is CfirIfExpression -> {
            if (unwrapped.isIfExpressionWithoutEndingElse()) {
                TargetTypedCheckOutcome.NotHandled
            } else {
                checkTargetTypedIfExpression(unwrapped, expectedType)
            }
        }
        is CfirMatchExpression -> checkTargetTypedMatchExpression(unwrapped, expectedType)
        is CfirTryExpression -> {
            if (unwrapped.resources.isNotEmpty()) {
                TargetTypedCheckOutcome.NotHandled
            } else {
                checkTargetTypedTryExpression(unwrapped, expectedType)
            }
        }
        else -> TargetTypedCheckOutcome.NotHandled
    }
}

/**
 * 检查 block 的尾结果是否满足 target-typed 语境给出的期望类型。
 *
 * 非尾语句不继承外层 expected type；尾部声明或空 block 以 `Unit` 结果参与检查。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkTargetTypedBlockTail(
    block: CfirBlock,
    expectedType: ConeCangJieType,
): TargetTypedCheckOutcome {
    val tailStatement = block.statements.lastOrNull()
    val tailExpression = tailStatement as? CfirExpression
    if (tailExpression != null) {
        return checkTargetTypedTailExpression(tailExpression, expectedType)
    }

    return checkTargetTypedUnitResult(tailStatement?.source ?: block.source, expectedType)
}

/**
 * 递归下钻 block/if/match/try 的尾位置，并对真实结果表达式执行类型匹配检查。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkTargetTypedTailExpression(
    expression: CfirExpression,
    expectedType: ConeCangJieType,
): TargetTypedCheckOutcome {
    val unwrapped = expression.unwrapForTargetTyping()
    if (expectedType is ConeErrorType || unwrapped.coneTypeOrNull is ConeErrorType) {
        return TargetTypedCheckOutcome.Handled(reported = false)
    }

    return when (unwrapped) {
        is CfirBlock -> checkTargetTypedBlockTail(unwrapped, expectedType)
        is CfirIfExpression -> {
            if (unwrapped.isIfExpressionWithoutEndingElse()) {
                checkTargetTypedLeaf(unwrapped, expectedType)
            } else {
                checkTargetTypedIfExpression(unwrapped, expectedType)
            }
        }
        is CfirMatchExpression -> checkTargetTypedMatchExpression(unwrapped, expectedType)
        is CfirTryExpression -> {
            if (unwrapped.resources.isNotEmpty()) {
                checkTargetTypedLeaf(unwrapped, expectedType)
            } else {
                checkTargetTypedTryExpression(unwrapped, expectedType)
            }
        }
        else -> checkTargetTypedLeaf(unwrapped, expectedType)
    }
}

/** 检查双分支 `if` 的 then/else 真实结果表达式。 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkTargetTypedIfExpression(
    expression: CfirIfExpression,
    expectedType: ConeCangJieType,
): TargetTypedCheckOutcome {
    val elseBranch = expression.elseBranch ?: return TargetTypedCheckOutcome.NotHandled
    return listOf(
        checkTargetTypedBlockTail(expression.thenBranch, expectedType),
        checkTargetTypedTailExpression(elseBranch, expectedType),
    ).combineTargetTypedOutcomes()
}

/** 检查 `match` 每个分支 body 的尾结果表达式。 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkTargetTypedMatchExpression(
    expression: CfirMatchExpression,
    expectedType: ConeCangJieType,
): TargetTypedCheckOutcome {
    return expression.branches
        .map { branch -> checkTargetTypedBlockTail(branch.body, expectedType) }
        .combineTargetTypedOutcomes()
}

/** 检查普通 `try` 的 try/catch/handle 结果，resources 形式不在这里下推 target type。 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkTargetTypedTryExpression(
    expression: CfirTryExpression,
    expectedType: ConeCangJieType,
): TargetTypedCheckOutcome {
    return buildList {
        add(checkTargetTypedBlockTail(expression.tryBlock, expectedType))
        expression.catches.mapTo(this) { catchClause ->
            checkTargetTypedBlockTail(catchClause.body, expectedType)
        }
        expression.handlers.mapTo(this) { handleClause ->
            checkTargetTypedBlockTail(handleClause.body, expectedType)
        }
    }.combineTargetTypedOutcomes()
}

/** 对已经定位到的普通结果表达式执行 TYPE_MISMATCH 检查。 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkTargetTypedLeaf(
    expression: CfirExpression,
    expectedType: ConeCangJieType,
): TargetTypedCheckOutcome.Handled {
    val source = expression.source as? AbstractCjSourceElement
        ?: return TargetTypedCheckOutcome.Handled(reported = false)
    val actualType = expression.coneTypeOrNull ?: return TargetTypedCheckOutcome.Handled(reported = false)
    if (actualType is ConeErrorType || expectedType is ConeErrorType) {
        return TargetTypedCheckOutcome.Handled(reported = false)
    }

    val reported = checkTypeMismatch(
        expectedType = expectedType,
        actualType = actualType,
        expression = expression,
        source = source,
        preferredSpecializedSource = source,
        diagnosticFactory = CfirErrors.TYPE_MISMATCH,
    )
    return TargetTypedCheckOutcome.Handled(reported)
}

/** 对尾部声明或空 block 的 `Unit` 结果执行 target type 检查。 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkTargetTypedUnitResult(
    source: CjSourceElement?,
    expectedType: ConeCangJieType,
): TargetTypedCheckOutcome.Handled {
    if (expectedType is ConeErrorType) return TargetTypedCheckOutcome.Handled(reported = false)
    val diagnosticSource = source as? AbstractCjSourceElement
        ?: return TargetTypedCheckOutcome.Handled(reported = false)
    val unitType = ConePrimitiveType.UNIT
    if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, unitType, expectedType) == true) {
        return TargetTypedCheckOutcome.Handled(reported = false)
    }
    reporter.reportOn(
        diagnosticSource,
        CfirErrors.TYPE_MISMATCH,
        expectedType,
        unitType,
        false,
    )
    return TargetTypedCheckOutcome.Handled(reported = true)
}

/** 合并同一个复合表达式下多个尾结果检查的状态。 */
private fun List<TargetTypedCheckOutcome>.combineTargetTypedOutcomes(): TargetTypedCheckOutcome {
    var handled = false
    var reported = false
    for (outcome in this) {
        when (outcome) {
            is TargetTypedCheckOutcome.Handled -> {
                handled = true
                reported = reported || outcome.reported
            }
            TargetTypedCheckOutcome.NotHandled -> Unit
        }
    }
    return if (handled) {
        TargetTypedCheckOutcome.Handled(reported)
    } else {
        TargetTypedCheckOutcome.NotHandled
    }
}

/** 去掉 named/optional 等包装节点，使 target type 能落到真实结果表达式。 */
private fun CfirExpression.unwrapForTargetTyping(): CfirExpression {
    var current = this
    while (current is CfirWrappedExpression) {
        current = current.expression
    }
    return current
}

/** 判断 `if/else if` 链是否缺少最终 `else`。 */
private fun CfirIfExpression.isIfExpressionWithoutEndingElse(): Boolean {
    val elseBranch = elseBranch?.unwrapForTargetTyping() ?: return true
    return elseBranch is CfirIfExpression && elseBranch.isIfExpressionWithoutEndingElse()
}
