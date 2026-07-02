package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.firstOmittedLambdaParameterForInferenceFailure
import org.cangnova.cangjie.cfir.analysis.checkers.lambdaExpectedFunctionType
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.hasLambdaParameterShapeDiagnostic
import org.cangnova.cangjie.cfir.declarations.isLambdaParameterTypeOmitted
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
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * Lambda 表达式参数类型注解检查器
 *
 * 对齐 C++ TypeCheckExpr/LambdaExpr.cpp 中的参数类型推断检查：
 * - lambda 表达式的参数必须有类型注解（当无法从上下文推断时）
 *
 * 注册为 anonymousFunctionCheckers
 */
object CfirLambdaParameterTypeChecker : CfirAnonymousFunctionChecker() {
    /**
     * 检查显式参数列表 lambda 中未能从上下文推断出的参数类型。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirAnonymousFunction) {
        if (!declaration.isLambda) return
        if (!declaration.hasExplicitParameterList) return
        if (declaration.hasLambdaParameterShapeDiagnostic == true) return

        if (declaration.reportLambdaParameterShapeDiagnostic()) {
            declaration.hasLambdaParameterShapeDiagnostic = true
            return
        }

        val parameterToReport = declaration.firstOmittedLambdaParameterForInferenceFailure()

        // 如果已有匹配的函数类型（由上下文推断）且参数类型已被填充，不需要显式注解。
        if (declaration.matchingParameterFunctionType != null && parameterToReport == null) return
        if (declaration.hasLambdaParameterShapeDiagnostic == true) return
        if (declaration.isUnmappedCallArgumentLambda()) return

        parameterToReport?.reportLambdaParameterTypeAnnotation()
    }

    /**
     * 报告 lambda 头部与目标函数类型之间的最终形状错误。
     *
     * resolve 阶段可能为了继续分析 body 而把目标参数类型写回 lambda 参数；
     * 因此这里要通过 resolved type ref 的 delegated 原型取回源码显式类型，再按官方
     * `ChkLamParamTys` 规则决定报告参数列表、参数本身还是整个 lambda。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirAnonymousFunction.reportLambdaParameterShapeDiagnostic(): Boolean {
        val expectedFunctionType = lambdaExpectedFunctionType(context)
            ?: return false
        val hasOmittedParameterType = valueParameters.any { it.hasOmittedLambdaParameterType() }

        if (valueParameters.size != expectedFunctionType.parameterTypes.size) {
            if (!hasOmittedParameterType) return false
            reporter.reportOn(
                source = lambdaParameterListSource() ?: source,
                factory = CfirErrors.PARAM_COUNT_MISMATCH,
                a = expectedFunctionType.parameterTypes.size,
                b = valueParameters.size,
            )
            return true
        }

        valueParameters.forEachIndexed { index, parameter ->
            val actualType = parameter.explicitLambdaParameterType() ?: return@forEachIndexed
            val expectedType = expectedFunctionType.parameterTypes.getOrNull(index) ?: return@forEachIndexed
            if (actualType is ConeErrorType || expectedType is ConeErrorType) return@forEachIndexed
            if (isCompatibleExplicitLambdaParameterType(expectedType, actualType)) {
                return@forEachIndexed
            }

            if (hasOmittedParameterType) {
                reporter.reportOn(
                    source = parameter.source,
                    factory = CfirErrors.TYPE_MISMATCH,
                    a = expectedType,
                    b = actualType,
                    c = false,
                )
            } else {
                reporter.reportOn(
                    source = source,
                    factory = CfirErrors.TYPE_MISMATCH,
                    a = expectedFunctionType,
                    b = actualLambdaFunctionType(expectedFunctionType),
                    c = false,
                )
            }
            return true
        }

        return false
    }

    /** 源码中是否省略了 lambda 参数类型。 */
    private fun CfirValueParameter.hasOmittedLambdaParameterType(): Boolean =
        isLambdaParameterTypeOmitted == true ||
                returnTypeRef is CfirImplicitTypeRef ||
                returnTypeRef.source?.kind == CjFakeSourceElementKind.ImplicitReturnTypeOfLambdaValueParameter

