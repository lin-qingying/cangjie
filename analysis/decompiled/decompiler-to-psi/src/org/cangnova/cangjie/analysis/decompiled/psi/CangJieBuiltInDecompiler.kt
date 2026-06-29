package org.cangnova.cangjie.analysis.decompiled.psi

import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile
import org.cangnova.cangjie.analysis.decompiler.stub.file.CangJieMetadataStubBuilder

/**
 * 对位 Kotlin `KotlinBuiltInDecompiler`。
 */
class CangJieBuiltInDecompiler : CangJieMetadataDecompiler() {
    /**
     * 返回 builtins `.cjo` 文件专用的 metadata stub builder。
     */
    override fun getStubBuilder(): CangJieMetadataStubBuilder = CangJieBuiltInMetadataStubBuilder

    /**
     * 为 builtins `.cjo` 文件创建仓颉反编译 PSI 文件实例。
     */
    override fun createFile(viewProvider: CangJieDecompiledFileViewProvider): CjDecompiledFile {
        return CangJieBuiltinsDecompiledFile(viewProvider)
    }
}
