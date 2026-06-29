package org.cangnova.cangjie.chir.core.checker

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

/**
 * CHIR 校验诊断严重级别。
 */
enum class ChirValidationSeverity {
    ERROR,
    WARNING,
}

/**
 * 单条 CHIR 校验问题。
 */
data class ChirValidationIssue(
    /**
     * 稳定诊断代码。
     */
    val code: String,

    /**
     * 诊断严重级别。
     */
    val severity: ChirValidationSeverity,

    /**
     * 面向用户或开发者的诊断消息。
     */
    val message: String,

    /**
     * 关联节点语义标识。
     */
    val nodeId: ChirSemanticId? = null,

    /**
     * 额外诊断上下文。
     */
    val context: Map<String, String> = emptyMap(),
)

/**
 * CHIR 校验报告。
 */
data class ChirValidationReport(
    /**
     * 校验问题列表。
     */
    val issues: List<ChirValidationIssue>,
) {
    /**
     * 报告是否包含错误级别问题。
     */
    val hasErrors: Boolean
        get() = issues.any { it.severity == ChirValidationSeverity.ERROR }

    /**
     * 校验报告静态工厂与常量。
     */
    companion object {
        /**
         * 无问题的空校验报告。
         */
        val EMPTY = ChirValidationReport(emptyList())
    }
}

/**
 * CHIR 校验报告文本格式化器。
 */
object ChirValidationReportFormatter {
    /**
     * 渲染校验报告为稳定文本。
     */
    fun render(report: ChirValidationReport): String {
        if (report.issues.isEmpty()) return "CHIR validation passed"

        return buildString {
            report.issues.forEachIndexed { index, issue ->
                append("[")
                append(index + 1)
                append("] ")
                append(issue.severity.name)
                append(' ')
                append(issue.code)
                append(": ")
                append(issue.message)
                if (issue.nodeId != null) {
                    append(" (node=")
                    append(issue.nodeId)
                    append(')')
                }
                if (issue.context.isNotEmpty()) {
                    append(" context=")
                    append(issue.context.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}=${it.value}" })
                }
                appendLine()
            }
        }.trimEnd()
    }
}
