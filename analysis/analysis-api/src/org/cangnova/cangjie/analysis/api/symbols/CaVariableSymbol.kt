package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol

/**
 * 变量族公共符号根接口。
 *
 * `isLet` 只表达 let/var 这一层声明侧可变性，
 * 不承载初始化器源码文本，也不混入 `mut` 语义。
 */
sealed class CaVariableSymbol : CaCallableSymbol(), CaNamedSymbol {
    abstract val isLet: Boolean
}
