
package org.cangnova.cangjie.analysis.api.cfir.utils

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.platform.CaCachedService
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionInvalidationService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLAnalysisSessionStatistics
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.LLFlightRecorder
import org.cangnova.cangjie.utils.rethrowIntellijPlatformExceptionIfNeeded
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * 按请求清理全部 low-level 解析缓存的设施（对齐 Kotlin `KaFirCacheCleaner`）。
 *
 * 调用契约（调用方必须成对遵守）：
 * - 在取得 Analysis API session 之前调用 [enterAnalysis]；一旦调用，就必须在同一个线程上
 *   于分析块结束后调用 [exitAnalysis]，即使分析块从未开始执行（进入阶段抛错）也一样。
 * - [scheduleCleanup] 可以重复调用；如果已有清理被挂起，重复调用会被忽略。
 *
 * CFIR 侧由 `CaCfirSessionProvider` 满足该契约：`getAnalysisSession` 进入，
 * `afterLeavingAnalysis` 与进入阶段的失败路径退出。批量入口
 * （`analyzeElements` / `analyzeModules`）必须先按 use-site module 分组，
 * 使进入次数与离开次数相等。
 */
@CaImplementationDetail
interface CaCfirCacheCleaner {
    /**
     * 标记当前线程进入一次分析（在取得 session 之前调用）。
     *
     * 若调用了本方法，必须在同一个线程上调用 [exitAnalysis] 来配对。
     */
    fun enterAnalysis()

    /**
     * 标记当前线程退出一次分析。
     *
     * 它是 [enterAnalysis] 的配对方法，在分析块结束后立即调用。
     */
    fun exitAnalysis()

    /**
     * 调度一次缓存清理。
     *
     * 若当前可以安全清理，清理会立即执行；否则推迟到所有进行中的分析结束。
     * 允许重复调用：已有清理被挂起时，重复调用会被忽略。
     */
    fun scheduleCleanup()

    @CaImplementationDetail
    companion object {
        /**
         * 取得当前工程的缓存清理器。
         *
         * 强制清理可由 registry key `cangjie.analysis.lowMemoryCacheCleanup`（默认开启）关闭；
         * 服务未注册时同样退化为空操作实现，保证分析路径不因缺少该服务而失败。
         */
        fun getInstance(project: Project): CaCfirCacheCleaner {
            if (!Registry.`is`("cangjie.analysis.lowMemoryCacheCleanup", true)) {
                return CaCfirNoOpCacheCleaner
            }

            return project.serviceOrNull<CaCfirCacheCleaner>() ?: CaCfirNoOpCacheCleaner
        }
    }
}

/**
 * 空实现：不做任何额外清理。
 *
 * 当强制清理被 registry key 关闭，或宿主没有注册该服务时，作为 [CaCfirStopWorldCacheCleaner] 的替代品。
 */
private object CaCfirNoOpCacheCleaner : CaCfirCacheCleaner {
    override fun enterAnalysis() {}
    override fun exitAnalysis() {}
    override fun scheduleCleanup() {}
}

/**
 * stop-the-world 语义的缓存清理器。
 *
 * 不能在任意时刻清理：正在进行的分析会因为其 use-site session 被失效而失败。
 * 因此本实现登记清理请求并等待所有分析块结束；一旦有清理被请求，还会阻止新的分析进入，
 * 直到清理完成（见 [cleanupLatch]）。若当前没有进行中的分析，缓存可立即清理。
 */
private class CaCfirStopWorldCacheCleaner(private val project: Project) : CaCfirCacheCleaner {
    private companion object {
        private val LOG = logger<CaCfirStopWorldCacheCleaner>()

        /** 等待清理时单次 [CountDownLatch.await] 的超时时长。 */
        private const val CACHE_CLEANER_LOCK_TIMEOUT_MS = 50L
    }

    private val lock = Any()

