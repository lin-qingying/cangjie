@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.test.framework.projectStructure

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
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
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile

/**
 * 测试阶段允许回填依赖的模块视图。
 */
interface CaMutableTestModule : CaModule {
    /**
     * 测试模块的普通直接依赖。
     */
    override val directRegularDependencies: MutableList<CaModule>

    /**
     * 测试模块的 depends-on 直接依赖。
     */
    override val directDependsOnDependencies: MutableList<CaModule>

    /**
     * 测试模块的 friend 直接依赖。
     */
    override val directFriendDependencies: MutableList<CaModule>
}

/**
 * Analysis API 测试模块实现公共基类。
 */
sealed class CaTestModuleBase(
    final override val project: Project,
    /**
     * 当前测试模块的基础内容作用域。
     */
    override val baseContentScope: GlobalSearchScope,
    /**
     * 当前测试模块的目标平台。
     */
    private val moduleTargetPlatform: TargetPlatform = CangJiePlatforms.defaultCangJiePlatform,
) : CaModuleBase(), CaMutableTestModule {
    /**
     * 测试模块的可变普通依赖列表。
     */
    final override val directRegularDependencies: MutableList<CaModule> = mutableListOf()

    /**
     * 测试模块的可变 depends-on 依赖列表。
     */
    final override val directDependsOnDependencies: MutableList<CaModule> = mutableListOf()

    /**
     * 测试模块的可变 friend 依赖列表。
     */
    final override val directFriendDependencies: MutableList<CaModule> = mutableListOf()

    /**
     * 当前模块对外暴露的目标平台。
     */
    open override val targetPlatform: TargetPlatform
        get() = moduleTargetPlatform
}

/**
 * Analysis API 测试中的源码模块实现。
 */
class CaSourceModuleImpl(
    /**
     * 源码模块名称。
     */
    override val name: String,
    /**
     * 源码模块使用的语言版本设置。
     */
    override val languageVersionSettings: LanguageVersionSettings,
    project: Project,
    psiRoots: List<PsiFileSystemItem>,
    targetPlatform: TargetPlatform = CangJiePlatforms.defaultCangJiePlatform,
) : CaTestModuleBase(project, createSourceRootsContentScope(project, psiRoots), targetPlatform), CaSourceModule {
    /**
     * 源码模块包含的 PSI root 列表。
     */
    override val psiRoots: List<PsiFileSystemItem> = psiRoots.toList()

    /**
     * 返回源码模块名称，便于测试失败输出。
     */
    override fun toString(): String = name
}

/**
 * Analysis API 测试中的 library binary 模块实现。
 */
class CaLibraryModuleImpl(
    /**
     * library 模块名称。
     */
    override val libraryName: String,
    project: Project,
    binaryRoots: List<PsiFileSystemItem>,
    targetPlatform: TargetPlatform = CangJiePlatforms.defaultCangJiePlatform,
) : CaTestModuleBase(project, TestLibraryRootContentScope(project, binaryRoots), targetPlatform), CaLibraryModule {
    /**
     * library binary root 列表。
     */
    override val binaryRoots: List<PsiFileSystemItem> = binaryRoots.toList()

    /**
     * 测试 library binary 模块默认不作为 source-like use-site 参与解析。
     */
    override val isResolvable: Boolean
        get() = false

    /**
     * 返回 library 名称，便于测试失败输出。
     */
    override fun toString(): String = libraryName
}

/**
 * Analysis API 测试中的 library source 模块实现。
 */
class CaLibrarySourceModuleImpl(
    /**
     * library source 模块名称。
     */
    override val libraryName: String,
    /**
     * 当前源码模块对应的 library binary 模块。
     */
    override val binaryLibraryModule: CaLibraryModule,
    project: Project,
    sourceRoots: List<PsiFileSystemItem>,
) : CaTestModuleBase(
    project,
    createSourceRootsContentScope(project, sourceRoots),
    binaryLibraryModule.targetPlatform,
), CaLibrarySourceModule {
    /**
     * library source root 列表。
     */
    override val sourceRoots: List<PsiFileSystemItem> = sourceRoots.toList()

    /**
     * 返回 library 名称，便于测试失败输出。
     */
    override fun toString(): String = libraryName
}

