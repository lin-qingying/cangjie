package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase

/**
 * Analysis API 统计指标的公开名称视图。
 *
 * 指标名的事实来源是 [LLStatisticsScopes]，但它是 low-level 模块的 internal 声明，
 * 跨模块的性能测试模块无法引用。这里以 `@LLStatisticsOnlyApi` 暴露同一组名字的只读视图，
 * 使得断言方（性能测试、诊断导出工具）无需复制字符串常量。
 *
 * Kotlin 侧没有对应的跨模块消费者（`LLStatisticsScopes` 在 Kotlin 中同样是 internal），
 * 因此这是仓颉为性能测试模块补出的 Cfir API，对应关系记录如下：
 *
 * - `LLStatisticsScopes`（internal，唯一事实来源）；
 * - `LLStatisticsMetricNames`（本对象，公开只读视图）。
 *
 * 名字变更必须同步 `low-level-api-cfir` 的 `LLStatisticsScopesTest`，它锁定根名与前缀。
 */
@LLStatisticsOnlyApi
object LLStatisticsMetricNames {
    /**
     * 根 scope：`cangjie.analysis`。
     */
    val root: String
        get() = LLStatisticsScopes.name

    /**
     * `analysisSessions.analyze.invocations`：进入 Analysis API 分析的次数。
     */
    val analyzeInvocations: String
        get() = LLStatisticsScopes.AnalysisSessions.Analyze.Invocations.name

    /**
     * `analysisSessions.lowMemoryCacheCleanup.invocations`：stop-the-world 缓存清理执行次数。
     */
    val lowMemoryCacheCleanupInvocations: String
        get() = LLStatisticsScopes.AnalysisSessions.LowMemoryCacheCleanup.Invocations.name

    /**
     * `analysisSessions.resolveCallCache.hits`：调用解析缓存命中次数。
     */
    val resolveCallCacheHits: String
        get() = LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache.hits.name

    /**
     * `analysisSessions.resolveCallCache.misses`：调用解析缓存未命中次数。
     */
    val resolveCallCacheMisses: String
        get() = LLStatisticsScopes.AnalysisSessions.Caches.ResolveCallCache.misses.name

    /**
     * `analysisSessions.resolveSymbolCache.hits`：元素级符号解析缓存命中次数。
     */
    val resolveSymbolCacheHits: String
        get() = LLStatisticsScopes.AnalysisSessions.Caches.ResolveSymbolCache.hits.name

    /**
     * `analysisSessions.resolveSymbolCache.misses`：元素级符号解析缓存未命中次数。
     */
    val resolveSymbolCacheMisses: String
        get() = LLStatisticsScopes.AnalysisSessions.Caches.ResolveSymbolCache.misses.name

    /**
     * `analysisSessions.resolveToSymbolsCache.hits`：引用到符号集合缓存命中次数。
     */
    val resolveToSymbolsCacheHits: String
        get() = LLStatisticsScopes.AnalysisSessions.Caches.ResolveToSymbolsCache.hits.name

    /**
     * `analysisSessions.resolveToSymbolsCache.misses`：引用到符号集合缓存未命中次数。
     */
    val resolveToSymbolsCacheMisses: String
        get() = LLStatisticsScopes.AnalysisSessions.Caches.ResolveToSymbolsCache.misses.name

    /**
     * `symbolProviders.combined.classes.hits`：组合符号提供者类缓存命中次数。
     */
    val combinedSymbolProviderClassCacheHits: String
        get() = LLStatisticsScopes.SymbolProviders.Combined.Classes.hits.name

    /**
     * `symbolProviders.combined.classes.misses`：组合符号提供者类缓存未命中次数。
     */
    val combinedSymbolProviderClassCacheMisses: String
        get() = LLStatisticsScopes.SymbolProviders.Combined.Classes.misses.name

    /**
     * `symbolProviders.combined.callables.hits`：组合符号提供者成员缓存命中次数。
     */
    val combinedSymbolProviderCallableCacheHits: String
        get() = LLStatisticsScopes.SymbolProviders.Combined.Callables.hits.name

    /**
     * `symbolProviders.combined.callables.misses`：组合符号提供者成员缓存未命中次数。
     */
    val combinedSymbolProviderCallableCacheMisses: String
        get() = LLStatisticsScopes.SymbolProviders.Combined.Callables.misses.name

    /**
     * 语义解析阶段耗时指标名（毫秒直方图）。
     */
    fun resolvePhaseDuration(phase: CfirResolvePhase): String = LLStatisticsScopes.Resolve.Phases.duration(phase)

    /**
     * 语义解析阶段覆盖文件数指标名，仅全量解析路径累加。
     */
    fun resolvePhaseFiles(phase: CfirResolvePhase): String = LLStatisticsScopes.Resolve.Phases.files(phase)

    /**
     * 语义解析阶段推进声明数指标名，仅按需解析路径累加。
     */
    fun resolvePhaseDeclarations(phase: CfirResolvePhase): String = LLStatisticsScopes.Resolve.Phases.declarations(phase)

    /**
     * 语义解析阶段执行次数指标名。
     */
    fun resolvePhaseRuns(phase: CfirResolvePhase): String = LLStatisticsScopes.Resolve.Phases.runs(phase)

    /**
     * 判断指标名是否属于本项目的统计命名空间。
     */
    fun isCangjieAnalysisMetric(name: String): Boolean = name.startsWith("$root.")
}