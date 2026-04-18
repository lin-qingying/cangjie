/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.resolver

import org.cangnova.cangjie.cfir.CfirSession
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirEmptyArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.ImplicitReceiverValue
import org.cangnova.cangjie.cfir.resolve.calls.candidate.*
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStageRunner
import org.cangnova.cangjie.cfir.resolve.createConeDiagnosticForCandidateWithError
import org.cangnova.cangjie.cfir.resolve.inference.CfirCallCompleter
import org.cangnova.cangjie.cfir.symbols.impl.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeProjection
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability

class SingleCandidateResolver(
    private val firSession: CfirSession,
    private val firFile: CfirFile,
) {
    val bodyResolveComponents = createStubBodyResolveComponents(firSession)
    private val firCallCompleter = CfirCallCompleter(
        bodyResolveComponents.transformer,
        bodyResolveComponents,
    )
    private val resolutionStageRunner = ResolutionStageRunner()

    fun resolveSingleCandidate(
        resolutionParameters: ResolutionParameters
    ): CfirFunctionCall? {

        val infoProvider = createCandidateInfoProvider(resolutionParameters)
        if (infoProvider.shouldFailBeforeResolve())
            return null

        val callInfo = infoProvider.callInfo()
        val explicitReceiverKind = infoProvider.explicitReceiverKind()
        val dispatchReceiverValue = infoProvider.dispatchReceiverValue()
        val implicitExtensionReceiverValue = infoProvider.implicitExtensionReceiverValue()

        val resolutionContext = bodyResolveComponents.transformer.resolutionContext

        val candidate = CandidateFactory(resolutionContext, callInfo).createCandidate(
            callInfo,
            resolutionParameters.callableSymbol,
            explicitReceiverKind = explicitReceiverKind,
            dispatchReceiver = dispatchReceiverValue?.receiverExpression,
            givenExtensionReceiver = if (explicitReceiverKind.isExtensionReceiver)
                callInfo.explicitReceiver
            else
                implicitExtensionReceiverValue?.receiverExpression,
            scope = null,
        )

        val applicability = resolutionStageRunner.processCandidate(candidate, resolutionContext, stopOnCfirstError = true)

        val fakeCall = if (candidate.isSuccessful) {
            buildCallForResolvedCandidate(candidate, resolutionParameters)
        } else if (
            resolutionParameters.allowUnsafeCall && applicability == CandidateApplicability.UNSAFE_CALL ||
            resolutionParameters.allowUnstableSmartCast && applicability == CandidateApplicability.UNSTABLE_SMARTCAST
        ) {
            resolutionStageRunner.fullyProcessCandidate(candidate, resolutionContext)
            buildCallForCandidateWithError(candidate, applicability, resolutionParameters)
        } else {
            return null
        }

        return firCallCompleter.completeCall(fakeCall, ResolutionMode.ContextIndependent)
    }

    private fun createCandidateInfoProvider(resolutionParameters: ResolutionParameters): CandidateInfoProvider {
        return when (resolutionParameters.singleCandidateResolutionMode) {
            SingleCandidateResolutionMode.CHECK_EXTENSION_FOR_COMPLETION -> CheckExtensionForCompletionCandidateInfoProvider(
                resolutionParameters,
                firFile,
                firSession
            )
        }
    }

    private fun buildCallForResolvedCandidate(candidate: Candidate, resolutionParameters: ResolutionParameters): CfirFunctionCall =
        buildFunctionCall {
            calleeReference = CfirNamedReferenceWithCandidate(
                source = null,
                name = resolutionParameters.callableSymbol.name,
                candidate = candidate
            )
        }

    private fun buildCallForCandidateWithError(
        candidate: Candidate,
        applicability: CandidateApplicability,
        resolutionParameters: ResolutionParameters
    ): CfirFunctionCall {
        val diagnostic = createConeDiagnosticForCandidateWithError(applicability, candidate)
        val name = resolutionParameters.callableSymbol.name
        return buildFunctionCall {
            calleeReference = CfirErrorReferenceWithCandidate(source = null, name, candidate, diagnostic)
        }
    }
}

/**
 * @param allowUnsafeCall if true, then candidate is resolved even if receiver's nullability doesn't match
 * @param allowUnstableSmartCast if true, then candidate is resolved even if it requires unstable smart cast
 */
class ResolutionParameters(
    val singleCandidateResolutionMode: SingleCandidateResolutionMode,
    val callableSymbol: CfirCallableSymbol<*>,
    val implicitReceiver: ImplicitReceiverValue<*>? = null,
    val explicitReceiver: CfirExpression? = null,
    /** THIS IS UNSAFE TO PASS ORIGINAL ARGUMENTS. THEY HAVE TO BE COPIED TO AVOID MUTABILITY ISSUES */
    val argumentList: CfirArgumentList = CfirEmptyArgumentList,
    val typeArgumentList: List<CfirTypeProjection> = emptyList(),
    val allowUnsafeCall: Boolean = false,
    val allowUnstableSmartCast: Boolean = false,
)

enum class SingleCandidateResolutionMode {
    /**
     * Run resolution stages necessary to type check extension receiver (explicit/implicit) for candidate function.
     * Candidate is expected to be taken from context scope.
     * Arguments and type arguments are not expected and not checked.
     * Explicit receiver can be passed and will always be interpreted as extension receiver.
     */
    CHECK_EXTENSION_FOR_COMPLETION,
}
