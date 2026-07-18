package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.explicitLambdaParameterType
import org.cangnova.cangjie.cfir.analysis.checkers.functionTypeForLambdaShape
import org.cangnova.cangjie.cfir.analysis.checkers.hasOmittedLambdaParameterType
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.resolve.calls.isLambdaTargetParameterSubtypeOfAnnotation
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement

/**
 * 普通变量 lambda 初始化器的目标函数类型检查。
 *
 * 变量声明的目标类型是 lambda header 形状诊断的语义 owner。checker 在变量声明阶段
 * 先根据声明类型检查 initializer 中的 lambda 参数列表，再交给 lambda 子树遍历提交
 * 参数源上的诊断，避免表达式阶段晚于参数遍历导致诊断无法稳定落入 inline 输出。
 */
object CfirVariableLambdaInitializerTypeMismatchChecker : CfirCallableDeclarationChecker() {
    /**
     * 检查变量 initializer 中 lambda 头部是否满足声明的函数类型。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirCallableDeclaration) {
        val variable = declaration as? CfirVariable ?: return
        val lambdaExpression = variable.initializer as? CfirAnonymousFunctionExpression ?: return
        val lambda = lambdaExpression.anonymousFunction
        if (!lambda.isLambda || !lambda.hasExplicitParameterList) return
        if (context.hasLambdaParameterShapeDiagnostic(lambda)) return

        val expectedFunctionType = variable.returnTypeRef.coneTypeOrNull
            .functionTypeForLambdaShape(context)
            ?: return

        if (lambda.reportVariableLambdaInitializerShapeDiagnostic(expectedFunctionType)) {
            context.recordLambdaParameterShapeDiagnostic(lambda)
        }
    }

    /**
     * 按目标函数类型检查 lambda 参数列表。
     *
     * 参数个数不匹配且源码存在省略参数时报告参数列表；完全显式的 lambda 则报告
     * 整个 lambda 不满足目标函数类型，这样普通变量 initializer 与调用实参保持一致。
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
            if (isLambdaTargetParameterSubtypeOfAnnotation(context.session, expectedType, actualType)) {
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
}
