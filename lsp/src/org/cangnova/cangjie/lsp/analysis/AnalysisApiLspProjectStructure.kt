package org.cangnova.cangjie.lsp.analysis

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.analysis.api.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.CaSourceModule
import org.cangnova.cangjie.analysis.api.CaTargetPlatform
import org.cangnova.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionChecker
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaContentScopeRefiner
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureSnapshot
import org.cangnova.cangjie.analysis.api.platform.restrictedAnalysis.CaRestrictedAnalysisService
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.state.LspWorkspaceModuleDefinition
import org.cangnova.cangjie.lsp.state.LspWorkspaceState
import org.cangnova.cangjie.lsp.state.uriToPathOrNull
import org.cangnova.cangjie.psi.CjFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.isRegularFile

/**
 * LSP 平台下的项目结构事实源。
 *
 * 这里统一维护两类模块：
 * 1. 工作区源码模块：来自 `initialize` / `workspaceFolders` 协商出的真实工程模块；
 * 2. 打开文档快照模块：承载当前编辑中的内存文本，并通过 `contextModule` 回挂到所属工作区模块。
 *
 * LSP 外层的文档事件、工作区事件与 Analysis API 语义查询都必须围绕这份状态工作，
 * 不允许再由不同服务各自推导“当前模块”“可见文件”或“失效边界”。
 */
