package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol

/**
 * 仓颉属性符号。
 *
 * Kotlin Analysis API 在属性族下还会继续拆出 `KaKotlinPropertySymbol`、
 * synthetic Java property 等多个公开叶子类型，因此其 `KaPropertySymbol` 仍保持 `sealed`。
 *
 * 仓颉当前公开语义只有一条属性族，不再额外发明“仅为后端继承服务”的桥接叶子。
 * 因此这里直接作为可继承的公共属性叶子，供后端实现模块落具体实现。
 */
abstract class CaPropertySymbol : CaVariableSymbol(), CaTypeParameterOwnerSymbol, CaDeclarationContainerSymbol {
    /**
     * 属性符号在公共层即收窄为 property pointer。
     *
     * 这与 Kotlin Analysis API 保持一致，便于 accessor 等上层语义直接依赖
     * `CaPropertySymbol` 恢复 owning property，而不是退回到更宽的 callable pointer。
     */
    abstract override fun createPointer(): CaSymbolPointer<CaPropertySymbol>

    /**
     * 是否为静态属性。
     */
    abstract  val isStatic: Boolean

    /**
     * 是否为编译期常量属性。
     */
    abstract val isConst: Boolean

    /**
     * `mut` 修饰符语义，参见 [CaFunctionSymbol.isMutating]。
     *
     * 用于区分"允许修改 receiver"与"绑定本身可变（var）"两种语义。
     */
    abstract val isMutating: Boolean

    /**
     * 是否为 `override` 声明，重写父类型的同名属性。
     */
    abstract   val isOverride: Boolean

    /**
     * 是否为 `unsafe` 属性。
     */
    abstract  val isUnsafe: Boolean

    /**
     * 是否为 `foreign` 属性。
     */
    abstract val isForeign: Boolean

    /**
     * getter 子符号；只读属性可能在某些场景下为 `null`，依实现而定。
     */
    abstract   val getter: CaPropertyGetterSymbol?

    /**
     * setter 子符号；只读属性为 `null`。
     */
    abstract   val setter: CaPropertySetterSymbol?
}
