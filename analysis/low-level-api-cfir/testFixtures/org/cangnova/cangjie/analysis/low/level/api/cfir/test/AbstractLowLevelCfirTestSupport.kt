@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.test

import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.psi.CjFile

/**
 * 返回当前测试文件所属模块的 low-level resolution facade。
 */
internal fun CjFile.getResolutionFacadeForTest(): LLResolutionFacade {
    val module = CangJieProjectStructureProvider.getModule(project, this, useSiteModule = null)
    return module.getResolutionFacade(project)
}

/**
 * 返回当前测试文件所属模块的可解析 low-level CFIR 会话。
 */
internal fun CjFile.getResolvableSessionForTest(): LLCfirResolvableModuleSession {
    val module = CangJieProjectStructureProvider.getModule(project, this, useSiteModule = null)
    val resolutionFacade = module.getResolutionFacade(project)
    return resolutionFacade.sessionProvider.getResolvableSession(module)
}
