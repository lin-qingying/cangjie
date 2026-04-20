package org.cangnova.cangjie.analysis.api.platform.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * 平台 project-structure 的一致性快照。
 *
 * Analysis API 在同一轮分析中通常会同时读取：
 * 1. 全部模块图；
 * 2. 可解析模块集合；
 * 3. source-like 模块集合；
 * 4. 源文件视图。
 *
 * 这些信息必须来自同一份结构事实，而不是由调用方分别访问多个属性后再自行拼装。
 */
data class CaProjectStructureSnapshot(
    val allModules: List<CaModule>,
    val allResolvableModules: List<CaModule>,
    val allSourceLikeModules: List<CaModule>,
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

    fun getModuleByStableName(stableModuleName: String): CaModule? {
        return modulesByStableName[stableModuleName]
    }

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
 * 该服务负责把 PSI 元素和文件系统项映射到 Analysis API 模块，
 * 同时暴露当前平台的统一模块图快照。
 */
interface CangJieProjectStructureProvider {
    fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule

    /**
     * 直接按文件系统项查询 use-site module。
     *
     * 平台实现可以覆写该入口以复用本地缓存；
     * 默认行为退化为元素查询。
     */
    fun getModule(item: PsiFileSystemItem, useSiteModule: CaModule? = null): CaModule =
        getModule(item as PsiElement, useSiteModule)

    /**
     * 当前平台 project-structure 的一致性快照。
     */
    val snapshot: CaProjectStructureSnapshot

    val allModules: List<CaModule>
        get() = snapshot.allModules

    val allResolvableModules: List<CaModule>
        get() = snapshot.allResolvableModules

    val allSourceLikeModules: List<CaModule>
        get() = snapshot.allSourceLikeModules

    val allSourceFiles: List<PsiFileSystemItem>
        get() = snapshot.allSourceFiles

    companion object {
        fun getInstance(project: Project): CangJieProjectStructureProvider = project.service()

        fun getModule(project: Project, element: PsiElement, useSiteModule: CaModule? = null): CaModule =
            getInstance(project).getModule(element, useSiteModule)
    }
}
