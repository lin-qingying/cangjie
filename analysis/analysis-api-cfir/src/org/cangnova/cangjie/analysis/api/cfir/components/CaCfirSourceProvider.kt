package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirBackedSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendSymbol
import org.cangnova.cangjie.analysis.api.components.CaSourceProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.psi.CjFile

/**
 * 符号到源码文件的稳定导航入口。
 *
 * 优先使用当前 symbol 自身持有的 source / psi / backing file；
 * 若当前 symbol 只存在 decompiled 载体，则统一交给共享的 decompiled 查找协议。
 */
internal class CaCfirSourceProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSourceProvider {
    override fun CaSymbol.getContainingFile(): CjFile? = withValidityAssertion {
        when (this@getContainingFile) {
            is CaFileSymbol -> file
            is CaCfirExtendSymbol -> (extendPsi?.containingFile as? CjFile) ?: decompiledFallbackFile(this@getContainingFile)
            is CaDeclarationSymbol -> {
                (psi?.containingFile as? CjFile)
                    ?: (this@getContainingFile as? CaCfirBackedSymbol<*>)?.let { symbol ->
                        analysisSession.symbolQueries.lookupContainingFile(symbol.backingSymbol)
                    }
                    ?: decompiledFallbackFile(this@getContainingFile)
            }

            is CaCfirBackedSymbol<*> -> {
                analysisSession.symbolQueries.lookupContainingFile(backingSymbol)
                    ?: decompiledFallbackFile(this@getContainingFile)
            }

            else -> decompiledFallbackFile(this@getContainingFile)
        }
    }

    private fun decompiledFallbackFile(symbol: CaSymbol): CjFile? {
        val packageFqName = symbol.decompiledContainingPackageFqName() ?: return null
        return analysisSession.findDecompiledContainingFile(packageFqName)
    }
}
