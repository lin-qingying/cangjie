package org.cangnova.cangjie.analysis.api.diagnostics

/**
 * 诊断等级。
 *
 * 与 Kotlin Analysis API 的 `KaSeverity` 对齐,
 * 表示诊断对编译/解析结果的影响强度。
 */
enum class CaSeverity {
    /** 错误:阻断编译或导致语义不正确,IDE 应高亮为红色。 */
    ERROR,

    /** 警告:语法/语义可疑但不阻断编译,通常以黄色提示。 */
    WARNING,

    /** 提示:风格、性能或最佳实践建议,严重程度最低。 */
    INFO,
}
