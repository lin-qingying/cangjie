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
     *
     * 该属性是「必须能确定种类」的入口。无法判定时（底层符号不可恢复，或出现了
     * 尚未被分类的 class-like 形态）实现会失败——调用方若只需展示/排序，应改用
     * [classKindOrNull]，不要用 `try/catch` 探测可用性。
     */
    val classKind: CaClassKind

    /**
     * 当前类型声明的种类；无法判定时返回 `null`。
     *
     * 默认实现等价于 [classKind]，适用于「种类总能确定」的实现；CFIR 后端覆盖它，
     * 使库符号不可恢复、或出现尚未分类的 class-like 形态时返回 `null` 而不是抛异常。
     *
     * 用它取代 `runCatching { classKind }.getOrNull()`——异常不是可用性查询的正确通道，
     * 而且会把真正的缺陷一起吞掉。
     */
    val classKindOrNull: CaClassKind?
        get() = classKind

    /**
     * 显式书写的直接父类型列表。
     *
     * 仅包含源码中真正出现的超类型，不展开传递闭包，也不包含隐式 `Any` 等默认父类型。
     */
    val superTypes: List<CaType>
}
