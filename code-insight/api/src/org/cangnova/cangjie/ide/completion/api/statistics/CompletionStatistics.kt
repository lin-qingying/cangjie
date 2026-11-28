package org.cangnova.cangjie.ide.completion.api.statistics

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project

/**
 * 补全管线里被测量的阶段。
 *
 * 阶段划分按「用户等的那段时间花在哪」来定，而不是按代码结构：
 * 用户在弹窗出现前等的是 [SETUP]..[CANDIDATES]，接受候选后等的是 [INSERT]。
 */
enum class CompletionStage {
    /** 请求准备：位置定位、dummy 修正、副本构造。 */
    SETUP,

    /** 位置上下文与作用域计算。 */
    CONTEXT,

    /** 候选生成（各区段执行 + 去重 + 过滤 + 排序）。 */
    CANDIDATES,

    /** 名称索引查询。 */
    INDEX_QUERY,

    /** 候选渲染成界面/协议对象。 */
    RENDER,

    /** 接受候选时的插入计算与写入。 */
    INSERT,

    /**
     * 延迟解析（`completionItem/resolve`）：按候选身份重进分析、取回文档这类按需补充的信息。
     *
     * 与 [CANDIDATES] 分开记：resolve 是**用户触发**的第二次分析，它的耗时直接体现在
     * 「用户按下箭头时卡不卡」上，混进候选生成会看不出来。
     */
    RESOLVE,
}

/**
 * 候选没有被交付出去的原因。
 *
 * 这些**不是错误**，而是分流结果：记下来才能回答「用户没看到候选，是位置不对、
 * 服务没装、还是他自己取消了」——这几种情况的处置完全不同。
 */
enum class CompletionRejection {
    /** 请求被 `$/cancelRequest` 取消。 */
    CANCELLED,

    /** 补全会话已过期（被淘汰或文档已变）。 */
    SESSION_EXPIRED,

    /** 会话仍在但文档版本不一致。 */
    VERSION_MISMATCH,

    /** 宿主没有可用的补全服务实现。 */
    SERVICE_UNAVAILABLE,

    /** 位置不允许补全（原始字符串内部等）。 */
    POSITION_NOT_ALLOWED,
}

/**
 * 补全统计接缝。
 *
 * 三条硬约束：
 * 1. **只记数值**——耗时、计数、枚举原因；源码、文档内容、候选文本一律不进统计；
 * 2. **不阻塞主路径**——实现必须是非阻塞的（计数/埋点），不能把弹窗卡在写指标上；
 * 3. **宿主可选择不装**——没有统计实现时用 [NoopCompletionStatisticsSink]，调用点不分支。
 */
interface CompletionStatisticsSink {
    /**
     * 记录一次阶段耗时。
     */
    fun recordStage(stage: CompletionStage, nanos: Long)

    /**
     * 记录一次候选量：算出来多少、最终交付多少。
     *
     * 两个数分开记：只记「交付数」看不出过滤吃掉了多少，只记「产出数」看不出交付折损。
     */
    fun recordCandidates(produced: Int, delivered: Int)

    /**
     * 记录一次「候选没有交付」的原因。
     */
    fun recordRejection(reason: CompletionRejection)
}

/**
 * 什么都不记。
 *
 * 宿主没有装配统计实现（或用户关掉了遥测）时使用；调用点因此不必到处判空。
 */
object NoopCompletionStatisticsSink : CompletionStatisticsSink {
    override fun recordStage(stage: CompletionStage, nanos: Long) = Unit

    override fun recordCandidates(produced: Int, delivered: Int) = Unit

    override fun recordRejection(reason: CompletionRejection) = Unit
}

/**
 * 记录到内存的统计实现。
 *
 * 用途有二：测试里断言「该记的都记了」；宿主自检（例如 IDE 的统计窗口）不必接遥测后端。
 * **不做线程安全包装**：调用点可能来自多个线程，但使用者是测试与自检，
 * 需要跨线程一致性时由调用方自行同步或用并发容器替换。
 */
class RecordingCompletionStatisticsSink : CompletionStatisticsSink {
    /**
     * 各阶段累计耗时（纳秒）。
     */
    val stageNanos: MutableMap<CompletionStage, Long> = linkedMapOf()

    /**
     * 各阶段被记录的次数。
     */
    val stageCounts: MutableMap<CompletionStage, Int> = linkedMapOf()

    /**
     * 各拒绝原因的次数。
     */
    val rejections: MutableMap<CompletionRejection, Int> = linkedMapOf()

    /**
     * 最近一次候选量记录。
     */
    var lastProduced: Int = 0

    /**
     * 最近一次交付量记录。
     */
    var lastDelivered: Int = 0

    override fun recordStage(stage: CompletionStage, nanos: Long) {
        stageNanos[stage] = (stageNanos[stage] ?: 0L) + nanos
        stageCounts[stage] = (stageCounts[stage] ?: 0) + 1
    }

    override fun recordCandidates(produced: Int, delivered: Int) {
        lastProduced = produced
        lastDelivered = delivered
    }

    override fun recordRejection(reason: CompletionRejection) {
        rejections[reason] = (rejections[reason] ?: 0) + 1
    }
}

/**
 * 项目级补全统计装配点。
 *
 * 宿主（IDE 插件、LSP 注册器）在装配阶段设置 [sink]；未设置时是 [NoopCompletionStatisticsSink]。
 * 放在项目服务里而不是全局单例：统计的生命周期与工程一致，测试也能按工程替换实现。
 */
@Service(Service.Level.PROJECT)
class CangJieCompletionStatistics {
    /**
     * 当前生效的统计实现。
     */
    @Volatile
    var sink: CompletionStatisticsSink = NoopCompletionStatisticsSink
}

/**
 * 取当前工程的补全统计实现。
 *
 * **宿主没注册统计服务时返回 [NoopCompletionStatisticsSink]，绝不抛异常**：
 * 统计是旁路信号，缺了它最多是没数据；让它把被测的补全功能弄挂
 * （实测过：轻量夹具没注册该服务时 `project.service()` 直接抛，8 个补全用例一起红）
 * 是主次颠倒。
 */
fun completionStatistics(project: Project): CompletionStatisticsSink =
    project.serviceOrNull<CangJieCompletionStatistics>()?.sink ?: NoopCompletionStatisticsSink
