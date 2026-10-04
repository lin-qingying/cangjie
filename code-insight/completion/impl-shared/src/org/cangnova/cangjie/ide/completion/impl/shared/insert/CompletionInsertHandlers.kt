package org.cangnova.cangjie.ide.completion.impl.shared.insert

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CaretPolicy
import org.cangnova.cangjie.ide.completion.api.lookup.InsertionValidity
import org.cangnova.cangjie.ide.completion.impl.shared.lookup.COMPLETION_SNAPSHOT

/**
 * 仓颉候选的插入处理器。
 *
 * 插入发生在**写事务**里，此时不能查询 Analysis（可能跨线程违规），
 * 也不该临时决定括号形态。因此所有结论都来自分析期冻结的 [CompletionSnapshot.insertion]。
 *
 * 本文件只做平台适配：把 `InsertionContext` 拆成 `Document` + caret 偏移 + 校验结论，
 * 真正的写入决策在 [performCangjieInsertion] 里，可直接用真实文档测试。
 */
object CompletionInsertHandlers {
    /**
     * 为快照选择插入处理器。
     *
     * 命名实参候选走独立分支：它写入 `name: ` 而不是 `name(`。
     */
    fun forSnapshot(snapshot: CompletionSnapshot): InsertHandler<LookupElement> =
        if (snapshot.insertion.namedArgumentPrefix.isNotEmpty()) NamedArgumentHandler else DefaultHandler

    /**
     * 从 lookup 元素取回快照。
     *
     * 元素不是由本工厂创建时返回 `null`：宿主可能把仓颉候选与其它插件的候选
     * 混在同一次补全里，此时不能假设每个元素都带仓颉快照。
     */
    fun snapshotOf(element: LookupElement): CompletionSnapshot? =
        element.getUserData(COMPLETION_SNAPSHOT)
}

/**
 * 平台插入入口：校验 → 提交 → 写入 → 落 caret → 提交。
 */
private object DefaultHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val snapshot = CompletionInsertHandlers.snapshotOf(item) ?: return
        val document = context.document
        val validity = InsertionValidator.validate(
            snapshot,
            context.startOffset,
            context.tailOffset,
            document,
            document.modificationStamp,
        )
        context.commitDocument()
        val caretModel = context.editor.caretModel
        performCangjieInsertion(snapshot, DocumentTextSink(document), validity, caretModel.offset) {
            caretModel.moveToOffset(it)
        }
        context.commitDocument()
    }
}

/**
 * 命名实参插入入口。
 *
 * 仓颉命名实参写作 `name: value`。**不得**插入 Kotlin 的 `name = value` 形式：
 * 那在仓颉里不是合法语法，会直接让文件无法编译。
 */
private object NamedArgumentHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val snapshot = CompletionInsertHandlers.snapshotOf(item) ?: return
        val document = context.document
        val validity = InsertionValidator.validate(
            snapshot,
            context.startOffset,
            context.tailOffset,
            document,
            document.modificationStamp,
        )
        context.commitDocument()
        val caretModel = context.editor.caretModel
        performNamedArgumentInsertion(snapshot, DocumentTextSink(document), validity, caretModel.offset) {
            caretModel.moveToOffset(it)
        }
        context.commitDocument()
    }
}

/**
 * 插入所作用的文本。
 *
 * 抽出这层是为了让插入策略与平台 `Document` 解耦：
 * 平台文档的读写受 EDT 与写访问检查约束，策略逻辑若直接焊在 `Document` 上，
 * 就只能在平台夹具里测——而夹具里能验证的恰恰只有「平台 API 能跑通」，
 * 括号补不补、caret 落在哪这些真正会破坏用户代码的判断反而测不到。
 */
interface CompletionTextSink {
    /** 当前文本长度。 */
    val length: Int

    /** 读取指定偏移处的字符；越界视为空白。 */
    fun charAt(offset: Int): Char

    /** 在指定偏移处插入文本。 */
    fun insert(offset: Int, text: String)
}

/**
 * 把平台文档适配为文本接缝。
 */
class DocumentTextSink(private val document: com.intellij.openapi.editor.Document) : CompletionTextSink {
    override val length: Int
        get() = document.textLength

    override fun charAt(offset: Int): Char {
        val text = document.charsSequence
        return if (offset in text.indices) text[offset] else ' '
    }

    override fun insert(offset: Int, text: String) {
        document.insertString(offset, text)
    }
}

/**
 * 插入的执行结果。
 *
 * [wroteText] 区分「确实改了文本」与「校验未通过，什么都没写」：
 * 失效候选必须走到这里且 `wroteText` 为 false，否则就是往已失效的位置硬插。
 */
data class InsertionResult(val wroteText: Boolean, val caretOffset: Int)

/**
 * 执行普通候选插入。
 *
 * 顺序固定为「先校验、再写任何字符」：校验未通过时**一个字符都不许写**。
 * 把括号补齐放在校验之后，可以避免失效候选在文档里留下半截括号。
 *
 * @param validity 插入前校验结论。
 * @param setCaret caret 落点回调。
 */
fun performCangjieInsertion(
    snapshot: CompletionSnapshot,
    sink: CompletionTextSink,
    validity: InsertionValidity,
    caretOffset: Int,
    setCaret: (Int) -> Unit,
): InsertionResult {
    if (!validity.writable) return InsertionResult(wroteText = false, caretOffset = caretOffset)

    var caret = caretOffset
    var wrote = false
    if (snapshot.insertion.insertParenthesis && !isFollowedByOpeningParen(sink, caret)) {
        sink.insert(caret, "()")
        caret += 1
        wrote = true
    }
    if (snapshot.insertion.caretPolicy == CaretPolicy.BEFORE_EXISTING_CLOSING_PARENTHESIS) {
        // 候选后面已经有实参和闭合括号：caret 必须停在右括号之前，
        // 否则用户写好的实参会被挤到括号外。
        caret = closingParenOffset(sink, caret) ?: caret
    }
    setCaret(caret)
    return InsertionResult(wroteText = wrote, caretOffset = caret)
}

/**
 * 执行命名实参插入：写入仓颉的 `name: ` 前缀并把 caret 放到冒号之后。
 */
fun performNamedArgumentInsertion(
    snapshot: CompletionSnapshot,
    sink: CompletionTextSink,
    validity: InsertionValidity,
    caretOffset: Int,
    setCaret: (Int) -> Unit,
): InsertionResult {
    if (!validity.writable) return InsertionResult(wroteText = false, caretOffset = caretOffset)
    val prefix = snapshot.insertion.namedArgumentPrefix
    sink.insert(caretOffset, prefix)
    val caret = caretOffset + prefix.length
    setCaret(caret)
    return InsertionResult(wroteText = true, caretOffset = caret)
}

/**
 * 插入点之后是否已经是左括号。
 *
 * 已有括号时**不再补**：用户在 `foo(bar)` 里补 `foo` 的实参，重复补会得到
 * `foo((bar))`，而这正是 Tab 补全最常见的落点。
 */
private fun isFollowedByOpeningParen(sink: CompletionTextSink, offset: Int): Boolean {
    var index = offset
    while (index < sink.length && sink.charAt(index).isWhitespace()) index++
    return index < sink.length && sink.charAt(index) == '('
}

/**
 * 找出插入点之后第一个右括号的位置；不存在时返回 `null`。
 */
private fun closingParenOffset(sink: CompletionTextSink, offset: Int): Int? {
    var index = offset
    while (index < sink.length && sink.charAt(index).isWhitespace()) index++
    return if (index < sink.length && sink.charAt(index) == ')') index else null
}
