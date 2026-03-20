package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.resolve.calls.candidate.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTupleType

/**
 * 参数类型检查阶段：逐个验证实参与形参的类型兼容性。
 */
object CfirCheckArguments : CfirResolutionStage() {

    override fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    ) {
        val parameterTypes = extractParameterTypes(candidate.symbol) ?: return
        val arguments = candidate.callInfo.arguments

        for ((argIndex, paramIndex) in candidate.argumentMapping) {
            if (sink.shouldStop) return

            val argument = arguments.getOrNull(argIndex) ?: continue
            val argType = argument.coneTypeOrNull ?: continue
            val paramType = parameterTypes.getOrNull(paramIndex) ?: continue
            val substitutedParamType = candidate.substitutor.substituteOrSelf(paramType)

            if (argType is ConeErrorType || substitutedParamType is ConeErrorType) continue
            if (!context.subtypeChecker.isSubtypeOf(argType, substitutedParamType)) {
                sink.reportDiagnostic(
                    ArgumentTypeMismatch(
                        expectedType = substitutedParamType,
                        actualType = argType,
                        parameterIndex = paramIndex,
                    ),
                )
            }
        }
    }

    private fun extractParameterTypes(symbol: CfirCallableSymbol<*>): List<ConeCangjieType>? {
        if (!symbol.isBound) return null
        return when (val decl = symbol.cfir) {
            is CfirFunction -> decl.valueParameters.mapNotNull {
                (it.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            }
            is CfirConstructor -> decl.valueParameters.mapNotNull {
                (it.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            }
            is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor -> {
                val payloadType = (decl.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return emptyList()
                when (payloadType) {
                    is ConeTupleType -> payloadType.elementTypes
                    is ConeErrorType -> emptyList()
                    else -> listOf(payloadType)
                }
            }
            else -> null
        }
    }
}

