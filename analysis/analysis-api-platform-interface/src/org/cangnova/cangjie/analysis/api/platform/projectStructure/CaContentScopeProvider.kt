package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaEngineService
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * `CaModule.contentScope` 的统一引擎服务。
 */
@CaPlatformInterface
interface CaContentScopeProvider : CaEngineService {
    fun getRefinedContentScope(module: CaModule): GlobalSearchScope

    @CaPlatformInterface
    companion object {
        fun getInstance(project: Project): CaContentScopeProvider = project.service()
    }
}
