package org.cangnova.cangjie.chir.core.pipeline

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import java.time.Duration
import java.time.Instant

/**
 * 单次 CHIR pass 执行记录。
 */
data class ChirPassExecutionRecord(
    /**
     * pass 名称。
     */
    val passName: String,

    /**
     * pass 执行状态。
     */
    val status: ChirPassStatus,

    /**
     * pass 开始时间。
     */
    val startedAt: Instant,

    /**
     * pass 结束时间。
     */
    val finishedAt: Instant,

    /**
     * pass 触达的节点集合。
     */
    val touchedNodes: Set<ChirSemanticId> = emptySet(),

    /**
     * pass 执行摘要。
     */
    val summary: String? = null,

    /**
     * pass 失败时的错误消息。
     */
    val errorMessage: String? = null,
)

/**
 * CHIR pass 执行状态。
 */
enum class ChirPassStatus {
    SUCCESS,
    FAILED,
}

/**
 * CHIR pipeline 执行上下文。
 */
class ChirPassContext {
    /**
     * 可变执行记录列表。
     */
    private val mutableRecords = mutableListOf<ChirPassExecutionRecord>()

    /**
     * 已完成 pass 的执行记录只读视图。
     */
    val records: List<ChirPassExecutionRecord>
        get() = mutableRecords

    /**
     * 执行一个 pass 并记录成功或失败状态。
     */
    fun <T> runPass(
        passName: String,
        action: () -> T,
        touchedNodesProvider: (T) -> Set<ChirSemanticId> = { emptySet() },
        summaryProvider: (T) -> String? = { null },
    ): T {
        val startedAt = Instant.now()
        return runCatching(action)
            .onSuccess {
                val touchedNodes = touchedNodesProvider(it)
                val summary = summaryProvider(it)
                mutableRecords += ChirPassExecutionRecord(
                    passName = passName,
                    status = ChirPassStatus.SUCCESS,
                    startedAt = startedAt,
                    finishedAt = Instant.now(),
                    touchedNodes = touchedNodes,
                    summary = summary,
                )
            }
            .onFailure {
                mutableRecords += ChirPassExecutionRecord(
                    passName = passName,
                    status = ChirPassStatus.FAILED,
                    startedAt = startedAt,
                    finishedAt = Instant.now(),
                    touchedNodes = emptySet(),
                    summary = null,
                    errorMessage = it.message,
                )
                throw it
            }
            .getOrThrow()
    }

    /**
     * 计算所有已记录 pass 的总耗时跨度。
     */
    fun totalDuration(): Duration {
        val first = mutableRecords.minByOrNull { it.startedAt } ?: return Duration.ZERO
        val last = mutableRecords.maxByOrNull { it.finishedAt } ?: return Duration.ZERO
        return Duration.between(first.startedAt, last.finishedAt)
    }

    /**
     * 渲染 pass 执行摘要文本。
     */
    fun renderSummary(): String {
        if (mutableRecords.isEmpty()) return "No pass executed"
        return buildString {
            appendLine("CHIR Pass Execution Summary")
            mutableRecords.forEach { record ->
                append("- ")
                append(record.passName)
                append(": ")
                append(record.status)
                record.summary?.let {
                    append(" | ")
                    append(it)
                }
                record.errorMessage?.let {
                    append(" | error=")
                    append(it)
                }
                if (record.touchedNodes.isNotEmpty()) {
                    append(" | touched=")
                    append(record.touchedNodes.joinToString(prefix = "[", postfix = "]"))
                }
                appendLine()
            }
            append("Total duration: ")
            append(totalDuration())
        }.trimEnd()
    }
}
