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
    /**
     * 返回 builtins `.cjo` stub 树参与 IntelliJ stub 缓存失效的版本号。
     */
    override fun getStubVersion(): Int = CangJieStubVersions.BUILTIN_STUB_VERSION

    /**
     * 声明该 builder 只处理仓颉内建二进制文件类型。
     */
    override val supportedFileType: FileType
        get() = CangJieBuiltInFileType

    /**
     * 判断虚拟文件是否包含可读取的 `.cjo` package metadata。
     */
    override fun hasMetadata(virtualFile: VirtualFile): Boolean {
        return CjoBinaryFileReader.readPackageFqName(virtualFile) != null
    }

    /**
     * 读取 `.cjo` 文件并封装成 metadata stub 构建所需的数据对象。
     *
     * 该实现要求 project 上下文存在，因为反编译出的声明必须绑定真实 [CjoModuleDataProvider]
     * 返回的 module data；版本不兼容时返回 [FileWithMetadata.Incompatible]。
     */
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
