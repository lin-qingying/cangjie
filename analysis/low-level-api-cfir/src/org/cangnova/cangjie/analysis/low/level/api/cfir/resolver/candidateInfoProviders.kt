/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.resolver

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.ImplicitReceiverValue
import org.cangnova.cangjie.cfir.resolve.calls.ReceiverValue
import org.cangnova.cangjie.cfir.resolve.calls.candidate.*
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind

/**
 * A supplier of information for resolving a call against a single provided candidate.
 * Implementors of this interface form a candidate from provided resolution parameters to fit requested resolution mode.
 * This includes creating artificial CallInfo, combining receivers and generating CallKind with specific resolution sequence.
 */
interface CandidateInfoProvider {
    fun callInfo(): CallInfo

    fun callKind(): CallKind

    fun explicitReceiverKind(): ExplicitReceiverKind

    fun dispatchReceiverValue(): ReceiverValue?

    fun implicitExtensionReceiverValue(): ImplicitReceiverValue<*>?

    fun shouldFailBeforeResolve(): Boolean
}

abstract class AbstractBaseCandidateInfoProvider(
    protected val resolutionParameters: ResolutionParameters,
    protected val firFile: CfirFile,
    protected val firSession: CfirSession,
) : CandidateInfoProvider {
    override fun callInfo(): CallInfo = with(resolutionParameters) {
        CallInfo(
            firFile, // TODO: consider passing more precise info here, if needed
            callKind = callKind(),
            name = callableSymbol.name,
            explicitReceiver = explicitReceiver,
            argumentList = argumentList,
            typeArguments = typeArgumentList,
            containingDeclarations = emptyList(), // TODO - maybe we should pass declarations from context here (no visible differences atm)
            containingFile = firFile,
            resolutionMode = ResolutionMode.ContextIndependent,
            isUsedAsGetClassReceiver = false,
            session = firSession,
            implicitInvokeMode = ImplicitInvokeMode.None,
        )
    }

    override fun shouldFailBeforeResolve(): Boolean = false
}

abstract class AbstractExtensionCandidateInfoProvider(
    resolutionParameters: ResolutionParameters,
    firFile: CfirFile,
    firSession: CfirSession,
) : AbstractBaseCandidateInfoProvider(resolutionParameters, firFile, firSession) {
    override fun callKind(): CallKind = buildCallKindWithCustomResolutionSequence {
        checkExtensionReceiver = false
    }

    override fun explicitReceiverKind(): ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER

    // Right now it's impossible to reason about dispatch receiver when candidate comes from arbitrary scope with no other information.
    // So dispatch receiver is not passed from provider and later not checked during the resolution sequence.
    override fun dispatchReceiverValue(): ReceiverValue? = null

    override fun implicitExtensionReceiverValue(): ImplicitReceiverValue<*>? = null

    override fun shouldFailBeforeResolve(): Boolean = false
}

/**
 * Provider for [SingleCandidateResolutionMode.CHECK_EXTENSION_FOR_COMPLETION] mode.
 */
class CheckExtensionForCompletionCandidateInfoProvider(
    resolutionParameters: ResolutionParameters,
    firFile: CfirFile,
    firSession: CfirSession,
) : AbstractExtensionCandidateInfoProvider(resolutionParameters, firFile, firSession)
