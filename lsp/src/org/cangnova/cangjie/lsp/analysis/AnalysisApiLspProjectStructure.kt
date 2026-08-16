@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.lsp.analysis

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.analysis.api.permissions.CaAnalysisPermissionRegistry
import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionChecker
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleBase
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureSnapshot
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.platform.restrictedAnalysis.CaRestrictedAnalysisService
import org.cangnova.cangjie.analysis.api.session.CaSessionProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.state.LspWorkspaceModuleDefinition
import org.cangnova.cangjie.lsp.state.LspWorkspaceState
import org.cangnova.cangjie.lsp.state.uriToPathOrNull
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.psi.CjCodeFragment
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
    /**
     * 当前 LSP 环境中的 IntelliJ project。
     */
    private val project: Project,
) : CaSessionInvalidationService {
    /**
     * 打开文档对应的 PSI 快照条目。
     */
    private data class OpenDocumentSnapshotEntry(
        /**
         * 当前打开文档的协议层快照。
         */
        val document: LspTextDocument,

        /**
         * 当前文档版本构造出的仓颉 PSI 文件。
         */
        val psiFile: CjFile,

        /**
         * 文档 URI 解析后的规范化本地路径。
         */
        val normalizedPath: Path?,
    )

    /**
     * 工作区外打开文档对应的 dangling module 条目。
     */
    private data class DanglingSnapshotEntry(
        /**
         * 当前打开文档的协议层快照。
         */
        val document: LspTextDocument,

        /**
         * 承载该打开文档的 dangling file module。
         */
        val module: CaLspDanglingFileModule,

        /**
         * dangling module 中的 PSI 文件。
         */
        val psiFile: CjFile,
    )

    /**
     * 工作区源码模块及其可见源码文件索引。
     */
    private data class WorkspaceModuleEntry(
        /**
         * 客户端声明或推导出的模块定义。
         */
        val definition: LspWorkspaceModuleDefinition,

        /**
         * Analysis API 可见的 LSP 源码模块。
         */
        val module: CaLspSourceModule,

        /**
         * 该模块包含的仓颉 PSI 源文件。
         */
        val sourceFiles: List<CjFile>,

        /**
         * 与 [sourceFiles] 顺序对应的文档 URI。
         */
        val sourceDocumentUris: List<String>,

        /**
         * 该模块的规范化源码根路径。
         */
        val sourceRootPaths: List<Path>,

        /**
         * 该模块中由打开文档 overlay 覆盖的 URI 集合。
         */
        val documentUris: Set<String>,
    )

    /**
     * 工作区磁盘源码文件或 overlay 源码文件的枚举结果。
     */
    private data class WorkspaceSourceFileEntry(
        /**
         * 源文件的规范化本地路径。
         */
        val path: Path,

        /**
         * 源文件对应的仓颉 PSI 文件。
         */
        val psiFile: CjFile,

        /**
         * 磁盘源文件的 IntelliJ virtual file。
         */
        val contentVirtualFile: VirtualFile?,

        /**
         * 源文件对应的 LSP 文档 URI。
         */
        val documentUri: String?,
    )

    /**
     * 当前 project 的 PSI 管理器。
     */
    private val psiManager: PsiManager = PsiManager.getInstance(project)

    /**
     * 全局项目结构修改计数。
     */
    private val globalModificationCount = AtomicLong(0)

    /**
     * 按模块维护的修改计数。
     */
    private val moduleModificationCounts = ConcurrentHashMap<CaModule, AtomicLong>()

    /**
     * 以模块名称为键的当前工作区源码模块表。
     */
    private val workspaceModulesByName = linkedMapOf<String, WorkspaceModuleEntry>()

    /**
     * 打开文档 URI 到所属工作区源码模块的映射。
     */
    private val workspaceModuleByDocumentUri = ConcurrentHashMap<String, CaLspSourceModule>()

    /**
     * 当前打开文档 URI 到 PSI 快照的映射。
     */
    private val openSnapshotsByUri = ConcurrentHashMap<String, OpenDocumentSnapshotEntry>()

    /**
     * 当前 dangling 文档 URI 到 dangling module 的映射。
     */
    private val danglingSnapshotsByUri = ConcurrentHashMap<String, DanglingSnapshotEntry>()

    /**
     * PSI 文件到 LSP 文档 URI 的反向映射。
     */
    private val documentUriByPsiFile = ConcurrentHashMap<CjFile, String>()

    /**
     * PSI 文件到所属 Analysis API 模块的映射。
     */
    private val modulesByPsiFile = ConcurrentHashMap<CjFile, CaModule>()

    /**
     * 当前项目结构中所有 LSP 管理的模块。
     */
    val allModules: List<CaModule>
        get() = buildList {
            addAll(workspaceModulesByName.values.map(WorkspaceModuleEntry::module))
            addAll(danglingSnapshotsByUri.values.map(DanglingSnapshotEntry::module))
        }

    /**
     * 当前项目结构中所有可作为源码根输入的 PSI 文件系统项。
     */
    val allSourceFiles: List<PsiFileSystemItem>
        get() = buildList {
            workspaceModulesByName.values.flatMapTo(this) { it.sourceFiles }
            danglingSnapshotsByUri.values.mapTo(this) { it.psiFile }
        }.distinct()

    /**
     * 当前项目结构中可解析的模块集合。
     */
    val allResolvableModules: List<CaModule>
        get() = allModules.filter(CaModule::isResolvable)

    /**
     * 当前项目结构中的源码类模块集合。
     */
    val allSourceLikeModules: List<CaModule>
        get() = allModules.filterIsInstance<CaSourceModule>()

    /**
     * 暴露给 Analysis API 的项目结构快照。
     */
    val snapshot: CaProjectStructureSnapshot
        get() = CaProjectStructureSnapshot(
            allModules = allModules,
            allResolvableModules = allResolvableModules,
            allSourceLikeModules = allSourceLikeModules,
            allSourceFiles = allSourceFiles,
        )

    /**
     * 当前全局修改计数。
     */
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

    /**
     * 返回指定模块的修改计数。
     */
    internal fun getModuleModificationCount(module: CaModule): Long {
        return moduleModificationCounts[module]?.get() ?: modificationCount
    }

    /**
     * 根据稳定模块名查找当前项目结构中的模块。
     */
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

    /**
     * 查询指定 URI 当前打开文档的分析快照。
     */
    internal fun openDocumentSnapshot(uri: String): LspOpenDocumentAnalysisSnapshot? {
        val entry = openSnapshotsByUri[uri] ?: return null
        return LspOpenDocumentAnalysisSnapshot(
            document = entry.document,
            psiFile = entry.psiFile,
        )
    }

    /**
     * 查找指定打开文档 URI 当前应使用的 use-site 模块。
     *
     * 工作区内 overlay 返回源码模块，工作区外打开文档返回 dangling module。
     */
    internal fun useSiteModuleForOpenDocument(uri: String): CaModule? {
        workspaceModuleByDocumentUri[uri]?.let { return it }
        return danglingSnapshotsByUri[uri]?.module
    }

    /**
     * 使指定模块集合的 Analysis API session 失效。
     *
     * 该方法更新 LSP 自身修改计数，并在存在外部 session invalidation service 时继续委托。
     */
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

    /**
     * 根据工作区模块定义和打开文档快照构建源码模块条目。
     */
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

    /**
     * 为工作区外的打开文档构建 dangling module 条目。
     */
    private fun buildDanglingSnapshotEntries(
        openSnapshots: List<OpenDocumentSnapshotEntry>,
    ): List<DanglingSnapshotEntry> {
        return openSnapshots.mapNotNull { snapshot ->
            val contextModule = snapshot.normalizedPath?.let(::findWorkspaceModuleForPath)
                ?: return@mapNotNull null
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

    /**
     * 枚举指定源码根下的仓颉源码文件，并用打开文档 overlay 替换同路径磁盘文件。
     */
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

    /**
     * 枚举指定文件或目录下的仓颉源码路径。
     */
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

    /**
     * 根据 URI 查找所属工作区源码模块。
     */
    private fun findWorkspaceModuleForUri(uri: String): CaLspSourceModule? {
        val targetPath = uri.uriToPathOrNull()?.normalize() ?: return null
        return findWorkspaceModuleForPath(targetPath)
    }

    /**
     * 根据本地路径查找最精确的工作区源码模块。
     */
    private fun findWorkspaceModuleForPath(targetPath: Path): CaLspSourceModule? {
        return workspaceModulesByName.values
            .sortedByDescending { entry -> entry.sourceRootPaths.maxOfOrNull { root -> root.toString().length } ?: 0 }
            .firstOrNull { entry -> entry.sourceRootPaths.any { root -> isUnder(root, targetPath) } }
            ?.module
    }

    /**
     * 判断目标路径是否位于指定根路径下。
     */
    private fun isUnder(
        root: Path,
        target: Path,
    ): Boolean {
        val normalizedRoot = root.normalize()
        return target == normalizedRoot || target.startsWith(normalizedRoot)
    }

    /**
     * 从本地文件系统查找或刷新指定路径的 virtual file。
     */
    private fun findLocalVirtualFile(path: Path): VirtualFile? {
        val localFileSystem = StandardFileSystems.local()
        return localFileSystem.findFileByPath(path.toString())
            ?: localFileSystem.refreshAndFindFileByPath(path.toString())
    }

    /**
     * 为磁盘上的仓颉源码文件创建 PSI 文件。
     */
    private fun createDiskPsiFile(path: Path): CjFile? {
        if (!Files.exists(path) || !path.isRegularFile()) return null
        return LspAnalysisPsiFileFactory.createFile(
            project = project,
            documentUri = path.toUri().toString(),
            fileName = path.fileName.toString(),
            text = Files.readString(path),
        )
    }

    companion object {
        /**
         * 获取当前 project 注册的 LSP 项目结构状态服务。
         */
        fun getInstance(project: Project): AnalysisApiLspProjectStructureState = project.service()
    }
}

/**
 * 打开文档的 Analysis API 快照。
 */
internal data class LspOpenDocumentAnalysisSnapshot(
    /**
     * 当前打开文档的协议层快照。
     */
    val document: LspTextDocument,

    /**
     * 当前文档版本对应的 PSI 文件。
     */
    val psiFile: CjFile,
)

/**
 * LSP 工作区源码模块。
 *
 * 这里显式把 source roots 展开成可解析的 PSI 文件集合，让 Analysis API 看到的是一个真实源码模块，
 * 而不是只包含单文件快照的临时容器。
 */
internal class CaLspSourceModule(
    /**
     * 源码模块所属 project。
     */
    override val project: Project,

    /**
     * LSP 工作区模块名称。
     */
    override val name: String,
    psiRoots: List<PsiFileSystemItem>,

    /**
     * 该源码模块的规范化源码根路径。
     */
    private val sourceRootPaths: List<Path>,
) : CaModuleBase(), CaSourceModule {
    /**
     * 源码模块使用的语言版本设置。
     */
    override val languageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettingsImpl.DEFAULT

    /**
     * 该源码模块暴露给 Analysis API 的 PSI 根。
     */
    override val psiRoots: List<PsiFileSystemItem> = psiRoots.toList()

    /**
     * 直接 regular 依赖列表。
     */
    override val directRegularDependencies: MutableList<CaModule> = mutableListOf()

    /**
     * 直接 dependsOn 依赖列表。
     */
    override val directDependsOnDependencies: MutableList<CaModule> = mutableListOf()

    /**
     * 直接 friend 依赖列表。
     */
    override val directFriendDependencies: MutableList<CaModule> = mutableListOf()

    /**
     * 该源码模块的目标平台。
     */
    override val targetPlatform: TargetPlatform = CangJiePlatforms.defaultCangJiePlatform

    /**
     * 该模块内容搜索范围。
     */
    override val baseContentScope: GlobalSearchScope =
        buildSourceModuleContentScope(project, sourceRootPaths)

    /**
     * Analysis API 使用的稳定模块名。
     */
    override val stableModuleName: String
        get() = "lsp-source:$name"

    /**
     * 调试和错误信息中展示的模块描述。
     */
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
    /**
     * dangling module 所属 project。
     */
    override val project: Project,

    /**
     * 打开文档的 LSP URI。
     */
    val documentUri: String,

    /**
     * 当前打开文档版本对应的 PSI 文件。
     */
    private val psiFile: CjFile,

    /**
     * 该 dangling file 依附的工作区上下文模块。
     */
    override val contextModule: CaModule,

    /**
     * 对应磁盘文件的 virtual file，若存在则参与内容 scope。
     */
    private val contentVirtualFile: VirtualFile?,
) : CaModuleBase(), CaDanglingFileModule {
    /**
     * 持有打开文档 PSI 的智能指针列表。
     */
    private val filePointers: List<SmartPsiElementPointer<CjFile>> =
        listOf(SmartPointerManager.getInstance(project).createSmartPsiElementPointer(psiFile))

    /**
     * dangling module 的展示名称。
     */
    override val name: String
        get() = documentUri

    /**
     * dangling module 使用的语言版本设置。
     */
    override val languageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettingsImpl.DEFAULT

    /**
     * 当前仍然有效的打开文档 PSI 文件。
     */
    override val files: List<CjFile>
        get() = validFilesOrNull ?: error("Dangling file module is invalid")

    /**
     * dangling module 的 PSI 根。
     */
    override val psiRoots: List<PsiFileSystemItem>
        get() = files

    /**
     * dangling file 的解析模式。
     */
    override val resolutionMode: CaDanglingFileResolutionMode
        get() = CaDanglingFileResolutionMode.PREFER_SELF

    /**
     * 当前 dangling file 是否为代码片段。
     */
    override val isCodeFragment: Boolean
        get() = files.any { it is CjCodeFragment || it.isCodeFragment }

    /**
     * 当前 dangling module 是否仍持有有效 PSI。
     */
    override val isValid: Boolean
        get() = validFilesOrNull != null

    /**
     * 直接 regular 依赖列表。
     */
    override val directRegularDependencies: MutableList<CaModule> = mutableListOf()

    /**
     * 直接 dependsOn 依赖列表。
     */
    override val directDependsOnDependencies: MutableList<CaModule> = mutableListOf()

    /**
     * 直接 friend 依赖列表。
     */
    override val directFriendDependencies: MutableList<CaModule> = mutableListOf()

    /**
     * dangling module 继承上下文模块的目标平台。
     */
    override val targetPlatform: TargetPlatform
        get() = contextModule.targetPlatform

    /**
     * dangling file 的内容搜索范围。
     */
    override val baseContentScope: GlobalSearchScope
        get() = GlobalSearchScope.filesWithoutLibrariesScope(
            project,
            files.map { it.viewProvider.virtualFile } + listOfNotNull(contentVirtualFile).filterNot { candidate ->
                files.any { file -> file.viewProvider.virtualFile == candidate }
            },
        )

    /**
     * Analysis API 使用的稳定模块名。
     */
    override val stableModuleName: String
        get() = documentUri

    /**
     * 调试和错误信息中展示的模块描述。
     */
    override val moduleDescription: String
        get() = "LSP dangling file $documentUri"

    /**
     * 通过智能指针恢复仍有效的 PSI 文件列表。
     */
    private val validFilesOrNull: List<CjFile>?
        get() {
            val result = ArrayList<CjFile>(filePointers.size)
            for (filePointer in filePointers) {
                val file = filePointer.element?.takeIf { it.isValid } ?: return null
                result += file
            }
            return result
        }
}

/**
 * 构造源码模块内容搜索范围。
 *
 * 搜索范围只包含给定源码根下的文件，不包含库内容。
 */
private fun buildSourceModuleContentScope(
    project: Project,
    sourceRootPaths: List<Path>,
): GlobalSearchScope {
    if (sourceRootPaths.isEmpty()) {
        return GlobalSearchScope.EMPTY_SCOPE
    }

    return object : GlobalSearchScope(project) {
        /**
         * 判断 virtual file 是否位于任一源码根下。
         */
        override fun contains(file: VirtualFile): Boolean {
            val filePath = runCatching { Path.of(file.path).normalize() }.getOrNull() ?: return false
            return sourceRootPaths.any { root ->
                filePath == root || filePath.startsWith(root)
            }
        }

        /**
         * LSP 源码范围不绑定 IntelliJ module content。
         */
        override fun isSearchInModuleContent(aModule: com.intellij.openapi.module.Module): Boolean = false

        /**
         * LSP 源码范围不包含库文件。
         */
        override fun isSearchInLibraries(): Boolean = false
    }
}

/**
 * Analysis API project structure provider 的 LSP 实现。
 */
internal class AnalysisApiLspProjectStructureProvider(
    /**
     * 当前 LSP project。
     */
    private val project: Project,
) : CangJieProjectStructureProvider {
    /**
     * 当前 project 的 LSP 项目结构状态。
     */
    private val state: AnalysisApiLspProjectStructureState
        get() = AnalysisApiLspProjectStructureState.getInstance(project)

    /**
     * 为 PSI 元素恢复 Analysis API 模块。
     */
    override fun getModule(element: PsiElement, useSiteModule: CaModule?): CaModule {
        return state.getModule(element, useSiteModule)
    }

    /**
     * 查找 dependsOn 指向指定模块的实现模块。
     */
    override fun getImplementingModules(module: CaModule): List<CaModule> {
        return state.allModules.filter { module in it.directDependsOnDependencies }
    }

    /**
     * 全局语言版本设置。
     */
    override val globalLanguageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettingsImpl.DEFAULT
}

/**
 * Analysis API module provider 的 LSP 实现。
 */
internal class AnalysisApiLspModuleProvider(
    /**
     * 当前 LSP project。
     */
    private val project: Project,
) : CaModuleProvider {
    /**
     * 当前 project 的 LSP 项目结构状态。
     */
    private val state: AnalysisApiLspProjectStructureState
        get() = AnalysisApiLspProjectStructureState.getInstance(project)

    /**
     * 当前项目结构快照。
     */
    override val snapshot: CaProjectStructureSnapshot
        get() = state.snapshot

    /**
     * 当前所有模块。
     */
    override val allModules: List<CaModule>
        get() = state.allModules

    /**
     * 当前可解析模块。
     */
    override val resolvableModules: List<CaModule>
        get() = state.allResolvableModules

    /**
     * 当前源码类模块。
     */
    override val sourceLikeModules: List<CaModule>
        get() = state.allSourceLikeModules

    /**
     * 根据稳定模块名查找模块。
     */
    override fun getModuleByStableName(stableModuleName: String): CaModule? {
        return state.getModuleByStableName(stableModuleName)
    }
}

/**
 * Analysis API modification tracker 的 LSP 实现。
 */
internal class AnalysisApiLspModificationTracker(
    /**
     * 当前 LSP project。
     */
    private val project: Project,
) : CaModificationTracker {
    /**
     * 当前 project 的 LSP 项目结构状态。
     */
    private val state: AnalysisApiLspProjectStructureState
        get() = AnalysisApiLspProjectStructureState.getInstance(project)

    /**
     * 全局修改计数。
     */
    override val modificationCount: Long
        get() = state.modificationCount

    /**
     * 指定模块的修改计数。
     */
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
    /**
     * 当前 LSP project。
     */
    private val project: Project,
) : CaSessionInvalidationService {
    /**
     * 当前 project 的 LSP 项目结构状态。
     */
    private val state: AnalysisApiLspProjectStructureState
        get() = AnalysisApiLspProjectStructureState.getInstance(project)

    /**
     * 委托 LSP 项目结构状态执行模块失效。
     */
    override fun invalidate(modules: Set<CaModule>) {
        state.invalidate(modules)
    }
}

/**
 * 判断路径是否为仓颉源码文件路径。
 */
private fun Path.isCangjieSourceFilePath(): Boolean {
    val fileName = fileName?.toString().orEmpty()
    return fileName.endsWith(".cj") || fileName.endsWith(".cjs")
}

/**
 * LSP 平台的受限分析服务。
 */
internal class AnalysisApiLspRestrictedAnalysisService : CaRestrictedAnalysisService {
    /**
     * LSP 平台当前不启用 restricted analysis。
     */
    override val isAnalysisRestricted: Boolean
        get() = false

    /**
     * LSP 平台允许普通分析。
     */
    override val isRestrictedAnalysisAllowed: Boolean
        get() = true

    /**
     * 拒绝 restricted analysis 的错误路径。
     */
    override fun rejectRestrictedAnalysis(): Nothing {
        error("LSP 平台当前未启用 restricted analysis，不应调用 rejectRestrictedAnalysis().")
    }
}

/**
 * LSP 平台设置。
 */
internal class AnalysisApiLspPlatformSettings : CaPlatformSettings {
    /**
     * 反序列化声明来源。
     */
    override val deserializedDeclarationsOrigin: CaDeserializedDeclarationsOrigin
        get() = CaDeserializedDeclarationsOrigin.BINARIES

    /**
     * 是否允许 use-site library module 分析。
     */
    override val allowUseSiteLibraryModuleAnalysis: Boolean
        get() = false
}

/**
 * LSP 平台的分析权限检查器。
 */
internal class AnalysisApiLspPermissionChecker : CaAnalysisPermissionChecker {
    /**
     * 分析权限注册表。
     */
    private val permissionRegistry by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaAnalysisPermissionRegistry.getInstance()
    }

    /**
     * 判断当前动作中是否允许分析。
     */
    override fun isAnalysisAllowed(): Boolean {
        return permissionRegistry.explicitAnalysisRestriction == null
    }

    /**
     * 返回分析被拒绝时的说明。
     */
    override fun getRejectionReason(): String {
        val restriction = permissionRegistry.explicitAnalysisRestriction
            ?: error("Cannot get a rejection reason when analysis is allowed.")
        return "Resolve is explicitly forbidden in the current action: ${restriction.description}."
    }
}
