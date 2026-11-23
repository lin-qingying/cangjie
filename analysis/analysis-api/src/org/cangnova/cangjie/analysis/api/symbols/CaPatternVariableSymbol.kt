package org.cangnova.cangjie.analysis.api.symbols

/**
 * 完整模式声明的容器符号。容器没有变量名，具名叶子各自拥有独立的变量身份。
 */
abstract class CaPatternVariableSymbol : CaDeclarationSymbol {
    /** 当前模式树中所有真实绑定；wildcard 和 enum constructor 不产生条目。 */
    abstract val entries: List<CaPatternBindingSymbol>

    /** 模式声明的关键字事实；不赋予容器名称或 callable 身份。 */
    abstract val isVar: Boolean
    abstract val isConst: Boolean

    abstract override fun createPointer(): org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer<CaPatternVariableSymbol>
}
