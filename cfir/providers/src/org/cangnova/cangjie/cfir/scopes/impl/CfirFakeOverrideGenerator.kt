package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.copy
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.expressions.builder.buildExpressionStub
import org.cangnova.cangjie.cfir.originalForSubstitutionOverrideAttr
import org.cangnova.cangjie.cfir.scopes.DeferredCallableCopyReturnType
import org.cangnova.cangjie.cfir.scopes.deferredCallableCopyReturnType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * 对齐 Kotlin FirFakeOverrideGenerator 的签名副本构造。
 *
 * scope 负责计算替换数据；本对象统一声明状态、参数 owner、默认实参标记和延迟返回类型。
 * 副本共享已知的声明注解，由 LL 解析其真实 owner；源码 body、initializer 和 CFG 不属于副本。
 */
object CfirFakeOverrideGenerator {
    fun createCopyForCfirFunction(
        newSymbol: CfirNamedFunctionSymbol,
        baseFunction: CfirNamedFunction,
        session: CfirSession,
        origin: CfirDeclarationOrigin,
        newDispatchReceiverType: ConeSimpleCangJieType?,
        newParameterTypes: List<CfirTypeRef>,
        newTypeParameters: List<CfirTypeParameter>,
        newReturnType: CfirTypeRef,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType?,
    ): CfirNamedFunction {
        checkStatusIsResolved(baseFunction)
        return buildNamedFunction {
            source = baseFunction.source
            moduleData = session.moduleData
            this.origin = origin
            attributes = baseFunction.attributes.copy()
            annotations += baseFunction.annotations
            isLocal = baseFunction.isLocal
            deprecationsProvider = baseFunction.deprecationsProvider
            status = baseFunction.status.copy(isOverride = true)
            symbol = newSymbol
            name = baseFunction.name
            resolvePhase = origin.resolvePhaseForCopy
            dispatchReceiverType = newDispatchReceiverType
            typeParameters += newTypeParameters
            returnTypeRef = newReturnType
            valueParameters += buildCopiesForValueParameters(baseFunction.valueParameters, newParameterTypes, origin, newSymbol)
            hasVariableLenArg = baseFunction.hasVariableLenArg
            isMut = baseFunction.isMut
        }.apply {
            attributes.deferredCallableCopyReturnType = deferredReturnTypeCalculation
            originalForSubstitutionOverrideAttr = baseFunction
        }
    }

    fun createCopyForCfirConstructor(
        newSymbol: CfirConstructorSymbol,
        baseConstructor: CfirConstructor,
        session: CfirSession,
        origin: CfirDeclarationOrigin,
        newDispatchReceiverType: ConeSimpleCangJieType?,
        newParameterTypes: List<CfirTypeRef>,
        newTypeParameters: List<CfirTypeParameter>,
        newReturnType: CfirTypeRef,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType?,
    ): CfirConstructor {
        checkStatusIsResolved(baseConstructor)
        return buildConstructor {
            source = baseConstructor.source
            moduleData = session.moduleData
            this.origin = origin
            attributes = baseConstructor.attributes.copy()
            annotations += baseConstructor.annotations
            isLocal = baseConstructor.isLocal
            deprecationsProvider = baseConstructor.deprecationsProvider
            status = baseConstructor.status.copy()
            symbol = newSymbol
            resolvePhase = origin.resolvePhaseForCopy
            dispatchReceiverType = newDispatchReceiverType
            typeParameters += newTypeParameters
            returnTypeRef = newReturnType
            valueParameters += buildCopiesForValueParameters(baseConstructor.valueParameters, newParameterTypes, origin, newSymbol)
            hasVariableLenArg = baseConstructor.hasVariableLenArg
        }.apply {
            attributes.deferredCallableCopyReturnType = deferredReturnTypeCalculation
            originalForSubstitutionOverrideAttr = baseConstructor
        }
    }

    fun createCopyForCfirEnumConstructor(
        newSymbol: CfirEnumConstructorSymbol,
        baseConstructor: CfirEnumConstructor,
        session: CfirSession,
        origin: CfirDeclarationOrigin,
        newDispatchReceiverType: ConeSimpleCangJieType?,
        newParameterTypes: List<CfirTypeRef>,
        newTypeParameters: List<CfirTypeParameterRef>,
        newReturnType: CfirTypeRef,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType?,
    ): CfirEnumConstructor {
        checkStatusIsResolved(baseConstructor)
        return buildEnumConstructor {
            source = baseConstructor.source
            moduleData = session.moduleData
            this.origin = origin
            attributes = baseConstructor.attributes.copy()
            annotations += baseConstructor.annotations
            isLocal = baseConstructor.isLocal
            deprecationsProvider = baseConstructor.deprecationsProvider
            status = baseConstructor.status.copy()
            symbol = newSymbol
            name = baseConstructor.name
            resolvePhase = origin.resolvePhaseForCopy
            dispatchReceiverType = newDispatchReceiverType
            typeParameters += newTypeParameters
            returnTypeRef = newReturnType
            valueParameters += buildCopiesForValueParameters(baseConstructor.valueParameters, newParameterTypes, origin, newSymbol)
        }.apply {
            attributes.deferredCallableCopyReturnType = deferredReturnTypeCalculation
            originalForSubstitutionOverrideAttr = baseConstructor
        }
    }

