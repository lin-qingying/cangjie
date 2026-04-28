package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPatternBindingSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPatternVariableSymbolPointer
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
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjPatternVariable
import com.intellij.psi.PsiElement

/**
 * 局部变量族叶子实现。
 *
 * 把普通局部变量、模式变量、模式绑定变量的本地可见性语义集中在同一簇，
 * 避免与属性或值参数混在一起。
 */
internal open class CaCfirLocalVariableSymbol(
    final override val cfirSymbol: CfirCallableSymbol<*>,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol(),
    CaCfirSymbol<CfirCallableSymbol<*>> {
    override val psi: PsiElement?
        get() = null

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { CaCfirAnnotationListForDeclaration.create(cfirSymbol, builder) }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    override val receiverType: CaType?
        get() = (cfirSymbol.cfir as? CfirCallableDeclaration)?.dispatchReceiverType?.let(builder.typeBuilder::buildType)

    override val returnType: CaType
        get() = cfirSymbol.returnType(builder)

    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    override val isVisibilityExplicit: Boolean
        get() = false

    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    override val isModalityExplicit: Boolean
        get() = false

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.LOCAL

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        error("Local variable symbol cannot create a stable pointer without source PSI")
    }

    override val isLet: Boolean
        get() = when (val currentDeclaration = cfirSymbol.cfir) {
            is CfirPatternVariable -> !currentDeclaration.isVar
            is CfirPatternBindingVariable -> !currentDeclaration.isVar
            else -> true
        }

    override val name: Name
        get() = cfirSymbol.name
}

internal class CaCfirPatternVariableSymbol private constructor(
    override val backingPsi: CjPatternVariable?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirPatternVariableSymbol>,
) : CaPatternVariableSymbol(),
    CaCfirCjBasedSymbol<CjPatternVariable, CfirPatternVariableSymbol> {
    constructor(declaration: CjPatternVariable, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(symbol: CfirPatternVariableSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjPatternVariable,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    override val cfirSymbol: CfirPatternVariableSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    override val psi
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    override val receiverType: CaType?
        get() = (cfirSymbol.cfir as? CfirCallableDeclaration)?.dispatchReceiverType?.let(builder.typeBuilder::buildType)

    override val returnType: CaType
        get() = cfirSymbol.returnType(builder)

    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    override val isVisibilityExplicit: Boolean
        get() = false

    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    override val isModalityExplicit: Boolean
        get() = false

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.LOCAL

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        val sourcePsi = psi ?: error("Pattern variable symbol is missing PSI")
        @Suppress("UNCHECKED_CAST")
        CaCfirPatternVariableSymbolPointer(sourcePsi) as CaSymbolPointer<CaCallableSymbol>
    }

    override val isLet: Boolean
        get() = withValidityAssertion { backingPsi?.isVar != true }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.pattern?.let { (it as? CjBindingPattern)?.nameAsSafeName } ?: cfirSymbol.name }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

internal class CaCfirPatternBindingSymbol private constructor(
    override val backingPsi: CjBindingPattern?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirPatternBindingSymbol>,
) : CaPatternBindingSymbol(),
    CaCfirCjBasedSymbol<CjBindingPattern, CfirPatternBindingSymbol> {
    constructor(declaration: CjBindingPattern, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol<CfirPatternBindingVariable, CfirPatternBindingSymbol>(
            declaration,
            session,
        ) { variable -> variable.symbol },
    )

    constructor(symbol: CfirPatternBindingSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjBindingPattern,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    override val cfirSymbol: CfirPatternBindingSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    override val psi
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { CaCfirAnnotationListForDeclaration.create(cfirSymbol, builder) }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    override val receiverType: CaType?
        get() = (cfirSymbol.cfir as? CfirCallableDeclaration)?.dispatchReceiverType?.let(builder.typeBuilder::buildType)

    override val returnType: CaType
        get() = cfirSymbol.returnType(builder)

    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    override val isVisibilityExplicit: Boolean
        get() = false

    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    override val isModalityExplicit: Boolean
        get() = false

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.LOCAL

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        val sourcePsi = psi ?: error("Pattern binding symbol is missing PSI")
        @Suppress("UNCHECKED_CAST")
        CaCfirPatternBindingSymbolPointer(sourcePsi) as CaSymbolPointer<CaCallableSymbol>
    }

    override val isLet: Boolean
        get() = withValidityAssertion { backingPsi?.variable?.isVar != true }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
