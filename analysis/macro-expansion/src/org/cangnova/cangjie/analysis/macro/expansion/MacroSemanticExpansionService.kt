package org.cangnova.cangjie.analysis.macro.expansion

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.AppExecutorUtil
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.macro.expansion.MacroExpansionOutcome
import org.cangnova.cangjie.macro.expansion.MacroExpansionRevision
import org.cangnova.cangjie.psi.CjMacroExpression
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 每工程一个的宏语义展开编排服务。
 *
 * 职责（全部与宿主无关）：
 * - 会话触发的入口与 in-flight 去重；
 * - 同一模块的构造串行化，避免多个会话同时重建造成 PSI 锁竞争与内存峰值；
 * - revision 记录、失败记录与旧快照失效；
 * - 通过 [MacroSemanticExpansionConstruction] 执行 pre-macro 构建、展开与发布；
 * - 为测试 / 批处理提供 [awaitIdle]。
 *
 * 宿主只通过 [MacroSemanticExpansionEnvironment] 提供取值、屏障与回调。
 */
interface MacroSemanticExpansionService {
    /** 请求为 [session] 所属模块构造 / 复用宏语义快照；会话失效或已有在途任务时安全忽略。 */
    fun request(session: LLCfirResolvableModuleSession)

    /**
     * 丢弃 [module] 的发布 / 失败记录。
     *
     * 已发布的快照本身由 `LLCfirResolvableModuleSession.invalidateMacroSemanticSnapshot()` 丢弃；
     * 本方法只清理框架侧的 revision 记录，使下一次 [request] 重新构造。
     */
    fun invalidate(module: CaSourceModule)

    /** 等待 [module] 当前所有在途构造结束；测试与批处理使用。 */
    fun awaitIdle(module: CaSourceModule)

    /** 当前模块的构造次数与最近结果，用于宿主遥测。 */
    fun metrics(module: CaSourceModule): MacroSemanticExpansionMetrics

    /** 查询已发布宏快照中该调用面的唯一宏/注解语义分类。 */
    fun invocationKind(expression: CjMacroExpression): MacroInvocationSemanticKind?

    companion object {
        /** 取得工程级服务实例；XML 未加载的容器需要先手工注册实现。 */
        fun getInstance(project: Project): MacroSemanticExpansionService = project.service()
    }
}

/** 宏构造解析器对一个调用面的最终分类。 */
enum class MacroInvocationSemanticKind {
    /** 唯一解析到宏定义或内建宏。 */
    MACRO,

    /** 唯一识别到语言内建注解。 */
    BUILTIN_ANNOTATION,

    /** 被宏构造解析器识别为内建或自定义注解候选；仍需 Analysis API 验证注解目标。 */
    ANNOTATION_CANDIDATE,
}

/** 可通过源码位置定位的宏调用解析结果。 */
internal data class MacroCallResolutionSite(
    val fileUrl: String,
    val startOffset: Int,
    val endOffset: Int,
    val name: String,
    val kind: MacroInvocationSemanticKind,
)

/**
 * 每模块的宏语义展开遥测快照。
 *
 * @property constructionsStarted 实际开始的构造次数（复用快照不计入）。
 * @property published 成功发布次数。
 * @property abandoned 失败/取消/输入变化次数。
 * @property lastOutcome 最近一次构造结果。
 * @property lastDurationMillis 最近一次构造耗时（毫秒）。
 */
data class MacroSemanticExpansionMetrics(
    val constructionsStarted: Long,
    val published: Long,
    val abandoned: Long,
    val lastOutcome: MacroExpansionOutcome?,
    val lastDurationMillis: Long,
)

/**
 * [MacroSemanticExpansionService] 的默认实现。
 *
 * 构造发生在后台线程；模块级串行锁只保护构造本身，会话触发（[request]）不会阻塞调用方线程。
 */
@Service(Service.Level.PROJECT)
internal class MacroSemanticExpansionServiceImpl(private val project: Project) : MacroSemanticExpansionService {
    /** 模块级编排状态；所有字段由 [lock] 保护。 */
    private class ModuleState {
        /** 状态锁；同时承载 [awaitIdle] 的等待条件。 */
        val lock = ReentrantLock()
        private val idle = lock.newCondition()

        /** 最近一次成功发布的输入 revision。 */
        var publishedRevision: MacroExpansionRevision? = null

