package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirResolvable
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostic.ConeFunctionExpectedError
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostic.ConeFunctionCallExpectedError
import org.cangnova.cangjie.cfir.diagnostic.ConeNoConstructorError
import org.cangnova.cangjie.cfir.diagnostic.ConeResolutionToClassifierError
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedReferenceError
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.calls.resolvedQualifierClassifier
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirPropertyAccess
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccess
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.CollectionLiteralOuterCandidateContext
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.createConeDiagnosticForCandidateWithError
import org.cangnova.cangjie.cfir.resolve.doesResolutionResultOverrideOtherToPreserveCompatibility
import org.cangnova.cangjie.cfir.resolve.fullyExpandedClass
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirAllCandidatesCollector
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateCollector
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirErrorReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.createErrorReferenceWithErrorCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.createErrorReferenceWithExistingCandidate
import org.cangnova.cangjie.cfir.resolve.calls.overloads.ConeCallConflictResolver
import org.cangnova.cangjie.cfir.resolve.calls.overloads.callConflictResolverFactory
import org.cangnova.cangjie.cfir.resolve.calls.tower.CfirTowerGroup
import org.cangnova.cangjie.cfir.resolve.inference.inferenceComponents
import org.cangnova.cangjie.cfir.resolve.typeFromCallee
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassStaticScope
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.AbstractCandidate
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.utils.runIf
import kotlin.text.get

