package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPackageSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPackageSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * CFIR package symbol 叶子实现。
 *
 * 对齐 Kotlin 的 `KaFirPackageSymbol` 落位，把 package 叶子从巨型模型文件中拆出。
 */
internal class CaCfirPackageSymbol(
    override val fqName: FqName,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirSymbolBase(containingModule, token), CaPackageSymbol, CaNamedSymbol {
    override val psi: PsiElement?
        get() = null

    override val origin: CaSymbolOrigin
        get() = CaSymbolOrigin.UNKNOWN

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.TOP_LEVEL

    override val name: Name
        get() = fqName.shortNameOrSpecial()

    override fun createPointer(): CaSymbolPointer<CaSymbol> = withValidityAssertion {
        CaCfirPackageSymbolPointer(CaCfirPackageSymbolCacheKey(fqName))
    }
}
