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
    val isEmpty: Boolean
    operator fun minus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope
    operator fun plus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope
    operator fun not(): AbstractProjectFileSearchScope

    companion object {
        val EMPTY: AbstractProjectFileSearchScope = PsiBasedProjectFileSearchScope(GlobalSearchScope.EMPTY_SCOPE)
        val ANY: AbstractProjectFileSearchScope = PsiBasedProjectFileSearchScope(GlobalSearchScope.notScope(GlobalSearchScope.EMPTY_SCOPE))
    }
}

class PsiBasedProjectFileSearchScope(val psiSearchScope: GlobalSearchScope) : AbstractProjectFileSearchScope {
    override val isEmpty: Boolean
        get() = psiSearchScope == GlobalSearchScope.EMPTY_SCOPE

    override fun minus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(psiSearchScope.intersectWith(GlobalSearchScope.notScope(other.asPsiSearchScope())))

    override fun plus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(psiSearchScope.uniteWith(other.asPsiSearchScope()))

    override fun not(): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(GlobalSearchScope.notScope(psiSearchScope))
}

open class VfsBasedProjectEnvironment(
    val project: Project,
    val knownFileSystems: List<VirtualFileSystem>,
) {
    constructor(project: Project, fileSystem: VirtualFileSystem) : this(project, listOf(fileSystem))

    private fun List<VirtualFile>.toSearchScope(allowOutOfProjectRoots: Boolean): GlobalSearchScope =
        takeIf { it.isNotEmpty() }
            ?.let {
                if (allowOutOfProjectRoots) GlobalSearchScope.filesWithLibrariesScope(project, it)
                else GlobalSearchScope.filesWithoutLibrariesScope(project, it)
            }
            ?: GlobalSearchScope.EMPTY_SCOPE

    open fun getSearchScopeByIoFiles(files: Iterable<File>, allowOutOfProjectRoots: Boolean): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(
            files.mapNotNull { file -> knownFileSystems.findFileByPath(file.absolutePath) }.toSearchScope(allowOutOfProjectRoots)
        )

    open fun getSearchScopeBySourceFiles(files: Iterable<CjSourceFile>, allowOutOfProjectRoots: Boolean): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(
            files.mapNotNull { source ->
                source.path?.let { knownFileSystems.findFileByPath(it) }
            }.toSearchScope(allowOutOfProjectRoots)
        )

    open fun getSearchScopeByDirectories(directories: Iterable<File>): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(
            directories
                .mapNotNull { knownFileSystems.findFileByPath(it.absolutePath) }
                .toSet()
                .takeIf { it.isNotEmpty() }
                ?.let { DirectoriesScope(project, it) }
                ?: GlobalSearchScope.EMPTY_SCOPE
        )

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

    open fun getSearchScopeByPsiFiles(files: Iterable<PsiFile>): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(GlobalSearchScope.filesWithoutLibrariesScope(project, files.map { it.virtualFile }))

    open fun getSearchScopeForProjectLibraries(): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(ProjectScope.getLibrariesScope(project))

    open fun getSearchScopeForProjectJavaSources(): AbstractProjectFileSearchScope =
        PsiBasedProjectFileSearchScope(ProjectScope.getProjectScope(project))

    class DirectoriesScope(
        project: Project,
        private val directories: Set<VirtualFile>,
    ) : DelegatingGlobalSearchScope(allScope(project)) {
        private val fileSystems = directories.mapTo(hashSetOf(), VirtualFile::getFileSystem)

        override fun contains(file: VirtualFile): Boolean {
            if (file.fileSystem !in fileSystems) return false
            var parent: VirtualFile = file
            while (true) {
                if (parent in directories) return true
                parent = parent.parent ?: return false
            }
        }

        override fun toString(): String = "All files under: $directories"
    }

    private class ClassPathScope(
        project: Project,
        roots: Iterable<VirtualFile>,
    ) : DelegatingGlobalSearchScope(allScope(project)) {
        private val fileSystemsToRoots = HashMap<VirtualFileSystem, HashSet<VirtualFile>>()

        init {
            for (root in roots) {
                val fs = root.fileSystem
                fileSystemsToRoots.getOrPut(fs) { HashSet() }.add(root)
            }
        }

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

        override fun toString(): String = "All files under: ${fileSystemsToRoots.values.flatten().joinToString { it.path }}"
    }
}

private fun AbstractProjectFileSearchScope.asPsiSearchScope(): GlobalSearchScope =
    when {
        this === AbstractProjectFileSearchScope.EMPTY -> GlobalSearchScope.EMPTY_SCOPE
        this === AbstractProjectFileSearchScope.ANY -> GlobalSearchScope.notScope(GlobalSearchScope.EMPTY_SCOPE)
        else -> (this as PsiBasedProjectFileSearchScope).psiSearchScope
    }

internal fun List<VirtualFileSystem>.findFileByPath(
    path: String,
    protocolFilter: String? = StandardFileSystems.FILE_PROTOCOL,
): VirtualFile? = firstNotNullOfOrNull {
    if (protocolFilter != null && it.protocol != protocolFilter) null else it.findFileByPath(path)
}

fun VfsBasedProjectEnvironment.findFileByPath(path: String): VirtualFile? = knownFileSystems.findFileByPath(path)
