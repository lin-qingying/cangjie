package org.cangnova.cangjie.analysis.api.cfir.session

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.LowMemoryWatcher
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.CaCfirCacheCleaner
import org.cangnova.cangjie.analysis.api.impl.base.sessions.CaBaseSessionProvider
import org.cangnova.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry
import org.cangnova.cangjie.analysis.api.platform.CaCachedService
import org.cangnova.cangjie.analysis.api.platform.CangJieAnalysisInWriteActionListener
import org.cangnova.cangjie.analysis.api.platform.analysisMessageBus
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.LLCfirDeclarationModificationService
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLResolutionFacadeService
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionInvalidationListener
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLAnalysisSessionStatistics
import org.cangnova.cangjie.psi.CjElement
import java.util.concurrent.ConcurrentHashMap

/**
 * CFIR Analysis API 会话提供器。
 *
 * 它位于 `analysis-api-cfir` 这一层，只负责：
 * 1. 根据 use-site 元素或模块选择对应 Analysis API session。
 * 2. 维护 Analysis API session 级缓存。
 * 3. 把失效请求同步到底层 `CaCfirResolutionFacadeService`。
 * 4. 维护 Analysis API 统计域与强制缓存清理器：取得 session 前进入缓存清理器的分析域，
 *    离开分析时归还；低内存事件则调度一次 stop-the-world 清理。
 *
 * 缓存清理器要求进入与离开成对（见 `CaCfirCacheCleaner`）：本 provider 的批量入口
 * `analyzeElements` / `analyzeModules` 先按 use-site module 分组，每个 module 恰好一次
 * `getAnalysisSession` 与一次 `afterLeavingAnalysis`，因此批量路径同样成对。
 *
 * 具体的 CFIR session 构建、Raw CFIR 生成与 resolve 流程全部留在 low-level 模块中。
 */
