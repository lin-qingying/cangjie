package org.cangnova.cangjie.lsp.completion

import com.google.gson.JsonObject
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemLabelDetails
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.jsonrpc.messages.Either

/**
 * 把仓颉候选快照转换成 LSP `CompletionItem`。
 *
 * **输入是快照而不是 `LookupElement`**：本模块走分支 B（见 `implementation-log.md` 的 15.1 结论），
 * 不经平台补全进程，因此没有 `LookupElementFactory` 产出的展示对象；
 * 快照里的 `presentableText` / `tailText` / `typeText` 就是两端的共同事实。
 *
 * 插入机制（方向容易搞反，写清楚）：候选带**空** `textEdit` 与 [LspCompletionCommands.APPLY_COMPLETION]
 * 命令——客户端接受候选后自动执行命令，服务端再算编辑并**请求** `workspace/applyEdit` 让客户端落盘。
 * 服务端不写文档，因此这里的 `textEdit` 什么都不改；真正的编辑在命令里算。
 *
 * 不设 `filterText` / `preselect` / `commitCharacters` / `deprecated`：排序由服务端预排序承担
 * （见 [LspCompletionSortingUtil]），过滤由客户端默认前缀行为承担；字段一开就多一份「两端行为可能不一致」的面。
 */
object LspCompletionItemConverter {
    /**
     * 转换单个候选。
     *
     * @param snapshot 上游管线给出的候选快照。
     * @param index 候选在最终列表中的下标，用于生成 `sortText`。
     * @param caret 光标位置；空编辑落在这里。
     * @param sessionKey 会话键，写进 `data` 与命令实参，供 `applyCompletion` 取回候选。
     */
    fun toCompletionItem(
        snapshot: CompletionSnapshot,
        index: Int,
        caret: Position,
        sessionKey: String,
    ): CompletionItem = CompletionItem(snapshot.presentableText).apply {
        sortText = LspCompletionSortingUtil.sortTextOf(index)
        kind = LspCompletionItemKindProvider.kindOf(snapshot)
        // 签名与类型分列两处：detail 是「同名之间的区别」（参数），description 是「它是什么类型」。
        // 两者都为空则**不设该字段**——空串会让客户端渲染出空括号区。
        labelDetails = labelDetailsOf(snapshot)
        // `textEdit` 在协议里是 `Either<TextEdit, InsertReplaceEdit>`：本实现给纯文本编辑，取左支。
        textEdit = Either.forLeft(emptyEditAt(caret))
        // 命令实参按位置约定：[会话键, 下标]；两个都由服务端生成，客户端只负责原样回传。
        command = Command(
            snapshot.presentableText,
            LspCompletionCommands.APPLY_COMPLETION,
            listOf(sessionKey, index),
        )
        // `data` 是给 `resolve` 用的定位信息：会话键 + 下标。放对象而不是单个字符串，
        // 因为 resolve 只拿到 item，没有命令实参里的那个下标。
        data = JsonObject().apply {
            addProperty(LspCompletionCommands.SESSION_KEY_FIELD, sessionKey)
            addProperty(LspCompletionCommands.INDEX_FIELD, index)
        }
    }

    /**
     * 候选的 `labelDetails`；签名与类型都没有时返回 `null`（该字段整体不设）。
     */
    private fun labelDetailsOf(snapshot: CompletionSnapshot): CompletionItemLabelDetails? {
        val detail = snapshot.tailText.takeIf(String::isNotEmpty)
        val description = snapshot.typeText.takeIf(String::isNotEmpty)
        if (detail == null && description == null) return null
        return CompletionItemLabelDetails().apply {
            this.detail = detail
            this.description = description
        }
    }

    /**
     * 光标处的空编辑。
     *
     * 「空」是刻意的：客户端应用它等于不改变文本，真正的编辑由 `applyCompletion` 计算后
     * 经 `workspace/applyEdit` 送达。范围取零长区间，`newText` 取空串。
     */
    private fun emptyEditAt(caret: Position): TextEdit =
        TextEdit(Range(caret, caret), "")
}
