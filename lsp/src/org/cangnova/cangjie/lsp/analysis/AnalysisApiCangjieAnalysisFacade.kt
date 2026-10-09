package org.cangnova.cangjie.lsp.analysis

import com.google.gson.JsonObject
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Computable
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
import org.cangnova.cangjie.ide.completion.api.CompletionKind
import org.cangnova.cangjie.ide.completion.api.statistics.CompletionRejection
import org.cangnova.cangjie.ide.completion.api.statistics.CompletionStage
import org.cangnova.cangjie.ide.completion.api.statistics.completionStatistics
import org.cangnova.cangjie.ide.completion.impl.cfir.cfirCompletionServiceOrNull
import org.cangnova.cangjie.ide.completion.impl.shared.PlatformCompletionRequest
import org.cangnova.cangjie.ide.completion.impl.shared.dummy.CompletionDummyIdentifierProvider
import org.cangnova.cangjie.ide.completion.impl.shared.resolve.completionDeclarationResolverOrNull
import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocTag
import org.cangnova.cangjie.lsp.capabilities.CangjieLspFeatureSet
import org.cangnova.cangjie.lsp.completion.LspCompletionCommands
import org.cangnova.cangjie.lsp.completion.LspCompletionItemConverter
import org.cangnova.cangjie.lsp.server.LspRequestCancellation
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
    /**
     * LSP 文档到 PSI 快照的工厂。
     */
    private val psiDocumentFactory = AnalysisApiPsiDocumentFactory(lifecycleContext)

    /**
     * 生命周期上下文：补全路径需要工程、描述符开关与补全会话存储。
     */
    private val lifecycle = lifecycleContext

    /**
     * Analysis API 语义查询和 LSP 坐标转换的共享支持层。
     */
    private val semanticSupport = AnalysisApiLspSemanticSupport(lifecycleContext, psiDocumentFactory)

    /**
     * LSP rename 协议适配器。
     */
    private val renameSupport = AnalysisApiLspRenameSupport(semanticSupport)

    /**
     * Analysis API facade 当前支持的 LSP 功能集合。
     */
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

    /**
     * 文档打开后创建或更新 PSI 快照。
     */
    override fun didOpen(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        upsertSnapshot(document)
    }

    /**
     * 文档变更后创建或更新 PSI 快照。
     */
    override fun didChange(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        upsertSnapshot(document)
        // 内容变了：旧会话的替换区间与候选都可能不再成立，留在那里只会让用户
        // 接受一个落在错误区间的编辑（applyCompletion 会因版本不同而拒绝）。
        lifecycle.completionSessions.invalidate(document.uri)
    }

    /**
     * 文档保存后刷新 PSI 快照。
     */
    override fun didSave(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        upsertSnapshot(document)
    }

    /**
     * 文档关闭后移除对应 PSI 快照。
     */
    override fun didClose(context: CangjieAnalysisRequestContext, document: LspTextDocument) {
        psiDocumentFactory.removeSnapshot(document.uri)
        lifecycle.completionSessions.invalidate(document.uri)
    }

    /**
     * 工作区目录变化后的语义层通知入口。
     *
     * 当前快照事实源不在此重建，目录变化由项目结构状态重新分类。
     */
    override fun didChangeWorkspaceFolders(
        context: CangjieAnalysisRequestContext,
        added: List<WorkspaceFolder>,
        removed: List<WorkspaceFolder>,
    ) {
        // 打开文档的 PSI 快照现在是稳定事实源，workspace 变更后由 project structure 重新分类。
    }

    /**
     * 项目结构刷新后的语义层通知入口。
     */
    override fun didRefreshProjectStructure(context: CangjieAnalysisRequestContext) {
        // project structure 刷新只重算模块投影，不再重建 PSI 快照。
    }

    /**
     * 收集单个打开文档的 Analysis API 诊断并转换为 LSP 诊断。
     */
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

    /**
     * 收集当前工作区所有可见源码文件的诊断报告。
     */
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

    /**
     * 计算当前位置的补全项。
     *
     * 默认走**与 IDE 共用的候选管线**（设计 L1：LSP 只做协议适配，不重新实现候选生成）。
     * 旧实现留在 [legacyCompletion]，由 `descriptor.useLegacyCompletion` 打开——
     * 两条路径不混用（旧实现按击键遍历工作区、类别一律 `Text`、无导入计划，
     * 混用会让候选质量类验收无法判定，见设计 §7.11.5）。
     */
    override fun completion(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: CompletionParams,
    ): Either<List<CompletionItem>, CompletionList> {
        if (lifecycle.descriptor.useLegacyCompletion) {
            return legacyCompletion(context, document, params)
        }
        return Either.forLeft(pipelineCompletion(context, document, params))
    }

    /**
     * 共享补全管线（分支 B）。
     *
     * 形态由 15.1② 的实验判定（见 `implementation-log.md`）：
     * 1. **在副本上**做 dummy 修正——修正是 PSI 写，改文档那份会污染 `LspDocumentStore`；
     *    副本的路径与文档相同，因此仍落进正确的模块作用域；
     * 2. 修正在 **EDT 的写命令**内执行（本线程直调 `CommandProcessor` 会静默不执行）；
     * 3. 管线调用要有**进度指示器**（RPC 线程没有）；
     * 4. 候选存进会话存储，客户端稍后接受时由 `applyCompletion` 按会话键取回。
     *
     * 前缀与替换区间取自修正结果：用户在补全前敲的那半截标识符由服务端负责替换
     * （候选自带的 `textEdit` 是空的）。
     */
    private fun pipelineCompletion(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: CompletionParams,
    ): List<CompletionItem> {
        val project = lifecycle.environment.project
        val statistics = completionStatistics(project)
        val setupStartedNanos = System.nanoTime()
        val uri = document.uri
        val analysisText = document.analysisText
        val caret = document.analysisOffsetAt(params.position).coerceIn(0, analysisText.length)

        // 副本文本 = 文档文本 + 光标处的 dummy 标记（IDE 侧由平台插入，无头宿主自己插）。
        val copyText = StringBuilder(analysisText)
            .insert(caret, CompletionDummyIdentifierProvider.DUMMY_MARKER)
            .toString()
        val copy = LspAnalysisPsiFileFactory.createFile(project, uri, lspFileNameOf(uri), copyText)

        var request: PlatformCompletionRequest? = null
        runInWriteCommand(project) {
            val dummy = CompletionDummyIdentifierProvider.provideFor(copy, caret)
            if (dummy == null) {
                LOG.fine("dummy 修正在 $uri@$caret 返回 null：该位置不允许补全")
                return@runInWriteCommand
            }
            request = PlatformCompletionRequest(
                originalPosition = dummy.position,
                completionPosition = dummy.position,
                prefix = dummy.prefix,
                // LSP 没有智能补全触发：恒为 BASIC（15.1③）。
                kind = CompletionKind.BASIC,
                // 同一位置重复请求 = 用户再要一次，对应索引段放宽前缀匹配的那条分支。
                invocationCount = lifecycle.completionSessions.invocationCountFor(uri, document.version, caret),
                offset = caret,
                replaceStartOffset = caret - dummy.prefix.length,
                replaceEndOffset = caret,
                // 过期判定接**LSP 文档版本**：分析副本上的改动与用户文档无关（15.4②）。
                documentStamp = document.version.toLong(),
                positionElement = dummy.position,
            )
        }
        val completionRequest = request ?: run {
            // 位置不允许补全（原始字符串内部等）：这是分流结果，不是错误——记下来才能回答
            // 「用户没看到候选是位置不对还是别的」。
            statistics.recordRejection(CompletionRejection.POSITION_NOT_ALLOWED)
            statistics.recordStage(CompletionStage.SETUP, System.nanoTime() - setupStartedNanos)
            return emptyList()
        }
        statistics.recordStage(CompletionStage.SETUP, System.nanoTime() - setupStartedNanos)

        val service = cfirCompletionServiceOrNull(project)
        if (service == null) {
            // 不静默：服务未注册时用户会看到「没有候选」，日志里必须留下原因。
            LOG.warning("补全服务未注册（CfirCompletionServiceProvider 缺失），$uri 不产出候选")
            statistics.recordRejection(CompletionRejection.SERVICE_UNAVAILABLE)
            return emptyList()
        }
        /*
         * 取消闸门（前）：本容器里 `ProgressManager.checkCanceled()` 不生效——实测推入一个
         * 已取消的进度指示器它也不抛（容器的 `ProgressIndicatorProvider` 未接线，全局指示器恒为 null）。
         * 因此不能只靠指示器，至少要在这两处直接查请求的取消状态。
         */
        context.cancellation.throwIfCancelledRecording(statistics)
        /*
         * 进度指示器仍接**本次请求的取消状态**：容器将来接上全局指示器时，
         * 管线里的 `checkCanceled()` 就能中途收场，不必等整轮跑完。
         */
        val delegateIndicator = EmptyProgressIndicator()
        val indicator = object : ProgressIndicator by delegateIndicator {
            override fun isCanceled(): Boolean =
                context.cancellation.isCancelled() || delegateIndicator.isCanceled()
        }
        val snapshots = try {
            ProgressManager.getInstance().runProcess(
                Computable { service.complete(completionRequest) },
                indicator,
            )
        } catch (cancelled: ProcessCanceledException) {
            // 取消不是失败：转成 LSP 的 `RequestCancelled`，客户端据此丢弃结果而不弹错。
            context.cancellation.throwIfCancelledRecording(statistics)
            throw cancelled
        }
        // 取消闸门（后）：管线期间收到的取消在这里生效——宁可丢弃算好的结果，也不返回给用户
        // 一份他已经不要的候选（指示器路径若将来生效，这里也会同样命中）。
        context.cancellation.throwIfCancelledRecording(statistics)
        val session = lifecycle.completionSessions.create(
            uri = uri,
            version = document.version,
            replaceStart = completionRequest.replaceStartOffset,
            replaceEnd = completionRequest.replaceEndOffset,
            items = snapshots,
        )
        val renderStartedNanos = System.nanoTime()
        val items = snapshots.mapIndexed { index, snapshot ->
            LspCompletionItemConverter.toCompletionItem(snapshot, index, params.position, session.key)
        }
        statistics.recordStage(CompletionStage.RENDER, System.nanoTime() - renderStartedNanos)
        return items
    }

    /**
     * 取消闸门：已取消时记一笔原因再抛 `RequestCancelled`。
     *
     * 记原因是必要的——「用户取消」与「位置不允许」「服务没装」在数字上完全一样（都是没有候选），
     * 混在一起就回答不了「用户为什么没看到候选」。
     */
    private fun LspRequestCancellation.throwIfCancelledRecording(
        statistics: org.cangnova.cangjie.ide.completion.api.statistics.CompletionStatisticsSink,
    ) {
        if (isCancelled()) {
            statistics.recordRejection(CompletionRejection.CANCELLED)
            throwIfCancelled()
        }
    }

    /**
     * 补齐候选的文档（LSP `completionItem/resolve`）。
     *
     * 恢复分两步，各自用最合适的层：
     * 1. **恢复声明**交给 `CompletionDeclarationResolver`（在生成候选的那一层实现，
     *    用的是与快照工厂**同一套身份算法**；放到这里就要复制那份算法）；
     * 2. **渲染 Markdown** 用本类既有的 CDoc 渲染（与 hover 同一份，不另写渲染器）。
     *
     * 任一步拿不到结果都返回原 item：`resolve` 只是补文档，失败不该让候选消失。
     */
    @OptIn(CjNonPublicApi::class, CaNonPublicApi::class)
    override fun resolveCompletionItem(
        context: CangjieAnalysisRequestContext,
        item: CompletionItem,
    ): CompletionItem {
        // 与候选生成分开计时：resolve 是用户按下候选才触发的第二次分析，
        // 它的耗时直接影响「翻候选卡不卡」，混进 CANDIDATES 就看不出来了。
        // 失败路径（含所有 skipResolve 早退）同样计时——「没填文档」也花了时间。
        val statistics = completionStatistics(lifecycle.environment.project)
        val resolveStartedNanos = System.nanoTime()
        try {
            return resolveCompletionItemInternal(context, item)
        } finally {
            statistics.recordStage(CompletionStage.RESOLVE, System.nanoTime() - resolveStartedNanos)
        }
    }

    /**
     * resolve 的实际实现；计时与统计在 [resolveCompletionItem] 里做。
     */
    @OptIn(CjNonPublicApi::class, CaNonPublicApi::class)
    private fun resolveCompletionItemInternal(
        context: CangjieAnalysisRequestContext,
        item: CompletionItem,
    ): CompletionItem {
        val data = item.data as? JsonObject ?: return skipResolve("item 没有 data", item)
        val sessionKey = data.get(LspCompletionCommands.SESSION_KEY_FIELD)?.asString
            ?: return skipResolve("data 里没有会话键", item)
        val index = data.get(LspCompletionCommands.INDEX_FIELD)?.asInt
            ?: return skipResolve("data 里没有候选下标", item)
        val session = lifecycle.completionSessions.find(sessionKey)
            ?: return skipResolve("会话已过期（key=$sessionKey）", item)
        val snapshot = session.itemAt(index) ?: return skipResolve("会话里没有第 $index 个候选", item)
        val document = lifecycle.documentStore.get(session.uri)
            ?: return skipResolve("文档已关闭（${session.uri}）", item)
        val resolver = completionDeclarationResolverOrNull(lifecycle.environment.project)
            ?: return skipResolve("宿主没有装配候选声明解析器", item)

        val documentation = semanticSupport.analyzeSnapshot(document) { psiSnapshot ->
            val leaf = psiSnapshot.psiFile.findElementAt(session.replaceEnd)
                ?: return@analyzeSnapshot skipResolveDocumentation("使用点偏移 ${session.replaceEnd} 处没有元素")
            val useSite = leaf.nearestCangjieElement()
                ?: return@analyzeSnapshot skipResolveDocumentation("使用点不在仓颉 PSI 里")
            // 恢复与消费都在**同一个分析会话内**：符号不得跨会话带出（见契约 KDoc）。
            val documentation = resolver.resolveDeclaration(this@analyzeSnapshot, snapshot) { symbol ->
                documentationOf(symbol)
            }
            documentation ?: return@analyzeSnapshot skipResolveDocumentation(
                "按身份恢复不出声明（name=${snapshot.identity.name}, qualified=${snapshot.identity.qualifiedName}）",
            )
        }
        if (documentation.isNullOrBlank()) {
            // 恢复成功却没有文档：本容器里 light PSI 上的 `docComment` 为空（见 implementation-log）。
            LOG.fine("resolve 不填文档：声明恢复成功但没有可渲染的 CDoc（${snapshot.identity.qualifiedName}）")
            return item
        }
        return item.apply {
            this.documentation = Either.forRight(MarkupContent(MarkupKind.MARKDOWN, documentation))
        }
    }

    /**
     * 跳过 resolve 并留下原因。
     *
     * resolve 的失败路径**必须留痕**：它按协议是「原样返回」，客户端看不出区别，
     * 没有日志就只能看到「文档没填」，而这可能是过期、缺装配、身份对不上三种完全不同的原因。
     */
    private fun skipResolve(reason: String, item: CompletionItem): CompletionItem {
        LOG.fine("resolve 跳过：$reason")
        return item
    }

    /**
     * 同上，供 `analyzeSnapshot` 的 lambda 里使用（返回值即「没有文档」）。
     */
    private fun skipResolveDocumentation(reason: String): String? {
        LOG.fine("resolve 跳过：$reason")
        return null
    }

    /**
     * 取距离叶子最近的仓颉元素（叶子本身不是仓颉 PSI 时向上找）。
     */
    private fun PsiElement.nearestCangjieElement(): CjElement? {
        var current: PsiElement? = this
        while (current != null) {
            if (current is CjElement) return current
            current = current.parent
        }
        return null
    }

    /**
     * 在 EDT 的写命令内执行一段 PSI 写。
     *
     * 两条约束都**实测**过（见 `implementation-log.md` 15.1 节）：PSI 写必须在命令内，
     * 而 RPC 线程不是 EDT——直接在本线程调 `CommandProcessor.executeCommand` 会**静默不执行**
     * （命令块不运行、也不抛异常），因此统一走 `EventQueue.invokeAndWait`。
     */
    private fun runInWriteCommand(project: com.intellij.openapi.project.Project, body: () -> Unit) {
        java.awt.EventQueue.invokeAndWait {
            WriteCommandAction.runWriteCommandAction(project, COMMAND_NAME, null, Runnable(body))
        }
    }

    /**
     * 从文档 URI 取文件名；导入/包判定按文件名与路径走，取不到时给稳定占位名。
     */
    private fun lspFileNameOf(uri: String): String =
        uri.substringAfterLast('/').substringBefore('?').ifEmpty { "document.cj" }

    /**
     * 旧补全实现：引用 variants 优先，缺省回落到文件作用域 + 全工作区声明扫描。
     *
     * 保留是为了对照（设计 §7.11.5）：按击键遍历工作区、类别一律 `Text`、无导入计划，
     * 因此只在 `useLegacyCompletion` 打开时使用，不作为默认路径的一部分。
     */
    private fun legacyCompletion(
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

    /**
     * 计算当前位置的 hover 内容。
     *
     * 内容包含公开符号渲染结果和可渲染 CDoc 文档。
     */
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

    /**
     * 计算当前位置所在调用表达式的签名帮助。
     */
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

    /**
     * 解析声明跳转目标。
     */
    override fun declaration(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DeclarationParams,
    ): Either<List<Location>, List<LocationLink>> = definitionLike(document, params.position)

    /**
     * 解析定义跳转目标。
     */
    override fun definition(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: DefinitionParams,
    ): Either<List<Location>, List<LocationLink>> = definitionLike(document, params.position)

    /**
     * 解析类型定义跳转目标。
     */
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

    /**
     * 查找当前位置类型或 class-like 声明的实现者。
     */
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

    /**
     * 查找当前位置目标在工作区中的引用。
     */
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

    /**
     * 计算当前文档内与目标相关的高亮范围。
     */
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

    /**
     * 构造当前文档的符号树。
     */
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

    /**
     * 根据查询文本收集工作区符号。
     */
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

    /**
     * 计算 code action。
     *
     * 当前 Analysis API facade 尚未提供 code action，返回空列表。
     */
    override fun codeActions(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: CodeActionParams,
    ): List<Either<Command, CodeAction>> = emptyList()

    /**
     * 计算整篇文档格式化编辑。
     */
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

    /**
     * 执行重命名并返回工作区编辑。
     */
    override fun rename(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: RenameParams,
    ): WorkspaceEdit? = renameSupport.rename(context, document, params)

    /**
     * 准备重命名并返回可编辑名称范围。
     */
    override fun prepareRename(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: RenameParams,
    ): Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>? = renameSupport.prepareRename(document, params)

    /**
     * 收集当前文档折叠范围。
     */
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

    /**
     * 计算当前位置集合的选择范围链。
     */
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

    /**
     * 计算整篇文档语义 token。
     */
    override fun semanticTokensFull(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: SemanticTokensParams,
    ): SemanticTokens? = semanticSupport.analyzeSnapshot(document) { snapshot ->
        val tokens = CangJieSemanticTokenCollector.collect(snapshot.psiFile)
        SemanticTokens(encodeSemanticTokens(document, tokens))
    }

    /**
     * 计算指定范围内的语义 token。
     */
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

    /**
     * 计算 inlay hints。
     *
     * 当前未接入 inlay hint 语义，返回空列表。
     */
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

    /**
     * 将 PSI 元素恢复成公开符号。
     *
     * 当前只支持具名声明，其他元素返回 null。
     */
    private fun PsiElement.toPublicSymbol(
        session: CaSession,
        useSiteFile: CjFile,
    ): CaSymbol? {
        return when (this) {
            is CjNamedDeclaration -> toPublicSymbol(session, useSiteFile)
            else -> null
        }
    }

    /**
     * 从可调用 PSI 声明恢复对应的公开可调用符号。
     *
     * 成员声明通过 owner 的 declaredMemberScope 查找，顶层声明通过包名和安全名称查找。
     */
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

    /**
     * 构造 LSP signature help 中的单个签名信息。
     */
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

    /**
     * 根据调用实参范围计算当前激活参数下标。
     */
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

    /**
     * 将 completion variant 转换为 LSP completion item。
     */
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

    /**
     * 将 PSI 声明构造成文档符号节点。
     */
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

    /**
     * 收集指定文件中的工作区符号。
     */
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

    /**
     * 将仓颉 PSI 声明分类为 LSP 符号类型。
     */
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

    /**
     * 将 LSP 符号类型映射为补全项类型。
     */
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

    /**
     * 将仓颉折叠类型映射为 LSP 折叠范围类型。
     */
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

    /**
     * 将语义 token 修饰符集合编码为 LSP bitset。
     */
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

    /**
     * 将格式化请求参数转换为仓颉格式化器使用的代码风格设置。
     */
    private fun DocumentFormattingParams.toCodeStyleSettings(): CodeStyleSettings {
        val settings = CangJieCodeStyleSettingsFactory.createDefaultSettings()
        val indentOptions = settings.cangjieCommonSettings.initIndentOptions()
        indentOptions.INDENT_SIZE = options.tabSize
        indentOptions.CONTINUATION_INDENT_SIZE = options.tabSize
        indentOptions.TAB_SIZE = options.tabSize
        indentOptions.USE_TAB_CHARACTER = !options.isInsertSpaces
        return settings
    }

    /**
     * 提取声明符号对应的 CDoc 文档字符串。
     */
    @OptIn(CjNonPublicApi::class, CaNonPublicApi::class)
    private fun CaSession.documentationOf(symbol: CaDeclarationSymbol): String? {
        val declaration = symbol.psiSafe<CjDeclaration>() ?: return null
        return declaration.findCDoc()?.renderToDocumentationString()
    }

    /**
     * 将 CDoc 描述渲染为 LSP hover 可使用的 Markdown 文本。
     */
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

    /**
     * 收集可以渲染到 hover 文档中的 CDoc 标签。
     */
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

    /**
     * 将单个 CDoc 标签渲染为一行文档文本。
     */
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

/**
 * 补全路径的日志记录器。
 *
 * 用于记录「不产出候选」的原因（服务未注册、位置不允许补全）：
 * 这些情况在协议上都是空列表，不写日志就只剩「用户没看到候选」这一条线索。
 */
private val LOG: java.util.logging.Logger =
    java.util.logging.Logger.getLogger("org.cangnova.cangjie.lsp.completion")

/**
 * dummy 修正所在的写命令名。
 *
 * 固定的命令名让 `UndoManager` 里这条修正可与插入区分——虽然它发生在**副本**上、
 * 不进用户文档的撤销栈，但排查时能一眼认出。
 */
private const val COMMAND_NAME: String = "cangjie.completion.dummy-correction"
