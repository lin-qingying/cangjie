package org.cangnova.cangjie.codeinsight.api.applicators.fixes

import com.intellij.codeInsight.intention.CommonIntentionAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.modcommand.ModCommandAction
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi

/**
 * CangJie K2 quick-fix factory 协议。
 *
 * factory 只在既有 [CaSession] 中工作，不跨层持有 analysis 结果。
 *
 * 同一诊断注册多个 factory 时按 [priority] 升序产出修复（值小的排前面），
 * 未声明优先级的 factory 默认 [DEFAULT_PRIORITY]，保持注册顺序稳定。
 */
fun interface CangJieQuickFixFactory<DIAGNOSTIC : CaDiagnosticWithPsi<*>> {
    /** 修复在 Alt+Enter 列表中的排序权重；值越小越靠前。 */
    val priority: Int
        get() = DEFAULT_PRIORITY

    fun CaSession.createQuickFixes(diagnostic: DIAGNOSTIC): List<CommonIntentionAction>

    /**
     * 生成传统 [IntentionAction] 的 factory。
     */
    fun interface IntentionBased<DIAGNOSTIC : CaDiagnosticWithPsi<*>> : CangJieQuickFixFactory<DIAGNOSTIC> {
        override fun CaSession.createQuickFixes(diagnostic: DIAGNOSTIC): List<IntentionAction>
    }

    /**
     * 生成 [ModCommandAction] 的 factory。
     */
    fun interface ModCommandBased<DIAGNOSTIC : CaDiagnosticWithPsi<*>> : CangJieQuickFixFactory<DIAGNOSTIC> {
        override fun CaSession.createQuickFixes(diagnostic: DIAGNOSTIC): List<ModCommandAction>
    }

    companion object {
        /** 默认优先级：未显式声明排序权重的 factory 共用该值。 */
        const val DEFAULT_PRIORITY: Int = 100
    }
}