class CaCfirSessionProvider(
    project: Project,
) : CaBaseSessionProvider(project), CaSessionInvalidationService {
    /**
     * use-site 模块到 CFIR Analysis API session 的缓存。
     */
    private val cache = ConcurrentHashMap<CaModule, CaCfirSession>()

    /**
     * low-level CFIR resolution facade 服务。
     */
    @OptIn(LLCfirInternals::class)
    private val resolutionFacadeService by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLResolutionFacadeService.getInstance(project)
    }

    /**
     * 平台 project-structure provider，用于从 PSI 恢复 use-site 模块。
     */
    private val projectStructureProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CangJieProjectStructureProvider.getInstance(project)
    }

    /**
     * low-level analysis session 统计域；统计开关关闭或缺少 OpenTelemetry 实例时为 `null`。
     */
    @CaCachedService
    private val analysisSessionStatistics: LLAnalysisSessionStatistics? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLStatisticsService.getInstance(project)?.analysisSessions
    }

    /**
     * 强制场景下的 low-level session 缓存清理器（对齐 Kotlin `KaFirCacheCleaner`）。
     */
    @CaCachedService
    private val cacheCleaner: CaCfirCacheCleaner by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaCfirCacheCleaner.getInstance(project)
    }

    /**
     * 低内存事件监听器；provider 释放时必须停止，避免监听器继续持有已释放的 provider。
     */
    private val lowMemoryWatcher: LowMemoryWatcher = LowMemoryWatcher.register(::handleLowMemoryEvent)

    /**
     * 低内存时调度一次 stop-the-world 缓存清理。
     *
     * 真正的清理由 [cacheCleaner] 在所有进行中的分析退出后执行；这里只递交请求。
     *
     * 与 Kotlin `KaFirSessionProvider.handleLowMemoryEvent` 的差异：Kotlin 侧还会顺带做
     * Caffeine 缓存维护与 session 结构 GraphML 导出，仓颉侧这两项尚未对齐，不在本职责内。
     */
    private fun handleLowMemoryEvent() {
        cacheCleaner.scheduleCleanup()
    }

    /**
     * 根据 use-site PSI 元素获取 Analysis API session。
     */
    override fun getAnalysisSession(useSiteElement: CjElement): CaSession {
        val module = resolveUseSiteModule(useSiteElement)
        return getAnalysisSession(module)
    }

    /**
     * 根据 use-site 模块获取或创建 Analysis API session。
     */
    override fun getAnalysisSession(useSiteModule: CaModule): CaSession {
        ProgressManager.checkCanceled()
        flushDeferredModificationsIfInsideWriteAction()

        // 必须在取得 session 之前进入缓存清理器的分析域：
        // 否则刚拿到的 session 可能被并发执行的强制清理立即失效。
        cacheCleaner.enterAnalysis()
        try {
            val session = cache.getOrPut(useSiteModule) {
                createAnalysisSession(useSiteModule)
            }

            checkSessionValidity(session)
            return session
        } catch (e: Throwable) {
            cacheCleaner.exitAnalysis()
            throw e
        }
    }

    /**
     * 进入基于 PSI 元素的分析前发布写动作分析事件。
     */
    override fun beforeEnteringAnalysis(session: CaSession, useSiteElement: CjElement) {
        try {
            analysisSessionStatistics?.analyzeCallCounter?.add(1)
            super.beforeEnteringAnalysis(session, useSiteElement)
            publishEnteringAnalysisInWriteActionIfNeeded()
        } catch (e: Throwable) {
            // getAnalysisSession 已进入缓存清理器的分析域，进入阶段失败时必须在这里归还。
            cacheCleaner.exitAnalysis()
            throw e
        }
    }

    /**
     * 进入基于模块的分析前发布写动作分析事件。
     */
    override fun beforeEnteringAnalysis(session: CaSession, useSiteModule: CaModule) {
        try {
            analysisSessionStatistics?.analyzeCallCounter?.add(1)
            super.beforeEnteringAnalysis(session, useSiteModule)
            publishEnteringAnalysisInWriteActionIfNeeded()
        } catch (e: Throwable) {
            // getAnalysisSession 已进入缓存清理器的分析域，进入阶段失败时必须在这里归还。
            cacheCleaner.exitAnalysis()
            throw e
        }
    }

    /**
     * 离开基于 PSI 元素的分析后发布写动作分析事件。
     */
    override fun afterLeavingAnalysis(session: CaSession, useSiteElement: CjElement) {
        afterLeavingAnalysisImpl { super.afterLeavingAnalysis(session, useSiteElement) }
    }

    /**
     * 离开基于模块的分析后发布写动作分析事件。
     */
    override fun afterLeavingAnalysis(session: CaSession, useSiteModule: CaModule) {
        afterLeavingAnalysisImpl { super.afterLeavingAnalysis(session, useSiteModule) }
    }

    /**
     * 离开分析域的公共收尾：基类回收 lifetime、发布写动作事件，最后退出缓存清理器的分析域。
     *
     * 退出必须位于最外层 `finally`：基类回收或平台事件发布抛错时，
     * 同样要归还进入计数，否则挂起的清理将永远无法执行。
     */
    private inline fun afterLeavingAnalysisImpl(superCall: () -> Unit) {
        try {
            try {
                superCall()
            } finally {
                publishAfterLeavingAnalysisInWriteActionIfNeeded()
            }
        } finally {
            cacheCleaner.exitAnalysis()
        }
    }

    /**
     * 逐出指定模块对应的 Analysis API session 缓存。
     */
    override fun invalidate(modules: Set<CaModule>) {
        modules.forEach(cache::remove)
    }

    /**
     * CFIR 批量元素分析优先按 project-structure 恢复 use-site module，
     * 再复用统一的模块批量入口。
     *
     * 这样模块选择、session cache 和 low-level facade 获取会沿同一条链工作，
     * 不再先“元素 -> session”逐个解析，再在基类里反推 session 分组。
     */
    override fun <R> analyzeElements(
        useSiteElements: Collection<CjElement>,
        action: CaSession.(CjElement) -> R,
    ): List<R> {
        if (useSiteElements.isEmpty()) return emptyList()

        val groupedElements = useSiteElements.withIndex().groupBy(
            keySelector = { indexedElement ->
                resolveUseSiteModule(indexedElement.value)
            },
            valueTransform = { indexedElement ->
                indexedElement.index to indexedElement.value
            },
        )

        val results = arrayOfNulls<Any?>(useSiteElements.size)
        analyzeModules(groupedElements.keys) { useSiteModule ->
            groupedElements.getValue(useSiteModule).forEach { (index, element) ->
                results[index] = action(element)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return results.map { it as R }
    }

    /**
     * 按 use-site module 逐个进入分析域。
     *
     * CFIR 的 Analysis API session 按 use-site module 一一缓存（见 [cache]），因此不同 module
     * 不会共享同一个 session，基类"先取得全部 session、再按 session 分组"的做法在这里没有收益，
     * 却会让批量入口在某个 module 的进入阶段抛错时留下未配对的缓存清理器进入计数。
     * 逐 module 进入既与基类语义等价（每个 module 各自进入、退出一次分析域），又保证
     * `getAnalysisSession` 与 `afterLeavingAnalysis` 严格成对。
     */
    override fun <R> analyzeModules(useSiteModules: Collection<CaModule>, action: CaSession.(CaModule) -> R): List<R> {
        if (useSiteModules.isEmpty()) return emptyList()

        val results = arrayOfNulls<Any?>(useSiteModules.size)
        useSiteModules.forEachIndexed { index, useSiteModule ->
            results[index] = analyze(useSiteModule) { action(useSiteModule) }
        }

        @Suppress("UNCHECKED_CAST")
        return results.map { it as R }
    }

    /**
     * 清空当前 provider 管理的全部 session 缓存。
     */
    override fun clearCaches() {
        invalidate(cache.keys.toSet())
    }

    /**
     * provider 释放时清空 session 缓存。
     */
    override fun dispose() {
        lowMemoryWatcher.stop()
        clearCaches()
    }

    /**
     * 为指定 use-site 模块创建新的 CFIR Analysis API session。
     */
    @OptIn(CaPlatformInterface::class, LLCfirInternals::class)
    private fun createAnalysisSession(useSiteModule: CaModule): CaCfirSession {
        val resolutionFacade = resolutionFacadeService.getResolutionFacade(useSiteModule)
        val token = tokenFactory.create(project, resolutionFacade.useSiteCfirSession.createValidityTracker())
        return CaCfirSession.createAnalysisSessionByResolutionFacade(

            resolutionFacade = resolutionFacade,
            token = token,
        )
    }

    /**
     * 确认缓存返回的 session 仍然有效。
     */
    private fun checkSessionValidity(session: CaCfirSession) {
        require(session.token.isValid()) {
            "通过 `getAnalysisSession` 获取的 Analysis API session 必须保持有效。"
        }
    }

    /**
     * 对齐 Kotlin `KaFirSessionProvider`：
     * 当分析发生在写动作中时，必须先把 low-level CFIR 延迟失效队列冲刷到当前时刻，
     * 否则本轮分析仍会读取编辑前的 file-structure / diagnostics 快照。
     */
    @OptIn(LLCfirInternals::class)
    private fun flushDeferredModificationsIfInsideWriteAction() {
        if (!ApplicationManager.getApplication().isWriteAccessAllowed) return
        LLCfirDeclarationModificationService.getInstance(project).flushDeferredModifications()
    }

    /**
     * 在写动作分析开始时发布平台事件。
     */
    @OptIn(CaPlatformInterface::class)
    private fun publishEnteringAnalysisInWriteActionIfNeeded() {
        if (!isAnalysisInWriteAction()) return
        project.analysisMessageBus.syncPublisher(CangJieAnalysisInWriteActionListener.TOPIC).onEnteringAnalysisInWriteAction()
    }

    /**
     * 在写动作分析结束时发布平台事件。
     */
    @OptIn(CaPlatformInterface::class)
    private fun publishAfterLeavingAnalysisInWriteActionIfNeeded() {
        if (!isAnalysisInWriteAction()) return
        project.analysisMessageBus.syncPublisher(CangJieAnalysisInWriteActionListener.TOPIC).afterLeavingAnalysisInWriteAction()
    }

    /**
     * 判断当前是否处于允许 Analysis API 运行的写动作中。
     */
    private fun isAnalysisInWriteAction(): Boolean {
        return ApplicationManager.getApplication().isWriteAccessAllowed &&
                CaAnalysisPermissionRegistry.getInstance().isAnalysisAllowedInWriteAction
    }

    /**
     * 元素到 use-site module 的恢复必须始终走平台 project-structure 服务，
     * 保证 CFIR session provider 与平台模块图使用同一份结构事实。
     */
    private fun resolveUseSiteModule(useSiteElement: CjElement): CaModule {
        return projectStructureProvider.getModule(useSiteElement, useSiteModule = null)
    }

    /**
     * 与 Kotlin `KaFirSessionProvider.SessionInvalidationListener` 对齐：
     * low-level CFIR session 失效后，analysis session cache 必须同步逐出对应条目，
     * 否则下一次 analysis 仍可能从 provider cache 取回已失效 token 的旧 session。
     */
    internal class SessionInvalidationListener(private val project: Project) : LLCfirSessionInvalidationListener {
        /**
         * 当前项目注册的 CFIR Analysis API session provider。
         */
        private val analysisSessionProvider: CaCfirSessionProvider
            get() = getInstance(project) as? CaCfirSessionProvider
                ?: error("Expected the analysis session provider to be a `${CaCfirSessionProvider::class.simpleName}`.")

        /**
         * low-level 按模块失效后逐出对应 Analysis API session。
         */
        override fun afterInvalidation(modules: Set<CaModule>) {
            analysisSessionProvider.invalidate(modules)
        }

        /**
         * low-level 全局失效后清空全部 Analysis API session。
         */
        override fun afterGlobalInvalidation() {
            analysisSessionProvider.clearCaches()
        }
    }
}
