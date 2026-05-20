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
 */
data class CfirAnnotationReplaceCarrier(
    val owner: CfirDeclaration,
    val annotationIndex: Int,
    val originalAnnotation: CfirAnnotationCall,
)

/**
 * raw builder 在 annotation append 后记录的完整 slot snapshot。
 *
 * [rawSyntax] 保留完整 `@Anno[...]` / `@!Anno[...]` 文本；[argumentText]
 * 仅保留 `[]` 参数列表文本，用于 cache 和 public analysis argument 投影。
 */
data class CfirAnnotationSlotSnapshot(
    val owner: CfirDeclaration,
    val annotationIndex: Int,
    val originalAnnotation: CfirAnnotationCall,
    val rawSyntax: String,
    val forcedCustom: Boolean,
    val qualifiedName: FqName?,
    val argumentText: String?,
    val tokens: List<MacroSurfaceToken>,
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
    private val snapshotsByCarrier = LinkedHashMap<CfirAnnotationReplaceCarrier, CfirAnnotationSlotSnapshot>()
    private val snapshotsByAnnotationIdentity = IdentityHashMap<CfirAnnotationCall, CfirAnnotationSlotSnapshot>()
    private var frozen = false

    val isFrozen: Boolean
        get() = frozen

    val snapshots: List<CfirAnnotationSlotSnapshot>
        get() = snapshotsByCarrier.values.toList()

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

    fun snapshot(carrier: CfirAnnotationReplaceCarrier): CfirAnnotationSlotSnapshot? =
        snapshotsByCarrier[carrier]

    fun snapshot(annotation: CfirAnnotationCall): CfirAnnotationSlotSnapshot? =
        snapshotsByAnnotationIdentity[annotation]

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

    fun freeze() {
        frozen = true
    }
}
