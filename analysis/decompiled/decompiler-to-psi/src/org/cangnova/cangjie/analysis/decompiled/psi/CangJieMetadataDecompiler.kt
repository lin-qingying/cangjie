package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile
import org.cangnova.cangjie.analysis.decompiler.stub.file.CangJieMetadataStubBuilder

/**
 * 对位 Kotlin `KotlinMetadataDecompiler`。
 *
 * 这里只保留 decompiler 顶层入口职责：
 * - 接受文件；
 * - 指定 stub builder；
 * - 通过 view provider 创建 decompiled PSI。
 */
abstract class CangJieMetadataDecompiler : CjoFileDecompilers.Full() {
    final override fun accepts(file: VirtualFile): Boolean = getStubBuilder().isSupported(file)

    abstract override fun getStubBuilder(): CangJieMetadataStubBuilder

    protected abstract fun createFile(viewProvider: CangJieDecompiledFileViewProvider): CjDecompiledFile

    final override fun createFileViewProvider(
        file: VirtualFile,
        manager: PsiManager,
        physical: Boolean,
    ): CangJieDecompiledFileViewProvider = CangJieDecompiledFileViewProvider(manager, file, physical) { provider ->
        if (getStubBuilder().hasStub(provider.virtualFile)) {
            createFile(provider)
        } else {
            null
        }
    }
}
