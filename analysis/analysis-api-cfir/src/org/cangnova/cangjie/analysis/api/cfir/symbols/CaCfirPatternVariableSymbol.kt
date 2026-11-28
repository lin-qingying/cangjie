package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPatternVariableSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.*
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.psi.CjPatternVariable

/** 模式声明只暴露声明容器身份；每个 active binder 由 entries 独立发布。 */
internal class CaCfirPatternVariableSymbol private constructor(
    override val backingPsi: CjPatternVariable?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirPatternVariableSymbol>,
) : CaPatternVariableSymbol(), CaCfirCjBasedSymbol<CjPatternVariable, CfirPatternVariableSymbol> {
    constructor(declaration: CjPatternVariable, session: CaCfirSession) : this(
        declaration, session, lazyCfirSymbol(declaration, session),
    )

    constructor(symbol: CfirPatternVariableSymbol, session: CaCfirSession) : this(
        symbol.backingPsiIfApplicable as? CjPatternVariable, session, lazyOf(symbol),
    )

    override val cfirSymbol: CfirPatternVariableSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    override val containingModule: CaModule
        get() = withValidityAssertion { cfirSymbol.llCfirModuleData.caModule }

    override val psi
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    override val annotations
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val entries: List<CaPatternBindingSymbol>
        get() = withValidityAssertion {
            cfirSymbol.cfir.pattern.bindingVariables().map {
                builder.variableBuilder.buildVariableSymbol(it.symbol) as CaPatternBindingSymbol
            }
        }

    override val isVar: Boolean
        get() = withValidityAssertion { cfirSymbol.cfir.isVar }

    override val isConst: Boolean
        get() = withValidityAssertion { cfirSymbol.symbolDeclarationStatus.isConst }

    override val location: CaSymbolLocation
        get() = withValidityAssertion { if (cfirSymbol.cfir.isLocal) CaSymbolLocation.LOCAL else CaSymbolLocation.TOP_LEVEL }

    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { cfirSymbol.symbolDeclarationStatus.visibility.asPublicVisibility() }

    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion { cfirSymbol.symbolDeclarationStatus.isVisibilityExplicit }

    override val modality: CaSymbolModality?
        get() = withValidityAssertion { cfirSymbol.symbolDeclarationStatus.modality?.asPublicModality() }

    override val isModalityExplicit: Boolean
        get() = withValidityAssertion { cfirSymbol.symbolDeclarationStatus.isModalityExplicit }

    override fun createPointer(): CaSymbolPointer<CaPatternVariableSymbol> = withValidityAssertion {
        CaCfirPatternVariableSymbolPointer(checkNotNull(psi) { "Pattern declaration is missing its canonical PSI" })
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
