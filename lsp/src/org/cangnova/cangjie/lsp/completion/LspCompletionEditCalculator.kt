package org.cangnova.cangjie.lsp.completion

import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.InsertionValidity
import org.cangnova.cangjie.ide.completion.impl.shared.insert.CompletionTextSink
import org.cangnova.cangjie.ide.completion.impl.shared.insert.performCangjieInsertion
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextEdit

/**
 * 计算「接受候选」需要发给客户端的文本编辑。
 *
 * 括号与 caret 策略**不在这里重写**：直接调用共享实现
 * [performCangjieInsertion]，与 IDE 走同一份判断（用户已手写左括号时不再补一对等）。
 * 本类只做两件 LSP 特有的事：
 * 1. 把「用户已键入的前缀」表达成删除编辑——客户端拿到的 `textEdit` 是**空**的
 *    （见 [LspCompletionItemConverter]），因此替换前缀必须由服务端算出来；
 * 2. 把插入结果从「变更中的坐标」折算回**原始文本**坐标，因为 LSP 的编辑都相对原始文本。
 *
 * 前缀删除与候选插入**互不重叠**：删除是 `[replaceStart, replaceEnd)`，
 * 插入发生在 `replaceEnd` 之后（删除后 caret 落在 `replaceStart`，所有插入都在此处及之后）。
 * 因此调用方可以安全地把二者与 import 编辑合并成同一个 `workspace/applyEdit`。
 *
 * 已知限制（显式记录，不静默）：[performCangjieInsertion] 会把 caret 落在括号**内**，
 * 而 `workspace/applyEdit` 不携带 caret 位置——客户端应用编辑后默认把 caret 放在编辑末尾。
 * 「插入 `foo()` 后 caret 停在 `(` 之后」这件事只能由客户端实现（§7.11.8 的宿主客户端工作），
 * 服务端不为此改走 snippet 机制：那会让候选的插入文本随客户端能力而变。
 */
object LspCompletionEditCalculator {
    /**
     * 计算接受 [snapshot] 所需的编辑，坐标全部相对 [text]。
     *
     * @param text 客户端当前文档文本（服务端会话记录的那一份）。
     * @param replaceStart 待替换前缀的起点（文档偏移）。
     * @param replaceEnd 待替换前缀的终点（文档偏移，通常等于 caret）。
     * @param snapshot 被接受的候选。
     */
    fun candidateEdits(
        text: CharSequence,
        replaceStart: Int,
        replaceEnd: Int,
        snapshot: CompletionSnapshot,
    ): List<TextEdit> {
        require(replaceStart in 0..replaceEnd) { "替换区间非法：[$replaceStart, $replaceEnd)" }
        require(replaceEnd <= text.length) {
            "替换区间越界：$replaceEnd > ${text.length}（文档已变，应判定会话过期）"
        }

        val edited = StringBuilder(text)
        val edits = mutableListOf<TextEdit>()
        // 删掉用户已键入的前缀：客户端不再替我们做这件事（textEdit 是空的）。
        if (replaceEnd > replaceStart) {
            edited.delete(replaceStart, replaceEnd)
            edits += TextEdit(Range(positionAt(text, replaceStart), positionAt(text, replaceEnd)), "")
        }

        /*
         * 插入在删除之后的文本上进行：坐标与「平台先替换前缀、再由插入处理器写入」一致。
         */
        val sink = RecordingTextSink(edited)
        val namedArgumentPrefix = snapshot.insertion.namedArgumentPrefix
        if (namedArgumentPrefix.isNotEmpty()) {
            /*
             * 命名实参候选插入的是**整个** `name: `（设计 §7.7 的「调用插入 `name:`」），
             * 因此这里写的是 `namedArgumentPrefix` 本身，而**不是** `lookupString` 再加前缀——
             * 后者会得到 `betabeta: `。
             */
            sink.insert(replaceStart, namedArgumentPrefix)
        } else {
            // 平台在 IDE 侧替换前缀时写入的就是 `lookupString`（展示名 `presentableText` 只上屏不落盘）。
            sink.insert(replaceStart, snapshot.lookupString)
            performCangjieInsertion(
                snapshot = snapshot,
                sink = sink,
                validity = InsertionValidity.Valid,
                caretOffset = replaceStart + snapshot.lookupString.length,
                setCaret = { /* caret 落点由客户端决定，见类注释的已知限制 */ },
            )
        }

        /*
         * 记录到的偏移是**变更中**的坐标，必须折算回原始文本坐标：
         *   原始 = 当前 − 之前所有插入的长度和 + 被删掉的前缀长度
         * 漏掉任何一项都会让后续编辑落偏（典型表现：括号被写到后缀字符之间）。
         * 折算成立的前提是插入偏移**单调不减**——`performCangjieInsertion` 只会从 caret 起
         * 向后写，这里用 require 把它钉住，而不是默默依赖。
         */
        var insertedSoFar = 0
        var lastOffset = -1
        val prefixRemoved = replaceEnd - replaceStart
        sink.inserted.forEach { (offset, insertedText) ->
            require(offset >= lastOffset) { "插入偏移必须单调不减，实际 $lastOffset → $offset" }
            lastOffset = offset
            val original = offset - insertedSoFar + prefixRemoved
            edits += TextEdit(Range(positionAt(text, original), positionAt(text, original)), insertedText)
            insertedSoFar += insertedText.length
        }
        return edits
    }

    /**
     * 记录式文本接缝：把每次插入既写进缓冲、也记下「偏移 + 文本」。
     */
    private class RecordingTextSink(private val buffer: StringBuilder) : CompletionTextSink {
        /** 按发生顺序记录的插入。 */
        val inserted: MutableList<Pair<Int, String>> = mutableListOf()

        override val length: Int
            get() = buffer.length

        override fun charAt(offset: Int): Char =
            if (offset in buffer.indices) buffer[offset] else ' '

        override fun insert(offset: Int, text: String) {
            buffer.insert(offset, text)
            inserted += offset to text
        }
    }

    /**
     * 把文档偏移换算成 LSP 位置。
     *
     * 行尾兼容 `\n` 与 `\r\n`：`\r\n` 时行的**字符数**要把 `\r` 排除在外，
     * 否则 CRLF 文件里每行的列号都会多一，编辑落点整体右移。
     */
    private fun positionAt(text: CharSequence, offset: Int): Position {
        var line = 0
        var lineStart = 0
        var index = 0
        while (index < offset && index < text.length) {
            if (text[index] == '\n') {
                line++
                lineStart = index + 1
            }
            index++
        }
        var character = offset - lineStart
        if (character > 0 && text.getOrNull(offset - 1) == '\r') character--
        return Position(line, character)
    }
}
