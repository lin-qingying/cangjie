package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckArguments
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckCallableReferenceExpectedType
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckDispatchReceiver
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckExtensionReceiver
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckExpectedReturnTypeBeforeArguments
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCheckVisibility
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCreateFreshTypeVariableSubstitutorStage
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirEagerResolveOfCallableReferences
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirInitializeEmptyArgumentMap
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirMapArguments
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirMapTypeArguments
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStage

/** 调用种类及其候选检查阶段序列。 */
sealed class CallKind(
    /** 常规候选检查阶段序列。 */
    vararg val resolutionSequence: ResolutionStage,
    additionalStages: Array<ResolutionStage> = emptyArray(),
) {
    /** 常规阶段追加额外阶段后的完整阶段序列。 */
    val resolutionSequenceWithAdditionalStages: Array<out ResolutionStage> =
        arrayOf(*resolutionSequence, *additionalStages)

    /** 普通函数调用。 */
    data object Function : CallKind(
        CfirCheckVisibility,
        CfirMapArguments,
        CfirMapTypeArguments,
        CfirCreateFreshTypeVariableSubstitutorStage,
        CfirCheckDispatchReceiver,
        CfirCheckExtensionReceiver,
        CfirCheckExpectedReturnTypeBeforeArguments,
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
     * 变量/名字访问链。
     *
     * 对齐 Kotlin `CallKind.VariableAccess`：即使没有实参检查，也必须初始化 argument map。
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

    /** enum constructor 调用。 */
    data object EnumConstructorCall : CallKind(
        CfirCheckVisibility,
        CfirMapArguments,
        CfirMapTypeArguments,
        CfirCreateFreshTypeVariableSubstitutorStage,
        CfirCheckDispatchReceiver,
        CfirCheckExpectedReturnTypeBeforeArguments,
        CfirCheckArguments,
        CfirEagerResolveOfCallableReferences,
    )
}
