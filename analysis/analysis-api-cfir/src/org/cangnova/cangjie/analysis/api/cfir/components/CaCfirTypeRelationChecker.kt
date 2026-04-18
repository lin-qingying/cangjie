package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaTypeRelationChecker
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 对齐 Kotlin `KaFirTypeRelationChecker` 的落位方式，
 * 单独负责公开类型之间的关系判断。
 */
internal class CaCfirTypeRelationChecker(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaTypeRelationChecker, CaCfirSessionComponent {
    override fun CaType.isSubTypeOf(superType: CaType): Boolean = withValidityAssertion {
        val subConeType = this@isSubTypeOf.requireCfirConeType("subtype check")
        val superConeType = superType.requireCfirConeType("subtype check")
        analysisSession.typeQueries.isSubTypeOf(subConeType, superConeType)
    }

    override fun CaType.semanticallyEquals(other: CaType): Boolean = withValidityAssertion {
        val leftConeType = this@semanticallyEquals.requireCfirConeType("type equality check")
        val rightConeType = other.requireCfirConeType("type equality check")
        analysisSession.typeQueries.areTypesEqual(leftConeType, rightConeType)
    }
}
