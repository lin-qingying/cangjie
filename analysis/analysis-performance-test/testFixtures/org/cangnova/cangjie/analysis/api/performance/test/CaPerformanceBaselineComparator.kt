package org.cangnova.cangjie.analysis.api.performance.test

/**
 * 单个信号的对比判定。
 *
 * 枚举存在的理由是"没有基线"与"有基线但变化在噪声内"必须分开呈现：前者是数据还不够，
 * 后者是数据够了但这次变化不足以说明问题。把两者混成"无变化"会让真正的回归被淹没。
 */
enum class CaPerformanceVerdict {
    /** 明显变慢。 */
    REGRESSED,

    /** 明显变快。 */
    IMPROVED,

    /** 有足够基线，但变化落在观测抖动以内，不能当作改进或回归。 */
    NOISE,

    /** 有上一次运行，但历史样本不足以下抖动判定。 */
    INSUFFICIENT_DATA,

    /** 历史里找不到这个用例。 */
    BASELINE_MISSING,
}

/**
 * 一个信号（用例墙钟或某个指标）的对比结果。
 *
 * @property signalName 信号名
 * @property current 本次运行的取值
 * @property baseline 基线取值
 * @property baselineSamples 基线取自多少次历史运行
 * @property baselineDescription 基线来源的人话描述
 * @property relativeChange 相对变化率；基线为 0 时为 null
 * @property threshold 判定阈值：观测抖动与固定下限取较大者
 * @property verdict 判定结果
 */
data class CaPerformanceSignalComparison(
    val signalName: String,
    val current: Double,
    val baseline: Double,
    val baselineSamples: Int,
    val baselineDescription: String,
    val relativeChange: Double?,
    val threshold: Double,
    val verdict: CaPerformanceVerdict,
)

/**
 * 某个用例的完整对比结果。
 */
data class CaPerformanceCaseComparison(
    val caseId: String,
    val testClass: String,
    val testMethod: String,
    val signals: List<CaPerformanceSignalComparison>,
) {
    /**
     * 需要被关注的信号：明确变慢或明显变快。
     */
    val notableSignals: List<CaPerformanceSignalComparison>
        get() = signals.filter {
            it.verdict == CaPerformanceVerdict.REGRESSED || it.verdict == CaPerformanceVerdict.IMPROVED
        }
}

/**
 * 一次运行的完整对比结果。
 */
data class CaPerformanceComparisonReport(
    val runId: String,
    val baselineRunId: String?,
    val cases: List<CaPerformanceCaseComparison>,
) {
    /**
     * 本次运行全部变慢的用例。
     */
    val regressions: List<CaPerformanceCaseComparison>
        get() = cases.filter { case ->
            case.signals.any { it.verdict == CaPerformanceVerdict.REGRESSED }
        }
}

/**
 * 把一次运行记录摊平成"信号名 → 数值"，供对比使用。
 *
 * 三类信号，**耗时类一律是纳秒**（见 [CaPerformanceSnapshot.capture] 的单位归一）：
 * - `用例墙钟`：该用例执行的墙钟耗时（纳秒）；
 * - `耗时·<指标>`：耗时直方图的单次均值（纳秒）；
 * - `计数·<指标>`：计数器增量。计数是精确值，不做单位换算。
 *
 * 只保留真正变化的指标，因此"这个用例到底触发了什么"直接可读，不需要再去翻指标全表。
 */
internal fun CaPerformanceCaseRecord.toSignals(): Map<String, Double> {
    val signals = LinkedHashMap<String, Double>()
    signals[WALL_CLOCK_SIGNAL] = wallNanos.toDouble()

    delta.durationNanosSum.forEach { (name, sumNanos) ->
        val count = delta.histogramCounts[name] ?: return@forEach
        if (count <= 0L) return@forEach
        signals["耗时·$name"] = sumNanos / count.toDouble()
    }
    delta.counters.forEach { (name, value) ->
        signals["计数·$name"] = value.toDouble()
    }
    return signals
}

/**
 * 信号是否属于耗时类（受绝对噪声下限约束）。
 */
