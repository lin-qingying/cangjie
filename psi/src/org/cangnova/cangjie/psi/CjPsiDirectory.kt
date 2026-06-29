/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.psi

import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.ui.Queryable
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.InvalidVirtualFileAccessException
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.navigation.NavigationRequest
import com.intellij.psi.*
import com.intellij.psi.impl.CheckUtil
import com.intellij.psi.impl.PsiElementBase
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.file.PsiBinaryFileImpl
import com.intellij.psi.impl.file.PsiDirectoryImpl
import com.intellij.psi.impl.file.UpdateAddedFileProcessor
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.search.PsiFileSystemItemProcessor
import com.intellij.psi.util.PsiUtilCore
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ArrayUtilRt
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ThrowableRunnable
import java.io.IOException

/**
 * 仓颉语言 PSI 目录实现
 *
 * 该类表示文件系统中的一个目录，用于仓颉语言项目的目录操作。
 * 继承自 [PsiElementBase] 并实现 [PsiDirectory] 和 [Queryable] 接口。
 *
 * ## 核心功能
 *
 * 1. **目录导航**: 获取父目录、子目录
 * 2. **文件管理**: 查找、创建、复制文件
 * 3. **子目录管理**: 创建、查找子目录
 * 4. **文件系统操作**: 重命名、删除、验证
 *
 * ## 使用场景
 *
 * ```kotlin
 * val directory: CjPsiDirectory = ...
 *
 * // 查找文件
 * val file = directory.findFile("main.cj")
 *
 * // 创建子目录
 * val subDir = directory.createSubdirectory("utils")
 *
 * // 遍历所有文件
 * directory.processChildren { item ->
 *     println(item.name)
 *     true
 * }
 * ```
 *
 * @param manager PSI 管理器实例
 * @param virtualFile 对应的虚拟文件
 *
 * @see PsiDirectory
 * @see PsiFileSystemItem
 */
