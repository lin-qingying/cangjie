package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.createTypePointer
import org.cangnova.cangjie.analysis.api.cfir.utils.restoreErrorType
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
    override val coneType: ConeCangJieType,
    override val analysisSession: CaCfirSession,
    private val errorMessageImpl: String,
    private val presentableTextImpl: String? = null,
) : CaErrorType, CaCfirType {
    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    override val abbreviation: org.cangnova.cangjie.analysis.api.types.CaUsualClassType?
        get() = withValidityAssertion { null }

    override val errorMessage: String
        get() = withValidityAssertion { errorMessageImpl }

    override val presentableText: String?
        get() = withValidityAssertion { presentableTextImpl }

    override fun createPointer(): CaTypePointer<CaErrorType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreErrorType)
    }

    override fun equals(other: Any?) = typeEquals(other)

    override fun hashCode() = typeHashcode()

    override fun toString(): String = coneType.renderForDebugging()
}
