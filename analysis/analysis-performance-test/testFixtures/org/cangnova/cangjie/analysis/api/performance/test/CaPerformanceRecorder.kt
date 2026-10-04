package org.cangnova.cangjie.analysis.api.performance.test

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 性能运行记录的采集与落盘入口，由性能测试基类在每个用例的前后调用。
 *
 * ## 为什么要挂在基类而不是每个用例自己写
 *
 * 指标是 JVM 累积的，只有在**用例边界**上取前后快照才能得到干净增量。
 * 让每个用例自己写 before/after，既容易漏，也会让"到底记录了什么"没有单一答案。
 *
 * ## 失败策略
 *
 * 记录与落盘全程不让异常外泄：性能记录是辅助产物，采集失败不该把用例判成失败。
 */
object CaPerformanceRecorder {
    /**
     * 一次运行内的采集状态。
     */
    private class RunState(
        val timestampMillis: Long,
        val records: MutableList<CaPerformanceCaseRecord> = mutableListOf(),
    ) {
        /** 当前用例开始时的指标快照。 */
        var snapshot: CaPerformanceSnapshot? = null

        /** 当前用例开始时的单调时钟读数。 */
        var startedNanos: Long = 0L
    }

    private val lock = Any()

    private var runState: RunState? = null

    private var flushHookInstalled = false

    /**
     * 用例开始前调用：采集指标快照并记录起始时间。
     */
    fun beforeCase() {
        runCatching {
            synchronized(lock) {
                val state = runState ?: RunState(System.currentTimeMillis()).also {
                    runState = it
                    installFlushHook()
                }
                state.snapshot = CaPerformanceSnapshot.capture()
                state.startedNanos = System.nanoTime()
            }
        }
    }

    /**
     * 用例结束后调用：按增量与墙钟记录一条用例结果。
     */
    fun afterCase(testClass: String, testMethod: String) {
        runCatching {
            synchronized(lock) {
                val state = runState ?: return@runCatching
                val before = state.snapshot ?: return@runCatching
                val wallNanos = (System.nanoTime() - state.startedNanos).coerceAtLeast(0L)
                val delta = CaPerformanceSnapshot.capture().minus(before)
                state.records += CaPerformanceCaseRecord(
                    testClass = testClass,
                    testMethod = testMethod,
                    wallNanos = wallNanos,
                    delta = delta,
                )
                state.snapshot = null
            }
        }
    }

    /**
     * 完成本次运行：与历史对比、写出报告、追加历史。
     *
     * 幂等：重复调用只生效一次，避免关停钩子与测试生命周期两头都触发时写两遍。
     *
     * @return 控制台摘要；本次运行没有记录任何用例时返回 null
     */
    fun flush(): String? {
        val state = synchronized(lock) {
            val current = runState ?: return null
            runState = null
            if (current.records.isEmpty()) return null
            current
        }

        val run = CaPerformanceRunRecord(
            runId = runIdFor(state.timestampMillis),
            timestampMillis = state.timestampMillis,
            gitCommit = resolveGitCommit(),
            cases = state.records.toList(),
        )

        val history = CaPerformanceHistoryStore.readAll()
        val report = CaPerformanceBaselineComparator.compare(
            current = run,
            history = history,
            changeFloor = CaPerformanceBaselineComparator.effectiveChangeFloor(),
        )

        CaPerformanceReportRenderer.writeReports(report, run, CaPerformanceHistoryStore.reportDirectory())
        CaPerformanceHistoryStore.append(run)
        CaPerformanceHistoryStore.trim()

        return CaPerformanceReportRenderer.renderConsole(report, run)
    }

    /**
     * 注册 JVM 关停钩子：测试任务是最后一个用例结束后进程立刻退出，
     * 没有关停钩子就来不及在退出前沿历史读基线。
     */
    private fun installFlushHook() {
        if (flushHookInstalled) return
        flushHookInstalled = true
        Runtime.getRuntime().addShutdownHook(
            Thread({ flush()?.let(::print) }, "ca-performance-report-flush"),
        )
    }

    /**
     * 由时间戳派生运行标识，同一模块内唯一。
     */
    private fun runIdFor(timestampMillis: Long): String =
        RUN_ID_FORMAT.format(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestampMillis), java.time.ZoneId.systemDefault()))

    /**
     * 尽力读取当前 commit；读不到返回 null，不影响其他行为。
     *
     * 兼容工作树：工作树里 `.git` 是指向真实 gitdir 的文件而不是目录。
     */
    internal fun resolveGitCommit(): String? = runCatching {
        val gitPath = findGitEntry(CaPerformanceHistoryStore.moduleDirectory()) ?: return null
        val gitDir = if (Files.isDirectory(gitPath)) {
            gitPath
        } else {
            val pointer = Files.readString(gitPath).trim()
            if (!pointer.startsWith("gitdir:")) return null
            val target = Paths.get(pointer.removePrefix("gitdir:").trim())
            if (target.isAbsolute) target else gitPath.parent.resolve(target)
        }

        val head = Files.readString(gitDir.resolve("HEAD")).trim()
        if (!head.startsWith("ref:")) return head.takeIf { it.isNotBlank() }
        val ref = head.removePrefix("ref:").trim()
        val refFile = gitDir.resolve(ref)
        if (Files.exists(refFile)) return Files.readString(refFile).trim().takeIf { it.isNotBlank() }
        // 引用可能已打包到 packed-refs。
        val packed = gitDir.resolve("packed-refs")
        if (!Files.exists(packed)) return null
        Files.readAllLines(packed)
            .firstOrNull { line -> !line.startsWith("#") && line.endsWith(" $ref") }
            ?.substringBeforeLast(" $ref")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

    /**
     * 从 [start] 向上寻找 `.git`。
     */
    private fun findGitEntry(start: Path): Path? {
        var current: Path? = start
        while (current != null) {
            val candidate = current.resolve(".git")
            if (Files.exists(candidate)) return candidate
            current = current.parent
        }
        return null
    }

    private val RUN_ID_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
}