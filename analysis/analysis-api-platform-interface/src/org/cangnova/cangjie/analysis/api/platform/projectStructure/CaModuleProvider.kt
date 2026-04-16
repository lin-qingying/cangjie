package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * 平台模块图服务。
 *
 * 相比 [CaProjectStructureProvider]，这个接口更聚焦“模块图本身”：
 * 1. 枚举当前平台可见模块；
 * 2. 按稳定身份查询模块；
 * 3. 让 session cache、失效传播和 low-level resolve 共用同一份结构快照。
 */
interface CaModuleProvider {
    /**
     * 当前平台模块图的一致性快照。
     */
    val snapshot: CaProjectStructureSnapshot

    val allModules: List<CaModule>
        get() = snapshot.allModules

    val resolvableModules: List<CaModule>
        get() = snapshot.allResolvableModules

    val sourceLikeModules: List<CaModule>
        get() = snapshot.allSourceLikeModules

    /**
     * 按稳定模块名查询模块。
     *
     * 未提供 [CaModule.stableModuleName] 的模块不会参与该索引。
     */
    fun getModuleByStableName(stableModuleName: String): CaModule? =
        snapshot.getModuleByStableName(stableModuleName)

    /**
     * 按调试描述查询模块。
     *
     * 该入口主要用于调试和兼容过渡；长期身份仍应以稳定模块名为准。
     */
    fun getModuleByDescription(moduleDescription: String): CaModule? =
        snapshot.getModuleByDescription(moduleDescription)

    companion object {
        fun getInstance(project: Project): CaModuleProvider = project.service()
    }
}
