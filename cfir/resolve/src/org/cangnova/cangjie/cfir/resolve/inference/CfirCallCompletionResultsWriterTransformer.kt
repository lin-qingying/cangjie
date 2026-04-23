package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostic.ConeConstraintSystemHasContradiction
import org.cangnova.cangjie.cfir.diagnostic.ConeTypeParameterInQualifiedAccess
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.buildArgumentListForErrorCall
import org.cangnova.cangjie.cfir.expressions.buildResolvedArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildBlockCopy
import org.cangnova.cangjie.cfir.expressions.builder.buildReturnExpression
import org.cangnova.cangjie.cfir.lookupTracker
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.render
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzer
import org.cangnova.cangjie.cfir.resolve.body.buildAppliedCallableReference
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtomWithPostponedChild
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedLambdaAtom
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirErrorReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.toErrorReference
import org.cangnova.cangjie.cfir.resolve.transformers.CfirAbstractTreeTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.IntegerLiteralAndOperatorApproximationTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.cfir.types.ConeTypeApproximator
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.commonSuperTypeOrNull
import org.cangnova.cangjie.cfir.types.coneTypeSafe
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.fakeElement

class CfirCallCompletionResultsWriterTransformer(
    override val session: CfirSession,
    override val scopeSession: ScopeSession,
    private val finalSubstitutor: ConeSubstitutor,
    private val typeCalculator: ReturnTypeCalculator,
    private val typeApproximator: ConeTypeApproximator,
    private val dataFlowAnalyzer: CfirDataFlowAnalyzer,
    private val integerOperatorApproximator: IntegerLiteralAndOperatorApproximationTransformer,
    private val context: BodyResolveContext,
    private val mode: Mode = Mode.Normal,
    private var insideAnnotationContext: Boolean = false,
) : CfirAbstractTreeTransformer<ExpectedArgumentType?>(phase = CfirResolvePhase.BODY_RESOLVE),
    SessionAndScopeSessionHolder {


    private fun <T : CfirQualifiedAccessExpression> prepareQualifiedTransform(
        qualifiedAccessExpression: T, calleeReference: CfirNamedReferenceWithCandidate,
    ): T {
        val subCandidate = calleeReference.candidate

        val declaration = subCandidate.symbol.cfir

        val type = if (declaration is CfirCallableDeclaration) {
            val calculated = typeCalculator.tryCalculateReturnType(declaration)
            if (calculated !is CfirErrorTypeRef) {
                calculated.coneType
            } else {
                ConeErrorType(calculated.diagnostic)
            }
        } else {
            // this branch is for cases when we have
            // some invalid qualified access expression itself.
            // e.g. `T::toString` where T is a generic type.
            // in these cases we should report an error on
            // the calleeReference.source which is not a fake source.
            ConeErrorType(
                when (declaration) {
                    is CfirTypeParameter -> ConeTypeParameterInQualifiedAccess(declaration.symbol)
                    else -> ConeSimpleDiagnostic("Callee reference to candidate without return type: ${declaration.render()}")
                }
            )
        }

        val resolvedReference =calleeReference.toResolvedReference()

        qualifiedAccessExpression.replaceCalleeReference(resolvedReference)
        qualifiedAccessExpression.replaceDispatchReceiver(
            subCandidate.dispatchReceiverExpression()?.transformSingle(integerOperatorApproximator, null)
        )
        qualifiedAccessExpression.replaceTypeArguments(computeTypeArguments(subCandidate))
        qualifiedAccessExpression.replaceConeTypeOrNull(type)

        runPCLARelatedTasksForCandidate(subCandidate)
        return qualifiedAccessExpression
    }

    private fun ConeCangJieType.substituteType(
        candidate: Candidate,
        // Substitutor from type variables (not type parameters)
        substitutor: ConeSubstitutor = finalSubstitutor,
    ): ConeCangJieType {
        // Type parameters are replaced with type variables
        val initialType = candidate.substitutor.substituteOrSelf(this)
        // Type variables are replaced with final type arguments
        val substitutedType = finallySubstituteOrNull(initialType, substitutor) ?: initialType
        // Everything is approximated
        val finalType = typeApproximator.approximateToSuperType(
            type = substitutedType,
            TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2,
        ) ?: substitutedType

        // This is probably a temporary hack, but it seems necessary because elvis has that attribute and it may leak further like
        // fun <E> foo() = materializeNullable<E>() ?: materialize<E>() // `foo` return type unexpectedly gets inferred to @Exact E
        //
        // In FE1.0, it's not necessary since the annotation for elvis have some strange form (see org.jetbrains.kotlin.resolve.descriptorUtil.AnnotationsWithOnly)
        // that is not propagated further.
        return finalType
    }

    private fun CfirNamedReferenceWithCandidate.computeAllArguments(
        originalArgumentList: CfirArgumentList,
        predefinedMapping: LinkedHashMap<CfirExpression, CfirValueParameter>? = null,
    ): List<CfirExpression> {
        return when {
            this.isError -> originalArgumentList.arguments
            predefinedMapping != null -> predefinedMapping.keys.toList()
            else -> candidate.argumentMapping.keys.unwrapAtoms()
        }
    }

    override fun transformFunctionCall(functionCall: CfirFunctionCall, data: ExpectedArgumentType?): CfirExpression {
        data?.argumentReplacements?.get(functionCall)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }

        val calleeReference = functionCall.calleeReference as? CfirNamedReferenceWithCandidate ?: return functionCall
        val result = prepareQualifiedTransform(functionCall, calleeReference)
        val candidate = calleeReference.candidate
        val originalArgumentList = result.argumentList

        val resultType = completedResultType(candidate)
        val allArgs = calleeReference.computeAllArguments(originalArgumentList)
        val (regularMapping, allArgsMapping) = candidate.handleVarargsAndReturnResultingArgumentsMapping(allArgs)
        val expectedArgumentsTypeMapping = candidate.createArgumentsMapping(forErrorReference = calleeReference.isError)
        result.replaceArgumentList(
            rewriteArgumentList(
                originalArgumentList = originalArgumentList,
                expectedArgumentsTypeMapping = expectedArgumentsTypeMapping,
                regularMapping = regularMapping,
                allArgsMapping = allArgsMapping,
                forErrorReference = calleeReference.isError,
            )
        )

        result.replaceConeTypeOrNull(resultType)
        session.lookupTracker?.recordTypeResolveAsLookup(resultType, functionCall.source, context.file.source)
        result.addNonFatalDiagnostics(candidate)
        return result
    }

    private fun rewriteArgumentList(
        originalArgumentList: CfirArgumentList,
        expectedArgumentsTypeMapping: ExpectedArgumentType.ArgumentsMap?,
        regularMapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
        allArgsMapping: LinkedHashMap<CfirExpression, CfirValueParameter?>,
        forErrorReference: Boolean,
    ): CfirArgumentList {
        val transformedRegularMapping = LinkedHashMap<CfirExpression, CfirValueParameter>(regularMapping.size)
        val transformedAllArgsMapping = LinkedHashMap<CfirExpression, CfirValueParameter?>(allArgsMapping.size)

        for (originalArgument in originalArgumentList.arguments) {
            val transformedArgument = transformCallArgument(originalArgument, expectedArgumentsTypeMapping)
            transformedAllArgsMapping[transformedArgument] = allArgsMapping[originalArgument]
            regularMapping[originalArgument]?.let { parameter ->
                transformedRegularMapping[transformedArgument] = parameter
            }
        }

        return if (forErrorReference) {
            buildArgumentListForErrorCall(originalArgumentList, transformedAllArgsMapping)
        } else {
            buildResolvedArgumentList(originalArgumentList, transformedRegularMapping)
        }
    }

    private fun transformCallArgument(
        originalArgument: CfirExpression,
        expectedArgumentsTypeMapping: ExpectedArgumentType.ArgumentsMap?,
    ): CfirExpression {
        val argumentBeforeTransform =
            (expectedArgumentsTypeMapping?.argumentReplacements?.get(originalArgument) ?: originalArgument) as CfirExpression
        val transformedArgument =
            argumentBeforeTransform.transformSingle(this, expectedArgumentsTypeMapping) as CfirExpression
        val replacedAfterTransform =
            (expectedArgumentsTypeMapping?.argumentReplacements?.get(transformedArgument) ?: transformedArgument) as CfirExpression
        val expectedType = expectedArgumentsTypeMapping?.getExpectedType(originalArgument)
            ?: expectedArgumentsTypeMapping?.getExpectedType(argumentBeforeTransform)
            ?: expectedArgumentsTypeMapping?.getExpectedType(replacedAfterTransform)
        return replacedAfterTransform.transformSingle(integerOperatorApproximator, expectedType) as CfirExpression
    }

    private fun Candidate.createArgumentsMapping(forErrorReference: Boolean): ExpectedArgumentType.ArgumentsMap? {
        val lambdasReturnType = postponedAtoms.filterIsInstance<ConeResolvedLambdaAtom>().associate { atom ->
            atom.anonymousFunction to finallySubstituteOrSelf(substitutor.substituteOrSelf(atom.returnType))
        }
        val arguments = LinkedHashMap<CfirElement, ConeCangJieType>()

        fun registerExpectedType(argument: CfirExpression, expectedType: ConeCangJieType) {
            arguments[argument] = expectedType
            if (argument is CfirAnonymousFunctionExpression) {
                arguments[argument.anonymousFunction] = expectedType
            }
        }

        for ((atom, valueParameter) in argumentMapping) {
            val expectedType = valueParameter.returnTypeRef.coneTypeOrNull?.substituteType(this) ?: continue
            registerExpectedType(atom.unwrapAtom(), expectedType)
        }

        val argumentReplacements = this@createArgumentsMapping.argumentReplacements
        argumentReplacements?.forEach { (original, replacement) ->
            val expectedType = arguments[original] ?: return@forEach
            if (replacement is CfirExpression) {
                registerExpectedType(replacement, expectedType)
            }
        }

        if (lambdasReturnType.isEmpty() && arguments.isEmpty() && argumentReplacements.isNullOrEmpty()) return null
        return ExpectedArgumentType.ArgumentsMap(
            map = arguments,
            lambdasReturnTypes = lambdasReturnType,
            forErrorReference = forErrorReference,
            argumentReplacements,
        )
    }
    private data class ResultingArgumentsMapping(
        val regularMapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
        val allArgsMapping: LinkedHashMap<CfirExpression, CfirValueParameter?>,
    )

    private fun Candidate.handleVarargsAndReturnResultingArgumentsMapping(
        argumentList: List<CfirExpression>,
        precomputedArgumentMapping: LinkedHashMap<CfirExpression, CfirValueParameter>? = null,
    ): ResultingArgumentsMapping {
        val argumentMapping = precomputedArgumentMapping ?: this.argumentMapping.unwrapAtoms()
//TODO 变长参数 目前仓颉使用的是Array，留后处理
//        val varargParameter = argumentMapping.values.firstOrNull { it.isVararg }
//        return if (varargParameter != null) {
//            // Create a CfirVarargArgumentExpression for the vararg arguments
//            val varargParameterTypeRef = varargParameter.returnTypeRef
//            val resolvedArrayType = varargParameterTypeRef.substitute(this)
//            val argumentMappingWithAllArgs =
//                remapArgumentsWithVararg(session, varargParameter, resolvedArrayType, argumentMapping, argumentList)
//            ResultingArgumentsMapping(
//                argumentMappingWithAllArgs.filterValuesNotNull(),
//                argumentMappingWithAllArgs
//            )
//        } else {
      return  ResultingArgumentsMapping(
            argumentMapping,
            argumentList.associateWithTo(LinkedHashMap()) { argumentMapping[it] }
        )
//        }
    }

    override fun transformNamedAccessExpression(
        namedAccessExpression: CfirNamedAccessExpression,
        data: ExpectedArgumentType?
    ): CfirExpression {
        data?.argumentReplacements?.get(namedAccessExpression)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }
        val replacement = replacementFor(namedAccessExpression)
        if (replacement != null) {
            return replacement.transformSingle(this, data)
        }
        return transformQualifiedAccessExpression(namedAccessExpression, data)
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        data: ExpectedArgumentType?
    ): CfirExpression {
        data?.argumentReplacements?.get(qualifiedAccessExpression)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }
        val replacement = replacementFor(qualifiedAccessExpression)
        if (replacement != null) {
            return replacement.transformSingle(this, data)
        }

        val calleeReference = qualifiedAccessExpression.calleeReference as? CfirNamedReferenceWithCandidate
            ?: return qualifiedAccessExpression
        val result = prepareQualifiedTransform(qualifiedAccessExpression, calleeReference)
        result.transformChildren(this, data)
        result.replaceConeTypeOrNull(
            integerOperatorApproximator.approximateType(
                result.coneTypeOrNull,
                data?.getExpectedType(qualifiedAccessExpression),
            )
        )
        return result
    }

    override fun transformArrayLiteral(arrayLiteral: CfirArrayLiteral, data: ExpectedArgumentType?): CfirExpression {
        data?.argumentReplacements?.get(arrayLiteral)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }
        arrayLiteral.transformChildren(this, data)
        arrayLiteral.replaceConeTypeOrNull(
            integerOperatorApproximator.approximateType(
                arrayLiteral.coneTypeOrNull,
                data?.getExpectedType(arrayLiteral),
            )
        )
        return arrayLiteral
    }

    override fun transformAnonymousFunctionExpression(
        anonymousFunctionExpression: CfirAnonymousFunctionExpression,
        data: ExpectedArgumentType?
    ): CfirExpression {
        anonymousFunctionExpression.transformChildren(this, null)
        finalizeAnonymousFunction(
            function = anonymousFunctionExpression.anonymousFunction,
            data = data,
            anonymousFunctionExpression = anonymousFunctionExpression,
        )
        val expectedType = data?.getExpectedType(anonymousFunctionExpression)
            ?: data?.getExpectedType(anonymousFunctionExpression.anonymousFunction)
        val anonymousFunction = anonymousFunctionExpression.anonymousFunction
        val approximatedType = integerOperatorApproximator.approximateType(
            buildLambdaType(anonymousFunction),
            expectedType,
        )
        if (approximatedType != null) {
            anonymousFunction.replaceTypeRef(
                approximatedType.toCfirResolvedTypeRef(anonymousFunction.typeRef.source, anonymousFunction.typeRef),
            )
        }
        return anonymousFunctionExpression
    }

    private fun computeTypeArgumentTypes(
        candidate: Candidate,
    ): List<ConeCangJieType> {
        val declaration = candidate.symbol.cfir as? CfirCallableDeclaration ?: return emptyList()

        return declaration.typeParameters.map {
            val typeParameter = ConeTypeParameterTypeImpl(it.symbol.toLookupTag())
            val substitution = candidate.substitutor.substituteOrSelf(typeParameter)
            finallySubstituteOrSelf(substitution).let { substitutedType ->
                typeApproximator.approximateToSuperType(
                    substitutedType, TypeApproximatorConfiguration.TypeArgumentApproximationAfterCompletionInK2,
                ) ?: substitutedType
            }
        }
    }

    private fun computeTypeArguments(candidate: Candidate): List<CfirResolvedTypeRef> {
        return computeTypeArgumentTypes(candidate).map { type ->
            when (type) {
                is ConeErrorType -> buildErrorTypeRef {
                    coneType = type
                    diagnostic = type.diagnostic
                }

                else -> buildResolvedTypeRef {
                    coneType = type
                }
            }
        }
    }



    private fun replacementFor(expression: CfirExpression): CfirExpression? {
        val candidateReference = (expression as? org.cangnova.cangjie.cfir.expressions.CfirResolvable)
            ?.calleeReference as? CfirNamedReferenceWithCandidate ?: return null
        return candidateReference.candidate.argumentReplacements?.get(expression)
    }

    private fun resolvedReferenceFor(
        calleeReference: CfirNamedReferenceWithCandidate,
        resultType: ConeCangJieType,
    ): CfirNamedReference {
        val resolved = calleeReference.toResolvedReference()
        return if (resolved is CfirResolvedNamedReference) {
            buildAppliedCallableReference(calleeReference.name, calleeReference.candidate, resultType, finalSubstitutor)
        } else {
            resolved
        }
    }

    private fun runPCLARelatedTasksForCandidate(candidate: Candidate) {
        for (postponedCall in candidate.postponedPCLACalls) {
            postponedCall.expression.transform<CfirElement, ExpectedArgumentType?>(this, null)
        }

        for (callback in candidate.onPCLACompletionResultsWritingCallbacks) {
            callback(finalSubstitutor)
        }

        for (lambda in candidate.lambdasAnalyzedWithPCLA) {
            finalizeAnonymousFunction(lambda as? CfirFunction ?: continue, null)
        }
    }

    private fun finalizeAnonymousFunction(
        function: CfirFunction,
        data: ExpectedArgumentType?,
        anonymousFunctionExpression: CfirAnonymousFunctionExpression? = null,
    ) {
        val anonymousFunction = function as? CfirAnonymousFunction ?: return
        val initialReturnType = anonymousFunction.returnTypeRef.coneTypeOrNull
        val returnExpressions = dataFlowAnalyzer
            .returnExpressionsOfAnonymousFunction(anonymousFunction)
            .replacePostponedAtomsInReturnExpressions(data)
        val containingCallIsError = (data as? ExpectedArgumentType.ArgumentsMap)?.forErrorReference == true
        val expectedFunctionType =
            (data?.let { context ->
                anonymousFunctionExpression?.let(context::getExpectedType)
            } as? ConeFunctionType)
                ?: (data?.getExpectedType(anonymousFunction) as? ConeFunctionType)
        /**
         * 对齐 Kotlin FIR 的语义意图：
         * 对“作为实参传入的 lambda”，它的返回类型首先应该受参数函数类型约束，
         * 而不是盲目继承前序阶段残留在 `returnTypeRef` 上的旧值。
         *
         * 当前仓颉在若干 trailing-lambda 场景里，早期阶段会把匿名函数的返回类型
         * 暂时写成外层 `Unit` 语境，若这里继续优先读取旧值，就会把合法的
         * `(Int64) -> Int64` lambda 错误降级成 `RETURN_TYPE_MISMATCH(expected Unit)`.
         *
         * 因此只要存在参数位 expected function type，就优先以它的 return type 作为
         * 匿名函数最终定型入口；没有参数位约束时，再回退到已有 returnTypeRef。
         */
        val expectedReturnType = expectedFunctionType?.returnType?.let(::finallySubstituteOrSelf)
            ?: initialReturnType?.let(::finallySubstituteOrSelf)
            ?: if (!containingCallIsError) {
                (data as? ExpectedArgumentType.ArgumentsMap)?.lambdasReturnTypes?.get(anonymousFunction)
            } else {
                null
            }
            ?: returnExpressions.firstNotNullOfOrNull { it.expression.coneTypeOrNull }

        val newData = expectedReturnType?.toExpectedType(data?.argumentReplacements)
        for (returnExpression in returnExpressions) {
            if (newData != null) {
                returnExpression.expression.transform<CfirElement, ExpectedArgumentType?>(this, newData)
            } else {
                returnExpression.expression.transform(this, null)
            }
            returnExpression.expression.transformSingle(integerOperatorApproximator, expectedReturnType)
        }

        anonymousFunction.body?.let { body ->
            if (newData != null) {
                body.transform<CfirElement, ExpectedArgumentType?>(this, newData)
            } else {
                body.transform(this, null)
            }
            body.transformSingle(integerOperatorApproximator, expectedReturnType)
        }

        val resultReturnType = computeAnonymousFunctionReturnType(
            anonymousFunction = anonymousFunction,
            expectedReturnType = expectedReturnType,
            returnExpressions = returnExpressions,
        )
        if (initialReturnType != resultReturnType) {
            anonymousFunction.replaceReturnTypeRef(
                anonymousFunction.returnTypeRef.resolvedTypeFromPrototype(resultReturnType, anonymousFunction.source)
            )
        }
        anonymousFunction.addReturnToLastStatementIfNeeded()
    }

    private fun buildLambdaType(function: CfirFunction): ConeCangJieType? {
        val parameterTypes = function.valueParameters.mapNotNull { it.returnTypeRef.coneTypeSafe<ConeCangJieType>() }
        val returnType =
            function.returnTypeRef.coneTypeSafe<ConeCangJieType>() ?: function.body?.coneTypeOrNull ?: return null
        return ConeFunctionType(parameterTypes, returnType)
    }

    private fun computeAnonymousFunctionReturnType(
        anonymousFunction: CfirAnonymousFunction,
        expectedReturnType: ConeCangJieType?,
        returnExpressions: Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo>,
    ): ConeCangJieType {
        if (anonymousFunction.isLambda && expectedReturnType?.isUnit == true) {
            return expectedReturnType
        }

        val inferredReturnType = session.typeContext.commonSuperTypeOrNull(
            returnExpressions.mapNotNull { it.expression.coneTypeOrNull }
        ) ?: anonymousFunction.body?.coneTypeOrNull
            ?: session.builtinTypes.unitType

        return if (anonymousFunction.isLambda && expectedReturnType != null && !inferredReturnType.isUnit) {
            expectedReturnType
        } else {
            inferredReturnType
        }
    }

    private fun CfirAnonymousFunction.addReturnToLastStatementIfNeeded() {
        val currentBody = body ?: return
        val returnType = returnTypeRef.coneTypeOrNull ?: return
        if (returnType.isUnit) return

        val lastStatement = currentBody.statements.lastOrNull() as? CfirExpression ?: return
        if (lastStatement is CfirReturnExpression) return

        val newBody = buildBlockCopy(currentBody) {
            statements.clear()
            statements.addAll(currentBody.statements.dropLast(1))
                statements.add(
                    buildReturnExpression {
                        source = (
                            lastStatement.source
                                ?: currentBody.source
                                ?: this@addReturnToLastStatementIfNeeded.source
                            )?.fakeElement(CjFakeSourceElementKind.ImplicitReturn.FromLastStatement)
                        coneTypeOrNull = lastStatement.coneTypeOrNull
                        target = CfirFunctionTarget(labelName = null, isLambda = this@addReturnToLastStatementIfNeeded.isLambda).also {
                            it.bind(this@addReturnToLastStatementIfNeeded)
                        }
                        result = lastStatement
                    }
                )
            }

        (this as? org.cangnova.cangjie.cfir.declarations.impl.CfirAnonymousFunctionImpl)?.body = newBody
    }

    private fun Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo>.replacePostponedAtomsInReturnExpressions(
        data: ExpectedArgumentType?,
    ): Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo> {
        return map { returnInfo ->
            val replacement =
                (data?.argumentReplacements?.get(returnInfo.expression) ?: replacePostponedAtom(returnInfo.expression)) as CfirExpression
            if (replacement === returnInfo.expression) {
                returnInfo
            } else {
                returnInfo.copy(expression = replacement)
            }
        }
    }

    private fun replacePostponedAtom(expression: CfirExpression): CfirExpression {
        val candidate = (expression as? org.cangnova.cangjie.cfir.expressions.CfirResolvable)
            ?.calleeReference as? CfirNamedReferenceWithCandidate
            ?: return expression
        val resolvedCandidate = candidate.candidate
        return when {
            expression is CfirQualifiedAccessExpression || expression is CfirNamedAccessExpression || expression is CfirFunctionCall -> {
                expression.transform<CfirElement, ExpectedArgumentType?>(this, null) as CfirExpression
            }

            else -> {
                val argumentReplacements = resolvedCandidate.argumentReplacements
                if (argumentReplacements?.containsKey(expression) == true) {
                    argumentReplacements.getValue(expression)
                } else {
                    expression
                }
            }
        }
    }

    private fun completedResultType(candidate: Candidate): ConeCangJieType {
        val substituted = finallySubstituteOrSelf(candidate.substitutedReturnType())
        val approximated = typeApproximator.approximateToSuperType(
            substituted,
            TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2,
        ) ?: substituted
        return integerOperatorApproximator.approximateType(approximated, null) ?: approximated
    }

    private fun CfirNamedReferenceWithCandidate.hasAdditionalResolutionErrors(): Boolean = false

    @OptIn(ApplicabilityDetail::class)
    private fun CfirNamedReferenceWithCandidate.toResolvedReference(): CfirNamedReference {
        val errorDiagnostic = when {
            this is CfirErrorReferenceWithCandidate -> this.diagnostic
            !candidate.lowestApplicability.isSuccess ->
                ConeInapplicableCandidateError(candidate.lowestApplicability, candidate)

            !candidate.isSuccessful -> {
                require(candidate.system.hasContradiction) {
                    "Candidate is not successful, but system has no contradiction"
                }
                ConeConstraintSystemHasContradiction(candidate)

            }

            hasAdditionalResolutionErrors() ->
                ConeConstraintSystemHasContradiction(candidate)

            else -> null
        }

        return when (errorDiagnostic) {
            null -> buildResolvedNamedReference {
                source = this@toResolvedReference.source
                name = this@toResolvedReference.name
                resolvedSymbol = this@toResolvedReference.candidateSymbol
            }

            else -> toErrorReference(errorDiagnostic)
        }
    }

    private fun finallySubstituteOrNull(
        type: ConeCangJieType,
        substitutor: ConeSubstitutor = finalSubstitutor,
    ): ConeCangJieType? {
        val result = substitutor.substituteOrNull(type)
        if (result == null && type is ConeIdealLiteralType) {
            return type.approximateIntegerLiteralType()
        }
        return result?.approximateIntegerLiteralType()
    }

    private fun finallySubstituteOrSelf(type: ConeCangJieType): ConeCangJieType {
        return finallySubstituteOrNull(type) ?: type
    }

    enum class Mode {
        Normal,

        // Retained only as an upstream-aligned seam. The current local direct chain has
        // no delegated-property inference session or writer-construction call site that
        // selects this mode, so this enum value is intentionally unreachable for now.
        DelegatedPropertyCompletion,
    }

    private inline fun <T> withCollectionLiteralInAnnotationResolution(block: () -> T): T {
        val savedInsideAnnotationContext = insideAnnotationContext
        insideAnnotationContext = true
        return try {
            block()
        } finally {
            insideAnnotationContext = savedInsideAnnotationContext
        }
    }

    override fun <E : CfirElement> transformElement(element: E, data: ExpectedArgumentType?): E {
        if (element is CfirDeclaration) return element
        return super.transformElement(element, data)
    }
}

