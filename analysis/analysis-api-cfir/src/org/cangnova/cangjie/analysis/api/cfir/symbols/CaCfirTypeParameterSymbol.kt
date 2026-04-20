package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirSourceTypeParameterSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirTypeParameterSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaAnnotatedSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
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
internal class CaCfirTypeParameterSymbolImpl(
    backingSymbol: CfirTypeParameterSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
    internal val stableParameterIndex: Int? = null,
) : CaCfirClassifierBackedSymbol<CfirTypeParameterSymbol>(backingSymbol, analysisSession, containingModule, token),
    CaTypeParameterSymbol,
    CaNamedSymbol {
    override val name: Name
        get() = backingSymbol.name

    override val upperBounds: List<CaType>
        get() = backingSymbol.cfir.bounds.mapNotNull { bound -> bound.coneTypeOrNull?.asCaType(analysisSession) }

    override val origin: CaSymbolOrigin
        get() = backingSymbol.origin.asPublicOrigin()

    override val containingDeclaration: CaSymbol?
        get() = analysisSession.findContainingDeclarationSymbol(psi)

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
