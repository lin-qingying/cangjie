package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaTypeRelationChecker
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 对齐 Kotlin `KaFirTypeRelationChecker` 的落位方式，
 * 单独负责公开类型之间的关系判断。
 */
internal class CaCfirTypeRelationChecker(
    /**
     * 延迟取得当前 CFIR Analysis session，关系判断复用该 session 的类型上下文。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaTypeRelationChecker, CaCfirSessionComponent {
    /**
     * 判断当前类型是否是指定父类型的子类型。
     */
    override fun CaType.isSubTypeOf(superType: CaType): Boolean = withValidityAssertion {
        val subConeType = this@isSubTypeOf.requireCfirConeType("subtype check")
        val superConeType = superType.requireCfirConeType("subtype check")
        AbstractTypeChecker.isSubtypeOf(analysisSession.cfirSession.typeContext, subConeType, superConeType) == true
    }

    /**
     * 判断两个公开类型在 CFIR 类型系统中是否语义等价。
     */
    override fun CaType.semanticallyEquals(other: CaType): Boolean = withValidityAssertion {
        val leftConeType = this@semanticallyEquals.requireCfirConeType("type equality check")
        val rightConeType = other.requireCfirConeType("type equality check")
        AbstractTypeChecker.equalTypes(analysisSession.cfirSession.typeContext, leftConeType, rightConeType) == true
    }
}
