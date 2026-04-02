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
    private val searchPath: CjoSearchPath,
) {
    private val headerCache = ConcurrentHashMap<String, CjoPackageHeader>()
    private val missingPackages = ConcurrentHashMap.newKeySet<String>()
    private val bufferCache = ConcurrentHashMap<String, ByteBuffer>()

    fun hasPackage(fqName: FqName): Boolean {
        val pkgName = fqName.asString()
        return headerCache.containsKey(pkgName) || searchPath.findCjoFile(pkgName) != null
    }

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

    fun loadPackage(fullPkgName: String): Package? {
        if (!bufferCache.containsKey(fullPkgName)) {
            loadPackageHeader(fullPkgName) ?: return null
        }
        val buffer = bufferCache[fullPkgName] ?: return null
        return Package.getRootAsPackage(buffer)
    }

    fun getAvailablePackageNames(): Set<FqName> {
        return searchPath.getAvailablePackageNames().mapTo(linkedSetOf(), ::FqName)
    }

    private fun readFileToByteBuffer(file: File): ByteBuffer {
        val bytes = file.readBytes()
        val buffer = ByteBuffer.allocate(bytes.size)
        buffer.put(bytes)
        buffer.flip()
        return buffer
    }
}
