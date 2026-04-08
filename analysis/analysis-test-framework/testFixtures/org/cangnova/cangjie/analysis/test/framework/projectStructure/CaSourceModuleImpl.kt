package org.cangnova.cangjie.analysis.test.framework.projectStructure

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
import org.cangnova.cangjie.analysis.api.CaSourceModule
import org.cangnova.cangjie.analysis.api.CaTargetPlatform
import org.cangnova.cangjie.analysis.api.decompiled.CaBuiltinsVirtualFileProvider

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
    private val platform: CaTargetPlatform,
) : CaMutableTestModule {
    final override val directRegularDependencies: MutableList<CaModule> = mutableListOf()

    final override val directDependsOnDependencies: MutableList<CaModule> = mutableListOf()

    final override val directFriendDependencies: MutableList<CaModule> = mutableListOf()

    final override val targetPlatform: CaTargetPlatform
        get() = platform

    final override val baseContentScope: GlobalSearchScope = GlobalSearchScope.filesWithoutLibrariesScope(
        project,
        scopeRoots.mapNotNull { it.virtualFile },
    )
}

class CaSourceModuleImpl(
    override val name: String,
    override val languageVersionSettings: LanguageVersionSettings,
    project: Project,
    psiRoots: List<PsiFileSystemItem>,
    targetPlatform: CaTargetPlatform = CaTargetPlatform.STANDALONE,
) : CaTestModuleBase(project, psiRoots, targetPlatform), CaSourceModule {
    override val psiRoots: List<PsiFileSystemItem> = psiRoots.toList()

    override fun toString(): String = name
}

class CaLibraryModuleImpl(
    override val libraryName: String,
    project: Project,
    binaryRoots: List<PsiFileSystemItem>,
    targetPlatform: CaTargetPlatform = CaTargetPlatform.STANDALONE,
) : CaTestModuleBase(project, binaryRoots, targetPlatform), CaLibraryModule {
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
    targetPlatform: CaTargetPlatform = CaTargetPlatform.STANDALONE,
) : CaTestModuleBase(project, sourceRoots, targetPlatform), CaLibrarySourceModule {
    override val sourceRoots: List<PsiFileSystemItem> = sourceRoots.toList()

    override fun toString(): String = libraryName
}

class CaBuiltinsModuleImpl(
    project: Project,
    scopeRoots: List<PsiFileSystemItem> = emptyList(),
    override val builtinsName: String = "<test-builtins>",
    targetPlatform: CaTargetPlatform = CaTargetPlatform.STANDALONE,
) : CaTestModuleBase(project, scopeRoots, targetPlatform), CaBuiltinsModule {
    override val isResolvable: Boolean
        get() = false

    override val contentScope: GlobalSearchScope
        get() = CaBuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)

    override fun toString(): String = builtinsName
}

class CaLibraryFallbackDependenciesModuleImpl(
    override val dependencyOwnerName: String,
    project: Project,
    scopeRoots: List<PsiFileSystemItem>,
    targetPlatform: CaTargetPlatform = CaTargetPlatform.STANDALONE,
) : CaTestModuleBase(project, scopeRoots, targetPlatform), CaLibraryFallbackDependenciesModule {
    override val isResolvable: Boolean
        get() = false

    override fun toString(): String = "$dependencyOwnerName.fallback"
}

class CaDanglingFileModuleImpl(
    override val name: String,
    override val languageVersionSettings: LanguageVersionSettings,
    project: Project,
    psiRoots: List<PsiFileSystemItem>,
    targetPlatform: CaTargetPlatform = CaTargetPlatform.STANDALONE,
) : CaTestModuleBase(project, psiRoots, targetPlatform), CaDanglingFileModule {
    override val psiRoots: List<PsiFileSystemItem> = psiRoots.toList()

    override var contextModule: CaModule? = null

    override fun toString(): String = name
}

class CaNotUnderContentRootModuleImpl(
    override val name: String,
    project: Project,
    scopeRoots: List<PsiFileSystemItem>,
    targetPlatform: CaTargetPlatform = CaTargetPlatform.STANDALONE,
) : CaTestModuleBase(project, scopeRoots, targetPlatform), CaNotUnderContentRootModule {
    override var originalModule: CaModule? = null

    override fun toString(): String = name
}
