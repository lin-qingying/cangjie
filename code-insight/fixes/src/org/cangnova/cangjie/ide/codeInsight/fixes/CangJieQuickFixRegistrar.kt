package org.cangnova.cangjie.ide.codeInsight.fixes

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
class CangJieQuickFixRegistrar : CangJieQuickFixRegistrar() {
    /**
     * K2 diagnostic 到 quick-fix factory 的注册表。
     */
    override val list: CangJieQuickFixesList = CangJieQuickFixesListBuilder.registerQuickFixes {
        registerFactory(
            CaCfirDiagnostic.UnresolvedImport::class,
            ImportQuickFixFactories.unresolvedImport,
        )
        registerFactory(
            CaCfirDiagnostic.UseExprWithoutImport::class,
            ImportQuickFixFactories.useExprWithoutImport,
        )
    }

    /**
     * 延迟 quick-fix 注册表：成员生成类重修复需全量 resolve 成员候选，
     * 迁到 lazy 通道后仅在用户点开 Alt+Enter 时才计算，避免拖慢整份高亮。
     */
    override val lazyList: CangJieQuickFixesList = CangJieQuickFixesListBuilder.registerQuickFixes {
        registerFactory(
            CaCfirDiagnostic.AbstractMemberNotImplemented::class,
            MemberNotImplementedQuickfixFactories.abstractMemberNotImplemented,
        )
    }
}
