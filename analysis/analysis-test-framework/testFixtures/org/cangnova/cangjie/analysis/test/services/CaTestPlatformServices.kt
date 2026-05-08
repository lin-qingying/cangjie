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
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
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
    private val project: Project,
) {
    private val globalModificationCount = AtomicLong(0)
    private val moduleModificationCounts = ConcurrentHashMap<CaModule, AtomicLong>()
    @Volatile
    private var installedProjectStructure: InstalledCaTestProjectStructure? = null

    val snapshot: CaProjectStructureSnapshot
        get() = requireInstalledProjectStructure().snapshot

    /**
     * 安装当前测试用例声明的模块图。
     *
     * 与 IDE / Standalone 平台一样，测试宿主也必须先拥有一份稳定 project-structure，
     * 再让 `CangJieProjectStructureProvider`、`CaModuleProvider` 等平台服务统一委托给它。
     */
    fun install(moduleStructure: CjTestModuleStructure) {
        val builtinsModule = CaBuiltinsModuleImpl(project)
        installedProjectStructure = InstalledCaTestProjectStructure(builtinsModule, moduleStructure)
    }

    val modificationCount: Long
        get() = globalModificationCount.get()

    fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        return requireInstalledProjectStructure().getModule(element, useSiteModule)
    }

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
        private val builtinsModule: CaBuiltinsModule,
        private val moduleStructure: CjTestModuleStructure,
    ) {
        private val binaryModules = moduleStructure.binaryModules

        private val cachedSnapshot: CaProjectStructureSnapshot by lazy(LazyThreadSafetyMode.NONE) {
            CaProjectStructureSnapshot(
                allModules = moduleStructure.allCaModules,
                allResolvableModules = moduleStructure.allCaModules.filter(CaModule::isResolvable),
                allSourceLikeModules = moduleStructure.allCaModules.filterIsInstance<org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule>(),
                allSourceFiles = moduleStructure.allCjFiles,
            )
        }

        val snapshot: CaProjectStructureSnapshot
            get() = cachedSnapshot

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
    private val project: Project,
) : CaModuleProvider {
    override val snapshot: CaProjectStructureSnapshot
        get() = project.getService(CaTestPlatformState::class.java).snapshot

    override fun getModuleByStableName(stableModuleName: String): CaModule? {
        return project.getService(CaTestPlatformState::class.java).getModuleByStableName(stableModuleName)
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
