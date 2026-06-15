/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers.expression

import java.math.BigInteger
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.collectors.components.ErrorNodeDiagnosticCollectorComponent
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferType
import org.cangnova.cangjie.cfir.diagnostic.ConeCommandHandleTypeError
import org.cangnova.cangjie.cfir.diagnostic.ConeCommandIncompatibleTypeError
import org.cangnova.cangjie.cfir.diagnostic.ConeMismatchingHandleBlockError
import org.cangnova.cangjie.cfir.diagnostics.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.CfirCatchPattern
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

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
        if (expression is CfirMatchBranch && expression.body.coneTypeOrNull is ConeErrorType) return
        if (expression is CfirSmartCastExpression) return

        if (expression is CfirDiagnosticHolder) return
        if (expression is CfirResolvable) {
            val calleeReference = expression.calleeReference
            if (calleeReference is CfirDiagnosticHolder) return
            if (calleeReference is CfirSuperReference && calleeReference.superTypeRef is CfirErrorTypeRef) return
            if (calleeReference is CfirResolvedNamedReference) {
                val symbol = calleeReference.resolvedSymbol as? CfirCallableSymbol<*>
                if (symbol?.resolvedReturnTypeRef is CfirErrorTypeRef) return
                if (symbol?.resolvedReturnType?.contains { it is ConeErrorType && it.diagnostic == type.diagnostic } == true) return
            }
        }
        if (expression is CfirThisReceiverExpression && expression.calleeReference.diagnostic != null) return
        if (expression is CfirAnnotationCall && expression.typeRef is CfirErrorTypeRef) return
        if (expression is CfirTypeOperator && expression.typeRef is CfirErrorTypeRef) return

        val source = expression.source
        if (source != null) {
            val diagnostic = type.diagnostic
            if (diagnostic is ConeMismatchingHandleBlockError &&
                (expression is CfirTryExpression || expression is CfirHandleClause)
            ) {
                return
            }
            if (diagnostic is ConeCannotInferType) return
            if (diagnostic is ConeSimpleDiagnostic) {
                when (diagnostic.kind) {
                    DiagnosticKind.RecursionInImplicitTypes -> return
                    else -> {}
                }
            }
            val diagnosticSource = when {
                expression is CfirPerformExpression && diagnostic is ConeCommandIncompatibleTypeError ->
                    expression.expression.source ?: source
                expression is CfirHandleClause && diagnostic is ConeCommandHandleTypeError ->
                    expression.commandPattern.typeRefs.firstOrNull()?.source ?: expression.commandPattern.source ?: source
                else -> source
            }
            ErrorNodeDiagnosticCollectorComponent.reportCfirDiagnostic(
                diagnostic,
                diagnosticSource,
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
 * finalizer 中禁止把 `this` 当值直接使用，但允许把它当作成员访问接收者。
 *
 * 对齐 `class_finalizer2.cj` 语义：
 * - `this.x` / 通过隐式 receiver 访问 `x` 合法；
 * - `f(this)`、裸 `this` 非法。
 *
 * `CfirThisReceiverExpression` 不是 `CfirQualifiedAccessExpression`，因此这里必须挂在
 * basic expression 分发上，而不能挂在 qualified access 分发上。
 */
object CfirFinalizerThisUsageChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        if (expression !is CfirThisReceiverExpression) return
        val containingFunction = context.findClosestDeclaration<CfirFunction>() ?: return
        if (containingFunction !is CfirFinalizer) return

        val parent = context.callsOrAssignments
            .lastOrNull() as? CfirQualifiedAccessExpression
        if (parent?.explicitReceiver === expression) return

        reporter.reportOn(
            source = expression.source ?: expression.calleeReference.source,
            factory = CfirErrors.INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER,
            a = "function",
        )
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
        val receiverType = expression.receiver.coneTypeOrNull
            ?.fullyExpandedType(context.session) as? ConeVArrayType ?: return

        if (expression.indices.size != 1) {
            reporter.reportOn(
                source = expression.source,
                factory = CfirErrors.VARRAY_SUBSCRIPT_NUM,
            )
            return
        }

        val index = expression.indices.single()
        val indexType = index.coneTypeOrNull
        if (indexType != null && indexType !is ConeErrorType) {
            val int64Type = ConePrimitiveType(PrimitiveTypeKind.INT64)
            if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, indexType, int64Type) != true) {
                reporter.reportOn(
                    source = index.source,
                    factory = CfirErrors.TYPE_MISMATCH,
                    a = int64Type,
                    b = indexType,
                    c = false,
                )
                return
            }
        } else {
            return
        }

        val parsedIndex = CfirIntConstantEvalUtils.parseSignedIntExpression(index) ?: return
        if (parsedIndex.explicitSuffix != null && parsedIndex.explicitSuffix != "i64") return

        val size = BigInteger.valueOf(receiverType.size)
        if (parsedIndex.value < BigInteger.ZERO || parsedIndex.value >= size) {
            reporter.reportOn(
                source = expression.receiver.source ?: index.source ?: expression.source,
                factory = CfirErrors.BUILTIN_INDEX_IN_BOUND,
            )
        }
    }
}

