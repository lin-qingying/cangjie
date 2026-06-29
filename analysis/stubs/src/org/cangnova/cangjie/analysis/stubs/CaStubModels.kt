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
    /** 文件级稳定键，通常来自 virtual file URL，缺失时回退到文件名。 */
    val fileKey: String,

    /** 当前文件 stub 的 kind，用于区分普通文件、facade 与 multifile facade。 */
    val stubKind: CangJieFileStubKind?,

    /** 当前文件所属包名；无法从 stub 解析时为 `null`。 */
    val packageFqName: FqName?,

    /** 文件中可供包级索引消费的顶层 classifier 名称集合。 */
    val topLevelClassifierNames: Set<Name>,

    /** 文件中可供包级索引消费的顶层 callable 名称集合。 */
    val topLevelCallableNames: Set<Name>,

    /** 文件中 class-like 声明到其成员名称集合的映射。 */
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
    /** 创建快照时使用的项目结构修改计数。 */
    val modificationCount: Long,

    /** 文件键到单文件 stub 摘要的映射。 */
    val fileSummaries: Map<String, CaStubFileSummary>,

    /** 包名到顶层 classifier 名称集合的索引。 */
    val packageClassifierNames: Map<FqName, Set<Name>>,

    /** 包名到顶层 callable 名称集合的索引。 */
    val packageCallableNames: Map<FqName, Set<Name>>,

    /** ClassId 到成员名称集合的索引。 */
    val classMemberNames: Map<ClassId, Set<Name>>,
)
