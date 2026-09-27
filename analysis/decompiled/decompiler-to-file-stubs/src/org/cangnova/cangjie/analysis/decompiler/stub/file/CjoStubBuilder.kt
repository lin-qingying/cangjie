package org.cangnova.cangjie.analysis.decompiler.stub.file

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.BinaryFileStubBuilder
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.Stub
import com.intellij.util.indexing.FileContent

/**
 * 仓颉 compiled binary file stub 构建协议。
 */
abstract class CjoStubBuilder : BinaryFileStubBuilder {
    /**
     * 非零正数，用于标识 compiled stub 版本。
     */
    abstract override fun getStubVersion(): Int

    /** 判断当前二进制文件是否由该 CJO builder 负责。 */
    abstract override fun acceptsFile(file: VirtualFile): Boolean

    /**
     * 从 binary 文件内容构建 file stub；不应处理的辅助文件可返回 `null`。
     */
    abstract fun buildFileStub(fileContent: FileContent): PsiFileStub<*>?

    /** 将 CJO file stub 暴露给 IntelliJ 通用二进制 stub loader。 */
    override fun buildStubTree(fileContent: FileContent): Stub? = buildFileStub(fileContent)
}
