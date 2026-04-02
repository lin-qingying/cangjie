package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaModule

/**
 * 平台模块提供器。
 *
 * 相比 [CaProjectStructureProvider]，该接口更聚焦模块枚举、依赖图查询和直接查找，
 * 便于会话缓存、失效传播和测试框架在不依赖 PSI 的前提下构建模块视图。
 */
interface CaModuleProvider {
    val allModules: List<CaModule>

    fun getModuleByDescription(moduleDescription: String): CaModule? =
        allModules.firstOrNull { it.moduleDescription == moduleDescription }

    companion object {
        fun getInstance(project: Project): CaModuleProvider = project.service()
    }
}
