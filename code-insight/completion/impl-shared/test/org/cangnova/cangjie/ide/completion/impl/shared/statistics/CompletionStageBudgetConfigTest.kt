package org.cangnova.cangjie.ide.completion.impl.shared.statistics

import org.cangnova.cangjie.ide.completion.api.statistics.CompletionStage
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.test.assertTrue

/**
 * 阶段预算文件的覆盖守卫（任务 13.2）。
 *
 * 盯的是**配置与枚举漂移**：新增一个 `CompletionStage` 却忘了给预算，门禁就会静默地少管一个阶段——
 * 这种漏不会报错，只会让「阶段门禁」看起来覆盖齐全。因此这里按枚举逐个核对，而不是抽查几项。
 */
class CompletionStageBudgetConfigTest {
    /**
     * 每个阶段 × 每个 profile 都必须有预算，且量级关系成立。
     */
    @Test
    fun everyStageHasBudgetInEveryProfile() {
        val budgets = loadBudgetFile()

        for (profile in PROFILES) {
            for (stage in CompletionStage.entries) {
                val key = keyOf(profile, stage)
                val raw = budgets.getProperty(key)
                assertTrue(raw != null, "预算文件缺少条目 $key（新增阶段必须同时给预算）")
                val value = requireNotNull(raw).trim().toLong()
                assertTrue(value > 0, "预算必须为正：$key=$value")
            }
        }

        for (stage in CompletionStage.entries) {
            assertTrue(
                valueOf(budgets, "small.cold", stage) >= valueOf(budgets, "small.warm", stage),
                "冷启动预算不得低于热补全：$stage",
            )
            assertTrue(
                valueOf(budgets, "large.warm", stage) >= valueOf(budgets, "small.warm", stage),
                "大投影预算不得低于小投影：$stage",
            )
        }
    }

    /**
     * 读具名预算文件（仓根相对路径，避免依赖测试运行目录）。
     */
    private fun loadBudgetFile(): Properties {
        var current: Path? = Path.of("").toAbsolutePath().normalize()
        while (current != null && !current.resolve("settings.gradle.kts").exists()) {
            current = current.parent
        }
        val root = current ?: error("找不到含 settings.gradle.kts 的仓根")
        val file = root.resolve(BUDGET_FILE_RELATIVE_PATH)
        assertTrue(file.exists(), "缺少具名预算文件：$file")
        return Properties().apply { file.inputStream().use { load(it) } }
    }

    private fun valueOf(budgets: Properties, profile: String, stage: CompletionStage): Long =
        requireNotNull(budgets.getProperty(keyOf(profile, stage))).trim().toLong()

    private fun keyOf(profile: String, stage: CompletionStage): String = "stage.$profile.${stage.name}"

    private companion object {
        /** 四个 profile：小/大投影 × 冷/热。 */
        private val PROFILES = listOf("small.cold", "small.warm", "large.cold", "large.warm")

        /** 具名预算文件相对仓根的路径。 */
        private const val BUDGET_FILE_RELATIVE_PATH = "code-insight/completion/completion-stage-budget.properties"
    }
}
