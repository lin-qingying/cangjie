package org.cangnova.cangjie.ide.codeInsight.fixes

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CaCfirDiagnostic
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.codeinsight.api.applicators.fixes.CangJieQuickFixFactory
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

/**
 * P1 quick-fix factory 的回归。
 *
 * 逐族锁定「诊断 → 修复」映射：
 * - 修饰符族必须产出删除修饰符修复；
 * - UnusedImport 必须产出移除 import 修复；
 * - NonExhaustiveMatch 必须为缺失 case 产出补分支修复；
 * - DeprecatedWarning 仅在注解给出 replacement 时产出替换修复。
 */
@OptIn(CaExperimentalApi::class)
class P1QuickFixesTest : AbstractAnalysisApiExecutionTest(
    "code-insight/code-actions/testData/fixes/p1",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 重复修饰符：产出一条「删除修饰符」修复。
     */
    @Test
    fun repeatedModifierProducesRemoveFix(repeatedModifier: CjFile) {
        val fixes = fixesFor<CaCfirDiagnostic.RepeatedModifier>(repeatedModifier)
        assertTrue(
            fixes.isNotEmpty() && fixes.all { it.text.startsWith("Remove modifier") },
            "重复修饰符必须产出删除修复，actual=${fixes.map { it.text }}",
        )
    }

    /**
     * 未使用导入：产出一条「移除未使用 import」修复。
     */
    @Test
    fun unusedImportProducesRemoveFix(unusedImport: CjFile) {
        val fixes = fixesFor<CaCfirDiagnostic.UnusedImport>(unusedImport)
        assertEquals(1, fixes.size, "未使用 import 必须产出恰好一条移除修复")
        assertEquals("Remove unused import", fixes.single().text)
    }

    /**
     * 非穷尽 match：为每个缺失 case 产出补分支修复。
     */
    @Test
    fun nonExhaustiveMatchProducesAddBranchFixes(nonExhaustiveMatch: CjFile) {
        val fixes = fixesFor<CaCfirDiagnostic.NonExhaustiveMatch>(nonExhaustiveMatch)
        assertTrue(
            fixes.isNotEmpty() && fixes.all { it.text.startsWith("Add match branch") },
            "非穷尽 match 必须产出补分支修复，actual=${fixes.map { it.text }}",
        )
    }

    /**
     * 弃用警告：仓颉 `@Deprecated` 注解官方只有 message/since/strict 参数
     * （无 Kotlin 的 ReplaceWith 概念，checker 上报的 replacement 恒为空串），
     * 因此不产出替换修复——锁定该降级行为，防止未来误产出猜测性替换。
     */
    @Test
    fun deprecatedWarningProducesReplaceFix(deprecatedWarning: CjFile) {
        val fixes = fixesFor<CaCfirDiagnostic.DeprecatedWarning>(deprecatedWarning)
        assertTrue(
            fixes.isEmpty(),
            "replacement 为空时不得产出猜测性替换修复，actual=${fixes.map { it.text }}",
        )
    }

    /*
     * 抽象成员实现族（AbstractMemberNotImplemented / InterfaceMemberMustBeImplemented）的
     * 「诊断 → Implement Members」映射不在本 standalone 套件断言：该诊断由
     * `classLikeNameOffsetsDiagnosticSource()` 以 CjOffsetsOnlySourceElement 锚点上报，
     * 而 low-level `LLCfirDiagnosticReporter.toPsiDiagnostic` 尚不能投影 offsets-only
     * 诊断（既有架构缺口，见 CfirOffsetsOnlyDiagnosticAnchorGuardTest 白名单），standalone
     * 路径收集不到它。映射行为由 IDE 产品套件 CangJieQuickFixRegistrationTest（走完整
     * 产品 checker + reporter 链）承担，factory 本身与 AbstractMemberNotImplemented
     * 场景共用同一 getUnimplementedMemberFixes 链路。
     */

    /**
     * 按诊断类型收集该类型的全部 quick fix。
     */
    private inline fun <reified D : CaCfirDiagnostic<*>> fixesFor(file: CjFile): List<com.intellij.codeInsight.intention.IntentionAction> {
        val diagnostic = analyzeForTest(file) {
            file.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
        }.filterIsInstance<D>().firstOrNull()
        assertNotNull(diagnostic, "夹具必须产出 ${D::class.simpleName} 诊断")

        val factory: CangJieQuickFixFactory<D> = factoryFor()
        return analyzeForTest(file) {
            @Suppress("UNCHECKED_CAST")
            with(factory) { createQuickFixes(diagnostic) } as List<com.intellij.codeInsight.intention.IntentionAction>
        }
    }

    /**
     * 取注册表里对应诊断类型的 factory；按类型分发到对应 factory 对象，
     * 与 registrar 注册保持同一来源。
     */
    @Suppress("UNCHECKED_CAST")
    private inline fun <reified D : CaCfirDiagnostic<*>> factoryFor(): CangJieQuickFixFactory<D> = when (D::class) {
        CaCfirDiagnostic.RepeatedModifier::class -> ModifierQuickFixFactories.repeatedModifier
        CaCfirDiagnostic.UnusedImport::class -> P1QuickFixFactories.unusedImport
        CaCfirDiagnostic.NonExhaustiveMatch::class -> P1QuickFixFactories.nonExhaustiveMatch
        CaCfirDiagnostic.DeprecatedWarning::class -> P1QuickFixFactories.deprecatedWarning
        else -> error("No factory mapped for ${D::class.simpleName}")
    } as CangJieQuickFixFactory<D>
}
