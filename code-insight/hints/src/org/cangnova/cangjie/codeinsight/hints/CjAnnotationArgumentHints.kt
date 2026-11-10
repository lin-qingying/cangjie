package org.cangnova.cangjie.codeinsight.hints

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType

/**
 * 注解实参名提示的候选判定。
 *
 * 声明前缀位置的 `@Ann(1, 2)` 在 PSI 里是 `CjMacroExpression`，实参落在
 * `MACRO_INPUT/QUOTE_TOKENS` 中、只有原始 token（见 `add-inlay-hints` design D56）。
 * 因此这里先把 `(1, 2)` 这段源码文本交给 `CjPsiFactory` 重建出真正的表达式，
 * 再按重建结果算回源码偏移。
 *
 * 只做 PSI 层工作：形参名的解析与配对放在 [CjAnnotationArgumentHintsCollector] 的解析阶段。
 */
internal object CjAnnotationArgumentHints {
    /**
     * 拆出注解实参；非括号形态（`@Ann { ... }`、`@Ann` 后直接跟声明）返回空。
     *
     * @return 按源码顺序排列的实参，偏移已换算成**源码**偏移（用于插入提示）。
     */
    fun argumentsOf(macro: CjMacroExpression): List<CjMacroArgument> = rebuildArguments(macro)

    /**
     * 重建实参表达式。
     *
     * `createCallArguments` 会在合成上下文里解析 `(a, b)`；重建出的表达式只用于读文本与
     * 判断是否命名实参，偏移必须按相对位置换算回源码。
     */
    private fun rebuildArguments(macro: CjMacroExpression): List<CjMacroArgument> {
        val input = macro.input ?: return emptyList()
        val text = input.text
        // 只有括号形态才是「调用式注解」；`@Ann` 后直接跟声明时 input 是声明文本。
        if (!text.startsWith("(")) return emptyList()

        val list = CjPsiFactory.contextual(macro).createCallArguments(text)
        val listStart = list.textRange.startOffset
        val inputStart = input.textRange.startOffset
        return list.arguments.mapNotNull { argument ->
            val expression = argument.getArgumentExpression() ?: return@mapNotNull null
            // `- listStart - 1` 越过左括号：实参在 `( ... )` 内的相对位置。
            val offset = inputStart + 1 + (expression.textRange.startOffset - listStart - 1)
            CjMacroArgument(
                offset = offset,
                text = expression.text,
                isNamed = argument.isNamed(),
            )
        }
    }
}

/**
 * 注解实参名的候选：一个注解调用 + 它重建出的实参。
 *
 * 只携带 PSI 与已换算的偏移，不含 session 生命周期对象。
 */
internal data class CjAnnotationArgumentHintCandidate(
    /** 承载实参的注解调用（`@Ann(...)` 整个宏表达式）。 */
    val macro: CjMacroExpression,
    /** 按源码顺序排列的实参。 */
    val arguments: List<CjMacroArgument>,
) {
    /**
     * 触发提示的锚点：取第一个实参的位置。
     */
    val offset: Int
        get() = arguments.first().offset
}

/**
 * 单个实参。
 */
internal data class CjMacroArgument(
    /** 实参在源码中的起始偏移。 */
    val offset: Int,
    /** 实参文本，仅用于调试与测试断言。 */
    val text: String,
    /** 是否已经是命名实参（`name: value`），命名实参不再补提示。 */
    val isNamed: Boolean,
)

/**
 * 遍历文件里的注解调用候选。
 */
internal fun collectAnnotationCandidates(
    file: CjFile,
    range: TextRange?,
): List<CjAnnotationArgumentHintCandidate> =
    file.collectDescendantsOfType<CjMacroExpression>()
        .mapNotNull { macro ->
            if (CjInlayHintFilters.hasErrorAncestor(macro as CjExpression)) return@mapNotNull null
            val arguments = CjAnnotationArgumentHints.argumentsOf(macro).filterNot { it.isNamed }
            if (arguments.isEmpty()) return@mapNotNull null
            if (!CjInlayHintFilters.isInRange(arguments.first().offset, range)) return@mapNotNull null
            CjAnnotationArgumentHintCandidate(macro, arguments)
        }
