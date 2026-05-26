package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.type.AbstractTypePreparator
import org.cangnova.cangjie.type.model.CangJieTypeMarker

/**
 * 对齐 Kotlin FIR `ConeTypePreparator`：
 * 进入通用 subtype/equality 算法前，先把顶层 typealias 规约成真实语义类型。
 *
 * `DISABLE_TYPEALIAS_EXPANSION` 只要求保留声明/引用处的别名视图，
 * 不应让类型系统内部的语义比较继续拿 `ConeTypeAliasType` 当真实类型头工作。
 */
class ConeTypePreparator(
    private val session: CfirSession,
) : AbstractTypePreparator() {
    private fun <T : ConeRigidType> prepareRigidType(type: T): T {
        @Suppress("UNCHECKED_CAST")
        return when (type) {
            is ConeTypeAliasType -> type.fullyExpandedType(session)
            else -> type
        } as T
    }

    override fun prepareType(type: CangJieTypeMarker): ConeCangJieType {
        if (type !is ConeCangJieType) {
            throw AssertionError("Unexpected type in ConeTypePreparator: ${type::class.java}")
        }

        return when (type) {
            is ConeRigidType -> prepareRigidType(type)
        }
    }
}
