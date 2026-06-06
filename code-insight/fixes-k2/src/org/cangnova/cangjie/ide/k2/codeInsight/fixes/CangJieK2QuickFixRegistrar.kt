package org.cangnova.cangjie.ide.k2.codeInsight.fixes

import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CaCfirDiagnostic
import org.cangnova.cangjie.codeinsight.api.applicators.fixes.CangJieQuickFixRegistrar
import org.cangnova.cangjie.codeinsight.api.applicators.fixes.CangJieQuickFixesList
import org.cangnova.cangjie.codeinsight.api.applicators.fixes.CangJieQuickFixesListBuilder
import org.cangnova.cangjie.ide.core.overrideImplement.MemberNotImplementedQuickfixFactories

/**
 * CangJie K2 quick-fix 总注册器。
 *
 * 对齐 Kotlin `code-insight/fixes-k2` 的 registrar 分层：本模块只负责把
 * K2 diagnostic 与具体 quick-fix factory 装配到 code-insight quick-fix 框架。
 */
class CangJieK2QuickFixRegistrar : CangJieQuickFixRegistrar() {
    override val list: CangJieQuickFixesList = CangJieQuickFixesListBuilder.registerQuickFixes {
        registerFactory(
            CaCfirDiagnostic.AbstractMemberNotImplemented::class,
            MemberNotImplementedQuickfixFactories.abstractMemberNotImplemented,
        )
    }
}
