package org.cangnova.cangjie.frontend.environment

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.cangnova.cangjie.CjSourceFile
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

/**
 * 前端工程的 VFS 与搜索域抽象。
 *
 * 这里承载的是“前端处理源文件、类路径、目录范围”所需的环境能力，
 * 不再把这套基础设施绑定到 CLI 语义上。
 */
interface AbstractProjectFileSearchScope {
    /**
     * 当前搜索域是否为空。
     */
    val isEmpty: Boolean

    /**
     * 从当前搜索域中排除另一个搜索域。
     */
    operator fun minus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope

    /**
     * 合并当前搜索域与另一个搜索域。
     */
    operator fun plus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope

    /**
     * 返回当前搜索域的补集。
     */
    operator fun not(): AbstractProjectFileSearchScope

    /**
     * 通用搜索域常量。
     */
    companion object {
        /**
         * 空搜索域。
         */
        val EMPTY: AbstractProjectFileSearchScope = PsiBasedProjectFileSearchScope(GlobalSearchScope.EMPTY_SCOPE)

        /**
         * 匹配任意文件的搜索域。
         */
        val ANY: AbstractProjectFileSearchScope = PsiBasedProjectFileSearchScope(GlobalSearchScope.notScope(GlobalSearchScope.EMPTY_SCOPE))
    }
}

/**
 * 基于 IntelliJ PSI [GlobalSearchScope] 的前端搜索域实现。
 */
class PsiBasedProjectFileSearchScope(
    /**
     * 底层 IntelliJ 搜索域。
     */
    val psiSearchScope: GlobalSearchScope,
) : AbstractProjectFileSearchScope {
    /**
     * 当前 PSI 搜索域是否为空。
     */
    override val isEmpty: Boolean
        get() = psiSearchScope == GlobalSearchScope.EMPTY_SCOPE

    /**
     * 返回当前搜索域减去 [other] 后的搜索域。
     */
    override fun minus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(psiSearchScope.intersectWith(GlobalSearchScope.notScope(other.asPsiSearchScope())))

    /**
     * 返回当前搜索域与 [other] 的并集。
     */
    override fun plus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(psiSearchScope.uniteWith(other.asPsiSearchScope()))

    /**
     * 返回当前 PSI 搜索域的补集。
     */
    override fun not(): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(GlobalSearchScope.notScope(psiSearchScope))
}

/**
 * 基于 IntelliJ VFS 的前端项目环境。
 */
