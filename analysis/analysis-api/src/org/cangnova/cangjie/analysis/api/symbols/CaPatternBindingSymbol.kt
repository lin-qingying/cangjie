package org.cangnova.cangjie.analysis.api.symbols

/**
 * 模式绑定符号。
 *
 * 表示模式匹配场景下整体绑定整个匹配子表达式的局部变量绑定，
 * 例如 `let p = ...` 中作为绑定根的 `p`。
 *
 * 与 [CaPatternVariableSymbol] 的区别：
 * - binding 指代"模式根的绑定"；
 * - pattern variable 指代"出现在模式内部、按结构解构得到的子绑定"。
 */
abstract class CaPatternBindingSymbol : CaLocalVariableSymbol()
