package org.cangnova.cangjie.analysis.api.standalone.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.CaLibraryFallbackDependenciesModule
import org.cangnova.cangjie.analysis.api.CaLibraryModule
import org.cangnova.cangjie.analysis.api.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.CaScriptDependencyModule
import org.cangnova.cangjie.analysis.api.CaScriptModule
import org.cangnova.cangjie.analysis.api.CaSourceModule
import org.cangnova.cangjie.analysis.api.CaTargetPlatform

/**
 * Standalone 平台模块实现基类。
 *
 * 它们是纯数据模块对象，不依赖 IDE content root 或 workspace model，
 * 适用于 CLI、批处理分析和测试框架自建项目图。
 */
sealed class CaStandaloneModule(
    final override val project: Project,
    private val scopeRoots: List<PsiFileSystemItem>,
) : CaModule {
    final override val directRegularDependencies: MutableList<CaModule> = mutableListOf()
    final override val directDependsOnDependencies: MutableList<CaModule> = mutableListOf()
    final override val directFriendDependencies: MutableList<CaModule> = mutableListOf()

    final override val targetPlatform: CaTargetPlatform
        get() = CaTargetPlatform.STANDALONE

    final override val baseContentScope: GlobalSearchScope =
        GlobalSearchScope.filesWithoutLibrariesScope(project, scopeRoots.mapNotNull { it.virtualFile })
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
) : CaStandaloneModule(project, scopeRoots), CaBuiltinsModule

class CaStandaloneScriptModule(
    override val name: String,
    override val languageVersionSettings: LanguageVersionSettings,
    project: Project,
    override val psiRoots: List<PsiFileSystemItem>,
) : CaStandaloneModule(project, psiRoots), CaScriptModule

class CaStandaloneScriptDependencyModule(
    override val scriptName: String,
    project: Project,
    scopeRoots: List<PsiFileSystemItem>,
) : CaStandaloneModule(project, scopeRoots), CaScriptDependencyModule

class CaStandaloneDanglingFileModule(
    override val name: String,
    override val languageVersionSettings: LanguageVersionSettings,
    override val contextModule: CaModule?,
    project: Project,
    override val psiRoots: List<PsiFileSystemItem>,
) : CaStandaloneModule(project, psiRoots), CaDanglingFileModule

class CaStandaloneNotUnderContentRootModule(
    override val name: String,
    override val originalModule: CaModule?,
    project: Project,
    scopeRoots: List<PsiFileSystemItem>,
) : CaStandaloneModule(project, scopeRoots), CaNotUnderContentRootModule
