@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaImplementationDetail::class,
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
)

package org.cangnova.cangjie.analysis.api.standalone.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.CaBuiltinsModuleImpl
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureSnapshot
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScope
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScopeProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.CaBaseResolutionScopeProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.analysis.api.standalone.base.projectStructure.CangJieStaticProjectStructureProvider
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.platform.presentableDescription
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Standalone 项目结构视图。
 *
 * 该对象同时承载：
 * 1. [CangJieProjectStructureProvider]
 * 2. [CaModuleProvider]
 * 3. [CaModificationTracker]
 * 4. [CaSessionInvalidationService]
 *
 * 这样 standalone 调用方只需要维护一份模块图，就能统一 project-structure、
 * 缓存失效和内容范围语义。
 */
class CaStandaloneProjectStructure(
    /**
     * standalone 上下文中完整、可达且已去重的模块图。
     */
    override val allModules: List<CaModule>,
) : CangJieStaticProjectStructureProvider(), CaModuleProvider, CaModificationTracker, CaSessionInvalidationService, CaResolutionScopeProvider {
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

    /**
     * 整张 standalone 模块图的全局修改计数。
     */
    private val globalModificationCount = AtomicLong(0)

    /**
     * 单模块粒度的修改计数表。
     */
    private val moduleModificationCounts = ConcurrentHashMap<CaModule, AtomicLong>()

    /**
     * 复用基础 Analysis API 解析作用域实现的委托。
     */
    private val resolutionScopeProvider = CaBaseResolutionScopeProvider()

    /**
     * standalone 模块图推导出的唯一目标平台。
     */
    private val platform: TargetPlatform = inferStandaloneTargetPlatform(allModules)

    /**
     * 当 PSI 不属于任何内容 root 且没有 PSI 文件可挂载时使用的兜底模块。
     */
    private val notUnderContentRootModuleWithoutPsiFile: CaNotUnderContentRootModule by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaStandaloneNotUnderContentRootModule(
            name = "unnamed-outside-content-root",
            originalModule = null,
            project = project,
            scopeRoots = emptyList(),
            targetPlatform = platform,
        )
    }

    /**
     * 当前平台下的 builtins 模块；模块图未显式提供时使用基础实现补齐。
     */
    private val builtinsModule: CaBuiltinsModule by lazy(LazyThreadSafetyMode.PUBLICATION) {
        allModules
            .filterIsInstance<CaBuiltinsModule>()
            .firstOrNull { module -> module.targetPlatform == platform }
            ?: CaBuiltinsModuleImpl(platform, project)
    }

    /**
     * 从所有模块 root 构建的归属判定表，较深 root 与源码 root 拥有更高优先级。
     */
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

    /**
     * 当前 standalone 模块图暴露给 Analysis API 的全部源码文件系统项。
     */
    override val allSourceFiles: List<PsiFileSystemItem> =
        rootEntries.map(ModuleRootEntry::root).distinct()

    /**
     * 当前模块图的不可变查询快照。
     */
    override val snapshot: CaProjectStructureSnapshot =
        CaProjectStructureSnapshot(
            allModules = allModules,
            allResolvableModules = allModules.filter(CaModule::isResolvable),
            allSourceLikeModules = allModules.filterIsInstance<CaSourceModule>(),
            allSourceFiles = allSourceFiles,
        )

    /**
     * 当前 standalone project structure 的全局修改计数。
     */
    override val modificationCount: Long
        get() = globalModificationCount.get()

    /**
     * 根据 PSI 元素、use-site 偏好和模块 root 表解析元素所属模块。
     */
    override fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        val containingFile = element.containingFile ?: return notUnderContentRootModuleWithoutPsiFile
        resolveBuiltinsModule(containingFile)?.let { return it }
        computeSpecialModule(containingFile)?.let { return it }

        useSiteModule?.let { return it }

        val containingItem = containingFile as? PsiFileSystemItem
        if (containingItem != null) {
            findModuleFor(containingItem)?.let { return it }
        }
        if (containingItem != null) {
            return CaStandaloneNotUnderContentRootModule(
                name = "unnamed-outside-content-root",
                originalModule = null,
                project = project,
                scopeRoots = listOf(containingItem),
                targetPlatform = platform,
            )
        }

        val availableModules = allModules.joinToString { module -> module.stableModuleName ?: module.moduleDescription }
        error(
            "Standalone 项目结构无法为 `${element.javaClass.simpleName}` 解析 use-site module。" +
                " 当前模块图：[$availableModules]",
        )
    }

    /**
     * 返回直接 depends-on 当前模块的实现模块集合。
     */
    override fun getImplementingModules(module: CaModule): List<CaModule> {
        return allModules.filter { module in it.directDependsOnDependencies }
    }

    /**
     * 返回 standalone 环境中不属于内容 root 的兜底模块。
     */
    override fun getNotUnderContentRootModule(project: Project): CaNotUnderContentRootModule {
        return notUnderContentRootModuleWithoutPsiFile
    }

    /**
     * 返回指定模块的解析作用域。
     */
    override fun getResolutionScope(module: CaModule): CaResolutionScope {
        return resolutionScopeProvider.getResolutionScope(module)
    }

    /**
     * 根据稳定模块名查询当前模块图中的模块。
     */
    override fun getModuleByStableName(stableModuleName: String): CaModule? {
        return snapshot.getModuleByStableName(stableModuleName)
    }

    /**
     * 返回指定模块的修改计数；未单独失效过的模块回落到全局修改计数。
     */
    override fun getModuleModificationCount(module: CaModule): Long {
        return moduleModificationCounts[module]?.get() ?: modificationCount
    }

    /**
     * 失效指定模块并同步通知 session invalidation service。
     */
    override fun invalidate(modules: Set<CaModule>) {
        if (modules.isEmpty()) return

        globalModificationCount.incrementAndGet()
        modules.forEach { module ->
            moduleModificationCounts.computeIfAbsent(module) { AtomicLong(0) }.incrementAndGet()
        }

        val delegatedInvalidationService =
            project.getService(CaSessionProvider::class.java) as? CaSessionInvalidationService

        if (delegatedInvalidationService != null) {
            delegatedInvalidationService.invalidate(modules)
        }
    }

    /**
     * 按 standalone 模块根解析文件所属模块。
     *
     * 这里不能只做“root 与文件完全相等”的匹配，
     * 因为 standalone 模块通常把目录根暴露给 Analysis API，而真正分析对象是该目录下的任意文件。
     */
    private fun findModuleFor(fileSystemItem: PsiFileSystemItem?): CaModule? {
        if (fileSystemItem == null) return null
        return rootEntries.firstOrNull { entry -> entry.contains(fileSystemItem) }?.module
    }

    /**
     * 判断指定 PSI 文件是否属于当前平台的 builtins 内容作用域。
     */
    private fun resolveBuiltinsModule(file: PsiFile): CaBuiltinsModule? {
        val virtualFile = file.virtualFile ?: return null
        return builtinsModule.takeIf { module -> virtualFile in module.contentScope }
    }

    /**
     * 单个模块 root 的归属判定记录。
     */
    private data class ModuleRootEntry(
        /**
         * 模块暴露的 PSI root。
         */
        val root: PsiFileSystemItem,
        /**
         * 该 root 所属模块。
         */
        val module: CaModule,
        /**
         * root 类型，用于在多个 root 命中时排序。
         */
        val kind: RootKind,
    ) {
        /**
         * root 路径长度，用于让更具体的 root 优先匹配。
         */
        val rootDepth: Int
            get() = root.virtualFile?.path?.length ?: root.name.length

        /**
         * 判断目标 PSI 文件系统项是否位于该 root 覆盖范围内。
         */
        fun contains(item: PsiFileSystemItem): Boolean {
            if (root == item) return true

            val rootVirtualFile = root.virtualFile ?: return false
            val itemVirtualFile = item.virtualFile ?: return false
            return VfsUtilCore.isAncestor(rootVirtualFile, itemVirtualFile, false)
        }
    }

    /**
     * 模块 root 的种类和优先级。
     */
    private enum class RootKind(
        /**
         * 多个 root 同时命中时的排序优先级。
         */
        val priority: Int,
    ) {
        /**
         * 二进制库 root，优先级最低。
         */
        LIBRARY_BINARY(0),

        /**
         * 库源码 root，优先级高于二进制库 root。
         */
        LIBRARY_SOURCE(1),

        /**
         * 源码 root，优先级最高。
         */
        SOURCE(2),
    }

    companion object {
        /**
         * Standalone project-structure 对位 Kotlin `KotlinStandaloneProjectStructureProvider`：
         * 一张模块图只承载一个高层 targetPlatform。
         */
        private fun inferStandaloneTargetPlatform(allModules: List<CaModule>): TargetPlatform {
            val distinctPlatforms = allModules.map(CaModule::targetPlatform).distinct()
            check(distinctPlatforms.size == 1) {
                "Standalone Analysis API 模块图中的模块必须全部属于同一个 targetPlatform，" +
                    "实际得到：${distinctPlatforms.joinToString { it.presentableDescription }}"
            }
            return distinctPlatforms.single()
        }
    }
}
