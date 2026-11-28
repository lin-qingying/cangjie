package org.cangnova.cangjie.analysis.stubs

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * stub 快照的文件来源。
 *
 * 聚合逻辑本身不认识平台：`CaStubSnapshotBuilder` 只按顺序问「给我这一轮要观察的
 * `CjFile`」，因此 IDE 侧可以接 project roots + decompiled binary index，无平台侧
 * 可以直接喂会话已知的文件集合。**不把 `Project` 提到快照契约上**是刻意的：
 * 提到契约上就等于让无平台实现也必须伪造一个 IntelliJ project。
 */
fun interface CaStubFileSource {
    /**
     * 返回本轮要参与 stub 摘要的全部仓颉文件。
     */
    fun collectFiles(): List<CjFile>
}

/**
 * 负责把单文件摘要组装成 project 级快照。
 *
 * 聚合与去重规则集中收敛在这里，service 层不允许再自行拼装 package / class 索引。
 */
class CaStubSnapshotBuilder(
    /** 提供本轮要观察的仓颉文件。 */
    private val fileSource: CaStubFileSource,

    /** 负责把单个 `CjFile` 转换为文件摘要。 */
    private val summaryBuilder: CaStubSummaryBuilder = CaStubSummaryBuilder(),

    /** 负责把文件摘要聚合成项目级快照。 */
    private val assembler: CaStubSnapshotAssembler = CaStubSnapshotAssembler(),
) {
    /**
     * 使用指定修改计数构建一份新的项目级 stub 快照。
     */
    fun build(modificationCount: Long): CaStubSnapshot {
        val summaries = fileSource.collectFiles().map(summaryBuilder::build)
        return assembler.assemble(modificationCount, summaries)
    }
}

/**
 * 纯聚合器，便于直接做单元测试。
 */
class CaStubSnapshotAssembler {
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
