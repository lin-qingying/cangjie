package org.cangnova.cangjie.ide.codeInsight.fixes

import com.intellij.codeInsight.intention.IntentionAction
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CaCfirDiagnostic
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.codeinsight.api.applicators.fixes.CangJieQuickFixFactory
import org.cangnova.cangjie.psi.CjFile

/**
 * quick-fix factory 测试断言工具（F6）。
 *
 * 把「收集诊断 → 按类型筛选 → 在 session 内跑 factory」三步收敛为一个入口，
 * 新增修复的测试只需声明「期望的诊断类型 + factory」，断言行数压缩到 1–3 行。
 *
 * 用法（JUnit 5 测试类继承分析测试基座后）：
 * ```
 * val fixes = analyzeForTest(file) {
 *     QuickFixTestSupport.fixesFor<CaCfirDiagnostic.UnusedImport>(
 *         file.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS),
 *         P1QuickFixFactories.unusedImport,
 *     )
 * }
 * QuickFixTestSupport.assertSingleFix(fixes, "Remove unused import")
 * ```
 */
object QuickFixTestSupport {

    /**
     * 从诊断集合中筛选首个 [D] 类型诊断并运行 [factory]，返回产出的修复列表。
     *
     * 必须在 `CaSession` 接收者内调用（`analyzeForTest(file) { ... }`）：
     * factory 协议 `createQuickFixes` 是 session 的扩展成员，离开 session 无法运行。
     */
    inline fun <reified D : CaCfirDiagnostic<*>> CaSession.fixesFor(
        diagnostics: Collection<CaDiagnosticWithPsi<*>>,
        factory: CangJieQuickFixFactory<D>,
    ): List<IntentionAction> {
        val diagnostic = diagnostics.filterIsInstance<D>().firstOrNull()
            ?: error("夹具必须产出 ${D::class.simpleName} 诊断")

        @Suppress("UNCHECKED_CAST")
        return with(factory) { createQuickFixes(diagnostic) } as List<IntentionAction>
    }

    /**
     * 断言 [fixes] 恰好产出一条修复，且文本以 [expectedTextPrefix] 开头。
     */
    fun assertSingleFix(fixes: List<IntentionAction>, expectedTextPrefix: String) {
        check(fixes.size == 1) { "必须产出恰好一条修复，实际 ${fixes.map { it.text }}" }
        check(fixes.single().text.startsWith(expectedTextPrefix)) {
            "修复文本必须以 '$expectedTextPrefix' 开头，实际 ${fixes.single().text}"
        }
    }

    /**
     * 断言 [fixes] 为空（降级场景锁定）。
     */
    fun assertNoFix(fixes: List<IntentionAction>) {
        check(fixes.isEmpty()) { "不得产出修复，实际 ${fixes.map { it.text }}" }
    }
}

/**
 * 收集文件级普通诊断的便捷入口；在 session 内调用，与高亮链路同一过滤器。
 */
fun CaSession.collectCommonDiagnostics(file: CjFile): Collection<CaDiagnosticWithPsi<*>> =
    file.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