class CfirCallResolver(
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val towerResolver: CfirTowerResolver =
        CfirTowerResolver(components, components.resolutionStageRunner),
) : SessionHolder {

    override val session: CfirSession get() = components.session

    private lateinit var transformer: CfirExpressionsResolveTransformer

    fun initTransformer(transformer: CfirExpressionsResolveTransformer) {
        this.transformer = transformer
    }

    val conflictResolver: ConeCallConflictResolver =
        session.callConflictResolverFactory.create(session.inferenceComponents, components)

    @ApplicabilityDetail
    private val ResolutionResult.isSuccess: Boolean
        get() = applicability.isSuccess

    fun resolveCallAndSelectCandidate(
        functionCall: CfirFunctionCall,
        resolutionMode: ResolutionMode,
        collectionLiteralContext: CollectionLiteralOuterCandidateContext? = null,
    ): CfirFunctionCall {
        val callee = functionCall.calleeReference as? CfirNamedReference ?: return functionCall
        val result = collectCandidates(
            qualifiedAccess = functionCall,
            name = callee.name,
            origin = functionCall.origin,
            resolutionMode = resolutionMode,
            collectionLiteralContext = collectionLiteralContext,
        )

        var forceCandidates: Collection<Candidate>? = null
        if (result.candidates.isEmpty()) {
            val newResult = collectCandidates(
                functionCall,
                callee.name,
                CallKind.VariableAccess,
                origin = functionCall.origin,
                resolutionMode = resolutionMode,
            )
            if (newResult.candidates.isNotEmpty()) {
                forceCandidates = newResult.candidates
            }
        }

        val matchedClassifier = if (result.candidates.isEmpty() && forceCandidates == null) {
            findClassifierForCall(functionCall, callee.name)
        } else {
            null
        }

        val nameReference = createResolvedNamedReference(
            callee,
            callee.name,
            result.info,
            result.candidates,
            result.applicability,
            functionCall.explicitReceiver,
            matchedClassifier = matchedClassifier,
            expectedCallKind = if (forceCandidates != null) CallKind.VariableAccess else null,
            expectedCandidates = forceCandidates
        )

        functionCall.replaceCalleeReference(nameReference)
        val candidate = (nameReference as? CfirNamedReferenceWithCandidate)?.candidate
        candidate?.updateSourcesOfReceivers()

        return functionCall
    }

    fun <T> resolveVariableAccessAndSelectCandidate(
        qualifiedAccess: T,
        resolutionMode: ResolutionMode,
        forceCallKind: CallKind? = null,
        isUsedAsGetClassReceiver: Boolean = false,
        callSite: CfirElement = qualifiedAccess,
    ): T where T : CfirExpression, T : CfirResolvable {
        val callee = qualifiedAccess.calleeReference as? CfirNamedReference ?: return qualifiedAccess

        var result = collectCandidates(
            qualifiedAccess = qualifiedAccess,
            name = callee.name,
            forceCallKind = forceCallKind,
            isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
            callSite = callSite,
            resolutionMode = resolutionMode,
        )

        var functionCallExpected = false
        if (result.candidates.isEmpty() && forceCallKind == null && qualifiedAccess !is CfirFunctionCall) {
            val newResult = collectCandidates(
                qualifiedAccess = qualifiedAccess,
                name = callee.name,
                forceCallKind = CallKind.Function,
                isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
                callSite = callSite,
                resolutionMode = resolutionMode,
            )
            if (newResult.candidates.isNotEmpty()) {
                result = newResult
                functionCallExpected = true
            }
        }

        if (result.candidates.isEmpty()) {
            val classifierFromQualifier = qualifiedAccess.explicitReceiverOrNull()?.let {
                findClassifierInQualifierScope(it, callee.name)
            }
            if (classifierFromQualifier != null) {
                qualifiedAccess.replaceCalleeReference(
                    buildResolvedNamedReference {
                        source = callee.source
                        name = callee.name
                        resolvedSymbol = classifierFromQualifier
                    }
                )
                return qualifiedAccess
            }
        }

        if (result.candidates.isEmpty() && qualifiedAccess.explicitReceiverOrNull() == null) {
            val classifier = towerResolver.findClassifiers(callee.name).firstOrNull()
            if (classifier != null) {
                qualifiedAccess.replaceCalleeReference(
                    buildResolvedNamedReference {
                        source = callee.source
                        name = callee.name
                        resolvedSymbol = classifier
                    }
                )
                return qualifiedAccess
            }
        }

        val nameReference = createResolvedNamedReference(
            callee,
            callee.name,
            result.info,
            result.candidates,
            result.applicability,
            qualifiedAccess.explicitReceiverOrNull(),
            expectedCallKind = if (functionCallExpected) CallKind.Function else null
        )

        qualifiedAccess.replaceCalleeReference(nameReference)
        if (result.candidates.size == 1) {
            val candidate = result.candidates.single()
            candidate.updateSourcesOfReceivers()
            // 存储类型信息（如果需要）
        }

        return qualifiedAccess
    }

    fun collectAllCandidates(
        qualifiedAccess: CfirQualifiedAccess,
        name: Name,
        containingDeclarations: List<CfirDeclaration> = transformer.components.containingDeclarations,
        resolutionContext: ResolutionContext = transformer.resolutionContext,
        resolutionMode: ResolutionMode,
    ): List<OverloadCandidate> {
        val collector = AllCandidatesCollector(components, components.resolutionStageRunner)
        var result = collectCandidates(
            qualifiedAccess = qualifiedAccess,
            name = name,
            containingDeclarations = containingDeclarations,
            resolutionContext = resolutionContext,
            collector = collector,
            resolutionMode = resolutionMode,
        )

        if (result.candidates.isEmpty() && qualifiedAccess !is CfirFunctionCall) {
            val functionResult = collectCandidates(
                qualifiedAccess = qualifiedAccess,
                name = name,
                forceCallKind = CallKind.Function,
                containingDeclarations = containingDeclarations,
                resolutionContext = resolutionContext,
                collector = collector,
                resolutionMode = resolutionMode,
            )
            if (functionResult.candidates.isNotEmpty()) {
                result = functionResult
            }
        }

        return collector.allCandidates.map { candidate ->
            OverloadCandidate(candidate, isInBestCandidates = candidate in result.candidates)
        }
    }

    private fun collectCandidates(
        qualifiedAccess: CfirExpression,
        name: Name,
        forceCallKind: CallKind? = null,
        isUsedAsGetClassReceiver: Boolean = false,
        origin: CfirFunctionCallOrigin = CfirFunctionCallOrigin.Regular,
        containingDeclarations: List<CfirDeclaration> = transformer.components.containingDeclarations,
        resolutionContext: ResolutionContext = transformer.resolutionContext,
        collector: CfirCandidateCollector? = null,
        callSite: CfirElement = qualifiedAccess,
        resolutionMode: ResolutionMode,
        collectionLiteralContext: CollectionLiteralOuterCandidateContext? = null,
    ): ResolutionResult {
        val explicitReceiver = qualifiedAccess.explicitReceiverOrNull()
        val arguments = (qualifiedAccess as? CfirFunctionCall)?.arguments ?: emptyList()
        val typeArguments = qualifiedAccess.typeArgumentsOrEmpty(forceCallKind)

        val callKind = when {
            forceCallKind != null -> forceCallKind
            collectionLiteralContext != null -> CallKind.Function
            qualifiedAccess is CfirFunctionCall -> CallKind.Function
            else -> CallKind.VariableAccess
        }

        val info = CallInfo(
            callSite = callSite,
            callKind = callKind,
            name = name,
            origin = origin,
            explicitReceiver = explicitReceiver,
            arguments = arguments,
            isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
            typeArguments = typeArguments,
            session = session,
            containingFile = components.file,
            containingDeclarations = containingDeclarations,
            resolutionMode = resolutionMode,
            containingCandidateForCollectionLiteral = collectionLiteralContext?.containingCandidate,
        )

        val resultCollector = towerResolver.runResolver(info, resolutionContext, collector)
        val (reducedCandidates, applicability) = reduceCandidates(resultCollector)

        return ResolutionResult(
            info = info,
            applicability = applicability,
            candidates = reducedCandidates,
            forwardedDiagnostics = resultCollector.forwardedDiagnostics(),
        )
    }

    private fun reduceCandidates(
        collector: CfirCandidateCollector,
        resolutionContext: ResolutionContext = transformer.resolutionContext,
    ): Pair<Set<Candidate>, CandidateApplicability> {
        return reduceCollectedCandidates(
            candidates = collector.bestCandidates(),
            collectorApplicability = collector.currentApplicability,
            isCandidateSuccessful = Candidate::isSuccessful,
            candidateApplicability = Candidate::lowestApplicability,
            fullyProcessCandidate = { candidate ->
                components.resolutionStageRunner.fullyProcessCandidate(candidate, resolutionContext)
            },
            chooseMostSpecific = { candidates ->
                candidates.singleOrNull()?.let(::setOf)
                    ?: conflictResolver.chooseMaximallySpecificCandidates(candidates)
            },
        )
    }

    private fun createResolvedNamedReference(
        reference: CfirReference,
        name: Name,
        callInfo: CallInfo,
        candidates: Collection<Candidate>,
        applicability: CandidateApplicability,
        explicitReceiver: CfirExpression? = null,
        createResolvedReferenceWithoutCandidateForLocalVariables: Boolean = true,
        matchedClassifier: CfirClassLikeSymbol<*>? = null,
        expectedCallKind: CallKind? = null,
        expectedCandidates: Collection<Candidate>? = null,
    ): CfirNamedReference {
        val source = reference.source
        val operatorToken = runIf(callInfo.origin == CfirFunctionCallOrigin.Operator) {
            OperatorNameConventions.TOKENS_BY_OPERATOR_NAME[name]
        }

        // 根据期望的调用种类生成诊断
        val diagnostic = when {
            expectedCallKind != null -> when (expectedCallKind) {
                CallKind.Function -> {
                    val hasValueParameters = candidates.any {
                        (it.symbol as? CfirFunctionSymbol<*>)?.valueParameterSymbols?.isNotEmpty() == true
                    }
                    ConeFunctionCallExpectedError(name, hasValueParameters, candidates as Collection<AbstractCallCandidate<*>>)
                }
                else -> {
                    val singleExpectedCandidate = expectedCandidates?.singleOrNull()
                    var symbol = singleExpectedCandidate?.symbol
                    if (symbol is CfirTypeAliasSymbol) {
                        symbol = symbol.fullyExpandedClass(session) ?: symbol
                    }

                    when (symbol) {
                        is CfirClassLikeSymbol<*> -> {
                            ConeResolutionToClassifierError(singleExpectedCandidate!!, symbol)
                        }
                        else -> {
                            val receiverType = explicitReceiver?.coneTypeOrNull
                            when {
                                receiverType != null && !receiverType.isUnit -> {
                                    val declarationType = (symbol as? CfirCallableSymbol<*>)?.let {
                                        components.returnTypeCalculator.tryCalculateReturnType(it.cfir).coneType
                                    }

                                    if (singleExpectedCandidate?.isSuccessful == false && declarationType is ConeFuncType) {
                                        createConeDiagnosticForCandidateWithError(
                                            singleExpectedCandidate.lowestApplicability,
                                            singleExpectedCandidate,
                                        )
                                    } else {
                                        ConeFunctionExpectedError(
                                            name.asString(),
                                            declarationType ?: receiverType,
                                        )
                                    }
                                }
                                singleExpectedCandidate != null && !singleExpectedCandidate.isSuccessful -> {
                                    createConeDiagnosticForCandidateWithError(
                                        singleExpectedCandidate.lowestApplicability,
                                        singleExpectedCandidate,
                                    )
                                }
                                else -> ConeUnresolvedNameError(name, operatorToken)
                            }
                        }
                    }
                }
            }

            candidates.isEmpty() -> {
                when {
                    matchedClassifier != null && callInfo.callKind == CallKind.Function -> ConeNoConstructorError
                    name.asString() == "invoke" && explicitReceiver is CfirLiteralExpression ->
                        ConeFunctionExpectedError(
                            explicitReceiver.value?.toString() ?: "",
                            explicitReceiver.coneTypeOrNull ?: components.typeFromCallee(reference),
                        )
                    else -> {
                        val receiverType = explicitReceiver?.coneTypeOrNull
                        when {
                            receiverType is ConeClassLikeType && receiverType.isInterface -> ConeNoConstructorError
                            else -> ConeUnresolvedNameError(name, operatorToken, receiverType)
                        }
                    }
                }
            }

            candidates.size > 1 -> {
                val candidatesWithErrors = candidates.associateWith {
                    runIf(!it.isSuccessful) { createConeDiagnosticForCandidateWithError(it.applicability, it) }
                }
                ConeAmbiguityError(name, applicability, candidatesWithErrors as Map<AbstractCandidate, ConeDiagnostic?>)
            }

            else -> {
                val candidate = candidates.single()
                runIf(!candidate.isSuccessful) {
                    createConeDiagnosticForCandidateWithError(applicability, candidate)
                }
            }
        }

        if (diagnostic != null) {
            return createErrorReferenceForSingleCandidate(
                candidates.singleOrNull(),
                diagnostic as ConeDiagnostic,
                callInfo,
                source,
            )
        }

        // 成功的候选
        val candidate = candidates.single()

        // 优化：对于没有类型参数的局部变量或属性，直接创建已解析引用而不保留候选
        if (candidate.usedOuterCs == false && explicitReceiver?.coneTypeOrNull !is ConeIdealLiteralType &&
            (candidate.symbol as? CfirCallableSymbol<*>)?.let {
                it is CfirVariableSymbol && (it !is CfirPropertySymbol || it.cfir.typeParameters.isEmpty())
            } == true &&
                createResolvedReferenceWithoutCandidateForLocalVariables &&
                !candidate.doesResolutionResultOverrideOtherToPreserveCompatibility()
        ) {
            return buildResolvedNamedReference {
                this.source = source
                this.name = name
                resolvedSymbol = candidate.symbol
            }
        }

        return CfirNamedReferenceWithCandidate(source, name, candidate)
    }

    private fun findClassifierForCall(
        qualifiedAccess: CfirExpression,
        name: Name,
    ): CfirClassLikeSymbol<*>? {
        val explicitReceiver = qualifiedAccess.explicitReceiverOrNull()
        return if (explicitReceiver != null) {
            findClassifierInQualifierScope(explicitReceiver, name)
        } else {
            towerResolver.findClassifiers(name).firstOrNull()
        }
    }

    private fun findClassifierInQualifierScope(
        receiver: CfirExpression,
        name: Name,
    ): CfirClassLikeSymbol<*>? {
        val qualifierClassifier = receiver.resolvedQualifierClassifier(session) ?: return null
        val declaration = qualifierClassifier.cfir as? CfirClassLikeDeclaration ?: return null
        val staticScope = CfirClassStaticScope(declaration)
        var result: CfirClassLikeSymbol<*>? = null
        staticScope.processClassifiersByName(name) { classifier ->
            if (result == null) {
                result = classifier
            }
        }
        return result
    }

    private fun createErrorReferenceForSingleCandidate(
        candidate: Candidate?,
        diagnostic: ConeDiagnostic,
        callInfo: CallInfo,
        source: org.cangnova.cangjie.source.CjSourceElement?,
    ): CfirNamedReference {
        return if (candidate != null) {
            createErrorReferenceWithExistingCandidate(
                candidate = candidate,
                diagnostic = diagnostic,
                source = source,
                resolutionContext = transformer.resolutionContext,
                resolutionStageRunner = components.resolutionStageRunner,
            )
        } else {
            createErrorReferenceWithErrorCandidate(
                callInfo = callInfo,
                diagnostic = diagnostic,
                source = source,
                resolutionContext = transformer.resolutionContext,
                resolutionStageRunner = components.resolutionStageRunner,
            )
        }
    }

    private fun CfirExpression.explicitReceiverOrNull() = when (this) {
        is CfirFunctionCall -> explicitReceiver
        is CfirQualifiedAccess -> explicitReceiver
        is CfirPropertyAccess -> explicitReceiver
        else -> null
    }

    private fun CfirExpression.typeArgumentsOrEmpty(forceCallKind: CallKind?): List<org.cangnova.cangjie.cfir.types.CfirTypeRef> = when {
        this is CfirFunctionCall -> typeArguments
        this is CfirQualifiedAccess && forceCallKind != null -> typeArguments
        else -> emptyList()
    }

    private data class ResolutionResult(
        val info: CallInfo,
        val applicability: CandidateApplicability,
        val candidates: Collection<Candidate>,
        val forwardedDiagnostics: List<ResolutionDiagnostic>,
    )
}

