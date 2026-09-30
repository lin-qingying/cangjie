package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiModificationTracker
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
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
 * 枚举结果按“根列表 + 修改计数”缓存：根发现是宿主级的廉价路径解析（SDK 目录），而递归枚举整棵
 * SDK 目录不是——类查找、候选收集、解析作用域构建都会高频调用这里，Kotlin 侧
 * `BuiltinsVirtualFileProviderBaseImpl` 也维护自己的枚举缓存。
 */
abstract class BuiltinsVirtualFileProviderBaseImpl : BuiltinsVirtualFileProvider() {
    /**
     * 应用级枚举缓存。
     */
    private val applicationFileCache = ConcurrentHashMap<EnumerationCacheKey, Set<VirtualFile>>()

    /**
     * 项目级枚举缓存。
     */
    private val projectFileCache = ConcurrentHashMap<EnumerationCacheKey, Set<VirtualFile>>()

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
        applicationModificationCount(),
    )

    /**
     * 收集项目级 builtins 根下的所有 `.cjo` 二进制文件。
     */
    override fun getBuiltinVirtualFiles(project: Project): Set<VirtualFile> = enumerateBuiltinFiles(
        getBuiltinRootVirtualFiles(project),
        projectFileCache,
        projectModificationCount(project),
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
     * 基于项目级 builtins 文件集合创建 IntelliJ 搜索作用域。
     */
    override fun createBuiltinsScope(project: Project): GlobalSearchScope {
        return GlobalSearchScope.filesScope(project, getBuiltinVirtualFiles(project))
    }

    /**
     * 按“根列表 + 修改计数”缓存枚举结果。
     *
     * 根列表或修改计数任一变化都会重建；旧键的条目随缓存表清理时消失，
     * 这里不保留过期结果（SDK 切换后旧文件集合必须立刻不可见）。
     */
    private fun enumerateBuiltinFiles(
        roots: Set<VirtualFile>,
        cache: ConcurrentHashMap<EnumerationCacheKey, Set<VirtualFile>>,
        modificationCount: Long,
    ): Set<VirtualFile> {
        val key = EnumerationCacheKey(roots, modificationCount)
        // 根列表变化（SDK 切换）时清掉旧根的条目：旧文件集合必须立刻不可见。
        cache.entries.removeIf { it.key.roots != key.roots }
        return cache.computeIfAbsent(key) { current ->
            current.roots.flatMapTo(linkedSetOf(), ::collectBuiltinFiles)
        }
    }

    /**
     * 枚举缓存键：根集合与修改计数共同决定结果。
     *
     * @property roots 宿主解析出的 builtins 根虚拟文件；VFS 对同一路径返回同一实例，
     *   因此按实例比较等价于按路径比较。
     * @property modificationCount 观察枚举结果时的项目（或应用）修改计数。
     */
    private class EnumerationCacheKey(
        val roots: Set<VirtualFile>,
        val modificationCount: Long,
    )

    /**
     * 项目修改计数：优先 Analysis API 的计数（结构变化），否则退回 PSI 修改计数，
     * 不能退化为常量 `0`，否则 SDK 变化后缓存永不失效。
     */
    private fun projectModificationCount(project: Project): Long =
        CaModificationTracker.getInstance(project)?.modificationCount
            ?: PsiModificationTracker.getInstance(project).modificationCount

    /**
     * 应用级修改计数：取所有未释放工程的计数之和。
     *
     * 应用级 provider 聚合多个工程的 SDK 根，单个工程的计数不足以代表整体；没有应用容器（CLI 单元测试）
     * 或没有打开工程时退化为 0，此时缓存只随根列表变化失效——SDK 切换本来就表现为根变化。
     */
    private fun applicationModificationCount(): Long = ProjectManager.getInstance()
        ?.openProjects
        .orEmpty()
        .asSequence()
        .filterNot { project -> project.isDisposed }
        .sumOf { project -> projectModificationCount(project) }

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
