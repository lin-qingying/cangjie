package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.collectors.components.ErrorNodeDiagnosticCollectorComponent
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferType
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.expressions.CfirSmartCastExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.expressions.CfirThisReceiverExpression
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperator
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
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
 * 错误类型表达式检查器。
 *
 * 对齐 Kotlin FIR `FirExpressionWithErrorTypeChecker`：只在错误没有被子节点、
 * 引用或显式错误类型引用报告时，才把表达式携带的 Cone diagnostic 交给统一
 * 的 ErrorNode collector 映射。仓颉的 `UNABLE_TO_INFER_EXPR` 仍由既有
 * Cone diagnostic -> CFIR diagnostic 映射产生。
 */
object CfirExpressionWithErrorTypeChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        if (expression !is CfirExpression) return
        val type = expression.coneTypeOrNull
        if (type !is ConeErrorType) return
        if (expression is CfirBlock) return
        if (expression is CfirSmartCastExpression) return

        if (expression is CfirDiagnosticHolder) return
        if (expression is CfirResolvable) {
            val calleeReference = expression.calleeReference
            if (calleeReference is CfirDiagnosticHolder) return
            if (calleeReference is CfirSuperReference && calleeReference.superTypeRef is CfirErrorTypeRef) return
            if (calleeReference is CfirResolvedNamedReference) {
                val symbol = calleeReference.resolvedSymbol as? CfirCallableSymbol<*>
                if (symbol?.resolvedReturnTypeRef is CfirErrorTypeRef) return
            }
        }
        if (expression is CfirThisReceiverExpression && expression.calleeReference.diagnostic != null) return
        if (expression is CfirAnnotationCall && expression.typeRef is CfirErrorTypeRef) return
        if (expression is CfirTypeOperator && expression.typeRef is CfirErrorTypeRef) return

        val source = expression.source
        if (source != null) {
            val diagnostic = type.diagnostic
            if (diagnostic is ConeCannotInferType) return
            if (diagnostic is ConeSimpleDiagnostic) {
                when (diagnostic.kind) {
                    DiagnosticKind.RecursionInImplicitTypes -> return
                    else -> {}
                }
            }
            ErrorNodeDiagnosticCollectorComponent.reportCfirDiagnostic(
                diagnostic,
                source,
                context,
                reporter = reporter,
            )
        }
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
 * - resolve 阶段产生的 subscript operator 错误由 Cone 诊断统一映射；
 * - 这里只检查不依赖 operator resolve 的 subscript 语义。
 */
object CfirSubscriptAssignmentChecker : CfirSubscriptExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirSubscriptExpression) {
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
