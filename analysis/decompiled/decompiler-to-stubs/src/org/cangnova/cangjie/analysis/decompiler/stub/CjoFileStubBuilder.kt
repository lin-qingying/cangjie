package org.cangnova.cangjie.analysis.decompiler.stub

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.psi.stubs.CangJieCompiledFileErrors

/**
 * 对齐 Kotlin metadata/file-stub builder 的 owner：
 * 这里只负责从已加载的 `.cjo package` 直接生成 file stub。
 */
object CjoFileStubBuilder {
    /**
     * 从 `.cjo` package 与 module data 构建反编译文件 stub。
     *
     * 版本不兼容或反序列化失败时返回 invalid file stub，让 PSI/编辑器层能够展示明确错误文本；
     * 正常路径会先加载 CFIR 声明，再交给通用 stub 构建函数创建仓颉文件 stub。
     */
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
