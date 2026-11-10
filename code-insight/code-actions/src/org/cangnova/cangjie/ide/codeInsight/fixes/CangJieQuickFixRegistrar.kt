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
     * K2 diagnostic 到 quick-fix factory 的注册表（轻修复，高亮期同步计算）。
     */
    override val list: CangJieQuickFixesList = CangJieQuickFixesListBuilder.registerQuickFixes {
        // ---- import 族 ----
        registerFactory(CaCfirDiagnostic.UnresolvedImport::class, ImportQuickFixFactories.unresolvedImport)
        registerFactory(CaCfirDiagnostic.UseExprWithoutImport::class, ImportQuickFixFactories.useExprWithoutImport)
        registerFactory(CaCfirDiagnostic.UnusedImport::class, P1QuickFixFactories.unusedImport)

        // ---- 修饰符族 ----
        registerFactory(CaCfirDiagnostic.RepeatedModifier::class, ModifierQuickFixFactories.repeatedModifier)
        registerFactory(CaCfirDiagnostic.RedundantModifier::class, ModifierQuickFixFactories.redundantModifier)
        registerFactory(CaCfirDiagnostic.IncompatibleModifiers::class, ModifierQuickFixFactories.incompatibleModifiers)
        registerFactory(CaCfirDiagnostic.WrongModifierTarget::class, ModifierQuickFixFactories.wrongModifierTarget)
        registerFactory(
            CaCfirDiagnostic.WrongModifierContainingDeclaration::class,
            ModifierQuickFixFactories.wrongModifierContainingDeclaration,
        )
        registerFactory(
            CaCfirDiagnostic.RedundantModifierForTarget::class,
            ModifierQuickFixFactories.redundantModifierForTarget,
        )
        registerFactory(
            CaCfirDiagnostic.DeprecatedModifierForTarget::class,
            ModifierQuickFixFactories.deprecatedModifierForTarget,
        )

        // ---- match / 弃用族 ----
        registerFactory(CaCfirDiagnostic.NonExhaustiveMatch::class, P1QuickFixFactories.nonExhaustiveMatch)
        registerFactory(CaCfirDiagnostic.DeprecatedWarning::class, P1QuickFixFactories.deprecatedWarning)
    }

    /**
     * import-on-the-fly 注册表：AddImport 类修复同时可用作「修复即补 import」通道。
     *
     * 与 Kotlin K2 对齐——补 import 的 factory 同时出现在普通通道与
     * import-on-the-fly 通道，后者由 IDE 高亮链在构建 HighlightInfo 时合并消费。
     */
    override val importOnTheFlyList: CangJieQuickFixesList = CangJieQuickFixesListBuilder.registerQuickFixes {
        registerFactory(
            CaCfirDiagnostic.UseExprWithoutImport::class,
            ImportQuickFixFactories.useExprWithoutImport,
        )
    }

    /**
     * 延迟 quick-fix 注册表：成员生成类重修复需全量 resolve 成员候选，
     * 迁到 lazy 通道后仅在用户点开 Alt+Enter 时才计算，避免拖慢整份高亮。
     *
     * 抽象成员实现族中只有「类实现接口」场景（`AbstractMemberNotImplemented` /
     * `InterfaceMemberMustBeImplemented` 的 class 上报点）的 PSI 挂在类型声明上、
     * 可复用 `CaClassSymbol` 成员收集；extend（`NeedMemberImplementation`）与
     * specific（`SpecificMemberMustHaveImplementation`）场景的符号模型不同，
     * 待成员生成器扩展后另行注册。
     */
    override val lazyList: CangJieQuickFixesList = CangJieQuickFixesListBuilder.registerQuickFixes {
        registerFactory(
            CaCfirDiagnostic.AbstractMemberNotImplemented::class,
            MemberNotImplementedQuickfixFactories.abstractMemberNotImplemented,
        )
        registerFactory(
            CaCfirDiagnostic.InterfaceMemberMustBeImplemented::class,
            MemberNotImplementedQuickfixFactories.interfaceMemberMustBeImplemented,
        )
    }
}
