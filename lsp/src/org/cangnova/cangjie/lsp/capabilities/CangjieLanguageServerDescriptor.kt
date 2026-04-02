package org.cangnova.cangjie.lsp.capabilities

import org.eclipse.lsp4j.CodeActionKind
import org.eclipse.lsp4j.PositionEncodingKind
import org.eclipse.lsp4j.TextDocumentSyncKind

/**
 * LSP 服务器静态描述。
 */
data class CangjieLanguageServerDescriptor(
    val name: String = "Cangjie Language Server",
    val version: String? = null,
    val positionEncoding: String = PositionEncodingKind.UTF16,
    val openClose: Boolean = true,
    val changeSyncKind: TextDocumentSyncKind = TextDocumentSyncKind.Incremental,
    val saveIncludeText: Boolean = false,
    val workspaceFoldersSupported: Boolean = true,
    val workspaceFolderChangeNotificationsId: String = "workspace/didChangeWorkspaceFolders",
    val completionResolveProvider: Boolean = false,
    val completionTriggerCharacters: List<String> = listOf(".", ":", "@"),
    val signatureHelpTriggerCharacters: List<String> = listOf("(", ","),
    val codeActionKinds: List<String> = listOf(
        CodeActionKind.QuickFix,
        CodeActionKind.Refactor,
        CodeActionKind.Source,
    ),
    val renamePrepareProvider: Boolean = true,
    val diagnosticIdentifier: String = "cangjie",
    val semanticTokenTypes: List<String> = listOf(
        "namespace",
        "type",
        "class",
        "enum",
        "interface",
        "struct",
        "typeParameter",
        "parameter",
        "variable",
        "property",
        "enumMember",
        "function",
        "method",
        "macro",
        "keyword",
        "comment",
        "string",
        "number",
        "operator",
    ),
    val semanticTokenModifiers: List<String> = listOf(
        "declaration",
        "definition",
        "readonly",
        "static",
        "deprecated",
        "abstract",
        "defaultLibrary",
    ),
    val executeCommands: List<String> = emptyList(),
    val features: CangjieLspFeatureSet = CangjieLspFeatureSet.frameworkDefaults(),
)
