package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.buildAbbreviatedType
import org.cangnova.cangjie.analysis.api.cfir.utils.createTypePointer
import org.cangnova.cangjie.analysis.api.cfir.utils.restorePrimitiveType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.renderForDebugging

/**
 * 基本类型 public 叶子，对齐 CFIR `ConePrimitiveType(kind)` 模型。
 */
internal class CaCfirPrimitiveType(
    /**
     * 底层 CFIR 基本类型。
     */
    override val coneType: ConePrimitiveType,
    /**
     * 构造公开类型所需的 CFIR Analysis API 会话。
     */
    private val analysisSession: CaCfirSession,
) : CaPrimitiveType(), CaCfirType {
    /**
     * 当前公开类型的生命周期令牌。
     */
    override val token: CaLifetimeToken
        get() = analysisSession.token

    /**
     * 面向调试和展示的类型文本。
     */
    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    /**
     * 基本类型当前不携带类型注解。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    /**
     * 基本类型对应的缩写类型。
     */
    override val abbreviation: org.cangnova.cangjie.analysis.api.types.CaUsualClassType?
        get() = withValidityAssertion { analysisSession.cfirSymbolBuilder.buildAbbreviatedType(coneType) }

    /**
     * 基本类型种类。
     */
    override val kind: PrimitiveTypeKind
        get() = withValidityAssertion { coneType.kind }

    /**
     * 创建可跨会话恢复该基本类型的指针。
     */
    override fun createPointer(): CaTypePointer<CaPrimitiveType> = withValidityAssertion {
        createTypePointer(coneType, ::restorePrimitiveType)
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
