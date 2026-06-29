
@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.state

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider

/**
 * 基于 use-site module 解析 PSI 所属 analysis module 的 provider。
 */
class LLModuleProvider(
    /**
     * 当前查询的 use-site module。
     */
    val useSiteModule: CaModule
) {
    /**
     * Returns a [CaModule] for a given [element] in context of the current session.
     *
     * See [CangJieProjectStructureProvider] for more information on contextual modules.
     */
    fun getModule(element: PsiElement): CaModule {
        return CangJieProjectStructureProvider.getModule(useSiteModule.project, element, useSiteModule)
    }
}
