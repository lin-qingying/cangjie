@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.base.index

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexEntry
import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexEntryKind
import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexNameFilter
import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexPersistence
import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexQuery
import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexQueryService
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.standalone.base.declarations.CangJieStandaloneSourceFileCollector
import org.cangnova.cangjie.analysis.stubs.CaStubFileSummary
import org.cangnova.cangjie.analysis.stubs.CaStubSnapshotAssembler
import org.cangnova.cangjie.analysis.stubs.CaStubSummaryBuilder
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import java.util.TreeMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Standalone 名称发现：会话内 stub 快照，按修改版本缓存。
 *
 * 只缓存**文件摘要与倒排名称表**，不缓存 PSI、符号或分析会话：索引离开会话依然可用，
 * 而符号必须在调用点重新按包作用域恢复。
 *
 * 查询期只在内存倒排表上过滤，不回读文件系统：每次键入都遍历项目会退化成
 * 与项目规模成正比的代价，这正是索引要避免的事。
 *
 * `.cjo` 不在本类物化：二进制名称由 CFIR 层的只读名字视图提供，两边在
 * `CaSymbolProvider` 侧合并。
 */
class CaStandaloneSymbolIndexQueryService(private val project: Project) : CaSymbolIndexQueryService {
    /**
     * 索引缓存键的版本来源。
     *
     * 默认取平台的修改计数。**刻意不是构造参数**：平台按 `(Project)` 单参构造实例化
     * 该服务，带默认值的双参构造器会让容器去解析 `Function0` 依赖并直接失败。
     * 做成可替换属性是为了让「失效即重建」这条契约可被直接验证——缓存键取错的表现
     * 是静默的：不报错，只是候选永远不更新。
     */
    internal var versionProvider: () -> Long = {
        project.getService(CaModificationTracker::class.java).modificationCount
    }

    private val collector = CangJieStandaloneSourceFileCollector(project)
    private val summaryBuilder = CaStubSummaryBuilder()
    private val assembler = CaStubSnapshotAssembler()

    @Volatile
    private var cached: SourceIndex? = null

    internal val queryCount = AtomicLong()
    internal val queryNanos = AtomicLong()
    internal val fileCollectionCount = AtomicLong()
    internal val summaryBuildCount = AtomicLong()

    override fun createSymbolIndexQuery(analysisScope: GlobalSearchScope): CaSymbolIndexQuery =
        object : CaSymbolIndexQuery {
            override val indexPersistence: CaSymbolIndexPersistence = CaSymbolIndexPersistence.SESSION_ONLY

            override fun findDeclarations(filter: CaSymbolIndexNameFilter): Collection<CaSymbolIndexEntry> {
                ProgressManager.checkCanceled()
                val started = System.nanoTime()
                queryCount.incrementAndGet()
                try {
                    val index = sourceIndex()
                    val result = linkedSetOf<CaSymbolIndexEntry>()
                    for ((name, entries) in index.entriesByName) {
                        ProgressManager.checkCanceled()
                        if (filter.prefix.isNotEmpty() && !name.startsWith(filter.prefix)) continue
                        for (entry in entries) {
                            if (!filter.matches(entry.identity.name)) continue
                            if (entry.files.none { it.isValid && analysisScope.contains(it) }) continue
                            result += entry.identity
                        }
                    }
                    return result
                } finally {
                    queryNanos.addAndGet(System.nanoTime() - started)
                }
            }
        }

    @Synchronized
    private fun sourceIndex(): SourceIndex {
        val version = versionProvider()
        cached?.takeIf { it.version == version }?.let { return it }
        ProgressManager.checkCanceled()
        fileCollectionCount.incrementAndGet()
        // 二进制文件不在这里物化：`.cjo` 的名字走 CFIR 只读视图，不重复收集。
        val files = collector.collect(GlobalSearchScope.allScope(project)).filterNot { it.isCompiled }
        val previous = cached?.summaries.orEmpty()
        val summaries = linkedMapOf<VirtualFile, VersionedSummary>()
        for (file in files) {
            ProgressManager.checkCanceled()
            val virtualFile = file.virtualFile ?: continue
            val old = previous[virtualFile]
            summaries[virtualFile] = if (old != null && old.stamp == file.modificationStamp) old
            else {
                summaryBuildCount.incrementAndGet()
                VersionedSummary(file.modificationStamp, summaryBuilder.build(file))
            }
        }
        val snapshot = assembler.assemble(version, summaries.values.map { it.summary })
        val fileByKey = summaries.entries.associate { (file, value) -> value.summary.fileKey to file }
        val entries = linkedMapOf<EntryKey, MutableSet<VirtualFile>>()
        for (summary in snapshot.fileSummaries.values) {
            ProgressManager.checkCanceled()
            val file = fileByKey.getValue(summary.fileKey)
            val packageFqName = summary.packageFqName ?: continue
            fun add(name: Name, kind: CaSymbolIndexEntryKind, owner: ClassId? = null) {
                if (name.isSpecial) return
                entries.getOrPut(EntryKey(packageFqName, name, kind, owner), ::linkedSetOf).add(file)
            }
            summary.topLevelClassifierNames.forEach { add(it, CaSymbolIndexEntryKind.CLASS_LIKE) }
            summary.topLevelCallableNames.forEach { add(it, CaSymbolIndexEntryKind.CALLABLE) }
            // 摘要只记录成员名称，恢复符号时再筛出枚举构造器；
            // 普通实例成员不进索引，它们只在接收者已知时才有意义。
            summary.classMemberNames.forEach { (owner, names) ->
                names.forEach { add(it, CaSymbolIndexEntryKind.ENUM_CONSTRUCTOR, owner) }
            }
        }
        val entriesByName = TreeMap<String, MutableList<Entry>>()
        for ((key, sourceFiles) in entries) {
            val entry = CaSymbolIndexEntry(key.packageFqName, key.name, key.kind, key.ownerClassId)
            entriesByName.getOrPut(key.name.asString(), ::mutableListOf).add(Entry(entry, sourceFiles.toSet()))
        }
        return SourceIndex(version, summaries, entriesByName).also { cached = it }
    }

    private data class EntryKey(
        val packageFqName: FqName,
        val name: Name,
        val kind: CaSymbolIndexEntryKind,
        val ownerClassId: ClassId?,
    )
    private data class Entry(val identity: CaSymbolIndexEntry, val files: Set<VirtualFile>)
    private data class VersionedSummary(val stamp: Long, val summary: CaStubFileSummary)
    private data class SourceIndex(
        val version: Long,
        val summaries: Map<VirtualFile, VersionedSummary>,
        val entriesByName: TreeMap<String, MutableList<Entry>>,
    )
}
