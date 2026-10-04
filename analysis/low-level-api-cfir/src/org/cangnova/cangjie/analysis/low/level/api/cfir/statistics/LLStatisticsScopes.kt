/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CaDiagnosticCheckerSet
import org.cangnova.cangjie.cfir.analysis.collectors.CfirCheckerComponentKind
import org.cangnova.cangjie.cfir.analysis.collectors.DiagnosticCollectionPhase
import org.cangnova.cangjie.cfir.builder.CfirRawBuildSource
import org.cangnova.cangjie.cfir.builder.CfirRawBuildStage
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroConstructionStage
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirMacroExpansionOutcome
import org.cangnova.cangjie.cfir.serialization.provider.CfirCjoDeserializationStage
import org.cangnova.cangjie.parsing.CangjiePsiParseKind

/**
 * OpenTelemetry 指标 scope 名称。
 */
internal abstract class LLStatisticsScope(val name: String) {
    /**
     * 返回 scope 名称，便于日志和调试输出。
     */
    override fun toString(): String = name
}

/**
 * 通过 [scope] 名称取得 OpenTelemetry meter。
 */
internal fun OpenTelemetry.getMeter(scope: LLStatisticsScope): Meter = getMeter(scope.name)

/**
 * 通过 [scope] 名称取得 OpenTelemetry tracer。
 *
 * 与 [getMeter] 共用同一份 scope 名字：同名指标与 span 在看板上可以直接对照。
 */
internal fun OpenTelemetry.getTracer(scope: LLStatisticsScope): Tracer = getTracer(scope.name)

/**
 * Caffeine cache 统计 scope 需要提供的 hit/miss/eviction 子 scope。
 */
internal interface LLCaffeineStatisticsScope {
    /**
     * cache hit 指标 scope。
     */
    val hits: LLStatisticsScope

    /**
     * cache miss 指标 scope。
     */
    val misses: LLStatisticsScope

    /**
     * cache eviction 指标 scope。
     */
    val evictions: LLStatisticsScope
}

/**
 * low-level analysis 统计指标的根 scope。
 *
 * 根名使用仓颉自己的命名空间 `cangjie.analysis`：所有 Analysis API 指标都以它为前缀，
 * 导出到平台后可以直接按产品前缀区分来源。
 */
internal object LLStatisticsScopes : LLStatisticsScope("cangjie.analysis") {
    /**
     * analysis session 相关指标 scope。
     */
    object AnalysisSessions : LLStatisticsScope("$name.analysisSessions") {
        /**
         * analyze 调用相关指标 scope。
         */
        object Analyze : LLStatisticsScope("$name.analyze") {
            /**
             * analyze 调用次数 scope。
             */
            object Invocations : LLStatisticsScope("$name.invocations")
        }

        /**
         * 低内存缓存清理相关指标 scope。
         */
        object LowMemoryCacheCleanup : LLStatisticsScope("$name.lowMemoryCacheCleanup") {
            /**
             * 低内存缓存清理触发次数 scope。
             */
            object Invocations : LLStatisticsScope("$name.invocations")
        }

        /**
         * analysis session 缓存指标 scope 集合。
         */
        object Caches {
            /**
             * resolve call cache 指标 scope。
             */
            object ResolveCallCache : LLStatisticsScope("$name.resolveCallCache"), LLCaffeineStatisticsScope {
                /**
                 * resolve call cache hit scope。
                 */
                object Hits : LLStatisticsScope("$name.hits")

                /**
                 * resolve call cache miss scope。
                 */
                object Misses : LLStatisticsScope("$name.misses")

                /**
                 * resolve call cache eviction scope。
                 */
                object Evictions : LLStatisticsScope("$name.evictions")

                /**
                 * cache hit 子 scope。
                 */
                override val hits: LLStatisticsScope get() = Hits

                /**
                 * cache miss 子 scope。
                 */
                override val misses: LLStatisticsScope get() = Misses

                /**
                 * cache eviction 子 scope。
                 */
                override val evictions: LLStatisticsScope get() = Evictions
            }

