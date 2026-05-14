package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.name.ClassId

/**
 * 构造器符号。
 *
 * 同时覆盖主构造器与次构造器；模态恒为 [CaSymbolModality.FINAL]
 * （构造器无法被继承/重写），由该约束在公共层直接固化。
 *
 * 对齐 Kotlin Analysis API 的 `KaConstructorSymbol`。
 */
abstract class  CaConstructorSymbol :   CaFunctionSymbol(), CaTypeParameterOwnerSymbol {
    /**
     * 是否为主构造器。
     *
     * 仓颉中只有声明体头部书写的构造器是主构造器，其余为次构造器。
     */
    abstract val isPrimary: Boolean

    /**
     * 构造器永远是 FINAL。
     *
     * 该值通过 [withValidityAssertion] 保护，确保只能在有效生命周期内读取。
     */
    final  override val modality: CaSymbolModality get() = withValidityAssertion { CaSymbolModality.FINAL }

    /**
     * 创建当前构造器符号的指针，返回值收窄到 [CaConstructorSymbol]。
     */
    abstract override fun createPointer(): CaSymbolPointer<CaConstructorSymbol>

    /**
     * 所属类型的稳定身份。
     *
     * 对匿名/局部 class-like 声明的构造器可能为 `null`。
     */
    abstract val containingClassId: ClassId?
}
