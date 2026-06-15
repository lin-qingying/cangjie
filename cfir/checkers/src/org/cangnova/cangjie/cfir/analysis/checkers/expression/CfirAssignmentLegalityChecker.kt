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
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirInitializationAssignmentClassifier
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirIncrementDecrementExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.isStaticMemberForOverride
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.scopes.unsubstitutedScope
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 赋值左值合法性检查。
 *
 * 这一层专门处理“这个目标能不能被赋值”，不处理赋值后的类型兼容性：
 * - `let`/不可变变量、只读属性 -> `CANNOT_ASSIGN_TO_IMMUTABLE`
 * - 函数名、类型名等非左值名字 -> `UNQUALIFIED_LEFT_VALUE_ASSIGNED`
 *
 * `subscript` 赋值仍交由独立的 `operator set` 语义链处理，这里暂不重复判定。
 */
object CfirAssignmentLegalityChecker : CfirAssignmentChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirAssignment) {
        val lValue = expression.lValue
        if (lValue is CfirSubscriptExpression) {
            when (val target = CfirMutationTargetClassifier.classifySubscriptAssignment(lValue, expression)) {
                is CfirMutationTargetClassifier.MutationTarget.ImmutableValue -> {
                    reporter.reportOn(
                        source = expression.source ?: lValue.source,
                        factory = CfirErrors.CANNOT_ASSIGN_TO_IMMUTABLE,
                    )
                }

                is CfirMutationTargetClassifier.MutationTarget.NonAssignableName -> {
                    reporter.reportOn(
                        source = lValue.source ?: expression.source,
                        factory = CfirErrors.UNQUALIFIED_LEFT_VALUE_ASSIGNED,
                        a = target.name,
                    )
                }

                CfirMutationTargetClassifier.MutationTarget.Assignable,
                null,
                -> Unit
            }
            return
        }

        val access = lValue as? CfirQualifiedAccessExpression ?: return
        val accessSource = access.calleeReference.source ?: access.source ?: expression.source ?: return
        val assignmentSource = expression.source ?: access.source ?: access.calleeReference.source ?: return

        if (CfirMutationTargetClassifier.isVArraySizeAccess(access)) {
            reporter.reportOn(
                source = assignmentSource,
                factory = CfirErrors.CANNOT_ASSIGN_TO_IMMUTABLE,
            )
            return
        }

        when (val target = CfirMutationTargetClassifier.classifyAssignment(access, expression)) {
            is CfirMutationTargetClassifier.MutationTarget.ImmutableValue -> {
                reporter.reportOn(
                    source = assignmentSource,
                    factory = CfirErrors.CANNOT_ASSIGN_TO_IMMUTABLE,
                )
            }

            is CfirMutationTargetClassifier.MutationTarget.NonAssignableName -> {
                reporter.reportOn(
                    source = accessSource,
                    factory = CfirErrors.UNQUALIFIED_LEFT_VALUE_ASSIGNED,
                    a = target.name,
                )
            }

            CfirMutationTargetClassifier.MutationTarget.Assignable,
            null,
            -> Unit
        }
    }
}

/**
 * 自增自减目标合法性检查。
 *
 * 官方 `InitializationChecker::CheckInitInExpr` 对 `IncOrDecExpr` 会调用同一套
 * `CheckLetFlag`，主诊断范围是整个自增自减表达式。因此这里复用修改目标分类器，
 * 只负责表达式自身是否能被修改，不在这一层处理数值类型或运算符类型检查。
 */
object CfirIncrementDecrementLegalityChecker : CfirIncrementDecrementExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirIncrementDecrementExpression) {
        val access = expression.expression as? CfirQualifiedAccessExpression ?: return
        val accessSource = access.calleeReference.source ?: access.source ?: expression.source ?: return
        val expressionSource = expression.source ?: access.source ?: access.calleeReference.source ?: return

        when (val target = CfirMutationTargetClassifier.classifyMutation(access)) {
            is CfirMutationTargetClassifier.MutationTarget.ImmutableValue -> {
                reporter.reportOn(
                    source = expressionSource,
                    factory = CfirErrors.CANNOT_ASSIGN_TO_IMMUTABLE,
                )
            }

            is CfirMutationTargetClassifier.MutationTarget.NonAssignableName -> {
                reporter.reportOn(
                    source = accessSource,
                    factory = CfirErrors.UNQUALIFIED_LEFT_VALUE_ASSIGNED,
                    a = target.name,
                )
            }

            CfirMutationTargetClassifier.MutationTarget.Assignable,
            null,
                -> Unit
        }
    }
}

