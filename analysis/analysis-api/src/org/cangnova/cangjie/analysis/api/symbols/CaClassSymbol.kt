package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * class / interface / struct / enum 这类真实类型声明的公开语义视图。
 *
 * - 提供 [classKind] 区分四类形态；
 * - 通过 [superTypes] 暴露直接父类型列表，供继承层级、覆盖关系等上层语义使用；
 * - 与 [CaTypeAliasSymbol] 区分：后者只是别名，并不引入新的类型实体。
 *
 * 对齐 Kotlin Analysis API 的 `KaClassSymbol`。
 */
interface CaClassSymbol : CaClassLikeSymbol, CaDeclarationContainerSymbol {
    /**
     * 当前类型声明的种类。
     */
    val classKind: CaClassKind

    /**
     * 显式书写的直接父类型列表。
     *
     * 仅包含源码中真正出现的超类型，不展开传递闭包，也不包含隐式 `Any` 等默认父类型。
     */
    val superTypes: List<CaType>
}
