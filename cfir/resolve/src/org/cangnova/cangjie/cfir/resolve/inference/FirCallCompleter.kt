package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedLambdaAtom
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType

class CfirCallCompleter(
    private val transformer: CfirAbstractBodyResolveTransformerDispatcher,
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
) : SessionHolder {
    override val session: CfirSession = components.session
    private val inferenceSession
        get() = transformer.context.inferenceSession

    val completer: ConstraintSystemCompleter = ConstraintSystemCompleter(components)

    fun <T> completeCall(
        call: T,
        resolutionMode: ResolutionMode,
        // Only expected to be true for resolving different versions of augmented assignments
        skipEvenPartialCompletion: Boolean = false,
    ): T where T : CfirResolvable, T : CfirExpression {
        val type = components.typeFromCallee(call)

        val reference = call.calleeReference as? CfirNamedReferenceWithCandidate ?: return call

        val candidate = reference.candidate
        val initialType = type.initialTypeOfCandidate(candidate)

        // Annotation types are resolved during type resolution, and generic arguments aren't inferred.
        // Updating the type of an annotation call is a no-op, it only checks if it's the same as the type of the annotation type ref.
        // In the case of a generic annotation, we would set it to a type containing type variable types which would cause an exception.
        // Delegated constructor calls always have type Unit but typeFromCallee returns the type of the superclass.
        if (call !is CfirAnnotationCall && call !is CfirDelegatedConstructorCall) {
            call.resultType = initialType
        }

        session.lookupTracker?.recordTypeResolveAsLookup(initialType, call.source, components.context.file.source)

        addConstraintFromExpectedType(
            candidate,
            initialType,
            resolutionMode,
        )

        if (skipEvenPartialCompletion) return call

        val completionMode = candidate.computeCompletionMode(
            session.inferenceComponents, resolutionMode, initialType
        ).let {
            when {
                it == ConstraintSystemCompletionMode.FULL ->
                    inferenceSession.customCompletionModeInsteadOfFull(call) ?: ConstraintSystemCompletionMode.FULL
                else -> it
            }
        }

        val analyzer = createPostponedArgumentsAnalyzer(transformer.resolutionContext)
        if (call is CfirFunctionCall) {
            call.replaceLambdaArgumentEffects(session)
        }

        return when (completionMode) {
            ConstraintSystemCompletionMode.FULL -> {
                runCompletionForCall(candidate, completionMode, call, initialType, analyzer)

                val readOnlyConstraintStorage = candidate.system.asReadOnlyStorage()
                checkStorageConstraintsAfterFullCompletion(readOnlyConstraintStorage)

                val finalSubstitutor = readOnlyConstraintStorage
                    .buildAbstractResultingSubstitutor(session.typeContext).asCone()
                call.transformSingle(
                    createCompletionResultsWriter(finalSubstitutor),
                    null
                )
            }

            ConstraintSystemCompletionMode.PARTIAL, ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL -> {
                runCompletionForCall(candidate, completionMode, call, initialType, analyzer)

                inferenceSession.processPartiallyResolvedCall(call, resolutionMode, completionMode)

                if (candidate.isSyntheticCallForTopLevelLambda() || candidate.isSyntheticCallForTopLevelCL()) {
                    // This piece is only relevant for top-level lambdas inside PCLA.
                    // For a non-PCLA case, their synthetic call would be complete in the FULL mode.
                    // See CfirSyntheticCallGenerator.resolveAnonymousFunctionExpressionWithSyntheticOuterCall
                    //
                    // Here we preliminarily run the completion writer on the call
                    // to make it write the resulting type to the lambda, so it can be used further.
                    // Otherwise, the type of the lambda would be left implicit.
                    //
                    // On the other hand, we can't complete such a call FULLy because it still contains
                    // not-fixed outer type variables.
                    //
                    // Frankly speaking, this is some sort of hack, which currently I don't know how to resolve properly.
                    //
                    // Similarly, we need to replace top-level collection literals with function calls.
                    val storage = candidate.system.currentStorage()
                    val finalSubstitutor = storage
                        .buildCurrentSubstitutor(session.typeContext, emptyMap()).asCone()
                    call.transformSingle(
                        createCompletionResultsWriter(finalSubstitutor),
                        null
                    )
                } else {
                    call
                }
            }

            @OptIn(ExclusiveForOverloadResolutionByLambdaReturnType::class)
            ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA
                -> throw IllegalStateException()
        }
    }

    private fun Candidate.isSyntheticCallForTopLevelLambda(): Boolean = callInfo.callSite is CfirAnonymousFunctionExpression
    private fun Candidate.isSyntheticCallForTopLevelCL(): Boolean {
        return symbol is CfirSyntheticFunctionSymbol && callInfo.callSite is CfirCollectionLiteral
    }

    private fun checkStorageConstraintsAfterFullCompletion(storage: ConstraintStorage) {
        // Fast path for sake of optimization
        if (storage.notFixedTypeVariables.isEmpty()) return

        // We unmuted assertion only since 2.1, together with a fix for KT-69040
        if (!LanguageFeature.PCLAEnhancementsIn21.isEnabled()) return

        val notFixedTypeVariablesBasedOnTypeParameters = storage.notFixedTypeVariables.filter {
            it.value.typeVariable is ConeTypeParameterBasedTypeVariable
        }

        // TODO: Turn it into `require(storage.notFixedTypeVariables.isEmpty())` (KT-66759)
        require(notFixedTypeVariablesBasedOnTypeParameters.isEmpty()) {
            "All variables should be fixed to something, " +
                    "but {${notFixedTypeVariablesBasedOnTypeParameters.keys.joinToString(", ")}} are found"
        }
    }

    private fun addConstraintFromExpectedType(
        candidate: Candidate,
        initialType: ConeCangJieType,
        resolutionMode: ResolutionMode,
    ) {
        if (resolutionMode !is ResolutionMode.WithExpectedType || resolutionMode.arrayLiteralPosition == ArrayLiteralPosition.AnnotationArgument) return
        val expectedType = resolutionMode.expectedType.fullyExpandedType()

        val system = candidate.system
        when {
            // Only add equality constraint in independent contexts (resolutionMode.forceFullCompletion) for K1 compatibility.
            // Otherwise,
            // we miss some constraints from incorporation which leads to NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER in cases like
            // compiler/testData/diagnostics/tests/inference/nestedIfWithExpectedType.kt.
            resolutionMode.forceFullCompletion && candidate.isSyntheticFunctionCallThatShouldUseEqualityConstraint(expectedType) -> {
                system.addEqualityConstraintIfCompatible(initialType, expectedType, ConeExpectedTypeConstraintPosition)
                candidate.markWasExpectedTypeAddedAsEqualityForSyntheticCall()
            }
            resolutionMode.fromCast -> {
                if (candidate.isFunctionForExpectTypeFromCastFeature()) {
                    system.addSubtypeConstraint(
                        initialType, expectedType,
                        ConeExpectedTypeConstraintPosition,
                    )
                }
            }
            // Hopefully, this whole part may be removed with KT-63678
            expectedType.isUnitOrFlexibleUnit && resolutionMode.lastStatementInBlock -> {
                when {
                    system.notFixedTypeVariables.isEmpty() -> return
                    expectedType.isUnit ->
                        // There's no much sense in using EQUALITY where just subtyping should be enough,
                        // but it seems like it was introduced as a workaround for KT-39900, to avoid adding Unit constraint
                        // which wouldn't fail before lambda analysis, but would lead after it.
                        // See diagnostics/tests/inference/coercionToUnit/coerctionToUnitForATypeWithUpperBound.kt
                        // But it seems like it should be generally resolved via KT-63678
                        // TODO: Consider using `addSubtypeConstraintIfCompatible` both for Unit and Unit! (KT-72396)
                        system.addEqualityConstraintIfCompatible(initialType, expectedType, ConeExpectedTypeConstraintPosition)
                    // Flexible Unit!
                    else -> system.addSubtypeConstraintIfCompatible(initialType, expectedType, ConeExpectedTypeConstraintPosition)
                }
            }
            // In general, we assume that type mismatch should always be reported in some checker/resolution stage explicitly,
            // so we only add an expected type as a hint, not as a strict requirement.
            // The idea behind it is that it would be more clear if we let the call be inferred without the expected type
            // leading to CS error, and then would report diagnostic in the checker for the whole resolved expression.
            //
            // For example, in a case like `val x: List<Int> = listOf("")`, it seems to be clearer to infer
            // the whole expression type to List<String> and then to report INITIALIZER_TYPE_MISMATCH on it then to handle vague CS errors
            // after adding apriori incorrect constraints from the expected type
            //
            // It's assumed to be safe & sound, because if a constraint system has contradictions when the expected type is added,
            // the resulting expression type cannot be inferred to something that is a subtype of `expectedType`,
            // thus the diagnostic would be reported in the checker in any case
            // (see the kdoc for ResolutionMode.WithExpectedType).
            else -> system.addSubtypeConstraintIfCompatible(initialType, expectedType, ConeExpectedTypeConstraintPosition)
        }
    }

    /**
     * For synthetic functions (when, try, !!, but **not** elvis), we need to add an equality constraint for the expected type
     * so that some type variables aren't inferred to `Nothing` that appears in one of the branches.
     *
     * @See org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils.createKnownTypeParameterSubstitutorForSpecialCall
     */
    private fun Candidate.isSyntheticFunctionCallThatShouldUseEqualityConstraint(expectedType: ConeCangJieType): Boolean {
        // If we're inside an assignment's RHS, we mustn't add an equality constraint because it might prevent smartcasts.
        // Example: val x: String? = null; x = if (foo) "" else throw Exception()
        if (!LanguageFeature.EqualityConstraintForOperatorsUnderAssignments.isEnabled() && components.context.isInsideAssignmentRhs) return false

        val symbol = symbol as? CfirCallableSymbol ?: return false
        if (symbol.origin != CfirDeclarationOrigin.Synthetic.FakeFunction ||
            expectedType.isUnitOrAnyWithArbitraryNullability() ||
            // We don't want to add an equality constraint to a nullable type to a !! call.
            // See compiler/testData/diagnostics/tests/inference/checkNotNullWithNullableExpectedType.kt
            (symbol.callableId == SyntheticCallableId.CHECK_NOT_NULL && expectedType.canBeNull(session))
        ) {
            return false
        }

        // If our expression contains any elvis, even nested, we mustn't add an equality constraint because it might influence the
        // inferred type of the elvis RHS.
        if (system.allTypeVariables.values.any {
                it is ConeTypeParameterBasedTypeVariable && it.typeParameterSymbol.containingDeclarationSymbol.isSyntheticElvisFunction()
            }
        ) {
            return false
        }

        return true
    }

    /**
     * @return true for Any?, Any!, Any, Unit?, Unit!, Unit, otherwise false
     */
    private fun ConeCangJieType.isUnitOrAnyWithArbitraryNullability(): Boolean {
        if (this is ConeDynamicType) return false
        val upperBound = upperBoundIfFlexible()
        return with(upperBound) { isUnitOrNullableUnit || isAnyOrNullableAny }
    }

    private fun CfirBasedSymbol<*>.isSyntheticElvisFunction(): Boolean {
        return origin == CfirDeclarationOrigin.Synthetic.FakeFunction && (this as? CfirCallableSymbol)?.callableId == SyntheticCallableId.ELVIS
    }

    fun <T> runCompletionForCall(
        candidate: Candidate,
        completionMode: ConstraintSystemCompletionMode,
        call: T,
        initialType: ConeCangJieType,
        analyzer: PostponedArgumentsAnalyzer? = null,
    ) where T : CfirExpression, T : CfirResolvable {
        @Suppress("NAME_SHADOWING")
        val analyzer = analyzer ?: createPostponedArgumentsAnalyzer(transformer.resolutionContext)
        completer.complete(
            candidate.system.asConstraintSystemCompleterContext(),
            completionMode,
            listOf(ConeAtomWithCandidate(call, candidate)),
            initialType,
            transformer.resolutionContext
        ) { atom, withPCLASession, precalculatedBoundsForCL ->
            analyzer.analyze(candidate.system, atom, candidate, withPCLASession, precalculatedBoundsForCL)
        }
    }

    fun prepareLambdaAtomForFactoryPattern(
        atom: ConeResolvedLambdaAtom,
        candidate: Candidate,
    ) {
        val returnVariable = ConeTypeVariableForLambdaReturnType(atom.anonymousFunction, PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE)
        val csBuilder = candidate.system.getBuilder()
        csBuilder.registerVariable(returnVariable)
        val functionalType = csBuilder.buildCurrentSubstitutor()
            .safeSubstitute(csBuilder, atom.expectedType!!) as ConeClassLikeType
        val size = functionalType.typeArguments.size
        val expectedType = ConeClassLikeTypeImpl(
            functionalType.lookupTag,
            Array(size) { index -> if (index != size - 1) functionalType.typeArguments[index] else returnVariable.defaultType },
            isMarkedNullable = functionalType.isMarkedNullable,
            functionalType.attributes
        )
        csBuilder.addSubtypeConstraint(expectedType, functionalType, ConeArgumentConstraintPosition(atom.anonymousFunction))
        atom.replaceExpectedType(expectedType, returnVariable.defaultType)
        atom.replaceTypeVariableForLambdaReturnType(returnVariable)
    }

    fun createCompletionResultsWriter(
        substitutor: ConeSubstitutor,
        mode: CfirCallCompletionResultsWriterTransformer.Mode = CfirCallCompletionResultsWriterTransformer.Mode.Normal,
    ): CfirCallCompletionResultsWriterTransformer {
        return CfirCallCompletionResultsWriterTransformer(
            session, components.scopeSession, substitutor, components.returnTypeCalculator,
            session.typeApproximator,
            components.dataFlowAnalyzer,
            components.integerLiteralAndOperatorApproximationTransformer,
            components.samResolver,
            components.context,
            mode,
            insideAnnotationContext = components.context.isInsideAnnotationContext,
        )
    }

    fun createPostponedArgumentsAnalyzer(context: ResolutionContext): PostponedArgumentsAnalyzer {
        val lambdaAnalyzer = LambdaAnalyzerImpl()
        return PostponedArgumentsAnalyzer(
            context,
            lambdaAnalyzer,
            session.inferenceComponents,
            transformer.components.callResolver
        )
    }

    private inner class LambdaAnalyzerImpl : LambdaAnalyzer {
        override fun analyzeAndGetLambdaReturnArguments(
            lambdaAtom: ConeResolvedLambdaAtom,
            receiverType: ConeCangJieType?,
            contextParameters: List<ConeCangJieType>,
            parameters: List<ConeCangJieType>,
            expectedReturnType: ConeCangJieType?,
            candidate: Candidate,
            withPCLASession: Boolean,
            forOverloadByLambdaReturnType: Boolean,
        ): ReturnArgumentsAnalysisResult {
            val lambda: CfirAnonymousFunction = lambdaAtom.anonymousFunction
            val needItParam = lambda.valueParameters.isEmpty() && parameters.size == 1

            lambda.matchingParameterFunctionType = lambdaAtom.expectedType

            val itParam = when {
                needItParam -> {
                    val name = StandardNames.IMPLICIT_LAMBDA_PARAMETER_NAME
                    val itType = parameters.single()
                    buildValueParameter {
                        resolvePhase = CfirResolvePhase.BODY_RESOLVE
                        source = lambdaAtom.anonymousFunction.source?.fakeElement(ItLambdaParameter)
                        containingDeclarationSymbol = lambda.symbol
                        moduleData = session.moduleData
                        origin = CfirDeclarationOrigin.Source
                        this.name = name
                        symbol = CfirValueParameterSymbol()
                        returnTypeRef =
                            itType.approximateLambdaInputType(symbol, withPCLASession, candidate).toCfirResolvedTypeRef(
                                lambdaAtom.anonymousFunction.source?.fakeElement(ImplicitReturnTypeOfLambdaValueParameter)
                            )
                        defaultValue = null
                        isCrossinline = false
                        isNoinline = false
                        isVararg = false
                    }
                }
                else -> null
            }

            val expectedReturnTypeRef = expectedReturnType?.let {
                lambda.returnTypeRef.resolvedTypeFromPrototype(it, lambda.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef))
            }

            when {
                receiverType == null -> lambda.replaceReceiverParameter(null)
                !lambdaAtom.coerceCfirstParameterToExtensionReceiver -> {
                    lambda.receiverParameter?.apply {
                        val type = receiverType.approximateLambdaInputType(valueParameter = null, withPCLASession, candidate)
                        val source =
                            source?.fakeElement(KtFakeSourceElementKind.LambdaReceiver)
                                ?: lambda.source?.fakeElement(KtFakeSourceElementKind.LambdaReceiver)
                        replaceTypeRef(typeRef.resolvedTypeFromPrototype(type, source))
                    }
                }
                else -> lambda.replaceReceiverParameter(null)
            }

            lambda.setContextParametersConfiguration(contextParameters, withPCLASession, candidate)

            val lookupTracker = session.lookupTracker
            val fileSource = components.file.source
            val theParameters = when {
                lambdaAtom.coerceCfirstParameterToExtensionReceiver -> when (receiverType) {
                    null -> error("Coercion to extension receiver while no receiver present")
                    else -> listOf(receiverType) + parameters
                }
                else -> parameters
            }
            lambda.valueParameters.forEachIndexed { index, parameter ->
                if (index >= theParameters.size) {
                    // May happen in erroneous code, see KT-60450
                    // In test forEachOnZip.kt we have two declared parameters, but in fact forEach expects only one
                    parameter.replaceReturnTypeRef(
                        buildErrorTypeRef {
                            diagnostic = ConeCannotInferValueParameterType(
                                parameter.symbol, "Lambda or anonymous function has more parameters than expected"
                            )
                            source = parameter.source
                        }
                    )
                    return@forEachIndexed
                }
                val newReturnType = theParameters[index].approximateLambdaInputType(parameter.symbol, withPCLASession, candidate)
                val newReturnTypeSource = parameter.source?.fakeElement(ImplicitReturnTypeOfLambdaValueParameter)
                val newReturnTypeRef = if (parameter.returnTypeRef is CfirImplicitTypeRef) {
                    newReturnType.toCfirResolvedTypeRef(newReturnTypeSource)
                } else {
                    parameter.returnTypeRef.resolvedTypeFromPrototype(newReturnType, newReturnTypeSource)
                }
                parameter.replaceReturnTypeRef(newReturnTypeRef)
                lookupTracker?.recordTypeResolveAsLookup(newReturnTypeRef, parameter.source, fileSource)
            }

            lambda.replaceValueParameters(lambda.valueParameters + listOfNotNull(itParam))
            lambda.replaceReturnTypeRef(
                expectedReturnTypeRef?.also {
                    lookupTracker?.recordTypeResolveAsLookup(it, lambda.source, fileSource)
                } ?: components.noExpectedType
            )

            var additionalConstraints: ConstraintStorage? = null

            transformer.context.withAnonymousFunctionTowerDataContext(lambda.symbol) {
                val pclaInferenceSession = runIf(withPCLASession) {
                    candidate.lambdasAnalyzedWithPCLA += lambda
                    CfirPCLAInferenceSession(candidate, session.inferenceComponents)
                }
                val lambdaExpression = lambdaAtom.expression
                val declarationsTransformer = transformer.declarationsTransformer!!
                if (pclaInferenceSession != null) {
                    transformer.context.withInferenceSession(pclaInferenceSession) {
                        declarationsTransformer.doTransformAnonymousFunctionBodyFromCallCompletion(
                            lambdaExpression,
                            expectedReturnTypeRef,
                        )

                        applyResultsToMainCandidate()
                    }
                } else {
                    additionalConstraints =
                        transformer.context.inferenceSession.runLambdaCompletion(candidate, forOverloadByLambdaReturnType) {
                            declarationsTransformer.doTransformAnonymousFunctionBodyFromCallCompletion(
                                lambdaExpression,
                                expectedReturnTypeRef,
                            )
                        }
                }
            }
            transformer.context.dropContextForAnonymousFunction(lambda)

            val returnArguments = components.dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(lambda)
                .map {
                    val rawAtom = ConeResolutionAtom.createRawAtom(it.expression)
                    when {
                        expectedReturnType == null ->
                            rawAtom
                        !LanguageFeature.PCLAEnhancementsIn21.isEnabled() ->
                            rawAtom
                        // Generally, this branch should be removed, and we should use ConeSimpleLeafResolutionAtom here, too.
                        // (see the comment under the "else").
                        //
                        // But due to subtle resolution details, we preserve atoms with candidates, so we might consider it
                        // as `haveSubsystem == true` at PostponedArgumentsAnalyzer.applyResultsOfAnalyzedLambdaToCandidateSystem,
                        // which would allow us to add Unit constraint there and infer Unit as a builder result at
                        //  diagnostics/tests/inference/pcla/issues/kt52838c.fir.kt
                        //
                        // Perfectly, this Unit constraint shouldn't be required at all or should be added at
                        // CfirCallCompleter.addConstraintFromExpectedType, but there we use EQUALITY constraints for Unit
                        // and `Captured(*) & PTVv` as an expression type (smart cast), so it fails.
                        // TODO: Hopefully this can be removed once we get rid of adding EQUALITY for Unit expected type (KT-72396)
                        rawAtom is ConeAtomWithCandidate ->
                            rawAtom
                        else ->
                            // If return statements of lambda analyzed with an expected type (so, not in dependent mode),
                            // there should be no complex atom left.
                            //
                            // This is especially crucial for lambdas used as return expressions of the current lambda, since,
                            // with expected, type, they have been completely analyzed, so we shouldn't create postponed atoms for them.
                            // Because such postponed atoms would be taken into account by ConstraintSystemCompleter
                            // (e.g., at fixNextReadyVariableForParameterTypeIfNeeded), thus affecting variable fixation order.
                            // See diagnostics/tests/inference/pcla/forceLambdaCompletionFromReturnStatement/noPostponedAtomForNestedLambda.fir.kt
                            ConeSimpleLeafResolutionAtom(it.expression, allowUnresolvedExpression = false)
                    }
                }

            return ReturnArgumentsAnalysisResult(returnArguments, additionalConstraints)
        }

        private fun CfirAnonymousFunction.setContextParametersConfiguration(
            givenContextParameterTypes: List<ConeCangJieType>,
            withPCLASession: Boolean,
            candidate: Candidate,
        ) {
            if (givenContextParameterTypes.isEmpty()) return
            val originalLambdaSource = source
            if (isLambda) {
                replaceContextParameters(
                    givenContextParameterTypes.map { contextParameterType ->
                        buildValueParameter {
                            resolvePhase = CfirResolvePhase.BODY_RESOLVE
                            source = originalLambdaSource?.fakeElement(KtFakeSourceElementKind.LambdaContextParameter)
                            containingDeclarationSymbol = this@setContextParametersConfiguration.symbol
                            moduleData = session.moduleData
                            origin = CfirDeclarationOrigin.Source
                            name = SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
                            symbol = CfirValueParameterSymbol()
                            returnTypeRef = contextParameterType
                                .approximateLambdaInputType(symbol, withPCLASession, candidate)
                                .toCfirResolvedTypeRef(originalLambdaSource?.fakeElement(KtFakeSourceElementKind.LambdaContextParameter))
                            valueParameterKind = CfirValueParameterKind.ContextParameter
                        }
                    }
                )
            } else {
                check(givenContextParameterTypes.size == contextParameters.size)
                contextParameters.forEachIndexed { index, parameter ->
                    val contextParameterType = givenContextParameterTypes[index]
                    parameter.replaceReturnTypeRef(
                        contextParameterType
                            .approximateLambdaInputType(parameter.symbol, withPCLASession, candidate)
                            .toCfirResolvedTypeRef(
                                parameter.returnTypeRef.source
                                    ?: parameter.source?.fakeElement(ImplicitReturnTypeOfLambdaValueParameter)
                            )
                    )
                }
            }
        }
    }

    private fun ConeCangJieType.approximateLambdaInputType(
        valueParameter: CfirValueParameterSymbol?,
        isRootLambdaForPCLASession: Boolean,
        containingCandidate: Candidate,
    ): ConeCangJieType {
        // We only run lambda completion from ConstraintSystemCompletionContext.analyzeRemainingNotAnalyzedPostponedArgument when they are
        // left uninferred.
        // Currently, we use stub types for builder inference, so CANNOT_INFER_PARAMETER_TYPE is the only possible result here.
        if (useErrorTypeInsteadOfTypeVariableForParameterType(isReceiver = valueParameter == null, isRootLambdaForPCLASession)) {
            val diagnostic = valueParameter?.let {
                ConeCannotInferValueParameterType(it, isTopLevelLambda = containingCandidate.isSyntheticCallForTopLevelLambda())
            } ?: ConeCannotInferReceiverParameterType()
            return ConeErrorType(diagnostic)
        }

        return session.typeApproximator.approximateToSuperType(
            this, TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2
        ) ?: this
    }

    private fun ConeCangJieType.useErrorTypeInsteadOfTypeVariableForParameterType(
        isReceiver: Boolean,
        isRootLambdaForPCLASession: Boolean,
    ): Boolean {
        if (this !is ConeTypeVariableType) return false

        // Receivers are expected to be fixed both for PCLA/nonPCLA lambdas, so just build error type
        if (isReceiver) return true

        // Besides PCLA, all type variables for parameter types should be fixed before lambda analysis
        // Inside PCLA (or when we start it), we force fixing receivers before lambda analysis, but allow value parameters
        // to remain unfixed TVs.
        if (isRootLambdaForPCLASession || inferenceSession is CfirPCLAInferenceSession) {
            // For type variables not based on type parameters (created for lambda parameters with no expected type)
            // we force them to be fixed before lambda analysis.
            //
            // Otherwise, it's a type variable based on a type parameter which resulting type might be inferred from the lambda body,
            // so in that case leave type variable type
            return typeConstructor.originalTypeParameter == null
        }

        return true
    }
}

