@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.ProjectScope
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
 * 测试阶段允许回填依赖的模块视图。
 */
interface CaMutableTestModule : CaModule {
    override val directRegularDependencies: MutableList<CaModule>

    override val directDependsOnDependencies: MutableList<CaModule>

    override val directFriendDependencies: MutableList<CaModule>
}

/**
 * Analysis API 测试模块实现公共基类。
 */
sealed class CaTestModuleBase(
    final override val project: Project,
    private val scopeRoots: List<PsiFileSystemItem>,
) : CaModuleBase(), CaMutableTestModule {
    final override val directRegularDependencies: MutableList<CaModule> = mutableListOf()

    final override val directDependsOnDependencies: MutableList<CaModule> = mutableListOf()

    final override val directFriendDependencies: MutableList<CaModule> = mutableListOf()

    override val baseContentScope: GlobalSearchScope = GlobalSearchScope.filesWithoutLibrariesScope(
        project,
        scopeRoots.mapNotNull { it.virtualFile },
    )
}

class CaSourceModuleImpl(
    override val name: String,
    override val languageVersionSettings: LanguageVersionSettings,
    project: Project,
    psiRoots: List<PsiFileSystemItem>,
) : CaTestModuleBase(project, psiRoots), CaSourceModule {
    override val psiRoots: List<PsiFileSystemItem> = psiRoots.toList()

    override fun toString(): String = name
}

class CaLibraryModuleImpl(
    override val libraryName: String,
    project: Project,
    binaryRoots: List<PsiFileSystemItem>,
) : CaTestModuleBase(project, binaryRoots), CaLibraryModule {
    override val binaryRoots: List<PsiFileSystemItem> = binaryRoots.toList()

    override val isResolvable: Boolean
        get() = false

    override fun toString(): String = libraryName
}

class CaLibrarySourceModuleImpl(
    override val libraryName: String,
    override val binaryLibraryModule: CaLibraryModule,
    project: Project,
    sourceRoots: List<PsiFileSystemItem>,
) : CaTestModuleBase(project, sourceRoots), CaLibrarySourceModule {
    override val sourceRoots: List<PsiFileSystemItem> = sourceRoots.toList()

    override fun toString(): String = libraryName
}

class CaBuiltinsModuleImpl(
    project: Project,
    scopeRoots: List<PsiFileSystemItem> = emptyList(),
    override val builtinsName: String = "<test-builtins>",
) : CaTestModuleBase(project, scopeRoots), CaBuiltinsModule {
    override val isResolvable: Boolean
        get() = false

    override val contentScope: GlobalSearchScope
        get() = BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)

    override fun toString(): String = builtinsName
}

class CaNotUnderContentRootModuleImpl(
    override val name: String,
    override val originalModule: CaModule?,
    project: Project,
    scopeRoots: List<PsiFileSystemItem>,
) : CaTestModuleBase(project, scopeRoots), CaNotUnderContentRootModule {
    override fun toString(): String = name
}

class CaLibraryFallbackDependenciesModuleImpl(
    private val dependentLibraryModule: CaLibraryModule,
) : CaModuleBase(), CaLibraryFallbackDependenciesModule {
    override val dependencyOwnerName: String
        get() = dependentLibraryModule.libraryName

    override val isResolvable: Boolean
        get() = false

    override val directRegularDependencies: List<CaModule>
        get() = emptyList()

    override val directDependsOnDependencies: List<CaModule>
        get() = emptyList()

    override val directFriendDependencies: List<CaModule>
        get() = emptyList()

    override val project: Project
        get() = dependentLibraryModule.project

    override val baseContentScope: GlobalSearchScope
        get() = ProjectScope.getLibrariesScope(project).intersectWith(GlobalSearchScope.notScope(dependentLibraryModule.contentScope))

    override fun toString(): String = "$dependencyOwnerName.fallback"
}

class CaDanglingFileModuleImpl(
    override val name: String,
    override val languageVersionSettings: LanguageVersionSettings,
    project: Project,
    psiRoots: List<PsiFileSystemItem>,
) : CaTestModuleBase(project, psiRoots), CaDanglingFileModule {
    private val filePointers: List<SmartPsiElementPointer<CjFile>> =
        psiRoots.map { psiRoot ->
            val file = psiRoot as? CjFile
                ?: error("Dangling file module expects CjFile roots, but got ${psiRoot::class.qualifiedName}")
            SmartPointerManager.getInstance(project).createSmartPsiElementPointer(file)
        }

    override val psiRoots: List<PsiFileSystemItem> = psiRoots.toList()

    override val files: List<CjFile>
        get() = validFilesOrNull ?: error("Dangling file module is invalid")

    override lateinit var contextModule: CaModule

    override val resolutionMode: CaDanglingFileResolutionMode = CaDanglingFileResolutionMode.PREFER_SELF

    override val isCodeFragment: Boolean
        get() = files.any { it is CjCodeFragment || it.isCodeFragment }

    override val isValid: Boolean
        get() = validFilesOrNull != null

    override val baseContentScope: GlobalSearchScope
        get() = GlobalSearchScope.filesWithoutLibrariesScope(project, files.map { it.viewProvider.virtualFile })

    override fun toString(): String = name

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