class AnalysisApiLspProjectStructureState(
    private val project: Project,
) : CaSessionInvalidationService {
    private data class OpenDocumentSnapshotEntry(
        val document: LspTextDocument,
        val psiFile: CjFile,
        val normalizedPath: Path?,
    )

    private data class DanglingSnapshotEntry(
        val document: LspTextDocument,
        val module: CaLspDanglingFileModule,
        val psiFile: CjFile,
    )

    private data class WorkspaceModuleEntry(
        val definition: LspWorkspaceModuleDefinition,
        val module: CaLspSourceModule,
        val sourceFiles: List<CjFile>,
        val sourceDocumentUris: List<String>,
        val sourceRootPaths: List<Path>,
        val documentUris: Set<String>,
    )

    private data class WorkspaceSourceFileEntry(
        val path: Path,
        val psiFile: CjFile,
        val contentVirtualFile: VirtualFile?,
        val documentUri: String?,
    )

    private val psiManager: PsiManager = PsiManager.getInstance(project)
    private val globalModificationCount = AtomicLong(0)
    private val moduleModificationCounts = ConcurrentHashMap<CaModule, AtomicLong>()

    private val workspaceModulesByName = linkedMapOf<String, WorkspaceModuleEntry>()
    private val workspaceModuleByDocumentUri = ConcurrentHashMap<String, CaLspSourceModule>()
    private val openSnapshotsByUri = ConcurrentHashMap<String, OpenDocumentSnapshotEntry>()
    private val danglingSnapshotsByUri = ConcurrentHashMap<String, DanglingSnapshotEntry>()
    private val documentUriByPsiFile = ConcurrentHashMap<CjFile, String>()
    private val modulesByPsiFile = ConcurrentHashMap<CjFile, CaModule>()

    val allModules: List<CaModule>
        get() = buildList {
            addAll(workspaceModulesByName.values.map(WorkspaceModuleEntry::module))
            addAll(danglingSnapshotsByUri.values.map(DanglingSnapshotEntry::module))
        }

    val allSourceFiles: List<PsiFileSystemItem>
        get() = buildList {
            workspaceModulesByName.values.flatMapTo(this) { it.sourceFiles }
            danglingSnapshotsByUri.values.mapTo(this) { it.psiFile }
        }.distinct()

    val allResolvableModules: List<CaModule>
        get() = allModules.filter(CaModule::isResolvable)

    val allSourceLikeModules: List<CaModule>
        get() = allModules.filterIsInstance<CaSourceModule>()

    val snapshot: CaProjectStructureSnapshot
        get() = CaProjectStructureSnapshot(
            allModules = allModules,
            allResolvableModules = allResolvableModules,
            allSourceLikeModules = allSourceLikeModules,
            allSourceFiles = allSourceFiles,
        )

    val modificationCount: Long
        get() = globalModificationCount.get()

    /**
     * 根据当前工作区定义和打开文档状态重建工作区模块图。
     *
     * 打开文档对应的磁盘文件会从工作区源码枚举中显式排除，由 snapshot 模块承载；
     * 这样同一份源码在任一时刻只会有一个真实 use-site 入口，不会同时混入磁盘旧文本和内存快照。
     */
    @Synchronized
    internal fun configure(workspaceState: LspWorkspaceState) {
        val previousModules = buildSet {
            addAll(workspaceModulesByName.values.map(WorkspaceModuleEntry::module))
            addAll(danglingSnapshotsByUri.values.map(DanglingSnapshotEntry::module))
        }
        val newEntries = buildWorkspaceModuleEntries(
            definitions = workspaceState.projectConfiguration().workspaceModules,
            openSnapshots = openSnapshotsByUri.values.toList(),
        )
        val workspaceDocumentUris = newEntries.flatMapTo(linkedSetOf()) { it.documentUris }

        workspaceModulesByName.clear()
        workspaceModuleByDocumentUri.clear()
        danglingSnapshotsByUri.clear()
        documentUriByPsiFile.clear()
        modulesByPsiFile.clear()

        newEntries.forEach { entry ->
            workspaceModulesByName[entry.definition.name] = entry
            entry.sourceFiles.forEach { psiFile ->
                modulesByPsiFile[psiFile] = entry.module
            }
            entry.sourceFiles.zip(entry.sourceDocumentUris).forEach { (psiFile, documentUri) ->
                documentUriByPsiFile[psiFile] = documentUri
            }
            entry.documentUris.forEach { documentUri ->
                workspaceModuleByDocumentUri[documentUri] = entry.module
            }
        }
        val danglingEntries = buildDanglingSnapshotEntries(
            openSnapshots = openSnapshotsByUri.values
                .filterNot { snapshot -> snapshot.document.uri in workspaceDocumentUris }
                .sortedBy { snapshot -> snapshot.document.uri },
        )
        danglingEntries.forEach { entry ->
            danglingSnapshotsByUri[entry.document.uri] = entry
            modulesByPsiFile[entry.psiFile] = entry.module
            documentUriByPsiFile[entry.psiFile] = entry.document.uri
        }

        val currentModules = buildSet {
            addAll(workspaceModulesByName.values.map(WorkspaceModuleEntry::module))
            addAll(danglingEntries.map(DanglingSnapshotEntry::module))
        }
        invalidate(previousModules + currentModules)
    }

    /**
     * 为任意 PSI 元素恢复 use-site 模块。
     *
     * 只允许使用显式注册的快照模块或根据真实磁盘路径回溯到工作区源码模块，
     * 不允许退化为“随便取一个模块”的兜底行为。
     */
    internal fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        useSiteModule?.let { return it }

        val containingFile = element.containingFile as? CjFile
            ?: error("LSP Analysis API 只能为带有仓颉源码文件的元素恢复 use-site 模块：${element.javaClass.simpleName}")

        return getModuleForFile(containingFile)
            ?: error("LSP Analysis API 未能为 `${containingFile.name}` 恢复所属模块，项目结构与快照状态不一致。")
    }

    internal fun getRefinedContentScope(module: CaModule, baseContentScope: GlobalSearchScope): GlobalSearchScope = baseContentScope

    internal fun getModuleModificationCount(module: CaModule): Long {
        return moduleModificationCounts[module]?.get() ?: modificationCount
    }

    internal fun getModuleByStableName(stableModuleName: String): CaModule? {
        return allModules.firstOrNull { it.stableModuleName == stableModuleName }
    }

    /**
     * 统一恢复 PSI 文件所属模块。
     *
     * 这里的恢复顺序只有两种稳定来源：
     * 1. 打开文档对应的快照模块；
     * 2. 根据磁盘路径回溯到的工作区源码模块。
     */
    internal fun getModuleForFile(file: CjFile): CaModule? {
        modulesByPsiFile[file]?.let { return it }

        val virtualFile = file.virtualFile ?: return null
        val workspaceModule = findWorkspaceModuleForPath(Path.of(virtualFile.path).normalize()) ?: return null
        modulesByPsiFile.putIfAbsent(file, workspaceModule)
        return workspaceModule
    }

    /**
     * 将 PSI 文件稳定映射回文档 URI。
     *
     * 打开文档优先返回真实 LSP 文档 URI；工作区磁盘文件则回退到规范化后的 `file:` URI。
     */
    internal fun documentUriOf(psiFile: CjFile): String? {
        documentUriByPsiFile[psiFile]?.let { return it }
        return psiFile.virtualFile?.path?.let(Path::of)?.toUri()?.toString()
    }

    /**
     * 注册或更新打开文档的 PSI 快照。
     *
     * 这里仅维护“当前文本 -> PSI”的事实，不在此处直接决定模块形态。
     * 工作区内 overlay 与工作区外 dangling 的区分统一延迟到 [configure]。
     */
    @Synchronized
    internal fun upsertOpenDocumentSnapshot(
        document: LspTextDocument,
        psiFile: CjFile,
    ) {
        val previousEntry = openSnapshotsByUri.put(
            document.uri,
            OpenDocumentSnapshotEntry(
                document = document,
                psiFile = psiFile,
                normalizedPath = document.uri.uriToPathOrNull()?.normalize(),
            ),
        )
        previousEntry?.psiFile?.let(documentUriByPsiFile::remove)
        documentUriByPsiFile[psiFile] = document.uri
    }

    /**
     * 删除打开文档的 PSI 快照。
     *
     * 文档关闭后，工作区内文件会在下一次 [configure] 时自动回退到磁盘 PSI；
     * 工作区外文档则不再暴露为 dangling module。
     */
    @Synchronized
    internal fun removeOpenDocumentSnapshot(uri: String) {
        val removedEntry = openSnapshotsByUri.remove(uri) ?: return
        documentUriByPsiFile.remove(removedEntry.psiFile)
    }

    internal fun openDocumentSnapshot(uri: String): LspOpenDocumentAnalysisSnapshot? {
        val entry = openSnapshotsByUri[uri] ?: return null
        return LspOpenDocumentAnalysisSnapshot(
            document = entry.document,
            psiFile = entry.psiFile,
        )
    }

    internal fun useSiteModuleForOpenDocument(uri: String): CaModule? {
        workspaceModuleByDocumentUri[uri]?.let { return it }
        return danglingSnapshotsByUri[uri]?.module
    }

    override fun invalidate(modules: Set<CaModule>) {
        if (modules.isEmpty()) return

        globalModificationCount.incrementAndGet()
        modules.forEach { module ->
            moduleModificationCounts.computeIfAbsent(module) { AtomicLong(0) }.incrementAndGet()
        }

        val delegatedInvalidationService = project.getService(CaSessionProvider::class.java) as? CaSessionInvalidationService
        if (delegatedInvalidationService != null && delegatedInvalidationService !== this) {
            delegatedInvalidationService.invalidate(modules)
        }
    }

    private fun buildWorkspaceModuleEntries(
        definitions: List<LspWorkspaceModuleDefinition>,
        openSnapshots: List<OpenDocumentSnapshotEntry>,
    ): List<WorkspaceModuleEntry> {
        val openSnapshotsByPath = linkedMapOf<Path, OpenDocumentSnapshotEntry>()
        openSnapshots.forEach { snapshot ->
            snapshot.normalizedPath?.let { path -> openSnapshotsByPath[path] = snapshot }
        }

        return definitions.map { definition ->
            val sourceRootPaths = definition.sourceRootUris
                .mapNotNull(String::uriToPathOrNull)
                .map(Path::normalize)
                .distinct()
            val sourceFileEntries = sourceRootPaths
                .asSequence()
                .flatMap { root -> collectWorkspaceSourceFiles(root, openSnapshotsByPath).asSequence() }
                .distinctBy(WorkspaceSourceFileEntry::path)
                .sortedBy { entry -> entry.path.toString() }
                .toList()
            val sourceFiles = sourceFileEntries.map(WorkspaceSourceFileEntry::psiFile)

            val module = CaLspSourceModule(
                project = project,
                name = definition.name,
                psiRoots = sourceFiles,
                sourceRootPaths = sourceRootPaths,
            )
            WorkspaceModuleEntry(
                definition = definition,
                module = module,
                sourceFiles = sourceFiles,
                sourceDocumentUris = sourceFileEntries.map { fileEntry ->
                    fileEntry.documentUri ?: fileEntry.path.toUri().toString()
                },
                sourceRootPaths = sourceRootPaths,
                documentUris = sourceFileEntries.mapNotNull(WorkspaceSourceFileEntry::documentUri).toSet(),
            )
        }
    }

    private fun buildDanglingSnapshotEntries(
        openSnapshots: List<OpenDocumentSnapshotEntry>,
    ): List<DanglingSnapshotEntry> {
        return openSnapshots.map { snapshot ->
            val contextModule = snapshot.normalizedPath?.let(::findWorkspaceModuleForPath)
            DanglingSnapshotEntry(
                document = snapshot.document,
                module = CaLspDanglingFileModule(
                    project = project,
                    documentUri = snapshot.document.uri,
                    psiFile = snapshot.psiFile,
                    contextModule = contextModule,
                    contentVirtualFile = snapshot.normalizedPath?.let(::findLocalVirtualFile),
                ),
                psiFile = snapshot.psiFile,
            )
        }
    }

    private fun collectWorkspaceSourceFiles(
        root: Path,
        openSnapshotsByPath: Map<Path, OpenDocumentSnapshotEntry>,
    ): List<WorkspaceSourceFileEntry> {
        if (!Files.exists(root)) return emptyList()

        val candidatePaths = linkedSetOf<Path>()
        candidatePaths += collectSourceFilePaths(root)
        openSnapshotsByPath.keys
            .filter { path -> isUnder(root, path) && path.isCangjieSourceFilePath() && Files.exists(path) }
            .forEach(candidatePaths::add)

        return candidatePaths.mapNotNull { filePath ->
            val normalizedPath = filePath.normalize()
            val openSnapshot = openSnapshotsByPath[normalizedPath]
            if (openSnapshot != null) {
                return@mapNotNull WorkspaceSourceFileEntry(
                    path = normalizedPath,
                    psiFile = openSnapshot.psiFile,
                    contentVirtualFile = findLocalVirtualFile(normalizedPath),
                    documentUri = openSnapshot.document.uri,
                )
            }

            val virtualFile = findLocalVirtualFile(normalizedPath)
            val psiFile = when {
                virtualFile != null -> (psiManager.findFile(virtualFile) as? CjFile) ?: createDiskPsiFile(normalizedPath)
                else -> createDiskPsiFile(normalizedPath)
            } ?: return@mapNotNull null
            WorkspaceSourceFileEntry(
                path = normalizedPath,
                psiFile = psiFile,
                contentVirtualFile = virtualFile,
                documentUri = normalizedPath.toUri().toString(),
            )
        }
    }

    private fun collectSourceFilePaths(root: Path): List<Path> {
        if (!Files.exists(root)) return emptyList()

        return if (Files.isDirectory(root)) {
            Files.walk(root).use { stream ->
                stream
                    .filter { path -> path.isRegularFile() }
                    .filter { path -> path.isCangjieSourceFilePath() }
                    .map(Path::normalize)
                    .toList()
            }
        } else {
            listOf(root.normalize()).filter(Path::isCangjieSourceFilePath)
        }
    }

    private fun findWorkspaceModuleForUri(uri: String): CaLspSourceModule? {
        val targetPath = uri.uriToPathOrNull()?.normalize() ?: return null
        return findWorkspaceModuleForPath(targetPath)
    }

    private fun findWorkspaceModuleForPath(targetPath: Path): CaLspSourceModule? {
        return workspaceModulesByName.values
            .sortedByDescending { entry -> entry.sourceRootPaths.maxOfOrNull { root -> root.toString().length } ?: 0 }
            .firstOrNull { entry -> entry.sourceRootPaths.any { root -> isUnder(root, targetPath) } }
            ?.module
    }

    private fun isUnder(
        root: Path,
        target: Path,
    ): Boolean {
        val normalizedRoot = root.normalize()
        return target == normalizedRoot || target.startsWith(normalizedRoot)
    }

    private fun findLocalVirtualFile(path: Path): VirtualFile? {
        val localFileSystem = StandardFileSystems.local()
        return localFileSystem.findFileByPath(path.toString())
            ?: localFileSystem.refreshAndFindFileByPath(path.toString())
    }

    private fun createDiskPsiFile(path: Path): CjFile? {
        if (!Files.exists(path) || !path.isRegularFile()) return null
        val psiFile = PsiFileFactory.getInstance(project).createFileFromText(
            path.fileName.toString(),
            CangJieFileType.INSTANCE,
            Files.readString(path),
        )
        return psiFile as? CjFile
    }

    companion object {
        fun getInstance(project: Project): AnalysisApiLspProjectStructureState = project.service()
    }
}

