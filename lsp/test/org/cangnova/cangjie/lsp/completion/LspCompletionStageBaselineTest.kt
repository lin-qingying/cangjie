package org.cangnova.cangjie.lsp.completion

import com.intellij.openapi.components.service
import org.cangnova.cangjie.ide.completion.api.statistics.CangJieCompletionStatistics
import org.cangnova.cangjie.ide.completion.api.statistics.CompletionStage
import org.cangnova.cangjie.ide.completion.api.statistics.RecordingCompletionStatisticsSink
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.exists
import kotlin.io.path.inputStream

/**
 * 补全阶段耗时基线与门禁（任务 13.2 的「冷/热、小/大项目可复测基线及预算」）。
 *
 * 三件事钉在一处：
 * 1. **可复测**：夹具由代码生成——小 = 单文件工作区，大 = 同一工作区 + 200 个生成文件；
 *    每次跑出来的项目形状完全一样，两次测量才可比。
 * 2. **预算具名**：阈值不写在测试代码里，读 `code-insight/completion/completion-stage-budget.properties`
 *    ——「改阈值」与「改测量方式」在 diff 里不会再长得一样。
 * 3. **门禁只挡量级**：预算比实测中位数宽 5–10 倍，目的是让「退化成全工作区扫描」
 *    （13.1 明令禁止的那件事）这类事故必然触发，又不让并行构建的机器噪声误报。
 *    大投影的意义就在这里：候选若开始遍历整仓声明，文件一多耗时立刻上一个量级。
 *
 * 每次运行都把实测值按 `CJD-BASELINE …` 打印，便于与预算文件里的基线注释人工比对漂移。
 */
class LspCompletionStageBaselineTest : AbstractLspIntegrationTest() {
    /**
     * 本类自建会话：冷启动要有意义，每个 profile 都必须是一份全新的服务端。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 四个 profile 的阶段中位数都必须落在具名预算内。
     */
    @Test
    fun stageBudgetsHoldForSmallAndLargeWorkspaces() {
        val budgets = budgetFile()
        val samples = listOf(
            measureProfile("small.cold", warmRuns = WARM_RUNS, generatedFileCount = 0),
            measureProfile("large.cold", warmRuns = WARM_RUNS, generatedFileCount = LARGE_GENERATED_FILES),
        )

        for (sample in samples) {
            println(sample.report())
            val warmProfile = sample.profile.replace(".cold", ".warm")
            for (stage in CompletionStage.entries) {
                // 记录里存的是纳秒，预算文件里写的是毫秒：比较前必须换算，
                // 否则「215ms 超了 3000ms 预算」这种自相矛盾的失败信息就会冒出来。
                val coldNanos = sample.stageNanos[stage]
                if (coldNanos != null) {
                    val coldMillis = coldNanos / NANOS_PER_MILLI
                    val budget = budgets.require(sample.profile, stage)
                    assertTrue(
                        coldMillis <= budget,
                        "冷启动阶段超预算：${sample.profile} $stage 实测 ${coldMillis}ms > 预算 ${budget}ms",
                    )
                }
                val warmNanos = sample.warmStageMedians[stage]
                if (warmNanos != null) {
                    val warmMillis = warmNanos / NANOS_PER_MILLI
                    val budget = budgets.require(warmProfile, stage)
                    assertTrue(
                        warmMillis <= budget,
                        "热补全阶段超预算：$warmProfile $stage 实测 ${warmMillis}ms > 预算 ${budget}ms",
                    )
                }
            }
        }
    }

    /**
     * 在一个 profile 上测冷启动与热补全的阶段耗时。
     *
     * 冷 = 会话建立后的**第一次**补全（含分析会话冷启动）；热 = 其后 [warmRuns] 次的**中位数**。
     */
    private fun measureProfile(profile: String, warmRuns: Int, generatedFileCount: Int): ProfileSample {
        val builder = LspWorkspaceFixtureBuilder()
        repeat(generatedFileCount) { index ->
            builder.source(
                "generated/Gen$index.cj",
                "package generated" + NL + NL + "func generated$index(): Int64 { return $index }" + NL,
            )
        }
        val fixture = builder.source(USE_NAME, USE_SOURCE).build()

        var coldStageNanos = emptyMap<CompletionStage, Long>()
        val warmStageNanos = mutableListOf<Map<CompletionStage, Long>>()

        LspIntegrationTestConnection.create(defaultServerOptions()).use { connection ->
            connection.initialize(fixture.initializeParams())
            connection.initialized()
            val recorder = installRecordingSink(connection)
            val uri = fixture.uri(USE_FILE)
            connection.openDocument(uri, fixture.text(USE_FILE))
            val params = CompletionParams(TextDocumentIdentifier(uri), positionAt(fixture.text(USE_FILE)))

            // 记录式 sink 的 `stageNanos` 是**累计**值（同一次运行里多次补全会累加），
            // 因此每次都要取前后差，否则第二次开始的“测量”实际是前几次之和。
            var before = snapshotOf(recorder)
            connection.completion(params)
            coldStageNanos = deltaOf(before, snapshotOf(recorder))
            repeat(warmRuns) {
                before = snapshotOf(recorder)
                connection.completion(params)
                warmStageNanos += deltaOf(before, snapshotOf(recorder))
            }
        }

        val warmMedians = CompletionStage.entries.mapNotNull { stage ->
            val values = warmStageNanos.mapNotNull { it[stage] }.sorted()
            if (values.isEmpty()) null else stage to values[values.size / 2]
        }.toMap()
        return ProfileSample(profile, coldStageNanos, warmMedians, warmRuns)
    }