internal fun isTimingSignal(signalName: String): Boolean =
    signalName == WALL_CLOCK_SIGNAL || signalName.startsWith("耗时·")

/**
 * 用例墙钟信号名。
 */
internal const val WALL_CLOCK_SIGNAL: String = "用例墙钟"

/**
 * 基线对比。
 *
 * ## 基线怎么选
 *
 * 默认取**上一次**运行——这是开发者最直接的参照物。但如果上一次本身就是一次抖动，
 * 下一次就会跟着跑偏。因此历史攒到 [MIN_BASELINE_SAMPLES] 次之后自动改用
 * **最近 N 次的中位数**，中位数对单次离群不敏感。
 *
 * ## 为什么要抖动阈值
 *
 * 单次测量的噪声往往和"想看到的改进"同量级。没有阈值的话，任何一次 +3% 都会被当成回归，
 * 久而久之就没人再看这个报告了。因此阈值取「历史样本观测到的抖动」与「固定下限」的较大者，
 * 只有超过它才判定为变慢/变快，其余一律记为噪声。
 */
object CaPerformanceBaselineComparator {
    /**
     * 判定为回归所需的最小历史样本数：少于这个数无法估计抖动，只能说"数据不足"。
     */
    const val MIN_BASELINE_SAMPLES: Int = 3

    /**
 * 固定下限：即使历史样本完全稳定，也要求变化超过这个比例才算有意义。
 *
 * 默认取 20%：性能用例的 fixture 很小，单次测量在几十毫秒量级，JIT 与调度抖动带来的
 * 变化常在 10%–30%。下限定太低会把每一次抖动都报成回归，报多了这份报告就没人看了。
 *
 * 可用系统属性 [CHANGE_FLOOR_PROPERTY] 覆盖。
 */
const val DEFAULT_CHANGE_FLOOR: Double = 0.20

/**
 * 覆盖固定下限的系统属性名。
 */
const val CHANGE_FLOOR_PROPERTY: String = "cangjie.performance.changeFloor"

/**
 * 耗时类信号的绝对噪声下限（纳秒）。
 *
 * 相对变化只在数值有意义时才成立：一个 0.3ms 的采样跳到 0.7ms 是 +100%，但两次都落在
 * 计时与调度的噪声里，这种"回归"没有任何信息量。基线低于该下限时一律判为噪声。
 *
 * 计数器不受此约束——计数是精确值，3 变 4 就是真的变了。
 */
const val ABSOLUTE_NOISE_FLOOR_NANOS: Double = 1_000_000.0

    /**
 * 读取生效的固定下限：系统属性优先，非法值回退到默认。
 */
fun effectiveChangeFloor(): Double {
    val raw = System.getProperty(CHANGE_FLOOR_PROPERTY) ?: return DEFAULT_CHANGE_FLOOR
    val parsed = raw.trim().toDoubleOrNull() ?: return DEFAULT_CHANGE_FLOOR
    return parsed.coerceIn(0.0, 1.0)
}

    /**
     * 参与中位数与抖动估计的最大历史样本数。
     */
    const val MAX_BASELINE_SAMPLES: Int = 5

    /**
     * 对比一次运行与其之前的历史。
     *
     * @param current 本次运行
     * @param history 之前的运行，按时间升序（[CaPerformanceHistoryStore.readAll] 的顺序）
     * @param changeFloor 固定变化下限
     */
    fun compare(
        current: CaPerformanceRunRecord,
        history: List<CaPerformanceRunRecord>,
        changeFloor: Double = DEFAULT_CHANGE_FLOOR,
    ): CaPerformanceComparisonReport {
        val priorRuns = history.filter { it.timestampMillis < current.timestampMillis }

        // 即便一条历史都没有也照常产出用例条目：首次运行至少要让"这些用例已记录、
        // 暂时没有基线"可见，而不是交一份空报告。
        val cases = current.cases.map { case ->
            CaPerformanceCaseComparison(
                caseId = case.caseId,
                testClass = case.testClass,
                testMethod = case.testMethod,
                signals = compareCase(case, priorRuns, changeFloor),
            )
        }
        return CaPerformanceComparisonReport(current.runId, priorRuns.lastOrNull()?.runId, cases)
    }

