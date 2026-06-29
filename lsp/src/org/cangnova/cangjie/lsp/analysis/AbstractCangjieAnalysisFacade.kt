package org.cangnova.cangjie.lsp.analysis

import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DeclarationParams
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.DocumentHighlight
import org.eclipse.lsp4j.DocumentHighlightParams
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.FoldingRange
import org.eclipse.lsp4j.FoldingRangeRequestParams
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.InlayHint
import org.eclipse.lsp4j.InlayHintParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior
import org.eclipse.lsp4j.PrepareRenameResult
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.RenameParams
import org.eclipse.lsp4j.SelectionRange
import org.eclipse.lsp4j.SelectionRangeParams
import org.eclipse.lsp4j.SemanticTokens
import org.eclipse.lsp4j.SemanticTokensParams
import org.eclipse.lsp4j.SemanticTokensRangeParams
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.SignatureHelpParams
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.TypeDefinitionParams
import org.eclipse.lsp4j.WorkspaceEdit
import org.eclipse.lsp4j.WorkspaceDocumentDiagnosticReport
import org.eclipse.lsp4j.WorkspaceSymbol
import org.eclipse.lsp4j.WorkspaceSymbolParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.jsonrpc.messages.Either3

/**
 * LSP 到 Analysis API 适配层的基础实现。
 *
 * 目前 Analysis API 还没有稳定接入点的能力，必须继续显式保留 TODO，
 * 这样缺失能力不会被伪装成“空实现”而悄悄吞掉问题。
 */
abstract class AbstractCangjieAnalysisFacade : CangjieAnalysisFacade {
    /**
     * 报告指定 LSP 功能尚未接入 Analysis API。
     *
     * 默认实现使用 TODO 明确失败，防止未实现能力被误认为合法空结果。
     */
    protected fun unsupported(feature: String, document: LspTextDocument? = null): Nothing {
        val scope = document?.uri ?: "workspace"
        TODO("Analysis API has not provided the LSP integration for $feature yet: $scope")
    }

    /**
     * 默认诊断收集实现，显式标记该能力未接入。
     */
    override fun collectDiagnostics(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
    ): List<Diagnostic> = unsupported("textDocument/diagnostic", document)

    /**
     * 默认工作区诊断收集实现，显式标记该能力未接入。
     */
    override fun collectWorkspaceDiagnostics(
        context: CangjieAnalysisRequestContext,
    ): List<WorkspaceDocumentDiagnosticReport> = unsupported("workspace/diagnostic")

    /**
     * 默认补全实现，显式标记该能力未接入。
     */
    override fun completion(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: CompletionParams,
    ): Either<List<CompletionItem>, CompletionList> = unsupported("textDocument/completion", document)

    /**
     * 默认悬停实现，显式标记该能力未接入。
     */
    override fun hover(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: HoverParams,
    ): Hover? = unsupported("textDocument/hover", document)

    /**
     * 默认签名帮助实现，显式标记该能力未接入。
     */
    override fun signatureHelp(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SignatureHelpParams,
    ): SignatureHelp? = unsupported("textDocument/signatureHelp", document)

    /**
     * 默认声明跳转实现，显式标记该能力未接入。
     */
    override fun declaration(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DeclarationParams,
    ): Either<List<Location>, List<LocationLink>> = unsupported("textDocument/declaration", document)

    /**
     * 默认定义跳转实现，显式标记该能力未接入。
     */
    override fun definition(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DefinitionParams,
    ): Either<List<Location>, List<LocationLink>> = unsupported("textDocument/definition", document)

    /**
     * 默认类型定义跳转实现，显式标记该能力未接入。
     */
    override fun typeDefinition(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: TypeDefinitionParams,
    ): Either<List<Location>, List<LocationLink>> = unsupported("textDocument/typeDefinition", document)

    /**
     * 默认实现跳转实现，显式标记该能力未接入。
     */
    override fun implementation(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: org.eclipse.lsp4j.ImplementationParams,
    ): Either<List<Location>, List<LocationLink>> = unsupported("textDocument/implementation", document)

    /**
     * 默认引用查找实现，显式标记该能力未接入。
     */
    override fun references(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: ReferenceParams,
    ): List<Location> = unsupported("textDocument/references", document)

    /**
     * 默认文档高亮实现，显式标记该能力未接入。
     */
    override fun documentHighlight(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DocumentHighlightParams,
    ): List<DocumentHighlight> = unsupported("textDocument/documentHighlight", document)

    /**
     * 默认文档符号实现，显式标记该能力未接入。
     */
    override fun documentSymbols(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DocumentSymbolParams,
    ): List<Either<SymbolInformation, DocumentSymbol>> = unsupported("textDocument/documentSymbol", document)

    /**
     * 默认工作区符号实现，显式标记该能力未接入。
     */
    override fun workspaceSymbols(
        context: CangjieAnalysisRequestContext,
        params: WorkspaceSymbolParams,
    ): Either<List<SymbolInformation>, List<WorkspaceSymbol>> = unsupported("workspace/symbol")

    /**
     * 默认 code action 实现，显式标记该能力未接入。
     */
    override fun codeActions(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: CodeActionParams,
    ): List<Either<Command, CodeAction>> = unsupported("textDocument/codeAction", document)

    /**
     * 默认格式化实现，显式标记该能力未接入。
     */
    override fun formatting(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DocumentFormattingParams,
    ): List<TextEdit> = unsupported("textDocument/formatting", document)

    /**
     * 默认重命名实现，显式标记该能力未接入。
     */
    override fun rename(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: RenameParams,
    ): WorkspaceEdit? = unsupported("textDocument/rename", document)

    /**
     * 默认 prepareRename 实现，显式标记该能力未接入。
     */
    override fun prepareRename(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: RenameParams,
    ): Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>? =
        unsupported("textDocument/prepareRename", document)

    /**
     * 默认折叠范围实现，显式标记该能力未接入。
     */
    override fun foldingRanges(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: FoldingRangeRequestParams,
    ): List<FoldingRange> = unsupported("textDocument/foldingRange", document)

    /**
     * 默认选择范围实现，显式标记该能力未接入。
     */
    override fun selectionRanges(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SelectionRangeParams,
    ): List<SelectionRange> = unsupported("textDocument/selectionRange", document)

    /**
     * 默认整篇语义 token 实现，显式标记该能力未接入。
     */
    override fun semanticTokensFull(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SemanticTokensParams,
    ): SemanticTokens? = unsupported("textDocument/semanticTokens/full", document)

    /**
     * 默认范围语义 token 实现，显式标记该能力未接入。
     */
    override fun semanticTokensRange(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SemanticTokensRangeParams,
    ): SemanticTokens? = unsupported("textDocument/semanticTokens/range", document)

    /**
     * 默认 inlay hint 实现，显式标记该能力未接入。
     */
    override fun inlayHints(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: InlayHintParams,
    ): List<InlayHint> = unsupported("textDocument/inlayHint", document)
}
