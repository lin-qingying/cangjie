package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckArguments
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckCallableReferenceExpectedType
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckExtensionReceiver
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckVisibility
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCreateFreshTypeVariableSubstitutorStage
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirEagerResolveOfCallableReferences
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirInitializeEmptyArgumentMap
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirMapArguments
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirMapTypeArguments
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStage

sealed class CallKind(
    vararg val resolutionSequence: ResolutionStage,
    additionalStages: Array<ResolutionStage> = emptyArray(),
) {
    val resolutionSequenceWithAdditionalStages: Array<out ResolutionStage> =
        arrayOf(*resolutionSequence, *additionalStages)

    data object Function : CallKind(
        CfirCheckVisibility,
        CfirMapArguments,
        CfirMapTypeArguments,
        CfirCreateFreshTypeVariableSubstitutorStage,
        CfirCheckExtensionReceiver,
        CfirCheckArguments,
        CfirEagerResolveOfCallableReferences,
    )

    /**
     * Variable/name access chain.
     *
     * Align with Kotlin's `CallKind.VariableAccess` contract:
     * argument map must be initialized even without argument checking.
     */
    data object NamedValueAccess : CallKind(
        CfirCheckVisibility,
        CfirMapTypeArguments,
        CfirInitializeEmptyArgumentMap,
        CfirCreateFreshTypeVariableSubstitutorStage,
        CfirCheckExtensionReceiver,
        CfirCheckCallableReferenceExpectedType,
    )

    data object EnumConstructorCall : CallKind(
        CfirCheckVisibility,
        CfirMapArguments,
        CfirMapTypeArguments,
        CfirCreateFreshTypeVariableSubstitutorStage,
        CfirCheckArguments,
    )
}
