package org.cangnova.cangjie.analysis.api.impl.base.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiModificationTracker
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.decompiled.CaBuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaContentScopeRefiner
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureSnapshot
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryFallbackDependenciesModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaTargetPlatform
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * IDE 平台的统一项目结构状态源。
 *
 * 这里不再把 IntelliJ 项目压扁成“source root + library”的简化模型，而是显式维护
 * Analysis API 需要的完整模块族：
 * 1. 项目源码模块
 * 2. 脚本模块
 * 3. 库二进制模块
 * 4. 库源码模块
 * 5. 游离文件模块
 * 6. 不在内容根下的临时模块
 * 7. fallback / script dependency / builtins 这类依赖边界模块
 *
 * 这样 low-level CFIR、session cache、权限服务和平台失效策略才能围绕同一张模块图工作，
 * 不再在各层重复推导 use-site module 和依赖边界。
 */
class CaIdeProjectStructureState(
    private val project: Project,
) : CaProjectStructureProvider, CaModuleProvider, CaContentScopeRefiner, CaModificationTracker, CaSessionInvalidationService {
    private val psiManager = PsiManager.getInstance(project)
    private val projectFileIndex = ProjectFileIndex.getInstance(project)
    private val projectRootManager = ProjectRootManager.getInstance(project)

    private val explicitGlobalInvalidationCount = AtomicLong(0)
    private val explicitModuleInvalidationCounts = ConcurrentHashMap<CaModule, AtomicLong>()

    private val sourceModulesByRootUrl = ConcurrentHashMap<String, CaIdeSourceModule>()
    private val libraryModulesByKey = ConcurrentHashMap<LibraryBinaryKey, CaIdeLibraryModule>()
    private val librarySourceModulesByKey = ConcurrentHashMap<LibrarySourceKey, CaIdeLibrarySourceModule>()
    private val fallbackDependencyModulesByOwnerKey = ConcurrentHashMap<String, CaIdeLibraryFallbackDependenciesModule>()
    private val danglingFileModulesByPath = ConcurrentHashMap<String, CaIdeDanglingFileModule>()
    private val outsideContentModulesByPath = ConcurrentHashMap<String, CaIdeNotUnderContentRootModule>()
    @Volatile
    private var cachedSnapshotState: CachedSnapshotState? = null

    private val builtinsModule: CaIdeBuiltinsModule by lazy {
        CaIdeBuiltinsModule(project = project)
    }

    override val allModules: List<CaModule>
        get() = snapshot.allModules

    override val allResolvableModules: List<CaModule>
        get() = snapshot.allResolvableModules

    override val allSourceLikeModules: List<CaModule>
        get() = snapshot.allSourceLikeModules

    override val allSourceFiles: List<PsiFileSystemItem>
        get() = snapshot.allSourceFiles

    override val snapshot: CaProjectStructureSnapshot
        get() = getOrBuildSnapshot()

    override val modificationCount: Long
        get() = PsiModificationTracker.getInstance(project).modificationCount + explicitGlobalInvalidationCount.get()

    override fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        useSiteModule?.let { return it }

        val containingItem = element.containingFile as? PsiFileSystemItem
            ?: error("无法为 `${element::class.simpleName}` 选择 Analysis API use-site module：元素不位于 PSI 文件中。")
        val virtualFile = containingItem.virtualFile
            ?: error("无法为 `${element::class.simpleName}` 选择 Analysis API use-site module：PSI 文件没有 VirtualFile。")

        val cjFile = containingItem as? CjFile
        if (cjFile != null && isDanglingLikeFile(cjFile)) {
            return danglingFileModuleFor(cjFile)
        }

        if (projectFileIndex.isInLibrarySource(virtualFile)) {
            return librarySourceModuleFor(virtualFile)
        }

        if (projectFileIndex.isInLibraryClasses(virtualFile)) {
            return libraryBinaryModuleFor(virtualFile)
        }

        val sourceRoot = projectFileIndex.getSourceRootForFile(virtualFile)
        if (sourceRoot != null && projectFileIndex.isInSource(virtualFile)) {
            return sourceModuleFor(sourceRoot)
        }

        return outsideContentModuleFor(containingItem)
    }

    override fun getRefinedContentScope(module: CaModule, baseContentScope: GlobalSearchScope): GlobalSearchScope {
        return baseContentScope
    }

    override fun getModuleByStableName(stableModuleName: String): CaModule? {
        return snapshot.getModuleByStableName(stableModuleName)
    }

    override fun getModuleModificationCount(module: CaModule): Long {
        return explicitModuleInvalidationCounts[module]?.get() ?: modificationCount
    }

    override fun invalidate(modules: Set<CaModule>) {
        if (modules.isEmpty()) return

        explicitGlobalInvalidationCount.incrementAndGet()
        modules.forEach { module ->
            explicitModuleInvalidationCounts.computeIfAbsent(module) { AtomicLong(0) }.incrementAndGet()
        }

        val delegatedInvalidationService = project.getService(CaSessionProvider::class.java) as? CaSessionInvalidationService
        if (delegatedInvalidationService != null && delegatedInvalidationService !== this) {
            delegatedInvalidationService.invalidate(modules)
        }
    }

    /**
     * IDE 平台统一构造当前模块图快照。
     *
     * 这里显式把“模块图枚举”“源码视图枚举”“可解析模块过滤”绑定到同一轮结构收集，
     * 避免不同属性访问时各自重建出不一致的中间结果。
     */
    private fun buildSnapshot(): CaProjectStructureSnapshot {
        val snapshotStamp = currentSnapshotStamp()
        val sourceEntries = sourceRootEntries()
        sourceEntries.forEach { entry ->
            refreshRegularDependencies(entry.module, entry.root.virtualFile, snapshotStamp)
        }

        val allModules = buildList {
            add(builtinsModule)
            addAll(sourceEntries.map(ModuleRootEntry::module))
            addAll(librarySourceModulesByKey.values.sortedBy(CaModule::moduleDescription))
            addAll(libraryModulesByKey.values.sortedBy(CaModule::moduleDescription))
            addAll(fallbackDependencyModulesByOwnerKey.values.sortedBy(CaModule::moduleDescription))
            addAll(danglingFileModulesByPath.values.sortedBy(CaModule::moduleDescription))
            addAll(outsideContentModulesByPath.values.sortedBy(CaModule::moduleDescription))
        }.distinctBy { module ->
            module.stableModuleName ?: module.moduleDescription
        }

        val allSourceFiles = buildList {
            sourceEntries.mapTo(this, ModuleRootEntry::root)
            librarySourceModulesByKey.values.flatMapTo(this) { it.sourceRoots }
            danglingFileModulesByPath.values.mapTo(this, CaIdeDanglingFileModule::item)
            outsideContentModulesByPath.values.mapTo(this, CaIdeNotUnderContentRootModule::item)
        }.distinctBy { item ->
            item.virtualFile?.url ?: item.name
        }

        return CaProjectStructureSnapshot(
            allModules = allModules,
            allResolvableModules = allModules.filter(CaModule::isResolvable),
            allSourceLikeModules = allModules.filterIsInstance<CaSourceModule>(),
            allSourceFiles = allSourceFiles,
        )
    }

    /**
     * IDE 平台的模块图快照需要绑定到统一的结构时间戳。
     *
     * 这里显式用“PSI 修改 + 显式 invalidation”组成结构戳，
     * 保证 session provider、模块查询和低层 resolve 至少围绕同一轮 Analysis API 结构视图工作。
     */
    private fun getOrBuildSnapshot(): CaProjectStructureSnapshot {
        val currentStamp = currentSnapshotStamp()
        val cached = cachedSnapshotState
        if (cached?.stamp == currentStamp) {
            return cached.snapshot
        }

        synchronized(this) {
            val synchronizedCached = cachedSnapshotState
            if (synchronizedCached?.stamp == currentStamp) {
                return synchronizedCached.snapshot
            }

            return buildSnapshot().also { snapshot ->
                cachedSnapshotState = CachedSnapshotState(currentStamp, snapshot)
            }
        }
    }

    private fun currentSnapshotStamp(): SnapshotStamp {
        return SnapshotStamp(
            psiModificationCount = PsiModificationTracker.getInstance(project).modificationCount,
            explicitInvalidationCount = explicitGlobalInvalidationCount.get(),
        )
    }

    private fun sourceRootEntries(): List<ModuleRootEntry> {
        return projectRootManager.contentSourceRoots
            .mapNotNull(::toPsiFileSystemItem)
            .map { root ->
                val module = sourceModuleFor(root.virtualFile)
                ModuleRootEntry(root, module)
            }
            .sortedBy { it.root.virtualFile.path }
    }

    private fun sourceModuleFor(root: VirtualFile): CaIdeSourceModule {
        return sourceModulesByRootUrl.computeIfAbsent(root.url) {
            val psiRoot = toPsiFileSystemItem(root)
                ?: error("无法为 source root `${root.path}` 构建 PSI 根。")
            CaIdeSourceModule(
                project = project,
                root = psiRoot,
                sourceRootUrl = root.url,
                sourceRootPath = root.path,
            )
        }.also { module ->
            refreshRegularDependencies(module, root, currentSnapshotStamp())
        }
    }

    private fun libraryBinaryModuleFor(file: VirtualFile): CaIdeLibraryModule {
        val identity = resolveLibraryIdentity(file)
        return libraryModulesByKey.computeIfAbsent(identity.binaryKey) {
            val binaryRoots = identity.binaryRoots.mapNotNull(::toPsiFileSystemItem)
            check(binaryRoots.isNotEmpty()) {
                "库 `${identity.libraryName}` 没有可用的二进制根，无法构建 IDE 库模块。"
            }
            CaIdeLibraryModule(
                project = project,
                libraryName = identity.libraryName,
                binaryRoots = binaryRoots,
                binaryRootUrls = identity.binaryRoots.map(VirtualFile::getUrl),
            )
        }
    }

    private fun librarySourceModuleFor(file: VirtualFile): CaIdeLibrarySourceModule {
        val identity = resolveLibraryIdentity(file)
        val binaryModule = libraryBinaryModuleFor(file)
        return librarySourceModulesByKey.computeIfAbsent(identity.sourceKey(binaryModule)) {
            val sourceRoots = identity.sourceRoots.mapNotNull(::toPsiFileSystemItem)
            check(sourceRoots.isNotEmpty()) {
                "库 `${identity.libraryName}` 没有可用的源码根，无法构建 IDE 库源码模块。"
            }
            CaIdeLibrarySourceModule(
                project = project,
                libraryName = identity.libraryName,
                binaryLibraryModule = binaryModule,
                sourceRoots = sourceRoots,
                sourceRootUrls = identity.sourceRoots.map(VirtualFile::getUrl),
            )
        }.also { module ->
            refreshLibrarySourceDependencies(module, file, currentSnapshotStamp())
        }
    }

    private fun danglingFileModuleFor(file: CjFile): CaIdeDanglingFileModule {
        val virtualFile = file.virtualFile
            ?: error("代码片段 `${file.name}` 缺少 VirtualFile，无法构建游离文件模块。")
        val pathKey = virtualFile.path.ifBlank { virtualFile.url }
        return danglingFileModulesByPath.computeIfAbsent(pathKey) {
            val contextModule = (file as? CjCodeFragment)?.context?.let { contextElement ->
                getModule(contextElement, null)
            }
            CaIdeDanglingFileModule(
                project = project,
                item = file,
                pathKey = pathKey,
                contextModule = contextModule,
            )
        }.also { module ->
            refreshDanglingDependencies(module, virtualFile, currentSnapshotStamp())
        }
    }

    private fun outsideContentModuleFor(item: PsiFileSystemItem): CaIdeNotUnderContentRootModule {
        val virtualFile = item.virtualFile
            ?: error("无法为不在 content root 下的 PSI 构建模块：缺少 VirtualFile。")
        return outsideContentModulesByPath.computeIfAbsent(virtualFile.path) {
            CaIdeNotUnderContentRootModule(
                project = project,
                item = item,
                pathKey = virtualFile.path,
            )
        }.also { module ->
            refreshOutsideContentDependencies(module, virtualFile, currentSnapshotStamp())
        }
    }

    /**
     * 从 IntelliJ order entry 恢复 Analysis API 的直接依赖边界。
     *
     * 这里不把依赖简单压成一组 VirtualFile，而是显式恢复：
     * - 项目 source module
     * - library source module
     * - library binary module
     *
     * 脚本依赖模块和 fallback 模块会在更外层再包装一次，避免上层误把它们当成源码 use-site 模块。
     */
    private fun regularDependenciesFor(owner: CaModule, anchorFile: VirtualFile): List<CaModule> {
        val dependencies = linkedSetOf<CaModule>()
        for (orderEntry in projectFileIndex.getOrderEntriesForFile(anchorFile)) {
            val sourceRoots = orderEntry.getFiles(OrderRootType.SOURCES).toList()
            val classRoots = orderEntry.getFiles(OrderRootType.CLASSES).toList()

            sourceRoots.forEach { root ->
                when {
                    projectFileIndex.isInLibrarySource(root) -> dependencies += librarySourceModuleFor(root)
                    projectFileIndex.getSourceRootForFile(root) != null -> dependencies += sourceModuleFor(root)
                }
            }

            classRoots.forEach { root ->
                dependencies += libraryBinaryModuleFor(root)
            }
        }

        dependencies -= owner
        dependencies -= builtinsModule
        return dependencies.toList()
    }

    /**
     * 以结构时间戳驱动依赖刷新，避免同一轮模块装配发生递归重入。
     *
     * source root / library source / fallback module 会在恢复 IntelliJ order entry 时
     * 彼此再次访问模块工厂。如果这里对每次访问都直接 clear + rebuild，遇到
     * `A -> B -> A` 或 “同 root 自回访” 就会无限递归。
     */
    private fun refreshRegularDependencies(
        module: CaIdeMutableModule,
        anchorFile: VirtualFile,
        snapshotStamp: SnapshotStamp,
    ) {
        if (!module.tryStartDependencyRefresh(snapshotStamp)) {
            return
        }

        try {
            module.directRegularDependencies.clear()
            module.directRegularDependencies += regularDependenciesFor(module, anchorFile)
            module.finishDependencyRefresh(snapshotStamp)
        } catch (throwable: Throwable) {
            module.resetDependencyRefresh(snapshotStamp)
            throw throwable
        }
    }

    private fun refreshLibrarySourceDependencies(
        module: CaIdeLibrarySourceModule,
        anchorFile: VirtualFile,
        snapshotStamp: SnapshotStamp,
    ) {
        refreshRegularDependencies(module, anchorFile, snapshotStamp)
        if (module.binaryLibraryModule !in module.directRegularDependencies) {
            module.directRegularDependencies.add(0, module.binaryLibraryModule)
        }
    }

    private fun refreshDanglingDependencies(
        module: CaIdeDanglingFileModule,
        anchorFile: VirtualFile,
        snapshotStamp: SnapshotStamp,
    ) {
        module.directRegularDependencies.clear()
        val fallbackModule = fallbackDependencyModuleFor(module, anchorFile, snapshotStamp)
        if (fallbackModule.directRegularDependencies.isNotEmpty()) {
            module.directRegularDependencies += fallbackModule
        }
    }

    private fun refreshOutsideContentDependencies(
        module: CaIdeNotUnderContentRootModule,
        anchorFile: VirtualFile,
        snapshotStamp: SnapshotStamp,
    ) {
        module.directRegularDependencies.clear()
        val fallbackModule = fallbackDependencyModuleFor(module, anchorFile, snapshotStamp)
        if (fallbackModule.directRegularDependencies.isNotEmpty()) {
            module.directRegularDependencies += fallbackModule
        }
    }

    private fun fallbackDependencyModuleFor(
        owner: CaModule,
        anchorFile: VirtualFile,
        snapshotStamp: SnapshotStamp,
    ): CaIdeLibraryFallbackDependenciesModule {
        val ownerKey = owner.stableModuleName ?: owner.moduleDescription
        return fallbackDependencyModulesByOwnerKey.computeIfAbsent(ownerKey) {
            CaIdeLibraryFallbackDependenciesModule(
                project = project,
                dependencyOwnerName = owner.moduleDescription,
                ownerStableName = ownerKey,
            )
        }.also { fallbackModule ->
            refreshRegularDependencies(fallbackModule, anchorFile, snapshotStamp)
        }
    }

    private fun resolveLibraryIdentity(file: VirtualFile): LibraryIdentity {
        val orderEntries = projectFileIndex.getOrderEntriesForFile(file)
        val sourceRoots = linkedSetOf<VirtualFile>()
        val binaryRoots = linkedSetOf<VirtualFile>()
        var presentableName: String? = null

        for (orderEntry in orderEntries) {
            if (presentableName == null) {
                presentableName = orderEntry.presentableName
            }
            sourceRoots += orderEntry.getFiles(OrderRootType.SOURCES)
            binaryRoots += orderEntry.getFiles(OrderRootType.CLASSES)
        }

        if (projectFileIndex.isInLibrarySource(file)) {
            projectFileIndex.getSourceRootForFile(file)?.let(sourceRoots::add)
        }
        if (projectFileIndex.isInLibraryClasses(file)) {
            projectFileIndex.getClassRootForFile(file)?.let(binaryRoots::add)
        }

        check(sourceRoots.isNotEmpty() || binaryRoots.isNotEmpty()) {
            "文件 `${file.path}` 位于库范围内，但无法从 IDE root model 恢复库根。"
        }

        val libraryName = presentableName
            ?.takeIf(String::isNotBlank)
            ?: binaryRoots.firstOrNull()?.name
            ?: sourceRoots.first().name

        return LibraryIdentity(
            libraryName = libraryName,
            sourceRoots = sourceRoots.sortedBy(VirtualFile::getUrl),
            binaryRoots = binaryRoots.sortedBy(VirtualFile::getUrl),
        )
    }

    private fun isDanglingLikeFile(file: CjFile): Boolean {
        return file.isCodeFragment || file is CjCodeFragment || !file.isPhysical
    }

    private fun toPsiFileSystemItem(file: VirtualFile): PsiFileSystemItem? {
        return psiManager.findDirectory(file) ?: psiManager.findFile(file)
    }

    private data class ModuleRootEntry(
        val root: PsiFileSystemItem,
        val module: CaIdeSourceModule,
    )

    private data class LibraryIdentity(
        val libraryName: String,
        val sourceRoots: List<VirtualFile>,
        val binaryRoots: List<VirtualFile>,
    ) {
        val binaryKey: LibraryBinaryKey
            get() = LibraryBinaryKey(
                libraryName = libraryName,
                binaryRootUrls = binaryRoots.map(VirtualFile::getUrl),
            )

        fun sourceKey(binaryModule: CaIdeLibraryModule): LibrarySourceKey {
            return LibrarySourceKey(
                libraryName = libraryName,
                sourceRootUrls = sourceRoots.map(VirtualFile::getUrl),
                binaryModuleStableName = binaryModule.stableModuleName,
            )
        }
    }

    private data class LibraryBinaryKey(
        val libraryName: String,
        val binaryRootUrls: List<String>,
    )

    private data class LibrarySourceKey(
        val libraryName: String,
        val sourceRootUrls: List<String>,
        val binaryModuleStableName: String,
    )

}

