package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.coneTypeOrNull

/**
 * 浮点字面量范围检查器
 *
 * 对齐 C++ TypeCheckExpr/LitConstExpr.cpp:
 * - EXCEED_FLOAT_LITERAL_RANGE: NaN/Infinity
 * - FLOAT_LITERAL_TOO_LARGE: 超出目标类型最大值（警告）
 * - FLOAT_LITERAL_TOO_SMALL: 小于目标类型最小正值（警告）
 */
object CfirFloatLiteralRangeChecker : CfirLiteralExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirLiteralExpression) {
        val value = expression.value
        if (value !is Double && value !is Float) return

        val doubleValue = (value as Number).toDouble()
        val resolvedType = expression.coneTypeOrNull

        if (doubleValue.isNaN() || doubleValue.isInfinite()) {
            reporter.reportOn(
                source = expression.source,
                factory = CfirErrors.EXCEED_FLOAT_LITERAL_RANGE,
                a = value.toString(),
            )
            return
        }

        if (resolvedType == null || resolvedType is ConeErrorType) return

        // Float32 范围检查
        if (resolvedType is ConePrimitiveType && resolvedType.kind == PrimitiveTypeKind.FLOAT32) {
            val absValue = kotlin.math.abs(doubleValue)
            if (absValue > Float.MAX_VALUE.toDouble() && absValue != 0.0) {
                reporter.reportOn(
                    source = expression.source,
                    factory = CfirErrors.FLOAT_LITERAL_TOO_LARGE,
                    a = resolvedType,
                    b = value.toString(),
                )
            } else if (absValue != 0.0 && absValue < Float.MIN_VALUE.toDouble()) {
                reporter.reportOn(
                    source = expression.source,
                    factory = CfirErrors.FLOAT_LITERAL_TOO_SMALL,
                    a = resolvedType,
                    b = value.toString(),
                )
            }
        }
    }
}

/**
 * 表达式类型推断失败检查器
 *
 * 通过 BasicExpressionChecker 分发，检查所有表达式的推断类型是否为 ConeErrorType。
 * 对齐 C++ DiagKind::sema_unable_to_infer_expr
 */
object CfirExpressionTypeInferenceChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        // 只检查有 source 的真实表达式
        val source = expression.source ?: return
        if (source.kind !is org.cangnova.cangjie.source.CjRealSourceElementKind) return

        val exprType = (expression as? org.cangnova.cangjie.cfir.expressions.CfirExpression)?.coneTypeOrNull ?: return
        if (exprType !is ConeErrorType) return

        // 避免对函数调用等已有更具体诊断的节点重复报告
        if (expression is CfirFunctionCall) return
        if (expression is CfirQualifiedAccessExpression) return

        reporter.reportOn(
            source = source,
            factory = CfirErrors.UNABLE_TO_INFER_EXPR,
        )
    }
}

/**
 * mut 函数引用限制
 *
 * 对齐 C++ DiagKind::sema_use_mutable_func_alone
 */
object CfirMutFuncReferenceChecker : CfirQualifiedAccessChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression is CfirFunctionCall) return
        val symbol = expression.resolvedFunctionSymbolOrNull() ?: return
        val function = symbol.takeIf { it.isBound }?.cfir as? CfirNamedFunction ?: return
        if (!function.status.isMut) return

        reporter.reportOn(
            source = expression.calleeReference.source ?: expression.source,
            factory = CfirErrors.USE_MUTABLE_FUNC_ALONE,
            a = function.name,
        )
    }

    private fun CfirQualifiedAccessExpression.resolvedFunctionSymbolOrNull(): CfirFunctionSymbol<*>? {
        return when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirNamedFunctionSymbol
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirNamedFunctionSymbol
            else -> null
        }
    }
}

/**
 * unsafe 函数引用限制
 *
 * 对齐 C++ DiagKind::sema_unsafe_func_can_only_be_called
 */
object CfirUnsafeFuncReferenceChecker : CfirQualifiedAccessChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression is CfirFunctionCall) return
        val symbol = expression.resolvedFunctionSymbolOrNull() ?: return
        val function = symbol.takeIf { it.isBound }?.cfir as? CfirNamedFunction ?: return
        if (!function.status.isUnsafe) return

        reporter.reportOn(
            source = expression.calleeReference.source ?: expression.source,
            factory = CfirErrors.UNSAFE_FUNC_CAN_ONLY_BE_CALLED,
        )
    }

    private fun CfirQualifiedAccessExpression.resolvedFunctionSymbolOrNull(): CfirFunctionSymbol<*>? {
        return when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirNamedFunctionSymbol
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirNamedFunctionSymbol
            else -> null
        }
    }
}

/**
 * subscript 表达式检查器
 *
 * 对齐 C++ TypeCheckExpr/SubscriptExpr.cpp:
 * - CANNOT_ASSIGN_TO_SUBSCRIPT: subscript 不可赋值（无 operator set）
 */
object CfirSubscriptAssignmentChecker : CfirSubscriptExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirSubscriptExpression) {
        val exprType = expression.coneTypeOrNull
        if (exprType is ConeErrorType) {
            val receiverType = expression.receiver.coneTypeOrNull ?: return
            if (receiverType is ConeErrorType) return
            reporter.reportOn(
                source = expression.source,
                factory = CfirErrors.INVALID_SUBSCRIPT_EXPR,
                a = receiverType,
                b = "subscript",
            )
        }

        // VArray subscript 下标数量检查
        val receiverType = expression.receiver.coneTypeOrNull
        if (receiverType is org.cangnova.cangjie.cfir.types.ConeVArrayType) {
            if (expression.indices.size != 1) {
                reporter.reportOn(
                    source = expression.source,
                    factory = CfirErrors.VARRAY_SUBSCRIPT_NUM,
                )
            }
        }
    }
}
