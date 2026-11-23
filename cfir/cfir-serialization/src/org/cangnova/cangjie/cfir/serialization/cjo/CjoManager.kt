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
    /** 同一 metadata 依赖图共用事务；包快照与 record 缓存仍各归本 manager。 */
    internal val declarationMaterialization: org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeclarationMaterialization =
        org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeclarationMaterialization(),
) : CjoLoadedPackageProvider {
    private class Snapshot(content: ByteArray, override val sourcePath: Path) : CjoLoadedPackage {
        /** 来源字节与可编辑 wire 声明池分开；wire mutator 不改变代来源核验事实。 */
        private val sourceContent = content.copyOf()
        private val buffer = ByteBuffer.wrap(sourceContent.copyOf())
        override val header: CjoPackageHeader = CjoPackageHeader.fromPackage(Package.getRootAsPackage(buffer.duplicate()))
        override val pkg: Package get() = Package.getRootAsPackage(buffer.duplicate())
        fun hasContent(content: ByteArray): Boolean = sourceContent.contentEquals(content)
    }

    /**
     * 按模块数据共享的反序列化符号查找入口；与本 manager 的包快照同生共死。
     */
    val deserializedSymbolLookups: CjoDeserializedSymbolLookupHolder = CjoDeserializedSymbolLookupHolder(this)

    private val snapshots = ConcurrentHashMap<String, CjoLoadedPackage>()
    private val missingPackages = ConcurrentHashMap.newKeySet<String>()
    private val exactSnapshots = ConcurrentHashMap<Path, Snapshot>()

    internal fun loadedSnapshotIdentity(header: CjoPackageHeader, path: Path?): CjoLoadedPackage? =
        path?.toAbsolutePath()?.normalize()?.let { exactSnapshots[it] }?.takeIf { it.header === header }

    /** 两个公开入口共用同一 manager 监视器和实际来源记录。 */
    @Synchronized
    fun loadPackageSnapshot(fullPkgName: String, sourcePath: Path, content: ByteArray): CjoLoadedPackage {
        val snapshot = sourceSnapshot(sourcePath, content)
        val diagnostics = validateSnapshot(fullPkgName, snapshot)
        check(diagnostics.isEmpty()) { "Rejected actual CJO " + snapshot.sourcePath + ": " + diagnostics }
        publishSourceSnapshot(snapshot)
        return snapshot
    }

    /** 只能在同一代的装载事务内选择来源对象；不能产生第二个同 source 对象。 */
    private fun sourceSnapshot(sourcePath: Path, content: ByteArray): Snapshot {
        check(Thread.holdsLock(this)) { "CJO source selection requires the manager load transaction" }
        val path = sourcePath.toAbsolutePath().normalize()
        val existing = exactSnapshots[path]
        if (existing != null) {
            check(existing.hasContent(content)) { "CJO content changed inside a metadata generation: $path" }
            return existing
        }
        return Snapshot(content, path)
    }

    private fun validateSnapshot(fullPkgName: String, snapshot: Snapshot): List<CfirCjmpLoadDiagnostic> =
        buildList {
            CjmpCommonPartLoadGate.checkFormatVersion(fullPkgName, snapshot.header.cjoVersion)?.let(::add)
            CjmpCommonPartLoadGate.checkPackageName(fullPkgName, snapshot.header.fullPkgName)?.let(::add)
        }

    /** package/source cache 只能发布同一个已接受对象；不同对象是装载合同错误。 */
    private fun publishSourceSnapshot(snapshot: Snapshot) {
        check(Thread.holdsLock(this))
        val existing = exactSnapshots[snapshot.sourcePath]
        check(existing == null || existing === snapshot) { "A different CJO snapshot already owns this source" }
        exactSnapshots[snapshot.sourcePath] = snapshot
    }

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
        val path = file.toPath().toAbsolutePath().normalize()
        // named 首次读取也核对实际来源字节；name cache 命中仍保持本代稳定，不重读。
        val snapshot = sourceSnapshot(path, file.readBytes())
        // 加载门①版本 / ②包名：违反即拒绝装载（官方 PreloadCommonPartOfPackage 的 return false 对位），
        // 但诊断仍记录，供装配层外显——静默地把包当"不存在"会让调用方难以定位问题。
        val diagnostics = validateSnapshot(fullPkgName, snapshot)
        if (diagnostics.isNotEmpty()) {
            loadDiagnostics[fullPkgName] = diagnostics
            missingPackages += fullPkgName
            return null
        }
        publishSourceSnapshot(snapshot)
        snapshots[fullPkgName] = snapshot
        return snapshot
    }

    fun loadPackageHeader(fullPkgName: String): CjoPackageHeader? = loadPackageSnapshot(fullPkgName)?.header
    fun loadPackage(fullPkgName: String): Package? = loadPackageSnapshot(fullPkgName)?.pkg

    fun getAvailablePackageNames(): Set<FqName> =
        searchPath.getAvailablePackageNames().mapTo(linkedSetOf(), ::FqName)
}
