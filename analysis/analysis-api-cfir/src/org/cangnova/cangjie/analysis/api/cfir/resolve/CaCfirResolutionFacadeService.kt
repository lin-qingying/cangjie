package org.cangnova.cangjie.analysis.api.cfir.resolve

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaModule

/**
 * 解析外观服务。
 *
 * 从 [CaModule] 创建对应的 [CaCfirResolutionFacade]。
 * 平台需要注册此服务的实现。
 * Kotlin 对应文件：`external/kotlin/analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/LLResolutionFacadeService.kt`
 */
interface CaCfirResolutionFacadeService {
    fun getResolutionFacade(module: CaModule): CaCfirResolutionFacade

    companion object {
        fun getInstance(project: Project): CaCfirResolutionFacadeService = project.service()
    }
}

