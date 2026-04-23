package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFinalizerSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * 局部或生命周期函数叶子实现。
 *
 * 匿名函数、析构器这类函数虽然都属于 `CaFunctionSymbol` 族，
 * 但它们的公开语义和 pointer/宿主恢复策略不同，单独落位更接近 Kotlin FIR 的叶子组织方式。
 */
internal class CaCfirAnonymousFunctionSymbol(
    final override val backingSymbol: CfirAnonymousFunctionSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaAnonymousFunctionSymbol(), CaCfirFunctionSymbolSupport<CfirAnonymousFunctionSymbol> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            CaCfirAnnotationListForDeclaration.create(backingSymbol, builder)
        }

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val location: CaSymbolLocation
        get() = locationImpl

    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        createStableCallablePointer(CaFunctionSymbol::class.java)
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

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    override val isVisibilityExplicit: Boolean
        get() = false

    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    override val isModalityExplicit: Boolean
        get() = false
}

internal class CaCfirFinalizerSymbol(
    final override val backingSymbol: CfirFinalizerSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaFinalizerSymbol(), CaCfirFunctionSymbolSupport<CfirFinalizerSymbol> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            CaCfirAnnotationListForDeclaration.create(backingSymbol, builder)
        }
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = callableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val location: CaSymbolLocation
        get() = locationImpl

    override fun createPointer(): CaSymbolPointer<CaFunctionSymbol> = withValidityAssertion {
        createStableCallablePointer(CaFunctionSymbol::class.java)
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

    override val containingClassId: ClassId?
        get() = (psi as? org.cangnova.cangjie.psi.CjFinalizer)?.getContainingTypeStatement()?.getClassId()
}
