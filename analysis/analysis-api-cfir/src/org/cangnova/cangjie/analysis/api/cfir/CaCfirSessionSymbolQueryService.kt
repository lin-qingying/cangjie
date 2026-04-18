package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirResolutionFacade
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirTopLevelSymbolQueryResult
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjReferenceExpression

/**
 * CFIR 会话内的符号与源码导航查询服务。
 *
 * 该层统一承接引用解析、顶层符号查询、class-like/file 符号恢复，
 * 以及 `symbol -> source psi/file` 的稳定导航协议。
 */
internal class CaCfirSessionSymbolQueryService(
    private val resolutionFacade: CaCfirResolutionFacade,
    private val cacheStore: CaCfirSessionCacheStore,
) {
    fun resolveSymbols(reference: CjReferenceExpression): Collection<CfirBasedSymbol<*>> =
        resolutionFacade.resolveReference(reference)

    fun lookupClassLikeSymbol(classId: ClassId): CfirClassLikeSymbol<*>? =
        cacheStore.getOrCreateClassLikeSymbol(classId) {
            resolutionFacade.getClassLikeSymbol(classId)
        }

    fun queryTopLevelSymbols(
        packageFqName: FqName,
        name: Name,
    ): CaCfirTopLevelSymbolQueryResult =
        resolutionFacade.getTopLevelSymbols(packageFqName, name)

    fun lookupFileSymbol(file: CjFile): CfirFileSymbol? =
        cacheStore.getOrCreateFileSymbol(file) {
            resolutionFacade.getFileSymbol(file)
        }

    fun lookupSourcePsi(symbol: CfirBasedSymbol<*>): PsiElement? =
        cacheStore.getOrCreateSourcePsi(symbol) {
            resolutionFacade.findSourcePsi(symbol)
        }

    fun lookupSymbolsByPsi(psi: PsiElement): List<CfirBasedSymbol<*>> =
        cacheStore.getOrCreatePsiSymbols(psi) {
            resolutionFacade.getDeclarationSymbols(psi)
        }

    fun lookupContainingFile(symbol: CfirBasedSymbol<*>): CjFile? =
        cacheStore.getOrCreateContainingFile(symbol) {
            resolutionFacade.getContainingFile(symbol)
        }
}
