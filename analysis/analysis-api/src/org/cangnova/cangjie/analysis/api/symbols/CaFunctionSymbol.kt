package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer

/**
 * 函数族公开符号根接口。
 *
 * 用 `sealed` 收敛函数家族的所有公开叶子，便于上层基于穷尽匹配处理：
 * - [CaNamedFunctionSymbol] / [CaMainFunctionSymbol] / [CaMacroSymbol]：具名函数；
 * - [CaAnonymousFunctionSymbol]：函数字面量；
 * - [CaConstructorSymbol] / [CaFinalizerSymbol]：构造器与析构器；
 * - [CaPropertyAccessorSymbol]：属性访问器。
 *
 * 该接口集中暴露仓颉函数级别的修饰符语义（`static`、`const`、`mut`、`override`、`operator`、`unsafe`、`foreign`）。
 */
sealed class CaFunctionSymbol : CaCallableSymbol(), CaTypeParameterOwnerSymbol, CaValueParameterOwnerSymbol {
    /**
     * 是否为静态成员函数。
     */
    abstract val isStatic: Boolean

    /**
     * 是否为编译期可求值的 const 函数。
     */
    abstract val isConst: Boolean

    /**
     * 创建当前函数符号的指针，返回值收窄到 [CaFunctionSymbol]。
     */
    abstract override fun createPointer(): CaSymbolPointer<CaFunctionSymbol>

    /**
     * `mut` 修饰符语义。
     *
     * 它和 let/var 可变性是两套完全不同的语义，不能混用。
     * `mut` 表达"是否允许修改 receiver 自身"，而不是变量绑定可变性。
     */
    abstract  val isMutating: Boolean

    /**
     * 是否为 `override` 声明（重写父类型成员）。
     */
    abstract val isOverride: Boolean

    /**
     * 是否为 operator 函数（如算术运算符等可被符号化调用的函数）。
     */
    abstract  val isOperator: Boolean

    /**
     * 是否为 `unsafe` 函数（绕过安全约束，必须在 unsafe 上下文使用）。
     */
    abstract  val isUnsafe: Boolean

    /**
     * 是否为 `foreign` 函数（声明指向外部实现，无函数体）。
     */
    abstract   val isForeign: Boolean
}
