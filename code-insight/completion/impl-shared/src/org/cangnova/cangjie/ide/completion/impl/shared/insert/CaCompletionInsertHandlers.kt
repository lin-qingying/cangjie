package org.cangnova.cangjie.ide.completion.impl.shared.insert

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiDocumentManager
import org.cangnova.cangjie.ide.completion.api.CaCompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CaCaretPolicy
import org.cangnova.cangjie.ide.completion.impl.shared.lookup.CA_COMPLETION_SNAPSHOT

/**
 * 仓颉候选的插入处理器。
 *
 * 插入发生在**写事务**里，此时不能查询 Analysis（可能跨线程违规），
 * 也不该临时决定括号形态。因此所有结论都来自分析期冻结的 [CaCompletionSnapshot.insertion]，
 * 这里只负责执行。
 *
 * 覆盖三种触发（Enter / Tab / 字符确认），它们对**已有实参**的处理不同：
 * - Enter 通常发生在空调用位置，直接补括号；
 * - Tab / 字符确认常发生在已有调用内部，此时补括号会把用户写好的实参挤到括号外，
 *   必须把 caret 放到既有右括号之前。
 */
object CaCompletionInsertHandlers {
    /**
     * 为快照选择插入处理器。
     *
     * 命名实参候选走独立分支：它写入 `name: ` 而不是 `name(`。
     */
    fun forSnapshot(snapshot: CaCompletionSnapshot): InsertHandler<LookupElement> =
        if (snapshot.insertion.namedArgumentPrefix.isNotEmpty()) NamedArgumentHandler else DefaultHandler

    /**
     * 从 lookup 元素取回快照。
     *
     * 元素不是由本工厂创建时返回 `null`：宿主可能把仓颉候选与其它插件的候选
     * 混在同一次补全里，此时不能假设每个元素都带仓颉快照。
     */
    fun snapshotOf(element: LookupElement): CaCompletionSnapshot? =
        element.getUserData(CA_COMPLETION_SNAPSHOT)
}

/**
 * 默认插入处理器：处理括号、caret 与文档校验。
 */
private object DefaultHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val snapshot = CaCompletionInsertHandlers.snapshotOf(item) ?: return
        val document = context.document
        val validity = CaInsertionValidator.validate(
            snapshot,
            context.startOffset,
            context.tailOffset,
            document,
            document.modificationStamp,
        )
        // 失效候选不写文档：静默跳过优于写入到已失效的位置。
        if (!validity.writable) return

        context.commitDocument()
        applyCaret(context, snapshot.insertion.caretPolicy)
        if (snapshot.insertion.insertParenthesis) {
            CaTextInsertions.appendParenthesisIfAbsent(context)
        }
        context.commitDocument()
    }
}

/**
 * 命名实参插入处理器。
 *
 * 仓颉命名实参写作 `name: value`。**不得**插入 Kotlin 的 `name = value` 形式：
 * 那在仓颉里不是合法语法，会直接让文件无法编译。
 */
private object NamedArgumentHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val snapshot = CaCompletionInsertHandlers.snapshotOf(item) ?: return
        val document = context.document
        val validity = CaInsertionValidator.validate(
            snapshot,
            context.startOffset,
            context.tailOffset,
            document,
            document.modificationStamp,
        )
        if (!validity.writable) return

        context.commitDocument()
        CaTextInsertions.insertNamedArgumentPrefix(context, snapshot.insertion.namedArgumentPrefix)
        applyCaret(context, snapshot.insertion.caretPolicy)
        context.commitDocument()
    }
}

/**
 * 按 caret 策略落点。
 *
 * 偏移全部相对插入文本之后计算：平台在 `handleInsert` 阶段已经把候选文本
 * 写入文档，直接用文档绝对偏移会在「替换了前缀」的情形下错位。
 */
private fun applyCaret(context: InsertionContext, policy: CaCaretPolicy) {
    when (policy) {
        CaCaretPolicy.AFTER_INSERTED_TEXT -> Unit
        CaCaretPolicy.INSIDE_NEW_PARENTHESIS -> Unit
        CaCaretPolicy.BEFORE_EXISTING_CLOSING_PARENTHESIS -> CaTextInsertions.moveCaretBeforeClosingParen(context)
        CaCaretPolicy.AFTER_NAMED_ARGUMENT_COLON -> Unit
    }
}

/**
 * 文本插入的底层操作。
 *
 * 单独抽出是为了让「括号是否已存在」「既有实参是否要保留」这些判断可被单独测试，
 * 不必经过完整的 `InsertionContext`。
 */
internal object CaTextInsertions {
    /**
     * 若插入点之后不是左括号，则补一对括号并把 caret 放到括号内。
     *
     * 已经存在括号时**不重复补**：用户在 `foo(bar|)` 里补 `foo` 的实参，
     * 重复补会得到 `foo((bar))`。
     */
    fun appendParenthesisIfAbsent(context: InsertionContext) {
        val document = context.document
        val offset = context.editor.caretModel.offset
        val nextChar = if (offset < document.textLength) document.charsSequence[offset] else ' '
        if (nextChar == '(') return
        document.insertString(offset, "()")
        context.editor.caretModel.moveToOffset(offset + 1)
    }

    /**
     * 在插入点写入仓颉命名实参前缀 `name: `，并把 caret 放到冒号之后。
     */
    fun insertNamedArgumentPrefix(context: InsertionContext, prefix: String) {
        val offset = context.editor.caretModel.offset
        context.document.insertString(offset, prefix)
        context.editor.caretModel.moveToOffset(offset + prefix.length)
    }

    /**
     * 把 caret 移到既有右括号之前。
     *
     * 用于「候选后面已经有实参和闭合括号」的场景：
     * 直接停在插入文本末尾会把用户写好的实参挤到括号外。
     */
    fun moveCaretBeforeClosingParen(context: InsertionContext) {
        val document = context.document
        val offset = context.editor.caretModel.offset
        val text = document.charsSequence
        // 跳过中间可能存在的空白，找到右括号。
        var index = offset
        while (index < text.length && text[index].isWhitespace()) index++
        if (index < text.length && text[index] == ')') {
            context.editor.caretModel.moveToOffset(index)
        }
    }

    /**
     * 提交文档并把 PSI 同步到当前状态。
     */
    fun commit(context: InsertionContext) {
        PsiDocumentManager.getInstance(context.project).commitDocument(context.document)
    }

}