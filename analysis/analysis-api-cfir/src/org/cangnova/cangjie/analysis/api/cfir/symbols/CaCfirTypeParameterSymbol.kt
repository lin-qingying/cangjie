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
    /**
     * 类型参数在 owner 类型参数列表中的稳定下标。
     */
    internal val stableParameterIndex: Int? = null,
) : CaTypeParameterSymbol,
    CaNamedSymbol,
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

    constructor(symbol: CfirTypeParameterSymbol, session: CaCfirSession, stableParameterIndex: Int? = null) : this(
        backingPsi = symbol.backingPsiIfApplicable as? org.cangnova.cangjie.psi.CjTypeParameter,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
        stableParameterIndex = stableParameterIndex,
    )

    /**
     * 类型参数所在的 use-site 模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

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
     * 类型参数上界类型列表。
     */
    override val upperBounds: List<CaType>
        get() = withValidityAssertion { cfirSymbol.resolvedBounds.map(builder.typeBuilder::buildType) }

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

    /**
     * 创建基于 owner pointer 或源码 PSI 的类型参数 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        val owner = builder.buildSymbol(cfirSymbol.containingDeclarationSymbol)
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
