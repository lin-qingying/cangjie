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

/**
 * 细粒度耗时直方图的桶边界（微秒）。
 *
 * 逐组件计量用这一组而不是 [DURATION_BUCKETS_MS]：一次「元素 × 组件」检查基本都在亚毫秒
 * 量级，整毫秒记录会让绝大多数样本塌成 0、直方图 `_sum` 恒为 0（实测某个 resolve 阶段
 * 5 个样本的毫秒总和是 1，其中 4 个记成了 0）。微秒是这类接缝的合适分辨率。
 */
internal val DURATION_BUCKETS_US: List<Long> =
    listOf(1L, 5L, 10L, 25L, 50L, 100L, 250L, 500L, 1_000L, 2_500L, 5_000L, 10_000L, 30_000L, 100_000L, 300_000L)

/**
 * 纳秒到微秒的换算。
 */
internal const val NANOS_PER_MICRO: Long = 1_000L
