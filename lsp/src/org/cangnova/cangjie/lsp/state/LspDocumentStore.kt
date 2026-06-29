package org.cangnova.cangjie.lsp.state

import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.TextDocumentItem
import java.util.concurrent.ConcurrentHashMap

/**
 * 管理当前 LSP 会话中打开文档的快照集合。
 *
 * 文档存储负责 open/change/close 生命周期和增量编辑应用，是协议层文本状态的唯一来源。
 */
class LspDocumentStore {
    /**
     * 以文档 URI 为键保存的打开文档快照。
     */
    private val openDocuments = ConcurrentHashMap<String, LspTextDocument>()

    /**
     * 打开一个新文档并保存初始快照。
     *
     * 返回值是写入存储后的快照，供后续分析 facade 立即消费。
     */
    fun open(document: TextDocumentItem): LspTextDocument {
        val snapshot = LspTextDocument(
            uri = document.uri,
            languageId = document.languageId,
            version = document.version,
            text = document.text,
        )
        openDocuments[snapshot.uri] = snapshot
        return snapshot
    }

    /**
     * 按 LSP `didChange` 参数应用一次文档变更。
     *
     * 方法按客户端给出的顺序处理多个变更，支持全文替换和带范围的增量替换。
     */
    fun applyChanges(params: DidChangeTextDocumentParams): LspTextDocument {
        val current = openDocuments[params.textDocument.uri]
            ?: error("Document is not open: ${params.textDocument.uri}")

        var updatedText = current.text
        params.contentChanges.forEach { change ->
            updatedText = change.range?.let { range ->
                LspTextDocument.applyRangeChange(
                    text = updatedText,
                    range = range,
                    replacement = change.text,
                )
            } ?: change.text
        }

        val updated = current.withText(
            newText = updatedText,
            newVersion = params.textDocument.version,
        )
        openDocuments[updated.uri] = updated
        return updated
    }

    /**
     * 关闭指定 URI 的文档并返回被移除的快照。
     */
    fun close(uri: String): LspTextDocument? = openDocuments.remove(uri)

    /**
     * 查询指定 URI 当前打开的文档快照。
     */
    fun get(uri: String): LspTextDocument? = openDocuments[uri]

    /**
     * 返回全部打开文档的稳定排序列表。
     */
    fun all(): List<LspTextDocument> = openDocuments.values.sortedBy { it.uri }
}
