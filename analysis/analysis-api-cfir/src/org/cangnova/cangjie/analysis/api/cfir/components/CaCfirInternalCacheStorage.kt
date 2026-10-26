package org.cangnova.cangjie.analysis.api.cfir.components

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.psi.util.CachedValue
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.references.CaCfirReference
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponent
import org.cangnova.cangjie.analysis.api.platform.CaCachedService
import org.cangnova.cangjie.analysis.api.platform.caches.NullableCaffeineCache
import org.cangnova.cangjie.analysis.api.platform.caches.NullableConcurrentCache
import org.cangnova.cangjie.analysis.api.platform.caches.withStatsCounter
import org.cangnova.cangjie.analysis.api.resolution.CaCallResolutionAttempt
import org.cangnova.cangjie.analysis.api.resolution.CaSymbolResolutionAttempt
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.LLCfirDeclarationModificationService
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.LLCfirInBlockModificationTracker
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.utils.caches.softCachedValue
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.psi.CjElement
import java.util.concurrent.ConcurrentHashMap

/**
 * 这是直接存放在 [CaSession] 内部的专用缓存位置。
 *
 * 对于 [CaSessionComponent]，这类缓存可以直接放在组件使用点附近；
 * 但对于不属于组件的对象则不行。
 * 例如 [CaCfirReference] 不是 session component，因此这里为它提供统一的
 * [CaSession] 生命周期与失效语义，使 [CaCfirReference.resolveToSymbols] 可以安全缓存。
 *
 * 此外，这个存储还提供了 [softCachedValueWithPsiKey] 这类统一入口。
 */

internal class CaCfirInternalCacheStorage(private val analysisSession: CaCfirSession) {
    @CaCachedService
    /**
     * 统计服务，用于把 Analysis API 缓存命中信息接入 low-level 统计系统。
     */
    private val statisticsService by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLStatisticsService.getInstance(project)
    }

    /**
     * 当前 analysis session 所属项目。
     */
    private val project get() = analysisSession.project

    @OptIn(CaPlatformInterface::class)
    /**
     * PSI 元素到调用解析结果的可空缓存。
     */
    val resolveCallCache: CachedValue<NullableCaffeineCache<CjElement, CaCallResolutionAttempt>> by lazy {
        softCachedValueWithPsiKey {
            NullableCaffeineCache {
                it.withStatsCounter(statisticsService?.analysisSessions?.resolveCallCacheStatsCounter)
            }
        }
    }

    @OptIn(CaPlatformInterface::class)
    /**
     * PSI 元素到单符号解析结果的可空缓存。
     */
    val resolveSymbolCache: CachedValue<NullableCaffeineCache<CjElement, CaSymbolResolutionAttempt>> by lazy {
        softCachedValueWithPsiKey {
            NullableCaffeineCache {
                it.withStatsCounter(statisticsService?.analysisSessions?.resolveSymbolCacheStatsCounter)
            }
        }
    }

    @OptIn(CaPlatformInterface::class)
    /**
     * CFIR 引用到公开符号集合的缓存。
     */
    val resolveToSymbolsCache: CachedValue<Cache<CaCfirReference, Collection<CaSymbol>>> by lazy {
        softCachedValueWithPsiKey {
            Caffeine.newBuilder()
                .withStatsCounter(statisticsService?.analysisSessions?.resolveToSymbolsCacheStatsCounter)
                .build()
        }
    }

    @OptIn(CaPlatformInterface::class)
    /**
     * Cone 类型到替换后成员作用域的会话级缓存。
     *
     * `CaType.scope` 家族与 `CaType.typeScope` 走同一条 `typeToScope` 接缝且参数
     * 完全一致（同 session、同 scopeSession、同 `USE_SITE` 模式），一个类型必须
     * 只构造一份作用域：补全是高频调用方，两个公开入口共享该缓存也避免同一接收者
     * 在一次补全会话里重复遍历 extend 索引与父类型图。
     */
    val typeScopeCache: CachedValue<NullableConcurrentCache<ConeCangJieType, CfirTypeScope?>> by lazy {
        softCachedValueWithPsiKey {
            NullableConcurrentCache()
        }
    }

    /**
     * 获取或计算 [type] 的替换后成员作用域，供 use-site 成员通道与类型作用域通道共享。
     */
    fun getOrCreateTypeScope(
        type: ConeCangJieType,
        create: () -> CfirTypeScope?,
    ): CfirTypeScope? = typeScopeCache.value.getOrPut(type) { create() }

    /**
     * 缓存随对应的 [CaSession] 持有；为响应 session 存续期间的语义变化，也会注册独立修改跟踪器。
     *
     * 函数体内修改由 [LLCfirDeclarationModificationService] 管理的 tracker 跟踪；当 CFIR session
     * 可解析时，宏语义快照修改也会通过 session 的快照 tracker 单独触发缓存失效。
     */
    private inline fun <T> softCachedValueWithPsiKey(crossinline createValue: () -> T): CachedValue<T> {
        val invalidationTrackers = buildList {
            add(LLCfirInBlockModificationTracker.getInstance(project))
            (analysisSession.cfirSession as? LLCfirResolvableModuleSession)
                ?.macroSemanticSnapshotModificationTracker
                ?.let(::add)
        }
        return softCachedValue(project, *invalidationTrackers.toTypedArray()) {
            createValue()
        }
    }
}
