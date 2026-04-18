/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.resolver

import com.intellij.openapi.diagnostic.logger
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.ContextCollector
import org.cangnova.cangjie.analysis.utils.printer.parentsOfType
import org.cangnova.cangjie.cfir.CfirSession
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunctionCopy
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.expressions.impl.CfirResolvedArgumentList
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.ScopeSession
import org.cangnova.cangjie.cfir.resolve.calls.*
import org.cangnova.cangjie.cfir.resolve.calls.candidate.fullyProcessCandidate
import org.cangnova.cangjie.cfir.resolve.calls.tower.CfirTowerResolver
import org.cangnova.cangjie.cfir.resolve.diagnostics.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.resolve.initialTypeOfCandidate
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirExpressionsResolveTransformer
import org.cangnova.cangjie.cfir.symbols.SymbolInternals
import org.cangnova.cangjie.cfir.symbols.impl.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirEntry
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.util.PrivateForInline
import org.cangnova.cangjie.utils.exceptions.logErrorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

class AllCandidatesResolver(private val firSession: CfirSession) {
    private val scopeSession = ScopeSession()

    // This transformer is not intended for actual transformations and created here only to simplify access to resolve components
    private val stubBodyResolveTransformer = CfirBodyResolveTransformer(
        session = firSession,
        phase = CfirResolvePhase.BODY_RESOLVE,
        implicitTypeOnly = false,
        scopeSession = scopeSession,
    )

    private val bodyResolveComponents = object : StubBodyResolveTransformerComponents(
        firSession,
        scopeSession,
        stubBodyResolveTransformer,
        stubBodyResolveTransformer.context,
    ) {
        val collector = AllCandidatesCollector(this, resolutionStageRunner)
        val towerResolver = CfirTowerResolver(this, resolutionStageRunner, collector)
        override val callResolver = CfirCallResolver(this, towerResolver)

        init {
            callResolver.initTransformer(CfirExpressionsResolveTransformer(stubBodyResolveTransformer))
        }
    }

    private val resolutionContext = ResolutionContext(firSession, bodyResolveComponents, bodyResolveComponents.transformer.context)

    fun getAllCandidates(
        resolutionFacade: LLResolutionFacade,
        qualifiedAccess: CfirQualifiedAccessExpression,
        calleeName: Name,
        element: CjElement,
        resolutionMode: ResolutionMode,
    ): List<OverloadCandidate> {
        initializeBodyResolveContext(resolutionFacade, element)

        val copiedAccess = copyQualifiedAccess(qualifiedAccess, element) ?: return emptyList()
        return run {
            bodyResolveComponents.callResolver
                .collectAllCandidates(
                    copiedAccess,
                    calleeName,
                    bodyResolveComponents.context.containers,
                    resolutionContext,
                    resolutionMode,
                )
                .apply { postProcessCandidates(copiedAccess) }
        }
    }

    fun getAllCandidatesForDelegatedConstructor(
        resolutionFacade: LLResolutionFacade,
        delegatedConstructorCall: CfirDelegatedConstructorCall,
        derivedClassLookupTag: ConeClassLikeLookupTag,
        element: CjElement
    ): List<OverloadCandidate> {
        initializeBodyResolveContext(resolutionFacade, element)

        val constructedType = delegatedConstructorCall.constructedTypeRef.coneType as ConeClassLikeType
        return run {
            val callInfo = bodyResolveComponents.callResolver.callInfoForDelegatingConstructorCall(
                delegatedConstructorCall,
                constructedType,
            )

            with(bodyResolveComponents.towerResolver) {
                reset()
                runResolverForDelegatingConstructor(callInfo, constructedType, derivedClassLookupTag, resolutionContext)
            }

            bodyResolveComponents.collector.allCandidates
                .map { OverloadCandidate(it, isInBestCandidates = it in bodyResolveComponents.collector.bestCandidates()) }
                .apply { postProcessCandidates(delegatedConstructorCall) }
        }
    }

    @OptIn(PrivateForInline::class, SymbolInternals::class)
    private fun initializeBodyResolveContext(resolutionFacade: LLResolutionFacade, element: CjElement) {
        val firFile = element.containingCjFile.getOrBuildCfirFile(resolutionFacade)

        // Set up needed context to get all candidates.
        val towerContext = ContextCollector.process(resolutionFacade, firFile, element)?.towerDataContext
        towerContext?.let { bodyResolveComponents.context.replaceTowerDataContext(it) }
        val containingDeclarations =
            element.parentsOfType<CjDeclaration>().map { it.resolveToCfirSymbol(resolutionFacade).fir }.toList().asReversed()
        bodyResolveComponents.context.containers.addAll(containingDeclarations)

        // `towerContext` from above should already contain all the scopes for the file,
        // so we just set it manually without calling `withFile`
        bodyResolveComponents.context.file = firFile
    }

