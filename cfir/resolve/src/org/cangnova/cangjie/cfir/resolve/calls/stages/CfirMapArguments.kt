package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostic.ArgumentPassedTwice
import org.cangnova.cangjie.cfir.diagnostic.HiddenCandidate
import org.cangnova.cangjie.cfir.diagnostic.MixingNamedAndPositionalArguments
import org.cangnova.cangjie.cfir.diagnostic.NamedArgumentsNotAllowed
import org.cangnova.cangjie.cfir.diagnostic.NamedParameterNotFound
import org.cangnova.cangjie.cfir.diagnostic.NeedNamedArgument
import org.cangnova.cangjie.cfir.diagnostic.TrailingLambdaCannotUsedForNonFunction
import org.cangnova.cangjie.cfir.diagnostic.TooManyArguments
import org.cangnova.cangjie.cfir.diagnostic.UnsupportedNamedArgument
import org.cangnova.cangjie.cfir.diagnostic.VisibilityError
import org.cangnova.cangjie.cfir.diagnostic.WrongArgumentCount
import org.cangnova.cangjie.cfir.diagnostic.WrongNumberOfArguments
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.calls.ArgumentMappingOutcome
import org.cangnova.cangjie.cfir.resolve.calls.CallShape
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.cangjieVariadicCallShapeOrNull
import org.cangnova.cangjie.cfir.resolve.calls.cangjieVariadicParameterForMapping
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldIfNeed
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.CjSourceElement

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

        if (candidate.hasTerminalPreArgumentMappingDiagnostic()) {
            if (!candidate.argumentMappingInitialized) {
                candidate.initializeArgumentMapping(argumentAtoms, linkedMapOf())
            }
            candidate.numDefaults = 0
            // 可见性或显式类型实参数量错误支配 value-argument 检查；必须显式
            // yield，不能依赖阶段 runner 在当前阶段返回后再次推断终止条件。
            sink.yieldIfNeed()
            return
        }

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
        val callShape = candidate.createCallShape(argumentInfos)
        val positionalArgumentCount = nonTrailingArguments.takeWhile { it.name == null }.size
        val variadicShape = candidate.cangjieVariadicCallShapeOrNull(parameters, positionalArgumentCount)
        if (variadicShape != null) {
            candidate.initializeVariadicCallInfo(
                parameter = variadicShape.parameter,
                elementType = variadicShape.elementType,
                fixedPositionalArity = variadicShape.fixedPositionalArity,
            )
        }

        if (candidate.isCallableValueCall && variadicShape == null && argumentAtoms.size != parameters.size) {
            candidate.initializeArgumentMapping(argumentAtoms, linkedMapOf())
            candidate.numDefaults = 0
            candidate.initializeArgumentMappingOutcome(
                createArgumentMappingOutcome(
                    callShape = callShape,
                    parameters = parameters,
                    variadicParameter = null,
                    mappedArgumentCount = 0,
                    matchedNamedArgumentCount = 0,
                    hasMappingFailure = true,
                ),
            )
            val arityDiagnostic = WrongNumberOfArguments(
                callShape.arityDiagnosticSource,
                parameters.size,
                argumentAtoms.size,
            )
            if (candidate.callInfo.name.asString() == "f") {
                System.err.println(
                    "CFIR_VARIADIC_ARITY_REPORTED source=${arityDiagnostic.source.startOffset}..${arityDiagnostic.source.endOffset} " +
                        "kind=${candidate.callInfo.callKind} implicit=${candidate.callInfo.isImplicitInvoke} " +
                        "receiver=${candidate.callInfo.candidateForCommonInvokeReceiver?.symbol?.debugName} " +
                        "symbol=${candidate.symbol.debugName}"
                )
            }
            sink.reportDiagnostic(arityDiagnostic)
            return
        }

        val requiredParameterCount = parameters.count { parameter ->
            parameter != variadicParameter && parameter.defaultValue == null
        }
        if (!candidate.isCallableValueCall && argumentAtoms.size < requiredParameterCount) {
            candidate.initializeArgumentMapping(argumentAtoms, linkedMapOf())
            candidate.numDefaults = 0
            candidate.initializeArgumentMappingOutcome(
                createArgumentMappingOutcome(
                    callShape = callShape,
                    parameters = parameters,
                    variadicParameter = variadicParameter,
                    mappedArgumentCount = argumentAtoms.size,
                    matchedNamedArgumentCount = 0,
                    hasMappingFailure = true,
                ),
            )
            val arityDiagnostic = WrongNumberOfArguments(
                callShape.arityDiagnosticSource,
                parameters.size,
                argumentAtoms.size,
            )
            if (candidate.callInfo.name.asString() == "f") {
                System.err.println(
                    "CFIR_VARIADIC_ARITY_REPORTED source=${arityDiagnostic.source.startOffset}..${arityDiagnostic.source.endOffset} " +
                        "kind=${candidate.callInfo.callKind} implicit=${candidate.callInfo.isImplicitInvoke} " +
                        "receiver=${candidate.callInfo.candidateForCommonInvokeReceiver?.symbol?.debugName} " +
                        "symbol=${candidate.symbol.debugName}"
                )
            }
            sink.reportDiagnostic(arityDiagnostic)
            return
        }

        if (variadicParameter == null && argumentAtoms.size > parameters.size) {
            candidate.initializeArgumentMapping(argumentAtoms, linkedMapOf())
            candidate.numDefaults = 0
            candidate.initializeArgumentMappingOutcome(
                createArgumentMappingOutcome(
                    callShape = callShape,
                    parameters = parameters,
                    variadicParameter = null,
                    mappedArgumentCount = parameters.size,
                    matchedNamedArgumentCount = 0,
                    hasMappingFailure = true,
                ),
            )
            val arityDiagnostic = WrongNumberOfArguments(
                callShape.arityDiagnosticSource,
                parameters.size,
                argumentAtoms.size,
            )
            if (candidate.callInfo.name.asString() == "f") {
                System.err.println(
                    "CFIR_VARIADIC_ARITY_REPORTED source=${arityDiagnostic.source.startOffset}..${arityDiagnostic.source.endOffset} " +
                        "kind=${candidate.callInfo.callKind} implicit=${candidate.callInfo.isImplicitInvoke} " +
                        "receiver=${candidate.callInfo.candidateForCommonInvokeReceiver?.symbol?.debugName} " +
                        "symbol=${candidate.symbol.debugName}"
                )
            }
            sink.reportDiagnostic(arityDiagnostic)
            return
        }

        val regularResult = createCallableArgumentMapping(
            candidate = candidate,
            argumentAtoms = argumentAtoms,
            parameters = parameters,
            nonTrailingArguments = nonTrailingArguments,
            trailingLambdaArguments = trailingLambdaArguments,
            variadicParameter = null,
            callShape = callShape,
        )
        val variadicResult = if (variadicShape != null) {
            createCallableArgumentMapping(
                candidate = candidate,
                argumentAtoms = argumentAtoms,
                parameters = parameters,
                nonTrailingArguments = nonTrailingArguments,
                trailingLambdaArguments = trailingLambdaArguments,
                variadicParameter = variadicParameter,
                callShape = callShape,
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
        candidate.initializeArgumentMappingOutcome(
            createArgumentMappingOutcome(
                callShape = callShape,
                parameters = parameters,
                variadicParameter = if (result === variadicResult) variadicParameter else null,
                mappedArgumentCount = result.argumentMapping.size,
                matchedNamedArgumentCount = result.matchedNamedArgumentCount,
                hasMappingFailure = result.diagnostics.isNotEmpty(),
            ),
        )
        candidate.initializeVariadicEligibleArguments(variadicEligibleArguments)
        if (result === variadicResult) {
            /*
             * 官方 `ChkVariadicCallExpr` 仅在普通调用的形参映射已失败时，才把
             * 该组位置实参整体解糖为一个 ArrayLit 后重试。此时每个实参都属于
             * 该合成数组的元素，包括本身为数组字面量的实参；不能再把其中一部分
             * 当作原 Array 形参做普通检查，否则 `f([1, 2], 3)` 会被错误接受。
             */
            variadicEligibleArguments.forEach(candidate::markVariadicArgument)
        }
        if (result.isEmptyVariadicCall) candidate.markEmptyVariadicCall()
        candidate.numDefaults = result.numDefaults
        result.nonBlockingDiagnostics.forEach(candidate::addNonBlockingResolutionDiagnostic)
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
        callShape: CallShape,
    ): CallableArgumentMappingResult {
        val variadicParameterIndex = parameters.indexOf(variadicParameter)
        val argumentMapping = LinkedHashMap<ConeResolutionAtom, CfirValueParameter>(argumentAtoms.size)
        val usedParameters = linkedSetOf<CfirValueParameter>()
        val variadicEligibleArguments = mutableListOf<ConeResolutionAtom>()
        val positionalArgumentCount = nonTrailingArguments.takeWhile { it.name == null }.size
        val diagnostics = mutableListOf<ResolutionDiagnostic>()
        val nonBlockingDiagnostics = mutableListOf<ResolutionDiagnostic>()

        val variadicInfo = variadicParameter?.let {
            candidate.cangjieVariadicCallShapeOrNull(parameters, positionalArgumentCount)
                ?.takeIf { info -> info.parameter == variadicParameter }
        }

        var nextPositionalIndex = 0
        var seenNamedArgument = false
        var hasArgumentMappingError = false
        var matchedNamedArgumentCount = 0

        for ((argumentIndex, argument) in nonTrailingArguments.withIndex()) {
            val argumentName = argument.name

            // 函数值的形参没有可引用的声明名称；命名语法逐项报告，但仍严格按位置映射，
            // 使同一个实参可以继续进入普通期望类型与类型不匹配检查。
            if (candidate.isCallableValueCall) {
                /*
                 * 函数值的最后一个 `Array<T>` 同样遵循普通变参规则。这里不能因为
                 * 函数值不支持命名形参就绕过 [variadicInfo]：否则 `f(1)` 会仍以
                 * `Array<T>` 检查 `1`，而不是在普通数组形参检查失败后以元素类型
                 * `T` 重试。后续 CfirCheckArguments 依据 eligible 集合决定是否真正
                 * 采用该重试，故 Array 实参本身仍保留普通调用语义。
                 */
                if (variadicInfo != null && nextPositionalIndex >= variadicInfo.fixedPositionalArity) {
                    usedParameters.add(variadicInfo.parameter)
                    argumentMapping[argument.atom] = variadicInfo.parameter
                    variadicEligibleArguments += argument.atom
                    nextPositionalIndex = variadicInfo.fixedPositionalArity + 1
                    if (argumentName != null) {
                        nonBlockingDiagnostics += UnsupportedNamedArgument(argument.nameSourceOrFail())
                    }
                    continue
                }

                val parameter = parameters.getOrNull(nextPositionalIndex)
                if (parameter == null) {
                    diagnostics.addWrongNumberOfArguments(callShape, parameters.size)
                    hasArgumentMappingError = true
                    break
                }
                if (argumentName != null) {
                    nonBlockingDiagnostics += UnsupportedNamedArgument(argument.nameSourceOrFail())
                }
                usedParameters.add(parameter)
                argumentMapping[argument.atom] = parameter
                nextPositionalIndex += 1
                continue
            }

            if (argumentName != null) {
                seenNamedArgument = true
                if (parameters.isEmpty()) {
                    diagnostics.addWrongNumberOfArguments(callShape, parameters.size)
                    hasArgumentMappingError = true
                    break
                }

                val parameter = parameters.firstOrNull { it.name == argumentName }
                if (parameter == null) {
                    diagnostics += NamedParameterNotFound(
                        argument = argument.atom.expression,
                        source = argument.nameSourceOrFail(),
                        name = argumentName,
                    )
                    hasArgumentMappingError = true
                    break
                }
                if (!parameter.isNamed) {
                    diagnostics += NamedArgumentsNotAllowed(
                        argument = argument.atom.expression,
                        source = argument.nameSourceOrFail(),
                        targetDescription = "parameter '${parameter.name.asString()}'",
                    )
                    hasArgumentMappingError = true
                    break
                }
                if (!usedParameters.add(parameter)) {
                    diagnostics += ArgumentPassedTwice(
                        argument = argument.atom.expression,
                        source = argument.nameSourceOrFail(),
                        parameter = parameter,
                    )
                    hasArgumentMappingError = true
                    break
                }
                argumentMapping[argument.atom] = parameter
                matchedNamedArgumentCount += 1
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
                diagnostics.addWrongNumberOfArguments(callShape, parameters.size)
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
            hasArgumentMappingError = mapTrailingLambdaArguments(
                candidate = candidate,
                parameters = parameters,
                trailingLambdaArguments = trailingLambdaArguments,
                usedParameters = usedParameters,
                argumentMapping = argumentMapping,
                diagnostics = diagnostics,
                callShape = callShape,
            )
        }

        if (!hasArgumentMappingError) {
            val hasMissingRequiredParameter = parameters
                .filterNot { it in usedParameters }
                .filter { it != variadicParameter }
                .any { it.defaultValue == null }
            if (hasMissingRequiredParameter) {
                diagnostics.addWrongNumberOfArguments(callShape, parameters.size)
            }
        }

        return CallableArgumentMappingResult(
            argumentMapping = argumentMapping,
            diagnostics = diagnostics,
            nonBlockingDiagnostics = nonBlockingDiagnostics,
            matchedNamedArgumentCount = matchedNamedArgumentCount,
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
        trailingLambdaArguments: List<CallArgumentInfo>,
        usedParameters: MutableSet<CfirValueParameter>,
        argumentMapping: MutableMap<ConeResolutionAtom, CfirValueParameter>,
        diagnostics: MutableList<ResolutionDiagnostic>,
        callShape: CallShape,
    ): Boolean {
        val externalArgument = trailingLambdaArguments.firstOrNull() ?: return false
        if (trailingLambdaArguments.size > 1) {
            diagnostics.addWrongNumberOfArguments(callShape, parameters.size)
            return true
        }

        val lastParameter = parameters.lastOrNull()
        if (lastParameter == null) {
            diagnostics.addWrongNumberOfArguments(callShape, parameters.size)
            return true
        }

        if (lastParameter in usedParameters) {
            diagnostics += ArgumentPassedTwice(
                argument = externalArgument.atom.expression,
                source = externalArgument.trailingLambdaOpeningBraceSource(),
                parameter = lastParameter,
            )
            return true
        }

        if (!candidate.isCallableValueCall && !candidate.acceptsImplicitTrailingLambda(lastParameter)) {
            diagnostics += TrailingLambdaCannotUsedForNonFunction(
                source = externalArgument.valueExpression.source
                    ?: error("Trailing lambda must have a source"),
                parameterType = lastParameter.returnTypeRef.coneType.fullyExpandedType(candidate.callInfo.session),
            )
            return true
        }

        /*
         * 函数值调用可能表现为 common invoke、函数类型 receiver 的 synthetic invoke，
         * 或直接由变量符号承载。此时 trailing lambda 只是普通位置实参，必须先映射到末形参；
         * 即使该形参是 Int64/Any，类型错误也由 ArgumentCheckingProcessor 按普通
         * ArgumentTypeMismatch 报告。声明调用专用的 function-formal 语法过滤不能提前改写诊断种类。
         */
        usedParameters.add(lastParameter)
        argumentMapping[externalArgument.atom] = lastParameter
        return false
    }

    /**
     * 官方 Cangjie `SyntaxFilterCandidates` 对隐式尾随 closure 的语法过滤：
     * 只有最后一个形参是函数类型的候选，才允许把该 lambda 映射到该形参。
     */
    private fun Candidate.acceptsImplicitTrailingLambda(parameter: CfirValueParameter): Boolean {
        val parameterType = parameter.returnTypeRef.coneType
            .fullyExpandedType(callInfo.session)
        return parameterType.isFunctionTypeOrHasFunctionUpperBound(callInfo.session, mutableSetOf())
    }

    /** 判断类型本身或类型参数的任一有效上界是否为函数类型。 */
    private fun org.cangnova.cangjie.cfir.types.ConeCangJieType.isFunctionTypeOrHasFunctionUpperBound(
        session: CfirSession,
        visited: MutableSet<CfirTypeParameterSymbol>,
    ): Boolean {
        val expanded = fullyExpandedType(session)
        return when (expanded) {
            is ConeFunctionType -> true
            is ConeTypeParameterType -> {
                val symbol = expanded.lookupTag.typeParameterSymbol
                if (!visited.add(symbol)) return false
                symbol.resolvedBounds.any { bound ->
                    bound.coneType.isFunctionTypeOrHasFunctionUpperBound(session, visited)
                }
            }

            else -> false
        }
    }

    /** 构造当前候选唯一的调用点参数形状。 */
    private fun Candidate.createCallShape(arguments: List<CallArgumentInfo>): CallShape {
        val functionCall = callInfo.callSite as? CfirFunctionCall
        val argumentListSource = functionCall?.argumentList?.source
        val callSiteSource = callInfo.callSite.source
        val firstAvailableSource = argumentListSource
            ?: callSiteSource
            ?: arguments.firstNotNullOfOrNull { it.atom.expression.source }
            ?: error("Callable argument mapping requires a source-bearing call site")
        val trailingLambdaEnd = arguments
            .asSequence()
            .filter { it.isTrailingLambda }
            .mapNotNull { it.valueExpression.source?.endOffset }
            .maxOrNull()
        val startOffset = argumentListSource?.startOffset ?: firstAvailableSource.startOffset
        val endOffset = trailingLambdaEnd
            ?: argumentListSource?.endOffset
            ?: callSiteSource?.endOffset
            ?: firstAvailableSource.endOffset
        check(endOffset >= startOffset) {
            "Invalid call-shape source range: $startOffset..$endOffset"
        }

        val arityDiagnosticSource = CjOffsetsOnlySourceElement(startOffset, endOffset)
        if (isCallableValueCall && callInfo.name.asString() == "f") {
            System.err.println(
                "CFIR_VARIADIC_ARITY_SHAPE call=${callSiteSource?.startOffset}..${callSiteSource?.endOffset} " +
                    "argumentList=${argumentListSource?.startOffset}..${argumentListSource?.endOffset} " +
                    "shape=${arityDiagnosticSource.startOffset}..${arityDiagnosticSource.endOffset}"
            )
        }

        return CallShape(
            actualArgumentCount = arguments.size,
            namedArgumentCount = arguments.count { it.name != null },
            trailingLambdaCount = arguments.count { it.isTrailingLambda },
            arityDiagnosticSource = arityDiagnosticSource,
        )
    }
}

/** 参数映射前的可见性和显式类型实参数量错误会支配后续调用形状诊断。 */
private fun Candidate.hasTerminalPreArgumentMappingDiagnostic(): Boolean = diagnostics.any { diagnostic ->
    diagnostic is HiddenCandidate ||
            diagnostic is VisibilityError ||
            diagnostic is WrongArgumentCount
}

/** 构造候选参数映射的共享结构化结果。 */
private fun createArgumentMappingOutcome(
    callShape: CallShape,
    parameters: List<CfirValueParameter>,
    variadicParameter: CfirValueParameter?,
    mappedArgumentCount: Int,
    matchedNamedArgumentCount: Int,
    hasMappingFailure: Boolean,
): ArgumentMappingOutcome = ArgumentMappingOutcome(
    callShape = callShape,
    expectedParameterCount = parameters.size,
    requiredParameterCount = parameters.count { parameter ->
        parameter != variadicParameter && parameter.defaultValue == null
    },
    maximumAcceptedArgumentCount = parameters.size.takeIf { variadicParameter == null },
    mappedArgumentCount = mappedArgumentCount,
    matchedNamedArgumentCount = matchedNamedArgumentCount,
    hasMappingFailure = hasMappingFailure,
)

/** 同一候选的参数数量失败始终聚合成一条调用级诊断。 */
private fun MutableList<ResolutionDiagnostic>.addWrongNumberOfArguments(
    callShape: CallShape,
    expectedCount: Int,
) {
    if (none { it is WrongNumberOfArguments }) {
        this += WrongNumberOfArguments(
            source = callShape.arityDiagnosticSource,
            expectedCount = expectedCount,
            actualCount = callShape.actualArgumentCount,
        )
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
    /** 不阻断后续类型检查的解析诊断。 */
    val nonBlockingDiagnostics: List<ResolutionDiagnostic>,
    /** 成功按名称匹配的命名实参数量。 */
    val matchedNamedArgumentCount: Int,
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
    val name: Name? = (atom.expression as? CfirNamedArgumentExpression)?.argumentName,
) {
    /** 命名包装内部的真实值表达式。 */
    val valueExpression: CfirExpression
        get() = (atom.expression as? CfirNamedArgumentExpression)?.expression ?: atom.expression

    /** 命名实参名称 token 的 source。 */
    fun nameSourceOrFail(): AbstractCjSourceElement =
        (atom.expression as? CfirNamedArgumentExpression)?.nameSource
            ?: error("Named argument must carry a name source")

    /**
     * 当前实参是否是外置尾随 lambda。
     */
    val isTrailingLambda: Boolean
        get() = (valueExpression as? CfirAnonymousFunctionExpression)?.isTrailingLambda == true

    /** 尾随 closure 左花括号 token 的 source。 */
    fun trailingLambdaOpeningBraceSource(): AbstractCjSourceElement {
        val source = valueExpression.source ?: error("Trailing lambda must have a source")
        return CjOffsetsOnlySourceElement(
            source.startOffset,
            (source.startOffset + 1).coerceAtMost(source.endOffset),
        )
    }
}

/**
 * 判断调用来源是否允许省略命名前缀。
 */
private fun CfirFunctionCallOrigin.isNamedPrefixOptionalOrigin(): Boolean {
    return this == CfirFunctionCallOrigin.Operator
}
