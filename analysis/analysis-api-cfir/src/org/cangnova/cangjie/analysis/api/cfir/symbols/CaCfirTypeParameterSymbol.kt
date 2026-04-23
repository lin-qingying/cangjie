package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.location
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirSourceTypeParameterSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirTypeParameterSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaAnnotatedSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.Name

/**
 * type parameter 叶子实现。
 *
 * 对齐 Kotlin 的 `KaFirTypeParameterSymbol` 落位，将类型参数叶子从巨型文件中拆出。
 */
internal class CaCfirTypeParameterSymbol private constructor(
    override val backingPsi: org.cangnova.cangjie.psi.CjTypeParameter?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirTypeParameterSymbol>,
    internal val stableParameterIndex: Int? = null,
) : CaTypeParameterSymbol,
    CaNamedSymbol,
    CaCfirCjBasedSymbol<org.cangnova.cangjie.psi.CjTypeParameter, CfirTypeParameterSymbol>,
    CaCfirBackedSymbol<CfirTypeParameterSymbol> {
    override val cfirSymbol: CfirTypeParameterSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    constructor(declaration: org.cangnova.cangjie.psi.CjTypeParameter, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(symbol: CfirTypeParameterSymbol, session: CaCfirSession, stableParameterIndex: Int? = null) : this(
        backingPsi = symbol.backingPsiIfApplicable as? org.cangnova.cangjie.psi.CjTypeParameter,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
        stableParameterIndex = stableParameterIndex,
    )

    override val backingSymbol: CfirTypeParameterSymbol
        get() = cfirSymbol

    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    override val psi
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: backingSymbol.name }

    override val upperBounds: List<CaType>
        get() = withValidityAssertion { backingSymbol.cfir.bounds.mapNotNull { bound -> bound.coneTypeOrNull?.let(builder.typeBuilder::buildType) } }

    override val containingDeclaration: CaSymbol?
        get() = withValidityAssertion { analysisSession.findContainingDeclarationSymbol(psi) }

    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { CaSymbolVisibility.LOCAL }

    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion { false }

    override val modality: CaSymbolModality?
        get() = withValidityAssertion { CaSymbolModality.FINAL }

    override val isModalityExplicit: Boolean
        get() = withValidityAssertion { false }

    override val location: CaSymbolLocation
        get() = withValidityAssertion { backingPsi?.location ?: CaSymbolLocation.LOCAL }

    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        val owner = containingDeclaration
        if (owner == null) {
            val sourcePsi = psi ?: error("Source-only type parameter `${name}` is missing PSI")
            return@withValidityAssertion CaCfirSourceTypeParameterSymbolPointer(sourcePsi)
        }

        val parameterIndex = stableParameterIndex
            ?: error("Type parameter `${name}` is missing a stable owner index")
        val ownerPointer: CaSymbolPointer<CaSymbol> = owner.createPointer()
        CaCfirTypeParameterSymbolPointer(ownerPointer, name, parameterIndex)
    }
}
