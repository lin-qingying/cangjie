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
    /**
     * 判断指定文件是否可由当前 metadata stub builder 处理。
     */
    final override fun accepts(file: VirtualFile): Boolean = getStubBuilder().isSupported(file)

    /**
     * 返回当前 decompiler 使用的 `.cjo` stub builder。
     */
    abstract override fun getStubBuilder(): CangJieMetadataStubBuilder

    /**
     * 根据 view provider 创建具体反编译 PSI 文件。
     */
    protected abstract fun createFile(viewProvider: CangJieDecompiledFileViewProvider): CjDecompiledFile

    /**
     * 创建带 metadata 校验的仓颉反编译 view provider。
     *
     * 只有 stub builder 能从虚拟文件读取 stub 时才构造 [CjDecompiledFile]，
     * 否则返回空 PSI，使平台继续按无可用反编译结果处理该文件。
     */
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
