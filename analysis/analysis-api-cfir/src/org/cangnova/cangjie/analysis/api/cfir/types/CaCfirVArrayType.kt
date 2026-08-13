package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.cfir.utils.buildAbbreviatedType
import org.cangnova.cangjie.analysis.api.cfir.utils.createTypePointer
import org.cangnova.cangjie.analysis.api.cfir.utils.restoreVArrayType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.analysis.api.types.CaVArrayType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.renderForDebugging

/**
 * 仓颉定长数组（VArray）public type 叶子。
 */
internal class CaCfirVArrayType(
    /**
     * 底层 CFIR VArray 类型。
     */
    override val coneType: ConeVArrayType,
    /**
     * 构造公开类型所需的 CFIR Analysis API 会话。
     */
    private val analysisSession: CaCfirSession,
) : CaVArrayType, CaCfirType {
    /**
     * 当前公开类型的生命周期令牌。
     */
    override val token: CaLifetimeToken
        get() = analysisSession.token

    /**
     * 面向调试和展示的 VArray 类型文本。
     */
    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    /**
     * VArray 类型当前不携带类型注解。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    /**
     * VArray 类型对应的缩写类型。
     */
    override val abbreviation: CaUsualClassType?
        get() = withValidityAssertion { analysisSession.cfirSymbolBuilder.buildAbbreviatedType(coneType) }

    /**
     * VArray 的元素类型。
     */
    override val elementType: CaType
        get() = withValidityAssertion { coneType.elementType.asCaType(analysisSession) }

    /**
     * VArray 的编译期定长大小。
     */
    override val size: Long
        get() = withValidityAssertion { coneType.size }

    /**
     * 创建可跨会话恢复该 VArray 类型的指针。
     */
    override fun createPointer(): CaTypePointer<CaVArrayType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreVArrayType)
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
