package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.utils.buildAbbreviatedType
import org.cangnova.cangjie.analysis.api.cfir.utils.createTypePointer
import org.cangnova.cangjie.analysis.api.cfir.utils.restoreTypeParameterType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaTypeParameterType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.name.Name

/**
 * 仓颉类型参数 public type 叶子。
 */
internal class CaCfirTypeParameterType(
    /**
     * 底层 CFIR 类型参数类型。
     */
    override val coneType: ConeTypeParameterType,
    /**
     * 用于构造类型参数符号和缩写类型的 CFIR builder。
     */
    private val builder: CaSymbolByCfirBuilder,

    ) : CaTypeParameterType(), CaCfirType {
    /**
     * 面向调试和展示的类型参数类型文本。
     */
    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    /**
     * 类型参数类型当前不携带类型注解。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    /**
     * 类型参数类型对应的缩写类型。
     */
    override val abbreviation: org.cangnova.cangjie.analysis.api.types.CaUsualClassType?
        get() = withValidityAssertion { builder.buildAbbreviatedType(coneType) }

    /**
     * 类型参数名称。
     */
    override val name: Name
        get() = withValidityAssertion { coneType.lookupTag.name }

    /**
     * 类型参数对应的公开符号。
     */
    override val symbol: CaTypeParameterSymbol
        get() = withValidityAssertion {
            builder.classifierBuilder.buildTypeParameterSymbol(coneType.lookupTag.typeParameterSymbol)
        }

    /**
     * 创建可跨会话恢复该类型参数类型的指针。
     */
    override fun createPointer(): CaTypePointer<CaTypeParameterType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreTypeParameterType)
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

    /**
     * 当前公开类型的生命周期令牌。
     */
    override val token: CaLifetimeToken get() = builder.token

}