private fun Candidate.isFunctionForExpectTypeFromCastFeature(): Boolean {
    if (typeArgumentMapping != TypeArgumentMapping.NoExplicitArguments) return false
    val fir = symbol.fir as? CfirFunction ?: return false

    return fir.isFunctionForExpectTypeFromCastFeature()
}

// Expect type is only being added to calls in a position of cast argument: foo() as R
// And that call should be resolved to something materialize()-like: it returns its single generic parameter and doesn't have value parameters
// fun <T> materialize(): T
internal fun CfirFunction.isFunctionForExpectTypeFromCastFeature(): Boolean {
    val typeParameter = typeParameters.singleOrNull() ?: return false

    val returnType = returnTypeRef.coneTypeSafe<ConeCangJieType>() ?: return false

    if ((returnType.unwrap() as? ConeTypeParameterType)?.lookupTag != typeParameter.symbol.toLookupTag()) return false

    fun CfirTypeRef.isBadType() =
        coneTypeSafe<ConeCangJieType>()
            ?.contains { (it.unwrap() as? ConeTypeParameterType)?.lookupTag == typeParameter.symbol.toLookupTag() } != false

    if (valueParameters.any { it.returnTypeRef.isBadType() } || receiverParameter?.typeRef?.isBadType() == true) return false

    return true
}

private fun ConeCangJieType.unwrap(): ConeSimpleCangJieType = lowerBoundIfFlexible().let {
    when (it) {
        is ConeDefinitelyNotNullType -> it.original.unwrap()
        is ConeSimpleCangJieType -> it
    }
}
