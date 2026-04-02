package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.analysis.api.CaModule

/**
 * 项目结构提供器。
 *
 * 该接口负责把 PSI、文件与平台项目模型映射到 Analysis API 的模块世界，
 * 是所有 use-site 分析入口的核心桥梁。
 */
interface CaProjectStructureProvider {
    fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule

    /**
     * 当前项目中对 Analysis API 可见的全部模块。
     */
    val allModules: List<CaModule>
        get() = emptyList()

    /**
     * 当前项目结构暴露给 Analysis API 的全部源文件根。
     */
    val allSourceFiles: List<PsiFileSystemItem>
        get() = emptyList()

    companion object {
        fun getInstance(project: Project): CaProjectStructureProvider = project.service()

        fun getModule(project: Project, element: PsiElement, useSiteModule: CaModule? = null): CaModule =
            getInstance(project).getModule(element, useSiteModule)
    }
}
