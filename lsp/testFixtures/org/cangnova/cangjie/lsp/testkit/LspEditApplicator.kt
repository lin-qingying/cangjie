package org.cangnova.cangjie.lsp.testkit

import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.WorkspaceEdit

/**
 * 把服务端返回的编辑应用到文本上。
 *
 * LSP 的编辑断言必须落在**最终文本**上：只比较编辑条数或区间，会让「插入点偏一格」
 * 「删除前缀与插入重叠」这类错误照样通过，而它们在真实客户端上的表现是吃掉用户代码。
 *
 * 坐标换算复用生产实现（[LspTextDocument]），不在测试里另写一套行列算法——
 * 否则测试和实现会一起错。
 */
object LspEditApplicator {
    /**
     * 按起始偏移升序应用单文档编辑，返回应用后的文本。
     *
     * 编辑区间必须互不重叠且落在文档内，违反时直接失败（这是服务端契约，不是测试宽容点）。
     */
    fun apply(
        text: String,
        edits: List<TextEdit>,
        uri: String = "file:///workspace/apply.cj",
    ): String {
        if (edits.isEmpty()) return text
        val document = document(uri, text)
        val positioned = edits.map { edit -> edit to document.offsetAt(edit.range.start) }
            .sortedBy { (_, start) -> start }

        val out = StringBuilder()
        var cursor = 0
        positioned.forEach { (edit, start) ->
            val end = document.offsetAt(edit.range.end)
            check(start in cursor..text.length) { "编辑起点 $start 落在已处理到的 $cursor 之前：$edit" }
            check(end in start..text.length) { "编辑区间非法：[$start, $end)，文本长度 ${text.length}" }
            out.append(text, cursor, start)
            out.append(edit.newText)
            cursor = end
        }
        out.append(text, cursor, text.length)
        return out.toString()
    }

    /**
     * 应用工作区编辑，返回 URI -> 应用后文本。
     *
     * 只处理 `changes`（文档变更），`documentChanges` 形态当前服务端不使用；
     * 出现未提供原文的 URI 时直接失败，避免「静默忽略一半编辑」。
     */
    fun apply(
        edit: WorkspaceEdit,
        documentTexts: Map<String, String>,
    ): Map<String, String> {
        val changes = edit.changes ?: return emptyMap()
        return changes.mapValues { (uri, edits) ->
            val original = documentTexts[uri] ?: error("工作区编辑引用了未提供原文的文档：$uri")
            apply(original, edits, uri)
        }
    }

    /**
     * 返回指定偏移对应的 LSP 位置。
     */
    fun positionOf(
        text: String,
        offset: Int,
        uri: String = "file:///workspace/apply.cj",
    ): Position = document(uri, text).positionAt(offset)

    /**
     * 返回指定 token 第 [occurrence] 次出现处（可加 [delta]）的 LSP 位置。
     */
    fun positionOfToken(
        text: String,
        token: String,
        occurrence: Int = 1,
        delta: Int = 0,
        uri: String = "file:///workspace/apply.cj",
    ): Position {
        var searchFrom = 0
        var offset = -1
        repeat(occurrence) {
            offset = text.indexOf(token, searchFrom)
            check(offset >= 0) { "文本中找不到第 $occurrence 次出现的 `$token`" }
            searchFrom = offset + token.length
        }
        return positionOf(text, offset + delta, uri)
    }

    /**
     * 构造覆盖整篇文档的 LSP 范围。
     */
    fun fullRange(
        text: String,
        uri: String = "file:///workspace/apply.cj",
    ) = document(uri, text).rangeOf(0, text.length)

    /**
     * 构造单文档快照，只用于坐标换算。
     */
    private fun document(uri: String, text: String): LspTextDocument =
        LspTextDocument(uri = uri, languageId = "cangjie", version = 1, text = text)
}
