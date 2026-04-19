package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaPlatformComponent
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * `CangJieModuleDependentsProvider` 对位 Kotlin `KotlinModuleDependentsProvider`。
 *
 * 该服务回答“谁依赖这个模块”，供 session invalidation、sealed inheritors 等路径复用。
 */
@CaPlatformInterface
interface CangJieModuleDependentsProvider : CaPlatformComponent {
    fun getDirectDependents(module: CaModule): Set<CaModule>

    fun getTransitiveDependents(module: CaModule): Set<CaModule>

    fun getRefinementDependents(module: CaModule): Set<CaModule>

    @CaPlatformInterface
    companion object {
        fun getInstance(project: Project): CangJieModuleDependentsProvider = project.service()
    }
}