private data class SnapshotStamp(
    val psiModificationCount: Long,
    val explicitInvalidationCount: Long,
)

private data class CachedSnapshotState(
    val stamp: SnapshotStamp,
    val snapshot: CaProjectStructureSnapshot,
)

/**
 * IDE 平台模块的公共基类。
 *
 * IDE 项目结构需要在模块图装配阶段回填依赖，因此这里显式暴露可变依赖集合。
 * 这些集合只在平台结构服务内部维护，对 Analysis API 消费方仍然表现为只读视图。
 */
private sealed class CaIdeMutableModule(
    final override val project: Project,
    private val scopeRoots: List<PsiFileSystemItem>,
    private val includeLibrariesInScope: Boolean,
) : CaModule {
    final override val directRegularDependencies: MutableList<CaModule> = mutableListOf()
    final override val directDependsOnDependencies: MutableList<CaModule> = mutableListOf()
    final override val directFriendDependencies: MutableList<CaModule> = mutableListOf()
    private val dependencyRefreshLock = Any()
    @Volatile
    private var dependencyRefreshState: DependencyRefreshState = DependencyRefreshState.Idle

    final override val targetPlatform: CaTargetPlatform
        get() = CaTargetPlatform.IDE

    final override val baseContentScope: GlobalSearchScope =
        if (includeLibrariesInScope) {
            GlobalSearchScope.filesWithLibrariesScope(project, scopeRoots.mapNotNull { it.virtualFile })
        } else {
            GlobalSearchScope.filesWithoutLibrariesScope(project, scopeRoots.mapNotNull { it.virtualFile })
        }

    fun tryStartDependencyRefresh(snapshotStamp: SnapshotStamp): Boolean {
        synchronized(dependencyRefreshLock) {
            return when (val state = dependencyRefreshState) {
                is DependencyRefreshState.Completed ->
                    if (state.snapshotStamp == snapshotStamp) {
                        false
                    } else {
                        dependencyRefreshState = DependencyRefreshState.Refreshing(snapshotStamp)
                        true
                    }

                is DependencyRefreshState.Refreshing ->
                    if (state.snapshotStamp == snapshotStamp) {
                        false
                    } else {
                        dependencyRefreshState = DependencyRefreshState.Refreshing(snapshotStamp)
                        true
                    }

                DependencyRefreshState.Idle -> {
                    dependencyRefreshState = DependencyRefreshState.Refreshing(snapshotStamp)
                    true
                }
            }
        }
    }

    fun finishDependencyRefresh(snapshotStamp: SnapshotStamp) {
        synchronized(dependencyRefreshLock) {
            dependencyRefreshState = DependencyRefreshState.Completed(snapshotStamp)
        }
    }

    fun resetDependencyRefresh(snapshotStamp: SnapshotStamp) {
        synchronized(dependencyRefreshLock) {
            val state = dependencyRefreshState
            if (state is DependencyRefreshState.Refreshing && state.snapshotStamp == snapshotStamp) {
                dependencyRefreshState = DependencyRefreshState.Idle
            }
        }
    }

    private sealed interface DependencyRefreshState {
        data object Idle : DependencyRefreshState
        data class Refreshing(val snapshotStamp: SnapshotStamp) : DependencyRefreshState
        data class Completed(val snapshotStamp: SnapshotStamp) : DependencyRefreshState
    }
}

