package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * 提供仓颉 builtins `.cjo` 虚拟文件集合的应用级服务。
 *
 * 该抽象把“内建库位置如何发现”与“反编译、索引、搜索作用域如何消费这些文件”分离；
 * CLI、LSP、IDE 宿主可以各自实现 roots 发现策略，而上层统一通过该 provider 获取文件集合。
 */
abstract class BuiltinsVirtualFileProvider {
    /**
     * 返回当前应用环境可见的 builtins `.cjo` 文件集合。
     */
    abstract fun getBuiltinVirtualFiles(): Set<VirtualFile>

    /**
     * 返回指定项目上下文中可见的 builtins `.cjo` 文件集合。
     *
     * IDE 宿主可以依据 project SDK 或项目模型解析 builtins；CLI 实现通常会退化为应用级路径。
     */
    abstract fun getBuiltinVirtualFiles(project: Project): Set<VirtualFile>

    /**
     * 为指定项目创建只覆盖 builtins `.cjo` 文件的搜索作用域。
     */
    abstract fun createBuiltinsScope(project: Project): GlobalSearchScope

    /**
     * 判断文件是否位于当前宿主的 builtins 根内（含根本身）。
     *
     * 默认实现按已枚举的 `.cjo` 文件集合判断；[BuiltinsVirtualFileProviderBaseImpl] 覆写为根路径包含判断，
     * 不需要枚举。调用方（IDE 候选收集、`.cjo` stub 构建）按文件逐个调用，必须避免每次都重建 `contentScope`。
     */
    open fun isInBuiltinRoot(file: VirtualFile, project: Project?): Boolean {
        val files = if (project == null) getBuiltinVirtualFiles() else getBuiltinVirtualFiles(project)
        return files.any { builtin -> builtin == file || VfsUtilCore.isAncestor(builtin, file, false) }
    }

    /**
     * 批量判断给定文件是否**全部**位于 builtins 根内（含根本身）。
     *
     * 语义等价于对每个文件调用 [isInBuiltinRoot] 再取 `&&`，但允许宿主只解析一次 builtins 根。
     * 逐个调用时每次都要重新解析根，而调用方常常是“同一个库下的每个 `.cjo` 各问一次”
     * ——IDE 宿主解析根要走 SDK 服务查找 + 路径存在性检查 + VFS 查找，逐个调用会把
     * 同一份磁盘 I/O 重复成百上千次。
     *
     * 典型调用方是项目结构快照里的“该库是否已被 builtins 通道完整覆盖”判定：
     * 它位于写动作尾部的每键必经路径上，重复 I/O 会直接表现为编辑器输入卡顿。
     */
    open fun areAllInBuiltinRoot(files: Collection<VirtualFile>, project: Project?): Boolean =
        files.all { file -> isInBuiltinRoot(file, project) }

    companion object {
        /**
         * 从 IntelliJ application service 容器中取得已注册的 builtins 文件 provider。
         */
        fun getInstance(): BuiltinsVirtualFileProvider {
            return requireNotNull(
                ApplicationManager.getApplication().getService(BuiltinsVirtualFileProvider::class.java),
            ) {
                "BuiltinsVirtualFileProvider is not registered in the current application container"
            }
        }
    }
}

/**
 * 对位 Kotlin `BuiltinsVirtualFileProviderBaseImpl`。
 *
 * 仓颉 builtins 的真实来源与 Kotlin 不同，
 * 但“由 provider 基类统一收集 binary files，再由具体宿主实现 root 定位”这一 owner 形状保持一致。
 *
 * 枚举结果按“根列表 + VFS 结构戳”缓存：根发现是宿主级的廉价路径解析（SDK 目录），而递归枚举整棵
 * SDK 目录不是——类查找、候选收集、解析作用域构建都会高频调用这里，Kotlin 侧
 * `BuiltinsVirtualFileProviderBaseImpl` 也维护自己的枚举缓存。
 */
abstract class BuiltinsVirtualFileProviderBaseImpl : BuiltinsVirtualFileProvider() {
    /**
     * 应用级枚举缓存。
     */
    private val applicationFileCache = ConcurrentHashMap<Set<VirtualFile>, CachedEnumeration>()

    /**
     * 项目级枚举缓存。
     */
    private val projectFileCache = ConcurrentHashMap<Set<VirtualFile>, CachedEnumeration>()

    /**
     * 返回当前宿主环境声明的 builtins 根虚拟文件。
     */
    protected abstract fun getBuiltinRootVirtualFiles(): Set<VirtualFile>