    /**
     * 取当前累计快照。
     */
    private fun snapshotOf(recorder: RecordingCompletionStatisticsSink): Map<CompletionStage, Long> =
        CompletionStage.entries.mapNotNull { stage -> recorder.stageNanos[stage]?.let { stage to it } }.toMap()

    /**
     * 由前后两次累计快照求单次补全的各阶段耗时。
     */
    private fun deltaOf(
        before: Map<CompletionStage, Long>,
        after: Map<CompletionStage, Long>,
    ): Map<CompletionStage, Long> = after.mapValues { (stage, value) -> value - (before[stage] ?: 0L) }

    /**
     * 装记录式 sink：阶段耗时只有从这里看得到（与统计接线用例同一接缝）。
     */
    private fun installRecordingSink(connection: LspIntegrationTestConnection): RecordingCompletionStatisticsSink {
        val recorder = RecordingCompletionStatisticsSink()
        connection.server.context.environment.project
            .service<CangJieCompletionStatistics>()
            .sink = recorder
        return recorder
    }

    /**
     * 读具名预算文件（按仓根定位，不依赖测试运行目录）。
     */
    private fun budgetFile(): Properties {
        var current: Path? = Path.of("").toAbsolutePath().normalize()
        while (current != null && !current.resolve("settings.gradle.kts").exists()) {
            current = current.parent
        }
        val root = current ?: error("找不到含 settings.gradle.kts 的仓根")
        val file = root.resolve(BUDGET_FILE_RELATIVE_PATH)
        assertTrue(file.exists(), "缺少具名预算文件：$file")
        return Properties().apply { file.inputStream().use { load(it) } }
    }

    private fun Properties.require(profile: String, stage: CompletionStage): Long =
        requireNotNull(getProperty("stage.$profile.${stage.name}")) {
            "预算文件缺少 stage.$profile.${stage.name}"
        }.trim().toLong()

    /**
     * 一次测量的结果：冷启动各阶段耗时 + 热补全各阶段中位数。
     */
    private data class ProfileSample(
        val profile: String,
        val stageNanos: Map<CompletionStage, Long>,
        val warmStageMedians: Map<CompletionStage, Long>,
        val warmRuns: Int,
    ) {
        /**
         * 打印成一行，供与预算文件里的基线注释比对。
         */
        fun report(): String {
            val cold = CompletionStage.entries.mapNotNull { stage ->
                stageNanos[stage]?.let { stage to it / 1_000_000 }
            }.joinToString(separator = ", ") { (stage, millis) -> "$stage=${millis}ms" }
            val warm = CompletionStage.entries.mapNotNull { stage ->
                warmStageMedians[stage]?.let { stage to it / 1_000_000 }
            }.joinToString(separator = ", ") { (stage, millis) -> "$stage=${millis}ms" }
            return "CJD-BASELINE profile=$profile 冷: $cold | 热（$warmRuns 次中位数）: $warm"
        }
    }

    companion object {
        /** 行尾：夹具源码用字符串拼，避免三引号带来的缩进歧义。 */
        private const val NL = "\n"

        /** 纳秒到毫秒的换算：记录侧用纳秒，预算文件用毫秒。 */
        private const val NANOS_PER_MILLI = 1_000_000L
        /**
         * 使用点文件的**源根相对名**（`source()` 收的是源根相对路径）。
         */
        private const val USE_NAME = "use.cj"

        /**
         * 使用点文件的**工作区相对路径**（`uri()`/`text()` 收的是这个，含 `src/` 前缀）。
         *
         * 两者不是同一个字符串：`source()` 写进源根之下，仓库相对路径才会带上源根名——
         * 混用会得到 `NoSuchFileException`。
         */
        private const val USE_FILE = "src/use.cj"

        /** 使用点源码：`let u =` 之后是补全位置，前缀为空。 */
        private const val USE_SOURCE = "package sample.click.base" + NL + NL +
            "func documented(): Int64 { return 0 }" + NL + NL +
            "func consume(): Int64 {" + NL + "    let u =" + NL + "    return 1" + NL + "}" + NL

        /** 大投影的生成文件数。 */
        private const val LARGE_GENERATED_FILES = 200

        /** 热补全重复次数（取中位数）。 */
        private const val WARM_RUNS = 5

        /** 具名预算文件相对仓根的路径。 */
        private const val BUDGET_FILE_RELATIVE_PATH = "code-insight/completion/completion-stage-budget.properties"


        /**
         * 补全位置：`let u =` 之后（与统计接线用例同一口径）。
         */
        private fun positionAt(text: String): Position {
            val offset = text.indexOf("let u =") + "let u =".length
            val prefix = text.substring(0, offset)
            return Position(prefix.count { it == '\n' }, offset - (prefix.lastIndexOf('\n') + 1))
        }
    }
}
