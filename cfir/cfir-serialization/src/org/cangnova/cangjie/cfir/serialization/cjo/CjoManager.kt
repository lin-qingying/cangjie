package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.Package
import org.cangnova.cangjie.name.FqName
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * `.cjo` 文件管理器。
 *
 * 负责按包名搜索、加载和缓存 `.cjo` 文件，
 * 使用 FlatBuffers 零拷贝方式读取，避免不必要的内存分配。
 */
class CjoManager(
    /** `.cjo` 文件搜索路径解析器。 */
    private val searchPath: CjoSearchPath,
) {
    /** 已读取的包头缓存，key 为完整包名。 */
    private val headerCache = ConcurrentHashMap<String, CjoPackageHeader>()
    /** 已确认不存在的包名集合，用于避免重复文件系统扫描。 */
    private val missingPackages = ConcurrentHashMap.newKeySet<String>()
    /** 已读取的 FlatBuffers buffer 缓存，供包头与完整 Package 共享。 */
    private val bufferCache = ConcurrentHashMap<String, ByteBuffer>()

    /** 判断指定包名是否能在当前 `.cjo` 搜索路径中找到。 */
    fun hasPackage(fqName: FqName): Boolean {
        val pkgName = fqName.asString()
        return headerCache.containsKey(pkgName) || searchPath.findCjoFile(pkgName) != null
    }

    /**
     * 加载指定完整包名的轻量包头。
     *
     * 成功时同时缓存原始 buffer，后续 [loadPackage] 可直接复用同一份 FlatBuffers 数据。
     */
    fun loadPackageHeader(fullPkgName: String): CjoPackageHeader? {
        headerCache[fullPkgName]?.let { return it }
        if (fullPkgName in missingPackages) return null

        val file = searchPath.findCjoFile(fullPkgName)
        if (file == null) {
            missingPackages += fullPkgName
            return null
        }

        val buffer = readFileToByteBuffer(file)
        bufferCache[fullPkgName] = buffer
        val header = CjoPackageHeader.fromPackage(Package.getRootAsPackage(buffer))

        missingPackages.remove(fullPkgName)
        headerCache.putIfAbsent(fullPkgName, header)
        return headerCache[fullPkgName] ?: header
    }

    /**
     * 加载指定包的完整 FlatBuffers [Package]。
     *
     * 如果包头尚未加载，会先触发一次包头加载以建立 buffer 缓存。
     */
    fun loadPackage(fullPkgName: String): Package? {
        if (!bufferCache.containsKey(fullPkgName)) {
            loadPackageHeader(fullPkgName) ?: return null
        }
        val buffer = bufferCache[fullPkgName] ?: return null
        return Package.getRootAsPackage(buffer)
    }

    /** 枚举当前搜索路径可发现的包名集合。 */
    fun getAvailablePackageNames(): Set<FqName> {
        return searchPath.getAvailablePackageNames().mapTo(linkedSetOf(), ::FqName)
    }

    /** 把 `.cjo` 文件内容读入 position 归零的 [ByteBuffer]。 */
    private fun readFileToByteBuffer(file: File): ByteBuffer {
        val bytes = file.readBytes()
        val buffer = ByteBuffer.allocate(bytes.size)
        buffer.put(bytes)
        buffer.flip()
        return buffer
    }
}