/**
 * IDE 中的项目源码模块。
 */
private class CaIdeSourceModule(
    project: Project,
    private val root: PsiFileSystemItem,
    private val sourceRootUrl: String,
    private val sourceRootPath: String,
) : CaIdeMutableModule(project, listOf(root), includeLibrariesInScope = false), CaSourceModule {
    override val name: String
        get() = root.name.ifBlank { sourceRootPath.substringAfterLast('/', sourceRootPath) }

    override val languageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettings.DEFAULT

    override val psiRoots: List<PsiFileSystemItem>
        get() = listOf(root)

    override val stableModuleName: String
        get() = "ide-source:$sourceRootUrl"

    override val moduleDescription: String
        get() = "IDE source root $sourceRootPath"
}

/**
 * IDE 中的库二进制模块。
 */
private class CaIdeLibraryModule(
    project: Project,
    override val libraryName: String,
    override val binaryRoots: List<PsiFileSystemItem>,
    private val binaryRootUrls: List<String>,
) : CaIdeMutableModule(project, binaryRoots, includeLibrariesInScope = true), CaLibraryModule {
    override val stableModuleName: String
        get() = "ide-library:${binaryRootUrls.joinToString(separator = "|")}"

    override val isResolvable: Boolean
        get() = false

    override val moduleDescription: String
        get() = "IDE library binaries $libraryName"
}

