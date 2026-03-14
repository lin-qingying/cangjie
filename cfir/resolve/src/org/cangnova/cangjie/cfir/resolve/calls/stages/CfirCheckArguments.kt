package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.calls.candidate.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeErrorType

/**
 * 参数类型检查阶段：逐个验证实参类型与形参类型的兼容性。
 *
 * 遍历 argumentMapping 中的每对 (实参, 形参)，
 * 使用 ConeSubtypeChecker 判断实参类型是否为形参类型的子类型。
 * IdealInt/IdealFloat 兼容性由 ConeSubtypeChecker 自动处理。
 *
 * 对齐 K2 CheckArguments。
 */
object CfirCheckArguments : CfirResolutionStage() {

    override fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    ) {
        val valueParameters = extractValueParameters(candidate.symbol) ?: return
        val arguments = candidate.callInfo.arguments
        val mapping = candidate.argumentMapping

        for ((argIndex, paramIndex) in mapping) {
            if (sink.shouldStop) return

            val argument = arguments.getOrNull(argIndex) ?: continue
            val parameter = valueParameters.getOrNull(paramIndex) ?: continue

            val argType = argument.coneTypeOrNull ?: continue
            val paramType = extractParameterType(parameter) ?: continue

            // 对形参类型应用候选的类型替换器
            val substitutedParamType = candidate.substitutor.substituteOrSelf(paramType)

            // 错误类型不做检查（静默传播）
            if (argType is ConeErrorType || substitutedParamType is ConeErrorType) continue

            // 子类型检查
            if (!context.subtypeChecker.isSubtypeOf(argType, substitutedParamType)) {
                sink.reportDiagnostic(
                    ArgumentTypeMismatch(
                        expectedType = substitutedParamType,
                        actualType = argType,
                        parameterIndex = paramIndex,
                    )
                )
            }
        }
    }

    /** 从候选符号中提取值参数列表 */
    private fun extractValueParameters(symbol: org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>): List<CfirValueParameter>? {
        if (!symbol.isBound) return null
        return when (val decl = symbol.cfir) {
            is CfirFunction -> decl.valueParameters
            is CfirConstructor -> decl.valueParameters
            else -> null
        }
    }

    /** 从值参数声明中提取参数类型 */
    private fun extractParameterType(parameter: CfirValueParameter): ConeCangjieType? {
        val typeRef = parameter.returnTypeRef
        return (typeRef as? CfirResolvedTypeRef)?.coneType
    }
}
