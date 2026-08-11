package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.diagnosticFactoryForReturnTypeMismatch
import org.cangnova.cangjie.cfir.analysis.checkers.hasUninferredOmittedLambdaParameterType
import org.cangnova.cangjie.cfir.analysis.checkers.isSubtypeForTypeMismatch
import org.cangnova.cangjie.cfir.analysis.checkers.lambdaExpectedFunctionType
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.analysis.diagnostics.specificTypeMismatchDiagnostic
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirErrorFunction

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.hasImplicitOrInferredReturnType
import org.cangnova.cangjie.cfir.diagnostic.ConeTypeMismatchError
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
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
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext

/**
 * 函数返回类型检查器。
 * 检查 `return expr` 中 `expr` 的类型是否为所在函数返回类型的子类型。
 */
object CfirReturnTypeMismatchChecker : CfirReturnExpressionChecker( ) {
    /**
     * 检查 `return` 表达式结果类型是否兼容目标函数返回类型。
     *
     * 错误函数、main、macro、隐式返回和 lambda 的 Unit 兼容场景会提前跳过；其余类型不兼容
     * 通过专门诊断或通用返回类型 mismatch 诊断报告。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirReturnExpression) {
        val result = expression.result ?: return
        val source = result.source as? AbstractCjSourceElement ?: return
        val containingFunction = expression.target.labeledElement
        if (
            containingFunction is CfirNamedFunction &&
            containingFunction.hasImplicitOrInferredReturnType()
        ) {
            return
        }
        val rawActualType = result.coneTypeOrNull ?: return
        val returnTypeMismatch = result.returnTypeMismatchError(rawActualType)
        val actualType = returnTypeMismatch?.actualType ?: rawActualType
        if (actualType is ConeErrorType) return
        if (result is CfirMatchExpression && result.branches.any { branch ->
                branch.body.coneTypeOrNull is ConeErrorType || branch.coneTypeOrNull is ConeErrorType
            }
        ) {
            return
        }
        if (expression.source?.kind == CjFakeSourceElementKind.DelegatedPropertyAccessor) return

        if (
            containingFunction is CfirAnonymousFunction &&
            containingFunction.hasUninferredOmittedLambdaParameterType() &&
            !containingFunction.hasLambdaShapeDiagnosticForReturnTypeCheck(context)
        ) {
            return
        }

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

        val expectedType = returnTypeMismatch?.expectedType ?: when (containingFunction) {
            is CfirAnonymousFunction ->
                containingFunction.lambdaExpectedFunctionType(context)?.returnType
                    ?: (containingFunction.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                    ?: return
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

        if (checkTargetTypedExpression(result, expectedType).isHandled) return

        specificTypeMismatchDiagnostic(
            source = source,
            expectedType = expectedType,
            actualType = actualType,
            expression = result,
            session = context.session,
        )?.let { diagnostic ->
            reporter.report(diagnostic, context)
            return
        }

        if (!isSubtypeForTypeMismatch(context.session, context.session.typeContext, actualType, expectedType)) {
            if (
                expectedType.rangeElementTypeOrNull() != null && actualType.rangeElementTypeOrNull() != null
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
                source, diagnosticFactoryForReturnTypeMismatch(context.session, expectedType),
                expectedType,
                actualType,
                false,
            )
        }
    }
}

/**
 * 提取 Range 类型的元素类型。
 *
 * class-like、struct 和 typealias 三种表示都可能承载标准库 Range。
 */
private fun org.cangnova.cangjie.cfir.types.ConeCangJieType?.rangeElementTypeOrNull(): org.cangnova.cangjie.cfir.types.ConeCangJieType? = when (this) {
    is ConeClassLikeType -> if (classId == StdlibClassIds.Range) typeArguments.singleOrNull()?.type else null
    is ConeStructType -> if (classId == StdlibClassIds.Range) typeArguments.singleOrNull()?.type else null
    is ConeTypeAliasType -> expandedType?.rangeElementTypeOrNull()
    else -> null
}

/**
 * 提取返回值根表达式携带的类型不匹配诊断。
 *
 * 构造器/调用失败时，类型不匹配可能挂在 callee reference 上，而不是表达式
 * `coneType` 的错误类型里。return checker 需要统一拿到该诊断，才能把根返回值
 * 分类为 RETURN_TYPE_MISMATCH。
 */
private fun CfirExpression.returnTypeMismatchError(
    rawActualType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
): ConeTypeMismatchError? {
    val typeDiagnostic = (rawActualType as? ConeErrorType)
        ?.diagnostic
        ?.unwrapUnreportedDuplicateDiagnostic() as? ConeTypeMismatchError
    if (typeDiagnostic != null) return typeDiagnostic

    return ((this as? CfirResolvable)?.calleeReference as? CfirDiagnosticHolder)
        ?.diagnostic
        ?.unwrapUnreportedDuplicateDiagnostic() as? ConeTypeMismatchError
}

private fun org.cangnova.cangjie.cfir.types.ConeDiagnostic.unwrapUnreportedDuplicateDiagnostic(): org.cangnova.cangjie.cfir.types.ConeDiagnostic =
    (this as? ConeUnreportedDuplicateDiagnostic)?.original ?: this

/**
 * Lambda 参数头部已有更具体形状错误时，显式 return 的类型检查不能被省略参数占位符屏蔽。
 */
private fun CfirAnonymousFunction.hasLambdaShapeDiagnosticForReturnTypeCheck(context: CheckerContext): Boolean {
    if (context.hasLambdaParameterShapeDiagnostic(this)) return true
    val expectedFunctionType = lambdaExpectedFunctionType(context)
        ?: return false
    return valueParameters.size != expectedFunctionType.parameterTypes.size
}
