package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.Name

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
        if (lValue is CfirSubscriptExpression) return

        val access = lValue as? CfirQualifiedAccessExpression ?: return
        val source = access.calleeReference.source ?: access.source ?: expression.source ?: return

        when (val target = access.assignmentTarget()) {
            is AssignmentTarget.ImmutableValue -> {
                reporter.reportOn(
                    source = source,
                    factory = CfirErrors.CANNOT_ASSIGN_TO_IMMUTABLE,
                )
            }

            is AssignmentTarget.NonAssignableName -> {
                reporter.reportOn(
                    source = source,
                    factory = CfirErrors.UNQUALIFIED_LEFT_VALUE_ASSIGNED,
                    a = target.name,
                )
            }

            AssignmentTarget.Assignable,
            null,
            -> Unit
        }
    }

    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.assignmentTarget(): AssignmentTarget? {
        val resolvedSymbol = resolvedAssignableSymbolOrNull()
        return when (resolvedSymbol) {
            is CfirFieldVariableSymbol -> {
                val field = resolvedSymbol.takeIf { it.isBound }?.cfir
                if (field != null && isImmutableFieldAssignmentForbidden(field)) {
                    AssignmentTarget.ImmutableValue
                } else {
                    AssignmentTarget.Assignable
                }
            }

            is CfirVariableSymbol<*> -> {
                val variable = resolvedSymbol.takeIf { it.isBound }?.cfir
                if (variable != null && !variable.isVar) AssignmentTarget.ImmutableValue else AssignmentTarget.Assignable
            }

            is CfirPropertySymbol -> {
                val property = resolvedSymbol.takeIf { it.isBound }?.cfir as? CfirProperty
                if (property != null && property.setter == null) AssignmentTarget.ImmutableValue else AssignmentTarget.Assignable
            }

            is CfirFunctionSymbol<*> -> AssignmentTarget.NonAssignableName(referenceNameOrFallback())
            is CfirClassLikeSymbol<*> -> AssignmentTarget.NonAssignableName(referenceNameOrFallback())
            null -> null
            else -> null
        }
    }

    private fun CfirQualifiedAccessExpression.resolvedAssignableSymbolOrNull(): CfirBasedSymbol<*>? {
        return when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
            else -> null
        }
    }

    private fun CfirQualifiedAccessExpression.referenceNameOrFallback(): Name {
        return (calleeReference as? CfirNamedReference)?.name ?: Name.ERROR_NAME
    }

    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.isImmutableFieldAssignmentForbidden(field: CfirFieldVariable): Boolean {
        if (field.isVar) return false
        val inConstructor = context.findClosestDeclaration<CfirConstructor>() != null
        if (!inConstructor || explicitReceiver != null) return true
        return field.initializer != null || field.status.isCommon
    }

    private sealed interface AssignmentTarget {
        data object Assignable : AssignmentTarget
        data object ImmutableValue : AssignmentTarget
        data class NonAssignableName(val name: Name) : AssignmentTarget
    }
}
