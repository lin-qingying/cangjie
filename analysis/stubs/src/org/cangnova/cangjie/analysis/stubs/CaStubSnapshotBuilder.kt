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
    /** 负责收集当前项目中可参与 stub 摘要的仓颉文件。 */
    private val fileCollector: CaStubFileCollector,

    /** 负责把单个 `CjFile` 转换为文件摘要。 */
    private val summaryBuilder: CaStubSummaryBuilder,

    /** 负责把文件摘要聚合成项目级快照。 */
    private val assembler: CaStubSnapshotAssembler = CaStubSnapshotAssembler(),
) {
    /**
     * 使用指定修改计数构建一份新的项目级 stub 快照。
     */
    fun build(modificationCount: Long): CaStubSnapshot {
        val summaries = fileCollector.collectFiles().map(summaryBuilder::build)
        return assembler.assemble(modificationCount, summaries)
    }
}

/**
 * 纯聚合器，便于直接做单元测试。
 */
internal class CaStubSnapshotAssembler {
    /**
     * 将单文件摘要集合聚合为项目级快照。
     *
     * 聚合过程同时建立文件、包级 classifier/callable 和 class member 三类索引。
     */
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
