package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.getContainingFile
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.realPsi
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjReferenceExpression

/**
 * CFIR 会话内的符号与源码导航查询服务。
 *
 * 该层只负责从真实的 CFIR/low-level 构件恢复：
 * - reference -> CFIR symbol
 * - PSI declaration -> CFIR symbol
 * - symbol -> source PSI / containing file
 *
 * public symbol 的选择、缓存与稳定 restore 不在这里完成。
 */
internal class CaCfirSessionSymbolQueryService(
    private val resolutionFacade: LLResolutionFacade,
    private val cacheStore: CaCfirSessionCacheStore,
) {
    private val useSiteSession get() = resolutionFacade.useSiteCfirSession

    fun resolveSymbols(reference: CjReferenceExpression): Collection<CfirBasedSymbol<*>> {
        val cfirElement = reference.getOrBuildCfir(resolutionFacade) as? CfirResolvable ?: return emptyList()
        val resolvedReference = cfirElement.calleeReference as? CfirResolvedNamedReference ?: return emptyList()
        return listOf(resolvedReference.resolvedSymbol)
    }

    fun lookupClassLikeSymbol(classId: ClassId): CfirClassLikeSymbol<*>? =
        cacheStore.getOrCreateClassLikeSymbol(classId) {
            useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)
        }

    fun queryTopLevelSymbols(
        packageFqName: FqName,
        name: Name,
    ): CaCfirTopLevelSymbolQueryResult {
        val classLikeSymbol = useSiteSession.symbolProvider.getClassLikeSymbolByClassId(ClassId(packageFqName, name))
        val callableSymbols = useSiteSession.symbolProvider.getTopLevelCallableSymbols(packageFqName, name)
        return CaCfirTopLevelSymbolQueryResult(
            classLikeSymbols = listOfNotNull(classLikeSymbol),
            callableSymbols = callableSymbols,
        )
    }

    fun lookupFileSymbol(file: CjFile): CfirFileSymbol? =
        cacheStore.getOrCreateFileSymbol(file) {
            file.getOrBuildCfirFile(resolutionFacade).symbol
        }

    fun lookupSourcePsi(symbol: CfirBasedSymbol<*>): PsiElement? =
        cacheStore.getOrCreateSourcePsi(symbol) {
            symbol.cfir.realPsi
        }

    /**
     * 这里故意只接受“与声明一一对应”的 PSI。
     *
     * Kotlin FIR 里也不是所有 PSI 都有稳定 symbol；仓颉侧同样保持这个边界，
     * 避免把 `lookupSymbolsByPsi()` 退化成模糊的树上兜底匹配。
     */
    fun lookupSymbolsByPsi(psi: PsiElement): List<CfirBasedSymbol<*>> =
        cacheStore.getOrCreatePsiSymbols(psi) {
            when (psi) {
                is CjDeclaration -> listOf(psi.resolveToCfirSymbol(resolutionFacade))
                is CjFile -> listOfNotNull(lookupFileSymbol(psi))
                else -> emptyList()
            }
        }

    fun lookupContainingFile(symbol: CfirBasedSymbol<*>): CjFile? =
        cacheStore.getOrCreateContainingFile(symbol) {
            symbol.cfir.getContainingFile()?.psi as? CjFile
        }
}