    /** low-level analysis session 统计域；统计未启用时为 `null`。 */
    @CaCachedService
    private val analysisSessionStatistics: LLAnalysisSessionStatistics? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLStatisticsService.getInstance(project)?.analysisSessions
    }

    /** 承载全部 session 失效的服务。 */
    @CaCachedService
    private val invalidationService: LLCfirSessionInvalidationService by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLCfirSessionInvalidationService.getInstance(project)
    }

    /**
     * 正在并行执行的非嵌套分析数量，即所有线程上的分析块总数。
     *
     * 外层 `analyze` 块内的内层 `analyze` 块不计入。
     * 本实现统计进行中的分析，并在最后一个分析块结束时立即清理缓存。
     */
    @Volatile
    private var analyzerCount: Int = 0

    /** 当前线程内嵌套的分析层数；`0` 表示当前线程没有进行中的分析。 */
    private val analyzerDepth: ThreadLocal<Int> = ThreadLocal.withInitial { 0 }

    /** 当前持有进行中 [analyze] 的线程集合。 */
    private val threadsWithAnalyze = ConcurrentHashMap.newKeySet<Thread>()

    /**
     * 阻止新分析进入的闩锁，直到被挂起的清理完成为止。
     * [cleanupLatch] 为 `null` 表示当前没有被挂起的清理。
     */
    @Volatile
    private var cleanupLatch: CountDownLatch? = null

    /** 当前被推迟的清理的调度时刻；没有清理被调度时取任意值。 */
    @Volatile
    private var cleanupScheduleMs: Long = 0

    /** 当前线程是否存在进行中的分析，即新登记的分析块是否是嵌套的。 */
    private val hasOngoingAnalysis: Boolean
        get() = analyzerDepth.get() > 0

    override fun enterAnalysis() {
        // 嵌套分析不能阻塞，否则外层分析永远等不到内层分析结束，清理也就永远不会执行。
        if (hasOngoingAnalysis) {
            incAnalysisDepth()
            return
        }

        waitForCleanupIfNeeded()

        synchronized(lock) {
            // 登记一个顶层分析块。
            analyzerCount += 1
            threadsWithAnalyze.add(Thread.currentThread())
        }

        incAnalysisDepth()
    }

    override fun exitAnalysis() {
        decAnalysisDepth()

        // 与 enterAnalysis 一样忽略嵌套分析。
        if (hasOngoingAnalysis) {
            return
        }

        synchronized(lock) {
            // 注销一个顶层分析块。
            analyzerCount -= 1
            threadsWithAnalyze.remove(Thread.currentThread())

            require(analyzerCount >= 0) { "Inconsistency in analyzer block counter" }

            if (cleanupLatch != null) {
                LOG.trace { "Analysis complete in ${Thread.currentThread()}, $analyzerCount left before the K2 cache cleanup" }
            }

            // 有被挂起的清理且已无进行中的分析时执行清理。
            if (analyzerCount == 0) {
                val existingLatch = cleanupLatch
                if (existingLatch != null) {
                    try {
                        performCleanup()
                    } finally {
                        // 唤醒所有等待中的分析。
                        // 即使在 cleanupLatch 置空之前有新的分析块到达，旧闩锁此时也已打开。
                        existingLatch.countDown()
                        cleanupLatch = null
                    }
                }
            }
        }
    }

    /**
     * 若有清理被挂起，等待它完成。
     *
     * 等待其它线程可能比较棘手：分析块内部可能开启新线程，而新线程里又会调用 [enterAnalysis]，
     * 因此这里不能简单地"等待除当前嵌套分析外的所有线程"。
     *
     * 举例来说，父线程的 `analyze` 内部 `thread { }` 开启子线程，子线程里再调用 `analyze`，
     * 子线程在 [waitForCleanupIfNeeded] 的循环里等待，而父线程又在等待子线程结束，
     * 朴素的"等待所有其它线程"实现会死锁。
     *
     * 为缓解该问题，这里改用 [threadsWithAnalyze] 以避免死锁；但它并非完美方案，
     * 理论上在某些角落仍可能出现活锁：在上例中，子线程可能在 [waitForCleanupIfNeeded] 里循环，
     * 而父线程在循环 (3) 里等待子线程结束；若两者恰好每次都在对方检查线程状态时才处于
     * [Thread.State.RUNNABLE]，就会活锁。并行执行的动作越多，活锁概率越高。
     *
     * @see cleanupLatch
     */
    private fun waitForCleanupIfNeeded() {
        val existingLatch = cleanupLatch ?: return

        val enterTime = System.currentTimeMillis()
        do {
            ProgressManager.checkCanceled()

            if (threadsWithAnalyze.none { it.state == Thread.State.RUNNABLE }) {
                val totalMs = System.currentTimeMillis() - enterTime
                LOG.trace { "A deadlock detected in K2 cache cleanup, ${Thread.currentThread()} is recovered after $totalMs ms" }
                break
            }
        } while (!existingLatch.await(CACHE_CLEANER_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS))
    }

    private fun incAnalysisDepth() {
        analyzerDepth.set(analyzerDepth.get() + 1)
    }

    private fun decAnalysisDepth() {
        val oldValue = analyzerDepth.get()
        assert(oldValue > 0) { "Inconsistency in analysis depth counter" }
        analyzerDepth.set(oldValue - 1)
    }

    override fun scheduleCleanup() {
        synchronized(lock) {
            val existingLatch = cleanupLatch

            // 没有进行中的分析时立即清理；否则把清理推迟到之后。
            if (analyzerCount == 0) {
                // 这里刻意不在读写动作内执行缓存失效：此时可能已有写动作在等待本监视器，
                // 不能新开读写动作。但可以确定的是，直到清理完成之前没有线程能取得 session。
                cleanupScheduleMs = System.currentTimeMillis()

                try {
                    performCleanup()
                } finally {
                    if (existingLatch != null) {
                        // 极端情况下的错误恢复：除非算法本身有缺陷，否则不应走到这里。
                        existingLatch.countDown()
                        cleanupLatch = null
                        LOG.error("K2 cache cleanup was expected to happen right after the last analysis block completion")
                    }
                }
            } else if (existingLatch == null) {
                LOG.trace { "K2 cache cleanup scheduled from ${Thread.currentThread()}, $analyzerCount analyses left" }
                LLFlightRecorder.stopWorldSessionInvalidationScheduled()
                cleanupScheduleMs = System.currentTimeMillis()
                cleanupLatch = CountDownLatch(1)
            }
        }
    }

    /**
     * 清理全部 K2 解析缓存。
     *
     * 必须在 `synchronized(lock)` 内运行，以避免并发清理。
     *
     * 平台异常（[rethrowIntellijPlatformExceptionIfNeeded] 判定的那一类）会被重抛。
     */
    private fun performCleanup() {
        try {
            // performCleanup 也可能在工程释放之后由低内存监听线程触发；已释放的工程无需清理，
            // 跳过既可避免无谓工作，也能规避潜在的异常。
            if (project.isDisposed) return

            analysisSessionStatistics?.lowMemoryCacheCleanupInvocationCounter?.add(1)

            val cleanupMs = measureTimeMillis {
                invalidationService.invalidateAll(
                    includeLibraryModules = true,
                    diagnosticInformation = "low-memory cache cleanup",
                )
            }

            val totalMs = System.currentTimeMillis() - cleanupScheduleMs
            LOG.trace { "K2 cache cleanup complete from ${Thread.currentThread()} in $cleanupMs ms ($totalMs ms after the request)" }

            LLFlightRecorder.stopWorldSessionInvalidationComplete()
        } catch (e: Throwable) {
            rethrowIntellijPlatformExceptionIfNeeded(e)

            LOG.error("Could not clean up K2 caches", e)
        }
    }
}
