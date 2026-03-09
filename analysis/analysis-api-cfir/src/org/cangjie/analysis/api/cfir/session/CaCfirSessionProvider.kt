package org.cangjie.analysis.api.cfir.session

import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.ProgressManager
import org.cangjie.analysis.api.CaModule
import org.cangjie.analysis.api.CaSession
import org.cangjie.analysis.api.cfir.CaCfirSession
import org.cangjie.analysis.api.cfir.resolve.CaCfirResolutionFacadeService
import org.cangjie.analysis.api.impl.base.sessions.CaBaseSessionProvider
import org.cangjie.analysis.api.platform.projectStructure.CaProjectStructureProvider
import org.cangnova.cangjie.psi.CjElement
import java.util.concurrent.ConcurrentHashMap

/**
 * CFIR 实现的会话提供器（对齐 Kotlin 的 KaFirSessionProvider）。
 *
 * 继承 [CaBaseSessionProvider]，负责创建、缓存和管理 [CaCfirSession]。
 *
 * 核心流程：
 * 1. getAnalysisSession(element) → 通过 [CaProjectStructureProvider] 解析模块 → 委托到模块重载
 * 2. getAnalysisSession(module) → 检查取消 → 从缓存获取或创建
 * 3. createAnalysisSession(module) → 获取 resolution facade → 创建 token → 构建 session
 */
class CaCfirSessionProvider(project: Project) : CaBaseSessionProvider(project) {

    private val cache = ConcurrentHashMap<CaModule, CaCfirSession>()

    private val resolutionFacadeService by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaCfirResolutionFacadeService.getInstance(project)
    }

    private val projectStructureProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaProjectStructureProvider.getInstance(project)
    }

    override fun getAnalysisSession(useSiteElement: CjElement): CaSession {
        val module = projectStructureProvider.getModule(useSiteElement, useSiteModule = null)
        return getAnalysisSession(module)
    }

    override fun getAnalysisSession(useSiteModule: CaModule): CaSession {
        ProgressManager.checkCanceled()

        val session = cache.getOrPut(useSiteModule) {
            createAnalysisSession(useSiteModule)
        }

        checkSessionValidity(session)
        return session
    }

    private fun createAnalysisSession(useSiteModule: CaModule): CaCfirSession {
        val resolutionFacade = resolutionFacadeService.getResolutionFacade(useSiteModule)
        val token = tokenFactory.create(project)
        return CaCfirSession.create(
            project = project,
            resolutionFacade = resolutionFacade,
            token = token,
        )
    }

    private fun checkSessionValidity(session: CaCfirSession) {
        require(session.token.isValid()) {
            "An analysis session acquired via `getAnalysisSession` must be valid."
        }
    }

    override fun beforeEnteringAnalysis(session: CaSession, useSiteElement: CjElement) {
        super.beforeEnteringAnalysis(session, useSiteElement)
    }

    override fun beforeEnteringAnalysis(session: CaSession, useSiteModule: CaModule) {
        super.beforeEnteringAnalysis(session, useSiteModule)
    }

    override fun afterLeavingAnalysis(session: CaSession, useSiteElement: CjElement) {
        super.afterLeavingAnalysis(session, useSiteElement)
    }

    override fun afterLeavingAnalysis(session: CaSession, useSiteModule: CaModule) {
        super.afterLeavingAnalysis(session, useSiteModule)
    }

    /**
     * 使指定模块的 session 缓存失效。
     * 当模块发生变化（源码修改、依赖变化等）时调用。
     */
    fun invalidate(modules: Set<CaModule>) {
        modules.forEach { cache.remove(it) }
    }

    override fun clearCaches() {
        cache.clear()
    }

    override fun dispose() {
        clearCaches()
    }
}
