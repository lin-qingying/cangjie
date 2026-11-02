package org.cangnova.cangjie.analysis.api.symbols

/**
 * 可由源码中单个声明节点稳定表示的公开符号。
 *
 * 区分原则：
 * - package / file 这类聚合语义不属于声明符号（它们没有单个对应的 declaration AST 节点）；
 * - class / function / property / extend 等都是声明符号。
 *
 * 声明符号必然带注解能力（通过继承 [CaAnnotatedSymbol]），
 * 同时统一暴露可见性与模态及其"是否显式书写"的元信息，便于渲染与重构等上层动作。
 */
interface CaDeclarationSymbol : CaSymbol ,CaAnnotatedSymbol{
    /**
     * 声明自身的可见性。
     */
    val visibility: CaSymbolVisibility

    /** 可见性 modifier 的显式性；`UNKNOWN` 不等同于源码隐式默认。 */
    val visibilityExplicitness: CaModifierExplicitness
        get() = if (isVisibilityExplicit) CaModifierExplicitness.EXPLICIT else CaModifierExplicitness.UNKNOWN

    /**
     * 可见性是否由源码显式写出。
     *
     * 用于区分"显式 public"与"省略修饰但语义为 public"等场景，方便渲染、迁移与诊断。
     */
    @Deprecated("Use visibilityExplicitness to distinguish implicit from unknown visibility.")
    val isVisibilityExplicit: Boolean

    /**
     * 声明自身的模态。
     *
     * 对没有模态语义的声明（例如局部变量）可能为 `null`。
     */
    val modality: CaSymbolModality?

    /** modality modifier 的显式性；没有 modality 的声明始终返回 `UNKNOWN`。 */
    val modalityExplicitness: CaModifierExplicitness
        get() = when {
            modality == null -> CaModifierExplicitness.UNKNOWN
            isModalityExplicit -> CaModifierExplicitness.EXPLICIT
            else -> CaModifierExplicitness.UNKNOWN
        }

    /**
     * 模态是否由源码显式写出。
     */
    @Deprecated("Use modalityExplicitness to distinguish implicit from unknown modality.")
    val isModalityExplicit: Boolean
}
