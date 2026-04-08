package org.cangnova.cangjie.analysis.api

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.LanguageVersionSettings

/**
 * Analysis API 视角下的分析宿主平台标识。
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
 * 这里承载 use-site 边界、依赖图边界、内容作用域和 session cache 边界。
 */
interface CaModule {
    val directRegularDependencies: List<CaModule>
        get() = emptyList()

    val directDependsOnDependencies: List<CaModule>
        get() = emptyList()

    val transitiveDependsOnDependencies: List<CaModule>
        get() = collectTransitiveDependsOnDependencies()

    val directFriendDependencies: List<CaModule>
        get() = emptyList()

    val allDirectDependencies: List<CaModule>
        get() = buildList {
            addAll(directRegularDependencies)
            addAll(directDependsOnDependencies)
            addAll(directFriendDependencies)
        }.distinct()

    val baseContentScope: GlobalSearchScope

    val contentScope: GlobalSearchScope
        get() = baseContentScope

    val project: Project

    val moduleDescription: String
        get() = this::class.simpleName ?: "CaModule"

    val stableModuleName: String?
        get() = null

    val targetPlatform: CaTargetPlatform
        get() = CaTargetPlatform.DEFAULT

    val isResolvable: Boolean
        get() = true
}

/**
 * 源码模块。
 */
interface CaSourceModule : CaModule {
    val name: String

    val languageVersionSettings: LanguageVersionSettings

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
 * fallback 依赖模块。
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
 * 代码片段、临时文件、预览文件等不受内容根管理的 use-site 模块。
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
