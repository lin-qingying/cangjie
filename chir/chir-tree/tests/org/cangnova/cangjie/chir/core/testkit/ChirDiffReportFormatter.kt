package org.cangnova.cangjie.chir.core.testkit

/**
 * 描述 CHIR 差异用例的比较状态。
 *
 * 状态用于区分完全通过、未接受失败以及已经登记接受的差异，供报告汇总计数使用。
 */
enum class ChirDiffStatus {
    /** 比较结果完全匹配基线。 */
    PASS,

    /** 比较结果存在未接受差异。 */
    FAIL,

    /** 比较结果存在差异，但该差异已经被显式接受。 */
    ACCEPTED_DIFF,
}

/**
 * 表示单个 CHIR 差异用例的报告条目。
 *
 * 每个条目携带用例标识、比较状态和人类可读摘要，最终由报告格式化器统一排序输出。
 */
data class ChirDiffEntry(
    /** 差异用例的稳定标识。 */
    val caseId: String,

    /** 该用例的比较状态。 */
    val status: ChirDiffStatus,

    /** 该用例的差异摘要或通过说明。 */
    val summary: String,
)

/**
 * 将 CHIR 差异条目渲染为稳定的文本报告。
 *
 * 报告按用例标识排序，并汇总通过、失败和已接受差异数量，便于 CI 或人工审查读取。
 */
object ChirDiffReportFormatter {
    /**
     * 渲染完整的 CHIR 差异报告。
     *
     * 该方法对条目排序、统计状态数量，并对摘要换行进行转义以保持一行一个用例的格式。
     */
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

    /**
     * 转义报告摘要中的换行符。
     *
     * 该方法保证单个差异条目不会破坏报告的一行一记录结构。
     */
    private fun sanitize(text: String): String =
        text.replace("\n", "\\n")
}
