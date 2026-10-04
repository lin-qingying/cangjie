package org.cangnova.cangjie.ide.completion.impl.shared.dummy

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.ide.completion.impl.shared.context.CaCompletionPositionContexts
import org.cangnova.cangjie.ide.completion.impl.shared.context.CaCompletionPositionKind
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.CjStringTemplateExpression

/**
 * 补全位置的 dummy 修正结果。
 *
 * @param dummyFile 平台插入 dummy 后的**文件副本**；分析必须在这个副本上进行，
 *   原件绝不能被写入，否则 dummy 文本会泄漏到用户文档。
 * @param position 副本中经过占位替换后、可用于语义分析的元素。
 * @param prefix 光标处的真实前缀，已去除 dummy 文本与限定名。
 */
data class CaCompletionDummyContext(
    val dummyFile: CjFile,
    val position: CjElement,
    val prefix: String,
)

/**
 * 仓颉补全的 dummy 标识符修正。
 *
 * 平台在调用 contributor 之前已把 `IntellijIdeaRulezzz` 插进**文件副本**，
 * 但这串文本在仓颉语法里通常不构成合法表达式：未完成的调用 `toDummy`、
 * 未闭合的实参列表都会让解析与作用域解析失败，结果是补全候选为空——
 * 而且不报任何错，这类失败最难定位。
 *
 * 因此这里按位置形态把 dummy 换成**语法合法**的占位表达式，再交给语义分析。
 * 只处理仓颉确实需要的形态：不复制 Kotlin 的 KDoc、`$name` 或尾随 lambda 规则；
 * 原始字符串不插值，其中的位置直接排除。
 */
object CaCompletionDummyIdentifierProvider {
    /**
     * 平台 dummy 标识符的稳定标记。
     */
    private const val DUMMY_MARKER = "IntellijIdeaRulezzz"

    /**
     * 对平台参数执行 dummy 修正，返回可用于分析的上下文。
     *
     * 返回 `null` 表示该位置不允许补全：调用方必须停止，
     * 绝不能在仍含裸 dummy 的 PSI 上继续分析。
     */
    fun provide(parameters: CompletionParameters): CaCompletionDummyContext? {
        val position = parameters.position.nearestCangJieElement() ?: return null
        val dummyFile = position.containingCjFile ?: return null
        if (position.isInsideRawStringLiteral()) return null

        val kind = CaCompletionPositionContexts.classify(position).kind
        val replaced = replaceWithPlaceholder(dummyFile, position, kind) ?: position
        return CaCompletionDummyContext(
            dummyFile = dummyFile,
            position = replaced,
            prefix = prefixOf(position),
        )
    }

    /**
     * 把光标所在表达式替换为语法合法的占位表达式。
     *
     * 只在调用类位置替换：其它形态下裸标识符已经可解析，
     * 额外加括号会改变候选形态（例如把类型位置变成调用）。
     */
    private fun replaceWithPlaceholder(
        dummyFile: CjFile,
        position: CjElement,
        kind: CaCompletionPositionKind,
    ): CjElement? {
        if (!kind.needsCallablePlaceholder) return null
        val expression = position as? CjExpression ?: position.parent as? CjExpression ?: return null
        val factory = CjPsiFactory(dummyFile.project)
        val placeholder = factory.createExpression(PLACEHOLDER_CALL) ?: return null
        return runCatching { expression.replace(placeholder) }.getOrNull() as? CjElement
    }

    /**
     * 判断该位置形态是否需要可调用的占位表达式。
     */
    private val CaCompletionPositionKind.needsCallablePlaceholder: Boolean
        get() = this == CaCompletionPositionKind.EXPRESSION ||
            this == CaCompletionPositionKind.FUNCTION_BODY ||
            this == CaCompletionPositionKind.MEMBER_ACCESS ||
            this == CaCompletionPositionKind.NAMED_ARGUMENT

    /**
     * 调用类位置的占位表达式。
     */
    private const val PLACEHOLDER_CALL = "cangjieCompletionPlaceholder()"

    /**
     * 取得光标处的真实前缀。
     *
     * 必须去掉 dummy 文本与限定名前缀，否则平台会按错误前缀把候选全部过滤掉。
     */
    private fun prefixOf(position: PsiElement): String =
        position.text.substringBeforeLast(DUMMY_MARKER)
            .substringAfterLast('.')
            .trimEnd()

    /**
     * 取得距离叶子位置最近的仓颉元素。
     */
    private fun PsiElement.nearestCangJieElement(): CjElement? {
        var current: PsiElement = this
        while (true) {
            if (current is CjElement) return current
            current = current.parent ?: return null
        }
    }

    /**
     * 判断位置是否位于原始字符串内。
     *
     * 原始字符串不插值，在其中补全会产出永远不会被求值的代码。
     */
    private fun PsiElement.isInsideRawStringLiteral(): Boolean =
        PsiTreeUtil.getParentOfType(this, CjStringTemplateExpression::class.java)?.isRawString == true
}