sealed class ExpectedArgumentType(
    val argumentReplacements: Map<CfirElement, CfirExpression>?,
) {
    class ArgumentsMap(
        val map: Map<CfirElement, ConeCangJieType>,
        val lambdasReturnTypes: Map<CfirAnonymousFunction, ConeCangJieType>,
        val forErrorReference: Boolean,
        argumentReplacements: Map<CfirElement, CfirExpression>?,
    ) : ExpectedArgumentType(argumentReplacements)

    class ExpectedType(
        val type: ConeCangJieType,
        argumentReplacements: Map<CfirElement, CfirExpression>?,
    ) : ExpectedArgumentType(argumentReplacements)
}

private fun ExpectedArgumentType.getExpectedType(argument: CfirElement): ConeCangJieType? = when (this) {
    is ExpectedArgumentType.ArgumentsMap -> map[argument]
    is ExpectedArgumentType.ExpectedType -> type
}

private fun ConeCangJieType.toExpectedType(
    argumentReplacements: Map<CfirElement, CfirExpression>?,
): ExpectedArgumentType = ExpectedArgumentType.ExpectedType(this, argumentReplacements)

private fun ConeCangJieType.approximateIntegerLiteralType(): ConeCangJieType =
    when (this) {
        is ConeIdealLiteralType -> getApproximatedType()
        else -> this
    }

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.resolvedTypeFromPrototype(
    type: ConeCangJieType,
    source: org.cangnova.cangjie.source.CjSourceElement?,
): CfirResolvedTypeRef {
    return when (type) {
        is ConeErrorType -> buildErrorTypeRef {
            this.source = source ?: this@resolvedTypeFromPrototype.source
            coneType = type
            delegatedTypeRef = this@resolvedTypeFromPrototype
            diagnostic = type.diagnostic
        }

        else -> buildResolvedTypeRef {
            this.source = source ?: this@resolvedTypeFromPrototype.source
            coneType = type
            delegatedTypeRef = this@resolvedTypeFromPrototype
        }
    }
}

private fun Collection<ConeResolutionAtom>.unwrapAtoms(): List<CfirExpression> {
    return map { it.unwrapAtom() }
}

private fun ConeResolutionAtom.unwrapAtom(): CfirExpression {
    return when (this) {
//        is ConeCollectionLiteralAtom -> subAtom?.unwrapAtom() ?: expression
        is ConeResolutionAtomWithPostponedChild -> subAtom?.unwrapAtom() ?: expression
        else -> expression
    }
}

fun <V> LinkedHashMap<ConeResolutionAtom, V>.unwrapAtoms(): LinkedHashMap<CfirExpression, V> {
    return mapKeysToLinkedMap { it.unwrapAtom() }
}

inline fun <K1, K2, V> LinkedHashMap<K1, V>.mapKeysToLinkedMap(transform: (K1) -> K2): LinkedHashMap<K2, V> {
    return mapKeysTo(LinkedHashMap()) { transform(it.key) }
}

internal fun CfirQualifiedAccessExpression.addNonFatalDiagnostics(candidate: Candidate){
//    TODO 用于增加非致命性错误
}
