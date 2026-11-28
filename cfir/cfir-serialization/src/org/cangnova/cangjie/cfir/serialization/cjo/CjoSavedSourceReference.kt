package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.source.CfirSourceSavedReference

/** 原 binder 的保存字段来源；不持有 FullId/accessor/ByteBuffer，不解析 ignored 语义引用。 */
internal data class CjoSavedSourceReference(
    val binderIdentity: Any,
    val target: Target,
    val declarationKey: String?,
    val index: UInt,
) : CfirSourceSavedReference {
    sealed interface Target {
        data object Missing : Target
        data object Invalid : Target
        data class Current(val rawIndex: Int, val declarationKind: UShort) : Target
        data class External(val packageName: String, val declarationKey: String) : Target
        data class Package(val packageName: String, val index: UInt) : Target
    }
}
