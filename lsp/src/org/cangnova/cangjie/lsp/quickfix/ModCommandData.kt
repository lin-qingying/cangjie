package org.cangnova.cangjie.lsp.quickfix

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.intellij.modcommand.ModCreateFile
import com.intellij.modcommand.ModCommand
import com.intellij.modcommand.ModCompositeCommand
import com.intellij.modcommand.ModDeleteFile
import com.intellij.modcommand.ModDisplayMessage
import com.intellij.modcommand.ModHighlight
import com.intellij.modcommand.ModNavigate
import com.intellij.modcommand.ModMoveFile
import com.intellij.modcommand.ModNothing
import com.intellij.modcommand.ModUpdateFileText
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.state.LspDocumentStore
import java.util.Base64
import java.util.logging.Logger

/**
 * 通过 LSP command 传输的平台命令数据。
 *
 * PSI、闭包和平台 action 不跨 JSON-RPC；PSI 更新由平台命令归约为文件旧文本和新文本。
 */
internal sealed interface ModCommandData {
    fun toJson(): JsonObject

    fun hasEffect(): Boolean = when (this) {
        Nothing -> false
        is Composite -> commands.any(ModCommandData::hasEffect)
        else -> true
    }

    fun isLspExecutable(): Boolean = when (this) {
        Nothing -> true
        is Composite -> commands.all(ModCommandData::isLspExecutable)
        is CreateFile -> content !is CreateFile.Content.Binary
        else -> true
    }

    fun requiredResourceOperations(): Set<String> = when (this) {
        is CreateFile -> setOf("create")
        is DeleteFile -> setOf("delete")
        is MoveFile -> setOf("rename")
        is Composite -> commands.flatMapTo(linkedSetOf(), ModCommandData::requiredResourceOperations)
        else -> emptySet()
    }

    fun requiresDocumentChanges(): Boolean = requiredResourceOperations().isNotEmpty()

    fun requiresApplyEdit(): Boolean = when (this) {
        Nothing, is Navigate, is DisplayMessage -> false
        is Composite -> commands.any(ModCommandData::requiresApplyEdit)
        else -> true
    }

    fun isApplicableTo(documentStore: LspDocumentStore): Boolean {
        val expectedTextByUri = linkedMapOf<String, String>()
        fun collect(data: ModCommandData): Boolean {
            return when (data) {
                Nothing -> true
                is Composite -> data.commands.all(::collect)
                is UpdateFileText -> {
                    val targetDocument = documentStore.get(data.documentUri)
                    if (data.documentVersion != null &&
                        (targetDocument == null || targetDocument.version != data.documentVersion)
                    ) {
                        return false
                    }
                    val expectedText = expectedTextByUri.getOrPut(data.documentUri) {
                        targetDocument?.analysisText ?: data.oldText
                    }
                    if (data.oldText != expectedText) return false
                    expectedTextByUri[data.documentUri] = data.newText
                    true
                }
                is CreateFile, is DeleteFile, is MoveFile, is Navigate, is DisplayMessage -> true
        }
        }
        return collect(this)
    }

    data object Nothing : ModCommandData {
        override fun toJson(): JsonObject = JsonObject().apply { addProperty(KEY_KIND, "nothing") }
    }

    data class Composite(val commands: List<ModCommandData>) : ModCommandData {
        override fun toJson(): JsonObject = JsonObject().apply {
            addProperty(KEY_KIND, "composite")
            add(KEY_COMMANDS, Gson().toJsonTree(commands.map(ModCommandData::toJson)))
        }
    }

    data class UpdateFileText(
        val documentUri: String,
        val documentVersion: Int?,
        val oldText: String,
        val newText: String,
    ) : ModCommandData {
        override fun toJson(): JsonObject = JsonObject().apply {
            addProperty(KEY_KIND, "updateFileText")
            addProperty(KEY_DOCUMENT_URI, documentUri)
            addProperty(KEY_DOCUMENT_VERSION, documentVersion)
            addProperty(KEY_OLD_TEXT, oldText)
            addProperty(KEY_NEW_TEXT, newText)
        }
    }

    data class CreateFile(
        val fileUri: String,
        val content: Content,
    ) : ModCommandData {
        sealed interface Content {
            data object Directory : Content
            data class Text(val text: String) : Content
            data class Binary(val base64: String) : Content
        }

        override fun toJson(): JsonObject = JsonObject().apply {
            addProperty(KEY_KIND, "createFile")
            addProperty(KEY_FILE_URI, fileUri)
            when (val fileContent = content) {
                Content.Directory -> addProperty(KEY_CONTENT_KIND, "directory")
                is Content.Text -> {
                    addProperty(KEY_CONTENT_KIND, "text")
                    addProperty(KEY_CONTENT, fileContent.text)
                }
                is Content.Binary -> {
                    addProperty(KEY_CONTENT_KIND, "binary")
                    addProperty(KEY_CONTENT, fileContent.base64)
                }
            }
        }
    }

