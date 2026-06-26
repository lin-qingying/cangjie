package org.cangnova.cangjie.macro

/**
 * 宏诊断严重级别常量。
 *
 * 数值与宏服务端协议中的严重级别字段保持一致，供 [MacroDiagnosticInfo.severity] 使用。
 */
object MacroDiagnosticSeverity {
    /**
     * 信息级诊断。
     */
    const val INFO: Int = 0
    /**
     * 警告级诊断。
     */
    const val WARNING: Int = 1
    /**
     * 错误级诊断。
     */
    const val ERROR: Int = 2
}