            /**
             * resolve-to-symbols cache 指标 scope。
             */
            object ResolveToSymbolsCache : LLStatisticsScope("$name.resolveToSymbolsCache"), LLCaffeineStatisticsScope {
                /**
                 * resolve-to-symbols cache hit scope。
                 */
                object Hits : LLStatisticsScope("$name.hits")

                /**
                 * resolve-to-symbols cache miss scope。
                 */
                object Misses : LLStatisticsScope("$name.misses")

                /**
                 * resolve-to-symbols cache eviction scope。
                 */
                object Evictions : LLStatisticsScope("$name.evictions")

                /**
                 * cache hit 子 scope。
                 */
                override val hits: LLStatisticsScope get() = Hits

                /**
                 * cache miss 子 scope。
                 */
                override val misses: LLStatisticsScope get() = Misses

                /**
                 * cache eviction 子 scope。
                 */
                override val evictions: LLStatisticsScope get() = Evictions
            }

            /**
             * resolve symbol cache 指标 scope。
             */
            object ResolveSymbolCache : LLStatisticsScope("$name.resolveSymbolCache"), LLCaffeineStatisticsScope {
                /**
                 * resolve symbol cache hit scope。
                 */
                object Hits : LLStatisticsScope("$name.hits")

                /**
                 * resolve symbol cache miss scope。
                 */
                object Misses : LLStatisticsScope("$name.misses")

                /**
                 * resolve symbol cache eviction scope。
                 */
                object Evictions : LLStatisticsScope("$name.evictions")

                /**
                 * cache hit 子 scope。
                 */
                override val hits: LLStatisticsScope get() = Hits

                /**
                 * cache miss 子 scope。
                 */
                override val misses: LLStatisticsScope get() = Misses

                /**
                 * cache eviction 子 scope。
                 */
                override val evictions: LLStatisticsScope get() = Evictions
            }
        }
    }

    /**
     * 语义解析（resolve）阶段指标。
     *
     * 每个阶段按 `duration`（毫秒直方图）、`runs`（该阶段执行次数）、`files`（全量解析覆盖的 CFIR 文件数）、
     * `declarations`（按需解析推进的声明数）四个后缀命名，阶段名取 [CfirResolvePhase] 的小写形式。
     */
    object Resolve : LLStatisticsScope("$name.resolve") {
        /**
         * 语义解析阶段指标集合。
         */
        object Phases : LLStatisticsScope("$name.phases") {
            /**
             * 阶段链路 span 名：`<阶段名>`。
             */
            fun span(phase: CfirResolvePhase): String = "$name.${phase.name.lowercase()}"

            /**
             * 阶段耗时（毫秒）。
             */
            fun duration(phase: CfirResolvePhase): String = "$name.${phase.name.lowercase()}.duration"

            /**
             * 阶段覆盖的文件数，仅全量解析路径累加。
             */
            fun files(phase: CfirResolvePhase): String = "$name.${phase.name.lowercase()}.files"

            /**
             * 阶段推进的声明数，仅按需解析路径累加。
             */
            fun declarations(phase: CfirResolvePhase): String = "$name.${phase.name.lowercase()}.declarations"

            /**
             * 阶段执行次数。
             */
            fun runs(phase: CfirResolvePhase): String = "$name.${phase.name.lowercase()}.runs"
        }
    }

    /**
     * raw CFIR 构建指标。
     *
     * 按「前端来源 × 流水线阶段」分段命名：`<source>.<stage>.duration`（毫秒直方图）
     * 与 `<source>.<stage>.runs`（执行次数），两段分别取
     * [CfirRawBuildSource.metricSuffix] 与 [CfirRawBuildStage.metricSuffix]。
     * 阶段维度是必需的：IDE 首开卡顿必须能区分解析慢与转换慢。
     */
    object RawBuild : LLStatisticsScope("$name.rawBuild") {
        /**
         * `来源.阶段` 段前缀。
         */
        fun segment(source: CfirRawBuildSource, stage: CfirRawBuildStage): String =
            "${name}.${source.metricSuffix}.${stage.metricSuffix}"

