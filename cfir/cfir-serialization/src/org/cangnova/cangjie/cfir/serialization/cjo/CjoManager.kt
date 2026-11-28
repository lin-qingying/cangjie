package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.Package
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnostic
import org.cangnova.cangjie.name.FqName
import java.nio.ByteBuffer
import java.nio.file.Paths
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * `.cjo` 文件管理器。一次装载原子发布包头、buffer 与实际来源路径。
 * 已存在/缺失结果在本代内稳定；CJO、sidecar 或搜索根变化后必须连同 provider/session 整体重建。
 */
class CjoManager(
    private val searchPath: CjoSearchPath,
    private val formatVersionPolicy: CjoFormatVersionPolicy,
    explicitCommonPartPaths: List<Path> = emptyList(),
    val canInline: Boolean = false,
) : CjoLoadedPackageProvider, CjoLoadedPackageSnapshotProvider {
    private class Snapshot(
        buffer: ByteBuffer,
        override val sourcePath: Path,
    ) : CjoLoadedPackage {
        private val buffer: ByteBuffer = buffer.asReadOnlyBuffer()
        override val header: CjoPackageHeader = CjoPackageHeader.fromPackage(Package.getRootAsPackage(buffer.duplicate()))
        override val pkg: Package get() = Package.getRootAsPackage(buffer.duplicate())
    }

    private val snapshots = ConcurrentHashMap<String, CjoLoadedPackage>()
    private val snapshotsByPath = ConcurrentHashMap<Path, CjoLoadedPackage>()
    private val snapshotsByPackage = ConcurrentHashMap<String, List<CjoLoadedPackage>>()
    private val explicitCommonPartPaths = explicitCommonPartPaths.map { it.toAbsolutePath().normalize() }
    private val missingPackages = ConcurrentHashMap.newKeySet<String>()

    /**
     * 加载门诊断（版本 / 包名）按包名记录，供反序列化符号 provider 汇入诊断事实流。
     *
     * 通道（计划 G17）：加载期没有 `DiagnosticReporter`，诊断随加载结果上浮，
     * 由 [org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider]
     * 转入 `CfirCjmpLoadDiagnosticsCollector`。
     */
    private val loadDiagnostics = ConcurrentHashMap<String, List<CfirCjmpLoadDiagnostic>>()

    /** 读取指定包的加载门诊断（无诊断时为空列表）。 */
    fun loadDiagnostics(fullPkgName: String): List<CfirCjmpLoadDiagnostic> =
        loadDiagnostics[fullPkgName].orEmpty()

    /** 判定快照来源是否是配置指定的 common-part 输入路径。 */
    fun isExplicitCommonPartPath(path: Path): Boolean =
        path.toAbsolutePath().normalize() in explicitCommonPartPaths

    fun hasPackage(fqName: FqName): Boolean =
        snapshotsByPackage[fqName.asString()]?.isNotEmpty() == true ||
                snapshots.containsKey(fqName.asString()) || searchPath.findCjoFile(fqName.asString()) != null

    /** 返回本代中已经读取的精确来源快照，不重新打开文件或按包名重新选择来源。 */
    override fun loadedPackageSnapshot(sourcePath: Path): CjoLoadedPackage? =
        snapshotsByPath[sourcePath.toAbsolutePath().normalize()]

    /** 同一管理器中来源与内容不可分离；串行装载也避免竞争线程发布不同文件版本。 */
    @Synchronized
    override fun loadPackageSnapshot(fullPkgName: String): CjoLoadedPackage? =
        loadPackageSnapshots(fullPkgName).firstOrNull()

    /** 指定 common-part 输入包含同一包时，按输入路径顺序返回各自独立的包快照。 */
    @Synchronized
    fun loadPackageSnapshots(fullPkgName: String): List<CjoLoadedPackage> {
        snapshotsByPackage[fullPkgName]?.let { return it }
        val unavailableExplicitPath = explicitCommonPartPaths.firstOrNull {
            !it.toFile().isFile || !it.toFile().canRead()
        }
        if (unavailableExplicitPath != null) {
            loadDiagnostics.merge(
                fullPkgName,
                listOf(
                    CfirCjmpLoadDiagnostic.InputFileInvalid(
                        requestedPackageName = fullPkgName,
                        path = unavailableExplicitPath.toString(),
                    ),
                ),
            ) { previous, current -> previous + current }
            snapshotsByPackage[fullPkgName] = emptyList()
            return emptyList()
        }
        var hasExplicitCandidate = false
        val explicit = buildList {
            for (path in explicitCommonPartPaths) {
                if (!path.toFile().isFile) continue
                val snapshot = loadExplicitSnapshot(path)
                val expectedRelativePath = Paths.get(CjoConstants.packageNameToPath(fullPkgName))
                if (snapshot.header.fullPkgName != fullPkgName && !path.endsWith(expectedRelativePath)) continue
                hasExplicitCandidate = true
                validateSnapshot(fullPkgName, snapshot)?.let(::add)
            }
        }
        if (hasExplicitCandidate) {
            val frozen = explicit.distinctBy { it.sourcePath }
            snapshotsByPackage[fullPkgName] = frozen
            return frozen
        }
        val single = loadSearchPathSnapshot(fullPkgName)
        val result = single?.let(::listOf).orEmpty()
        snapshotsByPackage[fullPkgName] = result
        return result
    }

    /** 单个显式 parent 路径的内容与来源一起缓存，不能按 package 名覆盖同包的其他 parent。 */
    private fun loadExplicitSnapshot(path: Path): CjoLoadedPackage {
        val normalizedPath = path.toAbsolutePath().normalize()
        return snapshotsByPath.computeIfAbsent(normalizedPath) {
            Snapshot(
                ByteBuffer.wrap(normalizedPath.toFile().readBytes()),
                normalizedPath,
            )
        }
    }

    private fun loadSearchPathSnapshot(fullPkgName: String): CjoLoadedPackage? {
        snapshots[fullPkgName]?.let { return it }
        if (fullPkgName in missingPackages) return null
        val file = searchPath.findCjoFile(fullPkgName)
        if (file == null) {
            missingPackages += fullPkgName
            return null
        }
        val snapshot = loadExplicitSnapshot(file.toPath())
        return validateSnapshot(fullPkgName, snapshot)?.also { snapshots[fullPkgName] = it }
    }

    private fun validateSnapshot(fullPkgName: String, snapshot: CjoLoadedPackage): CjoLoadedPackage? {
        // 格式版本只对显式 common-part 输入按当前编译语言版本策略判定；包名错配仍作为无效 package 元数据拒绝。
        // 诊断随加载结果上浮，供装配层外显，避免静默地把包当"不存在"。
        val diagnostics = buildList {
            if (isExplicitCommonPartPath(snapshot.sourcePath)) {
                formatVersionPolicy.checkCommonPart(fullPkgName, snapshot.header.cjoVersion)?.let(::add)
            }
            CjmpCommonPartLoadGate.checkPackageName(fullPkgName, snapshot.header.fullPkgName)?.let(::add)
        }
        if (diagnostics.isNotEmpty()) {
            loadDiagnostics.merge(fullPkgName, diagnostics) { previous, current -> previous + current }
            return null
        }
        return snapshot
    }

    fun loadPackageHeader(fullPkgName: String): CjoPackageHeader? = loadPackageSnapshot(fullPkgName)?.header
    fun loadPackage(fullPkgName: String): Package? = loadPackageSnapshot(fullPkgName)?.pkg

    fun getAvailablePackageNames(): Set<FqName> =
        buildSet {
            searchPath.getAvailablePackageNames().mapTo(this, ::FqName)
            for (path in explicitCommonPartPaths) {
                if (!path.toFile().isFile) continue
                val snapshot = loadExplicitSnapshot(path)
                snapshot.header.fullPkgName.takeIf(String::isNotBlank)?.let { add(FqName(it)) }
            }
        }

    /** 返回显式 parent 快照与普通搜索路径快照的包头并集。 */
    fun loadPackageHeaders(fullPkgName: String): List<CjoPackageHeader> =
        loadPackageSnapshots(fullPkgName).map { it.header }
}
