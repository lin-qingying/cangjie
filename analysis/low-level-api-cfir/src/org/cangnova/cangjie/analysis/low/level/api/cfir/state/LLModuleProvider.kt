

package org.cangnova.cangjie.analysis.low.level.api.cfir.state

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.platform.projectStructure.KotlinProjectStructureProvider

class LLModuleProvider(val useSiteModule: CaModule) {
    /**
     * Returns a [CaModule] for a given [element] in context of the current session.
     *
     * See [KotlinProjectStructureProvider] for more information on contextual modules.
     */
    fun getModule(element: PsiElement): CaModule {
        return KotlinProjectStructureProvider.getModule(useSiteModule.project, element, useSiteModule)
    }
}