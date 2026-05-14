package org.cangnova.cangjie.analysis.api.symbols

/**
 * 模式变量符号。
 *
 * 表示出现在模式内部、按结构解构得到的局部变量绑定，
 * 例如 `match` 模式中嵌套捕获出的子变量。
 *
 * 与 [CaPatternBindingSymbol] 配合使用，分别表达"整体绑定"与"结构内部绑定"两种角色。
 */
abstract class CaPatternVariableSymbol : CaLocalVariableSymbol()
