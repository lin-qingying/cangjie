package org.cangnova.cangjie.analysis.api.cfir.session

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirResolutionFacadeService
import org.cangnova.cangjie.analysis.api.impl.base.sessions.CaBaseSessionProvider
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureProvider
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
    private val cache = ConcurrentHashMap<CaModule, CaCfirSession>()

    private val resolutionFacadeService by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaCfirResolutionFacadeService.getInstance(project)
    }

    private val projectStructureProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaProjectStructureProvider.getInstance(project)
    }

    override fun getAnalysisSession(useSiteElement: CjElement): CaSession {
        val module = resolveUseSiteModule(useSiteElement)
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

    override fun invalidate(modules: Set<CaModule>) {
        modules.forEach(cache::remove)
        resolutionFacadeService.invalidate(modules)
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

    override fun clearCaches() {
        invalidate(cache.keys.toSet())
    }

    override fun dispose() {
        clearCaches()
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
            "通过 `getAnalysisSession` 获取的 Analysis API session 必须保持有效。"
        }
    }

    /**
     * 元素到 use-site module 的恢复必须始终走平台 project-structure 服务，
     * 保证 CFIR session provider 与平台模块图使用同一份结构事实。
     */
    private fun resolveUseSiteModule(useSiteElement: CjElement): CaModule {
        return projectStructureProvider.getModule(useSiteElement, useSiteModule = null)
    }
}
