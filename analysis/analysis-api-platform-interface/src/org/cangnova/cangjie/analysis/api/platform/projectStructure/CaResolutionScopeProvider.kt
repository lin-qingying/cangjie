package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaEngineService
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * `CaResolutionScopeProvider` 对位 Kotlin `KaResolutionScopeProvider`。
 *
 * 它返回某个 use-site module 的解析作用域。
 */
@CaPlatformInterface
interface CaResolutionScopeProvider : CaEngineService {
    fun getResolutionScope(module: CaModule): CaResolutionScope

    @CaPlatformInterface
    companion object {
        fun getInstance(project: Project): CaResolutionScopeProvider = project.service()
    }
}