internal data class LspOpenDocumentAnalysisSnapshot(
    val document: LspTextDocument,
    val psiFile: CjFile,
)

/**
 * LSP 工作区源码模块。
 *
 * 这里显式把 source roots 展开成可解析的 PSI 文件集合，让 Analysis API 看到的是一个真实源码模块，
 * 而不是只包含单文件快照的临时容器。
 */
internal class CaLspSourceModule(
    override val project: Project,
    override val name: String,
    psiRoots: List<PsiFileSystemItem>,
    private val sourceRootPaths: List<Path>,
) : CaSourceModule {
    override val languageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettings.DEFAULT

    override val psiRoots: List<PsiFileSystemItem> = psiRoots.toList()
    override val directRegularDependencies: MutableList<CaModule> = mutableListOf()
    override val directDependsOnDependencies: MutableList<CaModule> = mutableListOf()
    override val directFriendDependencies: MutableList<CaModule> = mutableListOf()

    override val targetPlatform: CaTargetPlatform
        get() = CaTargetPlatform.LSP

    override val baseContentScope: GlobalSearchScope =
        buildSourceModuleContentScope(project, sourceRootPaths)

    override val stableModuleName: String
        get() = "lsp-source:$name"

    override val moduleDescription: String
        get() = "LSP source module $name"
}

