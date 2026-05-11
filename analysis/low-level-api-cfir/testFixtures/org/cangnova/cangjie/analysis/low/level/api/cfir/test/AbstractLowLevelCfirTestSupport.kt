@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.test

import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.psi.CjFile

internal fun CjFile.getResolutionFacadeForTest(): LLResolutionFacade {
    val module = CangJieProjectStructureProvider.getModule(project, this, useSiteModule = null)
    return module.getResolutionFacade(project)
}

internal fun CjFile.getResolvableSessionForTest(): LLCfirResolvableModuleSession {
    val module = CangJieProjectStructureProvider.getModule(project, this, useSiteModule = null)
    val resolutionFacade = module.getResolutionFacade(project)
    return resolutionFacade.sessionProvider.getResolvableSession(module)
}
