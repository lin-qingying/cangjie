package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.name.FqName
import java.util.IdentityHashMap

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
 *
 * @property owner 持有 annotation 槽位的 CFIR 声明。
 * @property annotationIndex annotation 在 [owner] 的 annotation 列表中的稳定下标。
 * @property originalAnnotation raw build 阶段写入槽位的原始 annotation 对象身份。
 * @property rawSyntax 源码中的完整 annotation 文本，包含 `@` 前缀与参数列表。
 * @property forcedCustom 是否来自 `@!` 强制 custom annotation 语法。
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
    /** 是否来自 `@!` 强制 custom annotation 语法。 */
    val forcedCustom: Boolean,
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
 * session 级 annotation metadata registry。
 *
 * 生命周期：
 * 1. raw build 前创建；
 * 2. raw build / construction 期间可写；
 * 3. stable splice + cache snapshot 后或 failure path 返回前冻结。
 */
class CfirAnnotationMetadataRegistry : CfirSessionComponent {
    /** 以稳定槽位身份索引的 snapshot 表，保证同一 owner/index 只记录一次。 */
    private val snapshotsByCarrier = LinkedHashMap<CfirAnnotationReplaceCarrier, CfirAnnotationSlotSnapshot>()
    /** 以原始 annotation 对象身份索引的 snapshot 表，用于从 CFIR annotation 反查槽位。 */
    private val snapshotsByAnnotationIdentity = IdentityHashMap<CfirAnnotationCall, CfirAnnotationSlotSnapshot>()
    /** registry 是否已经冻结；冻结后禁止继续写入或迁移。 */
    private var frozen = false

    /** 当前 registry 是否已进入只读状态。 */
    val isFrozen: Boolean
        get() = frozen

    /** 按记录顺序返回所有 annotation 槽位 snapshot。 */
    val snapshots: List<CfirAnnotationSlotSnapshot>
        get() = snapshotsByCarrier.values.toList()

    /**
     * 记录一个 raw builder 刚写入的 annotation 槽位 snapshot。
     *
     * 本方法同时建立 carrier 与 annotation identity 两套索引，并校验
     * [CfirAnnotationSlotSnapshot.annotationIndex] 仍然指向同一个原始 annotation 对象。
     */
    fun record(snapshot: CfirAnnotationSlotSnapshot): CfirAnnotationReplaceCarrier {
        check(!frozen) { "CfirAnnotationMetadataRegistry is frozen." }
        val carrier = CfirAnnotationReplaceCarrier(
            owner = snapshot.owner,
            annotationIndex = snapshot.annotationIndex,
            originalAnnotation = snapshot.originalAnnotation,
        )
        require(snapshot.owner.annotations.getOrNull(snapshot.annotationIndex) === snapshot.originalAnnotation) {
            "Annotation slot snapshot must be recorded after append and must point at the original annotation identity."
        }
        require(carrier !in snapshotsByCarrier) {
            "Duplicate annotation metadata for owner/index ${snapshot.annotationIndex}."
        }
        snapshotsByCarrier[carrier] = snapshot
        snapshotsByAnnotationIdentity[snapshot.originalAnnotation] = snapshot
        return carrier
    }

    /** 通过稳定槽位身份查找 snapshot。 */
    fun snapshot(carrier: CfirAnnotationReplaceCarrier): CfirAnnotationSlotSnapshot? =
        snapshotsByCarrier[carrier]

    /** 通过原始或迁移后的 annotation 对象身份查找 snapshot。 */
    fun snapshot(annotation: CfirAnnotationCall): CfirAnnotationSlotSnapshot? =
        snapshotsByAnnotationIdentity[annotation]

    /** 从 macro surface 的 replace handle 中取 annotation carrier 并反查 snapshot。 */
    fun snapshotForSurface(surface: MacroSurface): CfirAnnotationSlotSnapshot? {
        val carrier = surface.replaceHandle.annotationCarrier ?: return null
        return snapshot(carrier)
    }

    /**
     * successful splice 后迁移 slot metadata 到 replacement annotation。
     */
    fun migrate(carrier: CfirAnnotationReplaceCarrier, replacement: CfirAnnotationCall) {
        check(!frozen) { "CfirAnnotationMetadataRegistry is frozen." }
        val snapshot = snapshotsByCarrier[carrier] ?: return
        val migrated = snapshot.copy(originalAnnotation = replacement)
        val migratedCarrier = carrier.copy(originalAnnotation = replacement)
        snapshotsByCarrier.remove(carrier)
        snapshotsByCarrier[migratedCarrier] = migrated
        snapshotsByAnnotationIdentity.remove(carrier.originalAnnotation)
        snapshotsByAnnotationIdentity[replacement] = migrated
    }

    /** 冻结 registry，阻止 construction 后续阶段继续修改 annotation 槽位元数据。 */
    fun freeze() {
        frozen = true
    }
}
