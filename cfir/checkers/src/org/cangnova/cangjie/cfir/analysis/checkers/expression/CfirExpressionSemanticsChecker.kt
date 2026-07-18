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

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.firstCharacterDiagnosticSource
import org.cangnova.cangjie.cfir.analysis.collectors.components.ErrorNodeDiagnosticCollectorComponent
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferType
import org.cangnova.cangjie.cfir.diagnostic.ConeCommandHandleTypeError
import org.cangnova.cangjie.cfir.diagnostic.ConeCommandIncompatibleTypeError
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithSingleCandidate
import org.cangnova.cangjie.cfir.diagnostic.ConeMismatchingHandleBlockError
import org.cangnova.cangjie.cfir.diagnostic.ConeTypeMismatchError
import org.cangnova.cangjie.cfir.diagnostics.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.patterns.CfirCatchPattern
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker
import java.math.BigInteger

/**
 * 浮点字面量范围检查器
 *
 * 对齐 C++ TypeCheckExpr/LitConstExpr.cpp:
 * - EXCEED_FLOAT_LITERAL_RANGE: NaN/Infinity
 * - FLOAT_LITERAL_TOO_LARGE: 超出目标类型最大值（警告）
 * - FLOAT_LITERAL_TOO_SMALL: 小于目标类型最小正值（警告）
 */
