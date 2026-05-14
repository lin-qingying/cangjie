package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol

/**
 * 变量族公共符号根接口。
 *
 * `isLet` 只表达 let/var 这一层声明侧可变性，
 * 不承载初始化器源码文本，也不混入 `mut` 语义。
 */
sealed class CaVariableSymbol : CaCallableSymbol(), CaNamedSymbol {
    /**
     * 是否以 `let` 声明（即声明侧不可变绑定）。
     *
     * 仅表达 let/var 这一层声明侧可变性，不混入 `mut` 修饰符（后者属于函数族的 receiver 可变语义）。
     */
    abstract val isLet: Boolean
}
