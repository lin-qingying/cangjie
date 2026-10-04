package org.cangnova.cangjie.analysis.api.performance.test

/**
 * 一次指标采集的快照：SDK 的指标按 JVM 累积，直接读绝对值拿到的是"自进程启动起的总量"。
 *
 * 因此所有跨用例的对比都建立在**增量**上：动作前后各采一次快照，相减得到 [minus]。
 * 这与既有用例的 `counterDelta` 是同一套语义，只是提前到基类统一采集。
 *
 * 耗时类指标在采集时就按 SDK 声明的单位换算成**纳秒**（见 [toNanos]）。不统一单位就直接
 * 参与对比是个真实的坑：逐诊断组件记的是微秒、其余记的是毫秒，混在一起比出来的比例毫无意义。
 *
 * 增量里丢掉未变化的项：一次完整运行有几十项指标，只保留真正动过的可以把运行记录
 * 压到可读大小，也让"哪些指标真的被这个用例触发了"一眼可见。
 */
data class CaPerformanceSnapshot(
    /** 计数器型指标的累积值。 */
    val counters: Map<String, Long>,
    /** 耗时直方图采样值之和，已统一换算为纳秒。 */
    val durationNanosSum: Map<String, Double>,
    /** 耗时直方图的采样点数。 */
    val histogramCounts: Map<String, Long>,
) {
    companion object {
        /**
         * 从当前 SDK 采集一份快照，耗时值同时完成单位归一。
         */
        fun capture(): CaPerformanceSnapshot {
            val units = CaPerformanceTestTelemetry.collectHistogramUnits()
            val sums = LinkedHashMap<String, Double>()
            CaPerformanceTestTelemetry.collectHistogramSums().forEach { (name, value) ->
                sums[name] = value * toNanos(units[name])
            }
            return CaPerformanceSnapshot(
                counters = CaPerformanceTestTelemetry.collectLongCounters(),
                durationNanosSum = sums,
                histogramCounts = CaPerformanceTestTelemetry.collectHistogramPointCounts(),
            )
        }

        /**
         * OpenTelemetry 单位到纳秒的换算系数。
         *
         * 未声明或无法识别的单位按毫秒处理——仓内所有耗时埋点都在这两档里，
         * 且错认成毫秒只会在报告里显示出偏大的数字，不会把结果判反。
         */
        internal fun toNanos(unit: String?): Double = when (unit?.lowercase()) {
            "ns" -> 1.0
            "us" -> 1_000.0
            "s" -> 1_000_000_000.0
            else -> 1_000_000.0
        }
    }

    /**
     * 返回相对 [earlier] 的增量，未变化的项不出现在结果里。
     */
    fun minus(earlier: CaPerformanceSnapshot): CaPerformanceDelta = CaPerformanceDelta(
        counters = (counters.keys + earlier.counters.keys)
            .mapNotNull { name -> subtract(counters[name], earlier.counters[name])?.let { name to it } }
            .toMap(),
        durationNanosSum = (durationNanosSum.keys + earlier.durationNanosSum.keys)
            .mapNotNull { name -> subtract(durationNanosSum[name], earlier.durationNanosSum[name])?.let { name to it } }
            .toMap(),
        histogramCounts = (histogramCounts.keys + earlier.histogramCounts.keys)
            .mapNotNull { name -> subtract(histogramCounts[name], earlier.histogramCounts[name])?.let { name to it } }
            .toMap(),
    )

    /**
     * 两个可空的数值之差；缺一侧按 0 计，相等时返回 null 以便从结果中剔除。
     */
    private fun subtract(current: Long?, earlier: Long?): Long? {
        if (current == null && earlier == null) return null
        val delta = (current ?: 0L) - (earlier ?: 0L)
        return delta.takeIf { it != 0L }
    }

    /**
     * 两个可空的数值之差；缺一侧按 0 计，相等时返回 null。
     */
    private fun subtract(current: Double?, earlier: Double?): Double? {
        if (current == null && earlier == null) return null
        val delta = (current ?: 0.0) - (earlier ?: 0.0)
        return delta.takeIf { it != 0.0 }
    }
}

/**
 * 两个 [CaPerformanceSnapshot] 之间的增量，只保留发生变化的项。
 */
data class CaPerformanceDelta(
    /** 计数器增量。 */
    val counters: Map<String, Long>,
    /** 耗时直方图采样值之和的增量，单位纳秒。 */
    val durationNanosSum: Map<String, Double>,
    /** 耗时直方图采样点数的增量。 */
    val histogramCounts: Map<String, Long>,
) {
    companion object {
        /**
         * 全零增量，供尚未采集到任何东西时使用。
         */
        val EMPTY = CaPerformanceDelta(emptyMap(), emptyMap(), emptyMap())
    }

    /**
     * 本增量涉及的全部指标名，按名称排序。
     */
    val metricNames: List<String>
        get() = (counters.keys + durationNanosSum.keys + histogramCounts.keys).sorted()
}