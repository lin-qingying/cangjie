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
            item.getUserData(COMPLETION_REVALIDATION) ?: RevalidationVerdict.NotPerformed,
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
            item.getUserData(COMPLETION_REVALIDATION) ?: RevalidationVerdict.NotPerformed,
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
 * 形态以**文档现状**为准，不以分析期结论为准：分析期看到的括号是当时的文本，
 * 用户此后可能已经手写了左括号甚至填好了实参。三种触发（Enter / Tab / 继续键入）
 * 共用同一套判断——差别只在平台给出的替换区间，不在括号处理。
 *
 * 类型实参列表先于调用括号处理：语法上它先出现，且它在文档里的落点决定了括号探测从哪里开始。
 * 两者都插时 caret 落在**尖括号内**（见 [CaretPolicy.INSIDE_NEW_TYPE_ARGUMENTS]）。
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

    var wrote = false
    // 期望的 caret 落点（尖括号内），仅在策略允许且确实处理了类型实参列表时生效。
    var caretInsideTypeArguments: Int? = null
    // 括号探测的起点；为 null 表示「无法判断位置，不处理括号」。
    var probeFrom: Int? = caretOffset

    val typeArguments = snapshot.insertion.typeParameterList
    if (typeArguments.isNotEmpty()) {
        when (val existing = existingTypeArguments(sink, caretOffset)) {
            // 用户还没写尖括号：补上声明自己的类型参数名当占位。
            ExistingTypeArguments.Absent -> {
                sink.insert(caretOffset, typeArguments)
                caretInsideTypeArguments = caretOffset + 1
                probeFrom = caretOffset + typeArguments.length
                wrote = true
            }

            // 已经写了完整的 `<...>`：绝不重复插，caret 交给括号内，括号探测从列表之后开始。
            is ExistingTypeArguments.Closed -> {
                caretInsideTypeArguments = existing.open + 1
                probeFrom = existing.close + 1
            }

            // 只写了 `<`：列表边界未知，此时补括号会写出 `Box<T()>` 这类错位文本，直接不处理括号。
            is ExistingTypeArguments.Unclosed -> {
                caretInsideTypeArguments = existing.open + 1
                probeFrom = null
            }
        }
    }

    var parenCaret: Int? = null
    if (probeFrom != null && snapshot.insertion.insertParenthesis) {
        when (val existing = existingCallParens(sink, probeFrom)) {
            // 用户已经手写了左括号：绝不能再补一对，否则写出 `foo((`。
            is ExistingCallParens.Closed -> {
                // 括号里已经有实参时 caret 停在右括号之前，把用户写好的实参让出来；
                // 空括号（含只有空白）则停在括号内，用户可以接着写第一个实参。
                parenCaret = if (sink.hasContent(existing.open + 1, existing.close)) {
                    existing.close
                } else {
                    existing.open + 1
                }
            }

            // 括号还没闭合：补一对会写出 `foo(()`，只能把 caret 交给用户接着写。
            is ExistingCallParens.Unclosed -> parenCaret = existing.open + 1

            ExistingCallParens.Absent -> {
                sink.insert(probeFrom, "()")
                parenCaret = probeFrom + 1
                wrote = true
            }
        }
    }
    if (snapshot.insertion.caretPolicy == CaretPolicy.BEFORE_EXISTING_CLOSING_PARENTHESIS) {
        // 候选后面已经有实参和闭合括号：caret 必须停在右括号之前，
        // 否则用户写好的实参会被挤到括号外。
        parenCaret = closingParenOffset(sink, parenCaret ?: caretOffset) ?: parenCaret
    }
    val caret = if (snapshot.insertion.caretPolicy == CaretPolicy.INSIDE_NEW_TYPE_ARGUMENTS) {
        caretInsideTypeArguments ?: parenCaret ?: caretOffset
    } else {
        parenCaret ?: caretOffset
    }
    setCaret(caret)
    return InsertionResult(wroteText = wrote, caretOffset = caret)
}