    /**
     * 取得源码显式写出的 lambda 参数类型。
     */
    private fun CfirValueParameter.explicitLambdaParameterType(): ConeCangJieType? {
        if (hasOmittedLambdaParameterType()) return null
        val resolvedTypeRef = returnTypeRef as? CfirResolvedTypeRef ?: return null
        val delegatedResolvedTypeRef = resolvedTypeRef.delegatedTypeRef as? CfirResolvedTypeRef
        return delegatedResolvedTypeRef?.coneType ?: resolvedTypeRef.coneType
    }

    /**
     * Lambda 参数类型按函数参数逆变规则检查，并关闭隐式装箱。
     *
     * 官方 `ChkLamParamTys` 使用 `IsSubtype(paramTy, annotatedTy, false, false)`；
     * 这里通过函数类型子类型检查复用同一条“参数位置不装箱”的框架规则，
     * 避免普通表达式 mismatch 规则把 `Int64` 放宽到 `ToString`。
     */
    context(context: CheckerContext)
    private fun isCompatibleExplicitLambdaParameterType(
        expectedType: ConeCangJieType,
        actualType: ConeCangJieType,
    ): Boolean {
        if (expectedType is ConeErrorType || actualType is ConeErrorType) return true
        val expectedFunctionType = ConeFunctionType(
            parameterTypes = listOf(expectedType),
            returnType = expectedType,
        )
        val actualFunctionType = ConeFunctionType(
            parameterTypes = listOf(actualType),
            returnType = expectedType,
        )
        return AbstractTypeChecker.isSubtypeOf(
            context.session.typeContext,
            actualFunctionType,
            expectedFunctionType,
        )
    }

    /**
     * 构造用于 lambda 整体类型不匹配的实际函数类型。
     */
    private fun CfirAnonymousFunction.actualLambdaFunctionType(expectedFunctionType: ConeFunctionType): ConeFunctionType {
        val parameterTypes = valueParameters.mapIndexed { index, parameter ->
            parameter.explicitLambdaParameterType()
                ?: expectedFunctionType.parameterTypes.getOrNull(index)
                ?: expectedFunctionType.returnType
        }
        val returnType = (returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: expectedFunctionType.returnType
        return ConeFunctionType(
            parameterTypes = parameterTypes,
            returnType = returnType,
            isCFunc = expectedFunctionType.isCFunc,
            isClosureType = expectedFunctionType.isClosureType,
            hasVariableLenArg = expectedFunctionType.hasVariableLenArg,
            attributes = expectedFunctionType.attributes,
        )
    }

    /** Lambda 参数个数错误覆盖完整参数列表。 */
    private fun CfirAnonymousFunction.lambdaParameterListSource(): AbstractCjSourceElement? {
        val parameterSources = valueParameters.mapNotNull { it.source }
        val first = parameterSources.firstOrNull() ?: return null
        val last = parameterSources.last()
        return CjOffsetsOnlySourceElement(first.startOffset, last.endOffset)
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

    /**
     * 从错误诊断中提取唯一候选。
     */
    private fun CfirFunctionCall.singleDiagnosticCandidateOrNull(): AbstractCallCandidate<*>? {
        val diagnostic = (calleeReference as? CfirDiagnosticHolder)?.diagnostic
        return (diagnostic as? ConeDiagnosticWithSingleCandidate)?.candidate
    }

    /**
     * 判断表达式是否承载指定匿名函数。
     */
    private fun CfirExpression.isExpressionForAnonymousFunction(
        anonymousFunction: CfirAnonymousFunction,
    ): Boolean {
        val expression = this as? CfirAnonymousFunctionExpression ?: return false
        return expression.anonymousFunction === anonymousFunction ||
            expression.anonymousFunction.symbol == anonymousFunction.symbol
    }

    /**
     * 官方 DiagInferParamTyFail 只诊断第一个仍无法解出的省略参数。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirValueParameter.reportLambdaParameterTypeAnnotation() {
        reporter.reportOn(
            source = source,
            factory = CfirErrors.LAMBDA_MUST_HAVE_TYPE_ANNOTATION,
        )
    }
}
