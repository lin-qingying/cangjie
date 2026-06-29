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
    /**
     * 从 `.cjo` 二进制文件头中读取包全限定名。
     *
     * 该方法只做轻量级 header 解析，不构造完整的反编译 stub 树；当文件类型不是仓颉二进制、
     * 内容无法按 package flatbuffer 读取，或包名为空时返回 `null`，由调用方继续走其它索引路径。
     */
    fun readPackageFqName(binaryFile: VirtualFile): FqName? {
        if (!isCjoBinaryFile(binaryFile)) return null
        val pkg = runCatching {
            CjoPackage.getRootAsPackage(ByteBuffer.wrap(binaryFile.contentsToByteArray()))
        }.getOrNull() ?: return null
        return pkg.fullPkgName?.takeIf(String::isNotBlank)?.let(::FqName)
    }

    /**
     * 判断 `.cjo` 包的序列化版本是否可由当前反编译器读取。
     *
     * 没有显式版本信息的旧产物按兼容处理；存在版本号时仅接受不高于当前
     * [CjoConstants] 的 major/minor/patch 组合，避免新格式被旧读取器误解释。
     */
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

    /**
     * 判断虚拟文件是否应被仓颉 `.cjo` 反编译链路处理。
     *
     * 这里同时检查 IntelliJ file type 和默认扩展名，用来覆盖文件类型尚未完成注册、
     * 但扩展名已经明确指向仓颉内建二进制的场景。
     */
    fun isCjoBinaryFile(binaryFile: VirtualFile): Boolean {
        return binaryFile.fileType == CangJieBuiltInFileType ||
            binaryFile.extension.equals(CangJieBuiltInFileType.defaultExtension, ignoreCase = true)
    }
}
