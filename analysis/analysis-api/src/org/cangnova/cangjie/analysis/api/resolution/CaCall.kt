package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.signatures.CaCallableSignature
import org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.*
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.psi.CjExpression

/**
 * 单个调用的公开语义视图。
 *
 * 该视图稳定暴露调用目标、适用性、接收者拆分、显式类型参数以及实参与形参映射,
 * 供 Analysis API 测试框架、引用服务、渲染器和上层工具共享。
 *
 * 对齐 Kotlin Analysis API 的 `KaCall`。
 */
sealed interface CaCall : CaLifetimeOwner

/**
 * 对函数的调用,或对变量/属性的简单/复合访问的视图。
 *
 * @param S 实际可调用符号类型。
 * @param C 与符号匹配的签名类型。
 */
sealed interface CaCallableMemberCall<S : CaCallableSymbol, C : CaCallableSignature<S>> : CaCall {
    /**
     * 调用被解析到的部分应用符号:已绑定 dispatch receiver、已做 use-site 替换的签名等,
     * 但还可能缺少实参/访问模式等运行时信息。
     */
    val partiallyAppliedSymbol: CaPartiallyAppliedSymbol<S, C>

    /**
     * 推断出的类型实参映射;键来自 [partiallyAppliedSymbol] 的类型形参。
     *
     * 解析或推断失败时,该映射可能为空。
     */
    val typeArgumentsMapping: Map<CaTypeParameterSymbol, CaType>
}

/**
 * 函数调用视图。
 *
 * 在 [CaCallableMemberCall] 的基础上,额外暴露实参 → 形参的稳定映射,
 * 供上层渲染、引用扫描等使用。
 */
@OptIn(CaImplementationDetail::class, CaExperimentalApi::class)
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaFunctionCall<S : CaFunctionSymbol> : CaSingleCall<S, CaFunctionSignature<S>>,
    CaCallableMemberCall<S, CaFunctionSignature<S>> {

    /**
     * 实参表达式到对应值形参签名的稳定映射。
     *
     * 对 `vararg` 形参,多个实参可能映射到同一个 [CaValueParameterSymbol]。
     *
     * @see contextArgumentMapping
     * @see combinedArgumentMapping
     */
    val valueArgumentMapping: Map<CjExpression, CaVariableSignature<CaValueParameterSymbol>>


    /**
     * 实参表达式到形参签名的"合并"映射,同时包含值实参与上下文实参。
     *
     * 对 `vararg` 形参,多个实参可能映射到同一个 [CaValueParameterSymbol]。
     *
     * @see valueArgumentMapping
     * @see contextArgumentMapping
     */
    val combinedArgumentMapping: Map<CjExpression, CaVariableSignature<CaParameterSymbol>>

    /**
     * 实参到值形参的映射(兼容入口)。
     *
     * 调用方应优先使用 [valueArgumentMapping] 或 [combinedArgumentMapping]。
     */
    @Deprecated("Use 'valueArgumentMapping' or 'combinedArgumentMapping' instead", ReplaceWith("valueArgumentMapping"))
    val argumentMapping: Map<CjExpression, CaVariableSignature<CaValueParameterSymbol>>
        get() = valueArgumentMapping

    /**
     * 部分应用符号(函数族特化)。
     *
     * 已 deprecated,建议直接访问内部组件。
     */
    @Deprecated("Use the content of the `partiallyAppliedSymbol` directly instead")
    override val partiallyAppliedSymbol: CaPartiallyAppliedSymbol<S, CaFunctionSignature<S>>
}

/**
 * "单 / 多"调用的共同父接口。
 *
 * 在 Kotlin Analysis API 中既存在普通单调用,也存在像 `for` 循环、属性委托
 * 这类一次性解析出多个相关调用的场景;[CaSingleOrMultiCall] 是它们的统一锚点。
 */
sealed interface CaSingleOrMultiCall : CaLifetimeOwner


/**
 * 单个可调用符号的调用视图。
 *
 * 既被 [CaFunctionCall] 也被属性/变量访问([CaVariableAccessCall])复用,
 * 抽象出"签名 + dispatch receiver + 类型实参"的最小集合。
 */
@CaExperimentalApi
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaSingleCall<S : CaCallableSymbol, C : CaCallableSignature<S>> : CaSingleOrMultiCall {
    /**
     * 函数或变量声明的 use-site 签名。
     */
    val signature: C

    /**
     * 调用的 [dispatch receiver](https://kotlin.github.io/analysis-api/receivers.html#types-of-receivers)。
     *
     * 仅当目标声明位于某个类型内部时存在。
     */
    val dispatchReceiver: CaReceiverValue?

    /**
     * 推断出的类型实参映射,键来自 [signature] 的类型形参。
     *
     * 解析或推断失败时可能为空。
     */
    val typeArgumentsMapping: Map<CaTypeParameterSymbol, CaType>
}

/**
 * 调用 receiver 的公开视图。
 *
 * 现阶段稳定层只暴露 receiver 的最终语义类型;
 * 是否区分 explicit / implicit / smart-cast 暂作为内部细节,后续按需扩展。
 */
sealed interface CaReceiverValue : CaLifetimeOwner {
    /**
     * Receiver 的推断类型;若发生 smart-cast,这里是经过 smart-cast 之后的类型。
     */
    val type: CaType
}