    fun createCopyForCfirProperty(
        newSymbol: CfirPropertySymbol,
        baseProperty: CfirProperty,
        session: CfirSession,
        origin: CfirDeclarationOrigin,
        newDispatchReceiverType: ConeSimpleCangJieType?,
        newTypeParameters: List<CfirTypeParameter>,
        newReturnType: CfirTypeRef,
        getter: CfirPropertyAccessor?,
        setter: CfirPropertyAccessor?,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType?,
    ): CfirProperty {
        checkStatusIsResolved(baseProperty)
        return buildProperty {
            source = baseProperty.source
            moduleData = session.moduleData
            this.origin = origin
            attributes = baseProperty.attributes.copy()
            annotations += baseProperty.annotations
            isLocal = baseProperty.isLocal
            deprecationsProvider = baseProperty.deprecationsProvider
            status = baseProperty.status.copy(isOverride = true)
            symbol = newSymbol
            name = baseProperty.name
            resolvePhase = origin.resolvePhaseForCopy
            dispatchReceiverType = newDispatchReceiverType
            typeParameters += newTypeParameters
            returnTypeRef = newReturnType
            this.getter = getter
            this.setter = setter
        }.apply {
            attributes.deferredCallableCopyReturnType = deferredReturnTypeCalculation
            originalForSubstitutionOverrideAttr = baseProperty
        }
    }

    fun createCopyForCfirField(
        newSymbol: CfirFieldVariableSymbol,
        baseField: CfirFieldVariable,
        session: CfirSession,
        origin: CfirDeclarationOrigin,
        newDispatchReceiverType: ConeSimpleCangJieType?,
        newTypeParameters: List<CfirTypeParameter>,
        newReturnType: CfirTypeRef,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType?,
    ): CfirFieldVariable {
        checkStatusIsResolved(baseField)
        return buildFieldVariable {
            source = baseField.source
            moduleData = session.moduleData
            this.origin = origin
            attributes = baseField.attributes.copy()
            annotations += baseField.annotations
            isLocal = baseField.isLocal
            deprecationsProvider = baseField.deprecationsProvider
            status = baseField.status.copy()
            symbol = newSymbol
            name = baseField.name
            resolvePhase = origin.resolvePhaseForCopy
            dispatchReceiverType = newDispatchReceiverType
            typeParameters += newTypeParameters
            returnTypeRef = newReturnType
            isVar = baseField.isVar
            isTypeImplicit = baseField.isTypeImplicit
        }.apply {
            attributes.deferredCallableCopyReturnType = deferredReturnTypeCalculation
            originalForSubstitutionOverrideAttr = baseField
        }
    }

    fun createCopyForCfirPropertyAccessor(
        newSymbol: CfirPropertyAccessorSymbol,
        propertySymbol: CfirPropertySymbol,
        baseAccessor: CfirPropertyAccessor,
        session: CfirSession,
        origin: CfirDeclarationOrigin,
        newDispatchReceiverType: ConeSimpleCangJieType?,
        newParameterTypes: List<CfirTypeRef>,
        newTypeParameters: List<CfirTypeParameter>,
        newReturnType: CfirTypeRef,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType?,
    ): CfirPropertyAccessor {
        checkStatusIsResolved(baseAccessor)
        return buildPropertyAccessor {
            source = baseAccessor.source
            moduleData = session.moduleData
            this.origin = origin
            attributes = baseAccessor.attributes.copy()
            annotations += baseAccessor.annotations
            isLocal = baseAccessor.isLocal
            deprecationsProvider = baseAccessor.deprecationsProvider
            status = baseAccessor.status.copy(isOverride = true)
            symbol = newSymbol
            this.propertySymbol = propertySymbol
            isGetter = baseAccessor.isGetter
            resolvePhase = origin.resolvePhaseForCopy
            dispatchReceiverType = newDispatchReceiverType
            typeParameters += newTypeParameters
            returnTypeRef = newReturnType
            valueParameters += buildCopiesForValueParameters(baseAccessor.valueParameters, newParameterTypes, origin, newSymbol)
            hasVariableLenArg = baseAccessor.hasVariableLenArg
        }.apply {
            attributes.deferredCallableCopyReturnType = deferredReturnTypeCalculation
            originalForSubstitutionOverrideAttr = baseAccessor
        }
    }

    /** 仓颉命名参数的默认值是签名事实；副本只保留存在标记与替换后的类型。 */
    private fun buildCopiesForValueParameters(
        originalParameters: List<CfirValueParameter>,
        newParameterTypes: List<CfirTypeRef>,
        origin: CfirDeclarationOrigin,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
    ): List<CfirValueParameter> {
        check(originalParameters.size == newParameterTypes.size)
        return originalParameters.zip(newParameterTypes) { original, newType ->
            buildValueParameterCopy(original) {
                this.origin = origin
                symbol = CfirValueParameterSymbol(original.symbol.callableId)
                this.containingDeclarationSymbol = containingDeclarationSymbol
                returnTypeRef = newType
                resolvePhase = origin.resolvePhaseForCopy
                defaultValue = original.defaultValue?.let {
                    buildExpressionStub { coneTypeOrNull = newType.coneTypeOrNull }
                }
            }.apply { originalForSubstitutionOverrideAttr = original }
        }
    }

    private fun checkStatusIsResolved(member: CfirCallableDeclaration) {
        checkWithAttachment(member.status is CfirResolvedDeclarationStatus, {
            "Status must be resolved before creating a signature copy"
        }) { withCfirEntry("declaration", member) }
    }

    private val CfirDeclarationOrigin.resolvePhaseForCopy: CfirResolvePhase
        get() = if (isLazyResolvable) CfirResolvePhase.STATUS else CfirResolvePhase.BODY_RESOLVE
}
