package org.cangnova.cangjie.analysis.decompiler.stub.file

import PackageFormat.Package as CjoPackage
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.name.FqName
import java.nio.ByteBuffer

/**
 * `.cjo` 二进制轻量读取工具。
 *
 * 这里只承载反编译框架四层都会复用的“头部级能力”，
 * 避免 package 识别和版本判断在 file-stubs / psi / binary decompiler 文本链路中各写一套。
 */
object CjoBinaryFileReader {
    fun readPackageFqName(binaryFile: VirtualFile): FqName? {
        if (!isCjoBinaryFile(binaryFile)) return null
        val pkg = runCatching {
            CjoPackage.getRootAsPackage(ByteBuffer.wrap(binaryFile.contentsToByteArray()))
        }.getOrNull() ?: return null
        return pkg.fullPkgName?.takeIf(String::isNotBlank)?.let(::FqName)
    }

    fun isSupportedVersion(pkg: CjoPackage): Boolean {
        val version = pkg.cjoVersion ?: return true
        val targetMajor = version.majorNum.toUInt().toInt()
        val targetMinor = version.minorNum.toUInt().toInt()
        val targetPatch = version.patchNum.toUInt().toInt()
        return when {
            targetMajor != CjoConstants.VERSION_MAJOR -> targetMajor < CjoConstants.VERSION_MAJOR
            targetMinor != CjoConstants.VERSION_MINOR -> targetMinor < CjoConstants.VERSION_MINOR
            else -> targetPatch <= CjoConstants.VERSION_PATCH
        }
    }

    fun isCjoBinaryFile(binaryFile: VirtualFile): Boolean {
        return binaryFile.fileType == CangJieBuiltInFileType ||
            binaryFile.extension.equals(CangJieBuiltInFileType.defaultExtension, ignoreCase = true)
    }
}
