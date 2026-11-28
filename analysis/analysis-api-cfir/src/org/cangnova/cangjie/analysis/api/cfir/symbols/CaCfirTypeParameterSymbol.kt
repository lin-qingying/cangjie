package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.location
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.name.Name

/**
 * type parameter 叶子实现。
 *
 * 对齐 Kotlin 的 `KaFirTypeParameterSymbol` 落位，将类型参数叶子从巨型文件中拆出。
 */
internal class CaCfirTypeParameterSymbol private constructor(
    /**
     * 类型参数对应的源码 PSI。
     */
    override val backingPsi: org.cangnova.cangjie.psi.CjTypeParameter?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR 类型参数符号。
     */
    override val lazyCfirSymbol: Lazy<CfirTypeParameterSymbol>,
) : CaCfirTypeParameterSymbolBase<org.cangnova.cangjie.psi.CjTypeParameter>(),
    CaCfirCjBasedSymbol<org.cangnova.cangjie.psi.CjTypeParameter, CfirTypeParameterSymbol> {
    /**
     * 类型参数底层 CFIR 符号。
     */
    override val cfirSymbol: CfirTypeParameterSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    constructor(declaration: org.cangnova.cangjie.psi.CjTypeParameter, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(symbol: CfirTypeParameterSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? org.cangnova.cangjie.psi.CjTypeParameter,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    /**
     * 类型参数对应的 PSI。
     */
    override val psi
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    /**
     * 类型参数公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    /**
     * 类型参数名称。
     */
    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }

    /**
     * 类型参数可见性固定为 local。
     */
    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { CaSymbolVisibility.LOCAL }

    /**
     * 类型参数不显式声明可见性。
     */
    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion { false }

    /**
     * 类型参数 modality 固定为 final。
     */
    override val modality: CaSymbolModality?
        get() = withValidityAssertion { CaSymbolModality.FINAL }

    /**
     * 类型参数不显式声明 modality。
     */
    override val isModalityExplicit: Boolean
        get() = withValidityAssertion { false }

    /**
     * 类型参数在公开 API 中的位置。
     */
    override val location: CaSymbolLocation
        get() = withValidityAssertion {
            when {
                backingPsi != null -> backingPsi.location
                cfirSymbol.containingDeclarationSymbol is CfirClassLikeSymbol<*> -> CaSymbolLocation.CLASS
                else -> CaSymbolLocation.LOCAL
            }
        }

}
