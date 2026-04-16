package org.cangnova.cangjie.analysis.test.services

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureSnapshot

/**
 * Analysis API 测试专用的项目结构提供器。
 *
 * 测试平台严格依赖预先构建好的测试模块图，不做 IDE 平台那种“内容根外兜底”推断。
 * 一旦元素无法映射回测试模块，就直接报错。
 */
class CaTestProjectStructureProvider(
    private val project: Project,
) : CaProjectStructureProvider {
    private val moduleStructure
        get() = CaTestProjectStructureRegistry.get(project)

    /**
     * 测试 project-structure 在单个用例生命周期内是稳定的，
     * 因而可以直接缓存成只读快照。
     */
    private val cachedSnapshot: CaProjectStructureSnapshot by lazy(LazyThreadSafetyMode.NONE) {
        CaProjectStructureSnapshot(
            allModules = moduleStructure.allCaModules,
            allResolvableModules = moduleStructure.allCaModules.filter(CaModule::isResolvable),
            allSourceLikeModules = moduleStructure.allCaModules.filterIsInstance<CaSourceModule>(),
            allSourceFiles = moduleStructure.allCjFiles,
        )
    }

    override val snapshot: CaProjectStructureSnapshot
        get() = cachedSnapshot

    override fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        useSiteModule?.let { return it }

        val containingFile = element.containingFile
            ?: error("Cannot resolve module for PSI element without containing file: $element")
        return moduleStructure.requireModuleByFile(containingFile).caModule
    }
}
