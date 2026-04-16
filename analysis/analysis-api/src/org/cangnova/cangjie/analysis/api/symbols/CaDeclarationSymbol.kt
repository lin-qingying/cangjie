package org.cangnova.cangjie.analysis.api.symbols

/**
 * 可由源码中单个声明节点稳定表示的公开符号。
 *
 * package/file 这类聚合语义不属于声明符号；class/function/property/extend/script 等属于声明符号。
 */
interface CaDeclarationSymbol : CaSymbol ,CaAnnotatedSymbol{
    /**
     * 语义上直接包含当前声明的容器符号。
     */
    val containingDeclaration: CaSymbol?

    /**
     * 声明自身的可见性。
     */
    val visibility: CaSymbolVisibility

    /**
     * 可见性是否由源码显式写出。
     */
    val isVisibilityExplicit: Boolean

    /**
     * 声明自身的模态。
     */
    val modality: CaSymbolModality?

    /**
     * 模态是否由源码显式写出。
     */
    val isModalityExplicit: Boolean
}
