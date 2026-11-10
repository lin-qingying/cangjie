package org.cangnova.cangjie.ide.codeInsight.fixes

import com.intellij.codeInsight.intention.IntentionAction
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CaCfirDiagnostic
import org.cangnova.cangjie.codeinsight.api.applicators.fixes.CangJieQuickFixFactory
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.psi.CjMatchExpression

/**
 * 修饰符族诊断的 K2 quick-fix factory 集合。
 *
 * 这族诊断全部携带具体的 [CjKeywordToken]，且「删除该修饰符」是唯一安全动作：
 * 冗余/重复/不适用/互斥修饰符删除任何一个后语义都回到合法状态。
 */
object ModifierQuickFixFactories {
    /** 「删除修饰符 X」的展示文本模板。 */
    private fun removeText(token: CjKeywordToken): String = "Remove modifier '${token.value}'"

    /**
     * `RepeatedModifier`：重复修饰符（如 `public public`），删除重复项。
     */
    val repeatedModifier: CangJieQuickFixFactory.IntentionBased<CaCfirDiagnostic.RepeatedModifier> =
        CangJieQuickFixFactory.IntentionBased { diagnostic ->
            listOf(RemoveModifierQuickFix(diagnostic.psi, diagnostic.modifier, removeText(diagnostic.modifier)))
        }

    /**
     * `RedundantModifier`：被另一修饰符冗余（如 `open abstract`），删除冗余项。
     */
    val redundantModifier: CangJieQuickFixFactory.IntentionBased<CaCfirDiagnostic.RedundantModifier> =
        CangJieQuickFixFactory.IntentionBased { diagnostic ->
            listOf(RemoveModifierQuickFix(diagnostic.psi, diagnostic.modifier, removeText(diagnostic.modifier)))
        }

    /**
     * `WrongModifierTarget`：修饰符不适用于目标声明，删除该修饰符。
     */
    val wrongModifierTarget: CangJieQuickFixFactory.IntentionBased<CaCfirDiagnostic.WrongModifierTarget> =
        CangJieQuickFixFactory.IntentionBased { diagnostic ->
            listOf(RemoveModifierQuickFix(diagnostic.psi, diagnostic.modifier, removeText(diagnostic.modifier)))
        }

    /**
     * `WrongModifierContainingDeclaration`：修饰符不适用于所在容器，删除该修饰符。
     */
    val wrongModifierContainingDeclaration: CangJieQuickFixFactory.IntentionBased<CaCfirDiagnostic.WrongModifierContainingDeclaration> =
        CangJieQuickFixFactory.IntentionBased { diagnostic ->
            listOf(RemoveModifierQuickFix(diagnostic.psi, diagnostic.modifier, removeText(diagnostic.modifier)))
        }

    /**
     * `RedundantModifierForTarget`：对目标冗余的修饰符，删除。
     */
    val redundantModifierForTarget: CangJieQuickFixFactory.IntentionBased<CaCfirDiagnostic.RedundantModifierForTarget> =
        CangJieQuickFixFactory.IntentionBased { diagnostic ->
            listOf(RemoveModifierQuickFix(diagnostic.psi, diagnostic.modifier, removeText(diagnostic.modifier)))
        }

    /**
     * `DeprecatedModifierForTarget`：已废弃的修饰符用法，删除。
     */
    val deprecatedModifierForTarget: CangJieQuickFixFactory.IntentionBased<CaCfirDiagnostic.DeprecatedModifierForTarget> =
        CangJieQuickFixFactory.IntentionBased { diagnostic ->
            listOf(RemoveModifierQuickFix(diagnostic.psi, diagnostic.modifier, removeText(diagnostic.modifier)))
        }

    /**
     * `IncompatibleModifiers`：两个互斥修饰符。诊断不指明删哪个，
     * 保守地为每个修饰符各产出一条修复，由用户选择。
     */
    val incompatibleModifiers: CangJieQuickFixFactory.IntentionBased<CaCfirDiagnostic.IncompatibleModifiers> =
        CangJieQuickFixFactory.IntentionBased { diagnostic ->
            listOf(
                RemoveModifierQuickFix(diagnostic.psi, diagnostic.modifier1, removeText(diagnostic.modifier1)),
                RemoveModifierQuickFix(diagnostic.psi, diagnostic.modifier2, removeText(diagnostic.modifier2)),
            )
        }
}

/**
 * import / match / 弃用族诊断的 P1 quick-fix factory 集合。
 */
object P1QuickFixFactories {
    /**
     * `UnusedImport`：移除未使用的导入项。
     *
     * 诊断 PSI 是 [CjImportItem]；分组 import 中只删叶项，独立语句整句删除。
     */
    val unusedImport: CangJieQuickFixFactory.IntentionBased<CaCfirDiagnostic.UnusedImport> =
        CangJieQuickFixFactory.IntentionBased { diagnostic ->
            listOf(RemoveImportQuickFix(diagnostic.psi))
        }

    /**
     * `NonExhaustiveMatch`：按诊断给出的缺失 case 文本补分支。
     *
     * 缺失文本来自穷尽性分析器的规范化模式（如枚举构造器 `A`、通配 `_`），
     * 每个文本产出一条「补 `case => ()` 分支」修复。
     */
    val nonExhaustiveMatch: CangJieQuickFixFactory.IntentionBased<CaCfirDiagnostic.NonExhaustiveMatch> =
        CangJieQuickFixFactory.IntentionBased { diagnostic ->
            val matchExpression = diagnostic.psi as? CjMatchExpression
                ?: diagnostic.psi.psiTreeMatchExpression()
            val expression = matchExpression ?: return@IntentionBased emptyList()
            diagnostic.missingCases.map { missingCase ->
                AddMatchBranchQuickFix(expression, missingCase)
            }
        }

    /**
     * `DeprecatedWarning`：按注解 `replacement` 命名参数替换使用点。
     *
     * replacement 为空（注解未写 replacement）时不产出修复——
     * 「替换成什么」只能由声明方给出，猜测替换是危险的。
     */
    val deprecatedWarning: CangJieQuickFixFactory.IntentionBased<CaCfirDiagnostic.DeprecatedWarning> =
        CangJieQuickFixFactory.IntentionBased { diagnostic ->
            val replacement = diagnostic.replacement.trim()
            if (replacement.isEmpty()) return@IntentionBased emptyList()
            listOf(ReplaceWithQuickFix(diagnostic.psi, diagnostic.name.asString(), replacement))
        }

    /**
     * 诊断 PSI 不一定是 match 表达式本体（可能定位在 subject 上），
     * 向上回溯找所属 match。
     */
    private fun com.intellij.psi.PsiElement.psiTreeMatchExpression(): CjMatchExpression? =
        generateSequence(this) { it.parent }.firstOrNull { it is CjMatchExpression } as? CjMatchExpression
}