class CjPsiDirectory(
    /**
     * 保存 `manager` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private val manager: PsiManager,
    /**
     * 保存 `virtualFile` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private val virtualFile: VirtualFile
) : PsiDirectoryImpl(manager as PsiManagerImpl,virtualFile), PsiDirectory, Queryable {

    companion object {
        private val LOG = Logger.getInstance(CjPsiDirectory::class.java)
        private val UPDATE_ADDED_FILE_KEY = Key.create<Boolean>("UPDATE_ADDED_FILE_KEY")
    }

    // ============================================================
    // 基本属性
    // ============================================================

    /**
     * 实现 `getVirtualFile` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getVirtualFile(): VirtualFile = virtualFile

    /**
     * 实现 `isDirectory` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isDirectory(): Boolean = true

    /**
     * 实现 `isValid` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isValid(): Boolean = virtualFile.isValid && !project.isDisposed

    /**
     * 实现 `getLanguage` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getLanguage(): Language = Language.ANY

    /**
     * 实现 `getManager` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getManager(): PsiManager = manager

    /**
     * 实现 `getName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String = virtualFile.name

    // ============================================================
    // 父目录和导航
    // ============================================================

    /**
     * 实现 `getParentDirectory` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getParentDirectory(): PsiDirectory? {
        val parentFile = virtualFile.parent ?: return null

        if (!parentFile.isValid) {
            LOG.error("Invalid parent: $parentFile of dir $virtualFile, dir.valid=${virtualFile.isValid}")
            return null
        }

        // 直接构造以避免通过 FileManagerImpl.findDirectoryImpl() 触发工作区文件索引检查
        // 在 IntelliJ 252+ 中，即使在 allowSlowOperations 内部，WorkspaceFileIndexDataImpl.ensureIsUpToDate
        // 也会在 EDT 上记录错误。直接构造可绕过该检查，对于目录树遍历等场景是正确的。
        return CjPsiDirectory(manager, parentFile)
    }

    /**
     * 实现 `getParent` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getParent(): PsiDirectory? = parentDirectory

    // ============================================================
    // 子目录和文件
    // ============================================================

    /**
     * 实现 `getSubdirectories` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getSubdirectories(): Array<PsiDirectory> {
        val files = virtualFile.children
        val dirs = mutableListOf<PsiDirectory>()

        for (file in files) {
            manager.findDirectory(file)?.let { dirs.add(it) }
        }

        return dirs.toTypedArray()
    }

    /**
     * 实现 `getFiles` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getFiles(): Array<PsiFile> {
        if (!virtualFile.isValid) {
            throw InvalidVirtualFileAccessException(virtualFile)
        }

        val files = virtualFile.children
        val psiFiles = mutableListOf<PsiFile>()

        for (file in files) {
            manager.findFile(file)?.let { psiFiles.add(it) }
        }

        return PsiUtilCore.toPsiFileArray(psiFiles)
    }

    /**
     * 实现 `findSubdirectory` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun findSubdirectory(name: String): PsiDirectory? {
        ProgressManager.checkCanceled()
        val childVFile = virtualFile.findChild(name) ?: return null
        return manager.findDirectory(childVFile)
    }

    /**
     * 实现 `findFile` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun findFile(name: String): PsiFile? {
        ProgressManager.checkCanceled()
        val childVFile = virtualFile.findChild(name) ?: return null

        if (!childVFile.isValid) {
            LOG.error(
                "Invalid file: $childVFile in dir $virtualFile, dir.valid=${virtualFile.isValid}",
                InvalidVirtualFileAccessException(childVFile)
            )
            return null
        }

        return manager.findFile(childVFile)
    }

    // ============================================================
    // 子元素处理
    // ============================================================

    /**
     * 实现 `processChildren` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun processChildren(processor: PsiElementProcessor<in PsiFileSystemItem>): Boolean {
        checkValid()

        for (vFile in virtualFile.children) {
            ProgressManager.checkCanceled()
            if (!vFile.isValid) continue

            val isDir = vFile.isDirectory
            if (processor is PsiFileSystemItemProcessor &&
                !processor.acceptItem(vFile.name, isDir)) {
                continue
            }

            val item = if (isDir) {
                manager.findDirectory(vFile)
            } else {
                manager.findFile(vFile)
            }

            if (item != null && !processor.execute(item)) {
                return false
            }
        }

        return true
    }

    /**
     * 实现 `getChildren` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getChildren(): Array<PsiElement> {
        checkValid()

        val children = mutableListOf<PsiElement>()
        processChildren { element ->
            children.add(element)
            true
        }

        return PsiUtilCore.toPsiElementArray(children)
    }

    // ============================================================
    // 目录操作
    // ============================================================

    /**
     * 实现 `createSubdirectory` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun createSubdirectory(name: String): PsiDirectory {
        checkCreateSubdirectory(name)

        try {
            val file = virtualFile.createChildDirectory(manager, name)
            return manager.findDirectory(file)
                ?: throw IncorrectOperationException("Cannot find directory in '${file.presentableUrl}'")
        } catch (e: IOException) {
            throw IncorrectOperationException(e.toString())
        }
    }

    /**
     * 实现 `checkCreateSubdirectory` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun checkCreateSubdirectory(name: String) {
        val existingFile = virtualFile.findChild(name)
        if (existingFile != null) {
            throw IncorrectOperationException(
                CjPsiBundle.message("file.already.exists.error", existingFile.presentableUrl)
            )
        }
        CheckUtil.checkWritable(this)
    }

    // ============================================================
    // 文件操作
    // ============================================================

    /**
     * 实现 `createFile` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun createFile(name: String): PsiFile {
        checkCreateFile(name)

        try {
            val vFile = virtualFile.createChildData(manager, name)
            return manager.findFile(vFile) ?: error("Cannot find file after creation: ${vFile.path}")
        } catch (e: IOException) {
            throw IncorrectOperationException(e)
        }
    }

    /**
     * 实现 `copyFileFrom` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun copyFileFrom(newName: String, originalFile: PsiFile): PsiFile {
        checkCreateFile(newName)

        val document = PsiDocumentManager.getInstance(project).getDocument(originalFile)
        document?.let { FileDocumentManager.getInstance().saveDocument(it) }

        val parent = virtualFile
        try {
            val vFile = originalFile.virtualFile
                ?: throw IncorrectOperationException("Cannot copy non-physical file: $originalFile")

            val copyVFile = when {
                parent.fileSystem == vFile.fileSystem -> {
                    vFile.copy(this, parent, newName)
                }
                vFile is LightVirtualFile -> {
                    parent.createChildData(this, newName).apply {
                        setBinaryContent(vFile.contentsToByteArray())
                    }
                }
                else -> {
                    VfsUtilCore.copyFile(this, vFile, parent, newName)
                }
            }

            if (getUserData(UPDATE_ADDED_FILE_KEY) != false) {
                DumbService.getInstance(project).completeJustSubmittedTasks()
                val copyPsi = findCopy(copyVFile, vFile)
                UpdateAddedFileProcessor.updateAddedFiles(
                    listOf(copyPsi),
                    listOf(originalFile)
                )
                return copyPsi
            }

            return findCopy(copyVFile, vFile)
        } catch (e: IOException) {
            throw IncorrectOperationException(e)
        }
    }

    /**
     * 执行 `findCopy` 内部辅助逻辑，支撑仓颉 PSI节点的结构解析与访问。
     */
    private fun findCopy(copyVFile: VirtualFile, originalVFile: VirtualFile): PsiFile {
        return manager.findFile(copyVFile)
            ?: throw IncorrectOperationException(
                "Could not find file $copyVFile after copying $originalVFile"
            )
    }

    /**
     * 在禁用更新添加文件的情况下执行操作
     */
    override fun <T : Throwable> executeWithUpdatingAddedFilesDisabled(runnable: ThrowableRunnable<T>) {
        try {
            putUserData(UPDATE_ADDED_FILE_KEY, false)
            runnable.run()
        } finally {
            putUserData(UPDATE_ADDED_FILE_KEY, null)
        }
    }

    /**
     * 实现 `checkCreateFile` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun checkCreateFile(name: String) {
        val existingFile = virtualFile.findChild(name)
        if (existingFile != null) {
            throw IncorrectOperationException(
                CjPsiBundle.message("file.already.exists.error", existingFile.presentableUrl)
            )
        }
        CheckUtil.checkWritable(this)
    }

    // ============================================================
    // 添加元素
    // ============================================================

    /**
     * 实现 `add` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun add(element: PsiElement): PsiElement {
        checkAdd(element)

        if (element !is PsiFile) {
            throw IncorrectOperationException("${element::class.java.name}")
        }

        val originalFile = element
        try {
            val newVFile = when (originalFile) {
                is PsiFileImpl -> {
                    val vFile = virtualFile.createChildData(manager, originalFile.name)
                    val text = originalFile.text
                    val psiFile = manager.findFile(vFile)
                    val document = psiFile?.viewProvider?.document
                    val fileDocumentManager = FileDocumentManager.getInstance()

                    if (document != null) {
                        document.setText(text)
                        if (psiFile.isPhysical) {
                            fileDocumentManager.saveDocument(document)
                        }
                        PsiDocumentManager.getInstance(project).commitDocument(document)
                    } else {
                        val lineSeparator = fileDocumentManager.getLineSeparator(vFile, project)
                        val finalText = if (lineSeparator != "\n") {
                            StringUtil.convertLineSeparators(text, lineSeparator)
                        } else {
                            text
                        }
                        LoadTextUtil.write(project, vFile, manager, finalText, -1)
                    }
                    vFile
                }
                is PsiBinaryFileImpl -> {
                    // 对于二进制文件，直接复制虚拟文件
                    VfsUtilCore.copyFile(null, originalFile.virtualFile, virtualFile)
                }
                else -> {
                    throw IncorrectOperationException("Unsupported file type: ${originalFile::class.java}")
                }
            }

            val newFile = manager.findFile(newVFile)
                ?: throw IncorrectOperationException("Could not find file $newVFile")

            UpdateAddedFileProcessor.updateAddedFiles(
                listOf(newFile),
                listOf(originalFile)
            )

            return newFile
        } catch (e: IOException) {
            throw IncorrectOperationException(e)
        }
    }

    /**
     * 实现 `checkAdd` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun checkAdd(element: PsiElement) {
        CheckUtil.checkWritable(this)

        if (element is PsiDirectory || element is PsiFile) {
            val name = element.name
            val caseSensitive = virtualFile.isCaseSensitive
            val existing = virtualFile.children.firstOrNull {
                Comparing.strEqual(it.name, name, caseSensitive)
            }

            if (existing != null) {
                val messageKey = if (existing.isDirectory) {
                    "dir.already.exists.error"
                } else {
                    "file.already.exists.error"
                }
                throw IncorrectOperationException(
                    CjPsiBundle.message(messageKey, existing.presentableUrl)
                )
            }
        } else {
            throw IncorrectOperationException(element::class.java.name)
        }
    }

    // ============================================================
    // 不支持的操作
    // ============================================================

    /**
     * 实现 `addBefore` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun addBefore(element: PsiElement, anchor: PsiElement?): PsiElement {
        throw IncorrectOperationException()
    }

    /**
     * 实现 `addAfter` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun addAfter(element: PsiElement, anchor: PsiElement?): PsiElement {
        throw IncorrectOperationException()
    }

    /**
     * 实现 `replace` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun replace(newElement: PsiElement): PsiElement {
        throw IncorrectOperationException()
    }

    // ============================================================
    // 删除和重命名
    // ============================================================

    /**
     * 实现 `delete` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun delete() {
        checkDelete()
        try {
            virtualFile.delete(manager)
        } catch (e: IOException) {
            throw IncorrectOperationException(e)
        }
    }

    /**
     * 实现 `checkDelete` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun checkDelete() {
        CheckUtil.checkDelete(virtualFile)
    }

    /**
     * 实现 `setName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun setName(name: String): PsiElement {
        checkSetName(name)

        try {
            virtualFile.rename(manager, name)
        } catch (e: IOException) {
            throw IncorrectOperationException(e.toString())
        }

        return this
    }

    /**
     * 实现 `checkSetName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun checkSetName(name: String) {
        CheckUtil.checkWritable(this)

        val parentFile = virtualFile.parent
            ?: throw IncorrectOperationException(
                CjPsiBundle.message("cannot.rename.root.directory", virtualFile.path)
            )

        val child = parentFile.findChild(name)
        if (child != null && child != virtualFile) {
            throw IncorrectOperationException(
                CjPsiBundle.message("dir.already.exists.error", child.presentableUrl)
            )
        }
    }

    // ============================================================
    // PSI 元素基础方法
    // ============================================================

    /**
     * 实现 `getContainingFile` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getContainingFile(): PsiFile? = null

    /**
     * 实现 `getTextRange` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getTextRange(): TextRange? = null

    /**
     * 实现 `getStartOffsetInParent` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getStartOffsetInParent(): Int = -1

    /**
     * 实现 `getTextLength` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getTextLength(): Int = -1

    /**
     * 实现 `findElementAt` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun findElementAt(offset: Int): PsiElement? = null

    /**
     * 实现 `getTextOffset` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getTextOffset(): Int = -1

    /**
     * 实现 `getText` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getText(): String = ""

    /**
     * 实现 `textToCharArray` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun textToCharArray(): CharArray = ArrayUtilRt.EMPTY_CHAR_ARRAY

    /**
     * 实现 `textMatches` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun textMatches(text: CharSequence): Boolean = false

    /**
     * 实现 `textMatches` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun textMatches(element: PsiElement): Boolean = false


    /**
     * 实现 `isPhysical` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isPhysical(): Boolean {
        return virtualFile.fileSystem !is NonPhysicalFileSystem &&
                virtualFile.fileSystem.protocol != "temp"
    }

    /**
     * 实现 `copy` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun copy(): PsiElement {
        throw IncorrectOperationException()
    }

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun accept(visitor: PsiElementVisitor) {
        visitor.visitDirectory(this)
    }

    /**
     * 实现 `getNode` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getNode(): ASTNode? = null

    // ============================================================
    // 导航支持
    // ============================================================

    /**
     * 实现 `canNavigateToSource` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun canNavigateToSource(): Boolean = false

    /**
     * 实现 `getPresentation` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getPresentation(): ItemPresentation? {
        return ItemPresentationProviders.getItemPresentation(this)
    }

    /**
     * 实现 `navigationRequest` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun navigationRequest(): NavigationRequest? {
        return NavigationRequest.directoryNavigationRequest(this)
    }

    /**
     * 实现 `navigate` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun navigate(requestFocus: Boolean) {
        PsiNavigationSupport.getInstance().navigateToDirectory(this, requestFocus)
    }

    // ============================================================
    // Queryable 接口
    // ============================================================

    /**
     * 实现 `putInfo` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun putInfo(info: MutableMap<in String, in String>) {
        info["fileName"] = name
    }

    // ============================================================
    // 工具方法
    // ============================================================

    /**
     * 执行 `checkValid` 内部辅助逻辑，支撑仓颉 PSI节点的结构解析与访问。
     */
    private fun checkValid() {
        if (!isValid) {
            throw PsiInvalidElementAccessException(this)
        }
    }

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String = "CjPsiDirectory:${virtualFile.presentableUrl}"

    /**
     * 实现 `equals` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CjPsiDirectory) return false

        return manager == other.manager && virtualFile == other.virtualFile
    }

    /**
     * 实现 `hashCode` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hashCode(): Int {
        var result = manager.hashCode()
        result = 31 * result + virtualFile.hashCode()
        return result
    }
}
