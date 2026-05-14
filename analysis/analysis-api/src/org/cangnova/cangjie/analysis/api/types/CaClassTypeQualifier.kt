package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.name.Name

/**
 * class-like 类型应用中的一段限定。
 *
 * 当一个类型由多段组成(例如 `Foo.Bar<T>`),其每一段都被建模为一个限定 qualifier:
 * - [name] 为该段的简单名;
 * - [typeArguments] 为该段上书写的类型实参投影列表。
 *
 * 该接口为 sealed:
 * - 解析成功的段使用 [CaResolvedClassTypeQualifier],可以拿到符号;
 * - 解析失败的段使用 [CaUnresolvedClassTypeQualifier],仅保留语法层信息,以避免把未解析状态伪装成已解析类型。
 *
 * 对齐 Kotlin Analysis API 的 `KaClassTypeQualifier`。
 */
sealed interface CaClassTypeQualifier : CaLifetimeOwner {
    /**
     * 该段的简单名,例如 `Foo.Bar` 中的 `Foo` 或 `Bar`。
     */
    val name: Name

    /**
     * 写在该段名字后面的类型实参投影,例如 `Foo<Int>.Bar<String>` 中 `Bar` 段的 `<String>`。
     */
    val typeArguments: List<CaTypeProjection>
}
