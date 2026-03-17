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
 * 鍙傛暟绫诲瀷妫€鏌ラ樁娈碉細閫愪釜楠岃瘉瀹炲弬绫诲瀷涓庡舰鍙傜被鍨嬬殑鍏煎鎬с€? *
 * 閬嶅巻 argumentMapping 涓殑姣忓 (瀹炲弬, 褰㈠弬)锛? * 浣跨敤 ConeSubtypeChecker 鍒ゆ柇瀹炲弬绫诲瀷鏄惁涓哄舰鍙傜被鍨嬬殑瀛愮被鍨嬨€? * IdealInt/IdealFloat 鍏煎鎬х敱 ConeSubtypeChecker 鑷姩澶勭悊銆? *
 * 瀵归綈 K2 CheckArguments銆? */
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

            // 瀵瑰舰鍙傜被鍨嬪簲鐢ㄥ€欓€夌殑绫诲瀷鏇挎崲鍣?
            val substitutedParamType = candidate.substitutor.substituteOrSelf(paramType)

            // 閿欒绫诲瀷涓嶅仛妫€鏌ワ紙闈欓粯浼犳挱锛?
            if (argType is ConeErrorType || substitutedParamType is ConeErrorType) continue

            // 瀛愮被鍨嬫鏌?
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

    /** 浠庡€欓€夌鍙蜂腑鎻愬彇鍊煎弬鏁板垪琛?*/
    private fun extractValueParameters(symbol: org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>): List<CfirValueParameter>? {
        if (!symbol.isBound) return null
        return when (val decl = symbol.cfir) {
            is CfirFunction -> decl.valueParameters
            is CfirConstructor -> decl.valueParameters
            else -> null
        }
    }

    /** 浠庡€煎弬鏁板０鏄庝腑鎻愬彇鍙傛暟绫诲瀷 */
    private fun extractParameterType(parameter: CfirValueParameter): ConeCangjieType? {
        val typeRef = parameter.returnTypeRef
        return (typeRef as? CfirResolvedTypeRef)?.coneType
    }
}