        /** 最近一次构造失败的输入 revision；同一输入不再重复整模块构建。 */
        var failedRevision: MacroExpansionRevision? = null

        /** 最近一次已发布快照中的成功宏解析结果。 */
        var macroCallResolutionSites: List<MacroCallResolutionSite> = emptyList()

        /** 当前在途构造数量，供 [awaitIdle] 等待。 */
        var activeTasks: Int = 0

        /** 构造次数与最近结果。 */
        var constructionsStarted: Long = 0
        var published: Long = 0
        var abandoned: Long = 0
        var lastOutcome: MacroExpansionOutcome? = null
        var lastDurationMillis: Long = 0

        fun awaitIdle() {
            lock.withLock {
                while (activeTasks > 0) {
                    idle.await(AWAIT_IDLE_POLL_MILLIS, TimeUnit.MILLISECONDS)
                }
            }
        }

        fun taskStarted() {
            lock.withLock { activeTasks++ }
        }

        fun taskFinished() {
            lock.withLock {
                activeTasks--
                idle.signalAll()
            }
        }
    }

    /**
     * 一次 submit 独占的终态登记；模块 lastOutcome 只是汇总，不能证明某个任务已经收尾。
     *
     * 取消方与执行方沿用 started 竞争，终态 CAS 只负责让计数/回调恰好登记一次。
     * CAS 与模块计数在同一状态锁内完成，宿主回调在锁外调用；回调失败不能触发第二次终态。
     */
    private class TaskTerminal(
        private val session: LLCfirResolvableModuleSession,
        private val state: ModuleState,
        private val environment: MacroSemanticExpansionEnvironment,
        private val onTerminalFinished: () -> Unit,
    ) {
        private val terminalClaimed = AtomicBoolean()

        /** 只保存本任务实际返回的 construction outcome；未执行/未返回时为空。 */
        @Volatile
        private var constructionOutcome: MacroExpansionOutcome? = null

        /** 原 construct 计时点；未构造的任务不制造耗时值或 started 回调。 */
        @Volatile
        private var constructionStartedAtNanos: Long? = null

        fun startConstructionTiming() {
            constructionStartedAtNanos = System.nanoTime()
        }

        fun recordConstructionCompletion(completion: MacroExpansionCompletion) {
            constructionOutcome = completion.outcome
        }

        fun elapsedMillis(): Long? = constructionStartedAtNanos?.let { startedAt ->
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
        }

        fun complete(
            outcome: MacroExpansionOutcome,
            durationMillis: Long?,
            updateModuleState: () -> Unit,
            notifyEnvironment: () -> Unit,
        ) {
            val claimed = state.lock.withLock {
                if (!terminalClaimed.compareAndSet(false, true)) return@withLock false
                state.lastOutcome = outcome
                if (durationMillis != null) state.lastDurationMillis = durationMillis
                if (outcome == MacroExpansionOutcome.PUBLISHED) {
                    state.published++
                } else {
                    state.abandoned++
                }
                // 此块仅提交已取得的 revision/结果值，不能调用可能失败的宿主取值或回调。
                updateModuleState()
                true
            }
            if (claimed) {
                try {
                    notifyEnvironment()
                } finally {
                    // 回调成功或抛异常都结束本次终态尝试；在途收尾还必须等真实执行体结束。
                    onTerminalFinished()
                }
            }
        }

        fun abandon(outcome: MacroExpansionOutcome) {
            complete(
                outcome = outcome,
                durationMillis = elapsedMillis(),
                updateModuleState = {},
                notifyEnvironment = { environment.onAbandoned(session, outcome) },
            )
        }

        /**
         * 整个已登记任务的异常出口。真实 FAILED 不被后续取消抹掉，PCE/中断原样交回调用方。
         * 收尾回调或日志再失败时附在原异常上，不能替换原 PCE，也不能重发 onAbandoned。
         */
        fun reportFailure(failure: Throwable) {
            if (failure is InterruptedException) Thread.currentThread().interrupt()
            val outcome = when {
                constructionOutcome == MacroExpansionOutcome.FAILED -> MacroExpansionOutcome.FAILED
                failure is ProcessCanceledException || failure is InterruptedException -> MacroExpansionOutcome.CANCELLED
                else -> MacroExpansionOutcome.FAILED
            }
            try {
                abandon(outcome)
            } catch (notificationFailure: Throwable) {
                if (notificationFailure !== failure) failure.addSuppressed(notificationFailure)
            }
            if (failure !is ProcessCanceledException && failure !is InterruptedException) {
                try {
                    LOG.error("Failed to execute macro semantic expansion task for ${session.caModule.moduleDescription}", failure)
                } catch (loggingFailure: Throwable) {
                    if (loggingFailure !== failure) failure.addSuppressed(loggingFailure)
                }
            }
        }
    }

