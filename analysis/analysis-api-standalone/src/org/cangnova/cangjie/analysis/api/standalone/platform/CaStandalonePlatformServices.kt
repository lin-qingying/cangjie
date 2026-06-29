@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.platform

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.PluginStructureProvider
import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureSnapshot
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScope
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScopeProvider
import org.cangnova.cangjie.analysis.api.platform.restrictedAnalysis.CaRestrictedAnalysisService
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.CaStandaloneProjectStructure
import java.util.concurrent.atomic.AtomicLong

/**
 * Standalone 平台状态服务。
 *
 * Standalone 与 IDE 的关键区别不在于 Analysis API 实现本身，而在于：
 * - 工程结构并非来自 IDE workspace model；
 * - 调用方会动态装入一组模块图并据此分析；
 * - 同一个 `Project` 容器需要随着新的模块图切换 project-structure 视图。
 *
 * 因此这里不把 [CaStandaloneProjectStructure] 直接注册成固定 service，
 * 而是由状态服务统一托管“当前激活的 standalone 模块图”，并同时实现：
 * - [CangJieProjectStructureProvider]
 * - [CaModuleProvider]
 * - [CaModificationTracker]
 * - [CaSessionInvalidationService]
 *
 * 这样 builder、session provider 与平台 service 容器之间始终只存在一条一致的装配链。
 */
class CaStandalonePlatformState(
    /**
     * 当前 standalone Analysis API 服务所在的 project 容器。
     */
    private val project: Project,
) {
    /**
     * 每次安装或显式失效 project structure 时递增的平台结构版本。
     */
    private val structureVersion = AtomicLong(0)

    /**
     * 当前激活的 standalone project structure。
     */
    @Volatile
    private var activeProjectStructure: CaStandaloneProjectStructure? = null

    /**
     * 当前模块图中的全部模块。
     */
    val allModules: List<CaModule>
        get() = requireProjectStructure().allModules

    /**
     * 当前模块图中的全部源码文件系统项。
     */
    val allSourceFiles: List<PsiFileSystemItem>
        get() = requireProjectStructure().allSourceFiles

    /**
     * 当前模块图中可参与解析的模块。
     */
    val allResolvableModules: List<CaModule>
        get() = requireProjectStructure().resolvableModules

    /**
     * 当前模块图中的 source-like 模块。
     */
    val allSourceLikeModules: List<CaModule>
        get() = requireProjectStructure().sourceLikeModules

    /**
     * 当前 project structure 快照。
     */
    val snapshot: CaProjectStructureSnapshot
        get() = requireProjectStructure().snapshot

    /**
     * 平台结构版本与当前 project structure 修改计数合成后的全局修改计数。
     */
    val modificationCount: Long
        get() = structureVersion.get() + requireProjectStructure().modificationCount

    /**
     * 安装新的 standalone 模块图。
     *
     * 这里显式把“模块图切换”视为平台级失效事件：
     * - 旧模块图中的 session 缓存必须失效；
     * - 新模块图必须立即成为 `CangJieProjectStructureProvider` 的可见视图；
     * - modification 计数需要发生跳变，确保 session provider 不会复用旧快照。
     */
    fun install(projectStructure: CaStandaloneProjectStructure) {
        val previousStructure = activeProjectStructure
        activeProjectStructure = projectStructure
        structureVersion.incrementAndGet()

        val invalidatedModules = buildSet {
            previousStructure?.allModules?.let(::addAll)
            addAll(projectStructure.allModules)
        }

        if (invalidatedModules.isNotEmpty()) {
            invalidateSessionProvider(invalidatedModules)
        }
    }

    /**
     * 返回指定 PSI 元素所属模块。
     */
    fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        return requireProjectStructure().getModule(element, useSiteModule)
    }

    /**
     * 返回指定模块的合成修改计数。
     */
    fun getModuleModificationCount(module: CaModule): Long {
        return structureVersion.get() + requireProjectStructure().getModuleModificationCount(module)
    }

    /**
     * 根据稳定模块名查找模块。
     */
    fun getModuleByStableName(stableModuleName: String): CaModule? {
        return snapshot.getModuleByStableName(stableModuleName)
    }

    /**
     * 返回指定模块的解析作用域。
     */
    fun getResolutionScope(module: CaModule): CaResolutionScope {
        return requireProjectStructure().getResolutionScope(module)
    }

    /**
     * 失效指定模块并推进 standalone 结构版本。
     */
    fun invalidate(modules: Set<CaModule>) {
        val projectStructure = requireProjectStructure()
        projectStructure.invalidate(modules)
        structureVersion.incrementAndGet()
    }

    /**
     * 返回已安装的 project structure，未安装时抛出明确错误。
     */
    private fun requireProjectStructure(): CaStandaloneProjectStructure {
        return activeProjectStructure
            ?: error("Standalone Analysis API 尚未安装 project structure，无法执行平台级查询。")
    }

    /**
     * 将模块失效事件转发给 session provider。
     */
    private fun invalidateSessionProvider(modules: Set<CaModule>) {
        val invalidationService = project.getService(CaSessionProvider::class.java) as? CaSessionInvalidationService
        if (invalidationService != null) {
            invalidationService.invalidate(modules)
        }
    }

    companion object {
        /**
         * 从 project service 容器中取得 standalone 平台状态服务。
         */
        fun getInstance(project: Project): CaStandalonePlatformState = project.service()
    }
}

