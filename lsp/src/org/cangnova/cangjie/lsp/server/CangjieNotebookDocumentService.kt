package org.cangnova.cangjie.lsp.server

import org.eclipse.lsp4j.DidChangeNotebookDocumentParams
import org.eclipse.lsp4j.DidCloseNotebookDocumentParams
import org.eclipse.lsp4j.DidOpenNotebookDocumentParams
import org.eclipse.lsp4j.DidSaveNotebookDocumentParams
import org.eclipse.lsp4j.services.NotebookDocumentService
import java.util.logging.Logger

/**
 * Notebook 当前只提供框架占位。
 */
class CangjieNotebookDocumentService : NotebookDocumentService {
    private val logger = Logger.getLogger(CangjieNotebookDocumentService::class.java.name)

    override fun didOpen(params: DidOpenNotebookDocumentParams) {
        logger.info("====> didOpen (notebook)")
    }

    override fun didChange(params: DidChangeNotebookDocumentParams) {
        logger.info("====> didChange (notebook)")
    }

    override fun didSave(params: DidSaveNotebookDocumentParams) {
        logger.info("====> didSave (notebook)")
    }

    override fun didClose(params: DidCloseNotebookDocumentParams) {
        logger.info("====> didClose (notebook)")
    }
}
