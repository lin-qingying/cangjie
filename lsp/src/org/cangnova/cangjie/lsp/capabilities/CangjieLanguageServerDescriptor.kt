package org.cangnova.cangjie.lsp.capabilities

import org.cangnova.cangjie.lsp.semantic.CangJieSemanticTokenModifier
import org.cangnova.cangjie.lsp.semantic.CangJieSemanticTokenType
import org.eclipse.lsp4j.CodeActionKind
import org.eclipse.lsp4j.PositionEncodingKind
import org.eclipse.lsp4j.TextDocumentSyncKind

/**
 * 语言服务器的静态能力描述。
 *
 * 这个对象只描述协议层“可以声明什么”，不直接代表最终启用的能力；
 * 最终可见能力还要再与 Analysis facade 的支持矩阵求交。
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
    /**
     * pull diagnostics 只有在客户端与服务端对刷新、缓存和结果增量语义完全对齐后才应启用。
     *
     * 当前默认关闭，服务端继续通过 `publishDiagnostics` 提供诊断，
     * 避免在端到端验证尚未完成前过早声明 `diagnosticProvider`。
     */
    val pullDiagnosticsEnabled: Boolean = false,
    val semanticTokenTypes: List<String> = CangJieSemanticTokenType.lspValues,
    val semanticTokenModifiers: List<String> = CangJieSemanticTokenModifier.lspValues,
    val executeCommands: List<String> = emptyList(),
    val features: CangjieLspFeatureSet = CangjieLspFeatureSet.frameworkDefaults(),
)
