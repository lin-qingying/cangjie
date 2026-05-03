package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.caSymbolModalityByModifiers
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.getCallableSymbolLocation
import org.cangnova.cangjie.analysis.api.cfir.isOpenFromInterface
import org.cangnova.cangjie.analysis.api.cfir.location
import org.cangnova.cangjie.analysis.api.cfir.psiBasedDefaultCaModality
import org.cangnova.cangjie.analysis.api.cfir.psiBasedVisibility
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPropertyGetterSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPropertySetterSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.visibilityByModifiers
import org.cangnova.cangjie.analysis.api.impl.base.util.callableId
import org.cangnova.cangjie.analysis.api.impl.base.util.callableIdForName
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjProperty

/**
 * 属性及访问器叶子实现。
 *
 * 这里对齐 Kotlin FIR 对 property / getter / setter 分别建模的方式，
 * 让属性族不再和局部变量、值参数等完全不同的语义揉在一个文件里。
 */
internal class CaCfirPropertyGetterSymbol(
    override val owningCaProperty: CaPropertySymbol,
) : CaPropertyGetterSymbol(), CaCfirBasePropertyGetterSymbol {
    override val annotations: CaAnnotationList
        get() = annotationsImpl

    override val psi
        get() = psiImpl

    override val origin
        get() = originImpl

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = callableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val location: CaSymbolLocation
        get() = locationImpl

    override val visibility: CaSymbolVisibility
        get() = visibilityImpl

    override val isVisibilityExplicit: Boolean
        get() = isVisibilityExplicitImpl

    override val modality: CaSymbolModality?
        get() = modalityImpl

    override val isModalityExplicit: Boolean
        get() = isModalityExplicitImpl

    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = createGetterPointer()

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

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)

    override fun hashCode(): Int = psiOrSymbolHashCode()
}

internal class CaCfirPropertySetterSymbol(
    override val owningCaProperty: CaPropertySymbol,
) : CaPropertySetterSymbol(), CaCfirBasePropertySetterSymbol {
    override val annotations: CaAnnotationList
        get() = annotationsImpl

    override val psi
        get() = psiImpl

    override val origin
        get() = originImpl

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = callableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val location: CaSymbolLocation
        get() = locationImpl

    override val visibility: CaSymbolVisibility
        get() = visibilityImpl

    override val isVisibilityExplicit: Boolean
        get() = isVisibilityExplicitImpl

    override val modality: CaSymbolModality?
        get() = modalityImpl

    override val isModalityExplicit: Boolean
        get() = isModalityExplicitImpl

    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = createSetterPointer()

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
        get() = parameterImpl

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)

    override fun hashCode(): Int = psiOrSymbolHashCode()
}

internal class CaCfirPropertySymbol private constructor(
    override val backingPsi: CjProperty?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirPropertySymbol>,
) : CaPropertySymbol(),
    CaCfirCjBasedSymbol<CjProperty, CfirPropertySymbol>,
    CaTypeParameterOwnerSymbol,
    CaDeclarationContainerSymbol {
    override val cfirSymbol: CfirPropertySymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    constructor(declaration: CjProperty, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(symbol: CfirPropertySymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjProperty,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val psi
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = withValidityAssertion { if (backingPsi != null) backingPsi.callableIdForName(backingPsi.nameAsSafeName) else cfirSymbol.getCallableId() }

    override val receiverType: CaType?
        get() = withValidityAssertion { analysisSession.getExplicitCallableReceiverType(cfirSymbol, backingPsi, builder) }

    override val returnType: CaType
        get() = withValidityAssertion { cfirSymbol.returnType(builder) }

    override val location: CaSymbolLocation
        get() = withValidityAssertion {
            analysisSession.getCallableSymbolLocation(cfirSymbol, backingPsi)
        }

    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion {
            backingPsi?.psiBasedVisibility(::isOverride)?.asPublicVisibility()
                ?: status?.visibility?.asPublicVisibility()
                ?: CaSymbolVisibility.PUBLIC
        }

    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion {
            backingPsi?.let { it.visibilityByModifiers != null }
                ?: (status?.isVisibilityExplicit == true)
        }

    override val modality: CaSymbolModality?
        get() = withValidityAssertion {
            val psiBasedModality = backingPsi?.run {
                val modalityByModifiers = caSymbolModalityByModifiers
                when {
                    modalityByModifiers != null -> when {
                        modalityByModifiers.isOpenFromInterface && !hasBody() -> CaSymbolModality.ABSTRACT
                        else -> modalityByModifiers
                    }

                    hasModifier(CjTokens.CONST_KEYWORD) -> CaSymbolModality.FINAL
                    else -> psiBasedDefaultCaModality(::isOverride)
                }
            }

            psiBasedModality ?: status?.modality?.asPublicModality()
        }

    override val isModalityExplicit: Boolean
        get() = withValidityAssertion {
            backingPsi?.let { it.caSymbolModalityByModifiers != null }
                ?: (status?.isModalityExplicit == true)
        }

    override fun createPointer(): CaSymbolPointer<CaPropertySymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<CaPropertySymbol> { psi ->
            (psi as? CjProperty)?.symbol
        } ?: error("Property symbol `${name}` cannot create a stable pointer")
    }

    override val isLet: Boolean
        get() = withValidityAssertion { backingPsi?.isVar != true }

    override val isStatic: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.STATIC_KEYWORD) ?: (status?.isStatic == true) }

    override val isConst: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.CONST_KEYWORD) ?: (status?.isConst == true) }

    override val isMutating: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.MUT_KEYWORD) ?: (status?.isMut == true) }

    override val isOverride: Boolean
        get() = withValidityAssertion { isOverrideWithWorkaround }

    override val isUnsafe: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.UNSAFE_KEYWORD) ?: (status?.isUnsafe == true) }

    override val isForeign: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(CjTokens.FOREIGN_KEYWORD) ?: (status?.isForeign == true) }

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = withValidityAssertion {
            createCaTypeParameters() ?: (cfirSymbol.cfir as? CfirProperty)
                ?.typeParameters
                ?.map { builder.classifierBuilder.buildTypeParameterSymbol(it.symbol) }
                .orEmpty()
        }

    override val getter: CaPropertyGetterSymbol?
        get() = (cfirSymbol.cfir as? CfirProperty)?.getter?.symbol?.let { getterSymbol ->
            builder.functionBuilder.buildPropertyAccessorSymbol(getterSymbol, this, CaCfirPropertyAccessorKind.GETTER) as CaPropertyGetterSymbol
        }

    override val setter: CaPropertySetterSymbol?
        get() = (cfirSymbol.cfir as? CfirProperty)?.setter?.symbol?.let { setterSymbol ->
            builder.functionBuilder.buildPropertyAccessorSymbol(setterSymbol, this, CaCfirPropertyAccessorKind.SETTER) as CaPropertySetterSymbol
        }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }
}
