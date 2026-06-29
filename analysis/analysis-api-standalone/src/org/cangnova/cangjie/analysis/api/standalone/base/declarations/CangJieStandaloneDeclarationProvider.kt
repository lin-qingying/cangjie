@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.base.declarations

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieAnnotationsResolver
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieAnnotationsResolverFactory
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieCompositeDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderFactory
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderMerger
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieEmptyDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieFileBasedDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjAnnotated
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.isCangJieFileType

/**
 * Standalone 平台的声明 provider 工厂。
 *
 * 对齐 Kotlin `KotlinStandaloneDeclarationProviderFactory` 的框架职责：
 * 工厂从当前平台模块图收集作用域内源码文件，并为 low-level session 提供 source declaration provider。
 */
class CangJieStandaloneDeclarationProviderFactory(
    /**
     * 提供 PSI、模块结构和 decompiled binary index 的 standalone project。
     */
    private val project: Project,
) : CangJieDeclarationProviderFactory {
    /**
     * 当前 project 的 standalone 源文件收集器。
     */
    private val fileCollector = CangJieStandaloneSourceFileCollector(project)

    /**
     * 用于从 `.cjo` binary virtual file 恢复 decompiled PSI 文件的 PSI 管理器。
     */
    private val psiManager: PsiManager = PsiManager.getInstance(project)

    /**
     * 为指定搜索作用域创建 declaration provider。
     */
    override fun createDeclarationProvider(scope: GlobalSearchScope, contextualModule: CaModule?): CangJieDeclarationProvider {
        val providers = when (contextualModule) {
            is CaLibraryModule -> createLibraryDeclarationProviders(contextualModule, scope)
            else -> fileCollector.collect(scope).map(::CangJieFileBasedDeclarationProvider)
        }
        if (providers.isEmpty()) return CangJieEmptyDeclarationProvider

        return CangJieCompositeDeclarationProvider.create(providers)
    }

    /**
     * 对齐 Kotlin standalone declaration provider 对 library module 的 owner 边界：
     * source-like provider 只看 `allSourceFiles`，而 binary library 的声明事实必须绑定到库自己的
     * binary roots / decompiled PSI，不能继续错误依赖 source-file collector。
     */
    private fun createLibraryDeclarationProviders(
        libraryModule: CaLibraryModule,
        scope: GlobalSearchScope,
    ): List<CangJieFileBasedDeclarationProvider> {
        val rootFiles = fileCollector.collectFromRoots(libraryModule.binaryRoots, scope)
        val decompiledFiles = CaDecompiledBinaryIndex.getInstance(project)
            .getBinaryFiles(libraryModule)
            .asSequence()
            .filter(scope::contains)
            .mapNotNull { binaryFile -> psiManager.findFile(binaryFile) as? CjFile }
            .toList()

        return (rootFiles + decompiledFiles)
            .distinctBy { file -> file.virtualFile?.url ?: file.name }
            .map(::CangJieFileBasedDeclarationProvider)
    }
}

/**
 * Standalone 平台的声明 provider 合并器。
 */
class CangJieStandaloneDeclarationProviderMerger : CangJieDeclarationProviderMerger {
    /**
     * 将多个 declaration provider 合并为组合 provider。
     */
    override fun merge(providers: List<CangJieDeclarationProvider>): CangJieDeclarationProvider {
        return CangJieCompositeDeclarationProvider.create(providers)
    }
}

/**
 * Standalone 平台的注解解析器工厂。
 *
 * 当前仓颉 standalone 平台没有独立注解索引，空解析器表示“不提供额外索引事实”。
 */
class CangJieStandaloneAnnotationsResolverFactory : CangJieAnnotationsResolverFactory {
    /**
     * 返回 standalone 平台当前使用的空注解索引解析器。
     */
    override fun createAnnotationResolver(searchScope: GlobalSearchScope): CangJieAnnotationsResolver {
        return CangJieStandaloneEmptyAnnotationsResolver
    }
}

/**
 * 不提供额外注解索引事实的 standalone 注解解析器。
 */
private object CangJieStandaloneEmptyAnnotationsResolver : CangJieAnnotationsResolver {
    /**
     * 当前实现不按注解 class id 反查声明。
     */
    override fun declarationsByAnnotation(annotationClassId: ClassId): Set<CjAnnotated> = emptySet()

    /**
     * 当前实现不记录声明上的注解 class id 集合。
     */
    override fun annotationsOnDeclaration(declaration: CjAnnotated): Set<ClassId> = emptySet()
}

/**
 * 从当前平台模块图收集作用域内仓颉源码文件。
 */
internal class CangJieStandaloneSourceFileCollector(
    /**
     * 提供 project structure 和 PSI 管理器的 standalone project。
     */
    private val project: Project,
) {
    /**
     * 从虚拟文件恢复 PSI 文件的管理器。
     */
    private val psiManager: PsiManager = PsiManager.getInstance(project)

    /**
     * 当前 project 的模块结构 provider。
     */
    private val moduleProvider: CaModuleProvider
        get() = CaModuleProvider.getInstance(project)

    /**
     * 从当前模块图全部 source files 中收集位于 [scope] 内的仓颉文件。
     */
    fun collect(scope: GlobalSearchScope): List<CjFile> {
        return collectFromRoots(moduleProvider.allSourceFiles, scope)
    }

    /**
     * 为 standalone source roots 与 stub-origin library roots 提供同一套 PSI 文件收集规则。
     */
    fun collectFromRoots(
        roots: Iterable<PsiFileSystemItem>,
        scope: GlobalSearchScope,
    ): List<CjFile> {
        return buildList {
            roots.forEach { sourceRoot ->
                collectFromSourceRoot(sourceRoot, scope, this)
            }
        }.distinctBy { file -> file.virtualFile ?: file }
    }

    /**
     * 从单个 source root 收集仓颉文件。
     */
    private fun collectFromSourceRoot(
        sourceRoot: PsiFileSystemItem,
        scope: GlobalSearchScope,
        destination: MutableList<CjFile>,
    ) {
        when (sourceRoot) {
            is CjFile -> addIfInScope(sourceRoot, scope, destination)
            is PsiFile -> (sourceRoot as? CjFile)?.let { addIfInScope(it, scope, destination) }
            is PsiDirectory -> collectFromDirectory(sourceRoot, scope, destination)
        }
    }

    /**
     * 递归遍历目录并收集位于搜索作用域内的仓颉文件。
     */
    private fun collectFromDirectory(
        directory: PsiDirectory,
        scope: GlobalSearchScope,
        destination: MutableList<CjFile>,
    ) {
        VfsUtilCore.visitChildrenRecursively(directory.virtualFile, object : VirtualFileVisitor<Void>() {
            override fun visitFile(file: VirtualFile): Boolean {
                if (file.isDirectory || !file.isCangJieFileType()) {
                    return true
                }
                if (!scope.contains(file)) {
                    return true
                }
                (psiManager.findFile(file) as? CjFile)?.let(destination::add)
                return true
            }
        })
    }

    /**
     * 如果文件没有虚拟文件或虚拟文件位于作用域内，则加入目标列表。
     */
    private fun addIfInScope(
        file: CjFile,
        scope: GlobalSearchScope,
        destination: MutableList<CjFile>,
    ) {
        val virtualFile = file.virtualFile
        if (virtualFile == null || scope.contains(virtualFile)) {
            destination += file
        }
    }
}
