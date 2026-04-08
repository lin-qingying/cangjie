package org.cangnova.cangjie.analysis.stubs

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind

/**
 * 单文件 stub 摘要。
 *
 * 这里刻意只保留 analysis 层真正消费的稳定信息：
 * - 文件 kind
 * - 顶层 classifier / callable 名称
 * - class-like 成员名称
 *
 * 避免把 IntelliJ stub 树直接泄漏到 analysis API 消费层。
 */
internal data class CaStubFileSummary(
    val fileKey: String,
    val stubKind: CangJieFileStubKind?,
    val packageFqName: FqName?,
    val topLevelClassifierNames: Set<Name>,
    val topLevelCallableNames: Set<Name>,
    val classMemberNames: Map<ClassId, Set<Name>>,
)

/**
 * analysis:stubs 模块对外暴露的统一快照。
 *
 * 所有 project-level 查询都必须从这份快照读取，保证：
 * 1. source / compiled 使用同一份聚合语义；
 * 2. 刷新边界只由 modification count 决定；
 * 3. service 层不再重复实现去重和聚合规则。
 */
internal data class CaStubSnapshot(
    val modificationCount: Long,
    val fileSummaries: Map<String, CaStubFileSummary>,
    val packageClassifierNames: Map<FqName, Set<Name>>,
    val packageCallableNames: Map<FqName, Set<Name>>,
    val classMemberNames: Map<ClassId, Set<Name>>,
)
