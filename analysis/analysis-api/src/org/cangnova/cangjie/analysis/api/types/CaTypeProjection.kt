package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken

/**
 * class-like 类型应用中某一段的单个类型实参投影。
 *
 * - 一般情况下 [type] 持有具体类型,表示一个普通的类型实参;
 * - 若仓颉/Analysis API 在未来支持类似 “通配” 语义时,[type] 也可能为 `null` 表示无确定类型。
 *
 * 该类型受 [CaLifetimeOwner] 生命周期约束,不能跨 session 持有。
 *
 * 对齐 Kotlin Analysis API 的 `KaTypeProjection`(Cangjie 当前不区分 in/out variance 与 star projection,
 * 因此公开层使用单一类直接承载 [type] 字段)。
 */
class CaTypeProjection(
    /**
     * 投影对应的具体类型,缺省/通配语义下可能为 `null`。
     */
    val type: CaType?,

    /**
     * 类型投影所属 session 的生命周期 token。
     */
    override val token: CaLifetimeToken,
) : CaLifetimeOwner
