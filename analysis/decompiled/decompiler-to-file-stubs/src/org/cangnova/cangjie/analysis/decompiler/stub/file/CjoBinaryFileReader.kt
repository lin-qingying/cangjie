package org.cangnova.cangjie.analysis.decompiler.stub.file

import PackageFormat.Package as CjoPackage
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.utils.exceptions.shouldIjPlatformExceptionBeRethrown
import java.nio.ByteBuffer

private val LOG = logger<CjoBinaryFileReader>()

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
     *
     * 四条返回 `null` 的输入路径各留一条 warn 日志：头部读失败此前被 `getOrNull` 静默吞掉，
     * 调用方只能看到一个与“文件不是 `.cjo`”无法区分的 `null`。控制流异常（取消、索引未就绪）
     * 不属于失败输入，必须原样重抛。
     */
    fun readPackageFqName(binaryFile: VirtualFile): FqName? {
        if (!isCjoBinaryFile(binaryFile)) {
            LOG.warn("CangJie `.cjo` header read skipped: not a CangJie binary file, path=${binaryFile.path}")
            return null
        }

        val pkg: CjoPackage? = try {
            CjoPackage.getRootAsPackage(ByteBuffer.wrap(binaryFile.contentsToByteArray()))
        } catch (t: Throwable) {
            if (shouldIjPlatformExceptionBeRethrown(t)) throw t

            LOG.warn(
                "CangJie `.cjo` header read failed: path=${binaryFile.path}, ${t.javaClass.name}: ${t.message}",
                t,
            )
            return null
        }

        if (pkg == null) {
            LOG.warn("CangJie `.cjo` header read produced no package root: path=${binaryFile.path}")
            return null
        }

        val fullPkgName = pkg.fullPkgName
        if (fullPkgName.isNullOrBlank()) {
            LOG.warn("CangJie `.cjo` header carries an empty package name: path=${binaryFile.path}")
            return null
        }

        return FqName(fullPkgName)
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