/**
 * throw 表达式类型检查。
 *
 * 对齐官方 C++ `TypeCheckExpr/ThrowExpr.cpp`：
 * - 被抛表达式必须是 `std.core.Exception` 或 `std.core.Error` 的子类型；
 * - `throw` 表达式本身在 resolve 阶段保持 `Nothing`，这里只负责诊断。
 */
object CfirThrowExpressionTypeChecker : CfirThrowExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirThrowExpression) {
        val thrownType = expression.exception.coneTypeOrNull ?: return
        if (thrownType is ConeErrorType) return
        if (thrownType == ConePrimitiveType.NOTHING) return
        if (thrownType.isSubtypeOfExceptionOrError(context)) return

        reporter.reportOn(
            source = expression.source,
            factory = CfirErrors.THROW_EXPR_WITH_WRONG_TYPE,
        )
    }
}

/**
 * try/catch 异常类型检查。
 *
 * 对齐官方 C++ `TypeCheckPattern.cpp#ChkExceptTypePattern`：
 * - catch 参数属性类型必须是 `std.core.Exception` 或 `std.core.Error` 的子类型；
 * - 后续 catch 类型若已被前面的 catch 类型覆盖，报告 `USELESS_EXCEPTION_TYPE`。
 */
object CfirCatchTypeChecker : CfirTryExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirTryExpression) {
        val includedTypes = mutableListOf<ConeCangJieType>()

        for (catchClause in expression.catches) {
            for ((catchType, source) in catchClause.pattern.resolvedCatchTypes()) {
                if (catchType is ConeErrorType) continue

                if (!catchType.isSubtypeOfExceptionOrError(context)) {
                    reporter.reportOn(
                        source = source ?: catchClause.pattern.source,
                        factory = CfirErrors.CATCH_TYPE_MUST_EXTEND_EXCEPTION,
                    )
                    continue
                }

                if (includedTypes.any { previous -> catchType.isSubtypeOf(previous, context) }) {
                    reporter.reportOn(
                        source = source ?: catchClause.pattern.source,
                        factory = CfirErrors.USELESS_EXCEPTION_TYPE,
                    )
                } else {
                    includedTypes += catchType
                }
            }
        }
    }
}

private fun CfirCatchPattern.resolvedCatchTypes(): List<Pair<ConeCangJieType, CjSourceElement?>> {
    if (typeRefs.isEmpty()) {
        return listOf(
            org.cangnova.cangjie.cfir.types.ConeClassLikeType(StdlibClassIds.Exception.toLookupTag()) to source,
        )
    }

    return typeRefs.mapNotNull { typeRef ->
        val coneType = (typeRef as? CfirResolvedTypeRef)?.coneType ?: return@mapNotNull null
        coneType to typeRef.source
    }
}

/**
 * try-with-resources 资源类型检查。
 *
 * 对齐官方 `TypeCheckExpr/TryExpr.cpp`：资源说明表达式的结果类型必须实现 `std.core.Resource`，
 * 但即使类型不匹配，资源绑定名和 try body 仍继续分析。
 */
object CfirTryResourceTypeChecker : CfirTryExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirTryExpression) {
        if (expression.resources.isEmpty()) return

        val resourceType = org.cangnova.cangjie.cfir.types.ConeClassLikeType(StdlibClassIds.Resource.toLookupTag())
        for (resource in expression.resources) {
            val actualType = (resource.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (actualType is ConeErrorType) continue
            if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, actualType, resourceType) == true) continue

            reporter.reportOn(
                source = resource.source,
                factory = CfirErrors.MISMATCHED_TYPES_BECAUSE,
                a = resourceType,
                b = actualType,
                c = "try-with-resources requires std.core.Resource",
            )
        }
    }
}

private fun ConeCangJieType.isSubtypeOfExceptionOrError(
    context: CheckerContext,
): Boolean {
    val exceptionType = org.cangnova.cangjie.cfir.types.ConeClassLikeType(StdlibClassIds.Exception.toLookupTag())
    val errorType = org.cangnova.cangjie.cfir.types.ConeClassLikeType(StdlibClassIds.Error.toLookupTag())
    return isSubtypeOf(exceptionType, context) || isSubtypeOf(errorType, context)
}

private fun ConeCangJieType.isSubtypeOf(
    superType: ConeCangJieType,
    context: CheckerContext,
): Boolean =
    AbstractTypeChecker.isSubtypeOf(context.session.typeContext, this, superType) == true