    /**
     * 返回指定项目上下文声明的 builtins 根虚拟文件。
     */
    protected abstract fun getBuiltinRootVirtualFiles(project: Project): Set<VirtualFile>

    /**
     * 收集应用级 builtins 根下的所有 `.cjo` 二进制文件。
     */
    override fun getBuiltinVirtualFiles(): Set<VirtualFile> = enumerateBuiltinFiles(
        getBuiltinRootVirtualFiles(),
        applicationFileCache,
    )

    /**
     * 收集项目级 builtins 根下的所有 `.cjo` 二进制文件。
     */
    override fun getBuiltinVirtualFiles(project: Project): Set<VirtualFile> = enumerateBuiltinFiles(
        getBuiltinRootVirtualFiles(project),
        projectFileCache,
    )

    /**
     * 判断文件是否位于当前宿主的 builtins 根内（含根本身）。
     *
     * 对位 Kotlin `CandidateCollector.collectBuiltinsCandidates` 的 builtins 候选判定：那里对
     * `builtinsModule.contentScope` 做 `in` 判断，会在每次判断前重新枚举整个 SDK 目录；
     * 这里只做根路径包含判断，不产生任何枚举。
     */
    override fun isInBuiltinRoot(file: VirtualFile, project: Project?): Boolean {
        val roots = if (project == null) getBuiltinRootVirtualFiles() else getBuiltinRootVirtualFiles(project)
        return roots.any { root -> VfsUtilCore.isAncestor(root, file, false) }
    }

    /**
     * 一次解析 builtins 根，再对整组文件做根路径包含判断。
     *
     * 与 [isInBuiltinRoot] 的差别只在“根解析的次数”：这里无论传入多少文件都只解析一次，
     * 逐个调用则是每个文件解析一次。热路径上这决定了是纯内存判断还是每键上百次磁盘 I/O。
     */
    override fun areAllInBuiltinRoot(files: Collection<VirtualFile>, project: Project?): Boolean {
        val roots = if (project == null) getBuiltinRootVirtualFiles() else getBuiltinRootVirtualFiles(project)
        return files.all { file -> roots.any { root -> VfsUtilCore.isAncestor(root, file, false) } }
    }

    /**
     * 基于项目级 builtins 文件集合创建 IntelliJ 搜索作用域。
     */
    override fun createBuiltinsScope(project: Project): GlobalSearchScope {
        return GlobalSearchScope.filesScope(project, getBuiltinVirtualFiles(project))
    }

    /**
     * 按“根列表 + VFS 结构戳”缓存枚举结果。
     *
     * **不能**用 PSI / Analysis API 修改计数做键：那个计数几乎每次编辑都会变，
     * 而 builtins 目录的内容只随 VFS 结构变化（SDK 切换、目录增删、`.cjo` 落地）。
     * 旧实现用修改计数做键，等于让“编辑一个字符后第一次类查找”重新遍历整棵 SDK 目录。
     *
     * **戳必须在枚举之后取**：`iterateChildrenRecursively` 首次把子节点装入 VFS 时
     * （`FSRecordsImpl.updateSymlinksForNewChildren`）会推进 `VFS_STRUCTURE_MODIFICATIONS`。
     * 若在枚举前取戳做键，枚举自身就会让刚写入的条目立刻失配，形成“每次查找都重枚举”的抖动。
     */
    private fun enumerateBuiltinFiles(
        roots: Set<VirtualFile>,
        cache: ConcurrentHashMap<Set<VirtualFile>, CachedEnumeration>,
    ): Set<VirtualFile> {
        // 根列表变化（SDK 切换）时清掉旧根的条目：旧文件集合必须立刻不可见。
        cache.keys.removeIf { cachedRoots -> cachedRoots != roots }

        val vfsStamp = vfsStructureModificationCount()
        cache[roots]?.let { cached ->
            if (cached.vfsStructureModificationCount == vfsStamp) {
                return cached.files
            }
        }

        val files = roots.flatMapTo(linkedSetOf(), ::collectBuiltinFiles)
        // 枚举之后再取戳：把枚举自身引起的 VFS 结构推进一并记入，避免自失效。
        cache[roots] = CachedEnumeration(vfsStructureModificationCount(), files)
        return files
    }

    /**
     * 单次枚举结果及其观测戳。
     *
     * @property vfsStructureModificationCount 本次枚举**完成后**观察到的 VFS 结构修改计数。
     * @property files 枚举出的 builtins `.cjo` 文件集合。
     */
    private class CachedEnumeration(
        val vfsStructureModificationCount: Long,
        val files: Set<VirtualFile>,
    )

