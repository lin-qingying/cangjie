package org.cangnova.cangjie.ide.completion.impl.shared.dummy

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionContexts
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind
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
data class CompletionDummyContext(
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
object CompletionDummyIdentifierProvider {
    /**
     * 平台 dummy 标识符的稳定标记。
     *
     * 公开是因为**无头宿主必须自己插入它**：IDE 侧由平台在副本里插好再交给 contributor，
     * 而 LSP 侧没有平台补全进程（分支 B），得自己拼那份带标记的副本。
     * 两侧用同一个常量，避免「标记拼错 → 占位替换找不到位置 → 静默零候选」。
     */
    const val DUMMY_MARKER: String = "IntellijIdeaRulezzz"

    /**
     * 对平台参数执行 dummy 修正，返回可用于分析的上下文。
     *
     * 返回 `null` 表示该位置不允许补全：调用方必须停止，
     * 绝不能在仍含裸 dummy 的 PSI 上继续分析。
     */
    fun provide(parameters: CompletionParameters): CompletionDummyContext? {
        val position = parameters.position.nearestCangJieElement() ?: return null
        return provideAt(position)
    }

    /**
     * 不依赖平台补全参数的修正入口：给定文件与文档偏移。
     *
     * 存在的理由是**可测与可复用**：平台入口需要构造 `CompletionParameters`，
     * 那在测试里几乎不可得，于是这套修正逻辑长期没有回归；而它恰恰是「候选为空但不报错」
     * 这类最难定位的失败的源头。无头宿主（LSP）同样拿不到平台补全参数，
     * 也需要一个直接入口。
     *
     * 与平台入口的差别只有「怎么找到位置」：这里按偏移取叶子节点再向上找最近的仓颉元素，
     * 之后的修正逻辑完全共用。
     *
     * **必须在命令或 undo-transparent 动作内调用**：占位替换是一次 PSI 写，
     * 命令外调用会被平台拒绝（`IncorrectOperationException: Must not change PSI outside
     * command or undo-transparent action`）。平台补全入口本身处于补全命令内，因此天然满足；
     * 无头宿主需要自己开命令。
     *
     * @param file 含 dummy 文本的文件（平台副本或调用方自建的副本）。
     * @param offset 文档偏移。
     * @return 修正结果；位置不允许补全时返回 `null`。
     * @throws com.intellij.util.IncorrectOperationException 在命令外调用时。
     */
    fun provideFor(file: CjFile, offset: Int): CompletionDummyContext? {
        val leaf = file.findElementAt(offset) ?: return null
        val position = leaf.nearestCangJieElement() ?: return null
        return provideAt(position)
    }

    /**
     * 对已定位的仓颉元素执行修正。两个入口共用，保证「怎么找到位置」不影响修正结论。
     */
    private fun provideAt(position: CjElement): CompletionDummyContext? {
        val dummyFile = position.containingCjFile ?: return null
        if (position.isInsideRawStringLiteral()) return null

        val kind = CompletionPositionContexts.classify(position).kind
        val replaced = replaceWithPlaceholder(dummyFile, position, kind) ?: position
        return CompletionDummyContext(
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
        kind: CompletionPositionKind,
    ): CjElement? {
        if (!kind.needsCallablePlaceholder) return null
        val expression = position as? CjExpression ?: position.parent as? CjExpression ?: return null
        val factory = CjPsiFactory(dummyFile.project)
        val placeholder = factory.createExpression(PLACEHOLDER_CALL) ?: return null
        // **不吞异常**：替换失败若被静默忽略，位置就留着裸 dummy，
        // 后续分析得到空候选且不报错——本文件 KDoc 里点名的「最难定位的失败」
        // 正是这样产生的。失败必须显形，由调用方决定是降级还是上报。
        return expression.replace(placeholder) as? CjElement
    }

    /**
     * 判断该位置形态是否需要可调用的占位表达式。
     */
    private val CompletionPositionKind.needsCallablePlaceholder: Boolean
        get() = this == CompletionPositionKind.EXPRESSION ||
            this == CompletionPositionKind.FUNCTION_BODY ||
            this == CompletionPositionKind.MEMBER_ACCESS ||
            this == CompletionPositionKind.NAMED_ARGUMENT

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
