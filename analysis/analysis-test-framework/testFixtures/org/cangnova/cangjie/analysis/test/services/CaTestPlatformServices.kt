@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.test.services

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CaBuiltinsModuleImpl
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureSnapshot
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieModuleDependentsProviderBase
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructure
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Analysis API 测试平台的统一状态源。
 *
 * 测试环境同样需要完整的平台接口闭环，而不是只提供一个 project-structure provider。
 * 这里统一维护模块快照与修改计数，让测试平台在行为模型上与 IDE、Standalone 平台保持一致。
 */
class CaTestPlatformState(
    /**
     * 当前测试平台服务绑定的 project。
     */
    private val project: Project,
) {
    /**
     * 测试模块图的全局修改计数。
     */
    private val globalModificationCount = AtomicLong(0)

    /**
     * 单个模块的修改计数表。
     */
    private val moduleModificationCounts = ConcurrentHashMap<CaModule, AtomicLong>()

    /**
     * 当前测试用例已经安装的稳定 project-structure 视图。
     */
    @Volatile
    private var installedProjectStructure: InstalledCaTestProjectStructure? = null

    /**
     * 当前测试模块图快照。
     */
    val snapshot: CaProjectStructureSnapshot
        get() = requireInstalledProjectStructure().snapshot

    /**
     * 安装当前测试用例声明的模块图。
     *
     * 与 IDE / Standalone 平台一样，测试宿主也必须先拥有一份稳定 project-structure，
     * 再让 `CangJieProjectStructureProvider`、`CaModuleProvider` 等平台服务统一委托给它。
     */
    fun install(moduleStructure: CjTestModuleStructure) {
        val builtinsModule =
            CaBuiltinsModuleImpl(
                targetPlatform = moduleStructure.mainModules.first().caModule.targetPlatform,
                project = project,
            )
        installedProjectStructure = InstalledCaTestProjectStructure(builtinsModule, moduleStructure)
    }

    /**
     * 当前测试平台的全局修改计数。
     */
    val modificationCount: Long
        get() = globalModificationCount.get()

    /**
     * 根据 PSI 元素和可选 use-site module 解析 Analysis API 模块。
     */
    fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        return requireInstalledProjectStructure().getModule(element, useSiteModule)
    }

    /**
     * 返回指定模块的修改计数。
     */
    fun getModuleModificationCount(module: CaModule): Long {
        return moduleModificationCounts[module]?.get() ?: modificationCount
    }

    /**
     * 根据稳定模块名查找模块。
     */
    fun getModuleByStableName(stableModuleName: String): CaModule? {
        return snapshot.getModuleByStableName(stableModuleName)
    }

    /**
     * 失效指定模块并推进全局/模块级修改计数。
     */
    fun invalidate(modules: Set<CaModule>) {
        if (modules.isEmpty()) return

        globalModificationCount.incrementAndGet()
        modules.forEach { module ->
            moduleModificationCounts.computeIfAbsent(module) { AtomicLong(0) }.incrementAndGet()
        }
    }

    /**
     * 返回已安装的测试 project structure，未安装时抛出明确错误。
     */
    private fun requireInstalledProjectStructure(): InstalledCaTestProjectStructure {
        return installedProjectStructure
            ?: error("Analysis API test project structure has not been installed yet.")
    }

    /**
     * 测试宿主安装完成后的稳定 project-structure 视图。
     *
     * Kotlin `KotlinTestProjectStructureProvider` 的核心语义在这里保持一致：
     * 1. builtins 先按 builtins scope 命中；
     * 2. library binary 再按 binary content scope 命中；
     * 3. source/file 最后回落到测试模块结构；
     * 4. 找不到模块时直接报错，而不是兜底生成额外模块。
     */
    private class InstalledCaTestProjectStructure(
        /**
         * 当前测试环境使用的 builtins 模块。
         */
        private val builtinsModule: CaBuiltinsModule,
        /**
         * 当前测试用例的测试模块结构。
         */
        private val moduleStructure: CjTestModuleStructure,
    ) {
        /**
         * 当前测试模块结构中的 library binary 模块。
         */
        private val binaryModules = moduleStructure.binaryModules

        /**
         * 当前测试 project structure 的缓存快照。
         */
        private val cachedSnapshot: CaProjectStructureSnapshot by lazy(LazyThreadSafetyMode.NONE) {
            val allModules = listOf(builtinsModule) + moduleStructure.allCaModules
            CaProjectStructureSnapshot(
                allModules = allModules,
                allResolvableModules = allModules.filter(CaModule::isResolvable),
                allSourceLikeModules = moduleStructure.allSourceLikeModules,
                allSourceFiles = moduleStructure.allSourceFiles,
            )
        }

        /**
         * 当前测试 project structure 的不可变快照。
         */
        val snapshot: CaProjectStructureSnapshot
            get() = cachedSnapshot

        /**
         * 按 builtins、library binary、source/module structure 的顺序解析元素所属模块。
         */
        fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
            useSiteModule?.let { return it }

            val containingFile = element.containingFile
                ?: error("Cannot resolve module for PSI element without containing file: $element")
            val virtualFile = containingFile.virtualFile

            if (virtualFile != null) {
                if (virtualFile in builtinsModule.contentScope) {
                    return builtinsModule
                }

                binaryModules
                    .firstOrNull { module -> virtualFile in module.contentScope }
                    ?.let { return it }
            }

            moduleStructure.findModuleByFile(containingFile)?.let { return it.caModule }

            error(
                buildString {
                    append("Cannot find CjTestModule for `${containingFile.name}` in Analysis API test module structure.")
                    if (virtualFile != null) {
                        append(" virtualFileUrl=")
                        append(virtualFile.url)
                        append(" fileType=")
                        append(virtualFile.fileType.name)
                    }
                },
            )
        }
    }
}