/**
 * Standalone 平台的 project-structure 服务委托。
 */
class CaStandaloneProjectStructureProvider(
    /**
     * 提供 [CaStandalonePlatformState] 的 project 容器。
     */
    private val project: Project,
) : CangJieProjectStructureProvider {
    /**
     * 当前 standalone 平台状态。
     */
    private val state: CaStandalonePlatformState
        get() = CaStandalonePlatformState.getInstance(project)

    /**
     * 返回指定 PSI 元素所属模块。
     */
    override fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        return state.getModule(element, useSiteModule)
    }

    /**
     * 返回直接依赖指定模块的实现模块。
     */
    override fun getImplementingModules(module: CaModule): List<CaModule> {
        return state.allModules.filter { module in it.directDependsOnDependencies }
    }

    /**
     * standalone 全局语言版本设置。
     */
    override val globalLanguageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettings.DEFAULT
}

/**
 * Standalone 平台的 module-provider 服务委托。
 */
class CaStandaloneModuleProvider(
    /**
     * 提供 [CaStandalonePlatformState] 的 project 容器。
     */
    private val project: Project,
) : CaModuleProvider {
    /**
     * 当前 standalone 平台状态。
     */
    private val state: CaStandalonePlatformState
        get() = CaStandalonePlatformState.getInstance(project)

    /**
     * 当前 project structure 快照。
     */
    override val snapshot: CaProjectStructureSnapshot
        get() = state.snapshot

    /**
     * 根据稳定模块名查找模块。
     */
    override fun getModuleByStableName(stableModuleName: String): CaModule? {
        return state.getModuleByStableName(stableModuleName)
    }
}

/**
 * Standalone 平台的 modification-tracker 服务委托。
 */
class CaStandaloneModificationTracker(
    /**
     * 提供 [CaStandalonePlatformState] 的 project 容器。
     */
    private val project: Project,
) : CaModificationTracker {
    /**
     * 当前 standalone 平台状态。
     */
    private val state: CaStandalonePlatformState
        get() = CaStandalonePlatformState.getInstance(project)

    /**
     * 当前全局修改计数。
     */
    override val modificationCount: Long
        get() = state.modificationCount

    /**
     * 返回指定模块的修改计数。
     */
    override fun getModuleModificationCount(module: CaModule): Long {
        return state.getModuleModificationCount(module)
    }
}

/**
 * Standalone 平台的 session 失效服务委托。
 */
class CaStandaloneSessionInvalidationService(
    /**
     * 提供 [CaStandalonePlatformState] 的 project 容器。
     */
    private val project: Project,
) : CaSessionInvalidationService {
    /**
     * 当前 standalone 平台状态。
     */
    private val state: CaStandalonePlatformState
        get() = CaStandalonePlatformState.getInstance(project)

    /**
     * 失效指定模块对应的 session。
     */
    override fun invalidate(modules: Set<CaModule>) {
        state.invalidate(modules)
    }
}

