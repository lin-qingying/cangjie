package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaPlatformComponent
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * `CaModule.contentScope` 的平台扩展点。
 */
@CaPlatformInterface
interface CaContentScopeRefiner : CaPlatformComponent {
    /**
     * 返回用于扩张 [CaModule.baseContentScope] 的额外作用域。
     */
    fun getEnlargementScopes(module: CaModule): List<GlobalSearchScope> = emptyList()

    /**
     * 返回用于裁剪 [CaModule.baseContentScope] 的额外作用域。
     */
    fun getRestrictionScopes(module: CaModule): List<GlobalSearchScope> = emptyList()

    @CaPlatformInterface
    companion object {
        val EP_NAME: ExtensionPointName<CaContentScopeRefiner> =
            ExtensionPointName("org.cangnova.cangjie.cangjieContentScopeRefiner")

        fun getRefiners(project: Project): List<CaContentScopeRefiner> = EP_NAME.getExtensionList(project)
    }
}