        /**
         * 单次阶段执行耗时（毫秒）。
         */
        fun duration(source: CfirRawBuildSource, stage: CfirRawBuildStage): String = "${segment(source, stage)}.duration"

        /**
         * 阶段执行次数。
         */
        fun runs(source: CfirRawBuildSource, stage: CfirRawBuildStage): String = "${segment(source, stage)}.runs"
    }

    /**
     * macro construction 指标。
     *
     * construction 主流程分三段（[CfirMacroConstructionStage]），每段各有耗时与次数；
     * 展开段额外记结果归类、覆盖文件数与宏 surface 数。
     */
    object Macro : LLStatisticsScope("$name.macro") {
        /**
         * construction 阶段的耗时与次数，两段指标名一致。
         */
        abstract class StageMetrics(name: String) : LLStatisticsScope(name) {
            /**
             * 本阶段耗时（毫秒）。
             */
            fun duration(): String = "$name.duration"

            /**
             * 本阶段执行次数。
             */
            fun runs(): String = "$name.runs"
        }

        /**
         * 阶段 1/3：符号索引构建。
         */
        object SymbolIndex : StageMetrics("$name.symbolIndex")

        /**
         * 阶段 2/3：import 绑定。
         */
        object ImportBinding : StageMetrics("$name.importBinding")

        /**
         * 阶段 3/3：真实展开。
         */
        object Expansion : StageMetrics("$name.expansion") {
            /**
             * construction 覆盖的 pre-macro 文件数。
             */
            fun files(): String = "$name.files"

            /**
             * construction 覆盖的宏 surface 数。
             */
            fun surfaces(): String = "$name.surfaces"

            /**
             * 按结果归类的次数。
             */
            fun outcome(outcome: CfirMacroExpansionOutcome): String = "$name.${outcome.metricSuffix}"
        }

        /**
         * 阶段 scope，按 [CfirMacroConstructionStage] 取。
         */
        fun stage(stage: CfirMacroConstructionStage): StageMetrics = when (stage) {
            CfirMacroConstructionStage.SYMBOL_INDEX -> SymbolIndex
            CfirMacroConstructionStage.IMPORT_BINDING -> ImportBinding
            CfirMacroConstructionStage.EXPANSION -> Expansion
        }
    }

    /**
     * IDE 请求级诊断收集指标。
     *
     * 四个维度回答四个问题：用户等待的端到端耗时（`collection`）、高频元素查询
     * （`elementCollection`）、文件结构首次构建（`structureBuild`）、checker 实际遍历
     * （`checkerPass.<set>`）。`collection` 包含后两者，总量与分解同时可见。
     */
    object Diagnostics : LLStatisticsScope("$name.diagnostics") {
        /**
         * 耗时、次数与产出诊断数三个指标。
         */
        abstract class OperationMetrics(name: String) : LLStatisticsScope(name) {
            /**
             * 本项操作耗时（毫秒）。
             */
            fun duration(): String = "$name.duration"

            /**
             * 本项操作次数。
             */
            fun runs(): String = "$name.runs"

            /**
             * 本项操作产出的诊断数。
             */
            fun diagnostics(): String = "$name.diagnostics"
        }

        /**
         * 文件级 `collectDiagnostics`。
         */
        object Collection : OperationMetrics("$name.collection")

        /**
         * 元素级 `getDiagnostics`。
         */
        object ElementCollection : OperationMetrics("$name.elementCollection")

        /**
         * 文件结构首次构建。
         */
        object StructureBuild : OperationMetrics("$name.structureBuild")

        /**
         * 单个 structure element 上、某个 checker 集合的完整诊断收集。
         *
         * 计量范围包含该元素上的全部遍历（`SEMA` 与可能发生的 `POST_SEMA`）与诊断提交，
         * 因此它与 [Pass] 的关系是"元素级总量 vs 单次遍历"，不是并列的两个切面。
         */
        object StructureElement : LLStatisticsScope("$name.structureElement") {
            /**
             * 集合段前缀。
             */
            fun set(set: CaDiagnosticCheckerSet): String = "${name}.${set.metricSuffix}"

