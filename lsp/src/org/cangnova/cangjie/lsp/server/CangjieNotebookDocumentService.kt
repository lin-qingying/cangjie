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
    /**
     * Notebook 协议通知的日志记录器。
     *
     * 当前 notebook 能力尚未实现，通知只记录生命周期事件用于调试协议连通性。
     */
    private val logger = Logger.getLogger(CangjieNotebookDocumentService::class.java.name)

    /**
     * 处理 notebook 打开通知。
     */
    override fun didOpen(params: DidOpenNotebookDocumentParams) {
        logger.info("====> didOpen (notebook)")
    }

    /**
     * 处理 notebook 变更通知。
     */
    override fun didChange(params: DidChangeNotebookDocumentParams) {
        logger.info("====> didChange (notebook)")
    }

    /**
     * 处理 notebook 保存通知。
     */
    override fun didSave(params: DidSaveNotebookDocumentParams) {
        logger.info("====> didSave (notebook)")
    }

    /**
     * 处理 notebook 关闭通知。
     */
    override fun didClose(params: DidCloseNotebookDocumentParams) {
        logger.info("====> didClose (notebook)")
    }
}