/**
 * 自增自减数值类型检查。
 *
 * 官方语义要求 `++` / `--` 只作用于整数类型；表达式结果类型由 resolve 阶段固定为
 * `Unit`，这里仅补充操作数类型约束，避免把非整数目标误当作合法自增表达式。
 */
object CfirIncrementDecrementTypeChecker : CfirIncrementDecrementExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirIncrementDecrementExpression) {
        val source = expression.source as? AbstractCjSourceElement ?: return
        val actualType = expression.expression.coneTypeOrNull ?: return
        if (actualType is ConeErrorType) return

        val expandedType = actualType.fullyExpandedType(context.session)
        if (expandedType is ConeErrorType) return
        if ((expandedType as? ConePrimitiveType)?.kind?.isInteger == true) return

        reporter.reportOn(
            source = source,
            factory = CfirErrors.TYPE_MISMATCH,
            a = ConePrimitiveType.INT64,
            b = actualType,
            c = false,
        )
    }
}

internal object CfirMutationTargetClassifier {
    context(context: CheckerContext)
    fun classifySubscriptAssignment(
        subscript: CfirSubscriptExpression,
        assignment: CfirAssignment,
    ): MutationTarget? {
        subscript.receiver.coneTypeOrNull
            ?.fullyExpandedType(context.session) as? ConeVArrayType ?: return null

        return when (val receiver = subscript.receiver) {
            is CfirSubscriptExpression -> MutationTarget.ImmutableValue
            is CfirFunctionCall -> MutationTarget.ImmutableValue
            is CfirQualifiedAccessExpression -> when (val target = receiver.mutationTarget(assignment)) {
                MutationTarget.Assignable -> MutationTarget.Assignable
                is MutationTarget.ImmutableValue -> target
                is MutationTarget.NonAssignableName -> MutationTarget.ImmutableValue
                null -> null
            }
            else -> MutationTarget.ImmutableValue
        }
    }

    context(context: CheckerContext)
    fun isVArraySizeAccess(access: CfirQualifiedAccessExpression): Boolean {
        val name = (access.calleeReference as? CfirNamedReference)?.name ?: return false
        if (name.asString() != "size") return false
        return access.explicitReceiver
            ?.coneTypeOrNull
            ?.fullyExpandedType(context.session) is ConeVArrayType
    }

    context(context: CheckerContext)
    fun classifyAssignment(
        access: CfirQualifiedAccessExpression,
        assignment: CfirAssignment,
    ): MutationTarget? {
        return access.mutationTarget(assignment)
    }

    context(context: CheckerContext)
    fun classifyMutation(access: CfirQualifiedAccessExpression): MutationTarget? {
        return access.mutationTarget(assignment = null)
    }

    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.mutationTarget(assignment: CfirAssignment?): MutationTarget? {
        val resolvedSymbol = resolvedAssignableSymbolOrNull()
        val directTarget = when (resolvedSymbol) {
            is CfirFieldVariableSymbol -> {
                val field = resolvedSymbol.takeIf { it.isBound }?.cfir
                if (field != null && isImmutableFieldAssignmentForbidden(field)) {
                    MutationTarget.ImmutableValue
                } else {
                    MutationTarget.Assignable
                }
            }

            is CfirVariableSymbol<*> -> {
                val variable = resolvedSymbol.takeIf { it.isBound }?.cfir
                if (variable != null && isImmutableVariableAssignmentForbidden(variable, assignment)) {
                    MutationTarget.ImmutableValue
                } else {
                    MutationTarget.Assignable
                }
            }

            is CfirPropertySymbol -> {
                val property = resolvedSymbol.takeIf { it.isBound }?.cfir as? CfirProperty
                if (property != null && !property.hasUsableSetter()) {
                    MutationTarget.ImmutableValue
                } else {
                    MutationTarget.Assignable
                }
            }

            is CfirFunctionSymbol<*> -> MutationTarget.NonAssignableName(referenceNameOrFallback())
            is CfirClassLikeSymbol<*> -> MutationTarget.NonAssignableName(referenceNameOrFallback())
            null -> null
            else -> null
        }
        if (directTarget is MutationTarget.ImmutableValue || directTarget is MutationTarget.NonAssignableName) {
            return directTarget
        }
        if (isImmutableStructReceiverMutationForbidden()) return MutationTarget.ImmutableValue
        return directTarget
    }