            /**
             * 某 checker 集合的收集耗时（毫秒）。
             */
            fun duration(set: CaDiagnosticCheckerSet): String = "${set(set)}.duration"

            /**
             * 某 checker 集合的收集次数。
             */
            fun runs(set: CaDiagnosticCheckerSet): String = "${set(set)}.runs"

            /**
             * 某 checker 集合产出的诊断数。
             */
            fun diagnostics(set: CaDiagnosticCheckerSet): String = "${set(set)}.diagnostics"
        }

        /**
         * 单次诊断遍历，按 SEMA / POST_SEMA 阶段分桶。
         */
        object Pass : LLStatisticsScope("$name.pass") {
            /**
             * 阶段段前缀。
             */
            fun phase(phase: DiagnosticCollectionPhase): String = "${name}.${phase.name.lowercase()}"

            /**
             * 某阶段的遍历耗时（毫秒）。
             */
            fun duration(phase: DiagnosticCollectionPhase): String = "${phase(phase)}.duration"

            /**
             * 某阶段的遍历次数。
             */
            fun runs(phase: DiagnosticCollectionPhase): String = "${phase(phase)}.runs"
        }

        /**
         * 单个诊断组件的计量：慢在声明检查、表达式检查还是 CFA。
         *
         * 与 [Pass] 的分工：[Pass] 计量"一次遍历整体"，本段计量"这次遍历里各组件分别多久"。
         * 耗时用微秒而非毫秒——一次「元素 × 组件」检查基本在亚毫秒量级，整毫秒会让样本
         * 塌成 0。
         */
        object CheckerComponent : LLStatisticsScope("$name.checkerComponent") {
            /**
             * 组件段前缀。
             */
            fun component(kind: CfirCheckerComponentKind): String = "${name}.${kind.metricSuffix}"

            /**
             * 单个「元素 × 组件」检查的耗时（微秒）。
             */
            fun duration(kind: CfirCheckerComponentKind): String = "${component(kind)}.duration"

            /**
             * 该组件被执行的元素数；与耗时相除即单次平均。
             */
            fun runs(kind: CfirCheckerComponentKind): String = "${component(kind)}.runs"
        }
    }

    /**
     * session 创建指标，按模块种类分桶。
     */
    object SessionCreation : LLStatisticsScope("$name.sessionCreation") {
        /**
         * 种类段前缀。
         */
        fun kind(kind: CaModuleKind): String = "${name}.${kind.metricSuffix}"

        /**
         * 某类模块的 session 创建耗时（毫秒）。
         */
        fun duration(kind: CaModuleKind): String = "${kind(kind)}.duration"

        /**
         * 某类模块的 session 创建次数。
         */
        fun runs(kind: CaModuleKind): String = "${kind(kind)}.runs"
    }

    /**
     * scope 会话指标。
     */
    object Scopes : LLStatisticsScope("$name.scopes") {
        /**
         * scope session 创建次数（scope 缓存未命中与失效重建）。
         */
        fun sessionCreated(): String = "$name.sessionCreated"
    }

    /**
     * 反序列化指标。
     *
     * 两条独立通道：`.cjo` 库与源码库混用时两者都会发生（打开工程、
     * 补全一个库符号），必须能分开看：`.cjo` 慢在读文件还是慢在逐声明反序列化，
     * 优化方向完全不同。
     */
    object Deserialization : LLStatisticsScope("$name.deserialization") {
        /**
         * class-like 反序列化指标集合（IntelliJ stub 树通道）。
         */
        object ClassLike : LLStatisticsScope("$name.classLike") {
            /**
             * 反序列化耗时（毫秒）。
             */
            fun duration(): String = "$name.duration"

            /**
             * 反序列化次数。
             */
            fun runs(): String = "$name.runs"
        }

