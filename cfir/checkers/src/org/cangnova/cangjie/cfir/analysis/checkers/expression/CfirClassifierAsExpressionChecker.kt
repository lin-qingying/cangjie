package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.hasInvalidGenericTypeArgument
import org.cangnova.cangjie.cfir.analysis.diagnostics.renderInvalidBinaryOperatorType
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.name.OperatorNameConventions

/**
 * 表达式位置不能把类型参数或 class / struct / enum 等类型名当作值使用。
 *
 * Kotlin FIR 在 `FirStandaloneQualifierChecker` 中检查独立 qualifier；
 * 本地 CFIR 暂无独立 qualifier 节点，类型名会以 `CfirResolvedNamedReference`
 * 暂存，因此在 qualified-access checker 层对齐官方仓颉 `sema_ref_not_be_type`。
 */
object CfirClassifierAsExpressionChecker : CfirQualifiedAccessChecker() {
    /**
     * 检查 qualified access 是否把类型名当作表达式值使用。
     *
     * 函数调用和作为外层接收者的 qualifier 不在这里报告。类型参数使用完整引用范围；
     * class-like symbol 保持现有首字符范围行为，由其独立诊断范围簇继续处理。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression is CfirFunctionCall) {
            expression.reportInvalidBinaryOperatorForClassifierOperands()
            return
        }
        if (expression.hasInvalidGenericTypeArgument()) return
        val resolvedReference = expression.calleeReference as? CfirResolvedNamedReference ?: return
        val source = resolvedReference.source ?: expression.source ?: return
        if (expression.isUsedAsOuterReceiver()) return
        if (expression.isUsedAsSubscriptReceiver()) return
        if (resolvedReference.resolvedSymbol is CfirClassLikeSymbol<*> && expression.isAssignmentExpectedTypeOperand()) return

        when (resolvedReference.resolvedSymbol) {
            is CfirTypeParameterSymbol -> reporter.reportOn(
                source = source,
                factory = CfirErrors.REF_NOT_BE_TYPE,
            )

            is CfirClassLikeSymbol<*> -> reporter.reportOn(
                source = CjOffsetsOnlySourceElement(source.startOffset, source.startOffset + 1),
                factory = CfirErrors.REF_NOT_BE_TYPE,
            )

            else -> Unit
        }
    }

    /** 赋值右值已有明确目标类型时，类型不匹配由 assignment checker 统一拥有。 */
    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.isAssignmentExpectedTypeOperand(): Boolean =
        context.callsOrAssignments.asReversed()
            .filterIsInstance<org.cangnova.cangjie.cfir.expressions.CfirAssignment>()
            .any { assignment -> assignment.rValue === this }

    /**
     * 官方在 `DiagnoseForBinaryExpr` 中会同时保留操作数的类型名表达式错误和外层二元表达式错误。
     *
     * CFIR 的可重载二元表达式会先降成 operator call；当该调用本身解析成功时，需要在
     * qualified-access checker 层补上这一语法级二元诊断，避免把 `A + A` 误当成合法 operator 调用。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirFunctionCall.reportInvalidBinaryOperatorForClassifierOperands() {
        if (origin != CfirFunctionCallOrigin.Operator) return
        if (calleeReference is CfirDiagnosticHolder) return
        val callee = calleeReference as? CfirNamedReference ?: return
        val operatorToken = OperatorNameConventions.TOKENS_BY_OPERATOR_NAME[callee.name] ?: return
        val left = explicitReceiver ?: return
        val right = argumentList.arguments.singleOrNull() ?: return
        val leftInvalid = left.hasInvalidGenericTypeArgument()
        val rightInvalid = right.hasInvalidGenericTypeArgument()
        if (leftInvalid || rightInvalid) return
        if (!left.isClassifierAsExpressionOperand() && !right.isClassifierAsExpressionOperand()) return

        val source = source ?: return
        val leftType = left.coneTypeOrNull ?: return
        val rightType = right.coneTypeOrNull ?: return
        reporter.reportOn(
            source = source,
            factory = CfirErrors.INVALID_BINARY_OPERATOR,
            a = operatorToken,
            b = leftType.renderInvalidBinaryOperatorType(context.session),
            c = rightType.renderInvalidBinaryOperatorType(context.session),
        )
    }

    /** 判断表达式是否为解析到 class / struct / enum 等分类器的裸类型名操作数。 */
    private fun org.cangnova.cangjie.cfir.expressions.CfirExpression.isClassifierAsExpressionOperand(): Boolean {
        return isResolvedClassLikeValueReference()
    }

    /**
     * 判断当前访问是否只是外层 qualified access 的显式接收者。
     *
     * 这种形态承担限定名解析结构，不代表最终把类型名作为运行时值读取。
     */
    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.isUsedAsOuterReceiver(): Boolean {
        return context.callsOrAssignments.asReversed().drop(1).any { call ->
            call is CfirQualifiedAccessExpression &&
                    call.explicitReceiver === this &&
                    !call.isPlainOperatorCall()
        }
    }

    /** 普通二元/一元 operator 的 receiver 仍是运行时表达式使用，不属于限定名结构 receiver。 */
    private fun CfirQualifiedAccessExpression.isPlainOperatorCall(): Boolean {
        val call = this as? CfirFunctionCall ?: return false
        if (call.origin != CfirFunctionCallOrigin.Operator) return false
        val name = (call.calleeReference as? CfirNamedReference)?.name ?: return false
        return name != OperatorNameConventions.GET && name != OperatorNameConventions.SET
    }

    /**
     * 类型名作为下标 receiver 时由调用解析报告 `ILLEGAL_ACCESS_NON_STATIC_MEMBER`。
     */
    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.isUsedAsSubscriptReceiver(): Boolean {
        return context.containingStatements.asReversed().any { statement ->
            statement is CfirSubscriptExpression && statement.receiver === this
        }
    }
}

/** 判断表达式是否为解析到 class / struct / enum 等声明的裸类型名值引用。 */
internal fun CfirExpression.isResolvedClassLikeValueReference(): Boolean {
    val access = this as? CfirQualifiedAccessExpression ?: return false
    if (access is CfirFunctionCall) return false
    val resolvedReference = access.calleeReference as? CfirResolvedNamedReference ?: return false
    return resolvedReference.resolvedSymbol is CfirClassLikeSymbol<*>
}
