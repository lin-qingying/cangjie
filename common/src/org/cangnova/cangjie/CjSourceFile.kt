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

/**
 * 基于 PSI 文件的源文件实现。
 */
class CjPsiSourceFile(
    /**
     * 被包装的 PSI 文件。
     */
    val psiFile: PsiFile,
) : CjSourceFile {
    /**
     * PSI 文件名。
     */
    override val name: String
        get() = psiFile.name

    /**
     * PSI 文件关联虚拟文件的路径。
     */
    override val path: String?
        get() = psiFile.virtualFile?.path

    /**
     * 读取 PSI 关联虚拟文件的内容流。
     */
    override fun getContentsAsStream(): InputStream = psiFile.virtualFile.inputStream
}

/**
 * 基于 IntelliJ VirtualFile 的源文件实现。
 */
class CjVirtualFileSourceFile(
    /**
     * 被包装的虚拟文件。
     */
    val virtualFile: VirtualFile,
) : CjSourceFile {
    /**
     * 虚拟文件名。
     */
    override val name: String
        get() = virtualFile.name

    /**
     * 虚拟文件路径。
     */
    override val path: String
        get() = virtualFile.path

    /**
     * 读取虚拟文件内容流。
     */
    override fun getContentsAsStream(): InputStream = virtualFile.inputStream
}

/**
 * 基于本地 IO 文件的源文件实现。
 */
class CjIoFileSourceFile(
    /**
     * 被包装的本地文件。
     */
    val file: File,
) : CjSourceFile {
    /**
     * 本地文件名。
     */
    override val name: String
        get() = file.name

    /**
     * 使用系统无关分隔符表示的本地文件路径。
     */
    override val path: String
        get() = FileUtilRt.toSystemIndependentName(file.path)

    /**
     * 读取本地文件内容流。
     */
    override fun getContentsAsStream(): InputStream = file.inputStream()
}

/**
 * 基于内存文本的源文件实现。
 */
class CjInMemoryTextSourceFile(
    /**
     * 内存源文件名。
     */
    override val name: String,
    /**
     * 内存源文件路径；没有稳定路径时为 null。
     */
    override val path: String?,
    /**
     * 内存源文件文本。
     */
    val text: CharSequence,
) : CjSourceFile {
    /**
     * 将内存文本转换为输入流。
     */
    override fun getContentsAsStream(): InputStream = ByteArrayInputStream(text.toString().toByteArray())
}