    private val moduleStates = ConcurrentHashMap<CaModule, ModuleState>()
    private val moduleLocks = ConcurrentHashMap<CaModule, Any>()
    private val inFlightSessions: MutableSet<LLCfirSession> =
        Collections.newSetFromMap(ConcurrentHashMap())
    private val construction = MacroSemanticExpansionConstruction(project)

    override fun request(session: LLCfirResolvableModuleSession) {
        if (project.isDisposed || !session.isValid) return
        val module = session.caModule as? CaSourceModule ?: return
        val environment = MacroSemanticExpansionEnvironment.getInstance(project) ?: return
        val revision = environment.revision(module)
        val state = stateFor(module)
        var snapshotInvalidated = false

        state.lock.withLock {
            if (state.failedRevision != revision) {
                state.failedRevision = null
                if (state.publishedRevision != revision) {
                    state.macroCallResolutionSites = emptyList()
                }
            }
            // 同一 revision 已经失败过：不再重复整模块构建，等输入变化后再试。
            if (state.failedRevision == revision) {
                LOG.info(
                    "Skip macro semantic snapshot rebuild for ${module.moduleDescription}: " +
                        "the same input revision previously failed",
                )
                return
            }

            // 仅复用当前 session 已有的同 revision 快照；模块级编排记录不持有另一 session 的 CFIR 树。
            if (session.macroSemanticSnapshot != null) {
                if (state.publishedRevision == revision) {
                    LOG.info("Reused macro semantic snapshot for ${module.moduleDescription}")
                    return
                }

                // 当前 session 快照仍在，但输入已变化：先退役其语义版本，再重新构造。
                LOG.info(
                    "Macro semantic snapshot input changed for ${module.moduleDescription}; rebuilding",
                )
                session.invalidateMacroSemanticSnapshot()
                state.publishedRevision = null
                state.macroCallResolutionSites = emptyList()
                snapshotInvalidated = true
            }
        }

        if (snapshotInvalidated) {
            environment.onSnapshotInvalidated(module)
        }
        submit(session, module, state, environment, revision)
    }

    override fun invalidate(module: CaSourceModule) {
        val state = stateFor(module)
        val snapshotInvalidated = state.lock.withLock {
            val wasPublished = state.publishedRevision != null
            state.publishedRevision = null
            state.failedRevision = null
            state.macroCallResolutionSites = emptyList()
            wasPublished
        }
        if (snapshotInvalidated) {
            MacroSemanticExpansionEnvironment.getInstance(project)?.onSnapshotInvalidated(module)
        }
    }

    override fun awaitIdle(module: CaSourceModule) {
        stateFor(module).awaitIdle()
    }

    override fun metrics(module: CaSourceModule): MacroSemanticExpansionMetrics {
        val state = stateFor(module)
        return state.lock.withLock {
            MacroSemanticExpansionMetrics(
                constructionsStarted = state.constructionsStarted,
                published = state.published,
                abandoned = state.abandoned,
                lastOutcome = state.lastOutcome,
                lastDurationMillis = state.lastDurationMillis,
            )
        }
    }

    override fun invocationKind(expression: CjMacroExpression): MacroInvocationSemanticKind? {
        val fileUrl = expression.containingFile.virtualFile?.url ?: return null
        val referenceName = expression.referenceExpression?.referencedNameElement ?: return null
        val name = expression.shortName?.asString() ?: return null
        val nameRange = referenceName.textRange
        val candidates = moduleStates.values.flatMap { state ->
            state.lock.withLock {
                state.macroCallResolutionSites.filter { site ->
                    site.fileUrl == fileUrl &&
                        site.name == name &&
                        site.startOffset <= nameRange.startOffset &&
                        site.endOffset >= nameRange.endOffset
                }
            }
        }
        val smallestRange = candidates.minOfOrNull { site -> site.endOffset - site.startOffset } ?: return null
        return candidates.asSequence()
            .filter { site -> site.endOffset - site.startOffset == smallestRange }
            .map(MacroCallResolutionSite::kind)
            .distinct()
            .singleOrNull()
    }

