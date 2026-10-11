package org.cangnova.cangjie.analysis.api.performance.test

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.io.path.readLines

/**
 * 性能运行历史：每次运行把一条 [CaPerformanceRunRecord] 以 JSONL 追加落盘。
 *
 * ## 为什么要落盘
 *
 * 基线对比的最小输入是"上一次是什么"。只在内存里留着，进程一退就没了；
 * 而单次测量本身噪声很大，只有把历次结果累积下来，才谈得上"这次到底是变慢了还是抖了一下"。
 *
 * ## 路径
 *
 * - 报告（人看）：`<模块>/build/reports/analysis-performance/`
 * - 历史（机读）：`<模块>/.gradle/performance-history/runs.jsonl`
 *
 * 历史刻意**不放在 `build/` 下**：`clean` 会清掉 `build/`，而基线恰恰需要跨 `clean` 存活，
 * 否则每次清理后第一次跑都只能得到"无基线"。
 *
 * ## 失败策略
 *
 * 落盘失败一律不抛出：性能记录是辅助产物，写不出去不该让测试变红。
 */
object CaPerformanceHistoryStore {
    /**
     * 模块目录的系统属性名，由构建脚本转发。
     */
    const val MODULE_DIR_PROPERTY: String = "cangjie.performance.moduleDir"

    /**
     * 历史文件保留的最大行数；超出时从最旧的一行开始截断。
     */
    const val MAX_HISTORY_RECORDS: Int = 50

    /**
     * 模块目录；取不到时回退到当前工作目录。
     */
    fun moduleDirectory(): Path = Paths.get(
        System.getProperty(MODULE_DIR_PROPERTY) ?: System.getProperty("user.dir")
    ).toAbsolutePath().normalize()

    /**
     * 报告输出目录。
     */
    fun reportDirectory(): Path = moduleDirectory().resolve("build").resolve("reports").resolve("analysis-performance")

    /**
     * 历史文件路径。
     */
    fun historyFile(): Path = moduleDirectory().resolve(".gradle").resolve("performance-history").resolve("runs.jsonl")

    /**
     * 追加一次运行记录；失败只返回 false，不抛出。
     */
    fun append(record: CaPerformanceRunRecord): Boolean = runCatching {
        val file = historyFile()
        Files.createDirectories(file.parent)
        Files.writeString(
            file,
            record.toJsonLine() + "\n",
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
        )
        true
    }.getOrElse { false }

    /**
     * 读取全部历史，按时间升序返回。
     *
     * 坏行直接跳过：历史文件可能来自别的分支或更旧的格式，一条坏行不该让整次运行失败。
     */
    fun readAll(): List<CaPerformanceRunRecord> {
        val file = historyFile()
        if (!file.exists()) return emptyList()
        return runCatching {
            file.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { CaPerformanceRunRecord.parse(it) }
                .sortedBy { it.timestampMillis }
        }.getOrElse { emptyList() }
    }

    /**
     * 截断历史，只保留最近 [MAX_HISTORY_RECORDS] 次运行。
     */
    fun trim(): Boolean = runCatching {
        val records = readAll()
        if (records.size <= MAX_HISTORY_RECORDS) return@runCatching true
        val file = historyFile()
        Files.createDirectories(file.parent)
        Files.writeString(
            file,
            records.takeLast(MAX_HISTORY_RECORDS).joinToString("\n", postfix = "\n"),
        )
        true
    }.getOrElse { false }
}