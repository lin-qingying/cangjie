package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.analysis.api.CaModule

/**
 * 项目结构提供器。
 *
 * 对齐 Kotlin `KotlinProjectStructureProvider` 的职责，负责把 PSI 元素映射到 Analysis API 模块，
 * 并暴露当前项目结构中可见的模块与源码文件集合。
 */
interface CaProjectStructureProvider {
    fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule

    /**
     * 当前项目中 Analysis API 可见的全部模块。
     */
    val allModules: List<CaModule>
        get() = emptyList()

    /**
     * 当前项目结构暴露的全部源码文件。
     */
    val allSourceFiles: List<PsiFileSystemItem>
        get() = emptyList()

    companion object {
        fun getInstance(project: Project): CaProjectStructureProvider = project.service()

        fun getModule(project: Project, element: PsiElement, useSiteModule: CaModule? = null): CaModule =
            getInstance(project).getModule(element, useSiteModule)
    }
}
