package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.analysis.diagnostics.literalConversionDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.patterns.CfirExpressionPattern
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * match 的 guard 与无 selector 条件都必须产生 Bool。
 *
 * 对应官方 ChkMatchCasePatGuard / SynMatchCaseNoSelector，诊断独立于分支结果类型；
 * 字面量转换失败复用共享诊断分类，已有错误表达式不追加条件类型错误。
 */
object CfirMatchConditionTypeChecker : CfirMatchExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        for (branch in expression.branches) {
            if (expression.subject == null) {
                val condition = (branch.pattern as? CfirExpressionPattern)?.expression
                if (condition != null) checkCondition(condition)
            }
            val guard = branch.guard
            if (guard != null) checkCondition(guard)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCondition(condition: CfirExpression) {
        val actualType = condition.coneTypeOrNull ?: return
        if (actualType.containsErrorType()) return
        val expectedType = context.session.builtinTypes.boolType
        if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, actualType, expectedType) == true) return
        literalConversionDiagnostic(condition.source, expectedType, condition, context.session)?.let {
            reporter.report(it, context)
            return
        }
        reporter.reportOn(condition.source, CfirErrors.TYPE_MISMATCH, expectedType, actualType, false)
    }
}
