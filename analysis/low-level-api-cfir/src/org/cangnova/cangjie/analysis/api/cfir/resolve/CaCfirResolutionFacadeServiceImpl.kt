package org.cangnova.cangjie.analysis.api.cfir.resolve

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaModule
import java.util.concurrent.ConcurrentHashMap

/**
 * CFIR low-level facade 服务实现。
 *
 * 这一层只保留 facade 级缓存与失效协调，不再直接持有 session 构建、Raw CFIR 构建或 diagnostics 归一化逻辑。
 * 这些职责已经下沉到：
 * - [CaCfirGlobalResolveComponents]
 * - [CaCfirSessionCache]
 * - [CaCfirModuleResolveComponents]
 *
 * 这样 `analysis-api-cfir` 看到的 low-level 入口依旧稳定，但底层基础设施已经具备继续向 Kotlin
 * `low-level-api-fir` 形态演进的骨架。
 */
class CaCfirResolutionFacadeServiceImpl(
    private val project: Project,
) : CaCfirResolutionFacadeService {
    private val facadeCache = ConcurrentHashMap<CaModule, CaCfirResolutionFacade>()
    private val globalResolveComponents by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaCfirGlobalResolveComponents(project)
    }

    override fun getResolutionFacade(module: CaModule): CaCfirResolutionFacade {
        return facadeCache.computeIfAbsent(module, ::createResolutionFacade)
    }

    override fun invalidate(modules: Set<CaModule>) {
        modules.forEach(facadeCache::remove)
        globalResolveComponents.sessionCache.invalidate(modules)
    }

    private fun createResolutionFacade(module: CaModule): CaCfirResolutionFacade {
        require(globalResolveComponents.isResolvableSourceLikeModule(module)) {
            "当前 low-level CFIR facade 只接受可解析的源码形态模块作为 use-site，实际收到: ${module.moduleDescription}"
        }

        val resolutionStrategyProvider = globalResolveComponents.createResolutionStrategyProvider(module)
        val moduleProvider = CaCfirModuleProvider(module)
        val sessionProvider = CaCfirSessionProvider(
            moduleProvider = moduleProvider,
            sessionCache = globalResolveComponents.sessionCache,
            resolutionStrategyProvider = resolutionStrategyProvider,
        )
        val moduleResolveComponents = CaCfirModuleResolveComponents(
            module = module,
            globalResolveComponents = globalResolveComponents,
            moduleProvider = moduleProvider,
            sessionProvider = sessionProvider,
            resolutionStrategyProvider = resolutionStrategyProvider,
        )

        return CaCfirResolutionFacadeImpl(
            useSiteModule = module,
            useSiteFirSession = moduleResolveComponents.session,
            allModules = moduleResolveComponents.allModules,
            cfirFiles = moduleResolveComponents.cfirFiles,
            diagnostics = moduleResolveComponents.diagnostics,
            scopeProvider = moduleResolveComponents.scopeProvider,
            visibleSymbolProvider = moduleResolveComponents.visibleSymbolProvider,
            sourceNavigationProvider = moduleResolveComponents.sourceNavigationProvider,
        )
    }
}
