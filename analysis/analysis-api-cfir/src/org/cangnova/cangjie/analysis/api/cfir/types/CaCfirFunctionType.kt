package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.cfir.utils.buildAbbreviatedType
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
    /**
     * 底层 CFIR 函数类型。
     */
    override val coneType: ConeFunctionType,
    /**
     * 构造公开类型所需的 CFIR Analysis API 会话。
     */
    private val analysisSession: CaCfirSession,
) : CaFunctionType, CaCfirType {
    /**
     * 当前公开类型的生命周期令牌。
     */
    override val token: CaLifetimeToken
        get() = analysisSession.token

    /**
     * 面向调试和展示的函数类型文本。
     */
    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    /**
     * 函数类型当前不携带类型注解。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    /**
     * 函数类型对应的缩写类型。
     */
    override val abbreviation: org.cangnova.cangjie.analysis.api.types.CaUsualClassType?
        get() = withValidityAssertion { analysisSession.cfirSymbolBuilder.buildAbbreviatedType(coneType) }

    /**
     * 函数参数类型列表。
     */
    override val parameterTypes: List<CaType>
        get() = withValidityAssertion {
            coneType.parameterTypes.map { parameterType -> parameterType.asCaType(analysisSession) }
        }

    /**
     * 函数返回类型。
     */
    override val returnType: CaType
        get() = withValidityAssertion {
            coneType.returnType.asCaType(analysisSession)
        }

    /**
     * 当前函数类型是否为 C 函数类型。
     */
    override val isCFunction: Boolean
        get() = withValidityAssertion { coneType.isCFunc }

    /**
     * 当前函数类型是否为闭包类型。
     */
    override val isClosureType: Boolean
        get() = withValidityAssertion { coneType.isClosureType }

    /**
     * 当前函数类型是否包含可变长参数。
     */
    override val hasVariableLengthArgument: Boolean
        get() = withValidityAssertion { coneType.hasVariableLenArg }

    /**
     * 创建可跨会话恢复该函数类型的指针。
     */
    override fun createPointer(): CaTypePointer<CaFunctionType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreFunctionType)
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
