package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.CallableId

/**
 * callable 声明的公开语义视图。
 *
 * 覆盖所有"可被调用"的实体：[CaFunctionSymbol] 函数族（含构造器、访问器、宏、main、finalizer、匿名函数）、
 * [CaVariableSymbol] 变量族（含属性、字段、参数、局部变量、模式变量等）、
 * [CaEnumConstructorSymbol] 枚举构造器。
 *
 * 用 `sealed` 收敛叶子家族，便于上层基于穷尽分支处理。
 *
 * 对齐 Kotlin Analysis API 的 `KaCallableSymbol`。
 */
sealed class CaCallableSymbol : CaDeclarationSymbol {
    /**
     * 稳定 callable 身份。
     *
     * 对匿名或局部 callable 允许为 `null`。
     */
    abstract val callableId: CallableId?

    /**
     * 创建当前 callable 符号的指针，返回值收窄到 [CaCallableSymbol]。
     */
    abstract override fun createPointer(): CaSymbolPointer<CaCallableSymbol>

    /**
     * 显式 receiver 类型。
     *
     * 仅对源码中真正写出的 receiver 暴露，不把普通成员的隐式 dispatch receiver 误当成扩展 receiver。
     */
    abstract val receiverType: CaType?

    /**
     * callable 的语义返回类型。
     *
     * 对 variable 符号，这里就是变量类型。
     */
    abstract val returnType: CaType
}
