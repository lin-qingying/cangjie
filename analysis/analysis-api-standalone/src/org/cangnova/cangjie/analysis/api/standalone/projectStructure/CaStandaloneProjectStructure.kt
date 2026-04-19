@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaImplementationDetail::class,
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
)

package org.cangnova.cangjie.analysis.api.standalone.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaContentScopeRefiner
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureSnapshot
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScope
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScopeProvider
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.CaBaseResolutionScopeProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Standalone 项目结构视图。
 *
 * 该对象同时承载：
 * 1. [CangJieProjectStructureProvider]
 * 2. [CaModuleProvider]
 * 3. [CaContentScopeRefiner]
 * 4. [CaModificationTracker]
 * 5. [CaSessionInvalidationService]
 *
 * 这样 standalone 调用方只需要维护一份模块图，就能统一 project-structure、
 * 缓存失效和内容范围语义。
 */
class CaStandaloneProjectStructure(
    override val allModules: List<CaModule>,
) : CangJieProjectStructureProvider, CaModuleProvider, CaContentScopeRefiner, CaModificationTracker, CaSessionInvalidationService, CaResolutionScopeProvider {
    /**
     * Standalone 模块图必须显式绑定到唯一的 IntelliJ Project。
     */
    private val project: Project = run {
        check(allModules.isNotEmpty()) {
            "Standalone Analysis API 模块图不能为空。"
        }

        val distinctProjects = allModules.map(CaModule::project).distinct()
        check(distinctProjects.size == 1) {
            "Standalone Analysis API 模块图中的模块必须全部属于同一个 Project。"
        }
        distinctProjects.single()
    }

    private val globalModificationCount = AtomicLong(0)
    private val moduleModificationCounts = ConcurrentHashMap<CaModule, AtomicLong>()
    private val resolutionScopeProvider = CaBaseResolutionScopeProvider()
    private val rootEntries = buildList {
        allModules.forEach { module ->
            when (module) {
                is CaSourceModule ->
                    module.psiRoots.forEach { root -> add(ModuleRootEntry(root, module, RootKind.SOURCE)) }

                is CaLibrarySourceModule ->
                    module.sourceRoots.forEach { root -> add(ModuleRootEntry(root, module, RootKind.LIBRARY_SOURCE)) }

                is CaLibraryModule ->
                    module.binaryRoots.forEach { root -> add(ModuleRootEntry(root, module, RootKind.LIBRARY_BINARY)) }
            }
        }
    }.sortedWith(compareByDescending<ModuleRootEntry> { it.rootDepth }.thenByDescending { it.kind.priority })

    private val sourceFiles: List<PsiFileSystemItem> =
        rootEntries.map(ModuleRootEntry::root).distinct()

    override val snapshot: CaProjectStructureSnapshot =
        CaProjectStructureSnapshot(
            allModules = allModules,
            allResolvableModules = allModules.filter(CaModule::isResolvable),
            allSourceLikeModules = allModules.filterIsInstance<CaSourceModule>(),
            allSourceFiles = sourceFiles,
        )

    override val modificationCount: Long
        get() = globalModificationCount.get()

    override fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        useSiteModule?.let { return it }

        val containingFile = element.containingFile as? PsiFileSystemItem
        if (containingFile != null) {
            findModuleFor(containingFile)?.let { return it }
        }

        val availableModules = allModules.joinToString { module ->
            module.stableModuleName ?: module.moduleDescription
        }
        error(
            "Standalone 项目结构无法为 `${element.javaClass.simpleName}` 解析 use-site module。" +
                " 当前模块图：[$availableModules]",
        )
    }

    override fun getRefinedContentScope(module: CaModule, baseContentScope: GlobalSearchScope): GlobalSearchScope {
        return baseContentScope
    }

    override fun getResolutionScope(module: CaModule): CaResolutionScope {
        return resolutionScopeProvider.getResolutionScope(module)
    }

    override fun getModuleByStableName(stableModuleName: String): CaModule? {
        return snapshot.getModuleByStableName(stableModuleName)
    }

    override fun getModuleModificationCount(module: CaModule): Long {
        return moduleModificationCounts[module]?.get() ?: modificationCount
    }

    override fun invalidate(modules: Set<CaModule>) {
        if (modules.isEmpty()) return

        globalModificationCount.incrementAndGet()
        modules.forEach { module ->
            moduleModificationCounts.computeIfAbsent(module) { AtomicLong(0) }.incrementAndGet()
        }

        val delegatedInvalidationService =
            project.getService(CaSessionProvider::class.java) as? CaSessionInvalidationService

        if (delegatedInvalidationService != null && delegatedInvalidationService !== this) {
            delegatedInvalidationService.invalidate(modules)
        }
    }

    /**
     * 按 standalone 模块根解析文件所属模块。
     *
     * 这里不能只做“root 与文件完全相等”的匹配，
     * 因为 standalone 模块通常把目录根暴露给 Analysis API，而真正分析对象是该目录下的任意文件。
     */
    private fun findModuleFor(fileSystemItem: PsiFileSystemItem): CaModule? {
        return rootEntries.firstOrNull { entry -> entry.contains(fileSystemItem) }?.module
    }

    private data class ModuleRootEntry(
        val root: PsiFileSystemItem,
        val module: CaModule,
        val kind: RootKind,
    ) {
        val rootDepth: Int
            get() = root.virtualFile?.path?.length ?: root.name.length

        fun contains(item: PsiFileSystemItem): Boolean {
            if (root == item) return true

            val rootVirtualFile = root.virtualFile ?: return false
            val itemVirtualFile = item.virtualFile ?: return false
            return VfsUtilCore.isAncestor(rootVirtualFile, itemVirtualFile, false)
        }
    }

    private enum class RootKind(
        val priority: Int,
    ) {
        LIBRARY_BINARY(0),
        LIBRARY_SOURCE(1),
        SOURCE(2),
    }
}
