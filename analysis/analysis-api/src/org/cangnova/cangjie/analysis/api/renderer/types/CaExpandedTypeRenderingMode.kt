package org.cangnova.cangjie.analysis.api.renderer.types

/**
 * 类型别名(typealias)展开渲染模式。
 *
 * 控制 [CaTypeRenderer] 遇到类型别名时, 是按"原名"还是"展开后类型"输出, 以及是否附带注释。
 *
 * 对齐 Kotlin Analysis API 的 `KaExpandedTypeRenderingMode`。
 */
enum class CaExpandedTypeRenderingMode {
    /**
     * 仅按原名渲染缩写类型, 例如 `foo.bar.StringAlias`。
     */
    RENDER_ABBREVIATED_TYPE,

    /**
     * 按原名渲染缩写类型, 并把展开类型放在注释里;
     * 例如 `foo.bar.StringAlias /* = kotlin.String */`。
     */
    RENDER_ABBREVIATED_TYPE_WITH_EXPANDED_TYPE_COMMENT,

    /**
     * 直接渲染展开后的类型, 例如 `kotlin.String`。
     */
    RENDER_EXPANDED_TYPE,

    /**
     * 渲染展开类型, 并把原始缩写名放在注释里;
     * 例如 `kotlin.String /* from: foo.bar.StringAlias */`。
     */
    RENDER_EXPANDED_TYPE_WITH_ABBREVIATED_TYPE_COMMENT,
}
