package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckArguments
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckCallableReferenceExpectedType
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckDispatchReceiver
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
        CfirCheckDispatchReceiver,
        CfirCheckExtensionReceiver,
        CfirCheckArguments,
        CfirEagerResolveOfCallableReferences,
    )

    /**
     * 构造器委托调用 `this(...)` / `super(...)`。
     *
     * 对齐 Kotlin FIR 的 `CallKind.DelegatingConstructorCall`：它使用 callable
     * 参数映射、类型实参映射、候选约束与 lambda 期望类型解析，但候选集合由构造器
     * 语义专用入口提供，而不是走普通 tower 名字查找。
     */
    data object DelegatingConstructorCall : CallKind(
        CfirCheckVisibility,
        CfirMapArguments,
        CfirMapTypeArguments,
        CfirCreateFreshTypeVariableSubstitutorStage,
        CfirCheckDispatchReceiver,
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
        CfirCheckDispatchReceiver,
        CfirCheckExtensionReceiver,
        CfirCheckCallableReferenceExpectedType,
    )

    data object EnumConstructorCall : CallKind(
        CfirCheckVisibility,
        CfirMapArguments,
        CfirMapTypeArguments,
        CfirCreateFreshTypeVariableSubstitutorStage,
        CfirCheckDispatchReceiver,
        CfirCheckArguments,
    )
}
