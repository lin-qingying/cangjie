package org.cangnova.cangjie.analysis.test.services

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaContentScopeRefiner
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureSnapshot
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Analysis API 测试平台的统一状态源。
 *
 * 测试环境同样需要完整的平台接口闭环，而不是只提供一个 project-structure provider。
 * 这里统一维护模块快照与修改计数，让测试平台在行为模型上与 IDE、Standalone 平台保持一致。
 */
class CaTestPlatformState(
    private val project: Project,
) {
    private val globalModificationCount = AtomicLong(0)
    private val moduleModificationCounts = ConcurrentHashMap<CaModule, AtomicLong>()

    private val moduleStructure
        get() = CaTestProjectStructureRegistry.get(project)

    private val allModules: List<CaModule>
        get() = moduleStructure.allCaModules

    private val allSourceFiles
        get() = moduleStructure.allCjFiles

    /**
     * 测试模块图在单个用例生命周期内视为稳定事实，因此直接缓存快照。
     */
    private val cachedSnapshot: CaProjectStructureSnapshot by lazy(LazyThreadSafetyMode.NONE) {
        CaProjectStructureSnapshot(
            allModules = allModules,
            allResolvableModules = allModules.filter(CaModule::isResolvable),
            allSourceLikeModules = allModules.filterIsInstance<CaSourceModule>(),
            allSourceFiles = allSourceFiles,
        )
    }

    val snapshot: CaProjectStructureSnapshot
        get() = cachedSnapshot

    val modificationCount: Long
        get() = globalModificationCount.get()

    fun getModuleModificationCount(module: CaModule): Long {
        return moduleModificationCounts[module]?.get() ?: modificationCount
    }

    fun getModuleByStableName(stableModuleName: String): CaModule? {
        return snapshot.getModuleByStableName(stableModuleName)
    }

    fun invalidate(modules: Set<CaModule>) {
        if (modules.isEmpty()) return

        globalModificationCount.incrementAndGet()
        modules.forEach { module ->
            moduleModificationCounts.computeIfAbsent(module) { AtomicLong(0) }.incrementAndGet()
        }
    }
}

/**
 * 测试环境的模块图提供器。
 */
class CaTestModuleProvider(
    private val project: Project,
) : CaModuleProvider {
    override val snapshot: CaProjectStructureSnapshot
        get() = project.getService(CaTestPlatformState::class.java).snapshot

    override fun getModuleByStableName(stableModuleName: String): CaModule? {
        return project.getService(CaTestPlatformState::class.java).getModuleByStableName(stableModuleName)
    }
}

/**
 * 测试环境的内容范围精炼器。
 *
 * 当前测试框架显式构造出的 content scope 已经足够精确，
 * 因此这里保持恒等映射，但仍保留平台接口位置。
 */
class CaTestContentScopeRefiner : CaContentScopeRefiner {
    override fun getRefinedContentScope(module: CaModule, baseContentScope: GlobalSearchScope): GlobalSearchScope {
        return baseContentScope
    }
}

/**
 * 测试环境的修改计数服务。
 */
class CaTestModificationTracker(
    private val project: Project,
) : CaModificationTracker {
    private val state: CaTestPlatformState
        get() = project.getService(CaTestPlatformState::class.java)

    override val modificationCount: Long
        get() = state.modificationCount

    override fun getModuleModificationCount(module: CaModule): Long {
        return state.getModuleModificationCount(module)
    }
}

/**
 * 测试环境的 session 失效服务。
 *
 * 它先更新测试平台自己的修改计数，再把失效信号转发给真实的 session provider，
 * 保证 modification tracker 与 session cache 的行为边界一致。
 */
class CaTestSessionInvalidationService(
    private val project: Project,
) : CaSessionInvalidationService {
    override fun invalidate(modules: Set<CaModule>) {
        project.getService(CaTestPlatformState::class.java).invalidate(modules)

        val delegatedInvalidationService = project.getService(CaSessionProvider::class.java) as? CaSessionInvalidationService
        if (delegatedInvalidationService != null && delegatedInvalidationService !== this) {
            delegatedInvalidationService.invalidate(modules)
        }
    }
}
