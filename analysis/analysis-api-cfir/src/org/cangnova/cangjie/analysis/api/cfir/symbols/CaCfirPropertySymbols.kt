package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.components.asCaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.components.renderAnnotations
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPropertyGetterSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPropertySetterSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.Name

/**
 * 属性及访问器叶子实现。
 *
 * 这里对齐 Kotlin FIR 对 property / getter / setter 分别建模的方式，
 * 让属性族不再和局部变量、值参数等完全不同的语义揉在一个文件里。
 */
internal class CaCfirPropertyGetterSymbolImpl(
    final override val backingSymbol: CfirCallableSymbol<*>,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaPropertyGetterSymbol(), CaCfirPropertyAccessorSymbolSupport<CfirCallableSymbol<*>> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { analysisSession.renderAnnotations(this).asCaAnnotationList(token) }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = accessorCallableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val location: CaSymbolLocation
        get() = locationImpl

    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        CaCfirPropertyGetterSymbolPointer(owningProperty.createPointer())
    }

    override val isStatic: Boolean
        get() = isStaticImpl

    override val isConst: Boolean
        get() = isConstImpl

    override val isMutating: Boolean
        get() = isMutatingImpl

    override val isOverride: Boolean
        get() = isOverrideImpl

    override val isOperator: Boolean
        get() = isOperatorImpl

    override val isUnsafe: Boolean
        get() = isUnsafeImpl

    override val isForeign: Boolean
        get() = isForeignImpl

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = typeParametersImpl

    override val valueParameters: List<CaValueParameterSymbol>
        get() = valueParametersImpl

    override val owningProperty: CaPropertySymbol
        get() = owningPropertyImpl

    override val isDefault: Boolean
        get() = isDefaultImpl

    override val isGetter: Boolean
        get() = true
}

internal class CaCfirPropertySetterSymbolImpl(
    final override val backingSymbol: CfirCallableSymbol<*>,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaPropertySetterSymbol(), CaCfirPropertyAccessorSymbolSupport<CfirCallableSymbol<*>> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { analysisSession.renderAnnotations(this).asCaAnnotationList(token) }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = accessorCallableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val location: CaSymbolLocation
        get() = locationImpl

    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        CaCfirPropertySetterSymbolPointer(owningProperty.createPointer())
    }

    override val isStatic: Boolean
        get() = isStaticImpl

    override val isConst: Boolean
        get() = isConstImpl

    override val isMutating: Boolean
        get() = isMutatingImpl

    override val isOverride: Boolean
        get() = isOverrideImpl

    override val isOperator: Boolean
        get() = isOperatorImpl

    override val isUnsafe: Boolean
        get() = isUnsafeImpl

    override val isForeign: Boolean
        get() = isForeignImpl

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = typeParametersImpl

    override val valueParameters: List<CaValueParameterSymbol>
        get() = valueParametersImpl

    override val owningProperty: CaPropertySymbol
        get() = owningPropertyImpl

    override val isDefault: Boolean
        get() = isDefaultImpl

    override val isGetter: Boolean
        get() = false

    override val parameter: CaValueParameterSymbol
        get() = valueParameters.single()
}

internal class CaCfirPropertySymbolImpl(
    final override val backingSymbol: CfirPropertySymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaPropertySymbol(),
    CaCfirVariableSymbolSupport<CfirPropertySymbol>,
    CaTypeParameterOwnerSymbol,
    CaDeclarationContainerSymbol {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { analysisSession.renderAnnotations(this).asCaAnnotationList(token) }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = callableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val location: CaSymbolLocation
        get() = locationImpl

    override fun createPointer(): CaSymbolPointer<CaPropertySymbol> = withValidityAssertion {
        createStableCallablePointer(CaPropertySymbol::class.java)
    }

    override val isLet: Boolean
        get() = true

    override val isStatic: Boolean
        get() = status?.isStatic == true

    override val isConst: Boolean
        get() = status?.isConst == true

    override val isMutating: Boolean
        get() = status?.isMut == true

    override val isOverride: Boolean
        get() = status?.isOverride == true

    override val isUnsafe: Boolean
        get() = status?.isUnsafe == true

    override val isForeign: Boolean
        get() = status?.isForeign == true

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = (backingSymbol.cfir as? CfirProperty)
            ?.typeParameters
            ?.map { analysisSession.createTypeParameterSymbol(it.symbol) }
            .orEmpty()

    override val getter: CaPropertyGetterSymbol?
        get() = (backingSymbol.cfir as? CfirProperty)?.getter?.symbol?.let { getterSymbol ->
            analysisSession.createPropertyAccessorSymbol(getterSymbol, this, CaCfirPropertyAccessorKind.GETTER) as CaPropertyGetterSymbol
        }

    override val setter: CaPropertySetterSymbol?
        get() = (backingSymbol.cfir as? CfirProperty)?.setter?.symbol?.let { setterSymbol ->
            analysisSession.createPropertyAccessorSymbol(setterSymbol, this, CaCfirPropertyAccessorKind.SETTER) as CaPropertySetterSymbol
        }

    override val name: Name
        get() = nameImpl
}
