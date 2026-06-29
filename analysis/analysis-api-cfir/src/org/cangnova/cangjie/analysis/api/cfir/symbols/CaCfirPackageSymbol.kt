package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPackageSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPackageSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
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
    /**
     * 包的完全限定名。
     */
    override val fqName: FqName,
    /**
     * 包符号所在模块。
     */
    override val containingModule: CaModule,
    /**
     * 包符号的生命周期 token。
     */
    override val token: CaLifetimeToken,
) : CaPackageSymbol, CaNamedSymbol, CaLifetimeOwner {
    /**
     * 包符号没有单一 PSI 声明。
     */
    override val psi: PsiElement?
        get() = null

    /**
     * 包符号来源不对应具体声明 origin。
     */
    override val origin: CaSymbolOrigin
        get() = CaSymbolOrigin.UNKNOWN

    /**
     * 包符号属于顶层符号。
     */
    override val location: CaSymbolLocation
        get() = CaSymbolLocation.TOP_LEVEL

    /**
     * 包名最后一段作为公开短名。
     */
    override val name: Name
        get() = fqName.shortNameOrSpecial()

    /**
     * 创建可按包名恢复当前包符号的 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaSymbol> = withValidityAssertion {
        CaCfirPackageSymbolPointer(CaCfirPackageSymbolCacheKey(fqName))
    }
}
