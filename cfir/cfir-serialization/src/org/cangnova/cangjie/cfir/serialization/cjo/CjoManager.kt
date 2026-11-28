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
    private val formatVersionPolicy: CjoFormatVersionPolicy? = null,
    explicitCommonPartPaths: List<Path> = emptyList(),
    val canInline: Boolean = false,
    /** 同一 metadata 依赖图共用事务；包快照与 record 缓存仍各归本 manager。 */
    internal val declarationMaterialization: org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeclarationMaterialization =
        org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeclarationMaterialization(),
) : CjoLoadedPackageProvider, CjoLoadedPackageSnapshotProvider {
    private class Snapshot(
        bytes: ByteArray,
        override val sourcePath: Path,
    ) : CjoLoadedPackage {
        /** 来源字节独立于供 FlatBuffer 读取的同代声明池。 */
        private val sourceBytes = bytes.copyOf()
        private val buffer: ByteBuffer = ByteBuffer.wrap(sourceBytes.copyOf())
        fun hasContent(bytes: ByteArray): Boolean = sourceBytes.contentEquals(bytes)
        override val header: CjoPackageHeader = CjoPackageHeader.fromPackage(Package.getRootAsPackage(buffer.duplicate()))
        override val pkg: Package get() = Package.getRootAsPackage(buffer.duplicate())
    }

    private val snapshots = ConcurrentHashMap<String, CjoLoadedPackage>()
    private val snapshotsByPath = ConcurrentHashMap<Path, Snapshot>()
    /** 显式来源可先在名称枚举中读取。包名和版本门通过后才发布为正式来源。 */
    private val explicitSnapshotCandidates = HashMap<Path, Snapshot>()

    internal fun loadedSnapshotIdentity(header: CjoPackageHeader, path: Path?): CjoLoadedPackage? =
        path?.let(::loadedPackageSnapshot)?.takeIf { it.header === header }

    /** 实际文件读取与按包名读取共用已发布的来源身份，并检查请求字节。 */
    @Synchronized
    fun loadPackageSnapshot(fullPkgName: String, sourcePath: Path, content: ByteArray): CjoLoadedPackage {
        val path = sourcePath.toAbsolutePath().normalize()
        val old = snapshotsByPath[path] ?: explicitSnapshotCandidates[path]
        val snapshot = if (old == null) Snapshot(content, path) else old.also {
            check(it.hasContent(content)) { "CJO content changed inside a metadata generation: $path" }
        }
        val diagnostics = snapshotDiagnostics(fullPkgName, snapshot)
        check(diagnostics.isEmpty()) { "Rejected actual CJO $path: $diagnostics" }
        return publishSnapshot(snapshot)
    }

    private fun publishSnapshot(snapshot: Snapshot): CjoLoadedPackage {
        check(Thread.holdsLock(this))
        val previous = snapshotsByPath.putIfAbsent(snapshot.sourcePath, snapshot)
        check(previous == null || previous === snapshot) { "A different CJO snapshot already owns this source" }
        return snapshot
    }
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
    private fun loadExplicitSnapshot(path: Path): Snapshot {
        check(Thread.holdsLock(this))
        val normalizedPath = path.toAbsolutePath().normalize()
        return snapshotsByPath[normalizedPath] ?: explicitSnapshotCandidates.getOrPut(normalizedPath) {
            Snapshot(normalizedPath.toFile().readBytes(), normalizedPath)
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
        val path = file.toPath().toAbsolutePath().normalize()
        val bytes = file.readBytes()
        val old = snapshotsByPath[path] ?: explicitSnapshotCandidates[path]
        val snapshot = if (old == null) Snapshot(bytes, path) else old.also {
            check(it.hasContent(bytes)) { "CJO content changed inside a metadata generation: $path" }
        }
        return validateSnapshot(fullPkgName, snapshot)?.also { snapshots[fullPkgName] = it }
    }

    private fun snapshotDiagnostics(fullPkgName: String, snapshot: Snapshot): List<CfirCjmpLoadDiagnostic> = buildList {
        val versionProblem = when {
            formatVersionPolicy == null -> CjmpCommonPartLoadGate.checkFormatVersion(fullPkgName, snapshot.header.cjoVersion)
            isExplicitCommonPartPath(snapshot.sourcePath) -> formatVersionPolicy.checkCommonPart(fullPkgName, snapshot.header.cjoVersion)
            else -> null
        }
        versionProblem?.let(::add)
        CjmpCommonPartLoadGate.checkPackageName(fullPkgName, snapshot.header.fullPkgName)?.let(::add)
    }

    private fun validateSnapshot(fullPkgName: String, snapshot: Snapshot): CjoLoadedPackage? {
        val diagnostics = snapshotDiagnostics(fullPkgName, snapshot)
        if (diagnostics.isNotEmpty()) {
            loadDiagnostics.merge(fullPkgName, diagnostics) { previous, current -> previous + current }
            return null
        }
        return publishSnapshot(snapshot)
    }

    fun loadPackageHeader(fullPkgName: String): CjoPackageHeader? = loadPackageSnapshot(fullPkgName)?.header
    fun loadPackage(fullPkgName: String): Package? = loadPackageSnapshot(fullPkgName)?.pkg

    @Synchronized
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
