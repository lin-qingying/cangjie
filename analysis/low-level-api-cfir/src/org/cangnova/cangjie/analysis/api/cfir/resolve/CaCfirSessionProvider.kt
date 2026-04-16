package org.cangnova.cangjie.analysis.api.cfir.resolve

import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * low-level session 提供器。
 *
 * 它是 facade service 与底层 session cache 之间的薄适配层，职责与 Kotlin `LLSessionProvider` 对齐：
 * 1. 将 use-site 的解析策略绑定到具体 session 获取流程；
 * 2. 统一保证同一分析快照中所有模块都通过同一套 strategy provider 进入 cache；
 * 3. 为后续 low-level scope/resolver 组件提供稳定的 `module -> session` 查询入口。
 */
internal class CaCfirSessionProvider(
    private val moduleProvider: CaCfirModuleProvider,
    private val sessionCache: CaCfirSessionCache,
    private val resolutionStrategyProvider: CaCfirModuleResolutionStrategyProvider,
) {
    fun getSession(module: CaModule): CfirSession {
        require(module in moduleProvider.allModules || module == moduleProvider.useSiteModule) {
            "模块 `${module.moduleDescription}` 不在当前 use-site `${moduleProvider.useSiteModule.moduleDescription}` 的 low-level 可见模块闭包中。"
        }
        return sessionCache.getSession(module, resolutionStrategyProvider)
    }
}
