package org.cangnova.cangjie.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.StubBuilder
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType

/**
 * `.cjo` 等编译产物恢复出的只读 PSI 文件。
 *
 * 它的真源是二进制元数据，而不是可编辑源码，因此需要显式持有：
 * 1. compiled stub builder；
 * 2. decompiled 文本提供器。
 */
open class CjDecompiledFile(
    viewProvider: FileViewProvider,
    private val decompiledStubBuilder: StubBuilder,
    private val decompiledTextProvider: () -> String,
) : CjFile(viewProvider, isCompiled = true) {
    override val customStubBuilder: StubBuilder
        get() = decompiledStubBuilder

    override fun getFileType(): FileType = CangJieBuiltInFileType

    override fun getText(): String = decompiledTextProvider()

    override fun toString(): String = "CangJie Decompiled File: $name"
}
