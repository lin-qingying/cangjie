package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostic.ConeFunctionExpectedError
import org.cangnova.cangjie.cfir.diagnostic.ConeFunctionCallExpectedError
import org.cangnova.cangjie.cfir.diagnostic.ConeNoConstructorError
import org.cangnova.cangjie.cfir.diagnostic.ConeResolutionToClassifierError
import org.cangnova.cangjie.cfir.diagnostic.ConeHiddenCandidateError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedError
import org.cangnova.cangjie.cfir.calls.resolvedQualifierClassifier
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.unwrapSmartcastExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
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
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CandidateFactory
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirAllCandidatesCollector
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateCollector
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.createErrorReferenceWithErrorCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.createErrorReferenceWithExistingCandidate
import org.cangnova.cangjie.cfir.resolve.calls.overloads.ConeCallConflictResolver
import org.cangnova.cangjie.cfir.resolve.calls.overloads.callConflictResolverFactory
import org.cangnova.cangjie.cfir.resolve.calls.tower.CfirTowerGroup
import org.cangnova.cangjie.cfir.resolve.inference.inferenceComponents
import org.cangnova.cangjie.cfir.resolve.typeFromCallee
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassStaticScope
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.AbstractCandidate
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.utils.runIf

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
        val isCollectionLiteralCall = collectionLiteralContext != null
        val result = collectCandidates(
            qualifiedAccess = functionCall,
            name = callee.name,
            origin = functionCall.origin,
            resolutionMode = resolutionMode,
            collectionLiteralContext = collectionLiteralContext,
        )

        var effectiveResult = result
        var expectedCallKind: CallKind? = null
        var expectedCandidates: Collection<Candidate>? = null
        var matchedClassifier: CfirClassLikeSymbol<*>? = null
        if (result.candidates.isEmpty() && !isCollectionLiteralCall) {
            // 阶段2a：普通函数搜索为空时，先尝试枚举构造器搜索（对齐官方两阶段语义：普通函数完全遮蔽枚举构造器）
            val enumResult = collectCandidates(
                functionCall,
                callee.name,
                CallKind.EnumConstructorCall,
                origin = functionCall.origin,
                resolutionMode = resolutionMode,
            )
            if (enumResult.candidates.isNotEmpty()) {
                effectiveResult = enumResult
            } else {
                // 阶段2b：枚举构造器也未找到，fallback 到 NamedValueAccess（无参枚举作为值访问等场景）
                val variableAccessResult = collectCandidates(
                    functionCall,
                    callee.name,
                    CallKind.NamedValueAccess,
                    origin = functionCall.origin,
                    resolutionMode = resolutionMode,
                )
                matchedClassifier = findClassifierForCall(functionCall, callee.name)
                val constructorResult = matchedClassifier?.let { classifier ->
                    collectClassConstructorCandidates(
                        functionCall = functionCall,
                        classifier = classifier,
                        resolutionMode = resolutionMode,
                    )
                }
                if (constructorResult != null && constructorResult.candidates.isNotEmpty()) {
                    effectiveResult = constructorResult
                    matchedClassifier = null
                } else if (variableAccessResult.candidates.isNotEmpty()) {
                    expectedCallKind = CallKind.NamedValueAccess
                    expectedCandidates = variableAccessResult.candidates
                }
            }
        }

        if (matchedClassifier == null && effectiveResult.candidates.isEmpty() && expectedCandidates == null) {
            matchedClassifier = findClassifierForCall(functionCall, callee.name)
        }

        val nameReference = createResolvedNamedReference(
            callee,
            callee.name,
            effectiveResult.info,
            effectiveResult.candidates,
            effectiveResult.applicability,
            functionCall.explicitReceiver,
            matchedClassifier = matchedClassifier,
            expectedCallKind = expectedCallKind,
            expectedCandidates = expectedCandidates,
        )

        functionCall.replaceCalleeReference(nameReference)
        val candidate = (nameReference as? CfirNamedReferenceWithCandidate)?.candidate
        candidate?.updateSourcesOfReceivers()
        return functionCall
    }

    fun resolveNamedValueAccessAndSelectCandidate(
        qualifiedAccess: CfirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        isUsedAsGetClassReceiver: Boolean,
        callSite: CfirElement,
        resolutionMode: ResolutionMode,
    ): CfirExpression {
        return resolveNamedValueAccessAndSelectCandidateImpl(
            qualifiedAccess = qualifiedAccess,
            isUsedAsReceiver = isUsedAsReceiver,
            resolutionMode = resolutionMode,
            isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
            callSite = callSite,
        ) { true }
    }

    @Deprecated(
        message = "Use resolveNamedValueAccessAndSelectCandidate instead",
        replaceWith = ReplaceWith(
            "resolveNamedValueAccessAndSelectCandidate(qualifiedAccess, isUsedAsReceiver, isUsedAsGetClassReceiver, callSite, resolutionMode)"
        ),
    )
    fun resolveVariableAccessAndSelectCandidate(
        qualifiedAccess: CfirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        isUsedAsGetClassReceiver: Boolean,
        callSite: CfirElement,
        resolutionMode: ResolutionMode,
    ): CfirExpression =
        resolveNamedValueAccessAndSelectCandidate(
            qualifiedAccess = qualifiedAccess,
            isUsedAsReceiver = isUsedAsReceiver,
            isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
            callSite = callSite,
            resolutionMode = resolutionMode,
        )

    @OptIn(ApplicabilityDetail::class)
    private fun resolveNamedValueAccessAndSelectCandidateImpl(
        qualifiedAccess: CfirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        resolutionMode: ResolutionMode,
        isUsedAsGetClassReceiver: Boolean,
        callSite: CfirElement = qualifiedAccess,
        acceptCandidates: (Collection<Candidate>) -> Boolean,
    ): CfirExpression {
        val callee = qualifiedAccess.calleeReference as? CfirNamedReference ?: return qualifiedAccess

        val transformedAccess = transformer.transformExplicitReceiverOf(qualifiedAccess)

        val basicResult by lazy(LazyThreadSafetyMode.NONE) {
            collectCandidates(
                qualifiedAccess = transformedAccess,
                name = callee.name,
                isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
                callSite = callSite,
                resolutionMode = resolutionMode,
            )
        }

        if (isUsedAsReceiver || !basicResult.isSuccess) {
            val classifierFromQualifier = transformedAccess.explicitReceiver?.let {
                findClassifierInQualifierScope(it, callee.name)
            }
            if (classifierFromQualifier != null) {
                transformedAccess.replaceCalleeReference(
                    buildResolvedNamedReference {
                        source = callee.source
                        name = callee.name
                        resolvedSymbol = classifierFromQualifier
                    }
                )
                return transformedAccess
            }
        }

        var result = basicResult

        if (transformedAccess.explicitReceiver == null) {
            if (!result.isSuccess || (isUsedAsReceiver && result.candidates.all { it.symbol is CfirClassLikeSymbol<*> })) {
                val classifier = towerResolver.findClassifiers(callee.name).firstOrNull()
                if (classifier != null) {
                    transformedAccess.replaceCalleeReference(
                        buildResolvedNamedReference {
                            source = callee.source
                            name = callee.name
                            resolvedSymbol = classifier
                        }
                    )
                    return transformedAccess
                }
            }
        }

        val shouldTryEnumValueAccess =
            !isUsedAsReceiver &&
                    transformedAccess !is CfirFunctionCall &&
                    (result.candidates.isEmpty() || result.candidates.all { it.symbol is CfirEnumConstructorSymbol })

        var functionCallExpected = false
        if (shouldTryEnumValueAccess) {
            // 先尝试枚举构造器（作为值访问，对应无参枚举构造器的直接引用）
            val enumResult = collectCandidates(
                qualifiedAccess = transformedAccess,
                name = callee.name,
                forceCallKind = CallKind.EnumConstructorCall,
                isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
                callSite = callSite,
                resolutionMode = resolutionMode,
            )
            if (enumResult.candidates.isNotEmpty()) {
                result = enumResult
            } else if (result.candidates.isEmpty()) {
                val newResult = collectCandidates(
                    qualifiedAccess = transformedAccess,
                    name = callee.name,
                    forceCallKind = CallKind.Function,
                    isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
                    callSite = callSite,
                    resolutionMode = resolutionMode,
                )
                if (newResult.candidates.isNotEmpty()) {
                    result = newResult
                    functionCallExpected = newResult.applicability > CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
                }
            }
        }

        val reducedCandidates = result.candidates
        if (!acceptCandidates(reducedCandidates)) return transformedAccess

        val nameReference = createResolvedNamedReference(
            reference = callee,
            name = callee.name,
            callInfo = result.info,
            candidates = reducedCandidates,
            applicability = result.applicability,
            explicitReceiver = transformedAccess.explicitReceiver,
            expectedCallKind = if (functionCallExpected) CallKind.Function else null,
        )

        transformedAccess.replaceCalleeReference(nameReference)
        if (reducedCandidates.size == 1) {
            val candidate = reducedCandidates.single()
            candidate.updateSourcesOfReceivers()
        }
        transformer.storeTypeFromCallee(transformedAccess)
        return transformedAccess
    }

    fun collectAllCandidates(
        qualifiedAccess: CfirQualifiedAccessExpression,
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
        qualifiedAccess: CfirQualifiedAccessExpression,
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
        val explicitReceiver = qualifiedAccess.explicitReceiver
        val arguments = (qualifiedAccess as? CfirFunctionCall)?.argumentList?.arguments ?: emptyList()
        val typeArguments = qualifiedAccess.typeArguments

        val callKind = when {
            forceCallKind != null -> forceCallKind
            collectionLiteralContext != null -> CallKind.Function
            qualifiedAccess is CfirFunctionCall -> CallKind.Function
            else -> CallKind.NamedValueAccess
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

        return collectCandidates(info = info, resolutionContext = resolutionContext, collector = collector)
    }

    private fun collectCandidates(
        info: CallInfo,
        resolutionContext: ResolutionContext,
        collector: CfirCandidateCollector? = null,
    ): ResolutionResult {
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
        val argumentTypes = callInfo.arguments.mapNotNull { it.coneTypeOrNull }

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
                                else -> ConeUnresolvedNameError(name, operatorToken, argumentTypes = argumentTypes)
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
                            else -> ConeUnresolvedNameError(name, operatorToken, receiverType, argumentTypes)
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
        val symbol = candidate.symbol
        val canDropCandidateForNamedValueAccess = when (symbol) {
            is CfirPropertySymbol -> symbol.cfir.typeParameters.isEmpty()
            is CfirVariableSymbol -> true
            else -> false
        }

        if (
            !candidate.usedOuterCs && callInfo.callKind == CallKind.NamedValueAccess && explicitReceiver?.coneTypeOrNull !is ConeIdealLiteralType && canDropCandidateForNamedValueAccess && createResolvedReferenceWithoutCandidateForLocalVariables && !candidate.doesResolutionResultOverrideOtherToPreserveCompatibility()
        ) {
            return buildResolvedNamedReference {
                this.source = source
                this.name = name
                resolvedSymbol = symbol
            }
        }

        return CfirNamedReferenceWithCandidate(source, name, candidate)
    }

    private fun collectClassConstructorCandidates(
        functionCall: CfirFunctionCall,
        classifier: CfirClassLikeSymbol<*>,
        resolutionMode: ResolutionMode,
    ): ResolutionResult {
        val actualClassifier = (classifier as? CfirTypeAliasSymbol)?.fullyExpandedClass(session) ?: classifier
        val constructorSymbols = actualClassifier.cfir.declarations
            .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirConstructor>()
            .map(CfirConstructor::symbol)
        if (constructorSymbols.isEmpty()) {
            return ResolutionResult(
                info = CallInfo(
                    callSite = functionCall,
                    callKind = CallKind.Function,
                    name = classifier.name,
                    origin = functionCall.origin,
                    explicitReceiver = functionCall.explicitReceiver,
                    arguments = functionCall.argumentList.arguments,
                    isUsedAsGetClassReceiver = false,
                    typeArguments = functionCall.typeArguments,
                    session = session,
                    containingFile = components.file,
                    containingDeclarations = transformer.components.containingDeclarations,
                    resolutionMode = resolutionMode,
                ),
                applicability = CandidateApplicability.HIDDEN,
                candidates = emptyList(),
                forwardedDiagnostics = emptyList(),
            )
        }

        val callInfo = CallInfo(
            callSite = functionCall,
            callKind = CallKind.Function,
            name = classifier.name,
            origin = functionCall.origin,
            explicitReceiver = functionCall.explicitReceiver,
            arguments = functionCall.argumentList.arguments,
            isUsedAsGetClassReceiver = false,
            typeArguments = functionCall.typeArguments,
            session = session,
            containingFile = components.file,
            containingDeclarations = transformer.components.containingDeclarations,
            resolutionMode = resolutionMode,
        )
        val candidateFactory = CandidateFactory(transformer.resolutionContext, callInfo)
        val constructorCandidates = constructorSymbols.map { constructorSymbol ->
            candidateFactory.createCandidate(
                callInfo = callInfo,
                symbol = constructorSymbol,
                originScope = null,
            )
        }
        val (reducedCandidates, applicability) = reduceCollectedCandidates(
            candidates = constructorCandidates,
            collectorApplicability = CandidateApplicability.RESOLVED,
            isCandidateSuccessful = Candidate::isSuccessful,
            candidateApplicability = Candidate::lowestApplicability,
            fullyProcessCandidate = { candidate ->
                components.resolutionStageRunner.fullyProcessCandidate(candidate, transformer.resolutionContext)
            },
            chooseMostSpecific = { currentCandidates ->
                currentCandidates.singleOrNull()?.let(::setOf)
                    ?: conflictResolver.chooseMaximallySpecificCandidates(currentCandidates)
            },
        )
        return ResolutionResult(
            info = callInfo,
            applicability = applicability,
            candidates = reducedCandidates,
            forwardedDiagnostics = emptyList(),
        )
    }

    private fun findClassifierForCall(
        qualifiedAccess: CfirQualifiedAccessExpression,
        name: Name,
    ): CfirClassLikeSymbol<*>? {
        val explicitReceiver = qualifiedAccess.explicitReceiver
        return if (explicitReceiver != null) {
            findClassifierInQualifierScope(explicitReceiver, name)
        } else {
            towerResolver.findClassifiers(name).firstOrNull() ?: resolveTopLevelClassifierByShortName(name)
        }
    }

    private fun resolveTopLevelClassifierByShortName(name: Name): CfirClassLikeSymbol<*>? {
        val file = components.file
        val packageCandidates = LinkedHashSet<ClassId>()
        val explicitImportCandidates = LinkedHashSet<ClassId>()

        findSameFileTopLevelClassifier(file, name)?.let { declaration ->
            return declaration.symbol
        }

        for (importInfo in file.imports) {
            val importedFqName = importInfo.importedFqName ?: continue
            if (importInfo.isAllUnder) {
                explicitImportCandidates += ClassId(importedFqName, name)
                continue
            }

            val importedName = importInfo.aliasName?.asString() ?: importedFqName.shortName().asString()
            if (importedName == name.asString()) {
                explicitImportCandidates += ClassId.topLevel(importedFqName)
            }
        }

        packageCandidates += ClassId(file.packageDirective.packageFqName, name)

        val defaultImportCandidates = LinkedHashSet<ClassId>()
        val defaultImportsProvider = session.defaultImportsProvider
        val defaultImports = defaultImportsProvider.getDefaultImports(includeLowPriorityImports = true)
            .filter { it.fqName !in defaultImportsProvider.excludedImports }
        addDefaultImportCandidates(defaultImportCandidates, defaultImports, name)

        return sequenceOf(
            packageCandidates,
            explicitImportCandidates,
            defaultImportCandidates,
        ).flatMap { it.asSequence() }
            .firstNotNullOfOrNull(components.symbolProvider::getClassLikeSymbolByClassId)
    }

    private fun findSameFileTopLevelClassifier(
        file: org.cangnova.cangjie.cfir.declarations.CfirFile,
        shortName: Name,
    ): CfirClassLikeDeclaration? {
        return file.declarations
            .asSequence()
            .filterIsInstance<CfirClassLikeDeclaration>()
            .filter { declaration -> declaration.name == shortName }
            .firstOrNull()
    }

    private fun addDefaultImportCandidates(
        candidates: MutableSet<ClassId>,
        imports: List<ImportPath>,
        shortName: Name,
    ) {
        val simpleName = shortName.asString()
        for (importPath in imports) {
            if (importPath.isAllUnder) {
                candidates += ClassId(importPath.fqName, shortName)
                continue
            }

            val importedName = importPath.alias?.asString() ?: importPath.fqName.shortName().asString()
            if (importedName == simpleName) {
                candidates += ClassId.topLevel(importPath.fqName)
            }
        }
    }

    private fun findClassifierInQualifierScope(
        receiver: CfirExpression,
        name: Name,
    ): CfirClassLikeSymbol<*>? {
        val qualifierClassifier = receiver.unwrapSmartcastExpression().resolvedQualifierClassifier(session) ?: return null
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
        if (candidate == null) {
            return createErrorReferenceWithErrorCandidate(
                callInfo = callInfo,
                diagnostic = diagnostic,
                source = source,
                resolutionContext = transformer.resolutionContext,
                resolutionStageRunner = components.resolutionStageRunner,
            )
        } else {
            return when (diagnostic) {
                is ConeUnresolvedError,
                is ConeHiddenCandidateError,
                -> createErrorReferenceWithErrorCandidate(
                    callInfo = callInfo,
                    diagnostic = diagnostic,
                    source = source,
                    resolutionContext = transformer.resolutionContext,
                    resolutionStageRunner = components.resolutionStageRunner,
                )

                else -> createErrorReferenceWithExistingCandidate(
                    candidate = candidate,
                    diagnostic = diagnostic,
                    source = source,
                    resolutionContext = transformer.resolutionContext,
                    resolutionStageRunner = components.resolutionStageRunner,
                )
            }
        }
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
