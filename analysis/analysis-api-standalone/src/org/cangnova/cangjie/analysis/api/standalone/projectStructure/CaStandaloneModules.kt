@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
    org.cangnova.cangjie.analysis.api.CaImplementationDetail::class,
)

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
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.CaBuiltinsModuleImpl
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleBase
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile

/**
 * Standalone 平台模块基类。
 */
sealed class CaStandaloneModule(
    final override val project: Project,
    scopeRoots: List<PsiFileSystemItem>,
    final override val targetPlatform: TargetPlatform = CangJiePlatforms.defaultCangJiePlatform,
) : CaModuleBase() {
    /**
     * standalone 模块的普通直接依赖列表，由 session builder 在构造模块图时填充。
     */
    final override val directRegularDependencies: MutableList<CaModule> = mutableListOf()

    /**
     * standalone 模块的 depends-on 直接依赖列表，用于多平台/分层模块图中的可见性传播。
     */
    final override val directDependsOnDependencies: MutableList<CaModule> = mutableListOf()

    /**
     * standalone 模块的 friend 直接依赖列表，用于允许内部声明可见的测试或特殊编译场景。
     */
    final override val directFriendDependencies: MutableList<CaModule> = mutableListOf()

    /**
     * 由模块 root 构成的基础内容作用域。
     */
    override val baseContentScope: GlobalSearchScope =
        StandaloneRootContentScope(project, scopeRoots)
}

/**
 * standalone 环境中的源码模块。
 */
class CaStandaloneSourceModule(
    /**
     * 源码模块的稳定显示名。
     */
    override val name: String,
    /**
     * 该源码模块参与解析时使用的语言版本设置。
     */
    override val languageVersionSettings: LanguageVersionSettings,
    project: Project,
    /**
     * 构成该源码模块内容的 PSI root 列表。
     */
    override val psiRoots: List<PsiFileSystemItem>,
    targetPlatform: TargetPlatform = CangJiePlatforms.defaultCangJiePlatform,
) : CaStandaloneModule(project, psiRoots, targetPlatform), CaSourceModule

/**
 * standalone 环境中的二进制库模块。
 */
class CaStandaloneLibraryModule(
    /**
     * 库模块的稳定库名。
     */
    override val libraryName: String,
    project: Project,
    /**
     * 该库模块暴露的二进制 root 列表。
     */
    override val binaryRoots: List<PsiFileSystemItem>,
    targetPlatform: TargetPlatform = CangJiePlatforms.defaultCangJiePlatform,
) : CaStandaloneModule(project, binaryRoots, targetPlatform), CaLibraryModule

/**
 * standalone 环境中的库源码模块。
 */
class CaStandaloneLibrarySourceModule(
    /**
     * 库源码模块对应的稳定库名。
     */
    override val libraryName: String,
    /**
     * 该源码模块附着的二进制库模块。
     */
    override val binaryLibraryModule: CaLibraryModule,
    project: Project,
    /**
     * 该库源码模块暴露的源码 root 列表。
     */
    override val sourceRoots: List<PsiFileSystemItem>,
    targetPlatform: TargetPlatform = binaryLibraryModule.targetPlatform,
) : CaStandaloneModule(project, sourceRoots, targetPlatform), CaLibrarySourceModule

/**
 * standalone 环境中的库 fallback 依赖模块。
 */
class CaStandaloneLibraryFallbackDependenciesModule(
    /**
     * 触发 fallback 依赖模块创建的依赖拥有者名称。
     */
    override val dependencyOwnerName: String,
    project: Project,
    scopeRoots: List<PsiFileSystemItem>,
    targetPlatform: TargetPlatform = CangJiePlatforms.defaultCangJiePlatform,
) : CaStandaloneModule(project, scopeRoots, targetPlatform), CaLibraryFallbackDependenciesModule

/**
 * standalone 环境中的内建声明模块。
 */
class CaStandaloneBuiltinsModule(
    project: Project,
    scopeRoots: List<PsiFileSystemItem> = emptyList(),
    /**
     * 内建模块的稳定名称。
     */
    override val builtinsName: String = "<builtins>",
    targetPlatform: TargetPlatform = CangJiePlatforms.defaultCangJiePlatform,
) : CaStandaloneModule(project, scopeRoots, targetPlatform), CaBuiltinsModule {
    /**
     * 由基础内建模块实现提供的内建内容作用域。
     */
    override val contentScope: GlobalSearchScope
        get() = CaBuiltinsModuleImpl(targetPlatform, project).contentScope
}

