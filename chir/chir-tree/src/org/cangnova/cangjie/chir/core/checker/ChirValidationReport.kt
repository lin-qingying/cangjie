package org.cangnova.cangjie.chir.core.checker

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

enum class ChirValidationSeverity {
    ERROR,
    WARNING,
}

data class ChirValidationIssue(
    val code: String,
    val severity: ChirValidationSeverity,
    val message: String,
    val nodeId: ChirSemanticId? = null,
    val context: Map<String, String> = emptyMap(),
)

data class ChirValidationReport(
    val issues: List<ChirValidationIssue>,
) {
    val hasErrors: Boolean
        get() = issues.any { it.severity == ChirValidationSeverity.ERROR }

    companion object {
        val EMPTY = ChirValidationReport(emptyList())
    }
}

object ChirValidationReportFormatter {
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
