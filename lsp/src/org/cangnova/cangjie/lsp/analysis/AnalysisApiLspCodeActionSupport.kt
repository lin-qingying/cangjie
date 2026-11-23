package org.cangnova.cangjie.lsp.analysis

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModCommandAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.codeinsight.api.applicators.fixes.CangJieQuickFixService
import org.cangnova.cangjie.formatter.CangJieCodeStyleSettingsFactory
import org.cangnova.cangjie.lsp.quickfix.LspQuickFixCommands
import org.cangnova.cangjie.lsp.quickfix.ModCommandData
import org.cangnova.cangjie.lsp.quickfix.combinedPresentationNames
import org.cangnova.cangjie.lsp.quickfix.flattenChoiceActions
import org.cangnova.cangjie.lsp.quickfix.resolveLspDefaultOptions
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionKind
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.awt.EventQueue
import java.lang.reflect.InvocationTargetException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * 诊断 quick fix 的 LSP host provider。
 *
 * 共享 quick-fix service 决定诊断对应的动作；本层把 ModCommand action 转成 LSP command，
 * 不重写修复，也不在客户端执行时重新选择另一个动作。
 */
internal class AnalysisApiLspCodeActionSupport(
    private val semanticSupport: AnalysisApiLspSemanticSupport,
) {
    fun list(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: CodeActionParams,
    ): List<Either<Command, CodeAction>> {
        val onlyKinds = params.context.only
        if (onlyKinds != null && CodeActionKind.QuickFix !in onlyKinds) return emptyList()
        val requestedDiagnostics = params.context.diagnostics.orEmpty().mapNotNull { lspDiagnostic ->
            CangjieLspDiagnosticData.parse(lspDiagnostic.data)?.let { data -> data to lspDiagnostic }
        }
        if (params.context.diagnostics.orEmpty().isNotEmpty() && requestedDiagnostics.isEmpty()) return emptyList()
        val workspaceCapabilities = context.workspaceState.initializeParams()?.capabilities?.workspace

        val prepared = semanticSupport.analyzeSnapshot(document) { snapshot ->
            val quickFixService = try {
                CangJieQuickFixService.getInstance()
            } catch (cancelled: ProcessCanceledException) {
                throw cancelled
            } catch (failure: Exception) {
                LOG.log(Level.WARNING, "Could not obtain the Cangjie quick-fix service", failure)
                return@analyzeSnapshot emptyList()
            }
            val preparedActions = mutableListOf<PreparedAction>()
            val diagnostics = snapshot.psiFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
            // 空 context.diagnostics 是合法的主动请求：从同一当前快照生成正式诊断，不补造错误。
            val diagnosticTargets = if (params.context.diagnostics.orEmpty().isEmpty()) {
                diagnostics.flatMap { diagnostic ->
                    AnalysisApiDiagnostics.toLspDiagnostics(
                        document,
                        context.descriptor.diagnosticIdentifier,
                        listOf(diagnostic),
                        context.workspaceState.projectConfiguration().showDiagnosticFactoryNames,
                    ).map { lspDiagnostic -> diagnostic to lspDiagnostic }
                }
            } else {
                requestedDiagnostics.mapNotNull { (data, lspDiagnostic) ->
                    diagnostics.firstOrNull { candidate -> data.matches(document, candidate) }
                        ?.let { diagnostic -> diagnostic to lspDiagnostic }
                }
            }
            for ((diagnostic, lspDiagnostic) in diagnosticTargets) {
                val primaryRange = with(AnalysisApiDiagnostics) { diagnostic.lspRanges(document) }
                    .firstOrNull { it == lspDiagnostic.range && overlaps(it, params.range) }
                    ?: continue
                val offset = document.analysisOffsetAt(primaryRange.start)
                val actionContext = ActionContext(
                    snapshot.psiFile.project,
                    snapshot.psiFile,
                    offset,
                    TextRange(offset, offset),
                    diagnostic.psi,
                )
                val intentions = with(quickFixService) {
                    getAvailableQuickFixesWithCatchingFor(diagnostic)
                        .mapNotNull { result ->
                            result.fold(
                                onSuccess = { it },
                                onFailure = { failure ->
                                    if (failure is ProcessCanceledException) throw failure
                                    LOG.log(Level.WARNING, "Could not create quick fixes", failure)
                                    null
                                },
                            )
                        }
                        .toList()
                }
                for (intention in intentions) {
                    val modCommandAction = intention.asModCommandAction()
                    if (modCommandAction == null) {
                        LOG.fine("Skipping IntentionAction without a ModCommandAction: " + intention.javaClass.name)
                        continue
                    }

                    preparedActions += PreparedAction(modCommandAction, actionContext, lspDiagnostic, snapshot.psiFile)
                }
            }
            preparedActions
        }
        // 分析读访问已经退出。平台 ModCommand 仅在真实 EDT 写访问中更新自己的 PSI 副本，
        // 不能持有分析 ReadAction 再同步请求 EDT 写动作。
        if (prepared.isEmpty()) return emptyList()
        return onEdtWrite {
            // ModCommand 的延期 postprocess 也必须使用同一仓颉代码风格；设置作用域覆盖整个副本编辑。
            CodeStyleSettingsManager.getInstance(prepared.first().file.project).computeWithLocalSettings(
                CangJieCodeStyleSettingsFactory.createDefaultSettings(),
            ) {
                val workspaceEditCapabilities = workspaceCapabilities?.workspaceEdit
                val actions = mutableListOf<Pair<String, CodeAction>>()
                for (candidate in prepared) {
                    for (chain in candidate.action.flattenChoiceActions(candidate.context)) {
                        val command = chain.leaf.command.resolveLspDefaultOptions() ?: continue
                        val commandData = ModCommandData.from(command, document, candidate.file, context.documentStore) ?: continue
                        if (!commandData.hasEffect() || !commandData.isApplicableTo(context.documentStore)) continue
                        if (!commandData.isLspExecutable()) continue
                        if (commandData.requiresApplyEdit() && workspaceCapabilities?.applyEdit != true) continue
                        if (commandData.requiresDocumentChanges() && workspaceEditCapabilities?.documentChanges != true) continue
                        if (commandData.requiredResourceOperations().any {
                                it !in workspaceEditCapabilities?.resourceOperations.orEmpty()
                            }) continue
                        val codeAction = CodeAction()
                        codeAction.title = chain.combinedPresentationNames()
                        codeAction.kind = CodeActionKind.QuickFix
                        codeAction.setDiagnostics(listOf(candidate.diagnostic))
                        codeAction.command = Command(chain.combinedPresentationNames(), LspQuickFixCommands.APPLY_MOD_COMMAND,
                            listOf(commandData.toJson()))
                        actions += candidate.action.javaClass.name to codeAction
                    }
                }
                LspCodeActionDeduplicator.deduplicate(actions)
            }
        }
    }

    /** 这里只保留已渲染动作和协议诊断，不能把 Analysis session/symbol 带入写阶段。 */
    private data class PreparedAction(
        val action: ModCommandAction,
        val context: ActionContext,
        val diagnostic: Diagnostic,
        val file: PsiFile,
    )

    /** 使用实际 AWT EDT；MockApplication.invokeAndWait 不能替代 Document 的 EDT 合同。 */
    private fun <T : Any> onEdtWrite(action: () -> T): T {
        var result: T? = null
        val runnable = Runnable {
            CommandProcessor.getInstance().runUndoTransparentAction {
                result = WriteAction.compute<T, RuntimeException> { action() }
            }
        }
        if (EventQueue.isDispatchThread()) {
            runnable.run()
        } else {
            try {
                EventQueue.invokeAndWait(runnable)
            } catch (failure: InvocationTargetException) {
                throw failure.targetException
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                throw interrupted
            }
        }
        return checkNotNull(result)
    }

    /** quick-fix 编辑已随列表阶段生成的 command 传输，resolve 不再二次选择动作。 */
    fun resolve(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        action: CodeAction,
    ): CodeAction = action

    private fun overlaps(a: org.eclipse.lsp4j.Range, b: org.eclipse.lsp4j.Range): Boolean =
        positionAtOrBefore(a.start, b.end) && positionAtOrBefore(b.start, a.end)

    private fun positionAtOrBefore(a: org.eclipse.lsp4j.Position, b: org.eclipse.lsp4j.Position): Boolean =
        a.line < b.line || (a.line == b.line && a.character <= b.character)

    private companion object {
        val LOG: Logger = Logger.getLogger(AnalysisApiLspCodeActionSupport::class.java.name)
    }
}

/** 合并多个诊断重复报告的同一动作，并保留其关联诊断。 */
internal object LspCodeActionDeduplicator {
    private data class Key(
        val actionClassName: String,
        val title: String?,
        val kind: String?,
        val command: String?,
        val arguments: String?,
    )

    fun deduplicate(candidates: List<Pair<String, CodeAction>>): List<Either<Command, CodeAction>> {
        val actionsByKey = linkedMapOf<Key, CodeAction>()
        for ((actionClassName, action) in candidates) {
            val key = Key(
                actionClassName = actionClassName,
                title = action.title,
                kind = action.kind,
                command = action.command?.command,
                arguments = action.command?.arguments?.joinToString(separator = "\u0000") { it.toString() },
            )
            val existing = actionsByKey[key]
            if (existing == null) {
                actionsByKey[key] = action
                continue
            }
            val diagnostics = (existing.diagnostics.orEmpty() + action.diagnostics.orEmpty()).distinct()
            if (diagnostics.isNotEmpty()) existing.setDiagnostics(diagnostics)
        }
        return actionsByKey.values.map { action -> Either.forRight(action) }
    }
}
