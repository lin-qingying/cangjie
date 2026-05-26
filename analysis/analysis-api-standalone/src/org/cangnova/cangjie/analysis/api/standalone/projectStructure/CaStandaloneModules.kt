@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.projectStructure

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryFallbackDependenciesModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleBase
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile

/**
 * Standalone 平台模块基类。
 */
sealed class CaStandaloneModule(
    final override val project: Project,
    private val scopeRoots: List<PsiFileSystemItem>,
) : CaModuleBase() {
    final override val directRegularDependencies: MutableList<CaModule> = mutableListOf()
    final override val directDependsOnDependencies: MutableList<CaModule> = mutableListOf()
    final override val directFriendDependencies: MutableList<CaModule> = mutableListOf()

    override val baseContentScope: GlobalSearchScope =
        StandaloneRootContentScope(project, scopeRoots)
}

class CaStandaloneSourceModule(
    override val name: String,
    override val languageVersionSettings: LanguageVersionSettings,
    project: Project,
    override val psiRoots: List<PsiFileSystemItem>,
) : CaStandaloneModule(project, psiRoots), CaSourceModule

class CaStandaloneLibraryModule(
    override val libraryName: String,
    project: Project,
    override val binaryRoots: List<PsiFileSystemItem>,
) : CaStandaloneModule(project, binaryRoots), CaLibraryModule

class CaStandaloneLibrarySourceModule(
    override val libraryName: String,
    override val binaryLibraryModule: CaLibraryModule,
    project: Project,
    override val sourceRoots: List<PsiFileSystemItem>,
) : CaStandaloneModule(project, sourceRoots), CaLibrarySourceModule

class CaStandaloneLibraryFallbackDependenciesModule(
    override val dependencyOwnerName: String,
    project: Project,
    scopeRoots: List<PsiFileSystemItem>,
) : CaStandaloneModule(project, scopeRoots), CaLibraryFallbackDependenciesModule

class CaStandaloneBuiltinsModule(
    project: Project,
    scopeRoots: List<PsiFileSystemItem> = emptyList(),
    override val builtinsName: String = "<builtins>",
) : CaStandaloneModule(project, scopeRoots), CaBuiltinsModule {
    override val contentScope: GlobalSearchScope
        get() = BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)
}

class CaStandaloneDanglingFileModule(
    override val name: String,
    override val languageVersionSettings: LanguageVersionSettings,
    override val contextModule: CaModule,
    override val resolutionMode: CaDanglingFileResolutionMode = CaDanglingFileResolutionMode.PREFER_SELF,
    project: Project,
    override val psiRoots: List<PsiFileSystemItem>,
) : CaStandaloneModule(project, psiRoots), CaDanglingFileModule {
    private val filePointers: List<SmartPsiElementPointer<CjFile>> =
        psiRoots.map { psiRoot ->
            val file = psiRoot as? CjFile
                ?: error("Dangling file module expects CjFile roots, but got ${psiRoot::class.qualifiedName}")
            SmartPointerManager.getInstance(project).createSmartPsiElementPointer(file)
        }

    override val files: List<CjFile>
        get() = validFilesOrNull ?: error("Dangling file module is invalid")

    override val isCodeFragment: Boolean
        get() = files.any { it is CjCodeFragment || it.isCodeFragment }

    override val isValid: Boolean
        get() = validFilesOrNull != null

    override val baseContentScope: GlobalSearchScope
        get() = GlobalSearchScope.filesWithoutLibrariesScope(project, files.map { it.viewProvider.virtualFile })

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

class CaStandaloneNotUnderContentRootModule(
    override val name: String,
    override val originalModule: CaModule?,
    project: Project,
    scopeRoots: List<PsiFileSystemItem>,
) : CaStandaloneModule(project, scopeRoots), CaNotUnderContentRootModule

/**
 * Standalone 根作用域。
 *
 * 默认的 `filesWithoutLibrariesScope()` 能覆盖普通目录与文件 root，
 * 但不会把“目录 root 下通过目录链接暴露出来的文件”判进 scope。
 * standalone 的 source roots 明确允许目录 root，因此这里在默认 scope 之外，
 * 懒加载一份“从 root 递归可达的 VirtualFile 集合”，只在默认判定失败时再做补充。
 */
private class StandaloneRootContentScope(
    project: Project,
    private val scopeRoots: List<PsiFileSystemItem>,
) : GlobalSearchScope(project) {
    private val directScope = GlobalSearchScope.filesWithoutLibrariesScope(project, scopeRoots.mapNotNull { it.virtualFile })

    private val reachableFilesByTraversal: Set<VirtualFile> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildSet {
            scopeRoots.mapNotNull { it.virtualFile }.forEach { root ->
                add(root)
                if (root.isDirectory) {
                    VfsUtilCore.visitChildrenRecursively(root, object : VirtualFileVisitor<Void>() {
                        override fun visitFile(file: VirtualFile): Boolean {
                            add(file)
                            return true
                        }
                    })
                }
            }
        }
    }

    override fun contains(file: VirtualFile): Boolean {
        return directScope.contains(file) || file in reachableFilesByTraversal
    }

    override fun compare(file1: VirtualFile, file2: VirtualFile): Int = 0

    override fun isSearchInModuleContent(aModule: Module): Boolean = false

    override fun isSearchInLibraries(): Boolean = false
}
