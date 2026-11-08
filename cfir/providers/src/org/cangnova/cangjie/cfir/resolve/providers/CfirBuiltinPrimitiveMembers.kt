package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
import org.cangnova.cangjie.cfir.declarations.EmptyDeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperators
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name

/**
 * primitive 合成 operator 成员的统一构造入口。
 *
 * [CfirBuiltinSymbolProvider] 与 [CfirPrimitiveDeclarationFactory] 共用：
 * 成员集合只由 [BuiltinPrimitiveOperators.signaturesFor] 决定，构造形态（origin、
 * 状态、参数命名）对两个消费方必须完全一致，避免同一 primitive 在不同入口呈现
 * 不同成员表面。
 */
internal object CfirBuiltinPrimitiveMembers {
    /**
     * 为 primitive 类型构造合成 operator 成员。
     *
     * @param moduleData 成员声明归属的 module data（合成 builtins 宿主）。
     */
    fun build(
        kind: PrimitiveTypeKind,
        moduleData: CfirModuleData,
    ): List<CfirDeclaration> =
        BuiltinPrimitiveOperators.signaturesFor(kind).map { signature ->
            val functionSymbol = CfirNamedFunctionSymbol(CallableId(kind.classId, signature.name))
            val parameters = signature.parameterKinds.mapIndexed { index, parameterKind ->
                val parameterSymbol = CfirValueParameterSymbol(CallableId(signature.name))
                buildValueParameter {
                    this.moduleData = moduleData
                    resolvePhase = CfirResolvePhase.BODY_RESOLVE
                    origin = org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin.Synthetic.FakeFunction
                    attributes = org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes.EMPTY
                    isLocal = false
                    deprecationsProvider = EmptyDeprecationsProvider
                    dispatchReceiverType = null
                    symbol = parameterSymbol
                    containingDeclarationSymbol = functionSymbol
                    isNamed = false
                    status = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
                    returnTypeRef = buildResolvedTypeRef {
                        coneType = ConePrimitiveType(parameterKind)
                    }
                    name = Name.identifier("p$index")
                }
            }

            val status = CfirDeclarationStatusImpl().apply {
                isOperator = true
            }
            buildNamedFunction {
                this.moduleData = moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin.Synthetic.FakeFunction
                attributes = org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes.EMPTY
                isLocal = false
                dispatchReceiverType = null
                this.status = status
                returnTypeRef = buildResolvedTypeRef {
                    coneType = ConePrimitiveType(signature.returnKind)
                }
                valueParameters += parameters
                symbol = functionSymbol
                name = signature.name
                isMut = false
            }
        }
}
