package org.cangnova.cangjie.lsp.capabilities

import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.PositionEncodingKind

/**
 * LSP 初始化阶段的能力协商结果。
 *
 * 这里显式区分两类语义：
 * 1. `features` 表示在当前客户端下允许暴露的静态能力；
 * 2. `pullDiagnostics` / `workspaceFolders` / `positionEncoding` 表示需要额外参与
 *    `InitializeResult.capabilities` 结构构造的协商位。
 *
 * 这样可以避免把“服务端实现了什么”和“当前客户端真的接受什么”混在一起。
 */
data class CangjieClientCapabilityNegotiation(
    val features: CangjieLspFeatureSet,
    val pullDiagnostics: Boolean,
    val workspaceFolders: Boolean,
    val positionEncoding: String?,
)

/**
 * 服务端能力协商器。
 *
 * 设计原则：
 * 1. 对早期、广泛支持的核心能力默认保持开启，避免因客户端未显式上报 capability 而被误伤；
 * 2. 对 3.6+/3.14+/3.15+/3.17+ 的扩展能力，仅在客户端显式声明后再暴露；
 * 3. `publishDiagnostics` 与 pull-diagnostics 分离处理。前者是通知流，不依赖
 *    `diagnosticProvider`；后者必须在客户端明确声明支持后才广告。
 */
object CangjieClientCapabilityNegotiator {

    fun negotiate(
        params: InitializeParams,
        serverFeatures: CangjieLspFeatureSet,
        descriptor: CangjieLanguageServerDescriptor,
    ): CangjieClientCapabilityNegotiation {
        val clientCapabilities = params.capabilities
        val textDocument = clientCapabilities?.textDocument
        val workspace = clientCapabilities?.workspace
        val general = clientCapabilities?.general

        val negotiatedFeatures = CangjieLspFeatureSet(
            completion = serverFeatures.completion && supportsCore(textDocument?.completion),
            hover = serverFeatures.hover && supportsCore(textDocument?.hover),
            signatureHelp = serverFeatures.signatureHelp && supportsCore(textDocument?.signatureHelp),
            declaration = serverFeatures.declaration && supportsAdvanced(textDocument?.declaration),
            definition = serverFeatures.definition && supportsCore(textDocument?.definition),
            typeDefinition = serverFeatures.typeDefinition && supportsAdvanced(textDocument?.typeDefinition),
            implementation = serverFeatures.implementation && supportsAdvanced(textDocument?.implementation),
            references = serverFeatures.references && supportsCore(textDocument?.references),
            documentHighlight = serverFeatures.documentHighlight && supportsCore(textDocument?.documentHighlight),
            documentSymbol = serverFeatures.documentSymbol && supportsCore(textDocument?.documentSymbol),
            workspaceSymbol = serverFeatures.workspaceSymbol && supportsCore(workspace?.symbol),
            codeAction = serverFeatures.codeAction && supportsCore(textDocument?.codeAction),
            formatting = serverFeatures.formatting && supportsCore(textDocument?.formatting),
            rename = serverFeatures.rename && supportsCore(textDocument?.rename),
            foldingRange = serverFeatures.foldingRange && supportsAdvanced(textDocument?.foldingRange),
            selectionRange = serverFeatures.selectionRange && supportsAdvanced(textDocument?.selectionRange),
            semanticTokens = serverFeatures.semanticTokens && supportsAdvanced(textDocument?.semanticTokens),
            inlayHints = serverFeatures.inlayHints && supportsAdvanced(textDocument?.inlayHint),
            diagnostics = serverFeatures.diagnostics && supportsDiagnosticsChannel(textDocument, workspace),
        )

        return CangjieClientCapabilityNegotiation(
            features = negotiatedFeatures,
            pullDiagnostics = descriptor.pullDiagnosticsEnabled &&
                negotiatedFeatures.diagnostics &&
                supportsPullDiagnostics(textDocument, workspace),
            workspaceFolders = descriptor.workspaceFoldersSupported && workspace?.workspaceFolders == true,
            positionEncoding = negotiatePositionEncoding(general?.positionEncodings, descriptor.positionEncoding),
        )
    }

    private fun supportsDiagnosticsChannel(
        textDocument: org.eclipse.lsp4j.TextDocumentClientCapabilities?,
        workspace: org.eclipse.lsp4j.WorkspaceClientCapabilities?,
    ): Boolean {
        if (textDocument == null && workspace == null) return true
        return textDocument?.publishDiagnostics != null ||
            textDocument?.diagnostic != null ||
            workspace?.diagnostics != null
    }

    private fun supportsPullDiagnostics(
        textDocument: org.eclipse.lsp4j.TextDocumentClientCapabilities?,
        workspace: org.eclipse.lsp4j.WorkspaceClientCapabilities?,
    ): Boolean {
        return textDocument?.diagnostic != null || workspace?.diagnostics != null
    }

    private fun supportsCore(@Suppress("UNUSED_PARAMETER") capability: Any?): Boolean = true

    /**
     * 新增能力默认按“未声明则不暴露”处理，避免把客户端未实现的扩展接口强塞进初始化结果。
     */
    private fun supportsAdvanced(capability: Any?): Boolean = capability != null

    /**
     * `positionEncoding` 只能从客户端提供的集合中选择；若客户端未声明，则按协议默认 UTF-16。
     */
    private fun negotiatePositionEncoding(
        clientEncodings: List<String>?,
        preferredEncoding: String,
    ): String? {
        if (clientEncodings.isNullOrEmpty()) {
            return PositionEncodingKind.UTF16
        }
        return when {
            preferredEncoding in clientEncodings -> preferredEncoding
            PositionEncodingKind.UTF16 in clientEncodings -> PositionEncodingKind.UTF16
            else -> null
        }
    }
}
