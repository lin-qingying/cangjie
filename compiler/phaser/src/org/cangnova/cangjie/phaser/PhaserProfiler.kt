package org.cangnova.cangjie.phaser

/**
 * 一次命名 phase 的耗时记录。
 *
 * @property name phase 名。
 * @property depth phase 主体执行时的嵌套深度；顶层 phase 为 0。
 * @property sequence 主体开始执行的序号，用于把"结束才采集"的记录还原成开始顺序。
 * @property elapsedNanos phase 主体耗时（纳秒，含其内部嵌套 phase 的耗时）。
 * @property completed 主体是否正常返回；`false` 表示以异常结束，耗时为已消耗部分。
 */
data class PhaserPhaseTiming(
    val name: String,
    val depth: Int,
    val sequence: Long,
    val elapsedNanos: Long,
    val completed: Boolean,
)

/**
 * 编译器阶段耗时采集器。
 *
 * 它是 [PhaseConfig] 上"是否采集阶段耗时"的唯一载体：配置里挂上本采集器就采集，没挂就完全
 * 不计时。这取代了此前 `PhaseConfig.needProfiling` + `println` 的做法——后者即使打开也只会往
 * stdout 打一行，既无法汇总也无法落盘，且与编译器正常的 stdout 输出混在一起。
 *
 * 采集器本身不做输出：[renderReport] 返回报告文本，由调用方决定写 stderr 还是写文件，
 * 这样 phaser 模块不需要知道消息通道，报告格式也只有一个来源。
 *
 * 线程安全性：编译前端按 session 分发工作，一个采集器实例可能被多个线程写入，
 * 因此写入与读取都在同步块内完成。
 */
class PhaserProfiler {
    /**
     * 已采集的 phase 耗时，按记录顺序（完成顺序：嵌套先于父）排列。
     */
    private val recorded = mutableListOf<PhaserPhaseTiming>()

    /**
     * 下一次领取的执行序号。
     */
    private var nextSequence: Long = 0

    /**
     * 为主体开始执行领取一个序号。
     *
     * 采集发生在主体**结束**时（只有结束时才知道耗时），因此按记录顺序读到的是"完成顺序"：
     * 嵌套 phase 一定先于父 phase 落库。序号让 [timings] 能按"开始顺序"返回，
     * 报告才是从外到内的树形，而不是每行都缩进一层。
     */
    fun nextSequence(): Long = synchronized(recorded) { nextSequence++ }

    /**
     * 记录一次 phase 耗时。
     *
     * 失败的 phase 也要记录：崩在哪个 phase、崩之前花了多久，正是最需要看到的信息。
     */
    fun record(timing: PhaserPhaseTiming) {
        synchronized(recorded) {
            recorded.add(timing)
        }
    }

    /**
     * 返回已采集的 phase 耗时快照，按主体开始执行的先后排序。
     */
    fun timings(): List<PhaserPhaseTiming> = synchronized(recorded) { recorded.sortedBy { it.sequence } }

    /**
 * 按主体开始执行的顺序渲染人类可读报告。
     *
     * 每个 phase 一行、按嵌套深度缩进，耗时是**含子 phase 的自身总耗时**；末尾给出顶层
     * phase 合计。父 phase 与子 phase 的耗时会重复计入合计，这是有意为之：合计表示
     * "顶层阶段占了多少时间"，而不是把嵌套结构相加。
     */
    fun renderReport(): String {
        val timings = timings()
        if (timings.isEmpty()) return EMPTY_REPORT

        val builder = StringBuilder()
        for (timing in timings) {
            builder.append("\t".repeat(timing.depth))
            builder.append(timing.name).append(": ")
            builder.append(formatMillis(timing.elapsedNanos))
            if (!timing.completed) builder.append(" (failed)")
            builder.append('\n')
        }
        val totalNanos = timings.filter { it.depth == 0 }.sumOf { it.elapsedNanos }
        builder.append("total: ").append(formatMillis(totalNanos)).append('\n')
        return builder.toString()
    }

    /**
     * 把纳秒格式化为毫秒，保留三位小数。
     *
     * 用纳秒计时而非 `measureTimeMillis`：前端单个 phase 常常在亚毫秒量级，整数毫秒会把
     * 大量 phase 显示成 `0 msec`，无法区分快慢。
     */
    private fun formatMillis(elapsedNanos: Long): String {
        val millis = elapsedNanos.toDouble() / NANOS_PER_MILLI
        return String.format("%.3f ms", millis)
    }

    private companion object {
        private const val NANOS_PER_MILLI = 1_000_000.0

        private const val EMPTY_REPORT = "no phase was recorded\n"
    }
}
