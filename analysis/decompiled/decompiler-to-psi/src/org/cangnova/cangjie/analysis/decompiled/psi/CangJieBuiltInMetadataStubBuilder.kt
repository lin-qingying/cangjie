package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.decompiler.stub.file.CangJieMetadataStubBuilder
import org.cangnova.cangjie.analysis.decompiler.stub.file.CjoBinaryFileReader
import org.cangnova.cangjie.analysis.decompiler.stub.file.CjoModuleDataProvider
import org.cangnova.cangjie.analysis.decompiler.stub.file.DecompiledPackageDataFinder
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.psi.stubs.CangJieStubVersions

/**
 * 对位 Kotlin `KotlinBuiltInMetadataStubBuilder`。
 *
 * `.cjo` 没有 Kotlin builtins 那种额外的 builtins-definition 过滤协议，
 * 这里仍保持 Kotlin 的 owner 形状：
 * - stub version 固定在 builtins stub builder；
 * - readFile 只负责恢复 loaded package 与真实 moduleData owner；
 * - file stub 生成继续下放给 `CangJieMetadataStubBuilder` / `CjoFileStubBuilder`。
 */
object CangJieBuiltInMetadataStubBuilder : CangJieMetadataStubBuilder() {
    override fun getStubVersion(): Int = CangJieStubVersions.BUILTIN_STUB_VERSION

    override val supportedFileType: FileType
        get() = CangJieBuiltInFileType

    override fun hasMetadata(virtualFile: VirtualFile): Boolean {
        return CjoBinaryFileReader.readPackageFqName(virtualFile) != null
    }

    override fun readFile(
        virtualFile: VirtualFile,
        content: ByteArray?,
        project: Project?,
    ): FileWithMetadata? {
        project ?: return null
        val loadedPackage = project.getService(DecompiledPackageDataFinder::class.java).loadPackageData(virtualFile) ?: return null
        if (!loadedPackage.isVersionSupported) {
            return FileWithMetadata.Incompatible()
        }

        val moduleData = CjoModuleDataProvider.getInstance(project).getModuleData(virtualFile) ?: return null

        return FileWithMetadata.Compatible(loadedPackage, moduleData)
    }
}