        /**
         * `.cjo` 反序列化指标，按阶段分桶。
         */
        object Cjo : LLStatisticsScope("$name.cjo") {
            /**
             * 反序列化阶段的耗时与次数，两段指标名一致。
             *
             * `runs` 的口径是"一次未命中的符号查找"，不是"一个声明"：一次查找可能反序列化
             * 多个声明，声明数由 [Declaration.declarations] 单独给出。
             */
            abstract class StageMetrics(name: String) : LLStatisticsScope(name) {
                /**
                 * 本阶段耗时（毫秒）。
                 */
                fun duration(): String = "$name.duration"

                /**
                 * 本阶段执行次数。
                 */
                fun runs(): String = "$name.runs"
            }

            /**
             * 取某阶段对应的 scope；新增阶段漏登记时漂移守卫会失败。
             */
            fun stage(stage: CfirCjoDeserializationStage): StageMetrics = when (stage) {
                CfirCjoDeserializationStage.PACKAGE_LOAD -> PackageLoad
                CfirCjoDeserializationStage.DECLARATION -> Declaration
            }

            /**
             * 包加载阶段（读字节、解析 FlatBuffers 根、建上下文、初始化包 scope）。
             */
            object PackageLoad : StageMetrics("$name.packageLoad")

            /**
             * 按声明惰性反序列化阶段。
             */
            object Declaration : StageMetrics("$name.declaration") {
                /**
                 * 反序列化声明总数；与 [runs] 一起看才知道"慢在单次重"还是"慢在多"。
                 */
                fun declarations(): String = "$name.declarations"
            }
        }
    }

    /**
     * PSI 解析指标，按解析入口分桶。
     *
     * 与 [RawBuild] 的 LightTree `parse` 阶段互补而非重复：这里测平台驱动的 PSI AST 构建，
     * 那条测 `LightTree2Cfir` 内的 LightTree 构建。
     */
    object Parse : LLStatisticsScope("$name.parse") {
        /**
         * 入口段前缀。
         */
        fun kind(kind: CangjiePsiParseKind): String = "${name}.${kind.metricSuffix}"

        /**
         * 某入口的解析耗时（毫秒）。
         */
        fun duration(kind: CangjiePsiParseKind): String = "${kind(kind)}.duration"

        /**
         * 某入口的解析次数。
         */
        fun runs(kind: CangjiePsiParseKind): String = "${kind(kind)}.runs"
    }

    /**
     * symbol provider 相关指标 scope。
     */
    object SymbolProviders : LLStatisticsScope("$name.symbolProviders") {
        /**
         * combined symbol provider 相关指标 scope。
         */
        object Combined : LLStatisticsScope("$name.combined") {
            /**
             * combined class cache 指标 scope。
             */
            object Classes : LLStatisticsScope("$name.classes"), LLCaffeineStatisticsScope {
                /**
                 * class cache hit scope。
                 */
                object Hits : LLStatisticsScope("$name.hits")

                /**
                 * class cache miss scope。
                 */
                object Misses : LLStatisticsScope("$name.misses")

                /**
                 * class cache eviction scope。
                 */
                object Evictions : LLStatisticsScope("$name.evictions")

                /**
                 * cache hit 子 scope。
                 */
                override val hits: LLStatisticsScope get() = Hits

                /**
                 * cache miss 子 scope。
                 */
                override val misses: LLStatisticsScope get() = Misses

                /**
                 * cache eviction 子 scope。
                 */
                override val evictions: LLStatisticsScope get() = Evictions
            }

            /**
             * combined callable cache 指标 scope。
             */
            object Callables : LLStatisticsScope("$name.callables"), LLCaffeineStatisticsScope {
                /**
                 * callable cache hit scope。
                 */
                object Hits : LLStatisticsScope("$name.hits")

                /**
                 * callable cache miss scope。
                 */
                object Misses : LLStatisticsScope("$name.misses")

                /**
                 * callable cache eviction scope。
                 */
                object Evictions : LLStatisticsScope("$name.evictions")

                /**
                 * cache hit 子 scope。
                 */
                override val hits: LLStatisticsScope get() = Hits

                /**
                 * cache miss 子 scope。
                 */
                override val misses: LLStatisticsScope get() = Misses

                /**
                 * cache eviction 子 scope。
                 */
                override val evictions: LLStatisticsScope get() = Evictions
            }
        }
    }
}
