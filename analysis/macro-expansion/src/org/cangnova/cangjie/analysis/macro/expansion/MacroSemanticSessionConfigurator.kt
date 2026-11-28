package org.cangnova.cangjie.analysis.macro.expansion

import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionConfigurator

/**
 * 宏语义展开的**触发点**。
 *
 * 由本模块 XML 注册到 `llCfirSessionConfigurator` 扩展点：任何宿主只要加载了
 * `analysis:macro-expansion` 的插件描述符，就自动获得触发能力；是否真正构造
 * 只取决于该工程是否注册了 [MacroSemanticExpansionEnvironment]。
 *
 * 触发点属于 Analysis 层框架，宿主不得复制"何时构造、何时发布"的编排。
 */
class MacroSemanticSessionConfigurator : LLCfirSessionConfigurator {
    override fun configure(session: LLCfirSession) {
        val resolvableSession = session as? LLCfirResolvableModuleSession ?: return
        if (session.caModule !is CaSourceModule) return
        if (!session.isValid || session.project.isDisposed) return
        if (MacroSemanticExpansionEnvironment.getInstance(session.project) == null) return
        MacroSemanticExpansionService.getInstance(session.project).request(resolvableSession)
    }
}
