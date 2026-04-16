package org.cangnova.cangjie.analysis.api.cfir.resolve

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * CFIR 低层解析 facade 服务。
 *
 * 它以 `CaModule -> CaCfirResolutionFacade` 的方式提供 session 构建、缓存与重建入口，
 * 是 `analysis-api-cfir` 与 `cfir` 编译器实现之间的唯一低层桥接面。
 */
interface CaCfirResolutionFacadeService {
    fun getResolutionFacade(module: CaModule): CaCfirResolutionFacade

    fun invalidate(modules: Set<CaModule>)

    companion object {
        fun getInstance(project: Project): CaCfirResolutionFacadeService = project.service()
    }
}
