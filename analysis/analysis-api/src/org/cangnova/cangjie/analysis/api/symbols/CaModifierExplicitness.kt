package org.cangnova.cangjie.analysis.api.symbols

/**
 * 描述符号的 visibility 或 modality 是否由可用事实明确确定。
 *
 * `UNKNOWN` 表示来源没有保留显式性信息；它不能被当作 `IMPLICIT` 使用。
 */
enum class CaModifierExplicitness {
    /** 源码明确写出了对应 modifier。 */
    EXPLICIT,

    /** 源码存在且确认没有写出对应 modifier。 */
    IMPLICIT,

    /** 当前符号来源没有提供显式性信息。 */
    UNKNOWN,
}
