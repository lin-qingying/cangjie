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
import org.cangnova.cangjie.cfir.constraints.ExplicitReceiverKind
import org.cangnova.cangjie.cfir.resolve.CfirConstraintSystemImpl
import org.cangnova.cangjie.cfir.resolve.CfirTypeRelations
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveContext
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.body.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeContext
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Phase 3 调用解析单元测试共享工具。
 * 提供构建 `CfirFunction`、`CfirValueParameter`、`CfirCandidate` 等测试 fixture 的工厂方法。
 */
object CallResolutionTestFixtures {
    private val STUB_BODY_RESOLVE_CONTEXT = CfirBodyResolveContext(
        ReturnTypeCalculator.Default,
        CfirDataFlowAnalyzerContext(),
    )

    private val STUB_TYPE_RELATIONS = CfirTypeRelations(object : ConeTypeContext {
        override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> = emptyList()

        override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean = a == b
    })

    private val STUB_RESOLUTION_CONTEXT = CfirResolutionContext(
        session = StubCfirSession,
        bodyResolveContext = STUB_BODY_RESOLVE_CONTEXT,
        typeRelations = STUB_TYPE_RELATIONS,
    )


    val TEST_MODULE_DATA: CfirModuleData = CfirSourceModuleData(
        name = Name.identifier("test-module"),
        dependencies = emptyList(),
        refinementDependencies = emptyList(),
        platform = CfirPlatform.DEFAULT,
    ).apply {
        bindSession(StubCfirSession)
    }

    /** 构建一个绑定到函数声明上的 `CfirFunctionSymbol`。 */
    fun buildFunctionSymbol(
        name: String,
        returnType: ConeCangJieType = ConePrimitiveType.UNIT,
        parameterTypes: List<ConeCangJieType> = emptyList(),
        parameterNames: List<String>? = null,
        parameterDefaults: List<Boolean>? = null,
        typeParameters: List<CfirTypeParameter> = emptyList(),
    ): CfirFunctionSymbol {
        val symbol = CfirNamedFunctionSymbol(CallableId(FqName.ROOT, Name.identifier(name)))
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

    /** 构建一个 `CfirValueParameter`。 */
    fun buildValueParameter(
        name: String,
        type: ConeCangJieType,
        hasDefault: Boolean = false,
    ): CfirValueParameter {
        val symbol = CfirValueParameterSymbol(CallableId(Name.identifier(name)))
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

    /** 构建一个带类型的表达式 stub，用作调用参数。 */
    fun buildTypedExpression(type: ConeCangJieType): CfirExpression {
        return CfirLiteralExpressionImpl(source = null, annotations = emptyList(), coneTypeOrNull = type, kind = CfirLiteralKind.INT, value = 0)
    }

    /** 构建一个最小化的 `CfirCallInfo`，用于验证阶段测试。 */
    fun buildCallInfo(
        name: String,
        arguments: List<CfirExpression> = emptyList(),
        callKind: CfirCallKind = CfirCallKind.Function,
    ): CfirCallInfo {
        return CfirCallInfo(
            callSite = CfirLiteralExpressionImpl(source = null, annotations = emptyList(), coneTypeOrNull = null, kind = CfirLiteralKind.UNIT, value = null),
            callKind = callKind,
            name = Name.identifier(name),
            explicitReceiver = null,
            arguments = arguments,
            typeArguments = emptyList(),
            session = StubCfirSession,
        )
    }

    /** 构建 `CfirCandidate`。 */
    fun buildCandidate(
        functionSymbol: CfirFunctionSymbol,
        callInfo: CfirCallInfo,
    ): CfirCandidate {
        return CfirCandidate(
            symbol = functionSymbol,
            dispatchReceiver = null,
            givenExtensionReceiver = null,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            callInfo = callInfo,
            originScope = null,
            resolutionContext = STUB_RESOLUTION_CONTEXT,
            constraintSystem = CfirConstraintSystemImpl(STUB_TYPE_RELATIONS),
        )
    }
}

/**
 * 最小化的 `CfirSession` stub，仅用于测试。
 */
private object StubCfirSession : org.cangnova.cangjie.cfir.session.CfirSession(Kind.Source) {
    override fun toString(): String = "StubCfirSession"
}