/**
 * 测试环境的模块图提供器。
 */
class CaTestModuleProvider(
    /**
     * 当前测试平台服务绑定的 project。
     */
    private val project: Project,
) : CaModuleProvider {
    /**
     * 当前测试模块图快照。
     */
    override val snapshot: CaProjectStructureSnapshot
        get() = project.getService(CaTestPlatformState::class.java).snapshot

    /**
     * 根据稳定模块名查找当前测试模块。
     */
    override fun getModuleByStableName(stableModuleName: String): CaModule? {
        return project.getService(CaTestPlatformState::class.java).getModuleByStableName(stableModuleName)
    }
}

/**
 * 测试环境的修改计数服务。
 */
class CaTestModificationTracker(
    /**
     * 当前测试平台服务绑定的 project。
     */
    private val project: Project,
) : CaModificationTracker {
    /**
     * 当前测试平台状态服务。
     */
    private val state: CaTestPlatformState
        get() = project.getService(CaTestPlatformState::class.java)

    /**
     * 当前测试平台全局修改计数。
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
 * 测试环境的静态模块 dependents provider。
 *
 * Analysis API 测试中的模块图在单个用例执行期间是静态快照，
 * 因而这里直接按当前 snapshot 预计算 direct / refinement dependents，
 * 与 Kotlin `KtStaticModuleDependentsProvider` 保持同一职责边界。
 */
class CaTestModuleDependentsProvider(
    /**
     * 当前测试平台服务绑定的 project。
     */
    private val project: Project,
) : CangJieModuleDependentsProviderBase() {
    /**
     * 当前模块图中 direct dependency 到直接 dependent 的映射。
     */
    private val directDependentsByModule: Map<CaModule, Set<CaModule>>
        get() = buildDependentsMap(currentModules()) { module ->
            module.allDirectDependencies.asSequence()
        }

    /**
     * 当前模块图中 refinement dependency 到 dependent 的映射。
     */
    private val refinementDependentsByModule: Map<CaModule, Set<CaModule>>
        get() = buildDependentsMap(currentModules()) { module ->
            module.transitiveDependsOnDependencies.asSequence()
        }

    /**
     * 返回直接依赖指定模块的模块集合。
     */
    override fun getDirectDependents(module: CaModule): Set<CaModule> {
        return directDependentsByModule[module].orEmpty()
    }

    /**
     * 返回通过 refinement/depends-on 依赖指定模块的模块集合。
     */
    override fun getRefinementDependents(module: CaModule): Set<CaModule> {
        return refinementDependentsByModule[module].orEmpty()
    }

    /**
     * 返回当前测试快照中的全部模块。
     */
    private fun currentModules(): List<CaModule> {
        return project.getService(CaTestPlatformState::class.java).snapshot.allModules
    }
}

/**
 * 测试环境的 session 失效服务。
 *
 * 它先更新测试平台自己的修改计数，再把失效信号转发给真实的 session provider，
 * 保证 modification tracker 与 session cache 的行为边界一致。
 */
class CaTestSessionInvalidationService(
    /**
     * 当前测试平台服务绑定的 project。
     */
    private val project: Project,
) : CaSessionInvalidationService {
    /**
     * 失效指定模块，并将事件转发给真实 session provider。
     */
    override fun invalidate(modules: Set<CaModule>) {
        project.getService(CaTestPlatformState::class.java).invalidate(modules)

        val delegatedInvalidationService =
            project.getService(CaSessionProvider::class.java) as? CaSessionInvalidationService
        if (delegatedInvalidationService != null && delegatedInvalidationService !== this) {
            delegatedInvalidationService.invalidate(modules)
        }
    }
}

/**
 * 根据依赖投影函数构建 dependency -> dependents 的反向索引。
 */
private inline fun buildDependentsMap(
    modules: List<CaModule>,
    getDependencies: (CaModule) -> Sequence<CaModule>,
): Map<CaModule, Set<CaModule>> = buildMap {
    modules.forEach { module ->
        getDependencies(module).forEach { dependency ->
            if (dependency == module) return@forEach

            val dependents = (this[dependency] as? MutableSet<CaModule>) ?: linkedSetOf<CaModule>().also {
                put(dependency, it)
            }
            dependents.add(module)
        }
    }
}
