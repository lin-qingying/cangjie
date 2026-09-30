package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.Package
import org.cangnova.cangjie.cfir.serialization.provider.CjoDeserializedSymbolLookupHolder
import org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnostic
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

    /**
     * 按模块数据共享的反序列化符号查找入口；与本 manager 的包快照同生共死。
     */
    val deserializedSymbolLookups: CjoDeserializedSymbolLookupHolder = CjoDeserializedSymbolLookupHolder(this)

    private val snapshots = ConcurrentHashMap<String, CjoLoadedPackage>()
    private val missingPackages = ConcurrentHashMap.newKeySet<String>()

    /**
     * 加载门诊断（版本 / 包名）按包名记录，供 session 层的收集器取走。
     *
     * 通道（计划 G17）：加载期没有 `DiagnosticReporter`，诊断随加载结果上浮，
     * 由 [org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider]
     * 转入 `CfirCjmpLoadDiagnosticsComponent`。
     */
    private val loadDiagnostics = ConcurrentHashMap<String, List<CfirCjmpLoadDiagnostic>>()

    /** 读取指定包的加载门诊断（无诊断时为空列表）。 */
    fun loadDiagnostics(fullPkgName: String): List<CfirCjmpLoadDiagnostic> =
        loadDiagnostics[fullPkgName].orEmpty()

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
        val snapshot = Snapshot(
            ByteBuffer.wrap(file.readBytes()),
            file.toPath().toAbsolutePath().normalize(),
        )
        // 加载门①版本 / ②包名：违反即拒绝装载（官方 PreloadCommonPartOfPackage 的 return false 对位），
        // 但诊断仍记录，供装配层外显——静默地把包当"不存在"会让调用方难以定位问题。
        val diagnostics = buildList {
            CjmpCommonPartLoadGate.checkFormatVersion(fullPkgName, snapshot.header.cjoVersion)?.let(::add)
            CjmpCommonPartLoadGate.checkPackageName(fullPkgName, snapshot.header.fullPkgName)?.let(::add)
        }
        if (diagnostics.isNotEmpty()) {
            loadDiagnostics[fullPkgName] = diagnostics
            missingPackages += fullPkgName
            return null
        }
        return snapshot.also {
            snapshots[fullPkgName] = it
        }
    }

    fun loadPackageHeader(fullPkgName: String): CjoPackageHeader? = loadPackageSnapshot(fullPkgName)?.header
    fun loadPackage(fullPkgName: String): Package? = loadPackageSnapshot(fullPkgName)?.pkg

    fun getAvailablePackageNames(): Set<FqName> =
        searchPath.getAvailablePackageNames().mapTo(linkedSetOf(), ::FqName)
}