/**
 * IDE 中的库源码模块。
 *
 * 库源码不是普通项目源码，它必须通过 [binaryLibraryModule] 与真实库产物绑定，
 * 否则 low-level resolve 会把“查看库源码”误判为“项目源码 use-site”。
 */
private class CaIdeLibrarySourceModule(
    project: Project,
    override val libraryName: String,
    override val binaryLibraryModule: CaLibraryModule,
    override val sourceRoots: List<PsiFileSystemItem>,
    private val sourceRootUrls: List<String>,
) : CaIdeMutableModule(project, sourceRoots, includeLibrariesInScope = true), CaLibrarySourceModule {
    override val stableModuleName: String
        get() = "ide-library-source:${sourceRootUrls.joinToString(separator = "|")}"

    override val moduleDescription: String
        get() = "IDE library sources $libraryName"
}

/**
 * IDE 中的 fallback 依赖模块。
 *
 * 该模块用于 outside-content-root / dangling-file 这类没有稳定内容根的 use-site 场景，
 * 只表达默认可见依赖边界，不伪装成真正源码模块。
 */
private class CaIdeLibraryFallbackDependenciesModule(
    project: Project,
    override val dependencyOwnerName: String,
    private val ownerStableName: String,
) : CaIdeMutableModule(project, emptyList(), includeLibrariesInScope = true), CaLibraryFallbackDependenciesModule {
    override val stableModuleName: String
        get() = "ide-fallback:$ownerStableName"

    override val isResolvable: Boolean
        get() = false

    override val moduleDescription: String
        get() = "IDE fallback dependencies of $dependencyOwnerName"
}

