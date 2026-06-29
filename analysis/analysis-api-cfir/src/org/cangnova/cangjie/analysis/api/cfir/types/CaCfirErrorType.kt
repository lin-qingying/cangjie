package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.utils.buildAbbreviatedType
import org.cangnova.cangjie.analysis.api.cfir.utils.createPointer
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaErrorType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.analysis.api.util.requireIsInstance
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.renderForDebugging

/**
 * 非 class-like ConeErrorType 的公开 Analysis API 错误类型实现。
 */
internal class CaCfirErrorType(
    /**
     * 底层 CFIR 错误类型。
     */
    override val coneType: ConeErrorType,
    /**
     * 用于恢复缩写类型和创建指针的 CFIR builder。
     */
    private val builder: CaSymbolByCfirBuilder,
) : CaErrorType, CaCfirType {
    /**
     * 当前公开类型的生命周期令牌。
     */
    override val token: CaLifetimeToken get() = builder.token

    /**
     * 面向调试和展示的错误类型文本。
     */
    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    /**
     * CFIR 错误诊断提供的原因文本。
     */
    override val errorMessage: String
        get() = withValidityAssertion { coneType.diagnostic.reason }


    /**
     * 可展示的错误类型文本。
     */
    override val presentableText: String?
        get() = withValidityAssertion {
            when (val diagnostic = coneType.diagnostic) {
                is ConeCannotInferTypeParameterType -> diagnostic.typeParameter.name.asString()
                else -> coneType.delegatedType?.renderForDebugging()
            }
        }

    /**
     * 错误类型当前不携带类型注解。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
        emptyTypeAnnotations(token)
        }

    /**
     * 错误类型对应的缩写类型。
     */
    override val abbreviation: CaUsualClassType?
        get() = withValidityAssertion { builder.buildAbbreviatedType(coneType) }

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
    override fun toString() = coneType.renderForDebugging()

    /**
     * 创建可跨会话恢复该错误类型的指针。
     */
    @CaExperimentalApi
    override fun createPointer(): CaTypePointer<CaErrorType> = withValidityAssertion {
        return CaCfirErrorTypePointer(coneType, builder)
    }
}

/**
 * 通过 Cone 类型指针恢复 CFIR 错误类型的公开类型指针。
 */
private class CaCfirErrorTypePointer(
    coneType: ConeErrorType,
    builder: CaSymbolByCfirBuilder,
) : CaTypePointer<CaErrorType> {
    /**
     * 底层 Cone 错误类型指针。
     */
    private val coneTypePointer = coneType.createPointer(builder)

    /**
     * 在目标会话中恢复公开错误类型。
     */
    @CaImplementationDetail
    override fun restore(session: CaSession): CaErrorType? = session.withValidityAssertion {
        requireIsInstance<CaCfirSession>(session)

        val coneType = coneTypePointer.restore(session) ?: return null
        return CaCfirErrorType(coneType, session.cfirSymbolBuilder)
    }
}
