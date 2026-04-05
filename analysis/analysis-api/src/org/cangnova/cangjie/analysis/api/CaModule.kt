package org.cangnova.cangjie.analysis.api

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.LanguageVersionSettings

/**
 * Analysis API 视角下的分析宿主平台标识。
 *
 * 这里描述的是“模块图和会话运行所在的平台上下文”，
 * 而不是代码生成后端或目标运行时。
 */
data class CaTargetPlatform(
    val platformId: String,
) {
    companion object {
        val DEFAULT = CaTargetPlatform("default")
        val IDE = CaTargetPlatform("ide")
        val STANDALONE = CaTargetPlatform("standalone")
        val LSP = CaTargetPlatform("lsp")
    }
}

/**
 * Analysis API 视角下的模块抽象。
 *
 * 模块不是“名称 + 文件列表”的轻量壳，而是同时承载：
 * 1. use-site 语义边界；
 * 2. 依赖图边界；
 * 3. content scope 边界；
 * 4. session cache 与失效传播边界。
 */
interface CaModule {
    /**
     * 当前模块的直接 regular dependencies。
     */
    val directRegularDependencies: List<CaModule>
        get() = emptyList()

    /**
     * 当前模块的直接 dependsOn 依赖。
     */
    val directDependsOnDependencies: List<CaModule>
        get() = emptyList()

    /**
     * 当前模块的传递 dependsOn 闭包。
     */
    val transitiveDependsOnDependencies: List<CaModule>
        get() = collectTransitiveDependsOnDependencies()

    /**
     * 当前模块的直接 friend 依赖。
     */
    val directFriendDependencies: List<CaModule>
        get() = emptyList()

    /**
     * 当前模块在 Analysis API 语义中直接可见的所有依赖。
     */
    val allDirectDependencies: List<CaModule>
        get() = buildList {
            addAll(directRegularDependencies)
            addAll(directDependsOnDependencies)
            addAll(directFriendDependencies)
        }.distinct()

    /**
     * 模块在平台项目中的基础内容范围。
     */
    val baseContentScope: GlobalSearchScope

    /**
     * 模块最终暴露给 Analysis API 的内容范围。
     *
     * 平台可以在 [baseContentScope] 之上进一步精炼；
     * Analysis API 只要求模块显式暴露最终结果。
     */
    val contentScope: GlobalSearchScope
        get() = baseContentScope

    /**
     * 模块所属的 IntelliJ Project。
     */
    val project: Project

    /**
     * 面向调试、缓存和错误报告的描述信息。
     */
    val moduleDescription: String
        get() = this::class.simpleName ?: "CaModule"

    /**
     * 参与符号身份、可见性判断和 session cache 划分的稳定模块名。
     */
    val stableModuleName: String?
        get() = null

    /**
     * 当前模块所属的分析宿主平台。
     */
    val targetPlatform: CaTargetPlatform
        get() = CaTargetPlatform.DEFAULT

    /**
     * 当前模块是否允许直接作为 use-site module 进入分析。
     */
    val isResolvable: Boolean
        get() = true
}

/**
 * 源码模块。
 */
interface CaSourceModule : CaModule {
    val name: String

    val languageVersionSettings: LanguageVersionSettings

    /**
     * 模块显式暴露给 Analysis API 的源码根。
     */
    val psiRoots: List<PsiFileSystemItem>
        get() = emptyList()

    override val moduleDescription: String
        get() = "Sources of $name"

    override val stableModuleName: String?
        get() = name
}

/**
 * 库二进制模块。
 */
interface CaLibraryModule : CaModule {
    val libraryName: String

    val binaryRoots: List<PsiFileSystemItem>
        get() = emptyList()

    override val moduleDescription: String
        get() = "Library binaries of $libraryName"

    override val stableModuleName: String?
        get() = libraryName
}

/**
 * 库源码模块。
 */
interface CaLibrarySourceModule : CaModule {
    val libraryName: String

    val binaryLibraryModule: CaLibraryModule

    val sourceRoots: List<PsiFileSystemItem>
        get() = emptyList()

    override val moduleDescription: String
        get() = "Library sources of $libraryName"

    override val stableModuleName: String?
        get() = libraryName
}

/**
 * 库 fallback 依赖模块。
 *
 * 当 use-site 文件不在常规内容根下，但仍需要看到一组默认库依赖时，
 * 平台应显式用该模块承载这部分可见性边界。
 */
interface CaLibraryFallbackDependenciesModule : CaModule {
    val dependencyOwnerName: String

    override val moduleDescription: String
        get() = "Fallback dependencies of $dependencyOwnerName"
}

/**
 * 内建模块。
 */
interface CaBuiltinsModule : CaModule {
    val builtinsName: String
        get() = "<builtins>"

    override val moduleDescription: String
        get() = "Builtins module $builtinsName"

    override val stableModuleName: String?
        get() = builtinsName
}

/**
 * 脚本模块。
 */
interface CaScriptModule : CaSourceModule {
    override val moduleDescription: String
        get() = "Script $name"
}

/**
 * 脚本依赖模块。
 */
interface CaScriptDependencyModule : CaModule {
    val scriptName: String

    override val moduleDescription: String
        get() = "Script dependencies of $scriptName"
}

/**
 * 游离文件模块。
 *
 * 用于代码片段、临时文件、预览文件等不受项目内容根管理的场景。
 */
interface CaDanglingFileModule : CaSourceModule {
    val contextModule: CaModule?

    override val moduleDescription: String
        get() = "Dangling file module $name"
}

/**
 * 不在内容根下的模块。
 */
interface CaNotUnderContentRootModule : CaModule {
    val name: String

    /**
     * 若该模块是从某个真实模块派生出的临时分析视图，
     * 则显式保留其原始模块。
     */
    val originalModule: CaModule?
        get() = null

    override val moduleDescription: String
        get() = "Not-under-content-root module $name"
}

private fun CaModule.collectTransitiveDependsOnDependencies(): List<CaModule> {
    val result = linkedSetOf<CaModule>()

    fun visit(module: CaModule) {
        module.directDependsOnDependencies.forEach { dependency ->
            if (result.add(dependency)) {
                visit(dependency)
            }
        }
    }

    visit(this)
    return result.toList()
}
