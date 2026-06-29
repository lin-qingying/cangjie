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
    /**
     * 返回直接依赖指定模块的模块集合。
     */
    fun getDirectDependents(module: CaModule): Set<CaModule>

    /**
     * 返回直接或间接依赖指定模块的模块集合。
     */
    fun getTransitiveDependents(module: CaModule): Set<CaModule>

    /**
     * 返回通过 refinement/depends-on 关系依赖指定模块的模块集合。
     */
    fun getRefinementDependents(module: CaModule): Set<CaModule>

    @CaPlatformInterface
    companion object {
        /**
         * 获取项目级模块依赖方 provider 服务。
         */
        fun getInstance(project: Project): CangJieModuleDependentsProvider = project.service()
    }
}