    /**
     * 提交一次构造任务；同一会话已有在途任务时直接忽略。
     *
     * 任务在线程池中等待模块锁，拿到锁后再检查会话与 revision，避免为已失效会话做无意义构建。
     */
    private fun submit(
        session: LLCfirResolvableModuleSession,
        module: CaSourceModule,
        state: ModuleState,
        environment: MacroSemanticExpansionEnvironment,
        requestedRevision: MacroExpansionRevision,
    ) {
        if (!inFlightSessions.add(session)) return

        val started = AtomicBoolean()
        // 构造开始/取消回调沿用 started；执行体入口与未进入执行体的取消另行登记在途收尾。
        val executionClaimed = AtomicBoolean()
        val finished = AtomicBoolean()
        val executionFinished = AtomicBoolean()
        val terminalFinished = AtomicBoolean()
        fun finishTaskIfSettled() {
            if (executionFinished.get() && terminalFinished.get() && finished.compareAndSet(false, true)) {
                inFlightSessions.remove(session)
                state.taskFinished()
            }
        }
        fun finishTask() {
            executionFinished.set(true)
            finishTaskIfSettled()
        }
        val taskTerminal = TaskTerminal(session, state, environment) {
            // 取消方可在执行体之外回调；只在两方都结束后释放在途登记，避免 idle 先于终态。
            terminalFinished.set(true)
            finishTaskIfSettled()
        }

        state.taskStarted()
        state.lock.withLock { state.constructionsStarted++ }
        val future: Future<*> = try {
            AppExecutorUtil.getAppExecutorService().submit task@ {
                if (!executionClaimed.compareAndSet(false, true)) return@task
                try {
                    val lock = moduleLocks.computeIfAbsent(module) { Any() }
                    synchronized(lock) {
                        if (started.compareAndSet(false, true)) {
                            if (session.isValid &&
                                !project.isDisposed &&
                                !Thread.currentThread().isInterrupted
                            ) {
                                runTask(session, module, state, environment, requestedRevision, taskTerminal)
                            } else {
                                taskTerminal.abandon(MacroExpansionOutcome.CANCELLED)
                            }
                        }
                    }
                } catch (failure: Throwable) {
                    // 包含 runTask 最初 revision、复用/构造 started 回调、结果 revision 和终态回调。
                    taskTerminal.reportFailure(failure)
                    throw failure
                } finally {
                    finishTask()
                }
            }
        } catch (failure: Throwable) {
            try {
                taskTerminal.reportFailure(failure)
            } finally {
                finishTask()
            }
            throw failure
        }

        try {
            Disposer.register(
                session.requestDisposable(),
                Disposable {
                    try {
                        future.cancel(true)
                        // 保留原取消回调：执行体可能已进入，但仍在模块锁外等待、尚未开始构造。
                        if (started.compareAndSet(false, true)) {
                            taskTerminal.abandon(MacroExpansionOutcome.CANCELLED)
                        }
                    } catch (failure: Throwable) {
                        taskTerminal.reportFailure(failure)
                        throw failure
                    } finally {
                        // 未进入执行体的 Future 取消后不会运行 lambda；其在途登记由取消方收尾。
                        if (executionClaimed.compareAndSet(false, true)) finishTask()
                    }
                },
            )
        } catch (failure: Throwable) {
            // 父 disposable 登记失败时 Future 可能已运行；取消在锁外，执行体仍由原 finally 收尾。
            try {
                future.cancel(true)
            } catch (cancellationFailure: Throwable) {
                if (cancellationFailure !== failure) failure.addSuppressed(cancellationFailure)
            }
            try {
                taskTerminal.reportFailure(failure)
            } finally {
                if (executionClaimed.compareAndSet(false, true)) finishTask()
            }
            throw failure
        }
    }