    /**
     * VFS 结构修改计数：builtins 目录内容变化的唯一廉价观察点。
     *
     * 用 `VFS_STRUCTURE_MODIFICATIONS` 而不是 `CaModificationTracker` / `PsiModificationTracker`：
     * 后两者的变化包含源码编辑，把它们并进键会让缓存随每次按键失效，表现为输入卡顿。
     * 本仓 `AbstractVirtualFileRootsScope.extractFileEnumeration` 用的是同一个观察点。
     */
    private fun vfsStructureModificationCount(): Long =
        VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS.modificationCount

    /**
     * 显式失效枚举缓存。
     *
     * 宿主在确认 builtins 根目录内容发生变化（SDK 切换、标准库重新安装）时调用；
     * 不依赖任何全局修改计数，避免“该失效时没失效”或“编辑源码也失效”两个方向的误判。
     */
    fun invalidateBuiltinFileCache() {
        applicationFileCache.clear()
        projectFileCache.clear()
    }

    /**
     * 从单个 builtins 根收集可反编译的 `.cjo` 文件。
     *
     * 根可以是目录，也可以是一个具体 `.cjo` 文件；目录场景会递归遍历所有子文件。
     */
    protected fun collectBuiltinFiles(root: VirtualFile): List<VirtualFile> {
        if (!root.isDirectory) {
            return listOfNotNull(root.takeIf(::isBuiltinBinary))
        }

        val files = linkedSetOf<VirtualFile>()
        VfsUtilCore.iterateChildrenRecursively(root, null) { child ->
            ProgressManager.checkCanceled()
            if (!child.isDirectory && isBuiltinBinary(child)) {
                files += child
            }
            true
        }
        return files.toList()
    }

    /**
     * 判断虚拟文件是否是 builtins provider 应当暴露的仓颉二进制。
     */
    protected fun isBuiltinBinary(file: VirtualFile): Boolean {
        return file.fileType == CangJieBuiltInFileType ||
            file.extension.equals(CangJieBuiltInFileType.defaultExtension, ignoreCase = true)
    }

    /**
     * 统一解析宿主传入的本地 stdlib 根路径。
     *
     * CLI/LSP/IDE 都可能在运行期拿到新复制出来的目录，
     * 这里只允许走同一条 refresh-aware VFS 恢复链，避免不同宿主各自出现
     * “属性已设置但 VirtualFile 尚未进入 VFS” 的分叉。
     */
    protected fun resolveLocalRootVirtualFile(path: String): VirtualFile? {
        val normalizedPath = path.replace('\\', '/')
        val localFileSystem = StandardFileSystems.local()
        val nioPath = runCatching { Path.of(path) }.getOrNull()

        return localFileSystem.findFileByPath(normalizedPath)
            ?: localFileSystem.refreshAndFindFileByPath(normalizedPath)
            ?: nioPath?.let(VirtualFileManager.getInstance()::findFileByNioPath)
    }
}

/**
 * 按宿主环境约定解析 builtins `.cjo` 根路径。
 *
 * 与 low-level CFIR 共用：
 * - `cangjie.stdlib.module`
 * - `CANGJIE_STDLIB_MODULE`
 */
class BuiltinsVirtualFileProviderCliImpl : BuiltinsVirtualFileProviderBaseImpl() {
    /**
     * 从系统属性或环境变量中读取 CLI/LSP 宿主暴露的 builtins 根路径并转换为虚拟文件。
     */
    override fun getBuiltinRootVirtualFiles(): Set<VirtualFile> {
        return readPaths("cangjie.stdlib.module", "CANGJIE_STDLIB_MODULE")
            .mapNotNull { path ->
                resolveLocalRootVirtualFile(path)
                    ?: run {
                        logger<BuiltinsVirtualFileProviderCliImpl>().warn("Cannot resolve builtins path: $path")
                        null
                    }
            }
            .toCollection(linkedSetOf())
    }

    /**
     * CLI 实现没有额外 project 维度，直接复用应用级 builtins 根集合。
     */
    override fun getBuiltinRootVirtualFiles(project: Project): Set<VirtualFile> = getBuiltinRootVirtualFiles()

    /**
     * 读取并拆分指定系统属性或环境变量中的本地路径列表。
     *
     * 系统属性优先于环境变量，多个路径使用当前平台的 [File.pathSeparator] 分隔。
     */
    private fun readPaths(propertyKey: String, envKey: String): List<String> {
        val raw = System.getProperty(propertyKey)
            ?.takeIf(String::isNotBlank)
            ?: System.getenv(envKey)?.takeIf(String::isNotBlank)
            ?: return emptyList()
        return raw.split(File.pathSeparator).filter(String::isNotBlank)
    }
}
