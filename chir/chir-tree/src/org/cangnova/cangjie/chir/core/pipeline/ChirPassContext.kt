package org.cangnova.cangjie.chir.core.pipeline

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import java.time.Duration
import java.time.Instant

data class ChirPassExecutionRecord(
    val passName: String,
    val status: ChirPassStatus,
    val startedAt: Instant,
    val finishedAt: Instant,
    val touchedNodes: Set<ChirSemanticId> = emptySet(),
    val summary: String? = null,
    val errorMessage: String? = null,
)

enum class ChirPassStatus {
    SUCCESS,
    FAILED,
}

class ChirPassContext {
    private val mutableRecords = mutableListOf<ChirPassExecutionRecord>()

    val records: List<ChirPassExecutionRecord>
        get() = mutableRecords

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

    fun totalDuration(): Duration {
        val first = mutableRecords.minByOrNull { it.startedAt } ?: return Duration.ZERO
        val last = mutableRecords.maxByOrNull { it.finishedAt } ?: return Duration.ZERO
        return Duration.between(first.startedAt, last.finishedAt)
    }

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
