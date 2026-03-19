package org.cangnova.cangjie

import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * 源文件抽象。
 *
 * 表示编译器处理的一个源文件，提供文件名、路径和内容访问。
 * 对应 K2 的 KtSourceFile。
 */
interface CjSourceFile {
    /** 源文件名（不含路径） */
    val name: String

    /** 源文件完整路径（虚拟文件可能为 null） */
    val path: String?

    /** 获取源文件内容的输入流 */
    fun getContentsAsStream(): InputStream
}

class CjPsiSourceFile(val psiFile: PsiFile) : CjSourceFile {
    override val name: String
        get() = psiFile.name

    override val path: String?
        get() = psiFile.virtualFile?.path

    override fun getContentsAsStream(): InputStream = psiFile.virtualFile.inputStream
}

class CjVirtualFileSourceFile(val virtualFile: VirtualFile) : CjSourceFile {
    override val name: String
        get() = virtualFile.name

    override val path: String
        get() = virtualFile.path

    override fun getContentsAsStream(): InputStream = virtualFile.inputStream
}

class CjIoFileSourceFile(val file: File) : CjSourceFile {
    override val name: String
        get() = file.name

    override val path: String
        get() = FileUtilRt.toSystemIndependentName(file.path)

    override fun getContentsAsStream(): InputStream = file.inputStream()
}

class CjInMemoryTextSourceFile(
    override val name: String,
    override val path: String?,
    val text: CharSequence,
) : CjSourceFile {
    override fun getContentsAsStream(): InputStream = ByteArrayInputStream(text.toString().toByteArray())
}
