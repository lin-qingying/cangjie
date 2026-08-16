package org.cangnova.cangjie.analysis.api.platform.projectStructure

import org.cangnova.cangjie.LanguageVersionSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule

/**
 * Analysis API 项目结构快照。
 */
@CaPlatformInterface
data class CaProjectStructureSnapshot(
    /**
     * 快照中的所有模块。
     */
    val allModules: List<CaModule>,
    /**
     * 快照中可解析的模块集合。
     */
    val allResolvableModules: List<CaModule>,
    /**
     * 快照中的源码类模块集合。
     */
    val allSourceLikeModules: List<CaModule>,
    /**
     * 快照覆盖的所有源文件。
     */
    val allSourceFiles: List<PsiFileSystemItem>,
) {
    /**
     * 稳定模块名索引。
     *
     * 稳定模块名会参与 session 划分、缓存命中和结构调试，
     * 因此必须绑定到同一份快照上。
     */
    private val modulesByStableName: Map<String, CaModule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildMap {
            allModules.forEach { module ->
                val stableName = module.stableModuleName ?: return@forEach
                val previous = put(stableName, module)
                check(previous == null || previous === module) {
                    "Analysis API project-structure 快照中出现重复稳定模块名 `$stableName`"
                }
            }
        }
    }

    /**
     * 仅用于调试的模块描述索引。
     *
     * `moduleDescription` 不是长期身份，因此这里只作为诊断辅助入口。
     */
    private val modulesByDescription: Map<String, List<CaModule>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        allModules.groupBy(CaModule::moduleDescription)
    }

    /**
     * 按稳定模块名查询模块。
     */
    fun getModuleByStableName(stableModuleName: String): CaModule? {
        return modulesByStableName[stableModuleName]
    }

    /**
     * 按稳定模块名查询模块，不存在时抛出错误。
     */
    fun requireModuleByStableName(stableModuleName: String): CaModule {
        return getModuleByStableName(stableModuleName)
            ?: error("Analysis API project-structure 快照中不存在稳定模块名 `$stableModuleName`")
    }

    /**
     * 按描述查询模块。
     *
     * 描述允许重复，因此这里明确拒绝歧义结果，而不是偷偷取第一个模块。
     */
    fun getModuleByDescription(moduleDescription: String): CaModule? {
        val matches = modulesByDescription[moduleDescription].orEmpty()
        return when (matches.size) {
            0 -> null
            1 -> matches.single()
            else -> error(
                "Analysis API project-structure 快照中存在多个描述为 `$moduleDescription` 的模块：" +
                    matches.joinToString { module -> module.stableModuleName ?: module.moduleDescription },
            )
        }
    }
}

/**
 * 平台 project-structure 服务。
 *
 * 对齐 Kotlin `KotlinProjectStructureProvider`：
 * 1. 把 PSI 元素映射为 Analysis API use-site module；
 * 2. 提供平台侧的 implementing-modules 查询；
 * 3. 提供 project-global language version settings。
 */
@CaPlatformInterface
interface CangJieProjectStructureProvider {
    /**
     * 返回 PSI 元素所属的 use-site module。
     */
    fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule

    /**
     * 返回实现指定模块的模块列表。
     */
    fun getImplementingModules(module: CaModule): List<CaModule>

    /**
     * Project-global [LanguageVersionSettings] for source modules lacking explicit settings
     * such as [CaNotUnderContentRootModule].
     */
    val globalLanguageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettingsImpl.DEFAULT

    /**
     * Project-global [LanguageVersionSettings] for library-related modules.
     */
    val libraryLanguageVersionSettings: LanguageVersionSettings
        get() = globalLanguageVersionSettings

    companion object {
        /**
         * 获取项目级 project-structure provider 服务。
         */
        fun getInstance(project: Project): CangJieProjectStructureProvider = project.service()

        /**
         * 通过项目级 provider 查询 PSI 元素所属模块。
         */
        fun getModule(project: Project, element: PsiElement, useSiteModule: CaModule? = null): CaModule =
            getInstance(project).getModule(element, useSiteModule)
    }
}
