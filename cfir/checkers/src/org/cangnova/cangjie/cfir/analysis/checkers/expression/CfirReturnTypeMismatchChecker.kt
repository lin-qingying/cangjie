package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.isSubtypeForTypeMismatch
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.analysis.diagnostics.specificTypeMismatchDiagnostic
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirErrorFunction

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirRangeExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext

/**
 * 函数返回类型检查器。
 * 检查 `return expr` 中 `expr` 的类型是否为所在函数返回类型的子类型。
 */
object CfirReturnTypeMismatchChecker : CfirReturnExpressionChecker( ) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirReturnExpression) {
        val result = expression.result ?: return
        val source = result.source as? AbstractCjSourceElement ?: return
        val actualType = result.coneTypeOrNull ?: return
        if (actualType is ConeErrorType) return
        if (result is CfirMatchExpression && result.branches.any { branch ->
                branch.body.coneTypeOrNull is ConeErrorType || branch.coneTypeOrNull is ConeErrorType
            }
        ) {
            return
        }
        if (expression.source?.kind == CjFakeSourceElementKind.DelegatedPropertyAccessor) return

        val containingFunction = expression.target.labeledElement

        if(containingFunction is CfirErrorFunction || containingFunction is CfirMainFunction || containingFunction is CfirMacroDeclaration) {
            return
        }
        val sourceKind = expression.source?.kind
        if (
            containingFunction.returnTypeRef is CfirImplicitTypeRef &&
            sourceKind != CjRealSourceElementKind
        ) {
            return
        }

        val expectedType = when (containingFunction) {
            is CfirConstructor -> ConePrimitiveType.UNIT
            else -> (containingFunction.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        }
        if (expectedType is ConeErrorType) return

        if (containingFunction is CfirAnonymousFunction) {
            if (sourceKind == CjFakeSourceElementKind.ImplicitReturn.FromLastStatement && expectedType.isUnit) {
                return
            }

            if (containingFunction.isLambda) {
                when {
                    expectedType.isUnit -> Unit
                    result.source?.kind is CjFakeSourceElementKind.ImplicitUnit -> Unit
                    else -> return
                }
            }
        }

        specificTypeMismatchDiagnostic(
            source = source,
            expectedType = expectedType,
            actualType = actualType,
            session = context.session,
        )?.let { diagnostic ->
            reporter.report(diagnostic, context)
            return
        }

        if (!isSubtypeForTypeMismatch(context.session, context.session.typeContext, actualType, expectedType)) {
            if (
                expectedType.rangeElementTypeOrNull() != null && actualType.rangeElementTypeOrNull() != null ||
                result is CfirFunctionCall && result.argumentList.arguments.any { it is CfirRangeExpression }
            ) {
                reporter.reportOn(
                    source, CfirErrors.TYPE_MISMATCH,
                    expectedType,
                    actualType,
                    false,
                )
                return
            }
            reporter.reportOn(
                source, CfirErrors.RETURN_TYPE_MISMATCH,
                expectedType,
                actualType,
                false,
            )
        }
    }
}

private fun org.cangnova.cangjie.cfir.types.ConeCangJieType?.rangeElementTypeOrNull(): org.cangnova.cangjie.cfir.types.ConeCangJieType? = when (this) {
    is ConeClassLikeType -> if (classId == StdlibClassIds.Range) typeArguments.singleOrNull()?.type else null
    is ConeStructType -> if (classId == StdlibClassIds.Range) typeArguments.singleOrNull()?.type else null
    is ConeTypeAliasType -> expandedType?.rangeElementTypeOrNull()
    else -> null
}
