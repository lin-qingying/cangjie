package org.cangnova.cangjie.analysis.api.performance.test

/**
 * 性能测试断言用的指标名。
 *
 * 这些名字由 low-level 的 `LLStatisticsScopes` 派生（根 scope 为 `cangjie.analysis`）。
 * `LLStatisticsScopes` 是 low-level 模块的 internal 声明，测试模块无法直接引用，
 * 因此在这里按同样的层级拼出名字；`low-level-api-cfir` 的 `LLStatisticsScopesTest`
 * 锁定了根名与前缀，改动那边时必须同步本文件。
 */
object CaPerformanceMetricNames {
    /**
     * `cangjie.analysis` 根 scope。
     */
    const val ROOT: String = "cangjie.analysis"

    /**
     * `analysisSessions.analyze.invocations`：进入 Analysis API 分析的次数。
     */
    const val ANALYZE_INVOCATIONS: String = "$ROOT.analysisSessions.analyze.invocations"

    /**
     * `analysisSessions.lowMemoryCacheCleanup.invocations`：stop-the-world 缓存清理执行次数。
     */
    const val LOW_MEMORY_CACHE_CLEANUP_INVOCATIONS: String =
        "$ROOT.analysisSessions.lowMemoryCacheCleanup.invocations"

    /**
     * `analysisSessions.resolveCallCache.hits`：调用解析缓存命中次数。
     *
     * 注意 `LLStatisticsScopes.AnalysisSessions.Caches` 只是分组 object，本身不是 scope，
     * 因此缓存指标名里没有 `caches` 这一段。
     */
    const val RESOLVE_CALL_CACHE_HITS: String = "$ROOT.analysisSessions.resolveCallCache.hits"

    /**
     * `analysisSessions.resolveCallCache.misses`：调用解析缓存未命中次数。
     */
    const val RESOLVE_CALL_CACHE_MISSES: String = "$ROOT.analysisSessions.resolveCallCache.misses"

    /**
     * `analysisSessions.resolveSymbolCache.hits`：单符号解析缓存命中次数。
     */
    const val RESOLVE_SYMBOL_CACHE_HITS: String = "$ROOT.analysisSessions.resolveSymbolCache.hits"

    /**
     * `analysisSessions.resolveSymbolCache.misses`：单符号解析缓存未命中次数。
     */
    const val RESOLVE_SYMBOL_CACHE_MISSES: String = "$ROOT.analysisSessions.resolveSymbolCache.misses"

    /**
     * `analysisSessions.resolveToSymbolsCache.hits`：引用到符号集合缓存命中次数。
     */
    const val RESOLVE_TO_SYMBOLS_CACHE_HITS: String = "$ROOT.analysisSessions.resolveToSymbolsCache.hits"

    /**
     * `analysisSessions.resolveToSymbolsCache.misses`：引用到符号集合缓存未命中次数。
     */
    const val RESOLVE_TO_SYMBOLS_CACHE_MISSES: String = "$ROOT.analysisSessions.resolveToSymbolsCache.misses"

    /**
     * 判断指标名是否属于本项目的统计命名空间。
     */
    fun isCangjieAnalysisMetric(name: String): Boolean = name.startsWith("$ROOT.")
}