package org.cangnova.cangjie.ide.completion.impl.shared.dummy

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.DummyHolderViewProvider
import com.intellij.psi.PsiDocumentManager
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
     *
     * **不做占位替换**（`replace = false`）：平台补全副本的 PSI 由平台自己持有，
     * 替换（改文档提交或换元素）会让平台随后访问的元素失效——产品级 light fixture 实测
     * 报 `Invalid PSI Element ... containing file is null; hierarchy=CjNameReferenceExpression`
     * 或 `different providers: DummyHolderViewProvider(...)`，症状是整类测试失败而候选看不出问题。
     * 裸 dummy 在这个入口上仍能产出局部/关键字候选（实测 9 条），因此这里选择**不写**。
     */
    fun provide(parameters: CompletionParameters): CompletionDummyContext? {
        val position = parameters.position.nearestCangJieElement() ?: return null
        return provideAt(position, replace = false)
    }

    /**
     * 不依赖平台补全参数的修正入口：给定文件与文档偏移。
     *
     * 存在的理由是**可测与可复用**：平台入口需要构造 `CompletionParameters`，
     * 那在测试里几乎不可得，于是这套修正逻辑长期没有回归；而它恰恰是「候选为空但不报错」
     * 这类最难定位的失败的源头。无头宿主（LSP）同样拿不到平台补全参数，
     * 也需要一个直接入口。
     *
     * 与平台入口的差别有两处：**怎么找到位置**（这里按偏移取叶子再向上找最近的仓颉元素），
     * 以及**可以做占位替换**（`replace = true`）——调用方持有副本，没有别的人在用它的 PSI。
     *
     * **必须在命令或 undo-transparent 动作内调用**：占位替换是一次 PSI 写，
     * 命令外调用会被平台拒绝（`IncorrectOperationException: Must not change PSI outside
     * command or undo-transparent action`）。无头宿主需要自己开命令。
     *
     * @param file 含 dummy 文本的文件（调用方自建的副本）。
     * @param offset 文档偏移。
     * @return 修正结果；位置不允许补全时返回 `null`。
     * @throws com.intellij.util.IncorrectOperationException 在命令外调用时。
     */
    fun provideFor(file: CjFile, offset: Int): CompletionDummyContext? {
        val leaf = file.findElementAt(offset) ?: return null
        val position = leaf.nearestCangJieElement() ?: return null
        return provideAt(position, replace = true)
    }

    /**
     * 对已定位的仓颉元素执行修正。两个入口共用，保证「怎么找到位置」不影响修正结论；
     * 唯一按入口区分的是**能不能替换**（见 [provide] 的说明）。
     */
    private fun provideAt(position: CjElement, replace: Boolean): CompletionDummyContext? {
        val dummyFile = position.containingCjFile ?: return null
        if (position.isInsideRawStringLiteral()) return null

        val kind = CompletionPositionContexts.classify(position).kind
        val replaced = if (replace) replaceWithPlaceholder(dummyFile, position, kind) ?: position else position
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
        val start = expression.textRange.startOffset
        val end = expression.textRange.endOffset
        if (end <= start) return null

        // 两条载体路径，**语义完全一致**（同一段文本被换成同一个占位调用），差别只在载体形态：
        //
        // - **PSI 跟随文档**（文档驱动的副本）：改文本并提交，PSI 随文档重解析。
        // - **PSI 不跟随文档**（light 副本）：文本在创建时就固定，只能改 PSI 树本身；
        //   此时若目标挂在平台 `DummyHolder` 视图提供器下则**放弃替换**（见 [replaceExpressionViaPsi]）。
        //
        // 参照实现（Kotlin `AbstractCompletionDummyIdentifierProvider`）只算**文本**后缀
        // 交给平台，从不跨文件搬 PSI 元素。
        replaceTextViaDocument(dummyFile, start, end, expression.text)?.let { return it }
        return replaceExpressionViaPsi(dummyFile, expression)
    }

    /**
     * 首选路径：经文档替换占位文本，返回按偏移重新定位后的元素。
     *
     * 写入落在**副本**的文档上：平台补全命令内允许；无头宿主必须自己开命令（见 [provideFor]）。
     *
     * **文档获取要兜底**：平台补全副本的 view provider 不是 event-system enabled，
     * `PsiDocumentManager.getDocument(psiFile)` 直接返回 null，只能退到
     * `FileDocumentManager.getDocument(虚拟文件)`——实测平台副本在那一层有文档。
     *
     * **为什么要自检**：「有文档」不等于「PSI 跟着文档走」。light 载体（LSP 自建副本、
     * 解析夹具）的 PSI 文本在创建时就固定，`getDocument` 会为它现场造一个文档，
     * 改完只得到 PSI 与文档互相矛盾的树（实测断言
     * `Inconsistent cangjie.FILE tree ... nodeLength=54; fileLength=65`）。
     *
     * 判据取**结果**：提交后核对 PSI 文本是否已变成占位调用（按区间比对，不按元素文本——
     * 偏移末尾的叶子是 `)`，它最近的仓颉元素是实参列表 `()`，拿它比对整串占位文本必然不匹配）。
     * 没跟随就**撤销文档改动**再返回 `null`，由调用方走 PSI 回退；绝不能把改过的文档
     * 留给一个没重解析的 PSI。
     */
    private fun replaceTextViaDocument(dummyFile: CjFile, start: Int, end: Int, originalText: String): CjElement? {
        val manager = PsiDocumentManager.getInstance(dummyFile.project)
        val document = manager.getDocument(dummyFile)
            ?: FileDocumentManager.getInstance().getDocument(dummyFile.viewProvider.virtualFile)
            ?: return null
        document.replaceString(start, end, PLACEHOLDER_CALL)
        manager.commitDocument(document)
        // 判据是「PSI 有没有跟随文档」，且只能按**文本**判断：平台补全副本的 view provider
        // 不是 event-system enabled，`getDocument(psiFile)` 直接返回 null，只能经虚拟文件拿到
        // 文档（实测 doc=true），而这类副本提交后 PSI 会跟随；light 载体的 PSI 文本在创建时
        // 就固定，改文档只会让两者互相矛盾。
        if (dummyFile.textLength == document.textLength && dummyFile.text.startsWith(PLACEHOLDER_CALL, start)) {
            val leaf = dummyFile.findElementAt(start)
            val element = leaf?.nearestCangJieElement()
            if (element != null) return element
        }
        // PSI 未跟随（light 载体）：把文档还原，交给 PSI 路径。
        document.replaceString(start, start + PLACEHOLDER_CALL.length, originalText)
        manager.commitDocument(document)
        return null
    }

    /**
     * 回退路径：PSI 不跟随文档的载体上直接替换 PSI 元素。
     *
     * **平台 `DummyHolder` 载体上放弃替换**：那里的 PSI 由平台视图提供器托管，
     * 从另一个文件搬来的元素会被判「different providers」并记 ERROR。此时返回 `null`
     * 表示「保留裸 dummy」——这是刻意降级，不是遗漏（裸 dummy 仍能解析出局部/关键字候选）。
     */
    private fun replaceExpressionViaPsi(dummyFile: CjFile, expression: CjExpression): CjElement? {
        if (dummyFile.viewProvider is DummyHolderViewProvider) return null
        val factory = CjPsiFactory(dummyFile.project)
        val placeholder = factory.createExpression(PLACEHOLDER_CALL) ?: return null
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
