package org.cangnova.cangjie.lsp.quickfix

import com.intellij.modcommand.ModDisplayMessage
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findDocument
import org.cangnova.cangjie.lsp.analysis.LspTextEditsComputer
import org.cangnova.cangjie.lsp.state.LspDocumentStore
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.eclipse.lsp4j.ApplyWorkspaceEditParams
import org.eclipse.lsp4j.CreateFile as LspCreateFile
import org.eclipse.lsp4j.DeleteFile as LspDeleteFile
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.MessageType
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.RenameFile as LspRenameFile
import org.eclipse.lsp4j.ResourceOperation
import org.eclipse.lsp4j.ShowDocumentParams
import org.eclipse.lsp4j.SnippetTextEdit
import org.eclipse.lsp4j.TextDocumentEdit
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import org.eclipse.lsp4j.WorkspaceEdit
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import org.eclipse.lsp4j.services.LanguageClient
import java.util.concurrent.CompletableFuture

/**
 * 通过 LSP 客户端应用序列化后的 ModCommand 结果。
 *
 * 文档编辑和撤销由客户端负责；发送编辑前，本 handler 校验 CodeAction 创建时记录的版本和旧文本。
 */
class LspQuickFixCommandHandler(
    private val documentStore: LspDocumentStore,
    private val clientProvider: () -> LanguageClient?,
    private val supportsApplyEdit: () -> Boolean,
    private val supportsDocumentChanges: () -> Boolean,
    private val supportsResourceOperation: (String) -> Boolean,
) {
    fun applyModCommand(arguments: List<Any>): CompletableFuture<Any> {
        val command = ModCommandData.parse(arguments.firstOrNull())
            ?: return failed("applyModCommand received an invalid command payload")
        if (!command.hasEffect()) return failed("The quick-fix command contains no applicable effect")

        validateUpdates(command)?.let { return failed(it) }
        if (command.requiresDocumentChanges() && !supportsDocumentChanges()) {
            return failed("The client did not advertise workspace edit documentChanges")
        }
        val unsupportedOperation = command.requiredResourceOperations().firstOrNull { operation ->
            !supportsResourceOperation(operation)
        }
        if (unsupportedOperation != null) {
            return failed("The client does not support the " + unsupportedOperation + " resource operation")
        }
        if (command.requiresApplyEdit() && !supportsApplyEdit()) {
            return failed("The client did not advertise workspace/applyEdit")
        }
        val client = clientProvider() ?: return failed("The LSP client is not connected")
        return execute(command, client, mutableMapOf())
    }

    private fun validateUpdates(command: ModCommandData): String? {
        val expectedTextByUri = linkedMapOf<String, String>()
        val expectedVersionByUri = linkedMapOf<String, Int?>()

        fun visit(data: ModCommandData): String? {
            return when (data) {
                ModCommandData.Nothing -> null
                is ModCommandData.Composite -> {
                    data.commands.forEach { child -> visit(child)?.let { return it } }
                    null
                }
                is ModCommandData.UpdateFileText -> {
                    val document = documentStore.get(data.documentUri)
                    if (!expectedTextByUri.containsKey(data.documentUri)) {
                        if (data.documentVersion != null &&
                            (document == null || document.version != data.documentVersion)
                        ) {
                            return "The document changed after the quick fix was created; request the quick fix again"
                        }
                        expectedTextByUri[data.documentUri] = document?.analysisText ?: data.oldText
                        expectedVersionByUri[data.documentUri] = data.documentVersion
                    } else if (expectedVersionByUri[data.documentUri] != data.documentVersion) {
                        return "The quick-fix command contains inconsistent document versions"
                    }
                    val expectedText = expectedTextByUri.getValue(data.documentUri)
                    if (data.oldText != expectedText) {
                        return "The quick-fix source text no longer matches the document"
                    }
                    expectedTextByUri[data.documentUri] = data.newText
                    null
                }
                is ModCommandData.CreateFile,
                is ModCommandData.DeleteFile,
                is ModCommandData.MoveFile,
                is ModCommandData.Navigate,
                is ModCommandData.DisplayMessage -> null
            }
        }

        return visit(command)
    }

    private fun execute(
        command: ModCommandData,
        client: LanguageClient,
        changedFiles: MutableMap<String, String>,
    ): CompletableFuture<Any> = when (command) {
        ModCommandData.Nothing -> CompletableFuture.completedFuture(true)
        is ModCommandData.Composite -> command.commands.fold(
            CompletableFuture.completedFuture<Any>(true),
        ) { previous, child ->
            previous.thenCompose { execute(child, client, changedFiles) }
        }
        is ModCommandData.UpdateFileText -> applyUpdate(command, client, changedFiles)
        is ModCommandData.CreateFile -> applyCreate(command, client, changedFiles)
        is ModCommandData.DeleteFile -> applyWorkspaceEdit(
            client,
            WorkspaceEdit().apply {
                documentChanges = listOf(
                    Either.forRight<TextDocumentEdit, ResourceOperation>(LspDeleteFile(command.fileUri)),
                )
            },
            "Delete " + command.fileUri,
        )
        is ModCommandData.MoveFile -> applyWorkspaceEdit(
            client,
            WorkspaceEdit().apply {
                documentChanges = listOf(
                    Either.forRight<TextDocumentEdit, ResourceOperation>(
                        LspRenameFile(command.fileUri, command.targetUri),
                    ),
                )
            },
            "Move " + command.fileUri,
        )
        is ModCommandData.Navigate -> showDocument(command, client, changedFiles)
        is ModCommandData.DisplayMessage -> {
            client.showMessage(
                MessageParams(
                    if (command.kind == ModDisplayMessage.MessageKind.ERROR) MessageType.Error else MessageType.Info,
                    command.message,
                ),
            )
            CompletableFuture.completedFuture(true)
        }
    }

    private fun applyUpdate(
        command: ModCommandData.UpdateFileText,
        client: LanguageClient,
        changedFiles: MutableMap<String, String>,
    ): CompletableFuture<Any> {
        val previousText = changedFiles[command.documentUri]
            ?: documentStore.get(command.documentUri)?.analysisText
            ?: command.oldText
        if (previousText != command.oldText) {
            return failed("The quick-fix command's old text does not match the current document state")
        }
        if (command.oldText == command.newText) {
            changedFiles[command.documentUri] = command.newText
            return CompletableFuture.completedFuture(true)
        }

        val currentDocument = documentStore.get(command.documentUri)
        val document = currentDocument?.withText(
            newText = command.oldText,
            newVersion = currentDocument.version,
        ) ?: LspTextDocument(
            uri = command.documentUri,
            languageId = null,
            version = command.documentVersion ?: 0,
            text = command.oldText,
        )
        val edits = try {
            LspTextEditsComputer.computeTextEdits(document, command.oldText, command.newText)
        } catch (failure: Exception) {
            return failed("Could not compute the quick-fix edit: " + failure.message)
        }
        if (edits.isEmpty()) {
            changedFiles[command.documentUri] = command.newText
            return CompletableFuture.completedFuture(true)
        }

        val useVersionedDocumentEdit =
            command.documentVersion != null &&
                supportsDocumentChanges() &&
                command.documentUri !in changedFiles
        val edit = if (useVersionedDocumentEdit) {
            val textDocumentEdit = TextDocumentEdit(
                VersionedTextDocumentIdentifier(command.documentUri, command.documentVersion),
                edits.map { textEdit -> Either.forLeft<TextEdit, SnippetTextEdit>(textEdit) },
            )
            WorkspaceEdit(
                listOf(Either.forLeft<TextDocumentEdit, ResourceOperation>(textDocumentEdit)),
            )
        } else {
            WorkspaceEdit().apply { changes = mapOf(command.documentUri to edits) }
        }
        return applyWorkspaceEdit(
            client,
            edit,
            "Apply Cangjie quick fix",
        ).thenApply { result ->
            changedFiles[command.documentUri] = command.newText
            result
        }
    }

    private fun applyCreate(
        command: ModCommandData.CreateFile,
        client: LanguageClient,
        changedFiles: MutableMap<String, String>,
    ): CompletableFuture<Any> {
        val changes = mutableListOf<Either<TextDocumentEdit, ResourceOperation>>()
        changes += Either.forRight<TextDocumentEdit, ResourceOperation>(LspCreateFile(command.fileUri))
        when (val content = command.content) {
            ModCommandData.CreateFile.Content.Directory -> Unit
            is ModCommandData.CreateFile.Content.Text -> {
                val edit = TextEdit(Range(Position(0, 0), Position(0, 0)), content.text)
                changes += Either.forLeft<TextDocumentEdit, ResourceOperation>(
                    TextDocumentEdit(
                        VersionedTextDocumentIdentifier(command.fileUri, null),
                        listOf(Either.forLeft<TextEdit, SnippetTextEdit>(edit)),
                    ),
                )
            }
            is ModCommandData.CreateFile.Content.Binary ->
                return failed("LSP workspace edits cannot create binary files")
        }
        return applyWorkspaceEdit(
            client,
            WorkspaceEdit().apply { documentChanges = changes },
            "Create " + command.fileUri,
        ).thenApply { result ->
            (command.content as? ModCommandData.CreateFile.Content.Text)
                ?.let { content -> changedFiles[command.fileUri] = content.text }
            result
        }
    }

    private fun showDocument(
        command: ModCommandData.Navigate,
        client: LanguageClient,
        changedFiles: Map<String, String>,
    ): CompletableFuture<Any> {
        val document = documentStore.get(command.documentUri)
        val text = changedFiles[command.documentUri]
            ?: document?.text
            ?: VirtualFileManager.getInstance().findFileByUrl(command.documentUri)?.findDocument()?.text
        val targetDocument = text?.let { targetText ->
            LspTextDocument(
                uri = command.documentUri,
                languageId = document?.languageId,
                version = document?.version ?: command.documentVersion ?: 0,
                text = targetText,
            )
        }
        val selectionStart = command.selectionStart.takeIf { it >= 0 } ?: command.caret
        val selectionEnd = command.selectionEnd.takeIf { it >= 0 } ?: command.caret
        val selection = if (targetDocument != null && selectionStart >= 0 && selectionEnd >= 0) {
            Range(
                targetDocument.positionAt(selectionStart),
                targetDocument.positionAt(selectionEnd),
            )
        } else {
            null
        }
        return client.showDocument(
            ShowDocumentParams(command.documentUri).apply {
                external = false
                takeFocus = selection != null
                this.selection = selection
            },
        ).thenApply { result -> result as Any }
    }

    private fun applyWorkspaceEdit(
        client: LanguageClient,
        edit: WorkspaceEdit,
        label: String,
    ): CompletableFuture<Any> =
        client.applyEdit(ApplyWorkspaceEditParams(edit, label)).thenApply { response ->
            if (!response.isApplied) {
                throw ResponseErrorException(
                    ResponseError(
                        ResponseErrorCode.RequestFailed,
                        response.failureReason ?: "The client rejected the quick-fix edit",
                        null,
                    ),
                )
            }
            response as Any
        }

    private fun failed(message: String): CompletableFuture<Any> =
        CompletableFuture.failedFuture(
            ResponseErrorException(ResponseError(ResponseErrorCode.RequestFailed, message, null)),
        )
}
