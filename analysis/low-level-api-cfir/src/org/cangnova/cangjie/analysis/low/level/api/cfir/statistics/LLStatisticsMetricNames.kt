package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.CaModuleKind
import org.cangnova.cangjie.cfir.builder.CfirRawBuildSource
import org.cangnova.cangjie.cfir.builder.CfirRawBuildStage
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CaDiagnosticCheckerSet
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroConstructionStage
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroExpansionOutcome

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
     * `rawBuild.<source>.<stage>.duration`：单次 raw CFIR 构建阶段耗时（毫秒）。
     */
    fun rawBuildDuration(source: CfirRawBuildSource, stage: CfirRawBuildStage): String =
        LLStatisticsScopes.RawBuild.duration(source, stage)

    /**
     * `rawBuild.<source>.<stage>.runs`：raw CFIR 构建阶段执行次数。
     */
    fun rawBuildRuns(source: CfirRawBuildSource, stage: CfirRawBuildStage): String =
        LLStatisticsScopes.RawBuild.runs(source, stage)

    /**
     * `macro.<stage>.duration`：macro construction 阶段耗时（毫秒）。
     */
    fun macroConstructionDuration(stage: CfirMacroConstructionStage): String = LLStatisticsScopes.Macro.stage(stage).duration()

    /**
     * `macro.<stage>.runs`：macro construction 阶段执行次数。
     */
    fun macroConstructionRuns(stage: CfirMacroConstructionStage): String = LLStatisticsScopes.Macro.stage(stage).runs()

    /**
     * `macro.expansion.files`：macro construction 覆盖的 pre-macro 文件数。
     */
    val macroExpandFiles: String
        get() = LLStatisticsScopes.Macro.Expansion.files()

    /**
     * `macro.expansion.surfaces`：macro construction 覆盖的宏 surface 数。
     */
    val macroExpandSurfaces: String
        get() = LLStatisticsScopes.Macro.Expansion.surfaces()

    /**
     * `macro.expansion.<outcome>`：按结果归类的 construction 次数。
     */
    fun macroExpandOutcome(outcome: CfirMacroExpansionOutcome): String = LLStatisticsScopes.Macro.Expansion.outcome(outcome)

    /**
     * `diagnostics.collection.duration`：文件级诊断收集耗时（毫秒）。
     */
    val diagnosticsCollectionDuration: String
        get() = LLStatisticsScopes.Diagnostics.Collection.duration()

    /**
     * `diagnostics.collection.runs`：文件级诊断收集次数。
     */
    val diagnosticsCollectionRuns: String
        get() = LLStatisticsScopes.Diagnostics.Collection.runs()

    /**
     * `diagnostics.collection.diagnostics`：文件级诊断收集产出的诊断数。
     */
    val diagnosticsCollectionDiagnostics: String
        get() = LLStatisticsScopes.Diagnostics.Collection.diagnostics()

    /**
     * `diagnostics.elementCollection.duration`：元素级诊断收集耗时（毫秒）。
     */
    val diagnosticsElementCollectionDuration: String
        get() = LLStatisticsScopes.Diagnostics.ElementCollection.duration()

    /**
     * `diagnostics.elementCollection.runs`：元素级诊断收集次数。
     */
    val diagnosticsElementCollectionRuns: String
        get() = LLStatisticsScopes.Diagnostics.ElementCollection.runs()

    /**
     * `diagnostics.structureBuild.duration`：文件结构首次构建耗时（毫秒）。
     */
    val diagnosticsStructureBuildDuration: String
        get() = LLStatisticsScopes.Diagnostics.StructureBuild.duration()

    /**
     * `diagnostics.structureBuild.runs`：文件结构首次构建次数。
     */
    val diagnosticsStructureBuildRuns: String
        get() = LLStatisticsScopes.Diagnostics.StructureBuild.runs()

    /**
     * `diagnostics.checkerPass.<set>.duration`：某 checker 集合的遍历耗时（毫秒）。
     */
    fun diagnosticsCheckerPassDuration(set: CaDiagnosticCheckerSet): String = LLStatisticsScopes.Diagnostics.CheckerPass.duration(set)

    /**
     * `diagnostics.checkerPass.<set>.runs`：某 checker 集合的遍历次数。
     */
    fun diagnosticsCheckerPassRuns(set: CaDiagnosticCheckerSet): String = LLStatisticsScopes.Diagnostics.CheckerPass.runs(set)

    /**
     * `diagnostics.checkerPass.<set>.diagnostics`：某 checker 集合产出的诊断数。
     */
    fun diagnosticsCheckerPassDiagnostics(set: CaDiagnosticCheckerSet): String = LLStatisticsScopes.Diagnostics.CheckerPass.diagnostics(set)

    /**
     * `sessionCreation.<kind>.duration`：某类模块的 session 创建耗时（毫秒）。
     */
    fun sessionCreationDuration(kind: CaModuleKind): String = LLStatisticsScopes.SessionCreation.duration(kind)

    /**
     * `sessionCreation.<kind>.runs`：某类模块的 session 创建次数。
     */
    fun sessionCreationRuns(kind: CaModuleKind): String = LLStatisticsScopes.SessionCreation.runs(kind)

    /**
     * `scopes.sessionCreated`：scope session 创建次数。
     */
    val scopeSessionsCreated: String
        get() = LLStatisticsScopes.Scopes.sessionCreated()

    /**
     * `deserialization.classLike.duration`：stub class-like 反序列化耗时（毫秒）。
     */
    val deserializationClassLikeDuration: String
        get() = LLStatisticsScopes.Deserialization.ClassLike.duration()

    /**
     * `deserialization.classLike.runs`：stub class-like 反序列化次数。
     */
    val deserializationClassLikeRuns: String
        get() = LLStatisticsScopes.Deserialization.ClassLike.runs()

    /**
     * 判断指标名是否属于本项目的统计命名空间。
     */
    fun isCangjieAnalysisMetric(name: String): Boolean = name.startsWith("$root.")
}