package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.functionTypeForLambdaShape
import org.cangnova.cangjie.cfir.analysis.checkers.isExpressionForAnonymousFunction
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.hasLambdaParameterShapeDiagnostic
import org.cangnova.cangjie.cfir.declarations.isLambdaParameterTypeOmitted
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 普通变量 lambda 初始化器的目标函数类型检查。
 *
 * 字段变量、模式变量和值参数默认值已有各自的 initializer mismatch checker；
 * 普通 `let/var name: (..) -> .. = { ... }` 没有专用声明分发表，导致 lambda 头部
 * 的显式参数类型不兼容只能依赖匿名函数 checker 从父上下文恢复。这里在表达式层
 * 找回承载它的普通变量声明，并按官方 `ChkLamParamTys` 检查 lambda 头部。
 */
object CfirVariableLambdaInitializerTypeMismatchChecker : CfirBasicExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        val lambdaExpression = expression as? CfirAnonymousFunctionExpression ?: return
        val lambda = lambdaExpression.anonymousFunction
        if (!lambda.isLambda || !lambda.hasExplicitParameterList) return
        if (lambda.hasLambdaParameterShapeDiagnostic == true) return

        val variable = context.containingDeclarations
            .asReversed()
            .filterIsInstance<CfirVariable>()
            .firstOrNull { variable ->
                variable !is CfirFieldVariable &&
                        variable !is CfirPatternVariable &&
                        variable.initializer?.isExpressionForAnonymousFunction(lambda) == true
            }
            ?: return

        val expectedFunctionType = variable.returnTypeRef.coneTypeOrNull
            .functionTypeForLambdaShape(context)
            ?: return

        if (lambda.reportVariableLambdaInitializerShapeDiagnostic(expectedFunctionType)) {
            lambda.hasLambdaParameterShapeDiagnostic = true
        }
    }

    /**
     * 按目标函数类型检查 lambda 参数列表。
     *
     * 参数个数不匹配且源码存在省略参数时报告参数列表；完全显式的 lambda 则报告
     * 整个 lambda 的函数类型不兼容，这样普通变量 initializer 与调用实参保持一致。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirAnonymousFunction.reportVariableLambdaInitializerShapeDiagnostic(
        expectedFunctionType: ConeFunctionType,
    ): Boolean {
        val hasOmittedParameterType = valueParameters.any { it.hasOmittedLambdaParameterType() }

        if (valueParameters.size != expectedFunctionType.parameterTypes.size) {
            if (hasOmittedParameterType) {
                reporter.reportOn(
                    source = lambdaParameterListSource() ?: source,
                    factory = CfirErrors.PARAM_COUNT_MISMATCH,
                    a = expectedFunctionType.parameterTypes.size,
                    b = valueParameters.size,
                )
            } else {
                reportWholeLambdaMismatch(expectedFunctionType)
            }
            return true
        }

        valueParameters.forEachIndexed { index, parameter ->
            val actualType = parameter.explicitLambdaParameterType() ?: return@forEachIndexed
            val expectedType = expectedFunctionType.parameterTypes.getOrNull(index) ?: return@forEachIndexed
            if (actualType is ConeErrorType || expectedType is ConeErrorType) return@forEachIndexed
            if (isCompatibleExplicitLambdaParameterType(expectedType, actualType)) return@forEachIndexed

            if (hasOmittedParameterType) {
                reporter.reportOn(
                    source = parameter.source,
                    factory = CfirErrors.TYPE_MISMATCH,
                    a = expectedType,
                    b = actualType,
                    c = false,
                )
            } else {
                reportWholeLambdaMismatch(expectedFunctionType)
            }
            return true
        }

        return false
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirAnonymousFunction.reportWholeLambdaMismatch(expectedFunctionType: ConeFunctionType) {
        reporter.reportOn(
            source = source,
            factory = CfirErrors.TYPE_MISMATCH,
            a = expectedFunctionType,
            b = actualLambdaFunctionType(expectedFunctionType),
            c = false,
        )
    }

    /** 源码中是否省略了 lambda 参数类型。 */
    private fun CfirValueParameter.hasOmittedLambdaParameterType(): Boolean =
        isLambdaParameterTypeOmitted == true ||
                returnTypeRef is CfirImplicitTypeRef ||
                returnTypeRef.source?.kind == CjFakeSourceElementKind.ImplicitReturnTypeOfLambdaValueParameter

    /** 取得源码显式写出的 lambda 参数类型。 */
    private fun CfirValueParameter.explicitLambdaParameterType(): ConeCangJieType? {
        if (hasOmittedLambdaParameterType()) return null
        val resolvedTypeRef = returnTypeRef as? CfirResolvedTypeRef ?: return null
        val delegatedResolvedTypeRef = resolvedTypeRef.delegatedTypeRef as? CfirResolvedTypeRef
        return delegatedResolvedTypeRef?.coneType ?: resolvedTypeRef.coneType
    }

    /** 构造用于 lambda 整体类型不匹配的实际函数类型。 */
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
     * Lambda 参数类型按函数参数逆变规则检查，并关闭隐式装箱。
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
}