/**
 * Analysis API 测试中的 builtins 模块实现。
 */
class CaBuiltinsModuleImpl(
    project: Project,
    scopeRoots: List<PsiFileSystemItem> = emptyList(),
    /**
     * builtins 模块名称。
     */
    override val builtinsName: String = "<test-builtins>",
    targetPlatform: TargetPlatform = CangJiePlatforms.defaultCangJiePlatform,
) : CaTestModuleBase(project, createSourceRootsContentScope(project, scopeRoots), targetPlatform), CaBuiltinsModule {
    /**
     * 测试 builtins 模块不作为普通源码 use-site 解析入口。
     */
    override val isResolvable: Boolean
        get() = false

    /**
     * builtins 虚文件 provider 提供的 builtins 内容作用域。
     */
    override val contentScope: GlobalSearchScope
        get() = BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)

    /**
     * 返回 builtins 名称，便于测试失败输出。
     */
    override fun toString(): String = builtinsName
}

/**
 * Analysis API 测试中不属于任何内容 root 的模块实现。
 */
class CaNotUnderContentRootModuleImpl(
    /**
     * 兜底模块名称。
     */
    override val name: String,
    /**
     * 若 PSI 可追溯到原始模块，则记录对应模块。
     */
    override val originalModule: CaModule?,
    project: Project,
    scopeRoots: List<PsiFileSystemItem>,
    targetPlatform: TargetPlatform = originalModule?.targetPlatform ?: CangJiePlatforms.defaultCangJiePlatform,
) : CaTestModuleBase(project, createSourceRootsContentScope(project, scopeRoots), targetPlatform), CaNotUnderContentRootModule {
    /**
     * 返回兜底模块名称，便于测试失败输出。
     */
    override fun toString(): String = name
}

/**
 * Analysis API 测试中的 library fallback dependencies 模块实现。
 */
class CaLibraryFallbackDependenciesModuleImpl(
    /**
     * 需要 fallback dependencies 的 library module。
     */
    private val dependentLibraryModule: CaLibraryModule,
) : CaModuleBase(), CaLibraryFallbackDependenciesModule {
    /**
     * fallback dependencies 所属 library 的名称。
     */
    override val dependencyOwnerName: String
        get() = dependentLibraryModule.libraryName

    /**
     * fallback dependencies 模块不作为普通解析入口。
     */
    override val isResolvable: Boolean
        get() = false

    /**
     * fallback dependencies 不再声明普通直接依赖。
     */
    override val directRegularDependencies: List<CaModule>
        get() = emptyList()

    /**
     * fallback dependencies 不再声明 depends-on 依赖。
     */
    override val directDependsOnDependencies: List<CaModule>
        get() = emptyList()

    /**
     * fallback dependencies 不再声明 friend 依赖。
     */
    override val directFriendDependencies: List<CaModule>
        get() = emptyList()

    /**
     * fallback dependencies 所属 project。
     */
    override val project: Project
        get() = dependentLibraryModule.project

    /**
     * fallback dependencies 使用所属 library 的目标平台。
     */
    override val targetPlatform: TargetPlatform
        get() = dependentLibraryModule.targetPlatform

    /**
     * fallback dependencies 覆盖 library scope 之外的测试库搜索空间。
     */
    override val baseContentScope: GlobalSearchScope
        get() = ProjectScope.getLibrariesScope(project).intersectWith(GlobalSearchScope.notScope(dependentLibraryModule.contentScope))

    /**
     * 返回 fallback 模块名称，便于测试失败输出。
     */
    override fun toString(): String = "$dependencyOwnerName.fallback"
}

/**
 * Analysis API 测试中的 dangling file 模块实现。
 */
