package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.Package
import org.cangnova.cangjie.name.FqName
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * `.cjo` 文件管理器。一次装载原子发布包头、buffer 与实际来源路径。
 * 已存在/缺失结果在本代内稳定；CJO、sidecar 或搜索根变化后必须连同 provider/session 整体重建。
 */
class CjoManager(
    private val searchPath: CjoSearchPath,
) : CjoLoadedPackageProvider {
    private class Snapshot(
        private val buffer: ByteBuffer,
        override val sourcePath: Path,
    ) : CjoLoadedPackage {
        override val header: CjoPackageHeader = CjoPackageHeader.fromPackage(Package.getRootAsPackage(buffer.duplicate()))
        override val pkg: Package get() = Package.getRootAsPackage(buffer.duplicate())
    }

    private val snapshots = ConcurrentHashMap<String, CjoLoadedPackage>()
    private val missingPackages = ConcurrentHashMap.newKeySet<String>()

    fun hasPackage(fqName: FqName): Boolean =
        snapshots.containsKey(fqName.asString()) || searchPath.findCjoFile(fqName.asString()) != null

    /** 同一管理器中来源与内容不可分离；串行装载也避免竞争线程发布不同文件版本。 */
    @Synchronized
    override fun loadPackageSnapshot(fullPkgName: String): CjoLoadedPackage? {
        snapshots[fullPkgName]?.let { return it }
        if (fullPkgName in missingPackages) return null
        val file = searchPath.findCjoFile(fullPkgName)
        if (file == null) {
            missingPackages += fullPkgName
            return null
        }
        return Snapshot(ByteBuffer.wrap(file.readBytes()), file.toPath().toAbsolutePath().normalize()).also {
            snapshots[fullPkgName] = it
        }
    }

    fun loadPackageHeader(fullPkgName: String): CjoPackageHeader? = loadPackageSnapshot(fullPkgName)?.header
    fun loadPackage(fullPkgName: String): Package? = loadPackageSnapshot(fullPkgName)?.pkg

    fun getAvailablePackageNames(): Set<FqName> =
        searchPath.getAvailablePackageNames().mapTo(linkedSetOf(), ::FqName)
}