/**
 * 打开文档对应的 use-site 快照模块。
 *
 * 它只承载当前编辑中的内存文本，但通过 `contextModule` 明确指向所属工作区源码模块，
 * 这样单文件分析仍能进入真实工程依赖和可见性边界。
 */
internal class CaLspDanglingFileModule(
    override val project: Project,
    val documentUri: String,
    private val psiFile: CjFile,
    override val contextModule: CaModule?,
    private val contentVirtualFile: VirtualFile?,
) : CaDanglingFileModule {
    override val name: String
        get() = documentUri

    override val languageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettings.DEFAULT

    override val psiRoots: List<PsiFileSystemItem>
        get() = listOf(psiFile)

    override val directRegularDependencies: MutableList<CaModule> = mutableListOf()
    override val directDependsOnDependencies: MutableList<CaModule> = mutableListOf()
    override val directFriendDependencies: MutableList<CaModule> = mutableListOf()

    override val targetPlatform: CaTargetPlatform
        get() = CaTargetPlatform.LSP

    override val baseContentScope: GlobalSearchScope
        get() = GlobalSearchScope.filesWithoutLibrariesScope(project, listOfNotNull(contentVirtualFile ?: psiFile.virtualFile))

    override val stableModuleName: String
        get() = documentUri

    override val moduleDescription: String
        get() = "LSP dangling file $documentUri"
}

