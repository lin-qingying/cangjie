package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostic.ArgumentPassedTwice
import org.cangnova.cangjie.cfir.diagnostic.MixingNamedAndPositionalArguments
import org.cangnova.cangjie.cfir.diagnostic.NamedArgumentsNotAllowed
import org.cangnova.cangjie.cfir.diagnostic.NamedParameterNotFound
import org.cangnova.cangjie.cfir.diagnostic.NeedNamedArgument
import org.cangnova.cangjie.cfir.diagnostic.NoValueForParameter
import org.cangnova.cangjie.cfir.diagnostic.TooManyArguments
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.text

/**
 * 参数映射阶段负责把“调用站点实参”绑定到“可调用声明形参”。
 *
 * 这里优先对齐 Kotlin FIR 的分层方式：
 * - 参数个数、named argument、位置参数顺序等绑定级错误在本阶段产出；
 * - 具体类型兼容性交给 `CfirCheckArguments`。
 *
 * 这样可以避免把 call/constructor 语义错误继续退化成通用 unresolved 或 type mismatch。
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
        argumentAtoms.forEach { argument ->
            sink.reportDiagnostic(TooManyArguments(argument.expression, candidate.callInfo.name))
        }
    }

    context(sink: CheckerSink)
    private fun mapCallableArguments(
        candidate: Candidate,
        argumentAtoms: List<ConeResolutionAtom>,
    ) {
        val parameters = candidate.declaredParametersForMapping()
        val argumentInfos = argumentAtoms.map(::CallArgumentInfo)
        val argumentMapping = LinkedHashMap<ConeResolutionAtom, CfirValueParameter>(argumentAtoms.size)
        val usedParameters = linkedSetOf<CfirValueParameter>()

        if (candidate.symbol.takeIf { it.isBound }?.cfir is CfirVariable) {
            candidate.initializeArgumentMapping(argumentAtoms, argumentMapping)
            argumentInfos.filter { it.name != null }.forEach { argument ->
                sink.reportDiagnostic(
                    NamedArgumentsNotAllowed(argument.atom.expression, "variable function call")
                )
            }
            return
        }

        var nextPositionalIndex = 0
        var seenNamedArgument = false
        var hasUnmappedNamedArgumentError = false

        for (argument in argumentInfos) {
            val argumentName = argument.name
            if (argumentName != null) {
                seenNamedArgument = true
                val parameter = parameters.firstOrNull { it.name == argumentName }
                if (parameter == null) {
                    sink.reportDiagnostic(NamedParameterNotFound(argument.atom.expression, argumentName))
                    hasUnmappedNamedArgumentError = true
                    continue
                }
                if (!parameter.isNamed) {
                    sink.reportDiagnostic(
                        NamedArgumentsNotAllowed(argument.atom.expression, "parameter '${parameter.name.asString()}'")
                    )
                    hasUnmappedNamedArgumentError = true
                    continue
                }
                if (!usedParameters.add(parameter)) {
                    sink.reportDiagnostic(ArgumentPassedTwice(argument.atom.expression, parameter))
                    hasUnmappedNamedArgumentError = true
                    continue
                }
                argumentMapping[argument.atom] = parameter
                continue
            }

            // 对齐 Kotlin FIR 的参数绑定语义：
            // 命名参数可能提前绑定掉前面的形参，后续位置参数必须跳过这些已绑定项，
            // 否则会把“命名后的位置参数”错误地重新对到旧形参上，制造伪造的 missing / need-named 诊断。
            while (nextPositionalIndex < parameters.size && parameters[nextPositionalIndex] in usedParameters) {
                nextPositionalIndex += 1
            }

            val parameter = parameters.getOrNull(nextPositionalIndex)
            if (parameter == null) {
                sink.reportDiagnostic(TooManyArguments(argument.atom.expression, candidate.callInfo.name))
                continue
            }

            if (seenNamedArgument) {
                sink.reportDiagnostic(MixingNamedAndPositionalArguments(argument.atom.expression))
            }

            if (parameter.isNamed && !candidate.callInfo.origin.isNamedPrefixOptionalOrigin()) {
                sink.reportDiagnostic(NeedNamedArgument(argument.atom.expression, parameter))
            }

            usedParameters.add(parameter)
            argumentMapping[argument.atom] = parameter
            nextPositionalIndex += 1
        }

        if (!hasUnmappedNamedArgumentError) {
            parameters
                .filterNot { it in usedParameters }
                .filter { it.defaultValue == null }
                .forEach { parameter ->
                    sink.reportDiagnostic(NoValueForParameter(parameter))
                }
        }

        candidate.initializeArgumentMapping(argumentAtoms, argumentMapping)
        candidate.numDefaults = parameters.count { it !in usedParameters && it.defaultValue != null }
    }
}

private data class CallArgumentInfo(
    val atom: ConeResolutionAtom,
    val name: Name? = atom.expression.argumentNameOrNull(),
)

private fun CfirExpression.argumentNameOrNull(): Name? {
    val source = valueArgumentSourceOrNull() ?: return null
    val psiArgument = source?.psi as? CjValueArgument
    if (psiArgument != null) {
        return psiArgument.getArgumentName()?.asName
    }

    // LightTree 路径没有 PSI，可用的稳定信息只有整段 value-argument source。
    // 这里仅对“显式保留下来的 value-argument 包装层”做文本恢复，避免把普通表达式误判成命名参数。
    val rawText = source?.text?.toString()?.trim().orEmpty()
    val separatorIndex = rawText.indexOf(':')
    if (separatorIndex <= 0) return null

    val possibleName = rawText.substring(0, separatorIndex).trim()
    return Name.identifierIfValid(possibleName)
}

private fun CfirExpression.valueArgumentSourceOrNull(): CjSourceElement? {
    return when (this) {
        is CfirBlock -> source?.takeIf { statements.size == 1 }
        else -> source?.takeIf { it.psi is CjValueArgument }
    }
}

private fun CfirFunctionCallOrigin.isNamedPrefixOptionalOrigin(): Boolean {
    return this == CfirFunctionCallOrigin.Operator
}
