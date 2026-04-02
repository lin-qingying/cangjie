package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostic.WrongArgumentCount
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink

/**
 * 参数映射阶段：把调用实参映射到函数形参。
 * 支持函数、类构造器和枚举构造器（枚举构造器参数来自其 payload 类型）。
 */
object CfirMapArguments : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        val argumentAtoms = candidate.callInfo.arguments.map(ConeResolutionAtom::createRawAtom)

        when (candidate.callInfo.callKind) {
            CallKind.NamedValueAccess -> {
                val declaration = candidate.symbol.takeIf { it.isBound }?.cfir
                if (declaration is CfirEnumConstructor) {
                    mapCallableArguments(candidate, argumentAtoms)
                } else {
                    mapVariableAccessArguments(candidate, argumentAtoms)
                }
            }
            CallKind.Function,
            CallKind.EnumConstructorCall,
            -> mapCallableArguments(candidate, argumentAtoms)
        }
    }

    context(sink: CheckerSink)
    private fun mapVariableAccessArguments(
        candidate: Candidate,
        argumentAtoms: List<ConeResolutionAtom>,
    ) {
        candidate.initializeArgumentMapping(argumentAtoms, linkedMapOf())
        if (argumentAtoms.isNotEmpty()) {
            sink.reportDiagnostic(WrongArgumentCount(expectedCount = 0, actualCount = argumentAtoms.size))
        }
    }

    context(sink: CheckerSink)
    private fun mapCallableArguments(
        candidate: Candidate,
        argumentAtoms: List<ConeResolutionAtom>,
    ) {
        val parameters = candidate.declaredParametersForMapping()
        val requiredParameters = parameters.count { it.defaultValue == null }

        if (argumentAtoms.size < requiredParameters || argumentAtoms.size > parameters.size) {
            candidate.initializeArgumentMapping(argumentAtoms, linkedMapOf())
            sink.reportDiagnostic(WrongArgumentCount(expectedCount = parameters.size, actualCount = argumentAtoms.size))
            return
        }

        val mapping = LinkedHashMap<ConeResolutionAtom, CfirValueParameter>(argumentAtoms.size)
        for ((argument, parameter) in argumentAtoms.zip(parameters)) {
            mapping[argument] = parameter
        }

        candidate.initializeArgumentMapping(argumentAtoms, mapping)
        candidate.numDefaults = parameters.size - argumentAtoms.size
    }

}
