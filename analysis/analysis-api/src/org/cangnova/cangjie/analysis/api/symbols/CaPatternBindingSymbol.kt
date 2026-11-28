package org.cangnova.cangjie.analysis.api.symbols

/**
 * 模式绑定符号。
 *
 * 表示 Binding、Type 或语义确认后的 VarOrEnum 所引入的具名叶子。
 * 每个叶子拥有独立名称、类型与作用域；声明容器由 [CaPatternVariableSymbol] 表示。
 */
abstract class CaPatternBindingSymbol : CaVariableSymbol() {
    /** 绑定是否来自 const 声明，与 let/var 的可变性分别保存。 */
    abstract val isConst: Boolean
}
