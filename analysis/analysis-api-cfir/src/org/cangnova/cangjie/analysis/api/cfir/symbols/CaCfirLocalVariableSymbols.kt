package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.name.Name

/**
 * 局部变量族叶子实现。
 *
 * 把普通局部变量、模式变量、模式绑定变量的本地可见性语义集中在同一簇，
 * 避免与属性或值参数混在一起。
 */
internal open class CaCfirLocalVariableSymbol(
    final override val backingSymbol: CfirCallableSymbol<*>,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol(),
    CaCfirLocalVariableSymbolSupport<CfirCallableSymbol<*>> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { CaCfirAnnotationListForDeclaration.create(backingSymbol, builder) }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = localCallableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val visibility: CaSymbolVisibility
        get() = localVisibilityImpl

    override val isVisibilityExplicit: Boolean
        get() = isVisibilityExplicitImpl

    override val modality: CaSymbolModality?
        get() = modalityImpl

    override val isModalityExplicit: Boolean
        get() = isModalityExplicitImpl

    override val location: CaSymbolLocation
        get() = localLocationImpl

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        createStableCallablePointer(CaCallableSymbol::class.java)
    }

    override val isLet: Boolean
        get() = when (val currentDeclaration = backingSymbol.cfir) {
            is CfirPatternVariable -> !currentDeclaration.isVar
            is CfirPatternBindingVariable -> !currentDeclaration.isVar
            else -> true
        }

    override val name: Name
        get() = nameImpl
}

internal class CaCfirPatternVariableSymbol(
    final override val backingSymbol: CfirPatternVariableSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaPatternVariableSymbol(), CaCfirLocalVariableSymbolSupport<CfirPatternVariableSymbol> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { CaCfirAnnotationListForDeclaration.create(backingSymbol, builder) }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = localCallableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val visibility: CaSymbolVisibility
        get() = localVisibilityImpl

    override val isVisibilityExplicit: Boolean
        get() = isVisibilityExplicitImpl

    override val modality: CaSymbolModality?
        get() = modalityImpl

    override val isModalityExplicit: Boolean
        get() = isModalityExplicitImpl

    override val location: CaSymbolLocation
        get() = localLocationImpl

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        createStableCallablePointer(CaCallableSymbol::class.java)
    }

    override val isLet: Boolean
        get() = !(backingSymbol.cfir as CfirPatternVariable).isVar

    override val name: Name
        get() = nameImpl
}

internal class CaCfirPatternBindingSymbol(
    final override val backingSymbol: CfirPatternBindingSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaPatternBindingSymbol(), CaCfirLocalVariableSymbolSupport<CfirPatternBindingSymbol> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { CaCfirAnnotationListForDeclaration.create(backingSymbol, builder) }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = localCallableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val visibility: CaSymbolVisibility
        get() = localVisibilityImpl

    override val isVisibilityExplicit: Boolean
        get() = isVisibilityExplicitImpl

    override val modality: CaSymbolModality?
        get() = modalityImpl

    override val isModalityExplicit: Boolean
        get() = isModalityExplicitImpl

    override val location: CaSymbolLocation
        get() = localLocationImpl

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        createStableCallablePointer(CaCallableSymbol::class.java)
    }

    override val isLet: Boolean
        get() = !(backingSymbol.cfir as CfirPatternBindingVariable).isVar

    override val name: Name
        get() = nameImpl
}
