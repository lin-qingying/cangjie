package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.annotationMetadataRegistryOrNull
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.source.CjSourceElement

/**
 * CFIR annotation slot 的稳定身份。
 *
 * annotation macro construction 必须按 raw builder 已写入 owner.annotations 的
 * 槽位替换，不能再退回 source offset、PSI annotation 或 append 第二条 annotation。
 *
 * @property owner 持有 annotation 槽位的 CFIR 声明。
 * @property annotationIndex annotation 在 [owner] 的 annotation 列表中的稳定下标。
 * @property originalAnnotation raw build 阶段写入槽位的原始 annotation 对象身份。
 */
data class CfirAnnotationReplaceCarrier(
    /** 持有 annotation 槽位的 CFIR 声明。 */
    val owner: CfirDeclaration,
    /** annotation 在 [owner] 的 annotation 列表中的稳定下标。 */
    val annotationIndex: Int,
    /** raw build 阶段写入槽位的原始 annotation 对象身份。 */
    val originalAnnotation: CfirAnnotationCall,
)

/**
 * raw builder 在 annotation append 后记录的完整 slot snapshot。
 *
 * [rawSyntax] 保留完整 `@Anno[...]` / `@!Anno[...]` 文本；[argumentText]
 * 仅保留 `[]` 参数列表文本，用于 cache 和 public analysis argument 投影。
 * [isCompileTimeVisible] 是 parser 按官方 `@` / `@!` 语法确定的语义位，checker
 * 不得再通过解析结果或源码文本反推；[annotationSource] 精确覆盖完整 annotation。
 *
 * @property owner 持有 annotation 槽位的 CFIR 声明。
 * @property annotationIndex annotation 在 [owner] 的 annotation 列表中的稳定下标。
 * @property originalAnnotation raw build 阶段写入槽位的原始 annotation 对象身份。
 * @property rawSyntax 源码中的完整 annotation 文本，包含 `@` 前缀与参数列表。
 * @property forcedCustom 是否来自 `@!` 强制 custom annotation 语法，仅表示语法 provenance。
 * @property isCompileTimeVisible annotation 是否在编译时可见，对齐官方 AST 语义位。
 * @property annotationSource 源码中完整 annotation 的精确 source 范围。
 * @property qualifiedName annotation 调用的限定名；解析失败或语法缺失时为 null。
 * @property argumentText annotation 参数列表文本；无显式参数时为 null。
 * @property tokens raw builder 采集到的 annotation token 流。
 * @property callSite annotation 所附着的语义位置，用于决定 splice 路由。
 */
data class CfirAnnotationSlotSnapshot(
    /** 持有 annotation 槽位的 CFIR 声明。 */
    val owner: CfirDeclaration,
    /** annotation 在 [owner] 的 annotation 列表中的稳定下标。 */
    val annotationIndex: Int,
    /** raw build 阶段写入槽位的原始 annotation 对象身份。 */
    val originalAnnotation: CfirAnnotationCall,
    /** 源码中的完整 annotation 文本，包含 `@` 前缀与参数列表。 */
    val rawSyntax: String,
    /** 是否来自 `@!` 强制 custom annotation 语法，仅表示语法 provenance。 */
    val forcedCustom: Boolean,
    /** annotation 是否在编译时可见，对齐官方 AST 语义位。 */
    val isCompileTimeVisible: Boolean,
    /** 源码中完整 annotation 的精确 source 范围。 */
    val annotationSource: CjSourceElement,
    /** annotation 调用的限定名；解析失败或语法缺失时为 null。 */
    val qualifiedName: FqName?,
    /** annotation 参数列表文本；无显式参数时为 null。 */
    val argumentText: String?,
    /** raw builder 采集到的 annotation token 流。 */
    val tokens: List<MacroSurfaceToken>,
    /** annotation 所附着的语义位置，用于决定 splice 路由。 */
    val callSite: MacroCallSite,
)

/**
 * 只读 annotation metadata 查询；构造期使用私有事务，ordinary checker 使用已发布索引。
 */
