package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.cfir.utils.buildAbbreviatedType
import org.cangnova.cangjie.analysis.api.cfir.utils.createTypePointer
import org.cangnova.cangjie.analysis.api.cfir.utils.restoreTupleType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaTupleType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.renderForDebugging

/**
 * 仓颉 tuple public type 叶子。
 */
internal class CaCfirTupleType(
    /**
     * 底层 CFIR tuple 类型。
     */
    override val coneType: ConeTupleType,
    /**
     * 构造公开类型所需的 CFIR Analysis API 会话。
     */
    private val analysisSession: CaCfirSession,
) : CaTupleType, CaCfirType {
    /**
     * 当前公开类型的生命周期令牌。
     */
    override val token: CaLifetimeToken
        get() = analysisSession.token

    /**
     * 面向调试和展示的 tuple 类型文本。
     */
    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    /**
     * tuple 类型当前不携带类型注解。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    /**
     * tuple 类型对应的缩写类型。
     */
    override val abbreviation: org.cangnova.cangjie.analysis.api.types.CaUsualClassType?
        get() = withValidityAssertion { analysisSession.cfirSymbolBuilder.buildAbbreviatedType(coneType) }

    /**
     * tuple 的元素类型列表。
     */
    override val elementTypes: List<CaType>
        get() = withValidityAssertion {
            coneType.elementTypes.map { elementType -> elementType.asCaType(analysisSession) }
        }

    /**
     * 创建可跨会话恢复该 tuple 类型的指针。
     */
    override fun createPointer(): CaTypePointer<CaTupleType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreTupleType)
    }

    /**
     * 按底层 Cone 类型判断公开类型相等性。
     */
    override fun equals(other: Any?) = typeEquals(other)

    /**
     * 返回底层 Cone 类型的哈希码。
     */
    override fun hashCode() = typeHashcode()

    /**
     * 返回底层 Cone 类型调试文本。
     */
    override fun toString(): String = coneType.renderForDebugging()
}