class CaDanglingFileModuleImpl(
    /**
     * dangling file 模块名称。
     */
    override val name: String,
    /**
     * dangling file 使用的语言版本设置。
     */
    override val languageVersionSettings: LanguageVersionSettings,
    project: Project,
    psiRoots: List<PsiFileSystemItem>,
) : CaTestModuleBase(project, createSourceRootsContentScope(project, psiRoots)), CaDanglingFileModule {
    /**
     * 指向 dangling CjFile root 的智能指针集合。
     */
    private val filePointers: List<SmartPsiElementPointer<CjFile>> =
        psiRoots.map { psiRoot ->
            val file = psiRoot as? CjFile
                ?: error("Dangling file module expects CjFile roots, but got ${psiRoot::class.qualifiedName}")
            SmartPointerManager.getInstance(project).createSmartPsiElementPointer(file)
        }

    /**
     * dangling file 模块的 PSI root 列表。
     */
    override val psiRoots: List<PsiFileSystemItem> = psiRoots.toList()

    /**
     * 当前仍有效的 dangling 文件列表。
     */
    override val files: List<CjFile>
        get() = validFilesOrNull ?: error("Dangling file module is invalid")

    /**
     * dangling file 的上下文模块，由 project-structure 构造后回填。
     */
    override lateinit var contextModule: CaModule

    /**
     * dangling file 模块使用上下文模块的目标平台。
     */
    override val targetPlatform: TargetPlatform
        get() = contextModule.targetPlatform

    /**
     * dangling file 默认优先解析自身声明。
     */
    override val resolutionMode: CaDanglingFileResolutionMode = CaDanglingFileResolutionMode.PREFER_SELF

    /**
     * 标识该 dangling 模块是否承载代码片段。
     */
    override val isCodeFragment: Boolean
        get() = files.any { it is CjCodeFragment || it.isCodeFragment }

    /**
     * 标识所有 dangling 文件指针是否仍有效。
     */
    override val isValid: Boolean
        get() = validFilesOrNull != null

    /**
     * dangling 文件当前虚文件集合构成的内容作用域。
     */
    override val baseContentScope: GlobalSearchScope
        get() = GlobalSearchScope.filesWithoutLibrariesScope(project, files.map { it.viewProvider.virtualFile })

    /**
     * 返回 dangling 模块名称，便于测试失败输出。
     */
    override fun toString(): String = name

    /**
     * 若所有智能指针仍可解析为有效 CjFile，则返回文件列表。
     */
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

/**
 * 根据源码 root 列表创建 source-like 内容作用域。
 */
private fun createSourceRootsContentScope(
    project: Project,
    scopeRoots: List<PsiFileSystemItem>,
): GlobalSearchScope {
    return GlobalSearchScope.filesWithoutLibrariesScope(
        project,
        scopeRoots.mapNotNull { it.virtualFile },
    )
}

/**
 * 对齐 Kotlin `createLibraryModuleSearchScope(...)` 的职责边界：
 * library module 的 content scope 必须真实覆盖 binary roots 递归可达的库文件，
 * 不能沿用 source-style `filesWithoutLibrariesScope()` 把库文件自身排除掉。
 */
private class TestLibraryRootContentScope(
    project: Project,
    /**
     * library 模块暴露的 binary root 列表。
     */
    private val scopeRoots: List<PsiFileSystemItem>,
) : GlobalSearchScope(project) {
    /**
     * 从 library root 递归可达的全部虚文件。
     */
    private val reachableFiles: Set<VirtualFile> by lazy(LazyThreadSafetyMode.PUBLICATION) {
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

    /**
     * 判断指定虚文件是否属于 library root 覆盖范围。
     */
    override fun contains(file: VirtualFile): Boolean = file in reachableFiles

    /**
     * 测试 library scope 不定义文件排序。
     */
    override fun compare(file1: VirtualFile, file2: VirtualFile): Int = 0

    /**
     * 测试 library scope 不映射 IntelliJ module content。
     */
    override fun isSearchInModuleContent(aModule: Module): Boolean = false

    /**
     * 测试 library scope 明确表示会搜索 library 区域。
     */
    override fun isSearchInLibraries(): Boolean = true
}
