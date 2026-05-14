package org.cangnova.cangjie.analysis.decompiler.stub.file

import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent

/**
 * 仓颉 compiled binary file stub 构建协议。
 */
abstract class CjoStubBuilder {
    /**
     * 非零正数，用于标识 compiled stub 版本。
     */
    abstract fun getStubVersion(): Int

    /**
     * 从 binary 文件内容构建 file stub；不应处理的辅助文件可返回 `null`。
     */
    abstract fun buildFileStub(fileContent: FileContent): PsiFileStub<*>?
}
