package org.cangnova.cangjie.chir.core.testkit

enum class ChirDiffStatus {
    PASS,
    FAIL,
    ACCEPTED_DIFF,
}

data class ChirDiffEntry(
    val caseId: String,
    val status: ChirDiffStatus,
    val summary: String,
)

object ChirDiffReportFormatter {
    fun render(entries: List<ChirDiffEntry>): String {
        val sorted = entries.sortedBy { it.caseId }
        val passCount = sorted.count { it.status == ChirDiffStatus.PASS }
        val failCount = sorted.count { it.status == ChirDiffStatus.FAIL }
        val acceptedCount = sorted.count { it.status == ChirDiffStatus.ACCEPTED_DIFF }

        return buildString {
            appendLine("CHIR-DIFF-REPORT")
            appendLine("total=${sorted.size} pass=$passCount fail=$failCount accepted=$acceptedCount")
            sorted.forEach { entry ->
                appendLine("${entry.status.name}|${entry.caseId}|${sanitize(entry.summary)}")
            }
        }.trimEnd()
    }

    private fun sanitize(text: String): String =
        text.replace("\n", "\\n")
}
