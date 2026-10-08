package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider.DanglingFileFixturePreparation
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * dangling 夹具的**副本准备**逻辑（任务 2.4）。
 *
 * 背景：夹具里声明过 `IGNORE_SELF_MODE`，但准备逻辑从不读它——模式无从选择，
 * 「两种模式」这件事在测试里既跑不到也验不了。现在指令被消费（`PREFER_SELF_MODE`
 * 是仓颉侧新增的显式入口，因为仓颉默认仍是 IGNORE_SELF——PREFER_SELF 的**解析**路径
 * 被已记录的 LL 缺陷阻塞），准备逻辑也抽成可独立调用的
 * [DanglingFileFixturePreparation]，于是这里能**不跑分析**就把准备结果验干净。
 *
 * 本类只验「准备」：两种模式的形状、原件不变、副本保留原件声明。
 * 「PREFER_SELF 下的解析行为」是另一件事，受缺陷阻塞，不在这里假装验过。
 */
@OptIn(CaExperimentalApi::class)
class DanglingFileFixturePreparationTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/danglingFilePreparation",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 两种模式的准备结果、原件不变、副本保留原件声明。
     */
    @Test
    fun preparedCopyHonoursBothModes(mainFile: CjFile) {
        val originalTextBefore = mainFile.text

        for (mode in CaDanglingFileResolutionMode.entries) {
            val copy = DanglingFileFixturePreparation.prepareCopy(mainFile, mode)

            assertEquals(
                originalTextBefore,
                mainFile.text,
                "[$mode] 准备副本不得改动原件文本",
            )
            assertEquals(
                originalTextBefore,
                copy.text,
                "[$mode] 副本必须是原件文本的复制",
            )
            assertEquals(
                listOf("localHelper", "consumeIt"),
                copy.declarations.mapNotNull { it.name },
                "[$mode] 副本必须保留原件的顶层声明（否则 dangling 分析会凭空少声明）",
            )

            when (mode) {
                // IGNORE_SELF：副本把平台 `originalFile` 指向原件，非局部引用解析回原件。
                CaDanglingFileResolutionMode.IGNORE_SELF -> assertSame(
                    mainFile,
                    copy.originalFile,
                    "[$mode] 副本必须把 originalFile 指向原件",
                )

                // PREFER_SELF：对齐 Kotlin 夹具——**不**指向原件，只靠 contextModule 绑定上下文。
                // 平台语义：没设原件时 `originalFile` 返回自身。
                CaDanglingFileResolutionMode.PREFER_SELF -> assertSame(
                    copy,
                    copy.originalFile,
                    "[$mode] 不得把 originalFile 指向原件（那正是 IGNORE_SELF 的形状）",
                )
            }
        }
    }
}
