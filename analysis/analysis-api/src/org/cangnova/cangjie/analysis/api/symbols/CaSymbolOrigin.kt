package org.cangnova.cangjie.analysis.api.symbols

/**
 * 符号的来源枚举。
 *
 * 这里对齐 Kotlin Analysis API 的“来源”职责边界，但保留仓颉自己的扩展语义。
 */
enum class CaSymbolOrigin {
    UNKNOWN,
    SOURCE,
    LIBRARY,
    SYNTHETIC,
    IMPLICIT_DEFAULT,
    GENERIC_INSTANTIATION,
    EXTENSION,
    SAM_CONSTRUCTOR,
    SUBSTITUTION_OVERRIDE_DECLARATION_SITE,
    SUBSTITUTION_OVERRIDE_CALL_SITE,
}
