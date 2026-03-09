package org.cangjie.analysis.api.cfir.resolve

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangjie.analysis.api.CaModule

/**
 * Resolution facade 服务（对齐 Kotlin 的 LLResolutionFacadeService）。
 *
 * 从 [CaModule] 创建对应的 [CaCfirResolutionFacade]。
 * 平台需要注册此服务的实现。
 */
interface CaCfirResolutionFacadeService {
    fun getResolutionFacade(module: CaModule): CaCfirResolutionFacade

    companion object {
        fun getInstance(project: Project): CaCfirResolutionFacadeService = project.service()
    }
}
