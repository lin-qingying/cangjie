package org.cangnova.cangjie.lsp.analysis

import org.cangnova.cangjie.lsp.capabilities.CangjieLspFeatureSet
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
import org.eclipse.lsp4j.WorkspaceFolder
import org.eclipse.lsp4j.WorkspaceSymbol
import org.eclipse.lsp4j.WorkspaceSymbolParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.jsonrpc.messages.Either3

/**
 * LSP 模块与真实分析模块之间的统一桥接接口。
 *
 * 这里定义的是框架契约，真正的 CFIR/Analysis API 对接后续实现。
 */
interface CangjieAnalysisFacade : AutoCloseable {
    /**
     * 当前分析实现实际支持的 LSP 功能集合。
     */
    val supportedFeatures: CangjieLspFeatureSet

    /**
     * 通知分析层某个文档已经打开。
     */
    fun didOpen(context: CangjieAnalysisRequestContext, document: LspTextDocument) {}

    /**
     * 通知分析层某个打开文档的内容已经变化。
     */
    fun didChange(context: CangjieAnalysisRequestContext, document: LspTextDocument) {}

    /**
     * 通知分析层某个打开文档已经保存。
     */
    fun didSave(context: CangjieAnalysisRequestContext, document: LspTextDocument) {}

    /**
     * 通知分析层某个文档已经关闭。
     */
    fun didClose(context: CangjieAnalysisRequestContext, document: LspTextDocument) {}

    /**
     * 通知分析层工作区目录集合已经变化。
     */
    fun didChangeWorkspaceFolders(
        context: CangjieAnalysisRequestContext,
        added: List<WorkspaceFolder>,
        removed: List<WorkspaceFolder>,
    ) {
    }

    /**
     * 工作区结构刷新后，允许分析后端重新绑定打开文档的快照模块。
     *
     * LSP 外层不再自己猜测哪些 snapshot 需要重建，而是把“结构已更新”这一事实显式通知给语义层。
     */
    fun didRefreshProjectStructure(context: CangjieAnalysisRequestContext) {}

    /**
     * 收集指定打开文档的诊断。
     *
     * 返回值使用 LSP Diagnostic 模型，调用方负责决定 push 或 pull 的发布方式。
     */
    fun collectDiagnostics(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
    ): List<Diagnostic>

    /**
     * 统一收集工作区级诊断。
     *
     * 这里要求后端同时覆盖：
     * 1. 打开文档对应的内存快照；
     * 2. 工作区内未打开但仍可见的磁盘源码文件。
     */
    fun collectWorkspaceDiagnostics(
        context: CangjieAnalysisRequestContext,
    ): List<WorkspaceDocumentDiagnosticReport>

    /**
     * 计算指定文档位置的补全结果。
     */
    fun completion(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: CompletionParams,
    ): Either<List<CompletionItem>, CompletionList>

    /**
     * 计算指定文档位置的悬停信息。
     */
    fun hover(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: HoverParams,
    ): Hover?

    /**
     * 计算指定文档位置的签名帮助。
     */
    fun signatureHelp(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SignatureHelpParams,
    ): SignatureHelp?

    /**
     * 解析指定位置的声明跳转目标。
     */
    fun declaration(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DeclarationParams,
    ): Either<List<Location>, List<LocationLink>>

    /**
     * 解析指定位置的定义跳转目标。
     */
    fun definition(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DefinitionParams,
    ): Either<List<Location>, List<LocationLink>>

    /**
     * 解析指定位置的类型定义跳转目标。
     */
    fun typeDefinition(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: TypeDefinitionParams,
    ): Either<List<Location>, List<LocationLink>>

    /**
     * 解析指定位置的实现跳转目标。
     */
    fun implementation(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: org.eclipse.lsp4j.ImplementationParams,
    ): Either<List<Location>, List<LocationLink>>

    /**
     * 查找指定位置符号的引用位置。
     */
    fun references(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: ReferenceParams,
    ): List<Location>

    /**
     * 计算指定位置的文档高亮结果。
     */
    fun documentHighlight(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DocumentHighlightParams,
    ): List<DocumentHighlight>

    /**
     * 收集当前文档中的符号列表或符号树。
     */
    fun documentSymbols(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DocumentSymbolParams,
    ): List<Either<SymbolInformation, DocumentSymbol>>

    /**
     * 收集工作区符号查询结果。
     */
    fun workspaceSymbols(
        context: CangjieAnalysisRequestContext,
        params: WorkspaceSymbolParams,
    ): Either<List<SymbolInformation>, List<WorkspaceSymbol>>

    /**
     * 计算指定范围或诊断对应的 code action。
     */
    fun codeActions(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: CodeActionParams,
    ): List<Either<Command, CodeAction>>

    /**
     * 计算文档格式化产生的文本编辑。
     */
    fun formatting(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DocumentFormattingParams,
    ): List<TextEdit>

    /**
     * 执行重命名并返回工作区编辑。
     */
    fun rename(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: RenameParams,
    ): WorkspaceEdit?

    /**
     * 准备重命名并返回可重命名范围。
     */
    fun prepareRename(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: RenameParams,
    ): Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>?

    /**
     * 计算当前文档的折叠范围。
     */
    fun foldingRanges(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: FoldingRangeRequestParams,
    ): List<FoldingRange>

    /**
     * 计算当前文档的选择范围链。
     */
    fun selectionRanges(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SelectionRangeParams,
    ): List<SelectionRange>

    /**
     * 计算整篇文档的语义 token。
     */
    fun semanticTokensFull(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SemanticTokensParams,
    ): SemanticTokens?

    /**
     * 计算指定范围内的语义 token。
     */
    fun semanticTokensRange(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SemanticTokensRangeParams,
    ): SemanticTokens?

    /**
     * 计算当前文档范围内的 inlay hints。
     */
    fun inlayHints(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: InlayHintParams,
    ): List<InlayHint>

    /**
     * 释放分析 facade 持有的资源。
     */
    override fun close() {}
}