/**
 * standalone 环境中为单个悬挂文件或代码片段创建的临时模块。
 */
class CaStandaloneDanglingFileModule(
    /**
     * 悬挂文件模块的稳定名称。
     */
    override val name: String,
    /**
     * 悬挂文件参与解析时使用的语言版本设置。
     */
    override val languageVersionSettings: LanguageVersionSettings,
    /**
     * 为悬挂文件提供上下文依赖和平台信息的宿主模块。
     */
    override val contextModule: CaModule,
    /**
     * 悬挂文件与上下文模块合并解析时使用的解析模式。
     */
    override val resolutionMode: CaDanglingFileResolutionMode = CaDanglingFileResolutionMode.PREFER_SELF,
    project: Project,
    /**
     * 该悬挂模块管理的 PSI 文件 root 列表。
     */
    override val psiRoots: List<PsiFileSystemItem>,
) : CaStandaloneModule(project, psiRoots, contextModule.targetPlatform), CaDanglingFileModule {
    /**
     * 指向悬挂文件的智能指针列表，用于在 PSI 变更后判断模块是否仍有效。
     */
    private val filePointers: List<SmartPsiElementPointer<CjFile>> =
        psiRoots.map { psiRoot ->
            val file = psiRoot as? CjFile
                ?: error("Dangling file module expects CjFile roots, but got ${psiRoot::class.qualifiedName}")
            SmartPointerManager.getInstance(project).createSmartPsiElementPointer(file)
        }

    /**
     * 当前仍有效的悬挂文件集合。
     */
    override val files: List<CjFile>
        get() = validFilesOrNull ?: error("Dangling file module is invalid")

    /**
     * 标识该悬挂模块是否承载代码片段。
     */
    override val isCodeFragment: Boolean
        get() = files.any { it is CjCodeFragment || it.isCodeFragment }

    /**
     * 标识所有悬挂文件智能指针是否仍能解析到有效 PSI。
     */
    override val isValid: Boolean
        get() = validFilesOrNull != null

    /**
     * 基于当前悬挂文件虚文件集合构造的内容作用域。
     */
    override val baseContentScope: GlobalSearchScope
        get() = GlobalSearchScope.filesWithoutLibrariesScope(project, files.map { it.viewProvider.virtualFile })

    /**
     * 若全部智能指针仍有效则返回文件列表，否则返回 `null` 表示模块失效。
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
 * standalone 环境中承载不属于任何内容 root 的 PSI 的兜底模块。
 */
class CaStandaloneNotUnderContentRootModule(
    /**
     * 兜底模块的稳定名称。
     */
    override val name: String,
    /**
     * 若该文件可追溯到原始模块，则记录对应模块；否则为 `null`。
     */
    override val originalModule: CaModule?,
    project: Project,
    scopeRoots: List<PsiFileSystemItem>,
    targetPlatform: TargetPlatform = originalModule?.targetPlatform ?: CangJiePlatforms.defaultCangJiePlatform,
) : CaStandaloneModule(project, scopeRoots, targetPlatform), CaNotUnderContentRootModule

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
    /**
     * 构成该 scope 的 PSI root 列表。
     */
    private val scopeRoots: List<PsiFileSystemItem>,
) : GlobalSearchScope(project) {
    /**
     * IntelliJ 默认文件 scope，用于快速覆盖直接文件和普通目录 root。
     */
    private val directScope = filesWithoutLibrariesScope(project, scopeRoots.mapNotNull { it.virtualFile })

    /**
     * 从 root 递归遍历后可达的所有虚文件集合，用于补齐目录链接等默认 scope 覆盖不到的情况。
     */
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

    /**
     * 判断指定虚文件是否属于 standalone root 内容作用域。
     */
    override fun contains(file: VirtualFile): Boolean {
        return directScope.contains(file) || file in reachableFilesByTraversal
    }

    /**
     * standalone root scope 不定义文件排序，保持所有文件同级。
     */
    override fun compare(file1: VirtualFile, file2: VirtualFile): Int = 0

    /**
     * standalone scope 不映射 IntelliJ module content。
     */
    override fun isSearchInModuleContent(aModule: Module): Boolean = false

    /**
     * standalone root scope 只覆盖显式 root，不主动搜索 IDE 库区。
     */
    override fun isSearchInLibraries(): Boolean = false
}
