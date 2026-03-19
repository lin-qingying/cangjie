package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.analysis.checkers.CfirTypeCheckUtils
import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLambdaExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 函数参数类型检查器。
 * 检查函数调用中每个实参类型是否为对应形参类型的子类型。
 */
object CfirArgumentTypeMismatchChecker : CfirFunctionCallChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val resolvedRef = expression.calleeReference as? CfirResolvedNamedReference ?: return
        val functionSymbol = resolvedRef.resolvedSymbol as? CfirFunctionSymbol ?: return
        val function = functionSymbol.cfir
        val parameters = function.valueParameters
        val arguments = expression.arguments

        for (i in arguments.indices) {
            if (i >= parameters.size) break
            val argument = arguments[i]
            val argSource = argument.source as? AbstractCjSourceElement ?: continue
            val actualType = argument.coneTypeOrNull ?: continue
            val expectedType = (resolvedRef as? CfirResolvedAppliedCallableReference)
                ?.substitutedParameterTypes
                ?.getOrNull(i)
                ?: (parameters[i].returnTypeRef as? CfirResolvedTypeRef)?.coneType
                ?: continue

            if (CfirTypeCheckUtils.isSubtypeOf(actualType, expectedType)) continue

            val forceArgumentMismatch = context.languageVersionSettings.supportsFeature(
                LanguageFeature.LambdaReturnTypeMismatchAsArgumentTypeMismatch,
            )

            if (!forceArgumentMismatch && argument is CfirLambdaExpression && expectedType is ConeFuncType) {
                val lambdaBody = argument.anonymousFunction.body
                val actualReturnType = lambdaBody?.coneTypeOrNull
                    ?: (argument.coneTypeOrNull as? ConeFuncType)?.returnType

                if (actualReturnType != null &&
                    !CfirTypeCheckUtils.isSubtypeOf(actualReturnType, expectedType.returnType)
                ) {
                    val returnSource = (lambdaBody?.source ?: argSource) as? AbstractCjSourceElement ?: argSource
                    reporter.reportOn(
                        returnSource,
                        CfirErrors.RETURN_TYPE_MISMATCH,
                        expectedType.returnType,
                        actualReturnType,
                        false,
                    )
                    continue
                }
            }

            reporter.reportOn(
                argSource,
                CfirErrors.ARGUMENT_TYPE_MISMATCH,
                expectedType,
                actualType,
                false,
            )
        }
    }
}
