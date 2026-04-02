package org.cangnova.cangjie.lsp.state

import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.TextDocumentItem
import java.util.concurrent.ConcurrentHashMap

class LspDocumentStore {
    private val openDocuments = ConcurrentHashMap<String, LspTextDocument>()

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

    fun applyChanges(params: DidChangeTextDocumentParams): LspTextDocument {
        val current = openDocuments[params.textDocument.uri]
            ?: error("Document is not open: ${params.textDocument.uri}")

        var updatedText = current.text
        params.contentChanges.forEach { change ->
            updatedText = change.range?.let { range ->
                LspTextDocument.applyRangeChange(updatedText, range, change.text)
            } ?: change.text
        }

        val updated = current.withText(
            newText = updatedText,
            newVersion = params.textDocument.version,
        )
        openDocuments[updated.uri] = updated
        return updated
    }

    fun close(uri: String): LspTextDocument? = openDocuments.remove(uri)

    fun get(uri: String): LspTextDocument? = openDocuments[uri]

    fun all(): List<LspTextDocument> = openDocuments.values.sortedBy { it.uri }
}
