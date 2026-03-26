package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirLookupTrackerComponent
import org.cangnova.cangjie.cfir.CfirResolvable
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferValueParameterType
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.lookupTracker
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformerDispatcher
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedLambdaAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeSimpleLeafResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.stages.TypeArgumentMapping
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.cangnova.cangjie.cfir.resolve.initialTypeOfCandidate
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirPCLAInferenceSession
import org.cangnova.cangjie.cfir.resolve.typeFromCallee
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.types.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.coneTypeSafe
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.typeApproximator
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.resolve.calls.inference.addEqualityConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.fakeElement
import org.cangnova.cangjie.type.model.isUnit
import org.cangnova.cangjie.type.model.safeSubstitute
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import org.cangnova.cangjie.utils.runIf

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
        skipEvenPartialCompletion: Boolean = false,
    ): T where T : CfirResolvable, T : CfirExpression {

        val type = components.typeFromCallee(call)



        val reference = call.calleeReference as? CfirNamedReferenceWithCandidate ?: return call
        val candidate = reference.candidate
        val initialType = type.initialTypeOfCandidate(candidate)

        session.lookupTracker?.recordTypeResolveAsLookup(initialType, call.source, components.context.file.source)
        addConstraintFromExpectedType(candidate, initialType, resolutionMode)

        if (skipEvenPartialCompletion) return call

        val completionMode = candidate.computeCompletionMode(
            session.inferenceComponents,
            resolutionMode,
            initialType,
        )
        val analyzer = createPostponedArgumentsAnalyzer(transformer.resolutionContext)

        return when (completionMode) {
            ConstraintSystemCompletionMode.FULL -> {
                runCompletionForCall(candidate, completionMode, call, initialType, analyzer)
                val finalSubstitutor = candidate.system.asReadOnlyStorage()
                    .buildAbstractResultingSubstitutor(session.typeContext)
                    .asCone()
                call.transformSingle(createCompletionResultsWriter(finalSubstitutor), null)
            }

            ConstraintSystemCompletionMode.PARTIAL,
            ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL -> {
                runCompletionForCall(candidate, completionMode, call, initialType, analyzer)
                inferenceSession.processPartiallyResolvedCall(call, resolutionMode, completionMode)
                if (candidate.isSyntheticCallForTopLevelLambda()) {
                    val finalSubstitutor = candidate.system.currentStorage()
                        .buildCurrentSubstitutor(session.typeContext, emptyMap())
                        .asCone()
                    call.transformSingle(createCompletionResultsWriter(finalSubstitutor), null)
                } else {
                    call
                }
            }

            @OptIn(ConstraintSystemCompletionMode.ExclusiveForOverloadResolutionByLambdaReturnType::class)
            ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA -> error("Unexpected completion mode")
        }
    }

    private fun Candidate.isSyntheticCallForTopLevelLambda(): Boolean =
        callInfo.callSite is CfirAnonymousFunctionExpression

    private fun addConstraintFromExpectedType(
        candidate: Candidate,
        initialType: ConeCangJieType,
        resolutionMode: ResolutionMode,
    ) {
        if (resolutionMode !is ResolutionMode.WithExpectedType) return
        val expectedType = resolutionMode.expectedType.fullyExpandedType()
        val system = candidate.system

        when {
            resolutionMode.fromCast -> {
                if (candidate.isFunctionForExpectTypeFromCastFeature()) {
                    system.addSubtypeConstraint(initialType, expectedType, ConeExpectedTypeConstraintPosition)
                }
            }

            resolutionMode.forceFullCompletion &&
                candidate.isSyntheticFunctionCallThatShouldUseEqualityConstraint(expectedType) -> {
                system.addEqualityConstraintIfCompatible(initialType, expectedType, ConeExpectedTypeConstraintPosition)
                candidate.markWasExpectedTypeAddedAsEqualityForSyntheticCall()
            }

            with(session.typeContext) { expectedType.isUnit() } && resolutionMode.lastStatementInBlock -> {
                if (system.notFixedTypeVariables.isEmpty()) return
                system.addEqualityConstraintIfCompatible(initialType, expectedType, ConeExpectedTypeConstraintPosition)
            }

            else -> {
//                system.addSubtypeConstraintIfCompatible(initialType, expectedType, ConeExpectedTypeConstraintPosition)
                system.addSubtypeConstraint(initialType, expectedType, ConeExpectedTypeConstraintPosition)
            }
        }
    }

    private fun Candidate.isSyntheticFunctionCallThatShouldUseEqualityConstraint(
        expectedType: ConeCangJieType,
    ): Boolean {
        if (components.context.isInsideAssignmentRhs) return false
        val symbol = symbol as? CfirCallableSymbol ?: return false
        return symbol.origin == CfirDeclarationOrigin.Synthetic.FakeFunction && !expectedType.isUnitOrAny()
    }

    private fun ConeCangJieType.isUnitOrAny(): Boolean {
        return with(session.typeContext) {
            this@isUnitOrAny.isUnit() || this@isUnitOrAny == ConeAnyType
        }
    }

    fun <T> runCompletionForCall(
        candidate: Candidate,
        completionMode: ConstraintSystemCompletionMode,
        call: T,
        initialType: ConeCangJieType,
        analyzer: PostponedArgumentsAnalyzer? = null,
    ) where T : CfirExpression, T : CfirResolvable {
        val actualAnalyzer = analyzer ?: createPostponedArgumentsAnalyzer(transformer.resolutionContext)
        completer.complete(
            candidate.system.asConstraintSystemCompleterContext(),
            completionMode,
            listOf(ConeAtomWithCandidate(call, candidate)),
            initialType,
            transformer.resolutionContext,
        ) { atom, withPCLASession ->
            actualAnalyzer.analyze(candidate.system, atom, candidate, withPCLASession)
        }
    }

    fun prepareLambdaAtomForFactoryPattern(
        atom: ConeResolvedLambdaAtom,
        candidate: Candidate,
    ) {
        val expectedFunctionType = atom.expectedType as? ConeFuncType ?: return
        val returnVariable = ConeTypeVariableForLambdaReturnType(
            atom.anonymousFunction,
            PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE,
        )
        val csBuilder = candidate.system.getBuilder()
        csBuilder.registerVariable(returnVariable)

        val revisedExpectedType = ConeFuncType(
            parameterTypes = expectedFunctionType.parameterTypes,
            returnType = returnVariable.defaultType,
            isCFunc = expectedFunctionType.isCFunc,
            isClosureType = expectedFunctionType.isClosureType,
            hasVariableLenArg = expectedFunctionType.hasVariableLenArg,
            attributes = expectedFunctionType.attributes,
        )

        csBuilder.addSubtypeConstraint(
            revisedExpectedType,
            expectedFunctionType,
            ConeArgumentConstraintPosition(atom.anonymousFunction),
        )
        atom.replaceExpectedType(revisedExpectedType, returnVariable.defaultType)
        atom.replaceTypeVariableForLambdaReturnType(returnVariable)
    }

    fun createCompletionResultsWriter(
        substitutor: ConeSubstitutor,
        mode: CfirCallCompletionResultsWriterTransformer.Mode = CfirCallCompletionResultsWriterTransformer.Mode.Normal,
    ): CfirCallCompletionResultsWriterTransformer {
        return CfirCallCompletionResultsWriterTransformer(
            components.session,
            components.scopeSession,
            substitutor,
            components.returnTypeCalculator,
            components.session.typeApproximator,
            components.dataFlowAnalyzer,
            components.integerLiteralAndOperatorApproximationTransformer,
            components.samResolver,
            components.context,
            mode,
        )
    }

    fun completedResultType(candidate: Candidate): ConeCangJieType = candidate.substitutedReturnType()

    fun createPostponedArgumentsAnalyzer(context: ResolutionContext): PostponedArgumentsAnalyzer {
        return PostponedArgumentsAnalyzer(
            context,
            LambdaAnalyzerImpl(),
            session.inferenceComponents,
            transformer.components.callResolver,
        )
    }

    private inner class LambdaAnalyzerImpl : LambdaAnalyzer {
        override fun analyzeAndGetLambdaReturnArguments(
            lambdaAtom: ConeResolvedLambdaAtom,
            parameters: List<ConeCangJieType>,
            expectedReturnType: ConeCangJieType?,
            candidate: Candidate,
            withPCLASession: Boolean,
            forOverloadByLambdaReturnType: Boolean,
        ): ReturnArgumentsAnalysisResult {
            val lambda = lambdaAtom.anonymousFunction
            lambda.replaceMatchingParameterFunctionType(lambdaAtom.expectedType)
            rewriteLambdaParameterTypes(lambda.valueParameters, parameters, candidate, withPCLASession)

            if (expectedReturnType != null) {
                lambda.replaceReturnTypeRef(
                    lambda.returnTypeRef.resolvedTypeFromPrototype(
                        expectedReturnType,
                        lambda.source?.fakeElement(CjFakeSourceElementKind.ImplicitTypeRef),
                    ),
                )
            }

            val expectedFunctionType = ConeFuncType(
                parameterTypes = parameters,
                returnType = expectedReturnType ?: lambdaAtom.returnType,
            )
            val resolutionMode = org.cangnova.cangjie.cfir.resolve.withExpectedType(expectedFunctionType)
            var additionalConstraints: ConstraintStorage? = null

            transformer.context.withAnonymousFunctionTowerDataContext(lambda.symbol) {
                val lambdaExpression = lambdaAtom.expression as CfirAnonymousFunctionExpression
                val pclaInferenceSession = runIf(withPCLASession) {
                    candidate.lambdasAnalyzedWithPCLA += lambda
                    CfirPCLAInferenceSession(candidate, session.inferenceComponents)
                }

                if (pclaInferenceSession != null) {
                    transformer.context.withInferenceSession(pclaInferenceSession) {
                        lambdaExpression.transform<CfirElement, ResolutionMode>(transformer, resolutionMode)
                    }
                } else {
                    additionalConstraints = transformer.context.inferenceSession.runLambdaCompletion(
                        candidate,
                        forOverloadByLambdaReturnType,
                    ) {
                        lambdaExpression.transform<CfirElement, ResolutionMode>(transformer, resolutionMode)
                    }
                }
            }
            transformer.context.dropContextForAnonymousFunction(lambda)

            val returnArguments = components.dataFlowAnalyzer
                .returnExpressionsOfAnonymousFunction(lambda)
                .map { returnInfo ->
                    val rawAtom = ConeResolutionAtom.createRawAtom(returnInfo.expression)
                    when {
                        expectedReturnType == null -> rawAtom
                        rawAtom is ConeAtomWithCandidate -> rawAtom
                        else -> ConeSimpleLeafResolutionAtom(returnInfo.expression, allowUnresolvedExpression = false)
                    }
                }

            return ReturnArgumentsAnalysisResult(returnArguments, additionalConstraints)
        }

        private fun rewriteLambdaParameterTypes(
            parameters: List<CfirValueParameter>,
            inferredTypes: List<ConeCangJieType>,
            candidate: Candidate,
            withPCLASession: Boolean,
        ) {
            parameters.forEachIndexed { index, parameter ->
                if (index >= inferredTypes.size) {
                    parameter.replaceReturnTypeRef(
                        buildErrorTypeRef {
                            diagnostic = ConeCannotInferValueParameterType(
                                parameter.symbol,
                                "Lambda or anonymous function has more parameters than expected",
                            )
                            source = parameter.source
                        },
                    )
                    return@forEachIndexed
                }

                val approximated = inferredTypes[index].approximateLambdaInputType(
                    parameter.symbol,
                    withPCLASession,
                    candidate,
                )
                val source = parameter.source?.fakeElement(CjFakeSourceElementKind.ImplicitReturnTypeOfLambdaValueParameter)
                val newTypeRef = if (parameter.returnTypeRef is CfirImplicitTypeRef) {
                    approximated.toResolvedTypeRef(source)
                } else {
                    parameter.returnTypeRef.resolvedTypeFromPrototype(approximated, source)
                }
                parameter.replaceReturnTypeRef(newTypeRef)
            }
        }
    }

    private fun ConeCangJieType.approximateLambdaInputType(
        valueParameter: CfirValueParameterSymbol?,
        isRootLambdaForPCLASession: Boolean,
        containingCandidate: Candidate,
    ): ConeCangJieType {
        if (useErrorTypeInsteadOfTypeVariableForParameterType(isRootLambdaForPCLASession)) {
            val diagnostic = valueParameter?.let {
                ConeCannotInferValueParameterType(
                    it,
                    isTopLevelLambda = containingCandidate.isSyntheticCallForTopLevelLambda(),
                )
            } ?: ConeCannotInferValueParameterType(null, "Cannot infer parameter type")
            return ConeErrorType(diagnostic)
        }

        return session.typeApproximator.approximateToSuperType(
            this,
            TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2,
        ) ?: this
    }

    private fun ConeCangJieType.useErrorTypeInsteadOfTypeVariableForParameterType(
        isRootLambdaForPCLASession: Boolean,
    ): Boolean {
        if (this !is org.cangnova.cangjie.cfir.types.ConeTypeVariableType) return false
        if (isRootLambdaForPCLASession || inferenceSession is CfirPCLAInferenceSession) {
            return typeConstructor.originalTypeParameter == null
        }
        return true
    }
}

