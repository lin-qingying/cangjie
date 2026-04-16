package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.CallableId

/**
 * callable 声明的公开语义视图。
 */
public sealed class CaCallableSymbol : CaDeclarationSymbol {
    /**
     * 稳定 callable 身份。
     *
     * 对匿名或局部 callable 允许为 `null`。
     */
    abstract val callableId: CallableId?
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
