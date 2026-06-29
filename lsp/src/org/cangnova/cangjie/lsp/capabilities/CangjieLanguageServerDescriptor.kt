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
    /**
     * 初始化响应中暴露给客户端的服务端名称。
     */
    val name: String = "Cangjie Language Server",

    /**
     * 初始化响应中暴露给客户端的可选版本号。
     */
    val version: String? = null,

    /**
     * 服务端偏好的 LSP 位置编码。
     */
    val positionEncoding: String = PositionEncodingKind.UTF16,

    /**
     * 是否声明支持 textDocument/didOpen 与 didClose。
     */
    val openClose: Boolean = true,

    /**
     * 文本文档同步的变更粒度。
     */
    val changeSyncKind: TextDocumentSyncKind = TextDocumentSyncKind.Incremental,

    /**
     * 保存通知中是否要求客户端带上全文文本。
     */
    val saveIncludeText: Boolean = false,

    /**
     * 是否声明支持 workspace folders。
     */
    val workspaceFoldersSupported: Boolean = true,

    /**
     * workspace folder 变更通知注册标识。
     */
    val workspaceFolderChangeNotificationsId: String = "workspace/didChangeWorkspaceFolders",

    /**
     * completion item 是否支持 resolve 阶段。
     */
    val completionResolveProvider: Boolean = false,

    /**
     * 触发补全请求的字符集合。
     */
    val completionTriggerCharacters: List<String> = listOf(".", ":", "@"),

    /**
     * 触发 signature help 请求的字符集合。
     */
    val signatureHelpTriggerCharacters: List<String> = listOf("(", ","),

    /**
     * 服务端声明支持的 code action kind 集合。
     */
    val codeActionKinds: List<String> = listOf(
        CodeActionKind.QuickFix,
        CodeActionKind.Refactor,
        CodeActionKind.Source,
    ),

    /**
     * rename provider 是否支持 prepareRename。
     */
    val renamePrepareProvider: Boolean = true,

    /**
     * pull diagnostics provider 的协议标识。
     */
    val diagnosticIdentifier: String = "cangjie",
    /**
     * pull diagnostics 只有在客户端与服务端对刷新、缓存和结果增量语义完全对齐后才应启用。
     *
     * 当前默认关闭，服务端继续通过 `publishDiagnostics` 提供诊断，
     * 避免在端到端验证尚未完成前过早声明 `diagnosticProvider`。
     */
    val pullDiagnosticsEnabled: Boolean = false,

    /**
     * 语义 token legend 中声明的 token 类型名称。
     */
    val semanticTokenTypes: List<String> = CangJieSemanticTokenType.lspValues,

    /**
     * 语义 token legend 中声明的 token 修饰符名称。
     */
    val semanticTokenModifiers: List<String> = CangJieSemanticTokenModifier.lspValues,

    /**
     * workspace/executeCommand 可接受的命令名称集合。
     */
    val executeCommands: List<String> = emptyList(),

    /**
     * 服务端框架层默认愿意暴露的功能集合。
     */
    val features: CangjieLspFeatureSet = CangjieLspFeatureSet.frameworkDefaults(),
)
