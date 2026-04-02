package org.cangnova.cangjie.analysis.test.services

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureProvider

/**
 * Analysis API 测试专用的项目结构提供器。
 *
 * 它严格基于测试框架预先构造好的模块图工作。
 * 一旦元素无法映射到测试模块，就直接报错，避免像生产 IDE 平台那样退回到模糊的“内容根外模块”。
 */
class CaTestProjectStructureProvider(
    private val project: Project,
) : CaProjectStructureProvider {
    private val moduleStructure
        get() = CaTestProjectStructureRegistry.get(project)

    override fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        val containingFile = element.containingFile
            ?: error("Cannot resolve module for PSI element without containing file: $element")
        val virtualFile = containingFile.virtualFile

        return moduleStructure.mainModules.firstOrNull { testModule ->
            when {
                virtualFile != null && testModule.caModule.contentScope.contains(virtualFile) -> true
                else -> testModule.psiFiles.any { it == containingFile }
            }
        }?.caModule
            ?: error("Cannot find CaModule for `${containingFile.name}` in Analysis API test project structure.")
    }

    override val allModules: List<CaModule>
        get() = moduleStructure.allCaModules

    override val allSourceFiles: List<PsiFileSystemItem>
        get() = moduleStructure.allCjFiles
}
