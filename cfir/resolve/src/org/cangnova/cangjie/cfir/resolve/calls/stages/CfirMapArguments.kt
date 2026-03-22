package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirResolutionAtom
import org.cangnova.cangjie.cfir.diagnostic.WrongArgumentCount
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTupleType

/**
 * 参数映射阶段：把调用实参映射到函数形参。
 * 支持函数、类构造器和枚举构造器（枚举构造器参数来自其 payload 类型）。
 */
object CfirMapArguments : CfirResolutionStage() {

    override fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    ) {
        val atomArguments = candidate.callInfo.arguments.map { CfirResolutionAtom(it) }
        val mapping = LinkedHashMap<CfirResolutionAtom, org.cangnova.cangjie.cfir.declarations.CfirValueParameter>()
        candidate.initializeArgumentMapping(atomArguments, mapping)

        val callableSymbol = candidate.symbol as? CfirCallableSymbol<*> ?: return
        val totalParams = extractParameterCount(callableSymbol) ?: return
        val requiredParams = extractRequiredParameterCount(callableSymbol) ?: return
        val actualArgs = candidate.callInfo.arguments.size

        if (actualArgs < requiredParams || actualArgs > totalParams) {
            sink.reportDiagnostic(
                WrongArgumentCount(
                    expectedCount = if (requiredParams == totalParams) totalParams else requiredParams,
                    actualCount = actualArgs,
                ),
            )
            return
        }

        val parameterList = extractParameters(callableSymbol) ?: return
        for (i in 0 until actualArgs) {
            val atom = atomArguments.getOrNull(i) ?: continue
            val parameter = parameterList.getOrNull(i) ?: continue
            mapping[atom] = parameter
        }
        candidate.numDefaults = totalParams - actualArgs
    }

    private fun extractParameters(symbol: CfirCallableSymbol<*>): List<CfirValueParameter>? {
        if (!symbol.isBound) return null
        return when (val decl = symbol.cfir) {
            is CfirFunction -> decl.valueParameters
            is CfirConstructor -> decl.valueParameters
            is CfirEnumConstructor -> emptyList()
            else -> null
        }
    }

    private fun extractParameterCount(symbol: CfirCallableSymbol<*>): Int? {
        if (!symbol.isBound) return null
        return when (val decl = symbol.cfir) {
            is CfirFunction -> decl.valueParameters.size
            is CfirConstructor -> decl.valueParameters.size
            is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor -> enumPayloadArity(decl)
            else -> null
        }
    }

    private fun extractRequiredParameterCount(symbol: CfirCallableSymbol<*>): Int? {
        if (!symbol.isBound) return null
        return when (val decl = symbol.cfir) {
            is CfirFunction -> decl.valueParameters.count { it.defaultValue == null }
            is CfirConstructor -> decl.valueParameters.count { it.defaultValue == null }
            is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor -> enumPayloadArity(decl)
            else -> null
        }
    }

    private fun enumPayloadArity(decl: org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor): Int {
        val payloadType = (decl.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return 0
        return when (payloadType) {
            is ConeTupleType -> payloadType.elementTypes.size
            is ConeErrorType -> 0
            else -> 1
        }
    }
}
