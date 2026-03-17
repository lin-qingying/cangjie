package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.Package
import org.cangnova.cangjie.name.FqName
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * .cjo 文件管理器。
 *
 * 负责按包名搜索、加载和缓存 .cjo 文件。
 * 使用 FlatBuffers 零拷贝方式读取，避免不必要的内存分配。
 */
class CjoManager(
    private val searchPath: CjoSearchPath,
) {
    /** 包头缓存：fullPkgName → CjoPackageHeader */
    private val headerCache = ConcurrentHashMap<String, CjoPackageHeader?>()

    /** 原始字节缓存：fullPkgName → ByteBuffer（保持引用防止 GC） */
    private val bufferCache = ConcurrentHashMap<String, ByteBuffer>()

    /** 检查指定包是否存在对应的 .cjo 文件 */
    fun hasPackage(fqName: FqName): Boolean {
        val pkgName = fqName.asString()
        return headerCache.containsKey(pkgName) || searchPath.findCjoFile(pkgName) != null
    }

    /**
     * 加载指定包的包头信息。
     * @return 包头，未找到返回 null
     */
    fun loadPackageHeader(fullPkgName: String): CjoPackageHeader? {
        return headerCache.getOrPut(fullPkgName) {
            val file = searchPath.findCjoFile(fullPkgName) ?: return@getOrPut null
            val buffer = readFileToByteBuffer(file)
            bufferCache[fullPkgName] = buffer
            val pkg = Package.getRootAsPackage(buffer)
            CjoPackageHeader.fromPackage(pkg)
        }
    }

    /**
     * 获取指定包的 FlatBuffers Package 对象（零拷贝引用）。
     * @return Package 对象，未找到返回 null
     */
    fun loadPackage(fullPkgName: String): Package? {
        // 确保 buffer 已加载
        if (!bufferCache.containsKey(fullPkgName)) {
            loadPackageHeader(fullPkgName) ?: return null
        }
        val buffer = bufferCache[fullPkgName] ?: return null
        return Package.getRootAsPackage(buffer)
    }

    private fun readFileToByteBuffer(file: File): ByteBuffer {
        val bytes = file.readBytes()
        val buffer = ByteBuffer.allocate(bytes.size)
        buffer.put(bytes)
        buffer.flip()
        return buffer
    }
}
