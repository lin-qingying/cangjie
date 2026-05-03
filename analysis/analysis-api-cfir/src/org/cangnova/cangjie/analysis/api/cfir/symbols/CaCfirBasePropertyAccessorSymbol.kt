package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
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
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjPropertyAccessor

/**
 * 对齐 Kotlin `KaFirBasePropertyAccessorSymbol` 的访问器共性层。
 *
 * 仓颉 getter/setter 都依附于 property 本体，因此这里统一承接：
 * 1. property -> accessor 的 PSI / CFIR 映射；
 * 2. 访问器公共状态、可见性、返回类型与参数恢复；
 * 3. setter 参数优先使用 accessor PSI，对齐 Kotlin 的 parameterImpl 策略。
 */
internal sealed interface CaCfirBasePropertyAccessorSymbol :
    CaCfirCjBasedSymbol<CjPropertyAccessor, CfirPropertyAccessorSymbol> {
    val owningCaProperty: CaPropertySymbol

    private val owningCfirProperty: CaCfirPropertySymbol
        get() = owningCaProperty as? CaCfirPropertySymbol
            ?: error("Property accessor owner must be CaCfirPropertySymbol: ${owningCaProperty::class.simpleName}")

    private val isGetterAccessor: Boolean
        get() = this is CaCfirBasePropertyGetterSymbol

    override val lazyCfirSymbol: Lazy<CfirPropertyAccessorSymbol>
        get() = throw UnsupportedOperationException()

    override val backingPsi: CjPropertyAccessor?
        get() {
            val property = owningCfirProperty.backingPsi as? CjProperty ?: return null
            return if (isGetterAccessor) property.getter else property.setter
        }

    override val cfirSymbol: CfirPropertyAccessorSymbol
        get() {
            val propertySymbol = owningCfirProperty.cfirSymbol
            return if (isGetterAccessor) {
                propertySymbol.getterSymbol
            } else {
                propertySymbol.setterSymbol
            } ?: error("${if (isGetterAccessor) "Getter" else "Setter"} accessor is missing")
        }

    override val analysisSession: CaCfirSession
        get() = owningCfirProperty.analysisSession

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    private val status
        get() = (cfirSymbol.cfir as? CfirMemberDeclaration)?.status

    val annotationsImpl: CaAnnotationList
        get() = withValidityAssertion { CaCfirAnnotationListForDeclaration.create(cfirSymbol, builder) }

    val psiImpl: PsiElement?
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    val originImpl
        get() = withValidityAssertion { owningCaProperty.origin }

    val callableIdImpl: org.cangnova.cangjie.name.CallableId?
        get() = withValidityAssertion { null }

    val receiverTypeImpl: CaType?
        get() = withValidityAssertion { analysisSession.getExplicitCallableReceiverType(cfirSymbol, backingPsi, builder) }

    val returnTypeImpl: CaType
        get() = withValidityAssertion { cfirSymbol.returnType(builder) }

    val locationImpl: CaSymbolLocation
        get() = CaSymbolLocation.PROPERTY

    val visibilityImpl: CaSymbolVisibility
        get() = status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC

    val isVisibilityExplicitImpl: Boolean
        get() = status?.isVisibilityExplicit == true

    val modalityImpl: CaSymbolModality?
        get() = status?.modality?.asPublicModality()

    val isModalityExplicitImpl: Boolean
        get() = status?.isModalityExplicit == true

    val isStaticImpl: Boolean
        get() = status?.isStatic == true

    val isConstImpl: Boolean
        get() = status?.isConst == true

    val isMutatingImpl: Boolean
        get() = status?.isMut == true

    val isOverrideImpl: Boolean
        get() = status?.isOverride == true

    val isOperatorImpl: Boolean
        get() = status?.isOperator == true

    val isUnsafeImpl: Boolean
        get() = status?.isUnsafe == true

    val isForeignImpl: Boolean
        get() = status?.isForeign == true

    val typeParametersImpl: List<CaTypeParameterSymbol>
        get() = (cfirSymbol.cfir as? CfirCallableDeclaration)
            ?.typeParameters
            ?.map { typeParameter -> builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol) }
            .orEmpty()

    val valueParametersImpl: List<CaValueParameterSymbol>
        get() = withValidityAssertion {
            with(analysisSession) {
                backingPsi?.valueParameters?.map { parameter -> parameter.symbol as CaValueParameterSymbol }
            } ?: (cfirSymbol.cfir as? CfirFunction)
                ?.valueParameters
                ?.mapIndexed { parameterIndex, parameter ->
                    builder.variableBuilder.buildOwnedValueParameterSymbol(this@CaCfirBasePropertyAccessorSymbol as org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol, parameter, parameterIndex)
                }
                .orEmpty()
        }

    val owningPropertyImpl: CaPropertySymbol
        get() = owningCaProperty

    val isDefaultImpl: Boolean
        get() = false
}

internal interface CaCfirBasePropertyGetterSymbol : CaCfirBasePropertyAccessorSymbol {
    fun createGetterPointer(): org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<CaPropertyGetterSymbol> { psi ->
            (psi as? CjPropertyAccessor)?.symbol as? CaPropertyGetterSymbol
        } ?: org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPropertyGetterSymbolPointer(owningCaProperty.createPointer())
    }
}

internal interface CaCfirBasePropertySetterSymbol : CaCfirBasePropertyAccessorSymbol {
    val parameterImpl: CaValueParameterSymbol
        get() = withValidityAssertion {
            with(analysisSession) {
                backingPsi?.valueParameters?.firstOrNull()?.symbol as? CaValueParameterSymbol
            } ?: valueParametersImpl.single()
        }

    fun createSetterPointer(): org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<CaPropertySetterSymbol> { psi ->
            (psi as? CjPropertyAccessor)?.symbol as? CaPropertySetterSymbol
        } ?: org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPropertySetterSymbolPointer(owningCaProperty.createPointer())
    }
}
