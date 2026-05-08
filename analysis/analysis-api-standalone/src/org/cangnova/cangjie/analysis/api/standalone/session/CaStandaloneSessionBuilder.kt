@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.session

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureSnapshot
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.platform.lifetime.CaAlwaysAccessibleLifetimeTokenFactory
import org.cangnova.cangjie.analysis.api.platform.lifetime.CaLifetimeTokenFactory
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionOptions
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.analysis.api.standalone.base.permissions.CaStandaloneAnalysisPermissionOptions
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandalonePlatformState
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandalonePlatformSettings
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiSimpleServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.CaStandaloneProjectStructure
import org.cangnova.cangjie.analysis.internal.projectStructure.collectReachableModules
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

/**
 * Standalone Analysis API 上下文构建入口。
 *
 * 它不替代 IntelliJ service 容器，而是把 standalone 模块图提升为统一上下文：
 * 1. 先按根模块闭包构造完整模块图
 * 2. 再把该模块图安装到平台状态服务
 * 3. 最终复用同一套 Analysis API / CFIR 服务完成分析
 */
class CaStandaloneSessionBuilder(
    private val project: Project,
) {
    /**
     * 从单个入口模块的可达闭包构建 standalone 分析上下文。
     */
    fun build(rootModule: CaModule): CaStandaloneAnalysisContext {
        return build(rootModule.collectReachableModules())
    }

    /**
     * 从多个入口模块的并集闭包构建 standalone 分析上下文。
     *
     * Standalone 调用方经常需要同时暴露 source、library、script、dangling module，
     * 这里统一负责闭包扩张和去重，不把模块图拼装责任散给外层调用方。
     */
    fun build(modules: Collection<CaModule>): CaStandaloneAnalysisContext {
        val projectStructure = CaStandaloneProjectStructure(modules.collectReachableModules())
        CaStandalonePlatformState.getInstance(project).install(projectStructure)

        return CaStandaloneAnalysisContext(
            project = project,
            projectStructure = projectStructure,
        )
    }

    /**
     * 便捷的可变参数入口。
     */
    fun build(vararg modules: CaModule): CaStandaloneAnalysisContext =
        build(modules.asList())
}

/**
 * Standalone Analysis API 分析上下文。
 *
 * 该对象只承载“当前已安装的 standalone 模块图”这一视图，
 * 真正的 session 创建与缓存仍由统一的 [CaSessionProvider] 负责。
 */
