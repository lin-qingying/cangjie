package org.cangnova.cangjie.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.CaLibraryModule
import org.cangnova.cangjie.analysis.api.CaLibraryFallbackDependenciesModule
import org.cangnova.cangjie.analysis.api.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.CaScriptDependencyModule
import org.cangnova.cangjie.analysis.api.CaScriptModule
import org.cangnova.cangjie.analysis.api.CaSourceModule
import org.cangnova.cangjie.analysis.api.CaTargetPlatform

/**
 * 测试框架中的可变依赖模块视图。
 *
 * 测试项目结构工厂分两轮构建模块图：先创建模块，再回填依赖。
 * 因此测试模块实现需要显式暴露可变依赖集合，但这类可变性只存在于测试装配阶段。
 */
interface CaMutableTestModule : CaModule {
    override val directRegularDependencies: MutableList<CaModule>

    override val directDependsOnDependencies: MutableList<CaModule>

    override val directFriendDependencies: MutableList<CaModule>
}

/**
 * Analysis API 测试框架模块实现的公共基类。
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

/**
 * 源码模块实现。
 */
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

/**
 * 脚本源码模块实现。
 */
class CaScriptModuleImpl(
    override val name: String,
    override val languageVersionSettings: LanguageVersionSettings,
    project: Project,
    psiRoots: List<PsiFileSystemItem>,
    targetPlatform: CaTargetPlatform = CaTargetPlatform.STANDALONE,
) : CaTestModuleBase(project, psiRoots, targetPlatform), CaScriptModule {
    override val psiRoots: List<PsiFileSystemItem> = psiRoots.toList()

    override fun toString(): String = name
}

/**
 * 测试中的库二进制模块。
 *
 * 当前仍以 `PsiFileSystemItem` 视图建模二进制输出，便于测试框架在未接入真实编译产物前
 * 先把 library dependency 语义打通。
 */
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

/**
 * 测试中的库源码模块。
 */
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

/**
 * 代码片段/游离文件模块实现。
 */
/**
 * 测试框架中的 builtins 模块。
 *
 * 它显式承载“任何可解析模块都天然能看到一组基础符号”这一平台语义，
 * 避免测试模块图只表达源码间显式依赖，而遗漏 builtins 这一真实边界。
 */
class CaBuiltinsModuleImpl(
    project: Project,
    scopeRoots: List<PsiFileSystemItem> = emptyList(),
    override val builtinsName: String = "<test-builtins>",
    targetPlatform: CaTargetPlatform = CaTargetPlatform.STANDALONE,
) : CaTestModuleBase(project, scopeRoots, targetPlatform), CaBuiltinsModule {
    override val isResolvable: Boolean
        get() = false

    override fun toString(): String = builtinsName
}

/**
 * 测试框架中的 fallback dependencies 模块。
 *
 * dangling file 与 not-under-content-root 往往还会看到一组平台默认依赖，
 * 这里单独建模，便于测试框架区分“主模块本体”与“宿主注入依赖”。
 */
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

/**
 * 测试框架中的脚本依赖模块。
 *
 * 脚本本体之外还存在宿主注入的额外依赖，这里拆成独立模块，
 * 使脚本测试在依赖拓扑上与普通源码模块保持可区分的语义。
 */
class CaScriptDependencyModuleImpl(
    override val scriptName: String,
    project: Project,
    scopeRoots: List<PsiFileSystemItem>,
    targetPlatform: CaTargetPlatform = CaTargetPlatform.STANDALONE,
) : CaTestModuleBase(project, scopeRoots, targetPlatform), CaScriptDependencyModule {
    override val isResolvable: Boolean
        get() = false

    override fun toString(): String = "$scriptName.scriptDependencies"
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

/**
 * 非内容根模块实现。
 */
class CaNotUnderContentRootModuleImpl(
    override val name: String,
    project: Project,
    scopeRoots: List<PsiFileSystemItem>,
    targetPlatform: CaTargetPlatform = CaTargetPlatform.STANDALONE,
) : CaTestModuleBase(project, scopeRoots, targetPlatform), CaNotUnderContentRootModule {
    override var originalModule: CaModule? = null

    override fun toString(): String = name
}
