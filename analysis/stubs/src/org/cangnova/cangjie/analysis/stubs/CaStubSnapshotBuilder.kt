package org.cangnova.cangjie.analysis.stubs

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 负责把单文件摘要组装成 project 级快照。
 *
 * 聚合与去重规则集中收敛在这里，service 层不允许再自行拼装 package / class 索引。
 */
internal class CaStubSnapshotBuilder(
    private val fileCollector: CaStubFileCollector,
    private val summaryBuilder: CaStubSummaryBuilder,
    private val assembler: CaStubSnapshotAssembler = CaStubSnapshotAssembler(),
) {
    fun build(modificationCount: Long): CaStubSnapshot {
        val summaries = fileCollector.collectFiles().map(summaryBuilder::build)
        return assembler.assemble(modificationCount, summaries)
    }
}

/**
 * 纯聚合器，便于直接做单元测试。
 */
internal class CaStubSnapshotAssembler {
    fun assemble(
        modificationCount: Long,
        summaries: Iterable<CaStubFileSummary>,
    ): CaStubSnapshot {
        val fileSummaries = linkedMapOf<String, CaStubFileSummary>()
        val packageClassifierNames = linkedMapOf<FqName, MutableSet<Name>>()
        val packageCallableNames = linkedMapOf<FqName, MutableSet<Name>>()
        val classMemberNames = linkedMapOf<ClassId, MutableSet<Name>>()

        summaries.forEach { summary ->
            fileSummaries[summary.fileKey] = summary

            val packageFqName = summary.packageFqName ?: return@forEach
            packageClassifierNames.getOrPut(packageFqName, ::linkedSetOf).addAll(summary.topLevelClassifierNames)
            packageCallableNames.getOrPut(packageFqName, ::linkedSetOf).addAll(summary.topLevelCallableNames)
            summary.classMemberNames.forEach { (classId, names) ->
                classMemberNames.getOrPut(classId, ::linkedSetOf).addAll(names)
            }
        }

        return CaStubSnapshot(
            modificationCount = modificationCount,
            fileSummaries = fileSummaries,
            packageClassifierNames = packageClassifierNames.mapValues { (_, names) -> names.toSet() },
            packageCallableNames = packageCallableNames.mapValues { (_, names) -> names.toSet() },
            classMemberNames = classMemberNames.mapValues { (_, names) -> names.toSet() },
        )
    }
}