    /**
     * 对比单个用例。
     */
    private fun compareCase(
        current: CaPerformanceCaseRecord,
        priorRuns: List<CaPerformanceRunRecord>,
        changeFloor: Double,
    ): List<CaPerformanceSignalComparison> {
        val currentSignals = current.toSignals()
        if (currentSignals.isEmpty()) return emptyList()

        // 只保留历史里也出现过的用例，否则基线无意义。
        val priorCaseSignals = priorRuns.mapNotNull { run ->
            run.casesById[current.caseId]?.toSignals()?.takeIf { it.isNotEmpty() }
        }
        if (priorCaseSignals.isEmpty()) {
            return currentSignals.map { (name, value) ->
                signalComparison(name, value, null, emptyList(), changeFloor)
            }
        }

        return currentSignals.map { (name, value) ->
            val samples = priorCaseSignals.mapNotNull { it[name] }.takeLast(MAX_BASELINE_SAMPLES)
            signalComparison(name, value, priorCaseSignals.last()[name], samples, changeFloor)
        }
    }

    /**
     * 单个信号的对比与判定。
     *
     * @param lastPriorValue 上一次运行的取值；历史里没有该信号时为 null
     * @param samples 最近若干次运行中该信号的取值，可能为空
     */
    private fun signalComparison(
        name: String,
        current: Double,
        lastPriorValue: Double?,
        samples: List<Double>,
        changeFloor: Double,
    ): CaPerformanceSignalComparison {
        if (lastPriorValue == null) {
            return CaPerformanceSignalComparison(
                signalName = name,
                current = current,
                baseline = 0.0,
                baselineSamples = 0,
                baselineDescription = "无基线",
                relativeChange = null,
                threshold = changeFloor,
                verdict = CaPerformanceVerdict.BASELINE_MISSING,
            )
        }

        // 样本不足时只能用上一次；够则用中位数，抵抗单次离群。
        val useMedian = samples.size >= MIN_BASELINE_SAMPLES
        val baseline = if (useMedian) median(samples) else lastPriorValue
        val threshold = maxOf(jitter(samples), changeFloor)
        val relativeChange = if (baseline != 0.0) (current - baseline) / baseline else null
        val belowResolutionFloor = isTimingSignal(name) && baseline < ABSOLUTE_NOISE_FLOOR_NANOS

        val verdict = when {
            !useMedian -> CaPerformanceVerdict.INSUFFICIENT_DATA
            relativeChange == null -> CaPerformanceVerdict.INSUFFICIENT_DATA
            // 基线本身在噪声底上时，相对变化再大也说明不了问题。
            belowResolutionFloor -> CaPerformanceVerdict.NOISE
            relativeChange > threshold -> CaPerformanceVerdict.REGRESSED
            relativeChange < -threshold -> CaPerformanceVerdict.IMPROVED
            else -> CaPerformanceVerdict.NOISE
        }

        return CaPerformanceSignalComparison(
            signalName = name,
            current = current,
            baseline = baseline,
            baselineSamples = samples.size,
            baselineDescription = if (useMedian) "近 ${samples.size} 次中位数" else "上一次",
            relativeChange = relativeChange,
            threshold = threshold,
            verdict = verdict,
        )
    }

    /**
     * 中位数；样本数为偶时取中间两个的平均。
     */
    internal fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }

    /**
     * 观测抖动：样本极差相对中位数的比例；样本不足或中位数为 0 时返回 0。
     *
     * 用极差而不是标准差：基线样本本来就很少（个位数），极差对单次离群更敏感，
     * 正是这里想避免的情况。
     */
    internal fun jitter(values: List<Double>): Double {
        if (values.size < MIN_BASELINE_SAMPLES) return 0.0
        val middle = median(values)
        if (middle <= 0.0) return 0.0
        return ((values.max() - values.min()) / middle)
    }
}