private fun buildSourceModuleContentScope(
    project: Project,
    sourceRootPaths: List<Path>,
): GlobalSearchScope {
    if (sourceRootPaths.isEmpty()) {
        return GlobalSearchScope.EMPTY_SCOPE
    }

    return object : GlobalSearchScope(project) {
        override fun contains(file: VirtualFile): Boolean {
            val filePath = runCatching { Path.of(file.path).normalize() }.getOrNull() ?: return false
            return sourceRootPaths.any { root ->
                filePath == root || filePath.startsWith(root)
            }
        }

        override fun isSearchInModuleContent(aModule: com.intellij.openapi.module.Module): Boolean = false

        override fun isSearchInLibraries(): Boolean = false
    }
}

internal class AnalysisApiLspProjectStructureProvider(
    private val project: Project,
) : CaProjectStructureProvider {
    private val state: AnalysisApiLspProjectStructureState
        get() = AnalysisApiLspProjectStructureState.getInstance(project)

    override fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        return state.getModule(element, useSiteModule)
    }

    override val allModules: List<CaModule>
        get() = state.allModules

    override val allResolvableModules: List<CaModule>
        get() = state.allResolvableModules

    override val allSourceLikeModules: List<CaModule>
        get() = state.allSourceLikeModules

    override val allSourceFiles: List<PsiFileSystemItem>
        get() = state.allSourceFiles

    override val snapshot: CaProjectStructureSnapshot
        get() = state.snapshot
}

