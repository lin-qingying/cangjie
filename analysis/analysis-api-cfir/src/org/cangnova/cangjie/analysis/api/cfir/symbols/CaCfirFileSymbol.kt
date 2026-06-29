package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.api.impl.base.util.lazyPub
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirFileSymbolPointer
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * CFIR file symbol 叶子实现。
 *
 * 对齐 Kotlin 的 `KaFirFileSymbol` 落位，把文件叶子独立到单文件中。
 */
internal class CaCfirFileSymbol private constructor(
    /**
     * 文件符号对应的源码 PSI。
     */
    override val backingPsi: CjFile?,
    /**
     * 延迟取得的底层 CFIR 文件符号。
     */
    override val lazyCfirSymbol: Lazy<CfirFileSymbol>,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
) : CaFileSymbol, CaCfirCjBasedSymbol<CjFile, CfirFileSymbol>, CaNamedSymbol {
    constructor(file: CjFile, session: CaCfirSession) : this(
        backingPsi = file,
        lazyCfirSymbol = lazyPub {
            file.getOrBuildCfirFile(session.resolutionFacade).symbol
        },
        analysisSession = session,
    )

    constructor(symbol: CfirFileSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.cfir.psi as? CjFile,
        lazyCfirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    /**
     * 文件符号所在的 use-site 模块。
     */
    override val containingModule
        get() = analysisSession.useSiteModule

    /**
     * 当前符号代表的仓颉文件。
     */
    override val file: CjFile
        get() = backingPsi
            ?: (cfirSymbol.cfir.psi as? CjFile)
            ?: error("Cannot resolve backing file for `${cfirSymbol}`")

    /**
     * 文件符号对应的 PSI。
     */
    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi }

    /**
     * 文件符号始终位于顶层。
     */
    override val location: CaSymbolLocation
        get() = CaSymbolLocation.TOP_LEVEL

    /**
     * 文件声明的包名。
     */
    override val packageFqName: FqName
        get() = file.packageFqName

    /**
     * 文件名对应的公开名称。
     */
    override val name: Name
        get() = Name.identifier(file.name)

    /**
     * 创建可恢复当前文件符号的 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaSymbol> = withValidityAssertion {
        CaCfirFileSymbolPointer(CaCfirFileSymbolCacheKey(file))
    }
}