    /** 在模块锁内执行一次构造；所有异常由 submit 的同一任务边界处理。 */
    private fun runTask(
        session: LLCfirResolvableModuleSession,
        module: CaSourceModule,
        state: ModuleState,
        environment: MacroSemanticExpansionEnvironment,
        requestedRevision: MacroExpansionRevision,
        taskTerminal: TaskTerminal,
    ) {
        // outer guard 之后仍可能退役/关闭/中断；执行方已拥有 started，不能裸返回。
        if (!session.isValid || project.isDisposed || Thread.currentThread().isInterrupted) {
            taskTerminal.abandon(MacroExpansionOutcome.CANCELLED)
            return
        }
        // 等待模块锁期间，其它会话可能已完成发布：直接复用，避免再做一次整模块 pre-macro 构建。
        val currentRevision = environment.revision(module)
        val publishedRevision = state.lock.withLock { state.publishedRevision }
        if (session.macroSemanticSnapshot != null && publishedRevision == currentRevision) {
            LOG.info(
                "Reused macro semantic snapshot published by another session for ${module.moduleDescription}",
            )
            environment.onConstructionStarted(session)
            if (!session.isValid || project.isDisposed || Thread.currentThread().isInterrupted) {
                taskTerminal.abandon(MacroExpansionOutcome.CANCELLED)
                return
            }
            taskTerminal.complete(
                outcome = MacroExpansionOutcome.PUBLISHED,
                durationMillis = null,
                updateModuleState = {},
                notifyEnvironment = { environment.onPublished(session, emptySet()) },
            )
            return
        }

        taskTerminal.startConstructionTiming()
        val completion = construction.construct(session, module, environment)
        taskTerminal.recordConstructionCompletion(completion)
        val elapsedMillis = checkNotNull(taskTerminal.elapsedMillis())
        if (!session.isValid || project.isDisposed || Thread.currentThread().isInterrupted) {
            // 保留实际 FAILED；失效会话的完成结果不能作为 live PUBLISHED 继续通知消费者。
            val terminalOutcome = if (completion.outcome == MacroExpansionOutcome.PUBLISHED) {
                MacroExpansionOutcome.CANCELLED
            } else {
                completion.outcome
            }
            taskTerminal.abandon(terminalOutcome)
            return
        }
        LOG.info(
            "Macro semantic expansion construction for ${module.moduleDescription} finished in " +
                "${elapsedMillis}ms with outcome=${completion.outcome}",
        )

        val outcomeRevision = environment.revision(module)
        taskTerminal.complete(
            outcome = completion.outcome,
            durationMillis = elapsedMillis,
            updateModuleState = {
                when (completion.outcome) {
                    MacroExpansionOutcome.PUBLISHED -> {
                        // 记录快照实际代表的输入 revision；构造/发布期间输入变化不会让新 revision 误命中旧快照。
                        state.publishedRevision = completion.publishedRevision ?: outcomeRevision
                        state.failedRevision = null
                        state.macroCallResolutionSites = completion.macroCallResolutionSites
                    }

                    MacroExpansionOutcome.NO_MACRO_SURFACE -> {
                        state.publishedRevision = null
                        state.macroCallResolutionSites = emptyList()
                    }

                    MacroExpansionOutcome.FAILED -> {
                        // 只有实际取得的结果 revision 才登记失败输入，不能用 requested/use-site 值补造。
                        state.failedRevision = outcomeRevision
                        state.macroCallResolutionSites = completion.macroCallResolutionSites
                    }

                    MacroExpansionOutcome.CANCELLED,
                    MacroExpansionOutcome.INPUT_CHANGED,
                        -> Unit
                }
            },
            notifyEnvironment = {
                when (completion.outcome) {
                    MacroExpansionOutcome.PUBLISHED ->
                        environment.onPublished(session, completion.publishedFiles)

                    else ->
                        environment.onAbandoned(session, completion.outcome)
                }
            },
        )

        val snapshotRevision = completion.publishedRevision
        if (completion.outcome == MacroExpansionOutcome.PUBLISHED &&
            snapshotRevision != null &&
            requestedRevision != snapshotRevision
        ) {
            LOG.info(
                "Macro semantic snapshot published for ${module.moduleDescription} with a newer input revision; " +
                    "requested=$requestedRevision published=$snapshotRevision",
            )
        }
    }

    private fun stateFor(module: CaModule): ModuleState =
        moduleStates.computeIfAbsent(module) { ModuleState() }

    private companion object {
        private const val AWAIT_IDLE_POLL_MILLIS = 50L
        val LOG: Logger = Logger.getInstance(MacroSemanticExpansionServiceImpl::class.java)
    }
}
