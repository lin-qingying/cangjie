package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.caches

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolUnavailableException
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsOnlyApi
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLKnownClassDeclarationSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleSpecificSymbolProviderAccess
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.hasPsi
import org.cangnova.cangjie.cfir.caches.CfirCache
import org.cangnova.cangjie.cfir.caches.CfirCacheInternals
import org.cangnova.cangjie.cfir.caches.cfirCachesFactory
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * A cache for [CfirClassLikeSymbol]s which is able to provide an exact symbol for a given PSI declaration, even if the symbol's [ClassId] is
 * ambiguous.
 *
 * ## 两个精度，两个缓存，键不混用
 *
 * [CfirCache] 的契约是「命中只看 key，context 只参与首次计算」。因此**正确性约束必须进 key**：
 *
 * - [byClassIdCache] 的 key 是 [ClassId]，语义是「任意命中即可」；
 * - [byPsiCache] 的 key 是 PSI 声明，语义是「必须精确命中这个声明」——它只服务精确查询。
 *
 * 二者绝不共享条目：按 ClassId 的查询得到什么都只影响它自己的键，精确查询不会把
 * 「这个 ClassId 查不到」当成「这个声明对应的符号是 null」。这正是旧实现里
 * `getNotNullValueForNotNullContext` / `fixInconsistentValue` 试图在运行期纠正、
 * 却只能以「cache inconsistency」错误日志收场的那类契约冲突。
 *
 * ## 否定结果不持久化
 *
 * 两条路径都**不**把空结果写进缓存：空的成因绝大多数是「索引尚未就绪」，
 * 而 session 不会因为 dumb 模式结束而重建，缓存空值等于把它冻结成永久缺失。
 * 重算成本由符号层的 `MemoizedResolutionLazy` 用短 TTL 吸收，不在这一层重复做。
 *
 * 该缓存由 symbol provider 用于实现
 * [LLPsiAwareSymbolProvider][org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLPsiAwareSymbolProvider]。
 */
internal open class LLPsiAwareClassLikeSymbolCache<E : PsiElement, V : CfirClassLikeSymbol<*>?, CONTEXT>(
    /**
     * 按 class id 缓存的主 class-like symbol cache。
     */
    private val byClassIdCache: CfirCache<ClassId, V, CONTEXT>,

    /**
     * 按 PSI 声明缓存的精确 cache。
     */
    private val byPsiCache: CfirCache<E, V, CONTEXT>,
) {
    constructor(
        session: LLCfirSession,
        computeSymbolByClassId: (ClassId, CONTEXT) -> V,
        computeSymbolByPsi: (E, CONTEXT) -> V,
    ) : this(
        session.cfirCachesFactory.createCache<ClassId, V, CONTEXT> { classId, context ->
            computeSymbolByClassId(classId, context)
        },
        session.cfirCachesFactory.createCache<E, V, CONTEXT> { declaration, context ->
            computeSymbolByPsi(declaration, context)
        },
    )

    /**
     * Returns the already cached or newly computed symbol for the given [classId].
     *
     * 允许返回 `null`：调用方传入的 [context] 可能为空（只要求「任意命中」），
     * 此时「查不到」是合法结果而不是缓存不一致。
     *
     * **否定结果不写入缓存**：这里的 miss 大多是「索引尚未就绪」这类可自愈状态，
     * 一旦持久化就会在同一 session 里退化成永久缺失（session 只在模块修改事件时重建）。
     * 空值因此只作为本次查询的返回值，下一次查询会重新尝试。
     */
    @LLModuleSpecificSymbolProviderAccess
    fun getSymbolByClassId(
        classId: ClassId,
        context: CONTEXT,
    ): V = byClassIdCache.getValueCachingIf(classId, context) { it != null }

    /**
     * Returns the symbol for the given [classId] if it's already cached.
     *
     * The function does not query the ambiguity cache because any symbol corresponding to [classId] is acceptable.
     */
    fun getCachedSymbolByClassId(classId: ClassId): V? = byClassIdCache.getValueIfComputed(classId)

    /**
     * Returns the already cached or newly computed symbol for exactly the given [declaration], after ensuring that it is of type [T].
     *
     * [getSymbolByPsi] is a common implementation for [LLPsiAwareSymbolProvider.getClassLikeSymbolByPsi][org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLPsiAwareSymbolProvider.getClassLikeSymbolByPsi].
     */
    @LLModuleSpecificSymbolProviderAccess
    inline fun <reified T : E> getSymbolByPsi(
        classId: ClassId,
        declaration: PsiElement,
        createContext: (T) -> CONTEXT,
    ): V? {
        if (declaration !is T) return null

        // Avoid the creation of the context if the symbol is already cached.
        getCachedSymbolByPsi(classId, declaration)?.let { return it }

        return getSymbolByPsi(classId, declaration, createContext(declaration))
    }

    /**
     * Returns the already cached or newly computed symbol for exactly the given [declaration].
     *
     * 查找顺序：PSI 精确缓存 → ClassId 缓存（且必须 PSI 匹配）→ 以 [declaration] 为上下文计算。
     * 第三步的结果写入 [byPsiCache]，因此不会与 [byClassIdCache] 里的否定结果冲突。
     */
    @LLModuleSpecificSymbolProviderAccess
    fun getSymbolByPsi(
        classId: ClassId,
        declaration: E,
        context: CONTEXT,
    ): V? {
        byPsiCache.getValueIfComputed(declaration)?.let { return it }

        // 快速路径：绝大多数 ClassId 没有歧义，按 ClassId 命中的符号就是这个声明对应的符号。
        byClassIdCache.getValueIfComputed(classId)?.takeIf { it.hasPsi(declaration) }?.let { return it }

        // 精确路径：以声明自身为上下文计算，并缓存在 PSI 键下。
        // 与 ClassId 路径同样不缓存否定结果：同一声明在索引就绪后应当能恢复出来。
        return byPsiCache.getValueCachingIf(declaration, context) { it != null }
    }

    /**
     * Returns the symbol for exactly the given [declaration] if it's already cached.
     */
    fun getCachedSymbolByPsi(classId: ClassId, declaration: E): V? =
        byPsiCache.getValueIfComputed(declaration)
            ?: getCachedSymbolByClassId(classId)?.takeIf { it.hasPsi(declaration) }

    @OptIn(CfirCacheInternals::class)
    @LLStatisticsOnlyApi
    /**
     * 当前两个底层 cache 中已经缓存的 symbol 值。
     */
    internal val cachedValues: List<V>
        get() = byClassIdCache.cachedValues + byPsiCache.cachedValues
}

/**
 * 把「符号不可用」降级为「未找到」。
 *
 * 只捕获 Analysis API 公共层的 [CaSymbolUnavailableException]：它携带封闭的缺失原因分类，
 * 上层据此判断是否属于可预期缺失。这里不做任何异常消息文本匹配——
 * 分类必须由类型系统承载，否则新增原因时会静默漏判。
 */
internal fun <T> tolerateMissingCachedSymbol(classId: ClassId, action: () -> T?): T? =
    try {
        action()
    } catch (unavailable: CaSymbolUnavailableException) {
        if (unavailable.reason.isExpectedAbsence) {
            LOG.debug("Class-like symbol for $classId is unavailable (${unavailable.reason.name})")
            null
        } else {
            throw unavailable
        }
    }

private val LOG = Logger.getInstance("org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.caches.LLPsiAwareClassLikeSymbolCache")