interface CfirAnnotationMetadata {
    fun snapshot(carrier: CfirAnnotationReplaceCarrier): CfirAnnotationSlotSnapshot?
    fun snapshot(annotation: CfirAnnotationCall): CfirAnnotationSlotSnapshot?

    fun snapshotForSurface(surface: MacroSurface): CfirAnnotationSlotSnapshot? =
        surface.replaceHandle.annotationCarrier?.let(::snapshot)
}

/**
 * session 只拥有已发布元数据的弱索引，不拥有 raw writer 或历史文件图。
 * value 也必须为弱引用：snapshot 反向持有 annotation，仅弱 key 会泄漏整代声明。
 */
class CfirAnnotationMetadataRegistry internal constructor(private val session: CfirSession) : CfirSessionComponent {
    private val snapshots = com.intellij.util.containers.ContainerUtil
        .createConcurrentWeakKeyWeakValueMap<CfirAnnotationCall, CfirAnnotationSlotSnapshot>()

    fun snapshot(annotation: CfirAnnotationCall): CfirAnnotationSlotSnapshot? =
        synchronized(session) { snapshots[annotation] }

    /** 完整文件产品使用同一个准备/提交协议；失败不会留下部分 owner 或索引。 */
    fun publish(snapshot: CfirAnnotationMetadataSnapshot): Unit = publishAll(listOf(snapshot))

    fun publishAll(products: List<CfirAnnotationMetadataSnapshot>) {
        val publication = preparePublication(products)
        try {
            publication.commit()
        } catch (failure: Throwable) {
            publication.restore()
            throw failure
        }
    }

    /** 只准备验证与恢复依据；LL 将全部 metadata/usage 产品验证后才开始共同提交。 */
    fun preparePublication(products: List<CfirAnnotationMetadataSnapshot>): CfirAnnotationMetadataPublication =
        synchronized(session) {
            Publication(products.toList()).also { it.validate() }
        }

    /**
     * 原 annotation 槽位及 owner 保留时，当前 source 产品显式替换不可变位置快照。
     * 这不是重新发布 raw metadata：先校验旧表和索引身份，语法及 annotation identity 均不得变化。
     */
    fun prepareSourceReplacement(
        declarations: Collection<CfirDeclaration>,
        sourceGeneration: Any,
        mapSource: (CjSourceElement) -> CjSourceElement,
        mapTokens: (List<MacroSurfaceToken>) -> List<MacroSurfaceToken>,
    ): CfirSourceGenerationUpdate = synchronized(session) {
        val previous = declarations.mapNotNull { it.declarationAnnotationMetadata }.distinct()
        val next = previous.map { table ->
            CfirDeclarationAnnotationMetadata(table.owner, sourceGeneration, table.slots.map { slot ->
                slot.copy(annotationSource = mapSource(slot.annotationSource), tokens = mapTokens(slot.tokens))
            })
        }
        object : CfirSourceGenerationUpdate {
            private var committed = false

            override fun validate(): Unit = synchronized(session) {
                check(!committed)
                previous.forEach { table ->
                    check(table.owner.moduleData.session === session && table.owner.declarationAnnotationMetadata === table)
                    table.slots.forEach { slot ->
                        check(table.owner.annotations.getOrNull(slot.annotationIndex) === slot.originalAnnotation)
                        check(snapshots[slot.originalAnnotation] === slot)
                    }
                }
            }

            override fun commit(): Unit = synchronized(session) {
                validate()
                next.forEach { table ->
                    table.owner.declarationAnnotationMetadata = table
                    table.slots.forEach { slot -> snapshots[slot.originalAnnotation] = slot }
                }
                committed = true
            }
        }.also { it.validate() }
    }

    private enum class PublicationState { PREPARED, COMMITTING, COMMITTED, RESTORED }

