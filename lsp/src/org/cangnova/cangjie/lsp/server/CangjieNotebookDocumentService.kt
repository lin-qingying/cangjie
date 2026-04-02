package org.cangnova.cangjie.lsp.server

import org.eclipse.lsp4j.DidChangeNotebookDocumentParams
import org.eclipse.lsp4j.DidCloseNotebookDocumentParams
import org.eclipse.lsp4j.DidOpenNotebookDocumentParams
import org.eclipse.lsp4j.DidSaveNotebookDocumentParams
import org.eclipse.lsp4j.services.NotebookDocumentService

/**
 * Notebook 当前只提供框架占位。
 */
class CangjieNotebookDocumentService : NotebookDocumentService {
    override fun didOpen(params: DidOpenNotebookDocumentParams) {}

    override fun didChange(params: DidChangeNotebookDocumentParams) {}

    override fun didSave(params: DidSaveNotebookDocumentParams) {}

    override fun didClose(params: DidCloseNotebookDocumentParams) {}
}