/**
 * Standalone 平台的 resolution-scope 服务委托。
 *
 * standalone 的解析作用域必须绑定当前激活模块图，
 * 因而统一经由 [CaStandalonePlatformState] 转发到当前 [CaStandaloneProjectStructure]。
 */
class CaStandaloneResolutionScopeProvider(
    /**
     * 提供 [CaStandalonePlatformState] 的 project 容器。
     */
    private val project: Project,
) : CaResolutionScopeProvider {
    /**
     * 当前 standalone 平台状态。
     */
    private val state: CaStandalonePlatformState
        get() = CaStandalonePlatformState.getInstance(project)

    /**
     * 返回指定模块的解析作用域。
     */
    override fun getResolutionScope(module: CaModule): CaResolutionScope {
        return state.getResolutionScope(module)
    }
}

/**
 * Standalone 平台的受限分析服务。
 *
 * Standalone 宿主没有 IDE 的 dumb mode、索引窗口或交互式写动作限制，
 * 因而默认处于“非受限分析”状态。仍然单独建模该服务，是为了让
 * `CaBaseSessionProvider` 与 IDE/LSP 保持完全一致的平台协作协议。
 */
class CaStandaloneRestrictedAnalysisService : CaRestrictedAnalysisService {
    /**
     * standalone 默认不处于 restricted analysis 状态。
     */
    override val isAnalysisRestricted: Boolean
        get() = false

    /**
     * 即使查询该标志，standalone 也允许继续分析。
     */
    override val isRestrictedAnalysisAllowed: Boolean
        get() = true

    /**
     * standalone 不应进入拒绝受限分析的路径。
     */
    override fun rejectRestrictedAnalysis(): Nothing {
        error("Standalone 平台当前未启用 restricted analysis，不应调用 rejectRestrictedAnalysis().")
    }
}

/**
 * Standalone 平台设置。
 *
 * Standalone 宿主常用于批处理分析、测试和 CLI 工具，
 * 允许直接以库模块作为 use-site 进入分析，以便宿主按自己的模块图组织调用。
 */
class CaStandalonePlatformSettings : CaPlatformSettings {
    /**
     * standalone 反序列化声明默认来自 binary。
     */
    override val deserializedDeclarationsOrigin: CaDeserializedDeclarationsOrigin
        get() = CaDeserializedDeclarationsOrigin.BINARIES

    /**
     * standalone 默认不允许把 library module 作为 use-site 分析入口。
     */
    override val allowUseSiteLibraryModuleAnalysis: Boolean
        get() = false
}

/**
 * Standalone Headless 容器的 Analysis API 服务装配入口。
 *
 * 它与 LSP service registrar 的职责一致：
 * 1. 从 XML 恢复 Analysis API / CFIR / references / standalone 平台服务；
 * 2. 让 standalone 调用方无需手写 project service 注册代码；
 * 3. 保证生产环境与测试环境使用同一套 service 装配协议。
 */
object CaStandaloneAnalysisApiServiceRegistrar {
    /**
     * standalone headless 容器启动时必须加载的 Analysis API 插件 XML 列表。
     */
    private val analysisPluginXmls = listOf(
        "META-INF/analysis-api/cangjie-analysis-api-cfir.xml",
        "META-INF/analysis-api/cangjie-cj-references.xml",
        "META-INF/analysis-api/cangjie-analysis-api-standalone.xml",
    )

    /**
     * 向 mock application 与 mock project 注册 standalone Analysis API 所需的应用级和工程级服务。
     */
    fun register(application: MockApplication, project: MockProject) {
        analysisPluginXmls.forEach { pluginXmlPath ->
            PluginStructureProvider.registerApplicationServices(application, pluginXmlPath)
            PluginStructureProvider.registerProjectServices(project, pluginXmlPath)
        }
    }
}
