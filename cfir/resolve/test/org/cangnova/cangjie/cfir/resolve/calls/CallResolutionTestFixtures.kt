@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.declarations.ResolveStateAccess::class,
)

package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
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
 * Phase 3 璋冪敤瑙ｆ瀽鍗曞厓娴嬭瘯鍏变韩宸ュ叿銆? *
 * 鎻愪緵鏋勫缓 CfirFunction銆丆firValueParameter銆丆firCandidate 绛夋祴璇?fixture 鐨勫伐鍘傛柟娉曘€? */
object CallResolutionTestFixtures {

    val TEST_MODULE_DATA: CfirModuleData = CfirSourceModuleData(
        name = Name.identifier("test-module"),
        dependencies = emptyList(),
        refinementDependencies = emptyList(),
        platform = CfirPlatform.DEFAULT,
    ).apply {
        bindSession(StubCfirSession)
    }

    /** 鏋勫缓涓€涓粦瀹氬埌鍑芥暟澹版槑鐨?CfirFunctionSymbol */
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
            source = null,
            moduleData = TEST_MODULE_DATA,
            annotations = emptyList(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Source,
            attributes = CfirDeclarationAttributes.EMPTY,
            status = CfirDeclarationStatusImpl(),
            typeParameters = typeParameters,
            returnTypeRef = CfirResolvedTypeRefImpl(source = null, annotations = emptyList(), coneType = returnType, delegatedTypeRef = null),
            name = Name.identifier(name),
            valueParameters = params,
            body = null,
            isMut = false,
        )
        function.resolveState = CfirResolvePhase.BODY_RESOLVE.asResolveState()
        symbol.bind(function)
        return symbol
    }

    /** 鏋勫缓涓€涓?CfirValueParameter */
    fun buildValueParameter(
        name: String,
        type: ConeCangjieType,
        hasDefault: Boolean = false,
    ): CfirValueParameter {
        val symbol = CfirValueParameterSymbol()
        val defaultExpr: CfirExpression? = if (hasDefault) {
            CfirLiteralExpressionImpl(source = null, annotations = emptyList(), coneTypeOrNull = null, kind = CfirLiteralKind.INT, value = 0)
        } else null
        val param = CfirValueParameterImpl(
            source = null,
            moduleData = TEST_MODULE_DATA,
            annotations = emptyList(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Source,
            attributes = CfirDeclarationAttributes.EMPTY,
            status = CfirDeclarationStatusImpl(),
            typeParameters = emptyList(),
            returnTypeRef = CfirResolvedTypeRefImpl(source = null, annotations = emptyList(), coneType = type, delegatedTypeRef = null),
            name = Name.identifier(name),
            defaultValue = defaultExpr,
        )
        param.resolveState = CfirResolvePhase.BODY_RESOLVE.asResolveState()
        symbol.bind(param)
        return param
    }

    /** 鏋勫缓涓€涓甫绫诲瀷鐨勮〃杈惧紡 stub锛堢敤浣滆皟鐢ㄥ弬鏁帮級 */
    fun buildTypedExpression(type: ConeCangjieType): CfirExpression {
        return CfirLiteralExpressionImpl(source = null, annotations = emptyList(), coneTypeOrNull = type, kind = CfirLiteralKind.INT, value = 0)
    }

    /** 鏋勫缓涓€涓渶灏忕殑 CfirCallInfo锛堢敤浜庢祴璇曢獙璇侀樁娈碉級 */
    fun buildCallInfo(
        name: String,
        arguments: List<CfirExpression> = emptyList(),
        stages: List<CfirResolutionStage> = emptyList(),
    ): CfirCallInfo {
        return CfirCallInfo(
            callSite = CfirLiteralExpressionImpl(source = null, annotations = emptyList(), coneTypeOrNull = null, kind = CfirLiteralKind.UNIT, value = null),
            callKind = CfirCallKind.Function(stages),
            name = Name.identifier(name),
            explicitReceiver = null,
            arguments = arguments,
            typeArguments = emptyList(),
            session = StubCfirSession,
        )
    }

    /** 鏋勫缓 CfirCandidate */
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
 * 鏈€灏忓寲 CfirSession stub锛屼粎鐢ㄤ簬娴嬭瘯銆? */
private object StubCfirSession : org.cangnova.cangjie.cfir.session.CfirSession(Kind.Source) {
    override fun toString(): String = "StubCfirSession"
}


