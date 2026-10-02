package org.cangnova.cangjie.analysis.low.level.api.cfir.statistics

/**
 * 耗时类直方图的桶边界（毫秒）。
 *
 * 前端各阶段耗时跨度较大：从亚毫秒级的单声明推进到整包解析的秒级，用对数式边界覆盖两端。
 */
internal val DURATION_BUCKETS_MS: List<Long> =
    listOf(1L, 5L, 10L, 25L, 50L, 100L, 250L, 500L, 1_000L, 2_500L, 5_000L, 10_000L, 30_000L)

/**
 * 纳秒到毫秒的换算。
 */
internal const val NANOS_PER_MILLI: Long = 1_000_000L
