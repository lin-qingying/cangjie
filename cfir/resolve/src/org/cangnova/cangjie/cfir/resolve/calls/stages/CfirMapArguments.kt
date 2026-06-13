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
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.cangjieVariadicParameterForMapping
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
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
        val variadicParameter = candidate.cangjieVariadicParameterForMapping(parameters)
        val argumentInfos = argumentAtoms.map(::CallArgumentInfo)
        val nonTrailingArguments = argumentInfos.filterNot { it.isTrailingLambda }
        val trailingLambdaArguments = argumentInfos.filter { it.isTrailingLambda }

        val regularResult = createCallableArgumentMapping(
            candidate = candidate,
            argumentAtoms = argumentAtoms,
            parameters = parameters,
            nonTrailingArguments = nonTrailingArguments,
            trailingLambdaArguments = trailingLambdaArguments,
            variadicParameter = null,
        )
        val variadicResult = if (isPossibleCangjieVariadicCall(parameters, variadicParameter, nonTrailingArguments)) {
            createCallableArgumentMapping(
                candidate = candidate,
                argumentAtoms = argumentAtoms,
                parameters = parameters,
                nonTrailingArguments = nonTrailingArguments,
                trailingLambdaArguments = trailingLambdaArguments,
                variadicParameter = variadicParameter,
            )
        } else {
            null
        }

        val result = variadicResult ?: regularResult
        if (result.usesCangjieVariadic && regularResult.diagnostics.isNotEmpty()) {
            candidate.initializeCangjieVariadicRegularCallDiagnostics(regularResult.diagnostics)
        }
        candidate.initializeCangjieVariadicParameterForCall(result.variadicParameter)
        candidate.initializeArgumentMapping(argumentAtoms, result.argumentMapping)
        candidate.numDefaults = result.numDefaults
        result.diagnostics.forEach(sink::reportDiagnostic)
    }

    private fun createCallableArgumentMapping(
        candidate: Candidate,
        argumentAtoms: List<ConeResolutionAtom>,
        parameters: List<CfirValueParameter>,
        nonTrailingArguments: List<CallArgumentInfo>,
        trailingLambdaArguments: List<CallArgumentInfo>,
        variadicParameter: CfirValueParameter?,
    ): CallableArgumentMappingResult {
        val variadicParameterIndex = parameters.indexOf(variadicParameter)
        val argumentMapping = LinkedHashMap<ConeResolutionAtom, CfirValueParameter>(argumentAtoms.size)
        val usedParameters = linkedSetOf<CfirValueParameter>()
        val diagnostics = mutableListOf<ResolutionDiagnostic>()
        val isCallableValueCall = candidate.symbol.takeIf { it.isBound }?.cfir is CfirVariable

        var nextPositionalIndex = 0
        var seenNamedArgument = false
        var hasUnmappedNamedArgumentError = false

        for ((argumentIndex, argument) in nonTrailingArguments.withIndex()) {
            val argumentName = argument.name
            if (argumentName != null) {
                seenNamedArgument = true
                if (isCallableValueCall) {
                    diagnostics += NamedArgumentsNotAllowed(argument.atom.expression, "variable function call")
                    hasUnmappedNamedArgumentError = true
                    continue
                }

                val parameter = parameters.firstOrNull { it.name == argumentName }
                if (parameter == null) {
                    diagnostics += NamedParameterNotFound(argument.atom.expression, argumentName)
                    hasUnmappedNamedArgumentError = true
                    continue
                }
                if (!parameter.isNamed) {
                    diagnostics +=
                        NamedArgumentsNotAllowed(argument.atom.expression, "parameter '${parameter.name.asString()}'")
                    hasUnmappedNamedArgumentError = true
                    continue
                }
                if (!usedParameters.add(parameter)) {
                    diagnostics += ArgumentPassedTwice(argument.atom.expression, parameter)
                    hasUnmappedNamedArgumentError = true
                    continue
                }
                argumentMapping[argument.atom] = parameter
                continue
            }

            // 对齐 Kotlin FIR 的参数绑定语义：
            // 命名参数可能提前绑定掉前面的形参，后续位置参数必须跳过这些已绑定项，
            // 否则会把“命名后的位置参数”错误地重新对到旧形参上，制造伪造的 missing / need-named 诊断。
            while (
                nextPositionalIndex < parameters.size &&
                parameters[nextPositionalIndex] in usedParameters &&
                parameters[nextPositionalIndex] != variadicParameter
            ) {
                nextPositionalIndex += 1
            }

            operatorSetTrailingNamedParameterForPositionalArgument(
                candidate = candidate,
                parameters = parameters,
                variadicParameter = variadicParameter,
                variadicParameterIndex = variadicParameterIndex,
                usedParameters = usedParameters,
                nonTrailingArguments = nonTrailingArguments,
                argumentIndex = argumentIndex,
            )?.let { trailingNamedParameter ->
                usedParameters.add(trailingNamedParameter)
                argumentMapping[argument.atom] = trailingNamedParameter
                continue
            }

            if (
                variadicParameter != null &&
                variadicParameterIndex >= 0 &&
                nextPositionalIndex >= variadicParameterIndex &&
                !seenNamedArgument
            ) {
                usedParameters.add(variadicParameter)
                argumentMapping[argument.atom] = variadicParameter
                continue
            }

            val parameter = parameters.getOrNull(nextPositionalIndex)
            if (parameter == null) {
                diagnostics += TooManyArguments(argument.atom.expression, candidate.callInfo.name)
                continue
            }

            if (seenNamedArgument) {
                diagnostics += MixingNamedAndPositionalArguments(argument.atom.expression)
            }

            if (parameter.isNamed && !candidate.callInfo.origin.isNamedPrefixOptionalOrigin()) {
                diagnostics += NeedNamedArgument(argument.atom.expression, parameter)
            }

            usedParameters.add(parameter)
            argumentMapping[argument.atom] = parameter
            nextPositionalIndex += 1
        }

        mapTrailingLambdaArguments(
            candidate = candidate,
            parameters = parameters,
            variadicParameter = variadicParameter,
            trailingLambdaArguments = trailingLambdaArguments,
            usedParameters = usedParameters,
            argumentMapping = argumentMapping,
            diagnostics = diagnostics,
        )

        if (!hasUnmappedNamedArgumentError) {
            parameters
                .filterNot { it in usedParameters }
                .filter { it != variadicParameter }
                .filter { it.defaultValue == null }
                .forEach { parameter ->
                    diagnostics += NoValueForParameter(parameter)
                }
        }

        return CallableArgumentMappingResult(
            argumentMapping = argumentMapping,
            diagnostics = diagnostics,
            numDefaults = parameters.count { it != variadicParameter && it !in usedParameters && it.defaultValue != null },
            usesCangjieVariadic = variadicParameter != null,
            variadicParameter = variadicParameter,
        )
    }

    private fun operatorSetTrailingNamedParameterForPositionalArgument(
        candidate: Candidate,
        parameters: List<CfirValueParameter>,
        variadicParameter: CfirValueParameter?,
        variadicParameterIndex: Int,
        usedParameters: Set<CfirValueParameter>,
        nonTrailingArguments: List<CallArgumentInfo>,
        argumentIndex: Int,
    ): CfirValueParameter? {
        if (variadicParameter == null || variadicParameterIndex < 0) return null
        if (candidate.callInfo.origin != CfirFunctionCallOrigin.Operator) return null
        if (candidate.callInfo.name != OperatorNameConventions.SET) return null

        val trailingNamedParameters = parameters
            .drop(variadicParameterIndex + 1)
            .filter { it.isNamed && it.defaultValue == null && it !in usedParameters }
        if (trailingNamedParameters.isEmpty()) return null

        val remainingPositionalArguments = nonTrailingArguments
            .drop(argumentIndex)
            .takeWhile { it.name == null }
            .size

        return trailingNamedParameters.firstOrNull()
            ?.takeIf { remainingPositionalArguments <= trailingNamedParameters.size }
    }

    private fun mapTrailingLambdaArguments(
        candidate: Candidate,
        parameters: List<CfirValueParameter>,
        variadicParameter: CfirValueParameter?,
        trailingLambdaArguments: List<CallArgumentInfo>,
        usedParameters: MutableSet<CfirValueParameter>,
        argumentMapping: MutableMap<ConeResolutionAtom, CfirValueParameter>,
        diagnostics: MutableList<ResolutionDiagnostic>,
    ) {
        val externalArgument = trailingLambdaArguments.firstOrNull() ?: return
        val lastParameter = parameters.lastOrNull()
        if (lastParameter == null || lastParameter == variadicParameter || lastParameter in usedParameters) {
            if (diagnostics.none { it is TooManyArguments }) {
                diagnostics += TooManyArguments(externalArgument.atom.expression, candidate.callInfo.name)
            }
        } else {
            usedParameters.add(lastParameter)
            argumentMapping[externalArgument.atom] = lastParameter
        }

        trailingLambdaArguments.drop(1).forEach { argument ->
            if (diagnostics.none { it is TooManyArguments }) {
                diagnostics += TooManyArguments(argument.atom.expression, candidate.callInfo.name)
            }
        }
    }
}

private data class CallableArgumentMappingResult(
    val argumentMapping: LinkedHashMap<ConeResolutionAtom, CfirValueParameter>,
    val diagnostics: List<ResolutionDiagnostic>,
    val numDefaults: Int,
    val usesCangjieVariadic: Boolean,
    val variadicParameter: CfirValueParameter?,
)

private fun isPossibleCangjieVariadicCall(
    parameters: List<CfirValueParameter>,
    variadicParameter: CfirValueParameter?,
    nonTrailingArguments: List<CallArgumentInfo>,
): Boolean {
    if (variadicParameter == null) return false
    val positionalParameterSize = parameters.indexOfLast { !it.isNamed } + 1
    if (positionalParameterSize <= 0) return false
    val positionalArgumentSize = nonTrailingArguments.takeWhile { it.name == null }.size
    return positionalArgumentSize + 1 >= positionalParameterSize
}

private data class CallArgumentInfo(
    val atom: ConeResolutionAtom,
    val name: Name? = atom.expression.argumentNameOrNull(),
) {
    val isTrailingLambda: Boolean
        get() = (atom.expression as? CfirAnonymousFunctionExpression)?.isTrailingLambda == true
}

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
