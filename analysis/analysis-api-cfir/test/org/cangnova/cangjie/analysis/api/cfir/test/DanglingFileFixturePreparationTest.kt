package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider.DanglingFileFixturePreparation
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider.DanglingFileTestDirectives
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.RegisteredDirectivesImpl
import org.cangnova.cangjie.test.directives.model.StringDirective
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * dangling 夹具的**副本准备与模式选择**逻辑（任务 2.4）。
 *
 * 背景：夹具里声明过 `IGNORE_SELF_MODE`，但准备逻辑从不读它——模式无从选择，
 * 「两种模式」这件事在测试里既跑不到也验不了。现在指令被消费
 * （[DanglingFileFixturePreparation.resolutionMode] + `PREFER_SELF_MODE` 指令，
 * 后者是仓颉侧显式入口：Kotlin 默认即 PREFER_SELF，仓颉默认仍是 IGNORE_SELF），
 * 准备逻辑也抽成可独立调用的 [DanglingFileFixturePreparation]，于是这里能
 * **不跑分析**就把准备结果与指令映射验干净。
 *
 * 本类只验「准备与选模式」；「两种模式下的解析行为」由 `AnalysisApiDanglingFileExecutionTest`
 * 按解析目标验（PREFER_SELF→副本、IGNORE_SELF→原件），这里不重复也不假装验过。
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

        assertResolutionModeMapping()
    }

    /**
     * 指令→模式映射：**声明了就必须被消费**。
     *
     * 2.4 的原始缺陷正是「指令声明了却没人读」——那时两种模式在测试里既跑不到也验不了。
     * 这三条把映射本身钉住：默认、两种显式声明、以及同时声明时的拒绝。
     */
    private fun assertResolutionModeMapping() {
        fun directives(vararg declared: StringDirective): RegisteredDirectives =
            RegisteredDirectivesImpl(
                emptyList(),
                declared.associateWith { emptyList<String>() },
                emptyMap(),
            )

        assertEquals(
            CaDanglingFileResolutionMode.IGNORE_SELF,
            DanglingFileFixturePreparation.resolutionMode(directives()),
            "未声明模式指令时必须落到默认的 IGNORE_SELF",
        )
        assertEquals(
            CaDanglingFileResolutionMode.PREFER_SELF,
            DanglingFileFixturePreparation.resolutionMode(directives(DanglingFileTestDirectives.PREFER_SELF_MODE)),
            "PREFER_SELF_MODE 必须被消费成 PREFER_SELF，不能静默落回默认值",
        )
        assertEquals(
            CaDanglingFileResolutionMode.IGNORE_SELF,
            DanglingFileFixturePreparation.resolutionMode(directives(DanglingFileTestDirectives.IGNORE_SELF_MODE)),
            "IGNORE_SELF_MODE 必须被消费成 IGNORE_SELF",
        )
        assertThrows(IllegalArgumentException::class.java) {
            DanglingFileFixturePreparation.resolutionMode(
                directives(DanglingFileTestDirectives.IGNORE_SELF_MODE, DanglingFileTestDirectives.PREFER_SELF_MODE),
            )
        }
    }
}