    private fun CfirQualifiedAccessExpression.resolvedAssignableSymbolOrNull(): CfirBasedSymbol<*>? {
        return when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
            else -> null
        }
    }

    /**
     * Cangjie 允许 `mut prop` 的 getter/setter 分别由子类和父类提供。
     *
     * 官方 `GetUsableSetterForProperty` 在当前属性没有 setter 时会继续沿父类查找有效 setter；
     * 这里复用 CFIR 的 override scope 与签名规则实现同一语义。
     */
    context(context: CheckerContext)
    private fun CfirProperty.hasUsableSetter(): Boolean {
        if (!status.isMut) return false
        if (setter != null) return true

        val propertySymbol = symbol as? CfirPropertySymbol ?: return false
        val ownerClassId = propertySymbol.callableId.classId ?: return false
        val ownerSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId) ?: return false
        val ownerDeclaration = ownerSymbol.cfir as? CfirClassLikeDeclaration ?: return false
        val ownerScope = ownerDeclaration.unsubstitutedScope(
            useSiteSession = context.session,
            scopeSession = context.scopeSession,
            withForcedTypeCalculator = false,
            memberRequiredPhase = null,
        )

        return propertySymbol.hasInheritedUsableSetter(ownerScope, linkedSetOf())
    }

    private fun CfirPropertySymbol.hasInheritedUsableSetter(
        scope: CfirTypeScope,
        visited: MutableSet<CfirPropertySymbol>,
    ): Boolean {
        if (!visited.add(this)) return false

        val targetSignature = overrideSignatureKey()
        val targetIsStatic = isStaticMemberForOverride()
        var found = false
        scope.processDirectOverriddenPropertiesWithBaseScope(this) { candidate, baseScope ->
            if (
                candidate == this ||
                candidate.overrideSignatureKey() != targetSignature ||
                candidate.isStaticMemberForOverride() != targetIsStatic
            ) {
                return@processDirectOverriddenPropertiesWithBaseScope ProcessorAction.NEXT
            }

            val candidateProperty = candidate.takeIf { it.isBound }?.cfir
            if (
                candidateProperty != null &&
                candidateProperty.status.isMut &&
                (candidateProperty.setter != null || candidate.hasInheritedUsableSetter(baseScope, visited))
            ) {
                found = true
                ProcessorAction.STOP
            } else {
                ProcessorAction.NEXT
            }
        }
        return found
    }

    private fun CfirQualifiedAccessExpression.referenceNameOrFallback(): Name {
        return (calleeReference as? CfirNamedReference)?.name ?: Name.ERROR_NAME
    }

    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.isImmutableFieldAssignmentForbidden(field: CfirFieldVariable): Boolean {
        if (field.isVar) return false
        val inConstructor = context.findClosestDeclaration<CfirConstructor>() != null
        if (!inConstructor) return true
        return field.initializer != null
    }

    context(context: CheckerContext)
    private fun isImmutableVariableAssignmentForbidden(
        variable: CfirVariable,
        assignment: CfirAssignment?,
    ): Boolean {
        if (variable.isVar) return false
        if (assignment != null && variable.isLocal && variable.initializer == null) {
            return !CfirInitializationAssignmentClassifier.isInitializationAssignment(assignment, context)
        }
        return true
    }

    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.isImmutableStructReceiverMutationForbidden(): Boolean {
        val receiver = explicitReceiver ?: dispatchReceiver ?: return false
        val receiverAccess = receiver as? CfirQualifiedAccessExpression ?: return false
        val receiverSymbol = receiverAccess.resolvedAssignableSymbolOrNull() as? CfirVariableSymbol<*> ?: return false
        val receiverVariable = receiverSymbol.takeIf { it.isBound }?.cfir ?: return false
        if (receiverVariable.isVar) return false
        return receiver.coneTypeOrNull.mayBeStructValueType()
    }

    sealed interface MutationTarget {
        data object Assignable : MutationTarget
        data object ImmutableValue : MutationTarget
        data class NonAssignableName(val name: Name) : MutationTarget
    }
}

internal fun ConeCangJieType?.mayBeStructValueType(): Boolean = when (this) {
    is ConeStructType -> true
    is ConeTypeParameterType -> lookupTag.typeParameterSymbol.resolvedBounds.any { it.coneType.mayBeStructValueType() }
    is ConeClassLikeType -> isInterface
    else -> false
}
