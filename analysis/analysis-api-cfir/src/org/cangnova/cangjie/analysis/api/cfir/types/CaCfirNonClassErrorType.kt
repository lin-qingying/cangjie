package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.buildAbbreviatedType
import org.cangnova.cangjie.analysis.api.cfir.utils.createTypePointer
import org.cangnova.cangjie.analysis.api.cfir.utils.restoreErrorType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaErrorType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.renderForDebugging

/**
 * 非 class-like error type 的 public 叶子。
 *
 * 这类类型在仓颉公开 API 中仍然需要稳定的错误类型视图，
 * 但不再伪装成 class-like 类型。
 */
internal class CaCfirNonClassErrorType(
    /**
     * 底层 CFIR 非 class-like 错误类型。
     */
    override val coneType: ConeCangJieType,
    /**
     * 构造公开类型所需的 CFIR Analysis API 会话。
     */
    private val analysisSession: CaCfirSession,
    /**
     * 公开错误类型使用的错误原因文本。
     */
    private val errorMessageImpl: String,
    /**
     * 可展示错误文本；没有稳定展示文本时为 null。
     */
    private val presentableTextImpl: String? = null,
) : CaErrorType, CaCfirType {
    /**
     * 当前公开类型的生命周期令牌。
     */
    override val token: CaLifetimeToken
        get() = analysisSession.token

    /**
     * 面向调试和展示的错误类型文本。
     */
    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    /**
     * 错误类型当前不携带类型注解。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    /**
     * 错误类型对应的缩写类型。
     */
    override val abbreviation: org.cangnova.cangjie.analysis.api.types.CaUsualClassType?
        get() = withValidityAssertion { analysisSession.cfirSymbolBuilder.buildAbbreviatedType(coneType) }

    /**
     * 返回构造时指定的错误原因文本。
     */
    override val errorMessage: String
        get() = withValidityAssertion { errorMessageImpl }

    /**
     * 返回构造时指定的可展示文本。
     */
    override val presentableText: String?
        get() = withValidityAssertion { presentableTextImpl }

    /**
     * 创建可跨会话恢复该错误类型的指针。
     */
    override fun createPointer(): CaTypePointer<CaErrorType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreErrorType)
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