open class VfsBasedProjectEnvironment(
    /**
     * 当前 IntelliJ 项目。
     */
    val project: Project,
    /**
     * 前端可用于解析文件路径的 VFS 实例列表。
     */
    val knownFileSystems: List<VirtualFileSystem>,
) {
    /**
     * 使用单个文件系统创建项目环境。
     */
    constructor(project: Project, fileSystem: VirtualFileSystem) : this(project, listOf(fileSystem))

    /**
     * 将虚拟文件集合转换为 PSI 搜索域。
     */
    private fun List<VirtualFile>.toSearchScope(allowOutOfProjectRoots: Boolean): GlobalSearchScope =
        takeIf { it.isNotEmpty() }
            ?.let {
                if (allowOutOfProjectRoots) GlobalSearchScope.filesWithLibrariesScope(project, it)
                else GlobalSearchScope.filesWithoutLibrariesScope(project, it)
            }
            ?: GlobalSearchScope.EMPTY_SCOPE

    /**
     * 根据普通 IO 文件列表创建搜索域。
     */
    open fun getSearchScopeByIoFiles(files: Iterable<File>, allowOutOfProjectRoots: Boolean): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(
            files.mapNotNull { file -> knownFileSystems.findFileByPath(file.absolutePath) }.toSearchScope(allowOutOfProjectRoots)
        )

    /**
     * 根据仓颉源文件列表创建搜索域。
     */
    open fun getSearchScopeBySourceFiles(files: Iterable<CjSourceFile>, allowOutOfProjectRoots: Boolean): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(
            files.mapNotNull { source ->
                source.path?.let { knownFileSystems.findFileByPath(it) }
            }.toSearchScope(allowOutOfProjectRoots)
        )

    /**
     * 根据目录列表创建包含目录树的搜索域。
     */
    open fun getSearchScopeByDirectories(directories: Iterable<File>): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(
            directories
                .mapNotNull { knownFileSystems.findFileByPath(it.absolutePath) }
                .toSet()
                .takeIf { it.isNotEmpty() }
                ?.let { DirectoriesScope(project, it) }
                ?: GlobalSearchScope.EMPTY_SCOPE
        )

    /**
     * 根据 classpath 路径创建搜索域。
     */
    open fun getSearchScopeByClassPath(paths: Iterable<Path>): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(
            paths
                .mapNotNull {
                    when {
                        it.isDirectory() -> knownFileSystems.findFileByPath(it.toFile().absolutePath, StandardFileSystems.FILE_PROTOCOL)
                        !it.isRegularFile() -> null
                        else -> knownFileSystems.findFileByPath(it.toFile().absolutePath + JAR_SEPARATOR, StandardFileSystems.JAR_PROTOCOL)
                    }
                }
                .takeIf { it.isNotEmpty() }
                ?.let { ClassPathScope(project, it) }
                ?: GlobalSearchScope.EMPTY_SCOPE
        )

    /**
     * 根据 PSI 文件列表创建搜索域。
     */
    open fun getSearchScopeByPsiFiles(files: Iterable<PsiFile>): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(GlobalSearchScope.filesWithoutLibrariesScope(project, files.map { it.virtualFile }))

    /**
     * 返回项目库搜索域。
     */
    open fun getSearchScopeForProjectLibraries(): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(ProjectScope.getLibrariesScope(project))

    /**
     * 返回项目 Java 源码搜索域。
     */
    open fun getSearchScopeForProjectJavaSources(): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(ProjectScope.getProjectScope(project))

    /**
     * 匹配指定目录及其子孙文件的搜索域。
     */
    class DirectoriesScope(
        project: Project,
        /**
         * 搜索域包含的目录根。
         */
        private val directories: Set<VirtualFile>,
    ) : DelegatingGlobalSearchScope(allScope(project)) {
        /**
         * 目录根所在的文件系统集合。
         */
        private val fileSystems = directories.mapTo(hashSetOf(), VirtualFile::getFileSystem)

        /**
         * 判断文件是否位于任一目录根之下。
         */
        override fun contains(file: VirtualFile): Boolean {
            if (file.fileSystem !in fileSystems) return false
            var parent: VirtualFile = file
            while (true) {
                if (parent in directories) return true
                parent = parent.parent ?: return false
            }
        }

        /**
         * 返回用于调试的目录根描述。
         */
        override fun toString(): String = "All files under: $directories"
    }

    /**
     * 匹配 classpath 根下文件的搜索域。
     */
    private class ClassPathScope(
        project: Project,
        roots: Iterable<VirtualFile>,
    ) : DelegatingGlobalSearchScope(allScope(project)) {
        /**
         * 按文件系统分组的 classpath 根集合。
         */
        private val fileSystemsToRoots = HashMap<VirtualFileSystem, HashSet<VirtualFile>>()

        init {
            for (root in roots) {
                val fs = root.fileSystem
                fileSystemsToRoots.getOrPut(fs) { HashSet() }.add(root)
            }
        }

        /**
         * 判断文件是否属于任一 classpath 根。
         */
        override fun contains(file: VirtualFile): Boolean {
            val possibleRoots = fileSystemsToRoots[file.fileSystem] ?: return false
            val prefixPos = file.path.indexOf(JAR_SEPARATOR)
            if (prefixPos >= 0) {
                val root = file.fileSystem.findFileByPath(file.path.substring(0, prefixPos + JAR_SEPARATOR.length))
                return root in possibleRoots
            }

            var parent: VirtualFile = file
            while (true) {
                if (parent in possibleRoots) return true
                parent = parent.parent ?: return false
            }
        }

        /**
         * 返回用于调试的 classpath 根描述。
         */
        override fun toString(): String = "All files under: ${fileSystemsToRoots.values.flatten().joinToString { it.path }}"
    }
}

/**
 * 将前端搜索域转换为 IntelliJ PSI 搜索域。
 */
private fun AbstractProjectFileSearchScope.asPsiSearchScope(): GlobalSearchScope =
    when {
        this === AbstractProjectFileSearchScope.EMPTY -> GlobalSearchScope.EMPTY_SCOPE
        this === AbstractProjectFileSearchScope.ANY -> GlobalSearchScope.notScope(GlobalSearchScope.EMPTY_SCOPE)
        else -> (this as PsiBasedProjectFileSearchScope).psiSearchScope
    }

/**
 * 在给定 VFS 列表中按路径查找文件。
 */
internal fun List<VirtualFileSystem>.findFileByPath(
    path: String,
    protocolFilter: String? = StandardFileSystems.FILE_PROTOCOL,
): VirtualFile? = firstNotNullOfOrNull {
    if (protocolFilter != null && it.protocol != protocolFilter) null else it.findFileByPath(path)
}

/**
 * 在当前项目环境的已知文件系统中查找文件。
 */
fun VfsBasedProjectEnvironment.findFileByPath(path: String): VirtualFile? = knownFileSystems.findFileByPath(path)
