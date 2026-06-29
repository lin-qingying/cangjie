package org.cangnova.cangjie.analysis.api.cfir.session

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.impl.base.sessions.CaBaseSessionProvider
import org.cangnova.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry
import org.cangnova.cangjie.analysis.api.platform.CangJieAnalysisInWriteActionListener
import org.cangnova.cangjie.analysis.api.platform.analysisMessageBus
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.LLCfirDeclarationModificationService
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLResolutionFacadeService
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionInvalidationListener
import org.cangnova.cangjie.psi.CjElement
import java.util.concurrent.ConcurrentHashMap

/**
 * CFIR Analysis API 会话提供器。
 *
 * 它位于 `analysis-api-cfir` 这一层，只负责：
 * 1. 根据 use-site 元素或模块选择对应 Analysis API session。
 * 2. 维护 Analysis API session 级缓存。
 * 3. 把失效请求同步到底层 `CaCfirResolutionFacadeService`。
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

        val session = cache.getOrPut(useSiteModule) {
            createAnalysisSession(useSiteModule)
        }

        checkSessionValidity(session)
        return session
    }

    /**
     * 进入基于 PSI 元素的分析前发布写动作分析事件。
     */
    override fun beforeEnteringAnalysis(session: CaSession, useSiteElement: CjElement) {
        super.beforeEnteringAnalysis(session, useSiteElement)
        publishEnteringAnalysisInWriteActionIfNeeded()
    }

    /**
     * 进入基于模块的分析前发布写动作分析事件。
     */
    override fun beforeEnteringAnalysis(session: CaSession, useSiteModule: CaModule) {
        super.beforeEnteringAnalysis(session, useSiteModule)
        publishEnteringAnalysisInWriteActionIfNeeded()
    }

    /**
     * 离开基于 PSI 元素的分析后发布写动作分析事件。
     */
    override fun afterLeavingAnalysis(session: CaSession, useSiteElement: CjElement) {
        try {
            super.afterLeavingAnalysis(session, useSiteElement)
        } finally {
            publishAfterLeavingAnalysisInWriteActionIfNeeded()
        }
    }

    /**
     * 离开基于模块的分析后发布写动作分析事件。
     */
    override fun afterLeavingAnalysis(session: CaSession, useSiteModule: CaModule) {
        try {
            super.afterLeavingAnalysis(session, useSiteModule)
        } finally {
            publishAfterLeavingAnalysisInWriteActionIfNeeded()
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
     * 清空当前 provider 管理的全部 session 缓存。
     */
    override fun clearCaches() {
        invalidate(cache.keys.toSet())
    }

    /**
     * provider 释放时清空 session 缓存。
     */
    override fun dispose() {
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
