package org.cangnova.cangjie.analysis.decompiler.stub.file

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.cangnova.cangjie.analysis.decompiler.stub.CjoFileStubBuilder
import org.cangnova.cangjie.analysis.decompiler.stub.LoadedCjoPackage
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.psi.stubs.CangJieCompiledFileErrors
import java.io.IOException

/**
 * 对位 Kotlin `KotlinMetadataStubBuilder`。
 *
 * `.cjo` 文件级 compiled stub 的 owner 仍然固定在 decompiler-to-file-stubs，
 * 这里只负责：
 * - 判断文件是否属于反编译链；
 * - 从 binary 恢复 package 数据与真实 moduleData owner；
 * - 把已加载 package 交给 `CjoFileStubBuilder` 生成 file stub。
 */
abstract class CangJieMetadataStubBuilder : CjoStubBuilder() {
    /**
     * 当前 stub builder 支持的编译文件类型。
     */
    protected abstract val supportedFileType: FileType
    /**
     * 从二进制文件读取可用于构建 stub 的 metadata。
     */
    protected abstract fun readFile(
        virtualFile: VirtualFile,
        content: ByteArray?,
        project: Project?,
    ): FileWithMetadata?

    /**
     * 判断虚拟文件是否包含当前 builder 可处理的 metadata。
     */
    protected open fun hasMetadata(virtualFile: VirtualFile): Boolean = readFile(virtualFile, null, project = null) != null

    /**
     * 判断文件类型或扩展名是否受当前 builder 支持。
     */
    fun isSupported(file: VirtualFile): Boolean {
        val supportedType = supportedFileType
        return file.extension == supportedType.defaultExtension || file.fileType == supportedType
    }

    /**
     * 判断文件是否可构建 compiled stub。
     */
    fun hasStub(file: VirtualFile): Boolean = isSupported(file) && file.readSafely { hasMetadata(file) } == true

    /**
     * 在无项目上下文时安全读取文件 metadata。
     */
    fun readFileSafely(file: VirtualFile, content: ByteArray? = null): FileWithMetadata? = file.readSafely {
        readFile(file, content, project = null)
    }

    /**
     * 在给定项目上下文中安全读取文件 metadata。
     */
    fun readFileSafely(file: VirtualFile, content: ByteArray? = null, project: Project?): FileWithMetadata? = file.readSafely {
        readFile(file, content, project)
    }

    /**
     * 从 IntelliJ indexing file content 构建 compiled file stub。
     */
    final override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
        val virtualFile = content.file
        check(isSupported(virtualFile)) { "Unexpected compiled file type: ${virtualFile.fileType.name}" }

        val file = readFileSafely(virtualFile, content.content, content.project) ?: return null
        return when (file) {
            is FileWithMetadata.Incompatible -> org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl.forInvalid(file.errorText)
            is FileWithMetadata.Compatible -> CjoFileStubBuilder.buildFileStub(file.loadedPackage, file.moduleData)
        }
    }

    /**
     * 从编译文件读取到的 metadata 结果。
     */
    sealed class FileWithMetadata {
        /**
         * metadata 存在但当前反编译器无法兼容读取。
         */
        class Incompatible(
            /**
             * 展示给用户的反编译错误文本。
             */
            val errorText: String = CangJieCompiledFileErrors.NEWER_VERSION_DECOMPILE_ERROR,
        ) : FileWithMetadata()

        /**
         * metadata 已成功读取且版本兼容。
         */
        class Compatible(
            /**
             * 已加载的 `.cjo` package 数据。
             */
            val loadedPackage: LoadedCjoPackage,
            /**
             * 该 package 对应的 CFIR module data owner。
             */
            val moduleData: CfirModuleData,
        ) : FileWithMetadata()
    }
}

/**
 * 安全读取虚拟文件内容并吞掉 I/O 异常。
 */
private inline fun <T> VirtualFile.readSafely(action: () -> T): T? = try {
    if (isValid) {
        action()
    } else {
        null
    }
} catch (_: IOException) {
    null
}
