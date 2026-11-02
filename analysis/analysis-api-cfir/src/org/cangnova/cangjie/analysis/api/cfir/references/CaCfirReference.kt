package org.cangnova.cangjie.analysis.api.cfir.references

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findLibraryPsi
import org.cangnova.cangjie.analysis.api.cfir.findReferencePsi
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.platform.caches.getOrPut
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.idea.references.CjReference
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjImportInfo
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.CjSimpleNameExpression

/**
 * CFIR Analysis API 支持的仓颉 PSI 引用公共接口。
 */
internal sealed interface CaCfirReference : CjReference {
    /**
     * 当前引用使用的 CFIR 多结果解析器。
     */
    override val resolver get() = CaCfirReferenceResolver

    /**
     * 判断当前引用是否指向给定 import alias。
     */
    fun isReferenceToImportAlias(alias: CjImportAlias): Boolean {
        return getImportAlias(alias.importDirective) != null
    }

    /**
     * 在 import 信息中查找与当前引用目标等价的 alias。
     */
    fun getImportAlias(importInfo: CjImportInfo?): CjImportAlias? {
        val importItem = importInfo as? CjImportItem ?: return null
        val importedReference = importItem.importedReference ?: return null
        val importResults =
            when (importedReference) {
                is CjDotQualifiedExpression -> importedReference.selectorExpression?.mainReference?.multiResolve(false)
                is CjSimpleNameExpression -> importedReference.mainReference.multiResolve(false)
                else -> null
            } ?: return null
        val targets = multiResolve(false).mapNotNull { it.element }
        val adjustedImportTargets = importResults.mapNotNull { it.element }
        val manager = importItem.manager
        if (adjustedImportTargets.any { importTarget ->
                targets.any { target ->
                    manager.areElementsEquivalent(target, importTarget)
                }
            }) {
            return importItem.alias
        }
        return null
    }

    /**
     * The result of this method will be used by [resolveToSymbols] and can be cached
     */
    fun CaCfirSession.computeSymbols(): Collection<CaSymbol>

    /**
     * 将解析得到的公开符号转换为 PSI 目标集合。
     *
     * [receiverHint] 是调用点能提供的接收者类型文本（`this` 所在 extend 的接收者、
     * 或显式接收者的声明类型）。库 `extend` 成员没有 classId，只能靠它把同名成员
     * 收敛到唯一 extend。
     */
    fun getResolvedToPsi(
        analysisSession: CaSession,
        referenceTargetSymbols: Collection<CaSymbol>,
        receiverHint: String? = null,
    ): Collection<PsiElement> =
        with(analysisSession) {
            referenceTargetSymbols.flatMap { symbol ->
                when (symbol) {
                    is CaCfirSymbol<*> -> getPsiDeclarations(symbol, receiverHint)
                    else -> listOfNotNull(symbol.psi)
                }
            }
        }

    /**
     * 解析当前引用并返回 PSI 目标集合。
     */
    fun getResolvedToPsi(analysisSession: CaSession): Collection<PsiElement> =
        with(analysisSession) {
            getResolvedToPsi(analysisSession, resolveToSymbols())
        }

    /**
     * 在 CFIR session 缓存中解析当前引用对应的公开符号集合。
     *
     * 只缓存非空结果：冷启动阶段返回空集合时若写入缓存，该引用会在整个会话内保持
     * “无法解析”，直到用户修改对应 PSI 元素（新引用对象）才会重算。
     *
     * 缓存值同时记录解析时所在文件的修改戳。`PsiModificationTracker.MODIFICATION_COUNT` 只统计
     * **物理** PSI 的改动，dangling file / `CjFile.copy()` 这类非物理文件被编辑时它不推进，
     * 于是同一个引用对象会被继续复用：只按引用对象身份命中缓存，就会把编辑前算出的符号
     * 一直返回给编辑后的源码（[fileModificationStamp] 会随手修改推进，据此丢弃旧结果）。
     */
    @OptIn(CaPlatformInterface::class)
    fun CaSession.resolveToSymbols(): Collection<CaSymbol> = withValidityAssertion {
        check(this is CaCfirSession)
        val cache = this.cacheStorage.resolveToSymbolsCache.value
        val reference = this@CaCfirReference
        val stamp = reference.fileModificationStamp
        val cached = cache.getIfPresent(reference)
        if (cached != null) {
            if (cached.fileModificationStamp == stamp) return cached.symbols
            // 文件在两次解析之间被编辑过，旧结果对应的已不是同一段源码，必须丢弃而不是复用。
            cache.invalidate(reference)
        }
        val symbols = computeSymbols()
        if (symbols.isNotEmpty() && stamp != null) {
            cache.put(reference, CaResolvedSymbolsCacheEntry(stamp, symbols))
        }
        symbols
    }
}

/**
 * `resolveToSymbols` 缓存条目。
 *
 * [fileModificationStamp] 是解析时引用所在文件的修改戳，用于识别非物理 PSI 的编辑
 * （物理 PSI 由 `PsiModificationTracker` 产出新引用对象，不需要这一层校验）。
 *
 * @property fileModificationStamp 计算 [symbols] 时的文件修改戳。
 * @property symbols 当时解析出的公开符号集合。
 */
internal data class CaResolvedSymbolsCacheEntry(
    val fileModificationStamp: Long,
    val symbols: Collection<CaSymbol>,
)

/**
 * 引用所在文件的修改戳；用于判断缓存结果是否仍对应同一段源码。
 *
 * 取 `PsiFile.modificationStamp` 而不是 `PsiModificationTracker.MODIFICATION_COUNT`：
 * 后者只统计物理 PSI 的改动，dangling file / `CjFile.copy()` 这类非物理文件编辑后不会推进。
 */
internal val CaCfirReference.fileModificationStamp: Long?
    get() = element.containingFile?.modificationStamp

/**
 * 将 CFIR 公开符号恢复为 reference resolve 可用的 PSI 声明集合。
 */
internal fun CaSession.getPsiDeclarations(
    symbol: CaCfirSymbol<*>,
    receiverHint: String? = null,
): Collection<PsiElement> {
    val intersectionOverriddenSymbolsOrSingle = when {
        symbol.origin == CaSymbolOrigin.INTERSECTION_OVERRIDE && symbol is CaCallableSymbol -> symbol.intersectionOverriddenSymbols
        else -> listOf(symbol)
    }
    return intersectionOverriddenSymbolsOrSingle.mapNotNull {
        it.findPsiForReferenceResolve(analysisScope, receiverHint)
    }
}


/**
 * 恢复单个公开符号在给定搜索范围内的 reference resolve PSI。
 *
 * 源码声明走 `source` 回链；库声明在 CFIR 侧没有 `source`，必须走反编译 PSI
 * （对齐 Kotlin `KaSymbol.psi` 对 `LIBRARY` origin 的保证）。
 */
private fun CaSymbol.findPsiForReferenceResolve(
    scope: GlobalSearchScope,
    receiverHint: String? = null,
): PsiElement? {
    if (this !is CaCfirSymbol<*>) return null
    return cfirSymbol.cfir.findReferencePsi(scope, analysisSession.project)
        ?: findLibraryPsi(receiverHint)
}