/**
 * IDE 中的内建模块。
 */
private class CaIdeBuiltinsModule(
    project: Project,
) : CaIdeMutableModule(project, emptyList(), includeLibrariesInScope = true), CaBuiltinsModule {
    override val builtinsName: String
        get() = "<ide-builtins>"

    override val stableModuleName: String
        get() = "ide-builtins"

    override val isResolvable: Boolean
        get() = false

    override val contentScope: GlobalSearchScope
        get() = CaBuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)

    override val moduleDescription: String
        get() = "IDE builtins"
}

/**
 * IDE 中的游离文件模块。
 */
private class CaIdeDanglingFileModule(
    project: Project,
    val item: CjFile,
    private val pathKey: String,
    override val contextModule: CaModule?,
) : CaIdeMutableModule(project, listOf(item), includeLibrariesInScope = false), CaDanglingFileModule {
    override val name: String
        get() = item.name.ifBlank { pathKey.substringAfterLast('/', pathKey) }

    override val languageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettings.DEFAULT

    override val psiRoots: List<PsiFileSystemItem>
        get() = listOf(item)

    override val stableModuleName: String
        get() = "ide-dangling:$pathKey"

    override val moduleDescription: String
        get() = "IDE dangling file $pathKey"
}

/**
 * IDE 中的不在 content root 下的临时模块。
 */
