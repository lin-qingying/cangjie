@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.stubs

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
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
    /**
     * 提供项目结构、decompiled binary index 和 PSI 管理器的 IntelliJ project。
     */
    private val project: Project,
) {
    /**
     * 用于从虚拟文件恢复 `CjFile` PSI 的项目级 PSI 管理器。
     */
    private val psiManager: PsiManager = PsiManager.getInstance(project)

    /**
     * 收集当前项目结构中 analysis:stubs 应观察的所有仓颉文件。
     *
     * 返回结果同时包含源码文件和已反编译的 `.cjo` compiled PSI 文件，并通过 `LinkedHashSet` 去重保序。
     */
    fun collectFiles(): List<CjFile> {
        val files = linkedSetOf<CjFile>()
        collectSourceFiles(files)
        collectCompiledFiles(files)
        return files.toList()
    }

    /**
     * 从 project structure 暴露的源码根中收集仓颉源码文件。
     */
    private fun collectSourceFiles(destination: MutableSet<CjFile>) {
        val projectStructureProvider = CaModuleProvider.getInstance(project)
        projectStructureProvider.allSourceFiles.forEach { item ->
            collectCangJieFiles(item, destination)
        }
    }

    /**
     * 从已注册的 `.cjo` decompiled binary index 中收集 compiled PSI 文件。
     */
    private fun collectCompiledFiles(destination: MutableSet<CjFile>) {
        val decompiledBinaryIndex = project.getService(CaDecompiledBinaryIndex::class.java) ?: return
        val projectStructureProvider = CaModuleProvider.getInstance(project)

        projectStructureProvider.allModules.forEach { module ->
            when (module) {
                is CaLibraryModule -> {
                    decompiledBinaryIndex.getBinaryFiles(module)
                        .mapNotNull(psiManager::findFile)
                        .filterIsInstance<CjFile>()
                        .forEach(destination::add)
                }

                is CaBuiltinsModule -> {
                    decompiledBinaryIndex.getBinaryFiles(module)
                        .mapNotNull(psiManager::findFile)
                        .filterIsInstance<CjFile>()
                        .forEach(destination::add)
                }
            }
        }
    }

    /**
     * 从 PSI 文件系统项递归收集仓颉源文件或 compiled `.cjo` 文件。
     */
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

    /**
     * 判断虚拟文件是否可能生成 analysis:stubs 需要的仓颉 PSI。
     */
    private fun com.intellij.openapi.vfs.VirtualFile.isCangJieStubCandidate(): Boolean {
        return fileType == CangJieBuiltInFileType ||
            extension.equals("cjo", ignoreCase = true) ||
            isCangJieFileType()
    }
}
