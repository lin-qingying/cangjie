@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirFunctionImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirValueParameterImpl
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.impl.CfirLiteralExpressionImpl
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionStage
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.name.Name

/**
 * Phase 3 调用解析单元测试共享工具。
 *
 * 提供构建 CfirFunction、CfirValueParameter、CfirCandidate 等测试 fixture 的工厂方法。
 */
object CallResolutionTestFixtures {

    val TEST_MODULE_DATA = CfirModuleData(Name.identifier("test-module"))

    /** 构建一个绑定到函数声明的 CfirFunctionSymbol */
    fun buildFunctionSymbol(
        name: String,
        returnType: ConeCangjieType = ConePrimitiveType.UNIT,
        parameterTypes: List<ConeCangjieType> = emptyList(),
        parameterNames: List<String>? = null,
        parameterDefaults: List<Boolean>? = null,
        typeParameters: List<CfirTypeParameter> = emptyList(),
    ): CfirFunctionSymbol {
        val symbol = CfirFunctionSymbol()
        val params = parameterTypes.mapIndexed { index, type ->
            buildValueParameter(
                name = parameterNames?.getOrNull(index) ?: "p$index",
                type = type,
                hasDefault = parameterDefaults?.getOrNull(index) ?: false,
            )
        }
        val function = CfirFunctionImpl(
            symbol = symbol,
            origin = CfirDeclarationOrigin.Source,
            annotations = emptyList(),
            moduleData = TEST_MODULE_DATA,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            attributes = CfirDeclarationAttributes.EMPTY,
            status = CfirDeclarationStatusImpl(),
            typeParameters = typeParameters,
            returnTypeRef = CfirResolvedTypeRefImpl(returnType),
            name = Name.identifier(name),
            valueParameters = params,
            body = null,
            isMut = false,
        )
        symbol.bind(function)
        return symbol
    }

    /** 构建一个 CfirValueParameter */
    fun buildValueParameter(
        name: String,
        type: ConeCangjieType,
        hasDefault: Boolean = false,
    ): CfirValueParameter {
        val symbol = CfirValueParameterSymbol()
        val defaultExpr: CfirExpression? = if (hasDefault) {
            CfirLiteralExpressionImpl(null, CfirLiteralKind.INT, 0)
        } else null
        val param = CfirValueParameterImpl(
            symbol = symbol,
            origin = CfirDeclarationOrigin.Source,
            annotations = emptyList(),
            moduleData = TEST_MODULE_DATA,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            attributes = CfirDeclarationAttributes.EMPTY,
            status = CfirDeclarationStatusImpl(),
            typeParameters = emptyList(),
            returnTypeRef = CfirResolvedTypeRefImpl(type),
            name = Name.identifier(name),
            defaultValue = defaultExpr,
        )
        symbol.bind(param)
        return param
    }

    /** 构建一个带类型的表达式 stub（用作调用参数） */
    fun buildTypedExpression(type: ConeCangjieType): CfirExpression {
        return CfirLiteralExpressionImpl(type, CfirLiteralKind.INT, 0)
    }

    /** 构建一个最小的 CfirCallInfo（用于测试验证阶段） */
    fun buildCallInfo(
        name: String,
        arguments: List<CfirExpression> = emptyList(),
        stages: List<CfirResolutionStage> = emptyList(),
    ): CfirCallInfo {
        return CfirCallInfo(
            callSite = CfirLiteralExpressionImpl(null, CfirLiteralKind.UNIT, null),
            callKind = CfirCallKind.Function(stages),
            name = Name.identifier(name),
            explicitReceiver = null,
            arguments = arguments,
            typeArguments = emptyList(),
            session = StubCfirSession,
        )
    }

    /** 构建 CfirCandidate */
    fun buildCandidate(
        functionSymbol: CfirFunctionSymbol,
        callInfo: CfirCallInfo,
    ): CfirCandidate {
        return CfirCandidate(
            symbol = functionSymbol,
            callInfo = callInfo,
        )
    }
}

/**
 * 最小化 CfirSession stub，仅用于测试。
 */
private object StubCfirSession : org.cangnova.cangjie.cfir.session.CfirSession(Kind.Source) {
    override fun toString(): String = "StubCfirSession"
}
