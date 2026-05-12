package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPatternCopy
import org.cangnova.cangjie.cfir.patterns.builder.buildEnumPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildEnumPatternCopy
import org.cangnova.cangjie.cfir.patterns.builder.buildOrPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildTuplePatternCopy
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.references.impl.CfirNamedReferenceImpl
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessAnalyzer
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.resolve.typeFromCallee
import org.cangnova.cangjie.cfir.resolve.withExpectedType
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostic.ConeCommandHandleTypeError
import org.cangnova.cangjie.cfir.diagnostic.ConeCommandIncompatibleTypeError
import org.cangnova.cangjie.cfir.diagnostic.ConeEffectsFeatureDisabledError
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostic.ConeImplicitResumeOutsideHandlerError
import org.cangnova.cangjie.cfir.diagnostic.ConeMismatchingHandleBlockError
import org.cangnova.cangjie.cfir.diagnostic.ConeNoMatchingInvokeOperatorError
import org.cangnova.cangjie.cfir.diagnostic.ConeOptionalChainNonOptionalError
import org.cangnova.cangjie.cfir.diagnostic.ConeResumeNoWithError
import org.cangnova.cangjie.cfir.diagnostic.ConeResumeThrowingMismatchTypeError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSpecificTypeResolverTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.resultType
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.whileAnalysing
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * Expression resolve transformer.
 *
 * Responsibility: compute and propagate expression types only.
 * This includes literals, accesses, calls, patterns, control-flow, and lambdas.
 *
 * Diagnostic reporting is intentionally NOT performed here. Resolution keeps
 * candidate diagnostics attached to the resolver/completion pipeline output,
 * and a dedicated checker pass reports them after body resolve completes.
 */