    data class DeleteFile(val fileUri: String) : ModCommandData {
        override fun toJson(): JsonObject = JsonObject().apply {
            addProperty(KEY_KIND, "deleteFile")
            addProperty(KEY_FILE_URI, fileUri)
        }
    }

    data class MoveFile(
        val fileUri: String,
        val targetUri: String,
    ) : ModCommandData {
        override fun toJson(): JsonObject = JsonObject().apply {
            addProperty(KEY_KIND, "moveFile")
            addProperty(KEY_FILE_URI, fileUri)
            addProperty(KEY_TARGET_URI, targetUri)
        }
    }

    data class Navigate(
        val documentUri: String,
        val documentVersion: Int?,
        val selectionStart: Int,
        val selectionEnd: Int,
        val caret: Int,
    ) : ModCommandData {
        override fun toJson(): JsonObject = JsonObject().apply {
            addProperty(KEY_KIND, "navigate")
            addProperty(KEY_DOCUMENT_URI, documentUri)
            addProperty(KEY_DOCUMENT_VERSION, documentVersion)
            addProperty(KEY_SELECTION_START, selectionStart)
            addProperty(KEY_SELECTION_END, selectionEnd)
            addProperty(KEY_CARET, caret)
        }
    }

    data class DisplayMessage(
        val message: String,
        val kind: ModDisplayMessage.MessageKind,
    ) : ModCommandData {
        override fun toJson(): JsonObject = JsonObject().apply {
            addProperty(KEY_KIND, "displayMessage")
            addProperty(KEY_MESSAGE, message)
            addProperty(KEY_MESSAGE_KIND, kind.name)
        }
    }

