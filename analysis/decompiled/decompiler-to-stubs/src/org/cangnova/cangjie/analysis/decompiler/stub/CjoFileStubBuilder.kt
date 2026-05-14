package org.cangnova.cangjie.analysis.decompiler.stub

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.psi.stubs.CangJieCompiledFileErrors

/**
 * 对齐 Kotlin metadata/file-stub builder 的 owner：
 * 这里只负责从已加载的 `.cjo package` 直接生成 file stub。
 */
object CjoFileStubBuilder {
    fun buildFileStub(
        loadedPackage: LoadedCjoPackage,
        moduleData: CfirModuleData,
    ): org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl {
        if (!loadedPackage.isVersionSupported) {
            return org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl.forInvalid(
                CangJieCompiledFileErrors.NEWER_VERSION_DECOMPILE_ERROR,
            )
        }

        return runCatching {
            val declarations = CjoDeclarationLoader.loadDeclarations(loadedPackage, moduleData)
            createDecompiledFileStub(loadedPackage, declarations)
        }.getOrElse { throwable ->
            val errorText = buildString {
                appendLine("// Could not decompile .cjo package: ${loadedPackage.packageFqName.asString()}")
                appendLine("// ${throwable::class.simpleName}: ${throwable.message.orEmpty()}")
            }.trimEnd()
            org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl.forInvalid(errorText)
        }
    }
}