    @OptIn(ConstraintSystemCompletionMode.ExclusiveForOverloadResolutionByLambdaReturnType::class)
    private fun <T> List<OverloadCandidate>.postProcessCandidates(call: T) where T : CfirExpression, T : CfirResolvable {
        val callCompleter = bodyResolveComponents.callCompleter
        val analyzer = callCompleter.createPostponedArgumentsAnalyzer(resolutionContext)
        val components = resolutionContext.bodyResolveComponents

        forEach { overloadCandidate ->
            val candidate = overloadCandidate.candidate

            // Runs resolution stages. In particular, this action initiates type constraints
            components.resolutionStageRunner.fullyProcessCandidate(candidate, resolutionContext)

            // Runs completion for the candidate. This step is required to solve the constraint system
            callCompleter.runCompletionForCall(
                candidate = candidate,
                // The lambda's processing logic modifies the original tree,
                // so we cannot analyze them in the current state.
                // See KT-82121 for more details.
                completionMode = ConstraintSystemCompletionMode.UNTIL_CFIRST_LAMBDA,
                call = call,
                initialType = components.initialTypeOfCandidate(candidate),
                analyzer = analyzer,
            )

            overloadCandidate.preserveCalleeInapplicability()
        }
    }

    /**
     * Post-processes a candidate to carry the callee's inapplicability over into the candidate. Without this post-processing, an issue may
     * arise where [getAllCandidates] produces "applicable" candidates with inapplicable callee references.
     *
     * For example, a function call `generic<String, String>` of function `fun <A, B, C> generic() { }` is correctly marked as inapplicable
     * by the compiler (due to the missing type argument), but the `firFile` built during [getAllCandidates] will contain an inapplicable
     * function call `generic<String, String, ERROR>` (with the missing type argument inferred as an error type). The *subsequent*
     * resolution by `bodyResolveComponents.callResolver.collectAllCandidates` feeds this call to
     * [org.cangnova.cangjie.cfir.resolve.calls.CandidateFactory], which doesn't make any guarantees for inapplicable calls. Hence, the
     * resulting candidate is *not* marked as inapplicable and needs to be post-processed.
     */
    private fun OverloadCandidate.preserveCalleeInapplicability() {
        val callSite = candidate.callInfo.callSite
        val calleeReference = callSite.toReference(firSession) as? CfirDiagnosticHolder ?: return
        val diagnostic = calleeReference.diagnostic as? ConeInapplicableCandidateError ?: return
        if (diagnostic.applicability != CandidateApplicability.INAPPLICABLE) return

        candidate.addDiagnostic(InapplicableCandidate)
    }
}

/**
 * The passed [qualifiedAccess] is copied to avoid modification of the original tree.
 *
 * The copied tree is then passed to the [org.cangnova.cangjie.cfir.resolve.calls.overloads.CfirOverloadByLambdaReturnTypeResolver]
 * which may modify the tree. In particular, it may change the callee reference and lambdas.
 *
 * There is no goal to make a proper deep copy of the subtree – it is enough to cover the known cases there
 * the modification is possible.
 */
private fun copyQualifiedAccess(
    qualifiedAccess: CfirQualifiedAccessExpression,
    element: CjElement,
): CfirQualifiedAccessExpression? = when (qualifiedAccess) {
    is CfirFunctionCall -> buildFunctionCallCopy(qualifiedAccess) {
        argumentList = when (val argumentListToCopy = qualifiedAccess.argumentList) {
            is CfirEmptyArgumentList -> argumentListToCopy
            is CfirResolvedArgumentList -> {
                val newMapping = argumentListToCopy.mapping.mapKeysTo(LinkedHashMap()) { copyArgument(it.key) }

                /**
                 * Arguments from the original argument list are used, so it has to be copied as well.
                 * This usage can be found in [org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo.arguments]
                 * and was introduced in KT-66124
                 */
                val originalArgumentList = argumentListToCopy.originalArgumentList
                val newOriginalList = if (originalArgumentList != null) {
                    buildArgumentListCopy(originalArgumentList) {
                        arguments.replaceAll(::copyArgument)
                    }
                } else {
                    null
                }

                buildResolvedArgumentList(
                    original = newOriginalList,
                    mapping = newMapping,
                )
            }

            else -> {
                logger<AllCandidatesResolver>().logErrorWithAttachment("Unexpected argument list ${argumentListToCopy::class.simpleName}") {
                    withCfirEntry("argumentList", argumentListToCopy)
                    withPsiEntry("psi", element)
                }

                return null
            }
        }
    }
    is CfirPropertyAccessExpression -> buildPropertyAccessExpressionCopy(qualifiedAccess) {}
    else -> {
        logger<AllCandidatesResolver>().logErrorWithAttachment("Unsupported qualified access ${qualifiedAccess::class.simpleName}") {
            withCfirEntry("qualifiedAccess", qualifiedAccess)
            withPsiEntry("psi", element)
        }

        null
    }
}

private fun copyArgument(argument: CfirExpression): CfirExpression = when (argument) {
    is CfirWrappedArgumentExpression -> {
        val newExpression = copyArgument(argument.expression)
        when (argument) {
            is CfirNamedArgumentExpression -> buildNamedArgumentExpressionCopy(argument) { expression = newExpression }
            is CfirSpreadArgumentExpression -> buildSpreadArgumentExpressionCopy(argument) { expression = newExpression }
        }
    }
    is CfirAnonymousFunctionExpression -> {
        buildAnonymousFunctionExpressionCopy(argument) {
            anonymousFunction = buildAnonymousFunctionCopy(argument.anonymousFunction) { symbol = CfirAnonymousFunctionSymbol() }
        }
    }
    else -> argument
}