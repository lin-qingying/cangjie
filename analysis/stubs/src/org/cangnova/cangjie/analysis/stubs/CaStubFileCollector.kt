@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.stubs

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledPsiProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.isCangJieFileType

/**
 * 统一收集 analysis:stubs 需要观察的 `CjFile`。
 *
 * 设计约束：
 * - source 侧只从 project structure 暴露的 source roots 收集；
 * - compiled 侧只从 decompiled binary index + compiled PSI 链路恢复；
 * - analysis:stubs 不再自行反编译 `.cjo` 文本，也不维护第二套 compiled 发现逻辑。
 */
internal class CaStubFileCollector(
    private val project: Project,
) {
    private val psiManager: PsiManager = PsiManager.getInstance(project)

    fun collectFiles(): List<CjFile> {
        val files = linkedSetOf<CjFile>()
        collectSourceFiles(files)
        collectCompiledFiles(files)
        return files.toList()
    }

    private fun collectSourceFiles(destination: MutableSet<CjFile>) {
        val projectStructureProvider = CaModuleProvider.getInstance(project)
        projectStructureProvider.allSourceFiles.forEach { item ->
            collectCangJieFiles(item, destination)
        }
    }

    private fun collectCompiledFiles(destination: MutableSet<CjFile>) {
        val decompiledBinaryIndex = project.getService(CaDecompiledBinaryIndex::class.java) ?: return
        val decompiledPsiProvider = project.getService(CaDecompiledPsiProvider::class.java) ?: return
        val projectStructureProvider = CaModuleProvider.getInstance(project)

        projectStructureProvider.allModules.forEach { module ->
            when (module) {
                is CaLibraryModule -> {
                    decompiledBinaryIndex.getBinaryFiles(module)
                        .mapNotNull(decompiledPsiProvider::getDecompiledFile)
                        .forEach(destination::add)
                }

                is CaBuiltinsModule -> {
                    decompiledBinaryIndex.getBinaryFiles(module)
                        .mapNotNull(decompiledPsiProvider::getDecompiledFile)
                        .forEach(destination::add)
                }
            }
        }
    }

    private fun collectCangJieFiles(
        item: PsiFileSystemItem,
        destination: MutableSet<CjFile>,
    ) {
        when (item) {
            is CjFile -> destination += item
            is PsiFile -> (item as? CjFile)?.let(destination::add)
            is PsiDirectory -> {
                VfsUtilCore.iterateChildrenRecursively(item.virtualFile, null) { virtualFile ->
                    if (virtualFile.isDirectory || !virtualFile.isCangJieStubCandidate()) {
                        return@iterateChildrenRecursively true
                    }
                    (psiManager.findFile(virtualFile) as? CjFile)?.let(destination::add)
                    true
                }
            }
        }
    }

    private fun com.intellij.openapi.vfs.VirtualFile.isCangJieStubCandidate(): Boolean {
        return fileType == CangJieBuiltInFileType ||
            extension.equals("cjo", ignoreCase = true) ||
            isCangJieFileType()
    }
}
