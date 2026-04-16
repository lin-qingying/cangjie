package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.types.asCaType
import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowInfo
import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowStability
import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjParenthesizedExpression
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjVariableDeclaration

/**
 * CFIR 数据流快照实现。
 *
 * 该对象承载 Analysis API 对外暴露的数据流最小稳定视图：
 * 表达式类型、编译期值、是否为纯引用，以及基于源码形状和语义符号推断出的稳定性。
 */
internal class CaCfirDataFlowInfoImpl(
    override val expressionType: CaType?,
    override val compileTimeValue: CaCompileTimeValue?,
    override val isPureReference: Boolean,
    override val stability: CaDataFlowStability,
    override val token: CaLifetimeToken,
) : CaDataFlowInfo

/**
 * 计算表达式在当前 use-site session 下的数据流快照。
 */
internal fun CaCfirSession.getDataFlowInfo(expression: CjExpression): CaDataFlowInfo {
    return getOrCreateDataFlowInfo(expression) {
        val compileTimeValue = evaluateCompileTimeValue(expression)
        val resolvedSymbol = resolveStableReferenceTarget(expression)
        val isPureReference = expression.isPureReferenceExpression()
        CaCfirDataFlowInfoImpl(
            expressionType = queryExpressionType(expression)?.asCaType(this),
            compileTimeValue = compileTimeValue,
            isPureReference = isPureReference,
            stability = computeDataFlowStability(
                expression = expression,
                compileTimeValue = compileTimeValue,
                resolvedSymbol = resolvedSymbol,
                isPureReference = isPureReference,
            ),
            token = token,
        )
    }
}

private fun CaCfirSession.computeDataFlowStability(
    expression: CjExpression,
    compileTimeValue: CaCompileTimeValue?,
    resolvedSymbol: CaSymbol?,
    isPureReference: Boolean,
): CaDataFlowStability {
    if (compileTimeValue != null) return CaDataFlowStability.STABLE_VALUE
    if (!isPureReference) {
        return when (expression) {
            is CjCallExpression,
            is CjDotQualifiedExpression,
            -> CaDataFlowStability.COMPUTED_VALUE

            else -> CaDataFlowStability.UNKNOWN
        }
    }

    val psi = (resolvedSymbol as? org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol)?.psi
    return when (psi) {
        is CjVariableDeclaration -> if (psi.isVar) CaDataFlowStability.MUTABLE_VALUE else CaDataFlowStability.STABLE_VALUE
        is CjParameter -> if (psi.isMutable) CaDataFlowStability.MUTABLE_VALUE else CaDataFlowStability.STABLE_VALUE
        is CjNamedFunction -> if (psi.isConst) CaDataFlowStability.STABLE_VALUE else CaDataFlowStability.COMPUTED_VALUE
        is CjTypeStatement -> CaDataFlowStability.STABLE_VALUE
        else -> when (resolvedSymbol) {
            is CaPackageSymbol,
            is CaClassLikeSymbol,
            -> CaDataFlowStability.STABLE_VALUE

            is CaCallableSymbol -> CaDataFlowStability.COMPUTED_VALUE
            else -> CaDataFlowStability.UNKNOWN
        }
    }
}

private fun CaCfirSession.resolveStableReferenceTarget(expression: CjExpression): CaSymbol? {
    return when (expression) {
        is CjReferenceExpression -> with(this) { expression.resolveToSymbol() }
        is CjParenthesizedExpression -> expression.expression?.let(::resolveStableReferenceTarget)
        is CjDotQualifiedExpression -> expression.selectorExpression?.let(::resolveStableReferenceTarget)
        else -> null
    }
}

private fun CjExpression.isPureReferenceExpression(): Boolean = when (this) {
    is CjReferenceExpression -> true
    is CjParenthesizedExpression -> expression?.isPureReferenceExpression() == true
    is CjDotQualifiedExpression -> receiverExpression.isPureReferenceExpression() &&
        selectorExpression?.isPureReferenceExpression() == true

    else -> false
}
