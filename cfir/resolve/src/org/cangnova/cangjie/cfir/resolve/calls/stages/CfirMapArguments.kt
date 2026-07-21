package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostic.ArgumentPassedTwice
import org.cangnova.cangjie.cfir.diagnostic.MixingNamedAndPositionalArguments
import org.cangnova.cangjie.cfir.diagnostic.NamedArgumentsNotAllowed
import org.cangnova.cangjie.cfir.diagnostic.NamedParameterNotFound
import org.cangnova.cangjie.cfir.diagnostic.NeedNamedArgument
import org.cangnova.cangjie.cfir.diagnostic.NoValueForParameter
import org.cangnova.cangjie.cfir.diagnostic.TooManyArguments
import org.cangnova.cangjie.cfir.diagnostic.WrongNumberOfArguments
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.cangjieVariadicCallShapeOrNull
import org.cangnova.cangjie.cfir.resolve.calls.cangjieVariadicParameterForMapping
import org.cangnova.cangjie.cfir.resolve.calls.isSyntheticFunctionTypeInvoke
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldIfNeed
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.coneType
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
    /**
     * 对候选执行实参到形参的映射。
     */
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
            CallKind.DelegatingConstructorCall,
            -> mapCallableArguments(candidate, argumentAtoms)

            CallKind.EnumConstructorCall -> {
                // 裸 enum constructor 访问沿用 enum 专用 tower 查找，但不是零实参调用。
                // 只有真实的 CfirFunctionCall 才进入参数映射；否则缺参诊断会错误地
                // 与 ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS 级联。
                if (candidate.callInfo.callSite is CfirFunctionCall) {
                    mapCallableArguments(candidate, argumentAtoms)
                } else {
                    candidate.initializeArgumentMapping(emptyList(), linkedMapOf())
                }
            }
        }

        // 参数形态错误已经足以判定候选不可用；必须在进入类型实参和实参值检查前交还控制权。
        sink.yieldIfNeed()
    }

    /**
     * 命名值访问不允许携带调用实参，所有实参都报告过多实参。
     */
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

    /**
     * 映射函数、构造器和 enum constructor 的实参。
     */
    context(sink: CheckerSink)
    private fun mapCallableArguments(
        candidate: Candidate,
        argumentAtoms: List<ConeResolutionAtom>,
    ) {
        if (candidate.isBuiltinVArrayConstructorCandidate() && argumentAtoms.size != 1) {
            candidate.initializeArgumentMapping(argumentAtoms, linkedMapOf())
            candidate.numDefaults = 0
            return
        }

        val parameters = candidate.declaredParametersForMapping()
        val variadicParameter = candidate.cangjieVariadicParameterForMapping(parameters)
        val argumentInfos = argumentAtoms.map(::CallArgumentInfo)
        val nonTrailingArguments = argumentInfos.filterNot { it.isTrailingLambda }
        val trailingLambdaArguments = argumentInfos.filter { it.isTrailingLambda }
        val isCallableValueCall = candidate.callInfo.candidateForCommonInvokeReceiver != null ||
                candidate.isSyntheticFunctionTypeInvoke() ||
                candidate.symbol.takeIf { it.isBound }?.cfir is CfirVariable
        val positionalArgumentCount = nonTrailingArguments.takeWhile { it.name == null }.size
        val variadicShape = candidate.cangjieVariadicCallShapeOrNull(parameters, positionalArgumentCount)
        if (variadicShape != null) {
            candidate.initializeVariadicCallInfo(
                parameter = variadicShape.parameter,
                elementType = variadicShape.elementType,
                fixedPositionalArity = variadicShape.fixedPositionalArity,
            )
        }

        if (isCallableValueCall && variadicShape == null && argumentAtoms.size != parameters.size) {
            candidate.initializeArgumentMapping(argumentAtoms, linkedMapOf())
            candidate.numDefaults = 0
            sink.reportDiagnostic(WrongNumberOfArguments(parameters.size, argumentAtoms.size))
            return
        }

        if (variadicParameter == null && argumentAtoms.size > parameters.size) {
            candidate.initializeArgumentMapping(argumentAtoms, linkedMapOf())
            candidate.numDefaults = 0
            sink.reportDiagnostic(TooManyArguments(argumentAtoms[parameters.size].expression, candidate.callInfo.name))
            return
        }

        val regularResult = createCallableArgumentMapping(
            candidate = candidate,
            argumentAtoms = argumentAtoms,
            parameters = parameters,
            nonTrailingArguments = nonTrailingArguments,
            trailingLambdaArguments = trailingLambdaArguments,
            variadicParameter = null,
        )
        val variadicResult = if (variadicShape != null) {
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

        // 普通映射成功时必须保留其 argumentMapping/default/named 语义；并行计算的
        // variadicResult 此时只提供“exact arity 下哪些 atom 可以在类型检查阶段按元素重试”。
        // 只有普通映射已有结构诊断（缺参/多参等）时，才真正提交 variadic mapping。
        val result = when {
            regularResult.diagnostics.isEmpty() -> regularResult
            else -> variadicResult ?: regularResult
        }
        val variadicEligibleArguments = if (
            result === regularResult && variadicResult != null
        ) {
            variadicResult.variadicEligibleArguments
        } else {
            result.variadicEligibleArguments
        }
        if (result === variadicResult && regularResult.diagnostics.isNotEmpty()) {
            candidate.initializeCangjieVariadicRegularCallDiagnostics(regularResult.diagnostics)
        }
        candidate.initializeArgumentMapping(argumentAtoms, result.argumentMapping)
        candidate.initializeVariadicEligibleArguments(variadicEligibleArguments)
        if (result.isEmptyVariadicCall) candidate.markEmptyVariadicCall()
        candidate.numDefaults = result.numDefaults
        result.diagnostics.forEach(sink::reportDiagnostic)
    }

    /**
     * 构造一次 callable 实参映射结果。
     */
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
        val variadicEligibleArguments = mutableListOf<ConeResolutionAtom>()
        val positionalArgumentCount = nonTrailingArguments.takeWhile { it.name == null }.size
        val diagnostics = mutableListOf<ResolutionDiagnostic>()
        val isCallableValueCall = candidate.callInfo.candidateForCommonInvokeReceiver != null ||
                candidate.isSyntheticFunctionTypeInvoke() ||
                candidate.symbol.takeIf { it.isBound }?.cfir is CfirVariable

        val variadicInfo = variadicParameter?.let {
            candidate.cangjieVariadicCallShapeOrNull(parameters, positionalArgumentCount)
                ?.takeIf { info -> info.parameter == variadicParameter }
        }

        var nextPositionalIndex = 0
        var seenNamedArgument = false
        var hasArgumentMappingError = false

        for ((argumentIndex, argument) in nonTrailingArguments.withIndex()) {
            val argumentName = argument.name
            if (argumentName != null) {
                seenNamedArgument = true
                if (parameters.isEmpty()) {
                    diagnostics += TooManyArguments(argument.atom.expression, candidate.callInfo.name)
                    hasArgumentMappingError = true
                    break
                }
                if (isCallableValueCall) {
                    diagnostics += NamedArgumentsNotAllowed(argument.atom.expression, "variable function call")
                    hasArgumentMappingError = true
                    continue
                }

                val parameter = parameters.firstOrNull { it.name == argumentName }
                if (parameter == null) {
                    diagnostics += NamedParameterNotFound(argument.atom.expression, argumentName)
                    hasArgumentMappingError = true
                    break
                }
                if (!parameter.isNamed) {
                    diagnostics +=
                        NamedArgumentsNotAllowed(argument.atom.expression, "parameter '${parameter.name.asString()}'")
                    hasArgumentMappingError = true
                    break
                }
                if (!usedParameters.add(parameter)) {
                    diagnostics += ArgumentPassedTwice(argument.atom.expression, parameter)
                    hasArgumentMappingError = true
                    break
                }
                argumentMapping[argument.atom] = parameter
                continue
            }

            // 官方 `CheckArgsWithParamName` 在首个“命名后位置实参”处终止当前候选检查，
            // 不能继续跳过已绑定形参并制造后续 missing / duplicate / type 级联诊断。
            if (seenNamedArgument) {
                diagnostics += MixingNamedAndPositionalArguments(argument.atom.expression)
                hasArgumentMappingError = true
                break
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

            if (variadicInfo != null && nextPositionalIndex >= variadicInfo.fixedPositionalArity) {
                usedParameters.add(variadicInfo.parameter)
                argumentMapping[argument.atom] = variadicInfo.parameter
                variadicEligibleArguments += argument.atom
                nextPositionalIndex = variadicInfo.fixedPositionalArity + 1
                continue
            }

            val parameter = parameters.getOrNull(nextPositionalIndex)
            if (parameter == null) {
                diagnostics += TooManyArguments(argument.atom.expression, candidate.callInfo.name)
                hasArgumentMappingError = true
                break
            }

            if (parameter.isNamed && !candidate.callInfo.origin.isNamedPrefixOptionalOrigin()) {
                diagnostics += NeedNamedArgument(argument.atom.expression, parameter)
                hasArgumentMappingError = true
                break
            }

            usedParameters.add(parameter)
            argumentMapping[argument.atom] = parameter
            nextPositionalIndex += 1
        }

        val isEmptyVariadicCall = variadicInfo != null &&
                positionalArgumentCount == variadicInfo.fixedPositionalArity
        if (isEmptyVariadicCall) {
            usedParameters.add(variadicInfo.parameter)
        }

        if (!hasArgumentMappingError) {
            mapTrailingLambdaArguments(
                candidate = candidate,
                parameters = parameters,
                variadicParameter = variadicParameter,
                trailingLambdaArguments = trailingLambdaArguments,
                usedParameters = usedParameters,
                argumentMapping = argumentMapping,
                diagnostics = diagnostics,
            )
        }

        if (!hasArgumentMappingError) {
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
            variadicEligibleArguments = variadicEligibleArguments,
            isEmptyVariadicCall = isEmptyVariadicCall,
        )
    }

    /**
     * 对 operator set 的尾部命名参数进行位置实参补位。
     */
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

    /**
     * 将外置尾随 lambda 映射到最后一个函数类型形参。
     */
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
        if (
            lastParameter == null ||
            lastParameter == variadicParameter ||
            lastParameter in usedParameters ||
            !candidate.acceptsImplicitTrailingLambda(lastParameter)
        ) {
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

    /**
     * 官方 Cangjie `SyntaxFilterCandidates` 对隐式尾随 closure 的语法过滤：
     * 只有最后一个形参是函数类型的候选，才允许把该 lambda 映射到该形参。
     */
    private fun Candidate.acceptsImplicitTrailingLambda(parameter: CfirValueParameter): Boolean {
        val parameterType = parameter.returnTypeRef.coneType
            .fullyExpandedType(callInfo.session)
        return parameterType is ConeFunctionType
    }
}

/**
 * 判断候选是否为内建 VArray 构造器候选。
 */
private fun Candidate.isBuiltinVArrayConstructorCandidate(): Boolean {
    val function = symbol.takeIf { it.isBound }?.cfir as? CfirNamedFunction ?: return false
    if (function.origin != CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor) return false
    if ((callInfo.callSite as? CfirFunctionCall)?.varraySizeLiteral != null) return true
    return function.returnTypeRef.coneType is ConeVArrayType
}

/**
 * callable 实参映射结果。
 */
private data class CallableArgumentMappingResult(
    /**
     * 实参 atom 到形参的映射。
     */
    val argumentMapping: LinkedHashMap<ConeResolutionAtom, CfirValueParameter>,
    /**
     * 映射阶段产生的诊断。
     */
    val diagnostics: List<ResolutionDiagnostic>,
    /**
     * 使用默认值的形参数量。
     */
    val numDefaults: Int,
    /** ordinary Array 检查失败后允许按元素重试的实参。 */
    val variadicEligibleArguments: List<ConeResolutionAtom>,
    /** 本次映射是否省略了整个变参数组。 */
    val isEmptyVariadicCall: Boolean,
)

/**
 * 调用实参的映射前信息。
 */
private data class CallArgumentInfo(
    /**
     * 实参 atom。
     */
    val atom: ConeResolutionAtom,
    /**
     * 命名实参名称；位置实参为空。
     */
    val name: Name? = atom.expression.argumentNameOrNull(),
) {
    /**
     * 当前实参是否是外置尾随 lambda。
     */
    val isTrailingLambda: Boolean
        get() = (atom.expression as? CfirAnonymousFunctionExpression)?.isTrailingLambda == true
}

/**
 * 从表达式源码中恢复命名实参名称。
 */
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

/**
 * 返回表达式对应的 value-argument source。
 */
private fun CfirExpression.valueArgumentSourceOrNull(): CjSourceElement? {
    return when (this) {
        is CfirBlock -> source?.takeIf { statements.size == 1 }
        else -> source?.takeIf { it.psi is CjValueArgument }
    }
}

/**
 * 判断调用来源是否允许省略命名前缀。
 */
private fun CfirFunctionCallOrigin.isNamedPrefixOptionalOrigin(): Boolean {
    return this == CfirFunctionCallOrigin.Operator
}
