package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

/**
 * 仓颉公开类型的根接口。
 *
 * - 表示一个具体的仓颉类型（如 `Int64`、`String`、用户类 `Foo`、泛型 `Array<T>` 等);
 * - 由 Analysis API 在 [org.cangnova.cangjie.analysis.api.CaSession] 内构造,生命周期受
 *   [CaLifetimeOwner] 约束,不能跨 session 直接持有;
 * - 跨 session 持久化引用应通过 [createPointer] 拿到 [CaTypePointer],再由目标 session restore;
 * - 与 PSI/CFIR 内部的 `ConeCangjieType` 一一对应,但仅暴露公开稳定面,不泄露后端实现细节。
 *
 * 表示的类型既可能是合法类型,也可能是 [CaErrorType]——后者会额外提供错误信息,
 * 更具体的 [CaClassErrorType] 还会提供候选符号。
 *
 * 该接口对齐 Kotlin Analysis API 的 `KaType`。
 */
interface CaType : CaLifetimeOwner, CaAnnotated {
    /**
     * 类型的可展示文本（presentation),用于诊断、tooltip、调试输出等场景。
     *
     * 该字符串只是公开层的稳定文本视图,不保证可以反解析回原类型,具体格式由实现决定。
     */
    val presentation: String

    /**
     * 当前类型由别名（type alias)展开而来时的别名形式,若不存在别名上下文则为 `null`。
     *
     * - 携带具体应用上下文,例如 `typealias MyList<A> = Array<A>` 在 `MyList<String>` 处的
     *   缩写就是 `MyList<String>` 而非简单的 `MyList`;
     * - 类型参数本身不会再被自动转成缩写形式;
     * - 链式别名只保留最初的别名应用,中间层的别名会被丢弃;
     * - 当源模块不可达时也可能为 `null`,本字段仅在能稳定解析时返回。
     *
     * 对齐 Kotlin `KaType.abbreviation`。
     */
    val abbreviation: CaUsualClassType?

    /**
     * 创建当前类型的稳定跨 session 指针。
     *
     * 返回的 [CaTypePointer] 可以安全地穿越 analyze 块,
     * 通过 [org.cangnova.cangjie.analysis.api.CaSession] 的 restore 能力重新获取等价类型实例。
     */
    fun createPointer(): CaTypePointer<CaType>
}
