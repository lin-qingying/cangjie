package org.cangnova.cangjie.lsp.analysis

import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettings
import org.cangnova.cangjie.analysis.api.CaNonPublicApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.components.isSubClassOf
import org.cangnova.cangjie.analysis.api.resolution.*
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.symbols.*
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.codeinsight.folding.CangJieFoldingKind
import org.cangnova.cangjie.codeinsight.folding.CangJieFoldingRangeCollector
import org.cangnova.cangjie.formatter.CangJieCodeStyleSettingsFactory
import org.cangnova.cangjie.formatter.CangJieFormatter
import org.cangnova.cangjie.formatter.cangjieCommonSettings
import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocTag
import org.cangnova.cangjie.lsp.capabilities.CangjieLspFeatureSet
import org.cangnova.cangjie.lsp.semantic.CangJieSemanticToken
import org.cangnova.cangjie.lsp.semantic.CangJieSemanticTokenCollector
import org.cangnova.cangjie.lsp.semantic.CangJieSemanticTokenModifier
import org.cangnova.cangjie.lsp.semantic.CangJieSemanticTokenType
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType
import org.cangnova.cangjie.psi.psiUtil.getChildrenOfType
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.jsonrpc.messages.Either3

/**
 * 基于 Analysis API 的 LSP 语义适配器。
 *
 * 这个实现不再把 LSP 功能面限制在 diagnostics，而是把当前 Analysis API 与 references
 * 已经稳定支撑的能力统一接到一条 `document -> snapshot -> session` 主链上：
 * 1. 文档内语义能力全部基于实时 PSI snapshot；
 * 2. 工作区级符号能力统一复用打开文档与磁盘文件枚举；
 * 3. 引用、定义、悬停、补全、签名帮助共享同一套目标键和位置映射协议。
 */
