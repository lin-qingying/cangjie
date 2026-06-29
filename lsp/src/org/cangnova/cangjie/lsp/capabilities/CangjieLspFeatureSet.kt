package org.cangnova.cangjie.lsp.capabilities

/**
 * LSP 框架层对外暴露的特性矩阵。
 *
 * 真实运行时能力 = `descriptor.features` 与 `analysisFacade.supportedFeatures` 的交集。
 * 这样协议层声明和语义实现层能力可以独立演进，但最终对客户端呈现的仍是一致能力面。
 */
data class CangjieLspFeatureSet(
    /** 是否支持补全。 */
    val completion: Boolean = false,
    /** 是否支持悬停信息。 */
    val hover: Boolean = false,
    /** 是否支持签名帮助。 */
    val signatureHelp: Boolean = false,
    /** 是否支持声明跳转。 */
    val declaration: Boolean = false,
    /** 是否支持定义跳转。 */
    val definition: Boolean = false,
    /** 是否支持类型定义跳转。 */
    val typeDefinition: Boolean = false,
    /** 是否支持实现跳转。 */
    val implementation: Boolean = false,
    /** 是否支持引用查找。 */
    val references: Boolean = false,
    /** 是否支持文档高亮。 */
    val documentHighlight: Boolean = false,
    /** 是否支持文档符号。 */
    val documentSymbol: Boolean = false,
    /** 是否支持工作区符号。 */
    val workspaceSymbol: Boolean = false,
    /** 是否支持 code action。 */
    val codeAction: Boolean = false,
    /** 是否支持文档格式化。 */
    val formatting: Boolean = false,
    /** 是否支持重命名。 */
    val rename: Boolean = false,
    /** 是否支持折叠范围。 */
    val foldingRange: Boolean = false,
    /** 是否支持选择范围。 */
    val selectionRange: Boolean = false,
    /** 是否支持语义 token。 */
    val semanticTokens: Boolean = false,
    /** 是否支持内联提示。 */
    val inlayHints: Boolean = false,
    /** 是否支持诊断能力。 */
    val diagnostics: Boolean = false,
) {
    /**
     * 与另一个功能集合求交集。
     *
     * 仅当两侧都支持同一能力时，该能力才会在返回结果中启用。
     */
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
        /**
         * 返回全部能力关闭的功能集合。
         */
        fun none(): CangjieLspFeatureSet = CangjieLspFeatureSet()

        /**
         * 返回框架层默认支持的完整功能集合。
         *
         * 最终是否暴露仍需经过客户端协商和 analysis facade 能力求交。
         */
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
