package org.cangnova.cangjie.analysis.decompiled.psi

import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile
import org.cangnova.cangjie.analysis.decompiler.stub.file.CangJieMetadataStubBuilder

/**
 * 对位 Kotlin `KotlinBuiltInDecompiler`。
 */
class CangJieBuiltInDecompiler : CangJieMetadataDecompiler() {
    override fun getStubBuilder(): CangJieMetadataStubBuilder = CangJieBuiltInMetadataStubBuilder

    override fun createFile(viewProvider: CangJieDecompiledFileViewProvider): CjDecompiledFile {
        return CangJieBuiltinsDecompiledFile(viewProvider)
    }
}
