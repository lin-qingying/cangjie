package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowInfo
import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowStability
import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithCandidates
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.types.resolvedType
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjParenthesizedExpression
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjVariableDeclaration
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate

/**
 * CFIR 数据流快照实现。
 *
 * 该对象承载 Analysis API 对外暴露的数据流最小稳定视图：
 * 表达式类型、编译期值、是否为纯引用，以及基于源码形状和语义符号推断出的稳定性。
 */
internal class CaCfirDataFlowInfoImpl(
    /**
     * 表达式当前解析出的公开类型。
     */
    override val expressionType: CaType?,
    /**
     * 表达式可求出的编译期值。
     */
    override val compileTimeValue: CaCompileTimeValue?,
    /**
     * 表达式是否只由引用、括号和限定访问组成。
     */
    override val isPureReference: Boolean,
    /**
     * 表达式结果在数据流分析中的稳定性分类。
     */
    override val stability: CaDataFlowStability,
    /**
     * 约束数据流快照生命周期的会话 token。
     */
    override val token: CaLifetimeToken,
) : CaDataFlowInfo

/**
 * 计算表达式在当前 use-site session 下的数据流快照。
 */
internal fun CaCfirSession.getDataFlowInfo(expression: CjExpression): CaDataFlowInfo {
    val compileTimeValue = evaluateCompileTimeValue(expression)
    val resolvedSymbol = resolveStableReferenceTarget(expression)
    val isPureReference = expression.isPureReferenceExpression()
    return CaCfirDataFlowInfoImpl(
        expressionType = (expression.getOrBuildCfir(resolutionFacade) as? CfirExpression)?.resolvedType?.asCaType(this),
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

/**
 * 根据编译期值、引用目标和 PSI 形态推导公开数据流稳定性。
 */
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

    val psi = (resolvedSymbol as? CaDeclarationSymbol)?.psi
    return when (psi) {
        is CjVariableDeclaration -> if (psi.isVar) CaDataFlowStability.MUTABLE_VALUE else CaDataFlowStability.STABLE_VALUE
        is CjParameter -> if (psi.isMutable) CaDataFlowStability.MUTABLE_VALUE else CaDataFlowStability.STABLE_VALUE
        is CjProperty -> if (psi.isVar) CaDataFlowStability.MUTABLE_VALUE else CaDataFlowStability.STABLE_VALUE
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

/**
 * 解析表达式最终绑定的稳定引用目标。
 */
private fun CaCfirSession.resolveStableReferenceTarget(expression: CjExpression): CaSymbol? {
    return when (expression) {
        is CjParenthesizedExpression -> expression.expression?.let(::resolveStableReferenceTarget)
        is CjDotQualifiedExpression -> expression.selectorExpression?.let(::resolveStableReferenceTarget)
        is CjReferenceExpression -> resolveStableReferenceTargetByCfir(expression)
        else -> null
    }
}

/**
 * data-flow 只需要“当前引用最终绑定到哪个稳定声明”。
 *
 * 这里直接复用 CFIR 已完成的引用信息，而不是再绕一圈 public resolver。
 * 这样可以和 qualified/property access 的 CFIR 语义保持一致。
 */
private fun CaCfirSession.resolveStableReferenceTargetByCfir(expression: CjExpression): CaSymbol? {
    val cfir = expression.getOrBuildCfir(resolutionFacade)
    return when (cfir) {
        is CfirResolvable -> cfir.calleeReference.toStableTargetSymbol(this)
        is CfirResolvedNamedReference -> cfir.toStableTargetSymbol(this)
        else -> null
    }
}

/**
 * 将 CFIR 引用转换为数据流稳定性判定使用的公开目标符号。
 */
private fun CfirReference.toStableTargetSymbol(session: CaCfirSession): CaSymbol? {
    val symbol = when (this) {
        is CfirResolvedNamedReference -> resolvedSymbol
        is CfirNamedReferenceWithCandidate -> candidateSymbol
        is CfirThisReference -> boundSymbol
        is CfirErrorNamedReference -> (diagnostic as? ConeDiagnosticWithCandidates)?.candidateSymbols?.firstOrNull()
        is CfirSuperReference -> null
        else -> null
    } ?: return null

    return session.cfirSymbolBuilder.buildSymbol(symbol)
}

/**
 * 判断表达式是否只表达引用链而不引入调用、字面量或计算。
 */
private fun CjExpression.isPureReferenceExpression(): Boolean = when (this) {
    is CjReferenceExpression -> true
    is CjParenthesizedExpression -> expression?.isPureReferenceExpression() == true
    is CjDotQualifiedExpression -> receiverExpression.isPureReferenceExpression() &&
        selectorExpression?.isPureReferenceExpression() == true

    else -> false
}
