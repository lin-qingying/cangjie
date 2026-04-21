package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirFileSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirFileSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * CFIR file symbol 叶子实现。
 *
 * 对齐 Kotlin 的 `KaFirFileSymbol` 落位，把文件叶子独立到单文件中。
 */
internal class CaCfirFileSymbol(
    override val backingSymbol: CfirFileSymbol,
    override val file: CjFile,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirSymbolBase(containingModule, token), CaFileSymbol, CaNamedSymbol, CaCfirBackedSymbol<CfirFileSymbol> {
    override val psi: PsiElement?
        get() = withValidityAssertion { file }

    override val origin: CaSymbolOrigin
        get() = backingSymbol.origin.asPublicOrigin()

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.TOP_LEVEL

    override val packageFqName: FqName
        get() = file.packageFqName

    override val name: Name
        get() = Name.identifier(file.name)

    override fun createPointer(): CaSymbolPointer<CaSymbol> = withValidityAssertion {
        CaCfirFileSymbolPointer(CaCfirFileSymbolCacheKey(file))
    }
}