internal class AnalysisApiLspModuleProvider(
    private val project: Project,
) : CaModuleProvider {
    private val state: AnalysisApiLspProjectStructureState
        get() = AnalysisApiLspProjectStructureState.getInstance(project)

    override val snapshot: CaProjectStructureSnapshot
        get() = state.snapshot

    override val allModules: List<CaModule>
        get() = state.allModules

    override val resolvableModules: List<CaModule>
        get() = state.allResolvableModules

    override val sourceLikeModules: List<CaModule>
        get() = state.allSourceLikeModules

    override fun getModuleByStableName(stableModuleName: String): CaModule? {
        return state.getModuleByStableName(stableModuleName)
    }
}

internal class AnalysisApiLspContentScopeRefiner(
    private val project: Project,
) : CaContentScopeRefiner {
    private val state: AnalysisApiLspProjectStructureState
        get() = AnalysisApiLspProjectStructureState.getInstance(project)

    override fun getRefinedContentScope(module: CaModule, baseContentScope: GlobalSearchScope): GlobalSearchScope {
        return state.getRefinedContentScope(module, baseContentScope)
    }
}

internal class AnalysisApiLspModificationTracker(
    private val project: Project,
) : CaModificationTracker {
    private val state: AnalysisApiLspProjectStructureState
        get() = AnalysisApiLspProjectStructureState.getInstance(project)

    override val modificationCount: Long
        get() = state.modificationCount

    override fun getModuleModificationCount(module: CaModule): Long {
        return state.getModuleModificationCount(module)
    }
}

/**
 * LSP 平台的 session 失效服务委托。
 *
 * Project 容器中只保留一份 [AnalysisApiLspProjectStructureState] 作为真实状态源，
 * 其余平台接口都通过这个委托转发，避免 MockProject 中出现多份结构状态。
 */
internal class AnalysisApiLspSessionInvalidationService(
    private val project: Project,
) : CaSessionInvalidationService {
    private val state: AnalysisApiLspProjectStructureState
        get() = AnalysisApiLspProjectStructureState.getInstance(project)

    override fun invalidate(modules: Set<CaModule>) {
        state.invalidate(modules)
    }
}

private fun Path.isCangjieSourceFilePath(): Boolean {
    val fileName = fileName?.toString().orEmpty()
    return fileName.endsWith(".cj") || fileName.endsWith(".cjs")
}

/**
 * LSP 平台的受限分析服务。
 */
internal class AnalysisApiLspRestrictedAnalysisService : CaRestrictedAnalysisService {
    override val isAnalysisRestricted: Boolean
        get() = false

    override val isRestrictedAnalysisAllowed: Boolean
        get() = true

    override fun rejectRestrictedAnalysis(): Nothing {
        error("LSP 平台当前未启用 restricted analysis，不应调用 rejectRestrictedAnalysis().")
    }
}

/**
 * LSP 平台设置。
 */
internal class AnalysisApiLspPlatformSettings : CaPlatformSettings {
    override val allowUseSiteLibraryModuleAnalysis: Boolean
        get() = false
}

/**
 * LSP 平台的分析权限检查器。
 */
internal class AnalysisApiLspPermissionChecker : CaAnalysisPermissionChecker {
    private val permissionRegistry by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaAnalysisPermissionRegistry.getInstance()
    }

    override fun isAnalysisAllowed(): Boolean {
        return permissionRegistry.explicitAnalysisRestriction == null
    }

    override fun getRejectionReason(): String {
        val restriction = permissionRegistry.explicitAnalysisRestriction
            ?: error("Cannot get a rejection reason when analysis is allowed.")
        return "Resolve is explicitly forbidden in the current action: ${restriction.description}."
    }
}