class CaStandaloneAnalysisContext(
    val project: Project,
    val projectStructure: CaStandaloneProjectStructure,
) {
    /**
     * 当前 standalone project-structure 的一致性快照。
     */
    val snapshot: CaProjectStructureSnapshot
        get() = projectStructure.snapshot

    /**
     * 当前 standalone 上下文可见的全部模块。
     */
    val allModules: List<CaModule>
        get() = projectStructure.allModules

    /**
     * 按模块执行分析。
     */
    fun <R> analyze(useSiteModule: CaModule, action: CaSession.() -> R): R {
        return CaSessionProvider.getInstance(project).analyze(useSiteModule, action)
    }

    /**
     * 按元素执行分析。
     *
     * 如果调用方已经知道 use-site module，可通过 [preferredUseSiteModule] 直接约束；
     * 否则由 standalone project-structure 统一解析所属模块。
     */
    fun <R> analyze(
        useSiteElement: CjElement,
        preferredUseSiteModule: CaModule? = null,
        action: CaSession.() -> R,
    ): R {
        val useSiteModule = getUseSiteModule(useSiteElement, preferredUseSiteModule)
        return analyze(useSiteModule, action)
    }

    /**
     * 批量分析多个文件。
     *
     * 所有文件共享同一份 standalone 模块图，
     * 但每个文件仍按自身的 use-site module 进入分析。
     */
    fun <R> analyzeFiles(
        files: Collection<CjFile>,
        preferredUseSiteModule: CaModule? = null,
        action: CaSession.(CjFile) -> R,
    ): List<R> {
        return analyzeGroupedByUseSiteModule(
            items = files,
            preferredUseSiteModule = preferredUseSiteModule,
            resolveElement = { file -> file },
        ) { file ->
            action(file)
        }
    }

    /**
     * 按元素批量分析，并按 use-site module 分组复用 session。
     *
     * 这比“每个元素单独 analyze 一次”更符合 Analysis API 的会话模型：
     * 同一 use-site module 下的元素应共享同一份语义快照与缓存边界。
     */
    fun <R> analyzeElements(
        elements: Collection<CjElement>,
        preferredUseSiteModule: CaModule? = null,
        action: CaSession.(CjElement) -> R,
    ): List<R> {
        return analyzeGroupedByUseSiteModule(
            items = elements,
            preferredUseSiteModule = preferredUseSiteModule,
            resolveElement = { it },
        ) { element ->
            action(element)
        }
    }

    /**
     * Standalone 的批量分析入口需要同时满足两个约束：
     * 1. 复用 [CaSessionProvider] 的批量模块分析边界
     * 2. 允许调用方显式指定 preferred use-site module
     *
     * 因此这里不直接退回 `analyze(element)`，而是在 standalone 上下文内统一完成
     * “元素 -> use-site module -> session 批处理”的映射。
     */
    private fun <T, R> analyzeGroupedByUseSiteModule(
        items: Collection<T>,
        preferredUseSiteModule: CaModule?,
        resolveElement: (T) -> CjElement,
        action: CaSession.(T) -> R,
    ): List<R> {
        if (items.isEmpty()) return emptyList()

        val sessionProvider = CaSessionProvider.getInstance(project)
        val groupedItems = items.withIndex().groupBy(
            keySelector = { indexedItem ->
                getUseSiteModule(resolveElement(indexedItem.value), preferredUseSiteModule)
            },
            valueTransform = { indexedItem ->
                indexedItem.index to indexedItem.value
            },
        )

        val results = arrayOfNulls<Any?>(items.size)
        sessionProvider.analyzeModules(groupedItems.keys) { useSiteModule ->
            groupedItems.getValue(useSiteModule).forEach { (index, item) ->
                results[index] = action(item)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return results.map { it as R }
    }

    /**
     * 查询 PSI 元素在当前 standalone 模块图中的 use-site module。
     */
    fun getUseSiteModule(
        element: PsiElement,
        preferredUseSiteModule: CaModule? = null,
    ): CaModule = projectStructure.getModule(element, preferredUseSiteModule)

    /**
     * 按稳定模块名查询当前 standalone 上下文中的模块。
     */
    fun findModuleByStableName(stableModuleName: String): CaModule? {
        return projectStructure.getModuleByStableName(stableModuleName)
    }

    /**
     * 使指定模块及其相关 session 缓存失效。
     */
    fun invalidate(vararg modules: CaModule) {
        projectStructure.invalidate(modules.toSet())
    }

    /**
     * 使整张 standalone 模块图失效。
     */
    fun invalidateAll() {
        projectStructure.invalidate(allModules.toSet())
    }
}

/**
 * 注册 standalone 生产态与 standalone 测试共用的宿主服务。
 *
 * 对齐 Kotlin `StandaloneSessionServiceRegistrar` 的文件内归属：
 * - 应用级注册 standalone 权限选项；
 * - 项目级注册 always-accessible lifetime token factory 与 standalone platform settings。
 */
internal object CaStandaloneSessionServiceRegistrar : AnalysisApiSimpleServiceRegistrar() {
    override fun registerApplicationServices(application: MockApplication) {
        application.apply {
            registerService(CaAnalysisPermissionOptions::class.java, CaStandaloneAnalysisPermissionOptions::class.java)
        }
    }

    @OptIn(CaPlatformInterface::class)
    override fun registerProjectServices(project: MockProject) {
        project.apply {
            registerService(CaLifetimeTokenFactory::class.java, CaAlwaysAccessibleLifetimeTokenFactory::class.java)
            registerService(CaPlatformSettings::class.java, CaStandalonePlatformSettings::class.java)
        }
    }
}
