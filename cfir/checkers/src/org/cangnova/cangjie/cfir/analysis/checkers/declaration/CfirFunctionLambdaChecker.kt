package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithSingleCandidate
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType

/**
 * Lambda 表达式参数类型注解检查器
 *
 * 对齐 C++ TypeCheckExpr/LambdaExpr.cpp 中的参数类型推断检查：
 * - lambda 表达式的参数必须有类型注解（当无法从上下文推断时）
 *
 * 注册为 anonymousFunctionCheckers
 */
object CfirLambdaParameterTypeChecker : CfirAnonymousFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirAnonymousFunction) {
        if (!declaration.isLambda) return
        if (!declaration.hasExplicitParameterList) return

        // 如果已有匹配的函数类型（由上下文推断），参数类型会被填充，不需要显式注解
        if (declaration.matchingParameterFunctionType != null) return
        if (declaration.isUnmappedCallArgumentLambda()) return

        for (param in declaration.valueParameters) {
            checkParameterTypeAnnotation(param)
        }
    }

    /**
     * 当前 lambda 作为调用实参出现，但外层候选的参数映射没有把它绑定到任何形参时，
     * 它没有可用的函数类型上下文。该场景的主错误属于调用参数映射阶段，
     * 不能再把同一个 lambda 当成独立无上下文 lambda 重复报告参数类型注解错误。
     */
    context(context: CheckerContext)
    private fun CfirAnonymousFunction.isUnmappedCallArgumentLambda(): Boolean {
        val containingCall = context.callsOrAssignments
            .asReversed()
            .filterIsInstance<CfirFunctionCall>()
            .firstOrNull { call ->
                call.argumentList.arguments.any { argument ->
                    argument.isExpressionForAnonymousFunction(this)
                }
            }
            ?: return false

        val candidate = containingCall.singleDiagnosticCandidateOrNull() ?: return false
        if (!candidate.argumentMappingInitialized) return false

        return candidate.argumentMapping.keys.none { atom ->
            atom.expression.isExpressionForAnonymousFunction(this)
        }
    }

    private fun CfirFunctionCall.singleDiagnosticCandidateOrNull(): AbstractCallCandidate<*>? {
        val diagnostic = (calleeReference as? CfirDiagnosticHolder)?.diagnostic
        return (diagnostic as? ConeDiagnosticWithSingleCandidate)?.candidate
    }

    private fun CfirExpression.isExpressionForAnonymousFunction(
        anonymousFunction: CfirAnonymousFunction,
    ): Boolean {
        val expression = this as? CfirAnonymousFunctionExpression ?: return false
        return expression.anonymousFunction === anonymousFunction ||
            expression.anonymousFunction.symbol == anonymousFunction.symbol
    }

    /**
     * 对齐 C++ IsAnyParamTypeOmitted:
     * 当 lambda 参数没有类型注解且类型未被推断时报错。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkParameterTypeAnnotation(param: CfirValueParameter) {
        val typeRef = param.returnTypeRef
        if (typeRef is CfirImplicitTypeRef) {
            reporter.reportOn(
                source = param.source,
                factory = CfirErrors.LAMBDA_MUST_HAVE_TYPE_ANNOTATION,
            )
            return
        }
        if (typeRef is CfirResolvedTypeRef && typeRef.coneType is ConeErrorType) {
            reporter.reportOn(
                source = param.source,
                factory = CfirErrors.LAMBDA_MUST_HAVE_TYPE_ANNOTATION,
            )
        }
    }
}
