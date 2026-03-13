package org.cangnova.cangjie

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
