package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.cfir.utils.createTypePointer
import org.cangnova.cangjie.analysis.api.cfir.utils.restoreFunctionType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.renderForDebugging

/**
 * 仓颉函数类型 public 叶子。
 *
 * 这里保持仓颉既有公开语义：`CaFunctionType : CaType`，
 * 仅把 CFIR 侧实现落位拆回 Kotlin 风格的单叶子文件。
 */
internal class CaCfirFunctionType(
    override val coneType: ConeFunctionType,
    private val analysisSession: CaCfirSession,
) : CaFunctionType, CaCfirType {
    override val token: CaLifetimeToken
        get() = analysisSession.token

    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    override val abbreviation: org.cangnova.cangjie.analysis.api.types.CaUsualClassType?
        get() = withValidityAssertion { null }

    override val parameterTypes: List<CaType>
        get() = withValidityAssertion {
            coneType.parameterTypes.map { parameterType -> parameterType.asCaType(analysisSession) }
        }

    override val returnType: CaType
        get() = withValidityAssertion {
            coneType.returnType.asCaType(analysisSession)
        }

    override val isCFunction: Boolean
        get() = withValidityAssertion { coneType.isCFunc }

    override val isClosureType: Boolean
        get() = withValidityAssertion { coneType.isClosureType }

    override val hasVariableLengthArgument: Boolean
        get() = withValidityAssertion { coneType.hasVariableLenArg }

    override fun createPointer(): CaTypePointer<CaFunctionType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreFunctionType)
    }

    override fun equals(other: Any?) = typeEquals(other)

    override fun hashCode() = typeHashcode()

    override fun toString(): String = coneType.renderForDebugging()
}