class AnalysisApiCangjieAnalysisFacade(
    lifecycleContext: CangjieAnalysisLifecycleContext,
) : AbstractCangjieAnalysisFacade() {
    private val psiDocumentFactory = AnalysisApiPsiDocumentFactory(lifecycleContext)
    private val semanticSupport = AnalysisApiLspSemanticSupport(lifecycleContext, psiDocumentFactory)
    private val renameSupport = AnalysisApiLspRenameSupport(semanticSupport)

    override val supportedFeatures: CangjieLspFeatureSet = CangjieLspFeatureSet(
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
        rename = true,
        formatting = true,
        foldingRange = true,
        selectionRange = true,
        semanticTokens = true,
        diagnostics = true,
    )

    override fun didOpen(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        upsertSnapshot(document)
    }

    override fun didChange(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        upsertSnapshot(document)
    }

    override fun didSave(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        upsertSnapshot(document)
    }

    override fun didClose(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        psiDocumentFactory.removeSnapshot(document.uri)
    }

    override fun didChangeWorkspaceFolders(
        context: CangjieAnalysisRequestContext,
        added: List<WorkspaceFolder>,
        removed: List<WorkspaceFolder>,
    ) {
        // 打开文档的 PSI 快照现在是稳定事实源，workspace 变更后由 project structure 重新分类。
    }

    override fun didRefreshProjectStructure(context: CangjieAnalysisRequestContext) {
        // project structure 刷新只重算模块投影，不再重建 PSI 快照。
    }

    override fun collectDiagnostics(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
    ): List<Diagnostic> = semanticSupport.analyzeSnapshot(document) { snapshot ->
        val diagnostics = snapshot.psiFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        AnalysisApiDiagnostics.toLspDiagnostics(
            document = document,
            source = context.descriptor.diagnosticIdentifier,
            diagnostics = diagnostics,
        )
    }

    override fun collectWorkspaceDiagnostics(
        context: CangjieAnalysisRequestContext,
    ): List<WorkspaceDocumentDiagnosticReport> {
        return semanticSupport.workspaceFiles(context).map { workspaceFile ->
            val diagnostics = if (workspaceFile.openedDocument != null) {
                collectDiagnostics(context, workspaceFile.analysisDocument)
            } else {
                semanticSupport.analyzeFile(workspaceFile.psiFile) {
                    val collected = workspaceFile.psiFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                    AnalysisApiDiagnostics.toLspDiagnostics(
                        document = workspaceFile.analysisDocument,
                        source = context.descriptor.diagnosticIdentifier,
                        diagnostics = collected,
                    )
                }
            }

            WorkspaceDocumentDiagnosticReport(
                WorkspaceFullDocumentDiagnosticReport(
                    diagnostics,
                    workspaceFile.documentUri,
                    workspaceFile.versionOrNull,
                ),
            )
        }
    }

    override fun completion(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: CompletionParams,
    ): Either<List<CompletionItem>, CompletionList> {
        val items = semanticSupport.analyzeSnapshot(document) { snapshot ->
            val simpleName = semanticSupport.findSimpleNameExpression(document, snapshot.psiFile, params.position)
            val variantItems = simpleName
                ?.references
                ?.asSequence()
                ?.flatMap { reference -> reference.variants.asSequence() }
                ?.mapNotNull(::toCompletionItem)
                ?.distinctBy(CompletionItem::getLabel)
                ?.sortedBy(CompletionItem::getLabel)
                ?.toList()
                .orEmpty()

            variantItems.ifEmpty {
                val fileScopeItems = snapshot.psiFile.getFileScope()
                    .declarations
                    .filterIsInstance<CaNamedSymbol>()
                    .map(CaNamedSymbol::name)
                    .toList()
                    .distinctBy(Name::asString)
                    .sortedBy(Name::asString)
                    .map { name -> CompletionItem(name.asString()).apply { kind = CompletionItemKind.Text } }

                /*
                 * 空前缀位置没有主引用 variants 时，仍然需要把真实工作区里的源码声明并入补全结果。
                 * 这里保留当前文件作用域结果，同时补齐 workspace 级源码声明，避免多文件工程退化成“当前文件 only”。
                 */
                val workspaceItems = semanticSupport.workspaceFiles(context)
                    .asSequence()
                    .flatMap { workspaceFile ->
                        workspaceFile.psiFile.collectDescendantsOfType<CjNamedDeclaration>().asSequence()
                    }
                    .mapNotNull { declaration -> declaration.name }
                    .distinct()
                    .sorted()
                    .map { name -> CompletionItem(name).apply { kind = CompletionItemKind.Text } }
                    .toList()

                (fileScopeItems + workspaceItems)
                    .distinctBy(CompletionItem::getLabel)
                    .sortedBy(CompletionItem::getLabel)
            }
        }
        return Either.forLeft(items)
    }

    override fun hover(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: HoverParams,
    ): Hover? = semanticSupport.analyzeSnapshot(document) { snapshot ->
        val target = semanticSupport.findPrimaryTarget(document, snapshot.psiFile, params.position)
            ?: return@analyzeSnapshot null
        val declarationSymbol = target.toPublicSymbol(this, snapshot.psiFile) as? CaDeclarationSymbol
            ?: return@analyzeSnapshot null

        val rendered = declarationSymbol.render()
        val documentation = documentationOf(declarationSymbol)
        val hoverText = buildString {
            append("```cangjie\n")
            append(rendered)
            append("\n```")
            if (!documentation.isNullOrBlank()) {
                append("\n\n")
                append(documentation)
            }
        }

        Hover().apply {
            contents = Either.forRight(MarkupContent(MarkupKind.MARKDOWN, hoverText))
            range = semanticSupport.hoverRange(document, snapshot.psiFile, params.position)
        }
    }

    override fun signatureHelp(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SignatureHelpParams,
    ): SignatureHelp? = semanticSupport.analyzeSnapshot(document) { snapshot ->
        val callExpression = semanticSupport.findCallExpression(document, snapshot.psiFile, params.position) ?: return@analyzeSnapshot null
        val callInfo = callExpression.resolveToCall() ?: return@analyzeSnapshot null
        val call = callInfo.successfulCallOrNull<CaCallableMemberCall<*, *>>()
            ?: callInfo.singleCallOrNull<CaCallableMemberCall<*, *>>()
            ?: callInfo.calls.filterIsInstance<CaCallableMemberCall<*, *>>().firstOrNull()
            ?: return@analyzeSnapshot null
        val callableSymbol = call.symbol
        val callableDeclaration = callableSymbol.psiSafe<CjCallableDeclaration>()
        val signature = buildSignatureInformation(callableDeclaration, callableSymbol)
        val activeParameter = activeParameterIndex(callExpression, document.analysisOffsetAt(params.position))

        SignatureHelp().apply {
            signatures = listOf(signature)
            activeSignature = 0
            this.activeParameter = activeParameter
        }
    }

    override fun declaration(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DeclarationParams,
    ): Either<List<Location>, List<LocationLink>> = definitionLike(document, params.position)

    override fun definition(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DefinitionParams,
    ): Either<List<Location>, List<LocationLink>> = definitionLike(document, params.position)

    override fun typeDefinition(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: TypeDefinitionParams,
    ): Either<List<Location>, List<LocationLink>> = Either.forLeft(
        semanticSupport.analyzeSnapshot(document) { snapshot ->
            resolveTypeDefinitionTarget(document, snapshot.psiFile, params.position)
                ?.let { target -> locationOfClassLikeSymbol(target) }
                ?.let(::listOf)
                .orEmpty()
        },
    )

    override fun implementation(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: ImplementationParams,
    ): Either<List<Location>, List<LocationLink>> = Either.forLeft(
        semanticSupport.analyzeSnapshot(document) { snapshot ->
            val target = resolveImplementationTarget(document, snapshot.psiFile, params.position)
                ?: return@analyzeSnapshot emptyList()
            val targetPointer = target.createPointer()
            val targetClassId = target.classId ?: return@analyzeSnapshot emptyList()

            val locations = linkedSetOf<Location>()
            semanticSupport.workspaceFiles(context).forEach { workspaceFile ->
                semanticSupport.analyzeFile(workspaceFile.psiFile) {
                    val sessionTarget = restoreSymbol(targetPointer) as? CaClassSymbol
                        ?: return@analyzeFile
                    workspaceFile.psiFile.collectDescendantsOfType<CjClassLikeDeclaration>().forEach { declaration ->
                        val declarationSymbol = declaration.toPublicSymbol(this, workspaceFile.psiFile) as? CaClassSymbol
                            ?: return@forEach
                        val declarationClassId = declarationSymbol.classId ?: return@forEach
                        if (declarationClassId == targetClassId) return@forEach

                        val directlyTargetsSuperType = (declaration as? CjTypeStatement)
                            ?.superTypeListEntries
                            ?.asSequence()
                            ?.mapNotNull(CjSuperTypeListEntry::typeAsUserType)
                            ?.mapNotNull(CjUserType::referenceExpression)
                            ?.mapNotNull { superTypeReference ->
                                semanticSupport.run { targetKeyFor(superTypeReference) as? AnalysisApiLspTargetKey.ClassLike }
                            }
                            ?.any { superTypeTarget: AnalysisApiLspTargetKey.ClassLike ->
                                superTypeTarget.classId == targetClassId
                            }
                            ?: false
                        val isSubClass = declarationSymbol.isSubClassOf(sessionTarget)
                        if (!directlyTargetsSuperType && !isSubClass) return@forEach

                        semanticSupport.toLocation(declaration.nameIdentifier ?: declaration)?.let(locations::add)
                    }
                }
            }

            locations.toList()
        },
    )

    override fun references(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: ReferenceParams,
    ): List<Location> {
        val targetKeys = semanticSupport.analyzeSnapshot(document) { snapshot ->
            semanticSupport.findTargetElements(document, snapshot.psiFile, params.position)
                .mapNotNull(semanticSupport::targetKeyFor)
                .toSet()
        }
        if (targetKeys.isEmpty()) {
            return emptyList()
        }

        val locations = linkedSetOf<Location>()
        semanticSupport.workspaceFiles(context).forEach { workspaceFile ->
            semanticSupport.analyzeFile(workspaceFile.psiFile) {
                semanticSupport.referenceLikeElements(workspaceFile.psiFile).forEach { referenceLike ->
                    if (semanticSupport.run { targetKeyForReferenceLike(referenceLike) } in targetKeys) {
                        val locationElement = when (referenceLike) {
                            is CjSimpleNameExpression -> referenceLike.referencedNameElement
                            else -> referenceLike
                        }
                        semanticSupport.toLocation(locationElement)?.let(locations::add)
                    }
                }
            }
        }

        if (!params.context.isIncludeDeclaration) {
            return locations.toList()
        }

        val existingKeys = locations.mapTo(linkedSetOf()) { location ->
            "${location.uri}:${location.range.start.line}:${location.range.start.character}:${location.range.end.line}:${location.range.end.character}"
        }
        semanticSupport.workspaceFiles(context).forEach { workspaceFile ->
            semanticSupport.analyzeFile(workspaceFile.psiFile) {
                workspaceFile.psiFile.collectDescendantsOfType<CjNamedDeclaration>().forEach { declaration ->
                    if (semanticSupport.targetKeyFor(declaration) !in targetKeys) return@forEach
                    val location = semanticSupport.toLocation(declaration.nameIdentifier ?: declaration) ?: return@forEach
                    val locationKey =
                        "${location.uri}:${location.range.start.line}:${location.range.start.character}:${location.range.end.line}:${location.range.end.character}"
                    if (existingKeys.add(locationKey)) {
                        locations += location
                    }
                }
            }
        }

        return locations.toList()
    }

    override fun documentHighlight(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DocumentHighlightParams,
    ): List<DocumentHighlight> = semanticSupport.analyzeSnapshot(document) { snapshot ->
        val targetKeys = semanticSupport.findTargetElements(document, snapshot.psiFile, params.position)
            .mapNotNull(semanticSupport::targetKeyFor)
            .toSet()
        if (targetKeys.isEmpty()) return@analyzeSnapshot emptyList()

        buildList {
            semanticSupport.referenceLikeElements(snapshot.psiFile).forEach { referenceLike ->
                if (semanticSupport.run { targetKeyForReferenceLike(referenceLike) } in targetKeys) {
                    val rangeSource = when (referenceLike) {
                        is CjSimpleNameExpression -> referenceLike.referencedNameElement
                        else -> referenceLike
                    }
                    add(
                        DocumentHighlight().apply {
                            range = document.analysisRangeOf(
                                rangeSource.textRange.startOffset,
                                rangeSource.textRange.endOffset,
                            )
                            kind = DocumentHighlightKind.Text
                        },
                    )
                }
            }

            snapshot.psiFile.collectDescendantsOfType<CjNamedDeclaration>().forEach { namedDeclaration ->
                if (semanticSupport.targetKeyFor(namedDeclaration) in targetKeys) {
                    val nameIdentifier = namedDeclaration.nameIdentifier ?: return@forEach
                    add(
                        DocumentHighlight().apply {
                            range = document.analysisRangeOf(nameIdentifier.textRange.startOffset, nameIdentifier.textRange.endOffset)
                            kind = DocumentHighlightKind.Write
                        },
                    )
                }
            }
        }.distinctBy { highlight -> "${highlight.range.start.line}:${highlight.range.start.character}-${highlight.range.end.line}:${highlight.range.end.character}" }
    }

    override fun documentSymbols(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DocumentSymbolParams,
    ): List<Either<SymbolInformation, DocumentSymbol>> {
        val snapshot = psiDocumentFactory.createAnalyzableSnapshot(document)
        return snapshot.psiFile.declarations
            .mapNotNull { declaration -> buildDocumentSymbol(document, declaration) }
            .map(Either<SymbolInformation, DocumentSymbol>::forRight)
    }

    override fun workspaceSymbols(
        context: CangjieAnalysisRequestContext,
        params: WorkspaceSymbolParams,
    ): Either<List<SymbolInformation>, List<WorkspaceSymbol>> {
        val query = params.query.trim()
        val queryLower = query.lowercase()

        val workspaceSymbols = semanticSupport.workspaceFiles(context)
            .asSequence()
            .flatMap { file -> collectWorkspaceSymbols(file.psiFile).asSequence() }
            .filter { symbol ->
                queryLower.isBlank() ||
                    symbol.name.lowercase().contains(queryLower) ||
                    symbol.containerName?.lowercase()?.contains(queryLower) == true
            }
            .sortedBy(WorkspaceSymbol::getName)
            .toList()

        return Either.forRight(workspaceSymbols)
    }

    override fun codeActions(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: CodeActionParams,
    ): List<Either<Command, CodeAction>> = emptyList()

    override fun formatting(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DocumentFormattingParams,
    ): List<TextEdit> {
        val psiFile = psiDocumentFactory.upsertSnapshot(document)
        val formattedText = CangJieFormatter.format(psiFile, params.toCodeStyleSettings())
        return if (formattedText == document.analysisText) {
            emptyList()
        } else {
            listOf(
                TextEdit(
                    document.analysisRangeOf(0, document.analysisText.length),
                    formattedText,
                ),
            )
        }
    }

    override fun rename(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: RenameParams,
    ): WorkspaceEdit? = renameSupport.rename(context, document, params)

    override fun prepareRename(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: RenameParams,
    ): Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>? = renameSupport.prepareRename(document, params)

    override fun foldingRanges(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: FoldingRangeRequestParams,
    ): List<FoldingRange> = semanticSupport.analyzeSnapshot(document) { snapshot ->
        val foldingDocument = DocumentImpl(document.analysisText)
        CangJieFoldingRangeCollector.collect(snapshot.psiFile, foldingDocument).map { region ->
            val start = document.analysisPositionAt(region.range.startOffset)
            val end = document.analysisPositionAt(region.range.endOffset)
            FoldingRange().apply {
                startLine = start.line
                startCharacter = start.character
                endLine = end.line
                endCharacter = end.character
                kind = region.kind.toLspFoldingRangeKind()
            }
        }
    }

    override fun selectionRanges(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SelectionRangeParams,
    ): List<SelectionRange> = semanticSupport.analyzeSnapshot(document) { snapshot ->
        params.positions.map { position ->
            val leaf = semanticSupport.findSemanticLeaf(document, snapshot.psiFile, position)
            buildSelectionRange(document, leaf)
        }
    }

    override fun semanticTokensFull(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SemanticTokensParams,
    ): SemanticTokens? = semanticSupport.analyzeSnapshot(document) { snapshot ->
        val tokens = CangJieSemanticTokenCollector.collect(snapshot.psiFile)
        SemanticTokens(encodeSemanticTokens(document, tokens))
    }

    override fun semanticTokensRange(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SemanticTokensRangeParams,
    ): SemanticTokens? = semanticSupport.analyzeSnapshot(document) { snapshot ->
        val startOffset = document.analysisOffsetAt(params.range.start)
        val endOffset = document.analysisOffsetAt(params.range.end)
        val tokens = CangJieSemanticTokenCollector.collect(snapshot.psiFile, TextRange(startOffset, endOffset))
        SemanticTokens(encodeSemanticTokens(document, tokens))
    }

    override fun inlayHints(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: InlayHintParams,
    ): List<InlayHint> = emptyList()

    /**
     * 打开文档的 PSI 快照必须先稳定落库，再交给 project structure 选择 overlay 或 dangling 语义。
     */
    private fun upsertSnapshot(document: LspTextDocument) {
        psiDocumentFactory.upsertSnapshot(document)
    }

    /**
     * `declaration/definition` 共用同一条目标定位链。
     *
     * 仓颉当前 Analysis API 公开语义里尚未区分 declaration 与 definition 的不同跳转模型，
     * 因此这里显式共用“引用 -> 公开符号/源码声明 -> LSP Location”协议。
     */
    private fun definitionLike(
        document: LspTextDocument,
        position: Position,
    ): Either<List<Location>, List<LocationLink>> = Either.forLeft(
        semanticSupport.analyzeSnapshot(document) { snapshot ->
            semanticSupport.findTargetElements(document, snapshot.psiFile, position)
                .mapNotNull(semanticSupport::toLocation)
                .distinctBy { location ->
                    "${location.uri}:${location.range.start.line}:${location.range.start.character}:${location.range.end.line}:${location.range.end.character}"
                }
        },
    )

    /**
     * 统一从“引用 / 调用 / 表达式 / 声明”恢复当前位置对应的目标类型声明。
     *
     * `typeDefinition` 不能依赖 LSP 私有语法猜测，而是必须显式复用公开 Analysis API 的
     * `resolveToSymbol / resolveToCall / expressionType / returnType / classLikeSymbol` 语义链。
     */
    private fun CaSession.resolveTypeDefinitionTarget(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): CaClassLikeSymbol? {
        semanticSupport.findPrimaryTarget(document, file, position)
            ?.toPublicSymbol(this, file)
            ?.let { symbol -> classLikeTargetOfSymbol(symbol) }
            ?.let { return it }

        val reference = semanticSupport.findReferenceExpression(document, file, position)
        reference?.resolveToSymbol()?.let { symbol ->
            classLikeTargetOfSymbol(symbol)?.let { return it }
        }

        val callExpression = semanticSupport.findCallExpression(document, file, position)
        callExpression?.resolveToCall()
            ?.successfulFunctionCallOrNull()
            ?.symbol
            ?.returnType
            ?.classLikeSymbol
            ?.let { return it }

        val expression = semanticSupport.findExpression(document, file, position)
        expression?.expressionType?.classLikeSymbol?.let { return it }

        val declaration = semanticSupport.findNamedDeclaration(document, file, position)
        declaration?.toPublicSymbol(this, file)?.let { symbol ->
            classLikeTargetOfSymbol(symbol)?.let { return it }
        }

        return null
    }

    /**
     * `implementation` 的目标必须是稳定的 class-like 声明。
     *
     * 若当前位置本身就是 class-like，则直接以其为目标；否则沿用 `typeDefinition`
     * 的统一类型恢复协议，保证两条 LSP 能力共享同一套公开语义入口。
     */
    private fun CaSession.resolveImplementationTarget(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): CaClassSymbol? {
        semanticSupport.findNamedDeclaration(document, file, position)
            ?.let { declaration ->
                (declaration as? CjTypeStatement)
                    ?.toPublicSymbol(this, file)
                    ?.let { publicSymbol -> publicSymbol as? CaClassSymbol }
                    ?.let { return it }
            }

        val target = semanticSupport.findPrimaryTarget(document, file, position)
        if (target is CjTypeStatement) {
            target.toPublicSymbol(this, file)
                ?.let { publicSymbol -> publicSymbol as? CaClassSymbol }
                ?.let { return it }
        }

        return resolveTypeDefinitionTarget(document, file, position) as? CaClassSymbol
    }

    /**
     * 统一把公开符号投影到“类型定义目标”。
     */
    private fun CaSession.classLikeTargetOfSymbol(symbol: CaSymbol): CaClassLikeSymbol? = when (symbol) {
        is CaClassLikeSymbol -> symbol
        is CaCallableSymbol -> symbol.returnType.classLikeSymbol
        else -> null
    }

    /**
     * 统一把 class-like 公开符号映射到源码位置。
     */
    private fun locationOfClassLikeSymbol(symbol: CaClassLikeSymbol): Location? =
        symbol.psi?.let(semanticSupport::toLocation)

    /**
     * Selection Range 明确复用 PSI 父链，而不是在 LSP 层自建语法树。
     *
     * 这样所有层级边界都由真实的仓颉 PSI 决定：标识符 -> 表达式 -> 声明 -> 容器 -> 文件。
     */
    private fun buildSelectionRange(
        document: LspTextDocument,
        leaf: PsiElement?,
    ): SelectionRange {
        val chain = generateSequence(leaf) { current -> current.parent }
            .mapNotNull { element -> element.textRange }
            .distinctBy { range -> "${range.startOffset}:${range.endOffset}" }
            .toList()

        if (chain.isEmpty()) {
            val empty = document.analysisRangeOf(0, 0)
            return SelectionRange(empty, null)
        }

        var parent: SelectionRange? = null
        chain.asReversed().forEach { textRange ->
            parent = SelectionRange(
                document.analysisRangeOf(textRange.startOffset, textRange.endOffset),
                parent,
            )
        }
        return parent!!
    }

    /**
     * 把 PSI 声明恢复成公开符号。
     *
     * 这里统一走 Analysis API 的公开查询协议，不允许组件层直接碰 low-level 细节。
     */
    private fun CjNamedDeclaration.toPublicSymbol(
        session: CaSession,
        useSiteFile: CjFile,
    ): CaSymbol? {
        return when (this) {
            is CjClassLikeDeclaration -> getClassId()?.let(session::getClassLikeSymbol)
            is CjCallableDeclaration -> restoreCallableSymbol(session, useSiteFile)
            else -> null
        }
    }

    private fun PsiElement.toPublicSymbol(
        session: CaSession,
        useSiteFile: CjFile,
    ): CaSymbol? {
        return when (this) {
            is CjNamedDeclaration -> toPublicSymbol(session, useSiteFile)
            else -> null
        }
    }

    private fun CjCallableDeclaration.restoreCallableSymbol(
        session: CaSession,
        useSiteFile: CjFile,
    ): CaCallableSymbol? {
        val declarationName = runCatching { nameAsSafeName }.getOrNull() ?: return null
        val classLikeContainer = parent as? CjAbstractClassBody
        if (classLikeContainer != null) {
            val owner = classLikeContainer.parent as? CjTypeStatement ?: return null
            val ownerSymbol = owner.getClassId()?.let(session::getClassLikeSymbol) ?: return null
            return session.run { ownerSymbol.declaredMemberScope }
                .callables(declarationName)
                .toList()
                .singleOrNull { candidate: CaCallableSymbol ->
                    candidate.psi == this@restoreCallableSymbol
                }
        }

        return session.getTopLevelCallableSymbols(useSiteFile.packageFqName, declarationName)
            .singleOrNull { candidate: CaCallableSymbol ->
                candidate.psi == this@restoreCallableSymbol
            }
    }

    private fun CaSession.buildSignatureInformation(
        callableDeclaration: CjCallableDeclaration?,
        callableSymbol: CaCallableSymbol,
    ): SignatureInformation {
        if (callableDeclaration == null) {
            return SignatureInformation().apply {
                label = callableSymbol.render()
                documentation = Either.forRight(MarkupContent(MarkupKind.MARKDOWN, documentationOf(callableSymbol).orEmpty()))
                parameters = emptyList()
            }
        }

        val parameterLabels = callableDeclaration.valueParameters.map { parameter ->
            buildString {
                append(parameter.name ?: "_")
                parameter.typeReference?.text?.takeIf(String::isNotBlank)?.let { typeText ->
                    append(": ")
                    append(typeText)
                }
            }
        }
        val label = buildString {
            append(callableDeclaration.name ?: callableSymbol.callableId?.callableName?.asString() ?: "<anonymous>")
            append("(")
            append(parameterLabels.joinToString(", "))
            append(")")
            callableDeclaration.typeReference?.text?.takeIf(String::isNotBlank)?.let { returnTypeText ->
                append(": ")
                append(returnTypeText)
            }
        }

        return SignatureInformation().apply {
            setLabel(label)
            documentation = documentationOf(callableSymbol)
                ?.takeIf(String::isNotBlank)
                ?.let { text -> Either.forRight(MarkupContent(MarkupKind.MARKDOWN, text)) }
            parameters = parameterLabels.map { parameterLabel ->
                ParameterInformation().apply { setLabel(parameterLabel) }
            }
        }
    }

    private fun activeParameterIndex(
        callExpression: CjCallExpression,
        caretOffset: Int,
    ): Int {
        val arguments = callExpression.valueArguments
        if (arguments.isEmpty()) return 0

        for ((index, argument) in arguments.withIndex()) {
            val range = argument.asElement().textRange
            if (caretOffset in range.startOffset..range.endOffset) {
                return index
            }
        }

        return arguments.indexOfLast { argument -> argument.asElement().textRange.startOffset <= caretOffset }
            .coerceAtLeast(0)
    }

    private fun toCompletionItem(variant: Any): CompletionItem? = when (variant) {
        is String -> CompletionItem(variant).apply { kind = CompletionItemKind.Text }
        is CjNamedDeclaration -> {
            val label = variant.name ?: return null
            CompletionItem(label).apply {
                kind = symbolKindOf(variant).toCompletionItemKind()
                detail = variant.fqName?.asString()
            }
        }

        else -> variant.toString()
            .takeIf { text -> text.isNotBlank() && text != "null" }
            ?.let { label -> CompletionItem(label).apply { kind = CompletionItemKind.Text } }
    }

    private fun buildDocumentSymbol(
        document: LspTextDocument,
        declaration: CjDeclaration,
    ): DocumentSymbol? {
        val namedDeclaration = declaration as? CjNamedDeclaration ?: return null
        val nameIdentifier = namedDeclaration.nameIdentifier ?: namedDeclaration
        val symbol = DocumentSymbol().apply {
            name = namedDeclaration.name ?: "<anonymous>"
            kind = symbolKindOf(namedDeclaration)
            range = document.analysisRangeOf(declaration.textRange.startOffset, declaration.textRange.endOffset)
            selectionRange = document.analysisRangeOf(nameIdentifier.textRange.startOffset, nameIdentifier.textRange.endOffset)
            detail = namedDeclaration.fqName?.asString()
        }

        val nestedChildren = when (declaration) {
            is CjDeclarationContainer -> semanticSupport.declarationChildren(declaration)
            else -> emptyList()
        }.mapNotNull { child -> buildDocumentSymbol(document, child) }

        symbol.children = nestedChildren
        return symbol
    }

    private fun collectWorkspaceSymbols(file: CjFile): List<WorkspaceSymbol> {
        val fileUri = semanticSupport.documentUriOf(file) ?: return emptyList()

        fun collect(container: CjDeclarationContainer, ownerName: String?): List<WorkspaceSymbol> {
            return container.declarations.flatMap { declaration ->
                val namedDeclaration = declaration as? CjNamedDeclaration ?: return@flatMap emptyList()
                val location = semanticSupport.toLocation(namedDeclaration.nameIdentifier ?: namedDeclaration) ?: return@flatMap emptyList()
                val current = WorkspaceSymbol().apply {
                    name = namedDeclaration.name ?: "<anonymous>"
                    kind = symbolKindOf(namedDeclaration)
                    containerName = ownerName
                    this.location = Either.forLeft(location)
                }

                val children = (declaration as? CjDeclarationContainer)
                    ?.let { nested -> collect(nested, namedDeclaration.name) }
                    .orEmpty()

                listOf(current) + children
            }
        }

        return collect(file, ownerName = null)
    }

    private fun symbolKindOf(declaration: CjNamedDeclaration): SymbolKind = when (declaration) {
        is CjClass -> SymbolKind.Class
        is CjStruct -> SymbolKind.Struct
        is CjInterface -> SymbolKind.Interface
        is CjEnum -> SymbolKind.Enum
        is CjTypeAlias -> SymbolKind.TypeParameter
        is CjNamedFunction -> SymbolKind.Function
        is CjProperty -> SymbolKind.Property
        is CjVariableDeclaration -> SymbolKind.Variable
        is CjParameter -> SymbolKind.Variable
        else -> SymbolKind.Object
    }

    private fun SymbolKind.toCompletionItemKind(): CompletionItemKind = when (this) {
        SymbolKind.Class -> CompletionItemKind.Class
        SymbolKind.Struct -> CompletionItemKind.Struct
        SymbolKind.Interface -> CompletionItemKind.Interface
        SymbolKind.Enum -> CompletionItemKind.Enum
        SymbolKind.Function, SymbolKind.Method -> CompletionItemKind.Function
        SymbolKind.Property -> CompletionItemKind.Property
        SymbolKind.Variable -> CompletionItemKind.Variable
        else -> CompletionItemKind.Text
    }

    private fun CangJieFoldingKind.toLspFoldingRangeKind(): String = when (this) {
        CangJieFoldingKind.COMMENT -> FoldingRangeKind.Comment
        CangJieFoldingKind.IMPORTS -> FoldingRangeKind.Imports
        CangJieFoldingKind.REGION -> FoldingRangeKind.Region
    }

    /**
     * 共享 collector 只产出绝对 offset token；
     * LSP 层负责按协议 legend 编码为 delta line / delta start 结构。
     */
    private fun encodeSemanticTokens(
        document: LspTextDocument,
        tokens: List<CangJieSemanticToken>,
    ): List<Int> {
        if (tokens.isEmpty()) return emptyList()

        val typeIndex = CangJieSemanticTokenType.lspValues.withIndex().associate { it.value to it.index }
        val modifierIndex = CangJieSemanticTokenModifier.lspValues.withIndex().associate { it.value to it.index }
        val data = ArrayList<Int>(tokens.size * 5)

        var previousLine = 0
        var previousStartCharacter = 0

        tokens.forEach { token ->
            val start = document.analysisPositionAt(token.range.startOffset)
            val end = document.analysisPositionAt(token.range.endOffset)
            if (start.line != end.line) return@forEach

            val deltaLine = start.line - previousLine
            val deltaStart = if (deltaLine == 0) start.character - previousStartCharacter else start.character
            val length = end.character - start.character
            if (length <= 0) return@forEach

            data += deltaLine
            data += deltaStart
            data += length
            data += (typeIndex[token.type.lspName] ?: 0)
            data += encodeModifiers(token.modifiers, modifierIndex)

            previousLine = start.line
            previousStartCharacter = start.character
        }

        return data
    }

    private fun encodeModifiers(
        modifiers: Set<CangJieSemanticTokenModifier>,
        modifierIndex: Map<String, Int>,
    ): Int {
        var bitset = 0
        modifiers.forEach { modifier ->
            val index = modifierIndex[modifier.lspName] ?: return@forEach
            bitset = bitset or (1 shl index)
        }
        return bitset
    }

    private fun DocumentFormattingParams.toCodeStyleSettings(): CodeStyleSettings {
        val settings = CangJieCodeStyleSettingsFactory.createDefaultSettings()
        val indentOptions = settings.cangjieCommonSettings.initIndentOptions()
        indentOptions.INDENT_SIZE = options.tabSize
        indentOptions.CONTINUATION_INDENT_SIZE = options.tabSize
        indentOptions.TAB_SIZE = options.tabSize
        indentOptions.USE_TAB_CHARACTER = !options.isInsertSpaces
        return settings
    }

    @OptIn(CjNonPublicApi::class, CaNonPublicApi::class)
    private fun CaSession.documentationOf(symbol: CaDeclarationSymbol): String? {
        val declaration = symbol.psiSafe<CjDeclaration>() ?: return null
        return declaration.findCDoc()?.renderToDocumentationString()
    }

    @OptIn(CjNonPublicApi::class)
    private fun CDocCommentDescriptor.renderToDocumentationString(): String? {
        val rendered = buildString {
            val primaryContent = primaryTag.getContent().trim()
            if (primaryContent.isNotEmpty()) {
                append(primaryContent)
            }

            collectRenderableTags().forEach { tag ->
                val renderedTag = tag.renderTagLine()
                if (renderedTag.isNotEmpty()) {
                    if (isNotEmpty()) appendLine()
                    append(renderedTag)
                }
            }
        }

        return rendered.ifBlank { null }
    }

    @OptIn(CjNonPublicApi::class)
    private fun CDocCommentDescriptor.collectRenderableTags(): List<CDocTag> {
        return buildList {
            (primaryTag as? CDocSection)
                ?.getChildrenOfType<CDocTag>()
                ?.filterTo(this) { tag -> tag.name != null }

            additionalSections
                .filterNot { section -> section == primaryTag }
                .forEach { section ->
                    section.getChildrenOfType<CDocTag>()
                        .filterTo(this) { tag -> tag.name != null }
                }
        }
    }

    private fun CDocTag.renderTagLine(): String {
        val tagName = name ?: return ""
        val content = getContent().trim()
        val subjectName = getSubjectName()

        return buildString {
            append("@")
            append(tagName)
            if (!subjectName.isNullOrBlank()) {
                append(" ")
                append(subjectName)
            }
            if (content.isNotEmpty()) {
                append(" ")
                append(content)
            }
        }
    }
}