/**
 * 判断区间内是否有非空白内容。
 *
 * 只按非空白字符判断：`foo( )` 与 `foo()` 一样是「用户还没写实参」，
 * 不能因为中间有一个空格就把 caret 让到右括号之前。
 */
private fun CompletionTextSink.hasContent(from: Int, to: Int): Boolean {
    for (index in from until to) {
        if (!charAt(index).isWhitespace()) return true
    }
    return false
}

/**
 * 探测 [caret] 之后是否已经存在调用括号。
 *
 * 只做括号配对，不解析表达式：这里在写事务里跑，不能调用 Analysis，
 * 而需要的结论只是「这一对括号是否已经存在、里面有没有内容」。
 *
 * **未闭合也是有效信息**：用户可能刚敲下 `(` 就触发了补全，
 * 此时补一对会写出 `foo(()`，比不补更糟。
 */
private fun existingCallParens(sink: CompletionTextSink, caret: Int): ExistingCallParens {
    var open = caret
    while (open < sink.length && sink.charAt(open).isWhitespace()) open++
    if (open >= sink.length || sink.charAt(open) != '(') return ExistingCallParens.Absent
    var depth = 0
    var index = open
    while (index < sink.length) {
        when (sink.charAt(index)) {
            '(' -> depth++
            ')' -> {
                depth--
                if (depth == 0) return ExistingCallParens.Closed(open, index)
            }
        }
        index++
    }
    return ExistingCallParens.Unclosed(open)
}

/**
 * 插入点之后的调用括号现状。
 */
private sealed interface ExistingCallParens {
    /** 后面没有左括号，可以补一对。 */
    data object Absent : ExistingCallParens

    /** 有左括号且已闭合。 */
    data class Closed(val open: Int, val close: Int) : ExistingCallParens

    /** 有左括号但尚未闭合。 */
    data class Unclosed(val open: Int) : ExistingCallParens
}

/**
 * 探测 [caret] 之后是否已经存在类型实参列表。
 *
 * 与括号探测同源的理由：这里在写事务里跑，不能调用 Analysis，需要的结论只是
 * 「用户是不是已经自己写了尖括号」。**已经有 `<` 就绝不重复补**，
 * 否则会在 `Box<` 后面再插一段 `<T>`，写出 `Box<<T>`。
 *
 * 只做尖括号配对，不解析类型：嵌套 `Box<Box<Int64>>` 靠深度计数识别为已闭合。
 */
private fun existingTypeArguments(sink: CompletionTextSink, caret: Int): ExistingTypeArguments {
    var open = caret
    while (open < sink.length && sink.charAt(open).isWhitespace()) open++
    if (open >= sink.length || sink.charAt(open) != '<') return ExistingTypeArguments.Absent
    var depth = 0
    var index = open
    while (index < sink.length) {
        when (sink.charAt(index)) {
            '<' -> depth++
            '>' -> {
                depth--
                if (depth == 0) return ExistingTypeArguments.Closed(open, index)
            }
        }
        index++
    }
    return ExistingTypeArguments.Unclosed(open)
}

/**
 * 插入点之后的类型实参列表现状。
 */
private sealed interface ExistingTypeArguments {
    /** 后面没有 `<`，可以补一段类型参数列表。 */
    data object Absent : ExistingTypeArguments

    /** 已有完整 `<...>`。 */
    data class Closed(val open: Int, val close: Int) : ExistingTypeArguments

    /** 只写了 `<`，列表尚未闭合。 */
    data class Unclosed(val open: Int) : ExistingTypeArguments
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
 * 找出插入点之后第一个右括号的位置；不存在时返回 `null`。
 */
private fun closingParenOffset(sink: CompletionTextSink, offset: Int): Int? {
    var index = offset
    while (index < sink.length && sink.charAt(index).isWhitespace()) index++
    return if (index < sink.length && sink.charAt(index) == ')') index else null
}
