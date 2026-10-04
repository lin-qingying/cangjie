package org.cangnova.cangjie.lsp.server

/**
 * 本服务端经 [CangjieRequestExecutor] 执行的 LSP 方法。
 *
 * 请求级耗时只有带上方法名才有诊断价值：语义补全慢、悬停慢、全文诊断慢，指向的优化方向
 * 完全不同。把方法名做成枚举而不是字符串，是为了让每个调用点都必须显式声明自己测的是哪个
 * 请求——留一个"未标注"的兜底桶，只会让新增请求悄悄落进无名桶，看不出漏埋。
 *
 * @property lspMethod 对应的 LSP 方法名（JSON-RPC method），同时用作指标名段。
 */
enum class CangjieLspRequest(val lspMethod: String) {
    INITIALIZE("initialize"),

    PUBLISH_DIAGNOSTICS("textDocument/publishDiagnostics"),

    DID_OPEN("textDocument/didOpen"),

    DID_CHANGE("textDocument/didChange"),

    DID_CLOSE("textDocument/didClose"),

    DID_SAVE("textDocument/didSave"),

    COMPLETION("textDocument/completion"),

    HOVER("textDocument/hover"),

    SIGNATURE_HELP("textDocument/signatureHelp"),

    DECLARATION("textDocument/declaration"),

    DEFINITION("textDocument/definition"),

    TYPE_DEFINITION("textDocument/typeDefinition"),

    IMPLEMENTATION("textDocument/implementation"),

    REFERENCES("textDocument/references"),

    DOCUMENT_HIGHLIGHT("textDocument/documentHighlight"),

    DOCUMENT_SYMBOL("textDocument/documentSymbol"),

    CODE_ACTION("textDocument/codeAction"),

    FORMATTING("textDocument/formatting"),

    RANGE_FORMATTING("textDocument/rangeFormatting"),

    RENAME("textDocument/rename"),

    PREPARE_RENAME("textDocument/prepareRename"),

    FOLDING_RANGE("textDocument/foldingRange"),

    SELECTION_RANGE("textDocument/selectionRange"),

    SEMANTIC_TOKENS_FULL("textDocument/semanticTokens/full"),

    SEMANTIC_TOKENS_RANGE("textDocument/semanticTokens/range"),

    INLAY_HINT("textDocument/inlayHint"),

    DIAGNOSTIC("textDocument/diagnostic"),

    WORKSPACE_SYMBOL("workspace/symbol"),

    WORKSPACE_DIAGNOSTIC("workspace/diagnostic"),
}