private class CaIdeNotUnderContentRootModule(
    project: Project,
    val item: PsiFileSystemItem,
    private val pathKey: String,
) : CaIdeMutableModule(project, listOf(item), includeLibrariesInScope = false), CaNotUnderContentRootModule {
    override val name: String
        get() = item.name.ifBlank { pathKey.substringAfterLast('/', pathKey) }

    override val originalModule: CaModule?
        get() = null

    override val stableModuleName: String
        get() = "ide-outside:$pathKey"

    override val moduleDescription: String
        get() = "IDE not-under-content-root $pathKey"
}

/**
 * IDE 平台项目结构提供器。
 *
 * 平台接口层不直接持有状态，而是统一委托给 [CaIdeProjectStructureState]，
 * 与 standalone / LSP 一样遵循“单一状态源 + 多接口委托”的边界。
 */
class CaIdeProjectStructureProvider(
    project: Project,
) : CaProjectStructureProvider {
    private val state = project.getService(CaIdeProjectStructureState::class.java)

    override val snapshot: CaProjectStructureSnapshot
        get() = state.snapshot

    override fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule =
        state.getModule(element, useSiteModule)
}

/**
 * IDE 平台模块图提供器。
 */
class CaIdeModuleProvider(
    project: Project,
) : CaModuleProvider {
    private val state = project.getService(CaIdeProjectStructureState::class.java)

    override val snapshot: CaProjectStructureSnapshot
        get() = state.snapshot

    override fun getModuleByStableName(stableModuleName: String): CaModule? {
        return state.getModuleByStableName(stableModuleName)
    }
}

/**
 * IDE 平台内容作用域裁剪服务。
 */
class CaIdeContentScopeRefiner(
    project: Project,
) : CaContentScopeRefiner {
    private val state = project.getService(CaIdeProjectStructureState::class.java)

    override fun getRefinedContentScope(module: CaModule, baseContentScope: GlobalSearchScope): GlobalSearchScope =
        state.getRefinedContentScope(module, baseContentScope)
}

/**
 * IDE 平台修改计数服务。
 *
 * 对外暴露的是 Analysis API 视角下的统一修改计数，
 * 内部由项目结构状态源统一整合 PSI 变化与显式失效。
 */
class CaIdeModificationTracker(
    project: Project,
) : CaModificationTracker {
    private val state = project.getService(CaIdeProjectStructureState::class.java)

    override val modificationCount: Long
        get() = state.modificationCount

    override fun getModuleModificationCount(module: CaModule): Long =
        state.getModuleModificationCount(module)
}

/**
 * IDE 平台 session 失效服务。
 */
class CaIdeSessionInvalidationService(
    project: Project,
) : CaSessionInvalidationService {
    private val state = project.getService(CaIdeProjectStructureState::class.java)

    override fun invalidate(modules: Set<CaModule>) {
        state.invalidate(modules)
    }
}