    private inner class Publication(
        private val products: List<CfirAnnotationMetadataSnapshot>,
    ) : CfirAnnotationMetadataPublication {
        private val tables = products.flatMap { it.ownerTables }
        private var state = PublicationState.PREPARED

        override fun validate(): Unit = synchronized(session) {
            check(state == PublicationState.PREPARED) { "Metadata publication is $state." }
            check(session.annotationMetadataRegistryOrNull === this@CfirAnnotationMetadataRegistry) {
                "Only the canonical session metadata registry can publish."
            }
            check(products.all { it.session === session }) { "Annotation metadata belongs to another session." }
            check(products.distinct().size == products.size) { "Duplicate metadata publication product." }
            check(products.none { it.published }) { "Annotation metadata snapshot has already been published." }
            val owners = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CfirDeclaration, Boolean>())
            val annotations = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CfirAnnotationCall, Boolean>())
            for (table in tables) {
                check(owners.add(table.owner)) { "A declaration cannot be published by multiple metadata products." }
                check(table.owner.declarationAnnotationMetadata == null) {
                    "Annotation metadata cannot overwrite a published declaration generation."
                }
                for (slot in table.slots) {
                    check(table.owner.annotations.getOrNull(slot.annotationIndex) === slot.originalAnnotation) {
                        "Annotation slots changed after metadata was sealed."
                    }
                    check(annotations.add(slot.originalAnnotation) && snapshots[slot.originalAnnotation] == null) {
                        "An annotation identity cannot be published for multiple slots or generations."
                    }
                }
            }
        }

        override fun commit(): Unit = synchronized(session) {
            validate()
            // 撤销句柄已由调用方登记；先进入 COMMITTING，部分写入也能按身份撤销。
            state = PublicationState.COMMITTING
            for (table in tables) {
                table.owner.declarationAnnotationMetadata = table
                for (slot in table.slots) snapshots[slot.originalAnnotation] = slot
            }
            products.forEach { it.published = true }
            state = PublicationState.COMMITTED
        }

        override fun restore(): Unit = synchronized(session) {
            if (state == PublicationState.RESTORED) return@synchronized
            if (state != PublicationState.PREPARED) {
                // 原状态均为未发布；只移除本次写入，复用旧 owner 的表不属于本产品。
                for (table in tables) {
                    val current = table.owner.declarationAnnotationMetadata
                    check(current == null || current === table) { "Metadata owner changed outside its publication." }
                    for (slot in table.slots) {
                        val indexed = snapshots[slot.originalAnnotation]
                        check(indexed == null || indexed === slot) { "Annotation index changed outside its publication." }
                    }
                }
                for (table in tables) {
                    if (table.owner.declarationAnnotationMetadata === table) table.owner.declarationAnnotationMetadata = null
                    for (slot in table.slots) {
                        if (snapshots[slot.originalAnnotation] === slot) snapshots.remove(slot.originalAnnotation)
                    }
                }
                products.forEach { it.published = false }
            }
            state = PublicationState.RESTORED
        }
    }
}

/** 同一 session 锁下的 metadata 发布协议；恢复只覆盖本句柄登记的写入。 */
interface CfirAnnotationMetadataPublication {
    fun validate(): Unit
    fun commit(): Unit
    fun restore(): Unit
}
/** 封闭产品只在 seal/publish 之间聚合所有 owner；已发布 owner 表不反向持有此产品。 */
class CfirAnnotationMetadataSnapshot internal constructor(
    internal val session: CfirSession,
    internal val ownerTables: List<CfirDeclarationAnnotationMetadata>,
) {
    internal var published: Boolean = false
}

/** 实际 declaration 强持有自己的不可变槽位表，避免活跃图的 metadata 被弱索引提前回收。 */
internal class CfirDeclarationAnnotationMetadata(
    val owner: CfirDeclaration,
    val generation: Any,
    val slots: List<CfirAnnotationSlotSnapshot>,
)

private object DeclarationAnnotationMetadataKey : org.cangnova.cangjie.cfir.CfirDeclarationDataKey()
private var CfirDeclaration.declarationAnnotationMetadata: CfirDeclarationAnnotationMetadata? by
    org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry.data(DeclarationAnnotationMetadataKey)
