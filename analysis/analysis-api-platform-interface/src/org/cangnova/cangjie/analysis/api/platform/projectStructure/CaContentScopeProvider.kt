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
    /**
     * 返回模块经过平台修正后的内容搜索范围。
     */
    fun getRefinedContentScope(module: CaModule): GlobalSearchScope

    @CaPlatformInterface
    companion object {
        /**
         * 获取项目级内容范围 provider 服务。
         */
        fun getInstance(project: Project): CaContentScopeProvider = project.service()
    }
}
