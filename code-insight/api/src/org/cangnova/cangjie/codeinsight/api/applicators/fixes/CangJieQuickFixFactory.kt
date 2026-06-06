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
 */
fun interface CangJieQuickFixFactory<DIAGNOSTIC : CaDiagnosticWithPsi<*>> {
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
}