/** A candidate in the overload candidate set. */
data class OverloadCandidate(val candidate: Candidate, val isInBestCandidates: Boolean)

@OptIn(ApplicabilityDetail::class)
internal fun <T> reduceCollectedCandidates(
    candidates: Collection<T>,
    collectorApplicability: CandidateApplicability,
    isCandidateSuccessful: (T) -> Boolean,
    candidateApplicability: (T) -> CandidateApplicability,
    fullyProcessCandidate: (T) -> Unit,
    chooseMostSpecific: (Set<T>) -> Set<T>,
): Pair<Set<T>, CandidateApplicability> {
    if (candidates.isEmpty()) {
        return emptySet<T>() to collectorApplicability
    }

    val candidateSet = candidates.toSet()
    if (collectorApplicability.isSuccess) {
        return chooseMostSpecific(candidateSet) to collectorApplicability
    }

    if (candidateSet.size == 1) {
        val candidate = candidateSet.single()
        fullyProcessCandidate(candidate)
        return setOf(candidate) to normalizeReductionApplicability(
            isSuccessful = isCandidateSuccessful(candidate),
            applicability = candidateApplicability(candidate),
        )
    }

    val groupedByApplicability = candidateSet.groupBy { candidate ->
        fullyProcessCandidate(candidate)
        normalizeReductionApplicability(
            isSuccessful = isCandidateSuccessful(candidate),
            applicability = candidateApplicability(candidate),
        )
    }

    val selectedGroup = groupedByApplicability.maxBy { it.key }
    return chooseMostSpecific(selectedGroup.value.toSet()) to selectedGroup.key
}

@OptIn(ApplicabilityDetail::class)
private fun normalizeReductionApplicability(
    isSuccessful: Boolean,
    applicability: CandidateApplicability,
): CandidateApplicability {
    if (isSuccessful || !applicability.isSuccess) {
        return applicability
    }

    return CandidateApplicability.RESOLVED_WITH_ERROR
}

class AllCandidatesCollector(
    components: BodyResolveComponents,
    resolutionStageRunner: ResolutionStageRunner
) : CfirAllCandidatesCollector(
    components as CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    resolutionStageRunner
) {
    private val allCandidatesMap = mutableMapOf<CfirSymbol<*>, Candidate>()

    override fun consumeCandidate(
        group: CfirTowerGroup,
        candidate: Candidate,
        context: ResolutionContext
    ): CandidateApplicability {
        allCandidatesMap.getOrPut(candidate.symbol) { candidate }
        return super.consumeCandidate(group, candidate, context)
    }

    override fun shouldStopAtTheGroup(group: CfirTowerGroup): Boolean = false

    val allCandidates: Collection<Candidate>
        get() = allCandidatesMap.values
}