@OptIn(CfirImplementationDetail::class, ApplicabilityDetail::class)
open class CfirExpressionsResolveTransformer(
    transformer: CfirAbstractBodyResolveTransformerDispatcher,
) : CfirPartialBodyResolveTransformer(transformer) {
    private data class EffectHandlerContext(
        val commandResultType: ConeCangJieType,
    )

    private val builtinTypes get() = session.builtinTypes
    private val specificTypeResolverTransformer = CfirSpecificTypeResolverTransformer(session)
    private val callResolver get() = components.callResolver
    private val effectHandlerStack = ArrayDeque<EffectHandlerContext>()
    private fun errorType(
        reason: String,
        kind: DiagnosticKind = DiagnosticKind.Other,
        delegatedType: ConeCangJieType? = null,
    ): ConeErrorType = ConeErrorType(ConeSimpleDiagnostic(reason, kind), delegatedType = delegatedType)

    init {
        components.callResolver.initTransformer(this)
    }

    // ── Literals ─────────────────────────────────────────────────────────────

    override fun transformExpression(expression: CfirExpression, data: ResolutionMode): CfirExpression {
        if (!expression.hasResolvedType && expression !is CfirWrappedExpression) {
            expression.resultType = ConeErrorType(
                ConeSimpleDiagnostic(
                    "Type calculating for ${expression::class} is not supported",
                    DiagnosticKind.InferenceError
                )
            )
        }
        return (expression.transformChildren(transformer, data) as CfirExpression)
    }

    override fun transformWrappedExpression(
        wrappedExpression: CfirWrappedExpression,
        data: ResolutionMode,
    ): CfirExpression {
        wrappedExpression.transformChildren(transformer, data)
        wrappedExpression.replaceConeTypeOrNull(wrappedExpression.expression.coneTypeOrNull)
        components.dataFlowAnalyzer.exitWrappedExpression(wrappedExpression)
        return wrappedExpression
    }

    override fun transformOptionalExpression(
        optionalExpression: CfirOptionalExpression,
        data: ResolutionMode,
    ): CfirExpression {
        optionalExpression.transformChildren(transformer, data)
        optionalExpression.replaceConeTypeOrNull(optionalExpression.expression.coneTypeOrNull)
        return optionalExpression
    }

    override fun transformOptionalChainExpression(
        optionalChainExpression: CfirOptionalChainExpression,
        data: ResolutionMode,
    ): CfirExpression {
        components.dataFlowAnalyzer.enterOptionalChain(optionalChainExpression)
        optionalChainExpression.transformChildren(transformer, data)

        val chainRoot = optionalChainExpression.expression.optionalChainRootExpression()
        val rootType = chainRoot?.coneTypeOrNull
        if (rootType == null) {
            optionalChainExpression.replaceConeTypeOrNull(
                ConeErrorType(ConeSimpleDiagnostic("optional chain root type is unresolved", DiagnosticKind.InferenceError))
            )
            components.dataFlowAnalyzer.exitOptionalChain(optionalChainExpression)
            return optionalChainExpression
        }

        if (!rootType.isOption) {
            optionalChainExpression.replaceConeTypeOrNull(ConeErrorType(ConeOptionalChainNonOptionalError(rootType)))
            components.dataFlowAnalyzer.exitOptionalChain(optionalChainExpression)
            return optionalChainExpression
        }

        val liftedResultType = liftOptionalChainResultType(optionalChainExpression.expression.coneTypeOrNull)
        optionalChainExpression.replaceConeTypeOrNull(liftedResultType)
        components.dataFlowAnalyzer.exitOptionalChain(optionalChainExpression)
        return optionalChainExpression
    }

    private fun transformThisReceiverExpression(
        thisReceiverExpression: CfirThisReceiverExpression,
        data: ResolutionMode,
    ): CfirExpression {
        thisReceiverExpression.transformAnnotations(transformer, data)

        if (thisReceiverExpression.coneTypeOrNull == null) {
            val thisReference = thisReceiverExpression.calleeReference
            val resultType = components.typeFromCallee(thisReference)
            thisReceiverExpression.replaceConeTypeOrNull(resultType)
            thisReference.replaceDiagnostic((resultType as? ConeErrorType)?.diagnostic)

            if (thisReference.boundSymbol == null && resultType !is ConeErrorType) {
                components.implicitValueStorage[null].singleOrNull()?.let { implicitReceiver ->
                    thisReference.replaceBoundSymbol(implicitReceiver.boundSymbol)
                }
            }
        }

        return thisReceiverExpression
    }

    override fun transformSuperReceiverExpression(
        superReceiverExpression: CfirSuperReceiverExpression,
        data: ResolutionMode,
    ): CfirExpression {
        superReceiverExpression.transformAnnotations(transformer, data)

        val superReference = superReceiverExpression.calleeReference
        val resolvedSuperTypeRef = resolveSuperTypeRef(superReference.superTypeRef)
        if (resolvedSuperTypeRef !== superReference.superTypeRef) {
            superReference.replaceSuperTypeRef(resolvedSuperTypeRef)
        }

        val owner = context.containingRegularClass
        val receiverType = when {
            owner == null -> errorType("`super` is only allowed inside class declarations")
            resolvedSuperTypeRef is CfirResolvedTypeRef -> resolveExplicitSuperReceiverType(owner, resolvedSuperTypeRef)
            else -> resolveImplicitSuperReceiverType(owner)
        }

        superReceiverExpression.replaceConeTypeOrNull(receiverType)
        return superReceiverExpression
    }

    override fun transformLiteralExpression(
        literalExpression: CfirLiteralExpression,
        data: ResolutionMode,
    ): CfirExpression {
        val synthesized = synthesizeLiteralType(literalExpression.kind)
        val expectedType = data.expectedTypeOrNull
        literalExpression.replaceConeTypeOrNull(IdealTypeResolver.resolveIfIdeal(synthesized, expectedType))
        components.dataFlowAnalyzer.exitLiteralExpression(literalExpression)
        return literalExpression
    }

    private fun synthesizeLiteralType(kind: CfirLiteralKind): ConeCangJieType = when (kind) {
        CfirLiteralKind.INT     -> ConePrimitiveType.IDEAL_INT
        CfirLiteralKind.FLOAT   -> ConePrimitiveType.IDEAL_FLOAT
        CfirLiteralKind.BOOLEAN -> builtinTypes.boolType
        CfirLiteralKind.RUNE    -> ConePrimitiveType.RUNE
        CfirLiteralKind.STRING  -> stdlibStringType()
        CfirLiteralKind.UNIT    -> builtinTypes.unitType
    }

    // ── Named Access ─────────────────────────────────────────────────────────

    override fun transformNamedAccessExpression(
        namedAccessExpression: CfirNamedAccessExpression,
        data: ResolutionMode,
    ): CfirExpression =
        transformQualifiedAccessExpression(
            qualifiedAccessExpression = namedAccessExpression,
            data = data,
            isUsedAsReceiver = data is ResolutionMode.ReceiverResolution,
            isUsedAsGetClassReceiver = false,
        )

    // ── Qualified Access ──────────────────────────────────────────────────────

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        data: ResolutionMode,
    ): CfirExpression =
        transformQualifiedAccessExpression(
            qualifiedAccessExpression = qualifiedAccessExpression,
            data = data,
            isUsedAsReceiver = data is ResolutionMode.ReceiverResolution,
            isUsedAsGetClassReceiver = false,
        )

    private fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        data: ResolutionMode,
        isUsedAsReceiver: Boolean,
        isUsedAsGetClassReceiver: Boolean,
    ): CfirExpression =
        whileAnalysing(session, qualifiedAccessExpression) {
            val calleeReference = qualifiedAccessExpression.calleeReference

            if (qualifiedAccessExpression.coneTypeOrNull != null && calleeReference !is CfirNamedReferenceImpl) {
                return@whileAnalysing qualifiedAccessExpression
            }

            qualifiedAccessExpression.transformAnnotations(transformer, data)
            resolveAccessTypeArguments(qualifiedAccessExpression)

            val resolvedExpression = when (qualifiedAccessExpression.calleeReference) {
                is CfirThisReference -> {
                    if (qualifiedAccessExpression.coneTypeOrNull == null) {
                        val resultType = components.typeFromCallee(qualifiedAccessExpression)
                        qualifiedAccessExpression.replaceConeTypeOrNull(resultType)
                        (qualifiedAccessExpression.calleeReference as? CfirThisReference)
                            ?.replaceDiagnostic((resultType as? ConeErrorType)?.diagnostic)
                    }
                    qualifiedAccessExpression
                }

                is CfirResolvedNamedReference,
                is CfirErrorNamedReference,
                -> {
                    if (qualifiedAccessExpression.coneTypeOrNull == null) {
                        storeTypeFromCallee(qualifiedAccessExpression)
                    }
                    qualifiedAccessExpression
                }

                is CfirNamedReference -> {
                    val transformedCallee = resolveQualifiedAccessAndSelectCandidate(
                        qualifiedAccessExpression = qualifiedAccessExpression,
                        isUsedAsReceiver = isUsedAsReceiver,
                        isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
                        callSite = qualifiedAccessExpression,
                        data = data,
                    )
                    if (transformedCallee is CfirQualifiedAccessExpression) {
                        val candidateReference = transformedCallee.calleeReference as? CfirNamedReferenceWithCandidate
                        if (candidateReference != null) {
                            completeResolvedAccess(transformedCallee, data)
                        } else {
                            when (transformedCallee.calleeReference) {
                                is CfirResolvedNamedReference,
                                is CfirErrorNamedReference,
                                is CfirThisReference,
                                -> {
                                    if (transformedCallee.coneTypeOrNull == null) {
                                        storeTypeFromCallee(transformedCallee)
                                    }
                                    transformedCallee
                                }

                                else -> transformedCallee
                            }
                        }
                    } else {
                        transformedCallee
                    }
                }

                else -> {
                    qualifiedAccessExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
                    if (qualifiedAccessExpression.coneTypeOrNull == null) {
                        qualifiedAccessExpression.replaceConeTypeOrNull(
                            ConeErrorType(ConeSimpleDiagnostic("non-name reference", DiagnosticKind.Other))
                        )
                    }
                    qualifiedAccessExpression
                }
            }
            components.dataFlowAnalyzer.exitQualifiedAccessExpression(qualifiedAccessExpression)
            resolvedExpression
        }

    // ── Function Call ─────────────────────────────────────────────────────────

    override fun transformFunctionCall(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirExpression =
        transformFunctionCallInternal(functionCall, data, CallResolutionMode.REGULAR)

    internal fun transformFunctionCallInternal(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
        callResolutionMode: CallResolutionMode,
    ): CfirExpression =
        whileAnalysing(session, functionCall) {
            if (functionCall.origin.isConstructorDelegation) {
                return@whileAnalysing transformConstructorDelegationCall(functionCall, data)
            }

            val calleeReference = functionCall.calleeReference
            if (
                (calleeReference is CfirResolvedNamedReference || calleeReference is CfirErrorNamedReference) &&
                functionCall.coneTypeOrNull == null
            ) {
                storeTypeFromCallee(functionCall)
            }
            if (calleeReference is CfirNamedReferenceWithCandidate) return@whileAnalysing functionCall
            if (calleeReference !is CfirNamedReferenceImpl) {
                if (calleeReference !is CfirResolvedNamedReference) {
                    functionCall.transformChildren(transformer, ResolutionMode.ContextIndependent)
                }
                return@whileAnalysing functionCall
            }

            functionCall.transformAnnotations(transformer, data)
            resolveAccessTypeArguments(functionCall)

            val choosingOptionForAugmentedAssignment = callResolutionMode == CallResolutionMode.OPTION_FOR_AUGMENTED_ASSIGNMENT
            val withTransformedArguments = if (!choosingOptionForAugmentedAssignment) {
                components.dataFlowAnalyzer.enterCallArguments(functionCall, functionCall.argumentList.arguments)

                val withResolvedExplicitReceiver = when (callResolutionMode) {
                    CallResolutionMode.PROVIDE_DELEGATE -> functionCall
                    else -> transformExplicitReceiverOf(functionCall)
                }

                withResolvedExplicitReceiver.also {
                    components.dataFlowAnalyzer.exitCallExplicitReceiver()
                    it.replaceArgumentList(
                        it.argumentList.transform(transformer, ResolutionMode.ContextDependent)
                    )
                    components.dataFlowAnalyzer.exitCallArguments()
                }
            } else {
                functionCall
            }

            // 保存原始引用，resolveCallAndSelectCandidate 会原地修改 calleeReference
            val originalCalleeReference = withTransformedArguments.calleeReference
            val resolvedCall = callResolver.resolveCallAndSelectCandidate(withTransformedArguments, data)
            val callForCompletion = if (!choosingOptionForAugmentedAssignment) {
                tryResolveImplicitInvokeCall(originalCalleeReference, withTransformedArguments, resolvedCall, data) ?: resolvedCall
            } else {
                resolvedCall
            }

            if (!choosingOptionForAugmentedAssignment) {
                components.dataFlowAnalyzer.enterFunctionCall(callForCompletion)
            }

            val result = components.callCompleter.completeCall(
                callForCompletion,
                data,
                skipEvenPartialCompletion = choosingOptionForAugmentedAssignment,
            )

            if (!choosingOptionForAugmentedAssignment) {
                components.dataFlowAnalyzer.exitFunctionCall(result, data.forceFullCompletion)
            }

            result
        }

    /**
     * 构造器 delegation 调用不参与普通 tower resolve。
     *
     * `this(...)` / `super(...)` 的候选筛选、循环检测、父类构造器要求等
     * 都属于 constructor 语义，由专门的 declaration / expression checker 负责。
     * 这里仅解析其实参表达式，并把整条调用标记为 `Unit`，避免它先退化成普通 unresolved call。
     */
    private fun transformConstructorDelegationCall(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirFunctionCall {
        functionCall.transformAnnotations(transformer, data)
        resolveAccessTypeArguments(functionCall)

        components.dataFlowAnalyzer.enterCallArguments(functionCall, functionCall.argumentList.arguments)
        functionCall.replaceArgumentList(
            functionCall.argumentList.transform(transformer, ResolutionMode.ContextIndependent)
        )
        components.dataFlowAnalyzer.exitCallArguments()

        functionCall.replaceConeTypeOrNull(builtinTypes.unitType)
        return functionCall
    }

    private fun tryResolveImplicitInvokeCall(
        originalCalleeReference: CfirReference,
        originalCall: CfirFunctionCall,
        resolvedCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirFunctionCall? {
        if (originalCall.explicitReceiver != null) return null

        val diagnostic = (resolvedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic
        val shouldPreserveOriginalDiagnostic = diagnostic !is ConeUnresolvedNameError
        val canTryImplicitInvoke = when (diagnostic) {
            is ConeUnresolvedNameError -> true
            is ConeInapplicableCandidateError -> diagnostic.candidateSymbol is CfirEnumConstructorSymbol
            is ConeAmbiguityError -> !diagnostic.applicability.isSuccess &&
                    diagnostic.candidateSymbols.all { it is CfirEnumConstructorSymbol }
            else -> false
        }
        if (!canTryImplicitInvoke) return null

        val originalCallee = originalCalleeReference as? CfirNamedReferenceImpl ?: return null
        if (originalCallee.name == OperatorNameConventions.INVOKE) return null

        val resolvedAccess = callResolver.resolveNamedValueAccessAndSelectCandidate(
            qualifiedAccess = buildNamedAccessExpression {
                source = originalCall.source
                calleeReference = buildNamedReference {
                    source = originalCallee.source
                    name = originalCallee.name
                }
                typeArguments.addAll(originalCall.typeArguments)
            },
            isUsedAsReceiver = true,
            isUsedAsGetClassReceiver = false,
            callSite = originalCall,
            resolutionMode = data,
        ) as? CfirQualifiedAccessExpression ?: return null

        when (resolvedAccess.calleeReference) {
            is CfirResolvedNamedReference,
            is CfirNamedReferenceWithCandidate,
            -> Unit

            else -> return null
        }

        val invokeCall = buildFunctionCall {
            source = originalCall.source
            calleeReference = buildNamedReference {
                source = originalCallee.source
                name = OperatorNameConventions.INVOKE
            }
            explicitReceiver = resolvedAccess
            argumentList = buildArgumentList {
                arguments.addAll(originalCall.argumentList.arguments)
            }
            typeArguments.addAll(originalCall.typeArguments)
            origin = originalCall.origin
        }

        val invokeResult = callResolver.resolveCallAndSelectCandidate(invokeCall, data)
            .takeUnless { (it.calleeReference as? CfirDiagnosticHolder)?.diagnostic is ConeUnresolvedNameError }

        if (invokeResult != null) return invokeResult

        if (shouldPreserveOriginalDiagnostic) return null

        // 变量已解析但类型上没有 invoke 操作符 → 报告专用诊断
        val receiverType = resolvedAccess.coneTypeOrNull
        if (receiverType != null && receiverType !is ConeErrorType) {
            resolvedCall.replaceCalleeReference(
                buildErrorNamedReference {
                    source = originalCallee.source
                    name = originalCallee.name
                    this.diagnostic = ConeNoMatchingInvokeOperatorError(originalCallee.name, receiverType)
                }
            )
            return resolvedCall
        }

        return null
    }

    private fun storeTypeFromCallee(functionCall: CfirFunctionCall) {
        storeTypeFromCallee(functionCall as CfirQualifiedAccessExpression)
    }

    internal fun storeTypeFromCallee(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        @Suppress("UNUSED_PARAMETER") isLhsOfAssignment: Boolean = false,
    ) {
        qualifiedAccessExpression.replaceConeTypeOrNull(components.typeFromCallee(qualifiedAccessExpression))
    }

    fun <Q : CfirQualifiedAccessExpression> transformExplicitReceiverOf(qualifiedAccessExpression: Q): Q {
        if (qualifiedAccessExpression.explicitReceiver == null) return qualifiedAccessExpression
        qualifiedAccessExpression.transformExplicitReceiver(transformer, ResolutionMode.ReceiverResolution)
        return qualifiedAccessExpression
    }

    protected open fun resolveQualifiedAccessAndSelectCandidate(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        isUsedAsGetClassReceiver: Boolean,
        callSite: CfirElement,
        data: ResolutionMode,
    ): CfirExpression {
        return callResolver.resolveNamedValueAccessAndSelectCandidate(
            qualifiedAccess = qualifiedAccessExpression,
            isUsedAsReceiver = isUsedAsReceiver,
            isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
            callSite = callSite,
            resolutionMode = data,
        )
    }

    internal enum class CallResolutionMode {
        REGULAR,

        /**
         * For PROVIDE_DELEGATE we skip transforming explicit receiver of the call since it's already been resolved
         * at [FirDeclarationsResolveTransformer.transformPropertyAccessorsWithDelegate]
         */
        PROVIDE_DELEGATE,

        /**
         * When we're resolving an operator like `a += b` we try to resolve it with different options of desugaring like
         * `a = a.plus(b)` and `a.plusAssign(b)` until find something that looks successful.
         * But at this stage, we skip transformation of receiver, arguments and skip completion in any form.
         */
        OPTION_FOR_AUGMENTED_ASSIGNMENT,
    }

    // ── Block ─────────────────────────────────────────────────────────────────

    override fun transformBlock(block: CfirBlock, data: ResolutionMode): CfirExpression {
        components.dataFlowAnalyzer.enterBlock(block)
        block.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val lastExpr = block.statements.lastOrNull()
        block.replaceConeTypeOrNull(
            if (lastExpr is CfirExpression) lastExpr.coneTypeOrNull ?: builtinTypes.unitType
            else builtinTypes.unitType
        )
        components.dataFlowAnalyzer.exitBlock(block)
        return block
    }

    // ── Match ─────────────────────────────────────────────────────────────────

    override fun transformMatchExpression(
        matchExpression: CfirMatchExpression,
        data: ResolutionMode,
    ): CfirExpression {
        components.dataFlowAnalyzer.enterMatchExpression(matchExpression)
        matchExpression.subject?.resolveIndependently()
        val subjectType = matchExpression.subject?.coneTypeOrNull

        val branchTypes = matchExpression.branches.map { branch ->
            resolveBranch(branch, subjectType)
        }

        matchExpression.replaceExhaustiveness(resolveMatchExhaustiveness(matchExpression))
        matchExpression.replaceConeTypeOrNull(computeMatchResultType(branchTypes))
        components.dataFlowAnalyzer.exitMatchExpression(
            matchExpression,
            syntheticElseDecision = components.dataFlowAnalyzer.matchSyntheticElseDecision(matchExpression),
            callCompleted = data.forceFullCompletion,
        )
        return matchExpression
    }

    /**
     * BODY_RESOLVE 阶段将 shared semantics 的穷尽性结论正式回写到 tree。
     *
     * 若 shared analyzer 暂时无法给出稳定结论，则保持 `Unknown`，
     * 让 CFG 走“保守地补 synthetic else”而不是把内部分析失败固化成 tree-level Error。
     */
    private fun resolveMatchExhaustiveness(matchExpression: CfirMatchExpression): CfirMatchExhaustivenessStatus {
        return when (val result = ExhaustivenessAnalyzer.checkMatch(matchExpression, session)) {
            ExhaustivenessResult.Exhaustive -> CfirMatchExhaustivenessStatus.Exhaustive(
                source = CfirMatchExhaustivenessStatus.Source.BodyResolve,
            )

            is ExhaustivenessResult.NonExhaustive -> CfirMatchExhaustivenessStatus.NonExhaustive(
                missingCaseTexts = result.getMissingPatternTexts(),
                source = CfirMatchExhaustivenessStatus.Source.BodyResolve,
            )

            is ExhaustivenessResult.Error,
            ExhaustivenessResult.Skipped,
            -> CfirMatchExhaustivenessStatus.Unknown
        }
    }

    private fun resolveBranch(
        branch: CfirMatchBranch,
        subjectType: ConeCangJieType?,
    ): ConeCangJieType {
        return withNewLocalScope {
            components.dataFlowAnalyzer.enterMatchBranchCondition(branch)
            branch.transformPattern(transformer, ResolutionMode.ContextIndependent)
            if (branch is org.cangnova.cangjie.cfir.expressions.impl.CfirMatchBranchImpl) {
                branch.pattern = resolveDeferredMatchPattern(branch.pattern)
            }
            resolvePatternBindingTypes(branch.pattern, subjectType, specificTypeResolverTransformer)
            registerPatternBindings(branch.pattern)

            branch.transformGuard(transformer, ResolutionMode.ContextIndependent)
            components.dataFlowAnalyzer.exitMatchBranchCondition(branch)
            branch.transformBody(transformer, ResolutionMode.ContextIndependent)
            components.dataFlowAnalyzer.exitMatchBranchResult(branch)

            val bodyType = branch.body.coneTypeOrNull ?: builtinTypes.unitType
            branch.replaceConeTypeOrNull(bodyType)
            bodyType
        }
    }

    /**
     * 对齐官方 `VarOrEnumPattern` 的延迟决议：
     * 先保留裸名字歧义，进入 body resolve 后再根据当前作用域中是否可见 enum constructor
     * 决定它究竟是 enum pattern 还是 binding pattern。
     */
    private fun resolveDeferredMatchPattern(pattern: CfirPattern): CfirPattern {
        return when (pattern) {
            is CfirVarOrEnumPattern -> resolveVarOrEnumPattern(pattern)
            is CfirBindingPattern -> {
                val nestedPattern = pattern.nestedPattern ?: return pattern
                val resolvedNestedPattern = resolveDeferredMatchPattern(nestedPattern)
                if (resolvedNestedPattern === nestedPattern) pattern else buildBindingPatternCopy(pattern) {
                    this.nestedPattern = resolvedNestedPattern
                }
            }

            is CfirTuplePattern -> {
                val resolvedElements = pattern.elements.map(::resolveDeferredMatchPattern)
                if (resolvedElements.zip(pattern.elements).all { (resolved, original) -> resolved === original }) {
                    pattern
                } else {
                    buildTuplePatternCopy(pattern) {
                        elements.clear()
                        elements.addAll(resolvedElements)
                    }
                }
            }

            is CfirEnumPattern -> {
                val resolvedArguments = pattern.arguments.map(::resolveDeferredMatchPattern)
                if (resolvedArguments.zip(pattern.arguments).all { (resolved, original) -> resolved === original }) {
                    pattern
                } else {
                    buildEnumPatternCopy(pattern) {
                        arguments.clear()
                        arguments.addAll(resolvedArguments)
                    }
                }
            }

            is CfirOrPattern -> {
                val resolvedAlternatives = pattern.alternatives.map(::resolveDeferredMatchPattern)
                if (resolvedAlternatives.zip(pattern.alternatives).all { (resolved, original) -> resolved === original }) {
                    pattern
                } else {
                    buildOrPattern {
                        source = pattern.source
                        alternatives.clear()
                        alternatives.addAll(resolvedAlternatives)
                    }
                }
            }

            else -> pattern
        }
    }

    private fun resolveVarOrEnumPattern(pattern: CfirVarOrEnumPattern): CfirPattern {
        val enumConstructorReference = resolveEnumConstructorReferenceOrNull(pattern)
        if (enumConstructorReference != null) {
            return buildEnumPattern {
                source = pattern.source
                constructorReference = enumConstructorReference
            }
        }

        return buildBindingPattern {
            source = pattern.source
            name = pattern.name
            bindingVariable = pattern.bindingVariable
        }
    }

    private fun resolveEnumConstructorReferenceOrNull(pattern: CfirVarOrEnumPattern): CfirReference? {
        val temporaryAccess = buildNamedAccessExpression {
            source = pattern.source
            calleeReference = buildNamedReference {
                source = pattern.source
                name = pattern.name
            }
        }
        val resolvedAccess = callResolver.resolveVariableAccessAndSelectCandidate(
            qualifiedAccess = temporaryAccess,
            isUsedAsReceiver = false,
            isUsedAsGetClassReceiver = false,
            callSite = temporaryAccess,
            resolutionMode = ResolutionMode.ContextIndependent,
        ) as? CfirQualifiedAccessExpression ?: return null
        val resolvedReference = resolvedAccess.calleeReference

        return when {
            resolvedReference is CfirResolvedNamedReference && resolvedReference.resolvedSymbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor ->
                resolvedReference

            resolvedReference is CfirResolvedAppliedCallableReference && resolvedReference.resolvedSymbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor ->
                buildResolvedNamedReference {
                    source = resolvedReference.source ?: pattern.source
                    name = resolvedReference.name
                    resolvedSymbol = resolvedReference.resolvedSymbol
                }

            resolvedReference is CfirNamedReferenceWithCandidate &&
                    resolvedReference.candidate.symbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor ->
                buildResolvedNamedReference {
                    source = resolvedReference.source ?: pattern.source
                    name = resolvedReference.name
                    resolvedSymbol = resolvedReference.candidate.symbol
                }

            else -> null
        }
    }

    private fun computeMatchResultType(branchTypes: List<ConeCangJieType>): ConeCangJieType = when {
        branchTypes.isEmpty()                        -> builtinTypes.unitType
        branchTypes.size == 1                        -> branchTypes.single()
        branchTypes.all { it == branchTypes.first() } -> branchTypes.first()
        else                                         -> ConeUnionType(branchTypes.toSet())
    }

    // ── If ────────────────────────────────────────────────────────────────────

    override fun transformIfExpression(
        ifExpression: CfirIfExpression,
        data: ResolutionMode,
    ): CfirExpression {
        ifExpression.transformCondition(transformer, withExpectedType(builtinTypes.boolType))
        val branchResolutionMode = (data as? ResolutionMode.WithExpectedType)
            ?.takeUnless { it.fromCast }
            ?.copy(forceFullCompletion = false)
            ?: ResolutionMode.ContextDependent

        ifExpression.transformThenBranch(transformer, branchResolutionMode)
        ifExpression.transformElseBranch(transformer, branchResolutionMode)

        val thenType = ifExpression.thenBranch.coneTypeOrNull
        val elseType = ifExpression.elseBranch?.coneTypeOrNull
        val mergedType = when {
            thenType == null -> elseType ?: builtinTypes.unitType
            elseType == null -> builtinTypes.unitType
            thenType == elseType -> thenType
            else -> commonSupertype(listOf(thenType, elseType))
        }
        ifExpression.replaceConeTypeOrNull(
            IdealTypeResolver.resolveIfIdeal(mergedType, data.expectedTypeOrNull)
        )
        return ifExpression
    }

    // ── Return / Throw ────────────────────────────────────────────────────────

    override fun transformReturnExpression(
        returnExpression: CfirReturnExpression,
        data: ResolutionMode,
    ): CfirExpression {
        components.dataFlowAnalyzer.enterJump(returnExpression)
        val expectedReturnTypeRef = returnExpression.target.labeledElement.returnTypeRef as? CfirResolvedTypeRef
        val resultResolutionMode = expectedReturnTypeRef?.let(::withExpectedType) ?: ResolutionMode.ContextIndependent
        returnExpression.transformResult(transformer, resultResolutionMode)
        returnExpression.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
        returnExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        components.dataFlowAnalyzer.exitJump(returnExpression)
        return returnExpression
    }

    override fun transformLoopJump(
        jumpExpression: CfirLoopJump,
        data: ResolutionMode,
    ): CfirExpression {
        return transformLoopJumpLike(jumpExpression, data)
    }

    override fun transformBreakExpression(
        breakExpression: CfirBreakExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return transformLoopJumpLike(breakExpression, data)
    }

    override fun transformContinueExpression(
        continueExpression: CfirContinueExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return transformLoopJumpLike(continueExpression, data)
    }

    /**
     * loop jump 的公共 resolve 入口。
     *
     * Kotlin FIR 的基础 transformer 不会把 break/continue 自动委派到 loop-jump 抽象层，
     * 因此需要由具体节点 override 显式复用这段处理逻辑。
     */
    private fun transformLoopJumpLike(
        jumpExpression: CfirLoopJump,
        data: ResolutionMode,
    ): CfirExpression {
        jumpExpression.transformAnnotations(transformer, data)
        if (jumpExpression.coneTypeOrNull == null) {
            jumpExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        }
        components.dataFlowAnalyzer.exitJump(jumpExpression)
        return jumpExpression
    }

    override fun transformThrowExpression(
        throwExpression: CfirThrowExpression,
        data: ResolutionMode,
    ): CfirExpression {
        throwExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        throwExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        components.dataFlowAnalyzer.exitThrowException(throwExpression)
        return throwExpression
    }

    override fun transformPerformExpression(
        performExpression: CfirPerformExpression,
        data: ResolutionMode,
    ): CfirExpression {
        performExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)

        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.EffectHandlers)) {
            performExpression.replaceConeTypeOrNull(
                ConeErrorType(ConeEffectsFeatureDisabledError("perform"))
            )
            return performExpression
        }

        val commandSupertype = findCommandSupertype(performExpression.expression.coneTypeOrNull)
        performExpression.replaceConeTypeOrNull(
            commandSupertype?.typeArguments?.firstOrNull()?.type
                ?: ConeErrorType(
                    ConeCommandIncompatibleTypeError(performExpression.expression.coneTypeOrNull),
                ),
        )
        return performExpression
    }

    override fun transformResumeExpression(
        resumeExpression: CfirResumeExpression,
        data: ResolutionMode,
    ): CfirExpression {
        resumeExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)

        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.EffectHandlers)) {
            resumeExpression.replaceConeTypeOrNull(
                ConeErrorType(ConeEffectsFeatureDisabledError("resume"))
            )
            return resumeExpression
        }

        val handlerContext = effectHandlerStack.lastOrNull()
        if (handlerContext == null) {
            resumeExpression.replaceConeTypeOrNull(
                ConeErrorType(ConeImplicitResumeOutsideHandlerError)
            )
            return resumeExpression
        }

        resumeExpression.replaceConeTypeOrNull(builtinTypes.nothingType)

        val throwingType = resumeExpression.throwingExpression?.coneTypeOrNull
        if (throwingType != null && !isExceptionLikeType(throwingType)) {
            resumeExpression.replaceConeTypeOrNull(
                ConeErrorType(
                    ConeResumeThrowingMismatchTypeError(throwingType),
                    delegatedType = builtinTypes.nothingType,
                ),
            )
            return resumeExpression
        }

        if (resumeExpression.withExpression == null && resumeExpression.throwingExpression == null) {
            if (AbstractTypeChecker.isSubtypeOf(session.typeContext, handlerContext.commandResultType, builtinTypes.unitType) != true) {
                resumeExpression.replaceConeTypeOrNull(
                    ConeErrorType(
                        ConeResumeNoWithError(handlerContext.commandResultType),
                        delegatedType = builtinTypes.nothingType,
                    ),
                )
            }
        }

        return resumeExpression
    }

    // ── Assignment ────────────────────────────────────────────────────────────

    override fun transformAssignment(
        assignment: CfirAssignment,
        data: ResolutionMode,
    ): CfirExpression {
        val subscriptLValue = assignment.lValue as? CfirSubscriptExpression
        if (subscriptLValue != null) {
            assignment.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
            subscriptLValue.transformReceiver(transformer, ResolutionMode.ContextIndependent)
            subscriptLValue.transformIndices(transformer, ResolutionMode.ContextIndependent)
            assignment.transformRValue(transformer, ResolutionMode.ContextIndependent)

            resolveSubscriptSetAssignment(assignment, subscriptLValue, data)
            assignment.replaceConeTypeOrNull(builtinTypes.unitType)
            components.dataFlowAnalyzer.exitVariableAssignment(assignment)
            return assignment
        }

        assignment.transformChildren(transformer, ResolutionMode.ContextIndependent)
        assignment.replaceConeTypeOrNull(builtinTypes.unitType)
        components.dataFlowAnalyzer.recordAssignment(assignment)
        components.dataFlowAnalyzer.exitVariableAssignment(assignment)
        return assignment
    }

    // ── Tuple / Array / String Literals ──────────────────────────────────────

    override fun transformTupleLiteral(
        tupleLiteral: CfirTupleLiteral,
        data: ResolutionMode,
    ): CfirExpression {
        tupleLiteral.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val elementTypes = tupleLiteral.elements.map {
            it.coneTypeOrNull ?: errorType("unresolved element")
        }
        tupleLiteral.replaceConeTypeOrNull(ConeTupleType(elementTypes))
        return tupleLiteral
    }

    override fun transformArrayLiteral(
        arrayLiteral: CfirArrayLiteral,
        data: ResolutionMode,
    ): CfirExpression {
        arrayLiteral.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val elementType = arrayLiteral.elements.firstNotNullOfOrNull { it.coneTypeOrNull }
            ?: errorType("empty array literal")
        arrayLiteral.replaceConeTypeOrNull(
            constructNamedType(
                classId = StdlibClassIds.Array,
                typeArguments = listOf(elementType),
            )
        )
        return arrayLiteral
    }

    override fun transformStringInterpolation(
        stringInterpolation: CfirStringInterpolation,
        data: ResolutionMode,
    ): CfirExpression {
        stringInterpolation.transformChildren(transformer, ResolutionMode.ContextIndependent)
        stringInterpolation.replaceConeTypeOrNull(stdlibStringType())
        return stringInterpolation
    }

    // ── Comparison / Binary / Type Operators ──────────────────────────────────

    override fun transformComparisonExpression(
        comparisonExpression: CfirComparisonExpression,
        data: ResolutionMode,
    ): CfirExpression {
        comparisonExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        comparisonExpression.replaceConeTypeOrNull(resolveComparisonExpressionType(comparisonExpression, data))
        return comparisonExpression
    }

    private fun resolveComparisonExpressionType(
        comparisonExpression: CfirComparisonExpression,
        data: ResolutionMode,
    ): ConeCangJieType {
        val leftType = comparisonExpression.left.coneTypeOrNull
        val rightType = comparisonExpression.right.coneTypeOrNull
        if (leftType == null || rightType == null) return builtinTypes.boolType

        val operatorName = comparisonExpression.operation.toOperatorName()
        CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
            operatorName,
            leftType,
            listOf(rightType),
        )?.let { return it.returnType }

        val comparisonCall = buildFunctionCall {
            source = comparisonExpression.source
            calleeReference = buildNamedReference {
                source = comparisonExpression.source
                name = operatorName
            }
            explicitReceiver = comparisonExpression.left
            argumentList = buildArgumentList {
                source = comparisonExpression.source
                arguments.add(comparisonExpression.right)
            }
            origin = CfirFunctionCallOrigin.Operator
        }

        val resolvedCall = callResolver.resolveCallAndSelectCandidate(comparisonCall, data)
        (resolvedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            return ConeErrorType(diagnostic, delegatedType = builtinTypes.boolType)
        }

        val completedCall = components.callCompleter.completeCall(resolvedCall, data)
        (completedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            return ConeErrorType(diagnostic, delegatedType = builtinTypes.boolType)
        }

        return completedCall.coneTypeOrNull ?: builtinTypes.boolType
    }

    override fun transformBinaryOp(
        binaryOp: CfirBinaryOp,
        data: ResolutionMode,
    ): CfirExpression {
        binaryOp.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val resultType = when (binaryOp.kind) {
            CfirBinaryOpKind.AND, CfirBinaryOpKind.OR -> builtinTypes.boolType
            CfirBinaryOpKind.COALESCING               -> binaryOp.left.coneTypeOrNull
                ?: errorType("unresolved coalescing left")
            CfirBinaryOpKind.PIPELINE                 -> binaryOp.right.coneTypeOrNull
                ?: errorType("unresolved pipeline right")
        }
        binaryOp.replaceConeTypeOrNull(resultType)
        return binaryOp
    }

    override fun transformTypeOperator(
        typeOperator: CfirTypeOperator,
        data: ResolutionMode,
    ): CfirExpression {
        typeOperator.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val resultType = when (typeOperator.operation) {
            CfirTypeOperationKind.IS -> builtinTypes.boolType
            CfirTypeOperationKind.AS -> {
                val typeRef = typeOperator.typeRef
                if (typeRef is CfirResolvedTypeRef) typeRef.coneType
                else errorType("unresolved type in as-expression")
            }
        }
        typeOperator.replaceConeTypeOrNull(resultType)
        return typeOperator
    }

    // ── Error Expression ──────────────────────────────────────────────────────

    override fun transformErrorExpression(
        errorExpression: CfirErrorExpression,
        data: ResolutionMode,
    ): CfirExpression {
        errorExpression.replaceConeTypeOrNull(ConeErrorType(errorExpression.diagnostic))
        return errorExpression
    }

    // ── For-In / Loop ─────────────────────────────────────────────────────────

    override fun transformForInExpression(
        forInExpression: CfirForInExpression,
        data: ResolutionMode,
    ): CfirExpression {
        forInExpression.iterable.resolveIndependently()
        val iterVarType = inferIterableElementType(forInExpression.iterable.coneTypeOrNull)

        val varDecl = forInExpression.variable
        if (varDecl.returnTypeRef !is CfirResolvedTypeRef && varDecl.returnTypeRef !is CfirImplicitTypeRef) {
            varDecl.replaceReturnTypeRef(
                specificTypeResolverTransformer.transformTypeRef(
                    varDecl.returnTypeRef,
                    CfirTypeResolutionConfiguration(
                        useSiteFile = context.file,
                        topContainer = context.containers.lastOrNull(),
                    ),
                ),
            )
        } else if (varDecl.returnTypeRef !is CfirResolvedTypeRef) {
            varDecl.replaceReturnTypeRef(
                varDecl.returnTypeRef.resolvedTypeFromPrototype(iterVarType, varDecl.returnTypeRef.source)
            )
        }

        varDecl.transformPattern(transformer, ResolutionMode.ContextIndependent)
        resolvePatternBindingTypes(
            pattern = varDecl.pattern,
            expectedType = varDecl.returnTypeRef.coneTypeOrNull ?: iterVarType,
            typeResolver = specificTypeResolverTransformer,
        )

        withNewLocalScope {
            registerPatternBindings(varDecl.pattern)
            forInExpression.transformBody(transformer, ResolutionMode.ContextIndependent)
        }

        forInExpression.replaceConeTypeOrNull(builtinTypes.unitType)
        return forInExpression
    }

    private fun inferIterableElementType(iterableType: ConeCangJieType?): ConeCangJieType {
        if (iterableType == null) return errorType("iterable has no type")
        when (iterableType) {
            is ConeClassLikeType -> {
                if (iterableType.classId == StdlibClassIds.Range) {
                    return iterableType.typeArguments.firstOrNull()?.type ?: ConePrimitiveType.INT64
                }
                val typeArgs = iterableType.typeArguments
                if (typeArgs.isNotEmpty()) return typeArgs.first().type
            }
            is ConeStructType -> {
                if (iterableType.classId == StdlibClassIds.Range) {
                    return iterableType.typeArguments.firstOrNull()?.type ?: ConePrimitiveType.INT64
                }
            }
            else -> Unit
        }
        return errorType("cannot infer element type from: $iterableType")
    }

    override fun transformLoopExpression(
        loopExpression: CfirLoopExpression,
        data: ResolutionMode,
    ): CfirExpression {
        loopExpression.transformAnnotations(transformer, data)
        if (loopExpression.isDoWhile) {
            components.dataFlowAnalyzer.enterDoWhileLoop(loopExpression)
            loopExpression.transformBody(transformer, ResolutionMode.ContextIndependent)
            components.dataFlowAnalyzer.enterDoWhileLoopCondition(loopExpression)
            loopExpression.transformCondition(transformer, ResolutionMode.ContextIndependent)
            components.dataFlowAnalyzer.exitDoWhileLoop(loopExpression)
        } else {
            components.dataFlowAnalyzer.enterWhileLoop(loopExpression)
            loopExpression.transformCondition(transformer, ResolutionMode.ContextIndependent)
            components.dataFlowAnalyzer.exitWhileLoopCondition(loopExpression)
            loopExpression.transformBody(transformer, ResolutionMode.ContextIndependent)
            components.dataFlowAnalyzer.exitWhileLoop(loopExpression)
        }
        loopExpression.replaceConeTypeOrNull(builtinTypes.unitType)
        return loopExpression
    }

    // ── Try / Catch ───────────────────────────────────────────────────────────

    override fun transformHandleClause(
        handleClause: CfirHandleClause,
        data: ResolutionMode,
    ): CfirExpression {
        handleClause.transformAnnotations(transformer, data)
        resolveCommandPatternTypeRefs(handleClause.commandPattern)

        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.EffectHandlers)) {
            handleClause.transformBody(transformer, ResolutionMode.ContextIndependent)
            val delegatedType = normalizeTypeForJoin(handleClause.body.coneTypeOrNull) ?: builtinTypes.unitType
            handleClause.replaceConeTypeOrNull(
                ConeErrorType(
                    ConeEffectsFeatureDisabledError("handle"),
                    delegatedType = delegatedType,
                )
            )
            return handleClause
        }

        val commandResultType = resolveHandleCommandResultType(handleClause.commandPattern)
        val effectiveResultType = commandResultType ?: constructNamedType(StdlibClassIds.Any)

        effectHandlerStack.addLast(EffectHandlerContext(effectiveResultType))
        try {
            handleClause.transformBody(transformer, ResolutionMode.ContextIndependent)
        } finally {
            effectHandlerStack.removeLast()
        }

        val bodyType = handleClause.body.coneTypeOrNull ?: builtinTypes.unitType
        val normalizedBodyType = normalizeTypeForJoin(bodyType) ?: bodyType
        handleClause.replaceConeTypeOrNull(
            if (commandResultType == null) {
                ConeErrorType(
                    ConeCommandHandleTypeError(handleClause.commandPattern.typeRefs.firstOrNull()?.coneType),
                    delegatedType = normalizedBodyType,
                )
            } else {
                normalizedBodyType
            }
        )
        return handleClause
    }

    override fun transformTryExpression(
        tryExpression: CfirTryExpression,
        data: ResolutionMode,
    ): CfirExpression {
        tryExpression.transformAnnotations(transformer, data)
        components.dataFlowAnalyzer.enterTryExpression(tryExpression)
        tryExpression.transformTryBlock(transformer, ResolutionMode.ContextIndependent)
        components.dataFlowAnalyzer.exitTryMainBlock()
        for (catchClause in tryExpression.catches) {
            components.dataFlowAnalyzer.enterCatchClause(catchClause)
            catchClause.transform<CfirElement, ResolutionMode>(transformer, ResolutionMode.ContextIndependent)
            components.dataFlowAnalyzer.exitCatchClause(catchClause)
        }
        for (handleClause in tryExpression.handlers) {
            components.dataFlowAnalyzer.enterHandleClause(handleClause)
            handleClause.transform<CfirElement, ResolutionMode>(transformer, ResolutionMode.ContextIndependent)
            components.dataFlowAnalyzer.exitHandleClause(handleClause)
        }
        if (tryExpression.finallyBlock != null) {
            components.dataFlowAnalyzer.enterFinallyBlock()
            tryExpression.transformFinallyBlock(transformer, ResolutionMode.ContextIndependent)
            components.dataFlowAnalyzer.exitFinallyBlock()
        }
        components.dataFlowAnalyzer.exitTryExpression(data.forceFullCompletion)

        var currentJoinType = normalizeTypeForJoin(tryExpression.tryBlock.coneTypeOrNull) ?: builtinTypes.unitType
        tryExpression.catches.forEach { catchClause ->
            val catchType = normalizeTypeForJoin(catchClause.body.coneTypeOrNull) ?: builtinTypes.unitType
            currentJoinType = commonSupertype(listOf(currentJoinType, catchType))
        }

        var handleMismatchDiagnostic: ConeMismatchingHandleBlockError? = null
        tryExpression.handlers.forEach { handleClause ->
            val handleType = normalizeTypeForJoin(handleClause.coneTypeOrNull ?: handleClause.body.coneTypeOrNull)
                ?: builtinTypes.unitType
            val joinedType = commonSupertype(listOf(currentJoinType, handleType))
            if (joinedType is ConeUnionType) {
                val diagnostic = ConeMismatchingHandleBlockError(handleType, currentJoinType)
                handleClause.replaceConeTypeOrNull(
                    ConeErrorType(
                        diagnostic,
                        delegatedType = joinedType,
                    ),
                )
                handleMismatchDiagnostic = diagnostic
            } else {
                currentJoinType = joinedType
            }
        }

        tryExpression.replaceConeTypeOrNull(
            if (handleMismatchDiagnostic != null) {
                ConeErrorType(
                    handleMismatchDiagnostic!!,
                    delegatedType = currentJoinType,
                )
            } else {
                currentJoinType
            }
        )
        return tryExpression
    }

    override fun transformCatch(
        catch: CfirCatch,
        data: ResolutionMode,
    ): CfirExpression {
        catch.transformAnnotations(transformer, data)

        val parameter = catch.parameter
        val resolvedTypeRef = resolveSuperTypeRef(parameter.returnTypeRef)
        if (resolvedTypeRef !== parameter.returnTypeRef) {
            parameter.replaceReturnTypeRef(resolvedTypeRef)
        }

        context.withTowerDataCleanup {
            context.addLocalScope(CfirLocalScope(session))
            context.storeValueParameterIfNeeded(parameter, session)
            catch.transformBody(transformer, ResolutionMode.ContextIndependent)
        }

        catch.replaceConeTypeOrNull(catch.body.coneTypeOrNull ?: builtinTypes.unitType)
        return catch
    }

    // ── Subscript ─────────────────────────────────────────────────────────────

    override fun transformSubscriptExpression(
        subscriptExpression: CfirSubscriptExpression,
        data: ResolutionMode,
    ): CfirExpression {
        subscriptExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val resultType = when (val receiverType = subscriptExpression.receiver.coneTypeOrNull) {
            is ConeTupleType -> {
                val indexValue = extractConstantIntIndex(subscriptExpression.indices.firstOrNull())
                if (indexValue != null && indexValue in receiverType.elementTypes.indices) {
                    receiverType.elementTypes[indexValue]
                } else {
                    errorType("tuple index out of bounds or non-constant")
                }
            }
            is ConeVArrayType -> receiverType.elementType
            else -> {
                val arrayElementType = receiverType?.arrayElementType
                arrayElementType
                    ?: if (receiverType != null) {
                        resolveSubscriptExpressionType(subscriptExpression, receiverType, data)
                    } else {
                        errorType("receiver has no type")
                    }
            }
        }
        subscriptExpression.replaceConeTypeOrNull(resultType)
        return subscriptExpression
    }

    private fun resolveSubscriptExpressionType(
        subscriptExpression: CfirSubscriptExpression,
        receiverType: ConeCangJieType,
        data: ResolutionMode,
    ): ConeCangJieType {
        val argTypes = subscriptExpression.indices.mapNotNull { it.coneTypeOrNull }
        CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
            Name.identifier("[]"),
            receiverType,
            argTypes,
        )?.let { return it.returnType }

        val getCall = buildFunctionCall {
            source = subscriptExpression.source
            calleeReference = buildNamedReference {
                source = subscriptExpression.source
                name = OperatorNameConventions.GET
            }
            explicitReceiver = subscriptExpression.receiver
            argumentList = buildArgumentList {
                arguments.addAll(subscriptExpression.indices)
            }
            origin = CfirFunctionCallOrigin.Operator
        }

        val resolvedCall = callResolver.resolveCallAndSelectCandidate(getCall, data)
        (resolvedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            return ConeErrorType(diagnostic)
        }

        val completedCall = components.callCompleter.completeCall(resolvedCall, data)
        (completedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            return ConeErrorType(diagnostic)
        }

        return completedCall.coneTypeOrNull ?: errorType("no subscript operator for: $receiverType")
    }

    private fun resolveSubscriptSetAssignment(
        assignment: CfirAssignment,
        subscriptExpression: CfirSubscriptExpression,
        data: ResolutionMode,
    ) {
        val setCall = buildFunctionCall {
            source = subscriptExpression.source
            calleeReference = buildNamedReference {
                source = subscriptExpression.source
                name = OperatorNameConventions.SET
            }
            explicitReceiver = subscriptExpression.receiver
            argumentList = buildArgumentList {
                arguments.addAll(subscriptExpression.indices)
                arguments.add(assignment.rValue)
            }
            origin = CfirFunctionCallOrigin.Operator
        }

        val resolvedCall = callResolver.resolveCallAndSelectCandidate(setCall, data)
        (resolvedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            subscriptExpression.replaceConeTypeOrNull(ConeErrorType(diagnostic, delegatedType = builtinTypes.unitType))
            return
        }

        val completedCall = components.callCompleter.completeCall(resolvedCall, data)
        (completedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            subscriptExpression.replaceConeTypeOrNull(ConeErrorType(diagnostic, delegatedType = builtinTypes.unitType))
            return
        }

        subscriptExpression.replaceConeTypeOrNull(completedCall.coneTypeOrNull ?: builtinTypes.unitType)
    }

    private fun extractConstantIntIndex(expr: CfirExpression?): Int? {
        if (expr !is CfirLiteralExpression || expr.kind != CfirLiteralKind.INT) return null
        return (expr.value as? Long)?.toInt() ?: (expr.value as? Int)
    }

    // ── Lambda ────────────────────────────────────────────────────────────────

    override fun transformAnonymousFunctionExpression(
        anonymousFunctionExpression: CfirAnonymousFunctionExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return withClearedEffectHandlers {
            val anonFunc = anonymousFunctionExpression.anonymousFunction
            val expectedFuncType = data.expectedTypeOrNull as? ConeFunctionType

            val hasUnresolvedParameterType = anonFunc.valueParameters.any { it.returnTypeRef !is CfirResolvedTypeRef }
            if (expectedFuncType == null && hasUnresolvedParameterType) {
                // Keep top-level lambda shape unresolved until call completion provides an expected function type.
                // Eagerly fixing returnType here turns lambda return mismatches into outer argument mismatches.
                return@withClearedEffectHandlers anonymousFunctionExpression
            }

            components.dataFlowAnalyzer.enterFunction(anonFunc)

            val parameterTypes = context.withTowerDataCleanup {
                context.addLocalScope(CfirLocalScope(session))
                val types = anonFunc.valueParameters.mapIndexed { i, param ->
                    val expectedParamType = expectedFuncType?.parameterTypes?.getOrNull(i)
                    val declaredParamType = (param.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                    if (param.returnTypeRef !is CfirResolvedTypeRef && expectedParamType != null) {
                        param.replaceReturnTypeRef(
                            param.returnTypeRef.resolvedTypeFromPrototype(expectedParamType, param.returnTypeRef.source)
                        )
                    }
                    context.storeValueParameterIfNeeded(param, session)
                    declaredParamType ?: expectedParamType
                }
                anonFunc.body?.resolveIndependently()
                types
            }

            val returnType = when {
                anonFunc.returnTypeRef is CfirResolvedTypeRef -> (anonFunc.returnTypeRef as CfirResolvedTypeRef).coneType
                expectedFuncType != null -> expectedFuncType.returnType
                else -> anonFunc.body?.coneTypeOrNull
            }

            if (returnType != null && anonFunc.returnTypeRef !is CfirResolvedTypeRef) {
                anonFunc.replaceReturnTypeRef(
                    returnType.toCfirResolvedTypeRef(anonFunc.returnTypeRef.source, anonFunc.returnTypeRef),
                )
            }

            if (returnType != null && parameterTypes.all { it != null }) {
                // CfirAnonymousFunctionExpression.coneTypeOrNull is derived from anonymousFunction.typeRef.
                // Keep the source of truth on declaration side instead of writing expression cone type directly.
                val lambdaType = ConeFunctionType(parameterTypes.filterNotNull(), returnType)
                anonFunc.replaceTypeRef(lambdaType.toCfirResolvedTypeRef(anonFunc.typeRef.source, anonFunc.typeRef))
            }
            anonFunc.replaceControlFlowGraphReference(components.dataFlowAnalyzer.exitFunction(anonFunc))
            components.dataFlowAnalyzer.enterAnonymousFunctionExpression(anonymousFunctionExpression)
            anonymousFunctionExpression
        }
    }

    // ── Range ─────────────────────────────────────────────────────────────────

    override fun transformRangeExpression(
        rangeExpression: CfirRangeExpression,
        data: ResolutionMode,
    ): CfirExpression {
        rangeExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val startType = rangeExpression.start.coneTypeOrNull
        val endType   = rangeExpression.end.coneTypeOrNull
        val elementType = when {
            startType != null && startType == endType -> IdealTypeResolver.resolveIfIdeal(startType, null)
            startType != null                         -> IdealTypeResolver.resolveIfIdeal(startType, null)
            else                                      -> ConePrimitiveType.INT64
        }
        rangeExpression.replaceConeTypeOrNull(
            constructNamedType(
                classId = StdlibClassIds.Range,
                typeArguments = listOf(elementType),
            )
        )
        return rangeExpression
    }

    // ── Spawn ─────────────────────────────────────────────────────────────────

    override fun transformSpawnExpression(
        spawnExpression: CfirSpawnExpression,
        data: ResolutionMode,
    ): CfirExpression {
        spawnExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val taskReturnType = spawnExpression.body.coneTypeOrNull ?: builtinTypes.unitType
        spawnExpression.replaceConeTypeOrNull(
            constructNamedType(
                classId = StdlibClassIds.Future,
                typeArguments = listOf(taskReturnType),
            )
        )
        return spawnExpression
    }

    private fun <T> completeResolvedAccess(
        access: T,
        data: ResolutionMode,
    ): T where T : CfirExpression, T : CfirResolvable {
        val candidateReference = access.calleeReference as? CfirNamedReferenceWithCandidate
        if (candidateReference != null) {
            return components.callCompleter.completeCall(access, data)
        }

        if (access.coneTypeOrNull == null) {
            when (access.calleeReference) {
                is CfirResolvedNamedReference,
                is CfirErrorNamedReference,
                -> access.replaceConeTypeOrNull(components.typeFromCallee(access))
                else -> Unit
            }
        }
        return access
    }

    // ── Stdlib / ClassId Helpers ──────────────────────────────────────────────

    private fun stdlibStringType(): ConeCangJieType {
        val symbol = components.symbolProvider.getClassLikeSymbolByClassId(StdlibClassIds.String)
        if (symbol != null) {
            return constructClassLikeType(symbol, StdlibClassIds.String, emptyList())
        }
        return ConeClassLikeType(StdlibClassIds.String.toLookupTag())
    }

    private fun constructNamedType(
        classId: ClassId,
        typeArguments: List<ConeTypeProjection> = emptyList(),
    ): ConeCangJieType {
        val symbol = components.symbolProvider.getClassLikeSymbolByClassId(classId)
        return if (symbol != null) constructClassLikeType(symbol, classId, typeArguments)
        else ConeClassLikeType(classId.toLookupTag(), typeArguments)
    }

    /**
     * optional chain 的结果语义始终是 `Option<result>`。
     *
     * 本轮不做官方的完整 match/Some/None 解糖，只在 resolve 入口保证类型提升语义成立。
     */
    private fun liftOptionalChainResultType(resultType: ConeCangJieType?): ConeCangJieType {
        val effectiveResultType = resultType ?: return ConeErrorType(
            ConeSimpleDiagnostic("optional chain result type is unresolved", DiagnosticKind.InferenceError)
        )
        return constructNamedType(
            classId = StdlibClassIds.Option,
            typeArguments = listOf(effectiveResultType),
        )
    }

    /**
     * 从整条 optional chain 内部链条中找到 quest 包装的链首表达式。
     *
     * 链内普通访问/调用/索引节点不参与 optional 语义判定，真正需要校验的是最外层
     * `CfirOptionalExpression` 对应的 base expression 类型。
     */
    private fun CfirExpression.optionalChainRootExpression(): CfirExpression? = when (this) {
        is CfirOptionalExpression -> expression
        is CfirQualifiedAccessExpression -> explicitReceiver?.optionalChainRootExpression()
            ?: dispatchReceiver?.optionalChainRootExpression()
        is CfirFunctionCall -> explicitReceiver?.optionalChainRootExpression()
        is CfirSubscriptExpression -> receiver.optionalChainRootExpression()
        else -> null
    }

    private fun constructClassLikeType(
        symbol: CfirClassLikeSymbol<*>,
        classId: ClassId,
        typeArguments: List<ConeTypeProjection>,
    ): ConeCangJieType = when (symbol) {
        is CfirTypeAliasSymbol -> ConeTypeAliasType(classId, typeArguments = typeArguments)

        is CfirPrimitiveTypeSymbol -> ConePrimitiveType(symbol.kind)
        is CfirInterfaceSymbol -> ConeClassLikeType(classId.toLookupTag(), typeArguments, isInterface = true)
        is CfirStructSymbol -> ConeStructType(classId.toLookupTag(), typeArguments)
        is CfirEnumSymbol -> ConeEnumType(classId.toLookupTag(), typeArguments, isRefEnum = symbol.isRefEnum)
        else -> ConeClassLikeType(classId.toLookupTag(), typeArguments)
    }

    // ── Common Supertype ──────────────────────────────────────────────────────

    private fun commonSupertype(types: List<ConeCangJieType>): ConeCangJieType {
        if (types.isEmpty()) return builtinTypes.unitType
        val first = types.first()
        if (types.all { it == first }) return first

        val nonNothing = types.filter { it != ConePrimitiveType.NOTHING }
        if (nonNothing.isEmpty()) return ConePrimitiveType.NOTHING
        if (nonNothing.size == 1) return nonNothing.first()

        val typeCheckerContext = session.typeContext
        val chains = nonNothing.map { collectSupertypeChain(it, typeCheckerContext) }

        val commonTypes = chains.reduce { acc, chain ->
            acc.filter { candidate ->
                chain.any { typeCheckerContext.isSameTypeConstructor(candidate, it) }
            }
        }

        if (commonTypes.isEmpty()) return ConeUnionType(types.toSet())

        return commonTypes.firstOrNull { candidate ->
            commonTypes.none { other ->
                !typeCheckerContext.isSameTypeConstructor(candidate, other) &&
                        collectSupertypeChain(other, typeCheckerContext).any {
                            typeCheckerContext.isSameTypeConstructor(it, candidate)
                        }
            }
        } ?: commonTypes.first()
    }

    /**
     * 从某个 effect command 类型中提取 `Command<T>` 的 `T`。
     *
     * 这里直接沿解析后的超类型链查找 `stdx.effect.Command`，
     * 让 class/interface alias 展开后的实现类型都能复用同一条逻辑。
     */
    private fun resolveHandleCommandResultType(commandPattern: CfirCommandTypePattern): ConeCangJieType? {
        val commandType = (commandPattern.typeRefs.firstOrNull() as? CfirResolvedTypeRef)?.coneType ?: return null
        return findCommandSupertype(commandType)?.typeArguments?.firstOrNull()?.type
    }

    private fun resolveCommandPatternTypeRefs(commandPattern: CfirCommandTypePattern) {
        val additionalTypeParameters = context.containers
            .asSequence()
            .filterIsInstance<CfirDeclaration>()
            .flatMap { extractTypeParameters(it).asSequence() }
            .toList()

        val config = CfirTypeResolutionConfiguration(
            useSiteFile = context.file,
            topContainer = context.containers.lastOrNull(),
        ).withAdditionalTypeParameters(additionalTypeParameters)

        commandPattern.transformTypeRefs(specificTypeResolverTransformer, config)
    }

    private fun findCommandSupertype(type: ConeCangJieType?): ConeClassLikeType? {
        if (type == null) return null
        return collectSupertypeChain(type, session.typeContext)
            .filterIsInstance<ConeClassLikeType>()
            .firstOrNull { it.lookupTag.classId == StdlibClassIds.Command }
    }

    private fun isExceptionLikeType(type: ConeCangJieType): Boolean {
        val exceptionType = constructNamedType(StdlibClassIds.Exception)
        val errorType = constructNamedType(StdlibClassIds.Error)
        return AbstractTypeChecker.isSubtypeOf(session.typeContext, type, exceptionType) == true ||
                AbstractTypeChecker.isSubtypeOf(session.typeContext, type, errorType) == true
    }

    private fun normalizeTypeForJoin(type: ConeCangJieType?): ConeCangJieType? {
        return when (type) {
            is ConeErrorType -> type.delegatedType ?: type
            else -> type
        }
    }

    private inline fun <T> withClearedEffectHandlers(block: () -> T): T {
        if (effectHandlerStack.isEmpty()) return block()

        val snapshot = effectHandlerStack.toList()
        effectHandlerStack.clear()
        return try {
            block()
        } finally {
            effectHandlerStack.addAll(snapshot)
        }
    }

    private fun collectSupertypeChain(
        type: ConeCangJieType,
        context: ConeInferenceContext,
    ): List<ConeCangJieType> {
        val result = mutableListOf<ConeCangJieType>()
        val visited = mutableSetOf<ConeCangJieType>()
        val queue = ArrayDeque<ConeCangJieType>()
        queue.add(type)
        visited.add(type)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result += current
            val constructor = with(context) { (current as? ConeRigidType)?.typeConstructor() } ?: continue
            val supertypes = with(context) {
                constructor.supertypes().mapNotNull { it as? ConeCangJieType }
            }
            supertypes.forEach { supertype ->
                if (visited.add(supertype)) queue.add(supertype)
            }
        }
        return result
    }

    // ── Scope Utilities ───────────────────────────────────────────────────────

    /** 在新的空局部作用域里执行 [block]，退出后恢复外层作用域。薄壳包装 `context.forBlock`。 */
    private inline fun <T> withNewLocalScope(crossinline block: () -> T): T =
        context.forBlock(session) { block() }

    /**
     * `super` 的语义在仓颉里是固定的：
     * 1. 只能出现在 class 内部；
     * 2. 绑定到当前 class 声明的直接父 class；
     * 3. 不能落到接口父类型，也不能从继承链上做兜底推断。
     *
     * 这里在进入 tower resolve 前就把接收者类型确定下来，
     * 避免后续 `ExpressionReceiverValue.scope()` 再遇到未解析的 `super`。
     */
    private fun resolveImplicitSuperReceiverType(owner: CfirClass): ConeCangJieType {
        val directClassSuperTypes = owner.directClassSuperTypes()
        return when (directClassSuperTypes.size) {
            1 -> directClassSuperTypes.single()
            0 -> errorType("`super` requires a direct class supertype in ${owner.name}")
            else -> errorType("`super` is ambiguous because ${owner.name} declares multiple direct class supertypes")
        }
    }

    /**
     * 预留给未来显式 `super<T>` / `super<Base>` 语法：
     * 即使语法层已经指定了目标类型，也必须严格受“当前 class 的直接父 class”约束。
     */
    private fun resolveExplicitSuperReceiverType(
        owner: CfirClass,
        resolvedSuperTypeRef: CfirResolvedTypeRef,
    ): ConeCangJieType {
        val requestedType = resolvedSuperTypeRef.coneType
        if (!requestedType.isDirectClassSuperType()) {
            return errorType("`super` can only target a direct class supertype of ${owner.name}")
        }

        val directClassSuperTypes = owner.directClassSuperTypes()
        if (directClassSuperTypes.none { it == requestedType }) {
            return errorType("`super` can only target a direct class supertype of ${owner.name}")
        }

        return requestedType
    }

    private fun CfirClass.directClassSuperTypes(): List<ConeCangJieType> {
        return superTypeRefs
            .filterIsInstance<CfirResolvedTypeRef>()
            .map(CfirResolvedTypeRef::coneType)
            .filter { candidate -> candidate.isDirectClassSuperType() }
    }

    private fun ConeCangJieType.isDirectClassSuperType(): Boolean = when (this) {
        is ConeClassLikeType -> !isInterface
        is ConeStructType, is ConeEnumType -> true
        else -> false
    }

    // ── Small Extension Utilities ─────────────────────────────────────────────

    private fun CfirExpression.resolveIndependently() {
        transform<CfirElement, ResolutionMode>(transformer, ResolutionMode.ContextIndependent)
    }

    private fun CfirExpression.resolveIndependently(body: CfirBlock?) {
        body?.transform<CfirElement, ResolutionMode>(transformer, ResolutionMode.ContextIndependent)
    }

    private val ResolutionMode.expectedTypeOrNull: ConeCangJieType?
        get() = (this as? ResolutionMode.WithExpectedType)?.expectedTypeRef?.coneType

    private fun <T : CfirQualifiedAccessExpression> resolveAccessTypeArguments(access: T): T {
        if (access.typeArguments.isEmpty()) return access

        val additionalTypeParameters = context.containers
            .asSequence()
            .filterIsInstance<CfirDeclaration>()
            .flatMap { extractTypeParameters(it).asSequence() }
            .toList()

        val config = CfirTypeResolutionConfiguration(
            useSiteFile = context.file,
            topContainer = context.containers.lastOrNull(),
        ).withAdditionalTypeParameters(additionalTypeParameters)

        val resolvedTypeArguments = access.typeArguments.map { typeRef ->
            when (typeRef) {
                is CfirResolvedTypeRef -> typeRef
                is CfirImplicitTypeRef -> typeRef
                else -> specificTypeResolverTransformer.transformTypeRef(typeRef, config)
            }
        }

        access.replaceTypeArguments(resolvedTypeArguments)
        return access
    }

    private fun resolveSuperTypeRef(typeRef: CfirTypeRef): CfirTypeRef {
        if (typeRef is CfirResolvedTypeRef || typeRef is CfirImplicitTypeRef) return typeRef

        val additionalTypeParameters = context.containers
            .asSequence()
            .filterIsInstance<CfirDeclaration>()
            .flatMap { extractTypeParameters(it).asSequence() }
            .toList()

        val config = CfirTypeResolutionConfiguration(
            useSiteFile = context.file,
            topContainer = context.containers.lastOrNull(),
        ).withAdditionalTypeParameters(additionalTypeParameters)

        return specificTypeResolverTransformer.transformTypeRef(typeRef, config)
    }

    private fun extractTypeParameters(declaration: CfirDeclaration): List<CfirTypeParameter> = when (declaration) {
        is CfirClass -> declaration.typeParameters
        is CfirInterface -> declaration.typeParameters
        is CfirStruct -> declaration.typeParameters
        is CfirEnum -> declaration.typeParameters
        is CfirFunction -> declaration.typeParameters
        is CfirConstructor -> declaration.typeParameters
        is CfirProperty -> declaration.typeParameters
        is CfirFieldVariable -> declaration.typeParameters
        is CfirValueParameter -> declaration.typeParameters
        is CfirExtend -> declaration.typeParameters
        is CfirTypeAlias -> declaration.typeParameters
        is CfirPatternVariable -> declaration.typeParameters
        is CfirMacroDeclaration -> declaration.typeParameters
        is CfirMainFunction -> declaration.typeParameters
        is CfirFinalizer -> declaration.typeParameters
        is CfirEnumConstructor -> declaration.typeParameters
        else -> emptyList()
    }
}