object CfirFloatLiteralRangeChecker : CfirLiteralExpressionChecker() {
    /**
     * 检查浮点字面量的特殊值和 Float32 目标范围。
     *
     * NaN/Infinity 直接按错误报告；解析类型为 Float32 时进一步区分过大和过小的 warning。
     */
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
    /**
     * 将表达式自身携带的 Cone 错误类型诊断映射到 CFIR 诊断。
     *
     * 该入口会跳过已经由子表达式、引用节点、显式错误类型引用或特殊控制流节点报告过的错误，
     * 保证错误类型诊断不会重复落点。
     */
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
                val returnTypeRef = symbol?.cfir?.returnTypeRef
                if (returnTypeRef is CfirErrorTypeRef) return
                val returnType = (returnTypeRef as? CfirResolvedTypeRef)?.coneType
                if (returnType?.contains { it is ConeErrorType && it.diagnostic == type.diagnostic } == true) return
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
            if (
                diagnostic.unwrapUnreportedDuplicateDiagnostic() is ConeTypeMismatchError &&
                expression.isReturnedExpressionRoot(context)
            ) {
                return
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
 * 判断当前错误表达式是否正是 `return expr` 的返回值根表达式。
 *
 * 返回值根上的通用类型不匹配应交给 [CfirReturnTypeMismatchChecker] 统一分类为
 * RETURN_TYPE_MISMATCH；return 内部更深层的实参/接收者错误仍保留原有诊断入口。
 */
private fun CfirExpression.isReturnedExpressionRoot(context: CheckerContext): Boolean {
    val returnExpression = context.containingStatements
        .asReversed()
        .filterIsInstance<CfirReturnExpression>()
        .firstOrNull()
        ?: return false
    return returnExpression.result.unwrapWrappedExpression() === this.unwrapWrappedExpression()
}

/** 去掉 CFIR wrapped expression，取得实际表达式根。 */
private tailrec fun CfirExpression.unwrapWrappedExpression(): CfirExpression = when (this) {
    is CfirWrappedExpression -> expression.unwrapWrappedExpression()
    else -> this
}

/** 解开用于去重占位的诊断包装，返回真正需要比较和分类的原始诊断。 */
private fun ConeDiagnostic.unwrapUnreportedDuplicateDiagnostic(): ConeDiagnostic =
    (this as? ConeUnreportedDuplicateDiagnostic)?.original ?: this

/**
 * mut 函数引用限制
 *
 * 对齐 C++ DiagKind::sema_use_mutable_func_alone
 */
object CfirMutFuncReferenceChecker : CfirQualifiedAccessChecker() {
    /**
     * 检查 `mut` 函数是否被单独引用而非调用。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression is CfirFunctionCall) return
        val resolvedFunction = expression.resolvedFunctionSymbolOrNull()
            ?.takeIf { it.isBound }
            ?.cfir as? CfirNamedFunction
        val function = resolvedFunction ?: expression.declaredUpperBoundMutFunctionOrNull() ?: return
        if (!function.status.isMut) return

        reporter.reportOn(
            source = expression.calleeReference.source ?: expression.source,
            factory = CfirErrors.USE_MUTABLE_FUNC_ALONE,
            a = function.name,
        )
    }

    /**
     * 从 qualified access 中解析目标函数符号。
     */
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
    /**
     * 检查 `unsafe` 函数是否被单独引用而非调用。
     */
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

    /**
     * 从 qualified access 中解析目标函数符号。
     */
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
    /**
     * 检查 finalizer 中裸 `this` 的非法使用。
     *
     * 作为成员访问显式接收者的 `this` 被允许，其余作为普通值流动的 `this` 报 finalizer 限制诊断。
     */
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
 * open/abstract class 构造器中禁止把 `this` 当作普通表达式。
 *
 * 官方 `TypeCheckReference.cpp::CheckUsageOfThis` 使用最外层函数判断
 * “constructor of inheritable class”，因此 lambda 或局部函数体中的裸 `this`
 * 仍继承外层构造器语义；`this.member` 只是成员访问接收者，不由本规则报告。
 */
object CfirOpenConstructorThisUsageChecker : CfirBasicExpressionChecker() {
    /**
     * 检查 open/abstract class 构造器中的裸显式 `this`。
     *
     * 隐式 `this` 和成员访问接收者不由本规则报告；普通表达式位置的显式 `this` 使用构造器所属
     * class kind 构造诊断消息。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        if (expression !is CfirThisReceiverExpression) return
        if (expression.calleeReference.isImplicit) return
        val owner = context.openClassConstructorOwner() ?: return

        val parent = context.callsOrAssignments.lastOrNull() as? CfirQualifiedAccessExpression
        if (parent?.explicitReceiver === expression || parent?.dispatchReceiver === expression) return

        val classKind = if (owner.status.isOpen) "open" else "abstract"
        reporter.reportOn(
            source = expression.calleeReference.source?.firstCharacterDiagnosticSource()
                ?: expression.source?.firstCharacterDiagnosticSource(),
            factory = CfirErrors.THIS_AS_EXPRESSION_IN_FUNC,
            a = "constructor of $classKind class",
        )
    }
}

/**
 * static 函数体中禁止引用实例 `this`。
 *
 * 官方 `TypeCheckReference.cpp::CheckUsageOfThis` 在当前函数体带 `static`
 * 属性时报告 `sema_static_members_cannot_call_members`。这里挂在 basic
 * expression checker 上，覆盖裸 `this` 与 `this.member` 接收者。
 */
object CfirStaticContextThisUsageChecker : CfirBasicExpressionChecker() {
    /**
     * 检查 static 函数体内的显式实例 `this`。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        if (expression !is CfirThisReceiverExpression) return
        if (expression.calleeReference.isImplicit) return
        val containingFunction = context.findClosestDeclaration<CfirFunction>() ?: return
        if (!containingFunction.status.isStatic) return

        reporter.reportOn(
            source = expression.calleeReference.source ?: expression.source,
            factory = CfirErrors.STATIC_MEMBERS_CANNOT_CALL_MEMBERS,
        )
    }
}

/**
 * static 函数体和 static lambda 体中禁止隐式访问实例成员。
 *
 * 官方 `TypeCheckExpr.cpp::CheckRefExprOfCurStruct` 按当前函数体区分：
 * 有 static 函数声明时报告 static function 诊断；当前函数体来自 lambda
 * 时报告 static lambda 诊断。这里基于已解析 symbol 判断目标成员，避免重复名字查找。
 */
object CfirStaticContextNonStaticMemberAccessChecker : CfirQualifiedAccessChecker() {
    /**
     * 检查 static 上下文中对当前实例成员的隐式访问。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression.explicitReceiver != null) return
        val accessKind = context.staticNonStaticAccessKind() ?: return
        val memberName = expression.resolvedAccessSymbolOrNull()
            ?.nonStaticMemberNameForStaticContext()
            ?: return

        when (accessKind) {
            StaticNonStaticAccessKind.FUNCTION -> reporter.reportOn(
                source = expression.calleeReference.source?.firstCharacterDiagnosticSource()
                    ?: expression.source?.firstCharacterDiagnosticSource(),
                factory = CfirErrors.STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER,
                a = memberName,
            )

            StaticNonStaticAccessKind.LAMBDA -> reporter.reportOn(
                source = expression.calleeReference.source?.firstCharacterDiagnosticSource()
                    ?: expression.source?.firstCharacterDiagnosticSource(),
                factory = CfirErrors.STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC,
                a = memberName,
            )
        }
    }

    /**
     * 识别当前表达式所在的 static 函数或 static lambda 上下文。
     */
    private fun CheckerContext.staticNonStaticAccessKind(): StaticNonStaticAccessKind? {
        val closestFunction = containingDeclarations.asReversed()
            .filterIsInstance<CfirFunction>()
            .firstOrNull()
            ?: return null

        if (closestFunction is CfirAnonymousFunction) {
            if (!closestFunction.isLambda) return null
            return if (hasStaticEnclosingDeclarationAfter(closestFunction)) {
                StaticNonStaticAccessKind.LAMBDA
            } else {
                null
            }
        }

        return if (closestFunction.status.isStatic || hasStaticEnclosingDeclarationAfter(closestFunction)) {
            StaticNonStaticAccessKind.FUNCTION
        } else {
            null
        }
    }

    /**
     * lambda 和本地函数本身不带 static 状态；它们从外层 static 函数或 static 存储成员继承 static 语境。
     */
    private fun CheckerContext.hasStaticEnclosingDeclarationAfter(function: CfirFunction): Boolean {
        return containingDeclarations.asReversed()
            .dropWhile { declaration -> declaration !== function }
            .drop(1)
            .any { declaration -> declaration is CfirCallableDeclaration && declaration.status.isStatic }
    }

    /**
     * 只把当前类型的非 static 成员作为 static 上下文非法访问目标。
     */
    private fun CfirBasedSymbol<*>.nonStaticMemberNameForStaticContext(): org.cangnova.cangjie.name.Name? {
        return when (this) {
            is CfirVariableSymbol<*> -> if (isBound && callableId.classId != null && cfir is CfirFieldVariable && !cfir.status.isStatic) {
                name
            } else {
                null
            }

            is CfirPropertySymbol -> if (isBound && callableId.classId != null && !cfir.status.isStatic) {
                name
            } else {
                null
            }

            is CfirPropertyAccessorSymbol -> if (isBound) {
                propertySymbol.nonStaticMemberNameForStaticContext()
            } else {
                null
            }

            is CfirNamedFunctionSymbol -> if (isBound && callableId.classId != null && !cfir.status.isStatic) {
                name
            } else {
                null
            }

            else -> null
        }
    }

    /**
     * 从 qualified access 中解析访问目标符号。
     */
    private fun CfirQualifiedAccessExpression.resolvedAccessSymbolOrNull(): CfirBasedSymbol<*>? {
        return when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            is CfirResolvedErrorReference -> reference.resolvedSymbol
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
            is CfirErrorNamedReference -> (reference.diagnostic as? ConeDiagnosticWithSingleCandidate)?.candidateSymbol
            else -> null
        }
    }
}

/**
 * static 上下文中实例成员访问的诊断类别。
 */
private enum class StaticNonStaticAccessKind {
    FUNCTION,
    LAMBDA,
}

/**
 * open/abstract class 构造器中禁止访问实例函数或属性。
 *
 * 官方 `TypeCheckExpr.cpp::CheckForbiddenFuncReferenceAccess` 对 class-like 与 extend
 * 中的实例函数/属性统一生效，排除 constructor 与 static 成员。CFIR 侧按 resolved
 * callable 的 dispatch receiver 判断实例成员，并覆盖调用、函数引用与属性访问。
 */
object CfirOpenConstructorMemberAccessChecker : CfirQualifiedAccessChecker() {
    /**
     * 检查 open/abstract class 构造器中对实例成员的访问。
     *
     * 当前实例接收者上的实例函数和属性访问会被禁止；属性作为可写赋值左值时交由初始化/赋值规则处理。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        val owner = context.openClassConstructorOwner() ?: return
        if (!expression.usesCurrentInstanceReceiver()) return

        val target = expression.resolvedCallableTarget() ?: return
        if (target is CfirProperty && expression.isWritablePropertyAssignmentLValue(target)) return
        if (!target.isForbiddenOpenConstructorMember()) return

        val memberKind = when (target) {
            is CfirProperty -> "property"
            is CfirNamedFunction -> "function"
            else -> return
        }

        reporter.reportOn(
            source = expression.calleeReference.source?.firstCharacterDiagnosticSource()
                ?: expression.source?.firstCharacterDiagnosticSource(),
            factory = CfirErrors.ILLEGAL_MEMBER_USED_IN_OPEN_CONSTRUCTOR,
            a = memberKind,
            b = target.symbol.callableId.callableName.asString(),
            c = owner.name,
        )
    }

    /**
     * 判断访问是否使用当前实例作为接收者。
     *
     * 无显式接收者表示隐式当前实例接收者，显式 `this` / `super` 也属于当前实例访问。
     */
    private fun CfirQualifiedAccessExpression.usesCurrentInstanceReceiver(): Boolean {
        val receiver = explicitReceiver
        return receiver == null || receiver is CfirThisReceiverExpression || receiver is CfirSuperReceiverExpression
    }

    /**
     * 从引用节点解析 callable 声明目标。
     *
     * 正常引用、错误恢复引用、候选引用和携带单候选诊断的错误引用都可能提供真实 callable。
     */
    private fun CfirQualifiedAccessExpression.resolvedCallableTarget(): CfirCallableDeclaration? {
        return when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol.cfir as? CfirCallableDeclaration
            is CfirResolvedErrorReference -> reference.resolvedSymbol.cfir as? CfirCallableDeclaration
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol?.cfir as? CfirCallableDeclaration
            is CfirErrorNamedReference ->
                (reference.diagnostic as? ConeDiagnosticWithSingleCandidate)?.candidateSymbol?.cfir as? CfirCallableDeclaration
            else -> null
        }
    }

    /**
     * 判断 callable 是否属于 open 构造器中禁止访问的实例成员。
     *
     * 构造器、枚举构造器、static 成员不受限；普通成员通过 dispatch receiver 判断，extend 成员
     * 通过 extend provider 判断其所属 extend 是否可访问。
     */
    context(context: CheckerContext)
    private fun CfirCallableDeclaration.isForbiddenOpenConstructorMember(): Boolean {
        if (this is CfirConstructor || this is CfirEnumConstructor) return false
        if (this !is CfirNamedFunction && this !is CfirProperty) return false
        if (status.isStatic) return false
        if (dispatchReceiverType != null) return true
        val extendProvider = context.session.extendProviderOrNull ?: return false
        val ownerExtend = extendProvider.getContainingExtend(symbol.unwrapSubstitutionOverrides()) ?: return false
        return extendProvider.isExtendAccessible(ownerExtend)
    }

    /**
     * 判断当前 qualified access 是否作为可写属性赋值左值出现。
     *
     * 有 setter 的属性左值由赋值/初始化规则决定，不作为 open 构造器成员访问规则的直接错误。
     */
    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.isWritablePropertyAssignmentLValue(property: CfirProperty): Boolean {
        if (property.setter == null) return false
        val assignment = context.callsOrAssignments
            .asReversed()
            .filterIsInstance<CfirAssignment>()
            .firstOrNull()
            ?: return false
        return assignment.lValue === this
    }
}

/**
 * 禁止通过 `super` 直接访问抽象函数。
 *
 * 官方语义将 `super.abstractFunc()` 与 `super.abstractFunc` 都视为直接访问抽象成员。
 * 这里在解析后的 qualified access 上检查目标函数状态，并把诊断落在 `super` 关键字。
 */
object CfirAbstractSuperMemberAccessChecker : CfirQualifiedAccessChecker() {
    /**
     * 检查通过 `super` 直接访问抽象函数的场景。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        val receiver = expression.explicitReceiver as? CfirSuperReceiverExpression ?: return
        val target = expression.resolvedCallableTarget() as? CfirNamedFunction ?: return
        if (!target.status.isAbstract) return

        reporter.reportOn(
            source = receiver.calleeReference.source?.firstCharacterDiagnosticSource()
                ?: receiver.source?.firstCharacterDiagnosticSource()
                ?: expression.source,
            factory = CfirErrors.ABSTRACT_METHOD_CANNOT_BE_ACCESSED_DIRECTLY,
        )
    }

    /**
     * 从引用节点解析 callable 声明目标。
     */
    private fun CfirQualifiedAccessExpression.resolvedCallableTarget(): CfirCallableDeclaration? {
        return when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol.cfir as? CfirCallableDeclaration
            is CfirResolvedErrorReference -> reference.resolvedSymbol.cfir as? CfirCallableDeclaration
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol?.cfir as? CfirCallableDeclaration
            is CfirErrorNamedReference ->
                (reference.diagnostic as? ConeDiagnosticWithSingleCandidate)?.candidateSymbol?.cfir as? CfirCallableDeclaration
            else -> null
        }
    }
}

/**
 * 查找当前上下文中 open/abstract class 的构造器所属类。
 *
 * 只有调用栈中存在构造器且最近 class 为 open 或 abstract 时返回 owner。
 */
private fun CheckerContext.openClassConstructorOwner(): CfirClass? {
    containingDeclarations.asReversed().firstOrNull { it is CfirConstructor } ?: return null
    val owner = findClosestDeclaration<CfirClass>() ?: return null
    return owner.takeIf { it.status.isOpen || it.status.isAbstract }
}

/**
 * subscript 表达式检查器
 *
 * 对齐 C++ TypeCheckExpr/SubscriptExpr.cpp:
 * - resolve 阶段产生的 subscript operator 错误由 Cone 诊断统一映射；
 * - 这里只检查不依赖 operator resolve 的 subscript 语义。
 */
object CfirSubscriptAssignmentChecker : CfirSubscriptExpressionChecker() {
    /**
     * 检查 VArray 下标表达式的内建语义。
     *
     * VArray 只允许单个 Int64 兼容索引，常量索引必须位于 `[0, size)` 范围内。
     */
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
    /**
     * 检查 `throw` 后表达式是否为 Exception 或 Error 子类型。
     */
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
    /**
     * 检查 catch 类型合法性和覆盖关系。
     *
     * 每个 catch pattern 的类型必须是 Exception/Error 子类型，且不能被前面已经包含的 catch 类型覆盖。
     */
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

/**
 * 解析 catch pattern 中声明的 catch 类型列表。
 *
 * 无显式类型时按官方默认 Exception 类型处理，并保留源码范围用于诊断定位。
 */
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
    /**
     * 检查 try-with-resources 资源表达式类型。
     *
     * 每个资源声明的返回类型必须是 `std.core.Resource` 子类型，错误类型不重复报告。
     */
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

/**
 * 判断类型是否为 `std.core.Exception` 或 `std.core.Error` 的子类型。
 */
private fun ConeCangJieType.isSubtypeOfExceptionOrError(
    context: CheckerContext,
): Boolean {
    val exceptionType = org.cangnova.cangjie.cfir.types.ConeClassLikeType(StdlibClassIds.Exception.toLookupTag())
    val errorType = org.cangnova.cangjie.cfir.types.ConeClassLikeType(StdlibClassIds.Error.toLookupTag())
    return isSubtypeOf(exceptionType, context) || isSubtypeOf(errorType, context)
}

/**
 * 使用当前会话类型上下文判断 this 是否为指定超类型的子类型。
 */
private fun ConeCangJieType.isSubtypeOf(
    superType: ConeCangJieType,
    context: CheckerContext,
): Boolean =
    AbstractTypeChecker.isSubtypeOf(context.session.typeContext, this, superType) == true
