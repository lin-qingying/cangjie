package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaModule

/**
 * 项目结构提供器（对齐 Kotlin 的 KotlinProjectStructureProvider）。
 *
 * 负责将 PSI 元素映射到所属的 [CaModule]。
 */
interface CaProjectStructureProvider {
    fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule

    companion object {
        fun getInstance(project: Project): CaProjectStructureProvider = project.service()

        fun getModule(project: Project, element: PsiElement, useSiteModule: CaModule? = null): CaModule =
            getInstance(project).getModule(element, useSiteModule)
    }
}
