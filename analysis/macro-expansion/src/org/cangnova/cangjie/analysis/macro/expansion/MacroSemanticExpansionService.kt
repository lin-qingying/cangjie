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

    companion object {
        /** 取得工程级服务实例；XML 未加载的容器需要先手工注册实现。 */
        fun getInstance(project: Project): MacroSemanticExpansionService = project.service()
    }
}

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

        state.lock.withLock {
            // 同一 revision 已经失败过：不再重复整模块构建，等输入变化后再试。
            if (state.failedRevision == revision) {
                LOG.info(
                    "Skip macro semantic snapshot rebuild for ${module.moduleDescription}: " +
                        "the same input revision previously failed",
                )
                return
            }

            // 快照按模块共享：输入 revision 未变化时直接复用，不再为每个新 session 重跑整模块构建。
            if (session.macroSemanticSnapshot != null) {
                if (state.publishedRevision == revision) {
                    LOG.info("Reused macro semantic snapshot for ${module.moduleDescription}")
                    return
                }

                // 快照仍在，但输入已变化：先丢弃模块级快照，再重新构造。
                LOG.info(
                    "Macro semantic snapshot input changed for ${module.moduleDescription}; rebuilding",
                )
                session.invalidateMacroSemanticSnapshot()
                state.publishedRevision = null
            }
        }

        submit(session, module, state, environment, revision)
    }

    override fun invalidate(module: CaSourceModule) {
        val state = stateFor(module)
        state.lock.withLock {
            state.publishedRevision = null
            state.failedRevision = null
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
        state.taskStarted()
        state.lock.withLock { state.constructionsStarted++ }
        val future: Future<*> = try {
            AppExecutorUtil.getAppExecutorService().submit {
                try {
                    val lock = moduleLocks.computeIfAbsent(module) { Any() }
                    synchronized(lock) {
                        if (started.compareAndSet(false, true) &&
                            session.isValid &&
                            !project.isDisposed &&
                            !Thread.currentThread().isInterrupted
                        ) {
                            runTask(session, module, state, environment, requestedRevision)
                        }
                    }
                } finally {
                    inFlightSessions.remove(session)
                    state.taskFinished()
                }
            }
        } catch (failure: Throwable) {
            inFlightSessions.remove(session)
            state.taskFinished()
            throw failure
        }

        Disposer.register(
            session.requestDisposable(),
            Disposable {
                future.cancel(true)
                // 任务从未开始：由这里补齐收尾回调，避免宿主等待永不发生的构造。
                if (started.compareAndSet(false, true)) {
                    state.lock.withLock {
                        state.abandoned++
                        state.lastOutcome = MacroExpansionOutcome.CANCELLED
                    }
                    environment.onAbandoned(session, MacroExpansionOutcome.CANCELLED)
                }
            },
        )
    }

    /** 在模块锁内执行一次构造，并按结果更新 revision 记录、回调宿主。 */
    private fun runTask(
        session: LLCfirResolvableModuleSession,
        module: CaSourceModule,
        state: ModuleState,
        environment: MacroSemanticExpansionEnvironment,
        requestedRevision: MacroExpansionRevision,
    ) {
        if (project.isDisposed) {
            LOG.info("Project disposed before macro expansion for ${module.moduleDescription}")
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
            state.lock.withLock {
                state.published++
                state.lastOutcome = MacroExpansionOutcome.PUBLISHED
            }
            environment.onPublished(session, emptySet())
            return
        }

        val startedAtNanos = System.nanoTime()
        val completion = try {
            construction.construct(session, module, environment)
        } catch (cancelled: ProcessCanceledException) {
            // PCE 不记录日志、必须原样抛出；这里只补齐宿主收尾回调，避免等待者永远收不到结束信号。
            state.lock.withLock {
                state.abandoned++
                state.lastOutcome = MacroExpansionOutcome.CANCELLED
            }
            environment.onAbandoned(session, MacroExpansionOutcome.CANCELLED)
            throw cancelled
        }
        val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos)
        if (project.isDisposed) {
            LOG.info(
                "Project disposed during macro expansion for ${module.moduleDescription}; " +
                    "dropping completion callbacks (outcome=${completion.outcome})",
            )
            return
        }
        LOG.info(
            "Macro semantic expansion construction for ${module.moduleDescription} finished in " +
                "${elapsedMillis}ms with outcome=${completion.outcome}",
        )

        val outcomeRevision = environment.revision(module)
        state.lock.withLock {
            state.lastOutcome = completion.outcome
            state.lastDurationMillis = elapsedMillis
            if (completion.outcome == MacroExpansionOutcome.PUBLISHED) {
                state.published++
            } else {
                state.abandoned++
            }
            when (completion.outcome) {
                MacroExpansionOutcome.PUBLISHED -> {
                    // 记录快照实际代表的输入 revision；构造/发布期间输入变化不会让新 revision 误命中旧快照。
                    state.publishedRevision = completion.publishedRevision ?: outcomeRevision
                    state.failedRevision = null
                }

                MacroExpansionOutcome.NO_MACRO_SURFACE -> {
                    state.publishedRevision = null
                }

                MacroExpansionOutcome.FAILED -> {
                    // 记录失败 revision，避免同一输入在没有变化时被反复整模块重试。
                    state.failedRevision = outcomeRevision
                }

                MacroExpansionOutcome.CANCELLED,
                MacroExpansionOutcome.INPUT_CHANGED,
                    -> Unit
            }
        }

        when (completion.outcome) {
            MacroExpansionOutcome.PUBLISHED ->
                environment.onPublished(session, completion.publishedFiles)

            else ->
                environment.onAbandoned(session, completion.outcome)
        }

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
