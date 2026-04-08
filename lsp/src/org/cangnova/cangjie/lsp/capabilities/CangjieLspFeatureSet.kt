package org.cangnova.cangjie.lsp.capabilities

/**
 * LSP 框架层对外暴露的特性矩阵。
 *
 * 真实运行时能力 = `descriptor.features` 与 `analysisFacade.supportedFeatures` 的交集。
 * 这样协议层声明和语义实现层能力可以独立演进，但最终对客户端呈现的仍是一致能力面。
 */
data class CangjieLspFeatureSet(
    val completion: Boolean = false,
    val hover: Boolean = false,
    val signatureHelp: Boolean = false,
    val declaration: Boolean = false,
    val definition: Boolean = false,
    val typeDefinition: Boolean = false,
    val implementation: Boolean = false,
    val references: Boolean = false,
    val documentHighlight: Boolean = false,
    val documentSymbol: Boolean = false,
    val workspaceSymbol: Boolean = false,
    val codeAction: Boolean = false,
    val formatting: Boolean = false,
    val rename: Boolean = false,
    val foldingRange: Boolean = false,
    val selectionRange: Boolean = false,
    val semanticTokens: Boolean = false,
    val inlayHints: Boolean = false,
    val diagnostics: Boolean = false,
) {
    fun intersect(other: CangjieLspFeatureSet): CangjieLspFeatureSet {
        return CangjieLspFeatureSet(
            completion = completion && other.completion,
            hover = hover && other.hover,
            signatureHelp = signatureHelp && other.signatureHelp,
            declaration = declaration && other.declaration,
            definition = definition && other.definition,
            typeDefinition = typeDefinition && other.typeDefinition,
            implementation = implementation && other.implementation,
            references = references && other.references,
            documentHighlight = documentHighlight && other.documentHighlight,
            documentSymbol = documentSymbol && other.documentSymbol,
            workspaceSymbol = workspaceSymbol && other.workspaceSymbol,
            codeAction = codeAction && other.codeAction,
            formatting = formatting && other.formatting,
            rename = rename && other.rename,
            foldingRange = foldingRange && other.foldingRange,
            selectionRange = selectionRange && other.selectionRange,
            semanticTokens = semanticTokens && other.semanticTokens,
            inlayHints = inlayHints && other.inlayHints,
            diagnostics = diagnostics && other.diagnostics,
        )
    }

    companion object {
        fun none(): CangjieLspFeatureSet = CangjieLspFeatureSet()

        fun frameworkDefaults(): CangjieLspFeatureSet = CangjieLspFeatureSet(
            completion = true,
            hover = true,
            signatureHelp = true,
            declaration = true,
            definition = true,
            typeDefinition = true,
            implementation = true,
            references = true,
            documentHighlight = true,
            documentSymbol = true,
            workspaceSymbol = true,
            codeAction = true,
            formatting = true,
            rename = true,
            foldingRange = true,
            selectionRange = true,
            semanticTokens = true,
            inlayHints = true,
            diagnostics = true,
        )
    }
}
