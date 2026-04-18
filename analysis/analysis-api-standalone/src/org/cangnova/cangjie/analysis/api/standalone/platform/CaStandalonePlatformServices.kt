package org.cangnova.cangjie.analysis.api.standalone.platform

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.PluginStructureProvider
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaContentScopeRefiner
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureSnapshot
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
 * - [CaProjectStructureProvider]
 * - [CaModuleProvider]
 * - [CaContentScopeRefiner]
 * - [CaModificationTracker]
 * - [CaSessionInvalidationService]
 *
 * 这样 builder、session provider 与平台 service 容器之间始终只存在一条一致的装配链。
 */
class CaStandalonePlatformState(
    private val project: Project,
) {
    private val structureVersion = AtomicLong(0)

    @Volatile
    private var activeProjectStructure: CaStandaloneProjectStructure? = null

    val allModules: List<CaModule>
        get() = requireProjectStructure().allModules

    val allSourceFiles: List<PsiFileSystemItem>
        get() = requireProjectStructure().allSourceFiles

    val allResolvableModules: List<CaModule>
        get() = requireProjectStructure().allResolvableModules

    val allSourceLikeModules: List<CaModule>
        get() = requireProjectStructure().allSourceLikeModules

    val snapshot: CaProjectStructureSnapshot
        get() = requireProjectStructure().snapshot

    val modificationCount: Long
        get() = structureVersion.get() + requireProjectStructure().modificationCount

    /**
     * 安装新的 standalone 模块图。
     *
     * 这里显式把“模块图切换”视为平台级失效事件：
     * - 旧模块图中的 session 缓存必须失效；
     * - 新模块图必须立即成为 `CaProjectStructureProvider` 的可见视图；
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

    fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        return requireProjectStructure().getModule(element, useSiteModule)
    }

    fun getRefinedContentScope(module: CaModule, baseContentScope: GlobalSearchScope): GlobalSearchScope {
        return requireProjectStructure().getRefinedContentScope(module, baseContentScope)
    }

    fun getModuleModificationCount(module: CaModule): Long {
        return structureVersion.get() + requireProjectStructure().getModuleModificationCount(module)
    }

    fun getModuleByStableName(stableModuleName: String): CaModule? {
        return snapshot.getModuleByStableName(stableModuleName)
    }

    fun invalidate(modules: Set<CaModule>) {
        val projectStructure = requireProjectStructure()
        projectStructure.invalidate(modules)
        structureVersion.incrementAndGet()
    }

    private fun requireProjectStructure(): CaStandaloneProjectStructure {
        return activeProjectStructure
            ?: error("Standalone Analysis API 尚未安装 project structure，无法执行平台级查询。")
    }

    private fun invalidateSessionProvider(modules: Set<CaModule>) {
        val invalidationService = project.getService(CaSessionProvider::class.java) as? CaSessionInvalidationService
        if (invalidationService != null) {
            invalidationService.invalidate(modules)
        }
    }

    companion object {
        fun getInstance(project: Project): CaStandalonePlatformState = project.service()
    }
}

/**
 * Standalone 平台的 project-structure 服务委托。
 */
class CaStandaloneProjectStructureProvider(
    private val project: Project,
) : CaProjectStructureProvider {
    private val state: CaStandalonePlatformState
        get() = CaStandalonePlatformState.getInstance(project)

    override fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        return state.getModule(element, useSiteModule)
    }

    override val snapshot: CaProjectStructureSnapshot
        get() = state.snapshot
}

/**
 * Standalone 平台的 module-provider 服务委托。
 */
class CaStandaloneModuleProvider(
    private val project: Project,
) : CaModuleProvider {
    private val state: CaStandalonePlatformState
        get() = CaStandalonePlatformState.getInstance(project)

    override val snapshot: CaProjectStructureSnapshot
        get() = state.snapshot

    override fun getModuleByStableName(stableModuleName: String): CaModule? {
        return state.getModuleByStableName(stableModuleName)
    }
}

/**
 * Standalone 平台的 content-scope 服务委托。
 */
class CaStandaloneContentScopeRefiner(
    private val project: Project,
) : CaContentScopeRefiner {
    private val state: CaStandalonePlatformState
        get() = CaStandalonePlatformState.getInstance(project)

    override fun getRefinedContentScope(module: CaModule, baseContentScope: GlobalSearchScope): GlobalSearchScope {
        return state.getRefinedContentScope(module, baseContentScope)
    }
}

/**
 * Standalone 平台的 modification-tracker 服务委托。
 */
class CaStandaloneModificationTracker(
    private val project: Project,
) : CaModificationTracker {
    private val state: CaStandalonePlatformState
        get() = CaStandalonePlatformState.getInstance(project)

    override val modificationCount: Long
        get() = state.modificationCount

    override fun getModuleModificationCount(module: CaModule): Long {
        return state.getModuleModificationCount(module)
    }
}

/**
 * Standalone 平台的 session 失效服务委托。
 */
class CaStandaloneSessionInvalidationService(
    private val project: Project,
) : CaSessionInvalidationService {
    private val state: CaStandalonePlatformState
        get() = CaStandalonePlatformState.getInstance(project)

    override fun invalidate(modules: Set<CaModule>) {
        state.invalidate(modules)
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
    override val isAnalysisRestricted: Boolean
        get() = false

    override val isRestrictedAnalysisAllowed: Boolean
        get() = true

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
    override val allowUseSiteLibraryModuleAnalysis: Boolean
        get() = true
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
    private val analysisPluginXmls = listOf(
        "META-INF/analysis-api/cangjie-analysis-api-cfir.xml",
        "META-INF/analysis-api/cangjie-cj-references.xml",
        "META-INF/analysis-api/cangjie-analysis-api-standalone.xml",
    )

    fun register(application: MockApplication, project: MockProject) {
        analysisPluginXmls.forEach { pluginXmlPath ->
            PluginStructureProvider.registerApplicationServices(application, pluginXmlPath)
            PluginStructureProvider.registerProjectServices(project, pluginXmlPath)
        }
    }
}
