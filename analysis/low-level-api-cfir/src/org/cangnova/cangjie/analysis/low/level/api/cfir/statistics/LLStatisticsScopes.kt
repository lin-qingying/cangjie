/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter

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
 */
internal object LLStatisticsScopes : LLStatisticsScope("kotlin.analysis") {
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