private fun Candidate.isFunctionForExpectTypeFromCastFeature(): Boolean {
    if (typeArgumentMapping != TypeArgumentMapping.NoExplicitArguments) return false
    val cfir = symbol.cfir as? CfirFunction ?: return false
    return cfir.isFunctionForExpectTypeFromCastFeature()
}

internal fun CfirFunction.isFunctionForExpectTypeFromCastFeature(): Boolean {
    val typeParameter = typeParameters.singleOrNull() ?: return false
    val returnType = returnTypeRef.coneTypeSafe<ConeCangJieType>() ?: return false

    if ((returnType.unwrap() as? ConeTypeParameterType)?.lookupTag != typeParameter.symbol.toLookupTag()) {
        return false
    }

    fun CfirTypeRef.isBadType(): Boolean {
        return coneTypeSafe<ConeCangJieType>()
            ?.contains {
                (it.unwrap() as? ConeTypeParameterType)?.lookupTag == typeParameter.symbol.toLookupTag()
            } != false
    }

    return valueParameters.none { it.returnTypeRef.isBadType() }
}

private fun CfirLookupTrackerComponent.recordTypeResolveAsLookup(
    type: ConeCangJieType,
    source: CjSourceElement?,
    fileSource: CjSourceElement?,
) = Unit

private fun CfirTypeRef.resolvedTypeFromPrototype(
    type: ConeCangJieType,
    source: CjSourceElement?,
): CfirResolvedTypeRef {
    return buildResolvedTypeRef {
        this.source = source ?: this@resolvedTypeFromPrototype.source
        coneType = type
        delegatedTypeRef = this@resolvedTypeFromPrototype
    }
}

private fun ConeCangJieType.toResolvedTypeRef(source: CjSourceElement?): CfirResolvedTypeRef {
    return buildResolvedTypeRef {
        this.source = source
        coneType = this@toResolvedTypeRef
    }
}

private fun ConeCangJieType.unwrap(): ConeSimpleCangJieType = this as ConeSimpleCangJieType
