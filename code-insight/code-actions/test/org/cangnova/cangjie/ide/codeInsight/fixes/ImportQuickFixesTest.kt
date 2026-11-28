package org.cangnova.cangjie.ide.codeInsight.fixes

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CaCfirDiagnostic
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

/**
 * import 族 quick-fix factory 的回归。
 *
 * 断言到「factory 产出的修复文本」为止：写入行为本身由 `PsiImportPlanApplier`
 * 与 IDE 侧 light fixture 覆盖，这里锁定的是「诊断 → 修复」映射这一新增接缝。
 */
@OptIn(CaExperimentalApi::class)
class ImportQuickFixesTest : AbstractAnalysisApiExecutionTest(
    "code-insight/code-actions/testData/fixes/imports",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * `UseExprWithoutImport`：必须为诊断给出的 importPath 产出「补 import」修复。
     */
    @Test
    fun useExprWithoutImportProducesAddImportFix(useExprWithoutImport: CjFile) {
        val diagnostic = collectSingleDiagnostic<CaCfirDiagnostic.UseExprWithoutImport>(useExprWithoutImport)

        val fixes: List<com.intellij.codeInsight.intention.IntentionAction> = analyzeForTest(useExprWithoutImport) {
            ImportQuickFixFactories.useExprWithoutImport.run { createQuickFixes(diagnostic) }
        }

        assertEquals(1, fixes.size, "必须产出恰好一条修复")
        assertTrue(
            fixes.single().text.contains("std.ast"),
            "修复文本必须指向诊断给出的路径，实际 ${fixes.single().text}",
        )
    }

    /**
     * `UnresolvedImport`：必须产出「移除该 import」修复。
     */
    @Test
    fun unresolvedImportProducesRemoveFix(unresolvedImport: CjFile) {
        val diagnostic = collectSingleDiagnostic<CaCfirDiagnostic.UnresolvedImport>(unresolvedImport)

        val fixes: List<com.intellij.codeInsight.intention.IntentionAction> = analyzeForTest(unresolvedImport) {
            ImportQuickFixFactories.unresolvedImport.run { createQuickFixes(diagnostic) }
        }

        assertEquals(1, fixes.size, "必须产出恰好一条修复")
        assertEquals("Remove unresolved import", fixes.single().text)
    }

    /**
     * 分组 import 中的未解析项：修复对象必须挂在可整体删除的导入叶项上，
     * 而不是诊断定位到的失败段（如组内某个局部名或顶层段）。
     */
    @Test
    fun unresolvedImportInGroupTargetsDeletableEntry(unresolvedGroupImport: CjFile) {
        val diagnostic = collectSingleDiagnostic<CaCfirDiagnostic.UnresolvedImport>(unresolvedGroupImport)

        analyzeForTest(unresolvedGroupImport) {
            val fixes = ImportQuickFixFactories.unresolvedImport.run { createQuickFixes(diagnostic) }
            assertEquals(1, fixes.size, "分组 import 诊断同样必须产出修复")
        }
    }

    /**
     * 按类型收集夹具中唯一的目标诊断。
     */
    private inline fun <reified D : CaCfirDiagnostic<*>> collectSingleDiagnostic(file: CjFile): D {
        val diagnostic = analyzeForTest(file) {
            file.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
        }.filterIsInstance<D>().firstOrNull()
        return assertNotNull(diagnostic, "夹具必须产出 ${D::class.simpleName} 诊断")
    }
}
