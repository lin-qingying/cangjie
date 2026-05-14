@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.base.declarations

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieAnnotationsResolver
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieAnnotationsResolverFactory
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieCompositeDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderFactory
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderMerger
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieEmptyDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieFileBasedDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
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
    private val project: Project,
) : CangJieDeclarationProviderFactory {
    private val fileCollector = CangJieStandaloneSourceFileCollector(project)

    override fun createDeclarationProvider(scope: GlobalSearchScope, contextualModule: CaModule?): CangJieDeclarationProvider {
        val files = fileCollector.collect(scope)
        if (files.isEmpty()) return CangJieEmptyDeclarationProvider

        return CangJieCompositeDeclarationProvider.create(files.map(::CangJieFileBasedDeclarationProvider))
    }
}

/**
 * Standalone 平台的声明 provider 合并器。
 */
class CangJieStandaloneDeclarationProviderMerger : CangJieDeclarationProviderMerger {
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
    override fun createAnnotationResolver(searchScope: GlobalSearchScope): CangJieAnnotationsResolver {
        return CangJieStandaloneEmptyAnnotationsResolver
    }
}

private object CangJieStandaloneEmptyAnnotationsResolver : CangJieAnnotationsResolver {
    override fun declarationsByAnnotation(annotationClassId: ClassId): Set<CjAnnotated> = emptySet()

    override fun annotationsOnDeclaration(declaration: CjAnnotated): Set<ClassId> = emptySet()
}

/**
 * 从当前平台模块图收集作用域内仓颉源码文件。
 */
internal class CangJieStandaloneSourceFileCollector(
    private val project: Project,
) {
    private val psiManager: PsiManager = PsiManager.getInstance(project)
    private val moduleProvider: CaModuleProvider
        get() = CaModuleProvider.getInstance(project)

    fun collect(scope: GlobalSearchScope): List<CjFile> {
        return buildList {
            moduleProvider.allSourceFiles.forEach { sourceRoot ->
                collectFromSourceRoot(sourceRoot, scope, this)
            }
        }.distinctBy { file -> file.virtualFile ?: file }
    }

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

    private fun collectFromDirectory(
        directory: PsiDirectory,
        scope: GlobalSearchScope,
        destination: MutableList<CjFile>,
    ) {
        VfsUtilCore.iterateChildrenRecursively(directory.virtualFile, null) { virtualFile ->
            if (virtualFile.isDirectory || !virtualFile.isCangJieFileType()) {
                return@iterateChildrenRecursively true
            }
            if (!scope.contains(virtualFile)) {
                return@iterateChildrenRecursively true
            }
            (psiManager.findFile(virtualFile) as? CjFile)?.let(destination::add)
            true
        }
    }

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
