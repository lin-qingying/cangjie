package org.cangnova.cangjie.analysis.api.components

/**
 * [CaDiagnosticCheckerFilter] 用于控制诊断收集结果中包含哪些类型的诊断。
 */
enum class CaDiagnosticCheckerFilter {
    /**
     * 仅包含编译器通用检查器产生的诊断。
     */
    ONLY_COMMON_CHECKERS,

    /**
     * 包含扩展检查器产生的诊断（通常仅在 IDE 中运行）。
     */
    ONLY_EXTENDED_CHECKERS,

    /**
     * 包含实验性检查器产生的诊断。
     *
     * 其使用方式与 [ONLY_EXTENDED_CHECKERS] 类似，主要在 IDE 中运行，但有以下差异：
     * * 可能出现误报
     * * 可能较慢
     */
    ONLY_EXPERIMENTAL_CHECKERS,

    /**
     * 同时包含通用检查器与扩展检查器产生的诊断。
     */
    EXTENDED_AND_COMMON_CHECKERS,
}
