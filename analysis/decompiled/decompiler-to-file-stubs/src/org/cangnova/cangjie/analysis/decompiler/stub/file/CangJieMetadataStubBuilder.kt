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
    protected abstract val supportedFileType: FileType
    protected abstract fun readFile(
        virtualFile: VirtualFile,
        content: ByteArray?,
        project: Project?,
    ): FileWithMetadata?

    protected open fun hasMetadata(virtualFile: VirtualFile): Boolean = readFile(virtualFile, null, project = null) != null

    fun isSupported(file: VirtualFile): Boolean {
        val supportedType = supportedFileType
        return file.extension == supportedType.defaultExtension || file.fileType == supportedType
    }

    fun hasStub(file: VirtualFile): Boolean = isSupported(file) && file.readSafely { hasMetadata(file) } == true

    fun readFileSafely(file: VirtualFile, content: ByteArray? = null): FileWithMetadata? = file.readSafely {
        readFile(file, content, project = null)
    }

    fun readFileSafely(file: VirtualFile, content: ByteArray? = null, project: Project?): FileWithMetadata? = file.readSafely {
        readFile(file, content, project)
    }

    final override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
        val virtualFile = content.file
        check(isSupported(virtualFile)) { "Unexpected compiled file type: ${virtualFile.fileType.name}" }

        val file = readFileSafely(virtualFile, content.content, content.project) ?: return null
        return when (file) {
            is FileWithMetadata.Incompatible -> org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl.forInvalid(file.errorText)
            is FileWithMetadata.Compatible -> CjoFileStubBuilder.buildFileStub(file.loadedPackage, file.moduleData)
        }
    }

    sealed class FileWithMetadata {
        class Incompatible(
            val errorText: String = CangJieCompiledFileErrors.NEWER_VERSION_DECOMPILE_ERROR,
        ) : FileWithMetadata()

        class Compatible(
            val loadedPackage: LoadedCjoPackage,
            val moduleData: CfirModuleData,
        ) : FileWithMetadata()
    }
}

private inline fun <T> VirtualFile.readSafely(action: () -> T): T? = try {
    if (isValid) {
        action()
    } else {
        null
    }
} catch (_: IOException) {
    null
}