    companion object {
        private const val KEY_KIND = "kind"
        private const val KEY_COMMANDS = "commands"
        private const val KEY_DOCUMENT_URI = "documentUri"
        private const val KEY_DOCUMENT_VERSION = "documentVersion"
        private const val KEY_OLD_TEXT = "oldText"
        private const val KEY_NEW_TEXT = "newText"
        private const val KEY_SELECTION_START = "selectionStart"
        private const val KEY_SELECTION_END = "selectionEnd"
        private const val KEY_CARET = "caret"
        private const val KEY_MESSAGE = "message"
        private const val KEY_MESSAGE_KIND = "messageKind"
        private const val KEY_FILE_URI = "fileUri"
        private const val KEY_TARGET_URI = "targetUri"
        private const val KEY_CONTENT_KIND = "contentKind"
        private const val KEY_CONTENT = "content"
        private val LOG = Logger.getLogger("org.cangnova.cangjie.lsp.quickfix.ModCommandData")

        /** 把平台命令转成传输数据；文本更新只能指向当前打开的仓颉文档。 */
        fun from(
            command: ModCommand,
            document: LspTextDocument,
            sourceFile: PsiFile,
            documentStore: LspDocumentStore,
        ): ModCommandData? {
            val sourceVirtualFile = sourceFile.virtualFile ?: return null
            return from(command, document, sourceVirtualFile, documentStore)
        }

        private fun from(
            command: ModCommand,
            document: LspTextDocument,
            sourceVirtualFile: VirtualFile,
            documentStore: LspDocumentStore,
        ): ModCommandData? {
            return when (command) {
                is ModNothing -> Nothing
                is ModCompositeCommand -> {
                    val children = command.commands.map { child ->
                        from(child, document, sourceVirtualFile, documentStore) ?: return null
                    }
                    Composite(children)
                }
                is ModUpdateFileText -> {
                    val uri = command.file.lspUri(document, sourceVirtualFile) ?: return null
                    val targetDocument = documentStore.get(uri)
                    UpdateFileText(
                        documentUri = uri,
                        documentVersion = targetDocument?.version,
                        oldText = command.oldText,
                        newText = command.newText,
                    )
                }
                is ModCreateFile -> {
                    val uri = command.file.url.lspUri() ?: return null
                    val content = when (val fileContent = command.content) {
                        is ModCreateFile.Directory -> CreateFile.Content.Directory
                        is ModCreateFile.Text -> CreateFile.Content.Text(fileContent.text)
                        is ModCreateFile.Binary ->
                            CreateFile.Content.Binary(Base64.getEncoder().encodeToString(fileContent.bytes))
                    }
                    CreateFile(uri, content)
                }
                is ModDeleteFile -> DeleteFile(command.file.url.lspUri() ?: return null)
                is ModMoveFile -> MoveFile(
                    fileUri = command.file.lspUri(document, sourceVirtualFile) ?: return null,
                    targetUri = command.targetFile.url.lspUri() ?: return null,
                )
                is ModNavigate -> {
                    val uri = command.file.lspUri(document, sourceVirtualFile) ?: return null
                    Navigate(
                        documentUri = uri,
                        documentVersion = document.version.takeIf { uri == document.uri },
                        selectionStart = command.selectionStart,
                        selectionEnd = command.selectionEnd,
                        caret = command.caret,
                    )
                }
                is ModDisplayMessage -> DisplayMessage(command.messageText, command.kind)
                // Highlight is an optional IDE visual effect and has no LSP text-edit representation.
                is ModHighlight -> Nothing
                else -> {
                    LOG.fine("Unsupported ModCommand subtype: " + command.javaClass.name)
                    null
                }
            }
        }

        /** 解码 LSP4J 为 workspace/executeCommand 参数构造的 Gson JSON。 */
        fun parse(value: Any?): ModCommandData? {
            val element = when (value) {
                is JsonElement -> value
                is Map<*, *> -> Gson().toJsonTree(value)
                is String -> runCatching { com.google.gson.JsonParser.parseString(value) }.getOrNull()
                else -> null
            } ?: return null
            if (!element.isJsonObject) return null
            val root = element.asJsonObject
            return parseObject(root)
        }

        private fun parseObject(root: JsonObject): ModCommandData? {
            return when (root.string(KEY_KIND)) {
                "nothing" -> Nothing
                "composite" -> {
                    val commands = root.getAsJsonArray(KEY_COMMANDS) ?: return null
                    Composite(commands.map { child ->
                        if (!child.isJsonObject) return null
                        parseObject(child.asJsonObject) ?: return null
                    })
                }
                "updateFileText" -> UpdateFileText(
                    documentUri = root.string(KEY_DOCUMENT_URI) ?: return null,
                    documentVersion = root.int(KEY_DOCUMENT_VERSION),
                    oldText = root.string(KEY_OLD_TEXT) ?: return null,
                    newText = root.string(KEY_NEW_TEXT) ?: return null,
                )
                "createFile" -> {
                    val content = when (root.string(KEY_CONTENT_KIND)) {
                        "directory" -> CreateFile.Content.Directory
                        "text" -> CreateFile.Content.Text(root.string(KEY_CONTENT) ?: return null)
                        "binary" -> CreateFile.Content.Binary(root.string(KEY_CONTENT) ?: return null)
                        else -> return null
                    }
                    CreateFile(root.string(KEY_FILE_URI) ?: return null, content)
                }
                "deleteFile" -> DeleteFile(root.string(KEY_FILE_URI) ?: return null)
                "moveFile" -> MoveFile(
                    fileUri = root.string(KEY_FILE_URI) ?: return null,
                    targetUri = root.string(KEY_TARGET_URI) ?: return null,
                )
                "navigate" -> Navigate(
                    documentUri = root.string(KEY_DOCUMENT_URI) ?: return null,
                    documentVersion = root.int(KEY_DOCUMENT_VERSION),
                    selectionStart = root.int(KEY_SELECTION_START) ?: return null,
                    selectionEnd = root.int(KEY_SELECTION_END) ?: return null,
                    caret = root.int(KEY_CARET) ?: return null,
                )
                "displayMessage" -> DisplayMessage(
                    message = root.string(KEY_MESSAGE) ?: return null,
                    kind = root.string(KEY_MESSAGE_KIND)
                        ?.let { name -> runCatching { ModDisplayMessage.MessageKind.valueOf(name) }.getOrNull() }
                        ?: return null,
                )
                else -> null
            }
        }

        private fun JsonObject.string(key: String): String? =
            get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

        private fun JsonObject.int(key: String): Int? =
            get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
                ?.let { element -> runCatching { element.asInt }.getOrNull() }

        private fun String.lspUri(): String? =
            runCatching {
                if (startsWith("mock://")) replaceFirst("mock://", "file://") else this
            }.getOrNull()

        private fun VirtualFile.lspUri(
            document: LspTextDocument,
            sourceVirtualFile: VirtualFile,
        ): String? = if (this == sourceVirtualFile || url == sourceVirtualFile.url) {
            document.uri
        } else {
            url.lspUri()
        }
    }
}
