package org.cangnova.cangjie.lsp.analysis

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.intellij.openapi.command.CommandProcessor
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CaCfirDiagnostic
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.codeinsight.api.applicators.fixes.CangJieQuickFixService
import org.cangnova.cangjie.codeinsight.api.members.memberGenerationRegistry
import org.cangnova.cangjie.formatter.CangJieFormatter
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjTypeStatement
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionKind
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.WorkspaceEdit
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.awt.EventQueue
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Logger
import kotlin.reflect.KClass

/**
 * LSP codeAction 协议适配层。
 *
 * 列表与应用分开两条链：
 * 1. **列表**（`textDocument/codeAction`）：在分析会话中收集诊断，过滤出与请求范围重叠、
 *    且 LSP 侧存在 headless 应用路径的诊断，标题与存在性经 quick-fix 框架
 *    （[CangJieQuickFixService]）给出——与 IDE 共用同一份「诊断 → factory」注册表；
 * 2. **应用**（`codeAction/resolve`）：按 data 里的诊断标识在最新快照上重新定位诊断，
 *    经 [org.cangnova.cangjie.codeinsight.api.members.CangJieMemberGenerationService]
 *    headless 插入 + 共享 formatter 原地重排，diff 出 [TextEdit]。
 *
 * 列表阶段**绝不**修改 PSI；应用阶段只作用于本次请求新建的快照，原文件经由客户端
 * 应用编辑并 `didChange` 回流保持一致。
 */
internal class AnalysisApiLspCodeActionSupport(
    /**
     * 提供快照构造、诊断坐标转换等语义支撑。
     */
    private val semanticSupport: AnalysisApiLspSemanticSupport,
) {
    /**
     * 当前 LSP 侧已具备 headless 应用路径的诊断类别。
     *
     * quick-fix 框架里新注册的 factory 在这里没有对应应用路径时，不会出现在 LSP 列表里——
     * 宁可少列，也不广播一个点了之后什么都不做的动作。
     */
    private val applicableDiagnosticClasses: Set<KClass<out CaDiagnosticWithPsi<*>>> = setOf(
        CaCfirDiagnostic.AbstractMemberNotImplemented::class,
        CaCfirDiagnostic.InterfaceMemberMustBeImplemented::class,
    )

    /**
     * 列出与请求范围重叠且可应用的 quick fix。
     */
    fun list(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        params: CodeActionParams,
    ): List<Either<Command, CodeAction>> {
        return semanticSupport.analyzeSnapshot(document) { snapshot ->
            val diagnostics = snapshot.psiFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
            val quickFixService = runCatching { CangJieQuickFixService.getInstance() }.getOrNull()
            if (quickFixService == null) {
                LOG.warning("quick-fix 服务未装配，codeAction 列表保持为空")
                return@analyzeSnapshot emptyList()
            }

            val actions = mutableListOf<Pair<String, CodeAction>>()
            for (diagnostic in diagnostics) {
                if (diagnostic.diagnosticClass !in applicableDiagnosticClasses) continue

                val lspRanges = with(AnalysisApiDiagnostics) { diagnostic.lspRanges(document) }
                val primaryRange = lspRanges.firstOrNull { overlaps(it, params.range) } ?: continue

                /*
                 * 单个 factory 失败不拖垮整条列表：与框架 K2 语义一致，失败项丢弃，
                 * `ProcessCanceledException` 仍向上传播由请求层处理。
                 */
                val fixes = with(quickFixService) {
                    getAvailableQuickFixesWithCatchingFor(diagnostic)
                        .mapNotNull { result -> result.getOrNull() }
                        .toList()
                }
                if (fixes.isEmpty()) continue

                val lspDiagnostic = AnalysisApiDiagnostics.toLspDiagnostics(
                    document = document,
                    source = context.descriptor.diagnosticIdentifier,
                    diagnostics = listOf(diagnostic),
                    showDiagnosticFactoryNames =
                        context.workspaceState.projectConfiguration().showDiagnosticFactoryNames,
                ).firstOrNull()

                fixes.forEach { intention ->
                    val actionData = LspCodeActionData(
                        documentUri = document.uri,
                        diagnosticFactoryName = diagnostic.factoryName,
                        startLine = primaryRange.start.line,
                        startCharacter = primaryRange.start.character,
                        endLine = primaryRange.end.line,
                        endCharacter = primaryRange.end.character,
                    )
                    // 显式接收者构造，避免简单名 `diagnostics` 被 CaSession 的同名属性遮蔽。
                    val action = CodeAction()
                    action.title = intention.text
                    action.kind = CodeActionKind.QuickFix
                    if (lspDiagnostic != null) action.setDiagnostics(listOf(lspDiagnostic))
                    action.data = actionData
                    actions += intention.javaClass.name to action
                }
            }
            LspCodeActionDeduplicator.deduplicate(actions)
        }
    }

    /**
     * 解析 code action 并补齐编辑（LSP `codeAction/resolve`）。
     *
     * 诊断定位失败、文档已变化或应用后没有产生差异时，返回原样——
     * 与 `completionItem/resolve` 的契约一致：resolve 失败不该让动作消失。
     */
    fun resolve(
        context: CangjieAnalysisRequestContext,
        document: LspTextDocument,
        action: CodeAction,
    ): CodeAction {
        val data = LspCodeActionData.parse(action.data)
        if (data == null || data.documentUri != document.uri) {
            LOG.info("codeAction/resolve 跳过：data 解析失败或 URI 不匹配（${action.data}）")
            return action
        }

        val edits = semanticSupport.analyzeSnapshot(document) { snapshot ->
            val diagnostic = locateDiagnostic(document, snapshot.psiFile, data)
            if (diagnostic == null) {
                LOG.info("codeAction/resolve 跳过：按 factory=${data.diagnosticFactoryName} 未重新定位到诊断")
                return@analyzeSnapshot null
            }
            val typeStatement = generateSequence(diagnostic.psi) { element -> element.parent }
                .firstOrNull { it is CjTypeStatement } as? CjTypeStatement
            if (typeStatement == null) {
                LOG.info("codeAction/resolve 跳过：诊断 PSI 的祖先中没有类型声明（${diagnostic.psi?.javaClass?.name}）")
                return@analyzeSnapshot null
            }

            val generationService = memberGenerationRegistry(typeStatement.project).service
            if (generationService == null) {
                LOG.info("codeAction/resolve 跳过：成员生成服务未注册")
                return@analyzeSnapshot null
            }
            val members = generationService.collectUnimplementedMembers(typeStatement)
            if (members.isEmpty()) {
                LOG.info("codeAction/resolve 跳过：未收集到待实现成员")
                return@analyzeSnapshot null
            }

            val oldText = document.analysisText
            runInEdtWriteAction {
                generationService.generateMembers(typeStatement, members)
            }
            CangJieFormatter.reformatInPlace(snapshot.psiFile)

            LspTextEditsComputer.computeTextEdits(
                document = document,
                oldText = oldText,
                newText = snapshot.psiFile.text,
            )
        } ?: return action

        if (edits.isEmpty()) return action

        action.edit = WorkspaceEdit().apply {
            changes = mapOf(document.uri to edits)
        }
        return action
    }

    /**
     * 在最新快照的诊断集合中按 factory 名与记录范围重新定位诊断。
     *
     * 文档在列表与 resolve 之间可能被编辑过，这里用「factory 名一致 + 主范围仍然重叠」
     * 做容错匹配，不做逐字节比对。
     */
    private fun CaSession.locateDiagnostic(
        document: LspTextDocument,
        file: CjFile,
        data: LspCodeActionData,
    ): CaDiagnosticWithPsi<*>? {
        val recordedRange = Range(
            Position(data.startLine, data.startCharacter),
            Position(data.endLine, data.endCharacter),
        )
        return file.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
            .filter { it.factoryName == data.diagnosticFactoryName }
            .firstOrNull { diagnostic ->
                val ranges = with(AnalysisApiDiagnostics) { diagnostic.lspRanges(document) }
                ranges.any { overlaps(it, recordedRange) }
            }
    }

    /**
     * 判断两个 LSP 范围是否相交。
     */
    private fun overlaps(a: Range, b: Range): Boolean {
        return positionAtOrBefore(a.start, b.end) && positionAtOrBefore(b.start, a.end)
    }

    /**
     * 按 LSP 位置语义比较先后：行优先，行内比较字符。
     */
    private fun positionAtOrBefore(a: Position, b: Position): Boolean {
        return a.line < b.line || (a.line == b.line && a.character <= b.character)
    }

    /**
     * Kotlin LSP 在 EDT + write-intent/read action 中执行 headless PSI 写入。
     *
     * 与 rename 适配层同一约定：LSP 协议适配层负责切线程，共享模块不承担宿主调度。
     * PSI 修改必须处于 command 或 undo-transparent action 之内（`PomModelImpl` 的事务断言），
     * 与共享 formatter 的 headless 写路径一致，这里包一层 undo-transparent action——
     * LSP 编辑的撤销栈由客户端持有，服务端不产生 undo 条目，正好契合该语义。
     */
    private fun <T> runInEdtWriteAction(action: () -> T): T {
        fun runWriteAction(): T =
            com.intellij.openapi.application.ApplicationManager.getApplication().runWriteAction<T> { action() }

        fun runUndoTransparentWriteAction(): T {
            val result = AtomicReference<T>()
            CommandProcessor.getInstance().runUndoTransparentAction { result.set(runWriteAction()) }
            return result.get()
        }

        if (EventQueue.isDispatchThread()) return runUndoTransparentWriteAction()

        val result = AtomicReference<T>()
        val failure = AtomicReference<Throwable>()
        EventQueue.invokeAndWait {
            try {
                result.set(runUndoTransparentWriteAction())
            } catch (throwable: Throwable) {
                failure.set(throwable)
            }
        }

        failure.get()?.let { throw it }
        return result.get()
    }

    /**
     * codeAction/resolve 的 data 载荷：定位文档与诊断的最小信息集。
     */
    internal data class LspCodeActionData(
        val documentUri: String,
        val diagnosticFactoryName: String,
        val startLine: Int,
        val startCharacter: Int,
        val endLine: Int,
        val endCharacter: Int,
    ) {
        companion object {
            /** data 字段键名；resolve 时客户端原样回传，键必须稳定。 */
            private const val KEY_DOCUMENT_URI = "documentUri"
            private const val KEY_FACTORY_NAME = "diagnosticFactoryName"
            private const val KEY_START_LINE = "startLine"
            private const val KEY_START_CHARACTER = "startCharacter"
            private const val KEY_END_LINE = "endLine"
            private const val KEY_END_CHARACTER = "endCharacter"

            /**
             * 从客户端回传的 `data` 值解析载荷。
             *
             * lsp4j 对 `Object` 类型的 data 字段保留 Gson 元素树（`JsonObject`），
             * 与 `completionItem/resolve` 的既有解析一致；同时兼容调用方直接传
             * `Map` 的场景（数字经 Gson 往返可能是 `Double`，数字字符串也一并兼容）。
             */
            fun parse(value: Any?): LspCodeActionData? = when (value) {
                is JsonObject -> parseJsonObject(value)
                is Map<*, *> -> parseMap(value)
                else -> null
            }

            private fun parseJsonObject(obj: JsonObject): LspCodeActionData? {
                val documentUri = obj.stringField(KEY_DOCUMENT_URI) ?: return null
                val factoryName = obj.stringField(KEY_FACTORY_NAME) ?: return null
                val startLine = obj.intField(KEY_START_LINE) ?: return null
                val startCharacter = obj.intField(KEY_START_CHARACTER) ?: return null
                val endLine = obj.intField(KEY_END_LINE) ?: return null
                val endCharacter = obj.intField(KEY_END_CHARACTER) ?: return null
                return LspCodeActionData(documentUri, factoryName, startLine, startCharacter, endLine, endCharacter)
            }

            private fun parseMap(map: Map<*, *>): LspCodeActionData? {
                val documentUri = map[KEY_DOCUMENT_URI] as? String ?: return null
                val factoryName = map[KEY_FACTORY_NAME] as? String ?: return null
                val startLine = parseInt(map[KEY_START_LINE]) ?: return null
                val startCharacter = parseInt(map[KEY_START_CHARACTER]) ?: return null
                val endLine = parseInt(map[KEY_END_LINE]) ?: return null
                val endCharacter = parseInt(map[KEY_END_CHARACTER]) ?: return null
                return LspCodeActionData(documentUri, factoryName, startLine, startCharacter, endLine, endCharacter)
            }

            private fun JsonObject.stringField(key: String): String? =
                get(key)?.takeIf(JsonElement::isJsonPrimitive)?.runCatching { asString }?.getOrNull()

            private fun JsonObject.intField(key: String): Int? =
                get(key)?.takeIf(JsonElement::isJsonPrimitive)?.runCatching { asInt }?.getOrNull()

            /**
             * 兼容 Number（含 Double）与数字字符串的整数解析。
             */
            private fun parseInt(value: Any?): Int? = when (value) {
                is Number -> value.toInt()
                is String -> value.toIntOrNull()
                else -> null
            }
        }
    }

    /**
     * 适配层日志。
     */
    private companion object {
        private val LOG: Logger = Logger.getLogger(AnalysisApiLspCodeActionSupport::class.java.name)
    }
}

/**
 * 合并同一诊断位置、同一 intention 产生的重复 LSP quick-fix，并保留其关联诊断。
 *
 * 按成员逐条上报的诊断可能共用一个类型 PSI；成员实现 factory 为每条诊断收集到的是同一组待实现成员，
 * 因此协议层应只返回一个可解析动作。
 */
internal object LspCodeActionDeduplicator {
    private data class Key(
        val intentionClassName: String,
        val title: String?,
        val kind: String?,
        val data: Any?,
    )

    /** 保序去重，并把重复动作关联的诊断并入首个动作。 */
    fun deduplicate(candidates: List<Pair<String, CodeAction>>): List<Either<Command, CodeAction>> {
        val actionsByKey = linkedMapOf<Key, CodeAction>()
        for ((intentionClassName, action) in candidates) {
            val key = Key(intentionClassName, action.title, action.kind, action.data)
            val existing = actionsByKey[key]
            if (existing == null) {
                actionsByKey[key] = action
                continue
            }

            val relatedDiagnostics = (existing.diagnostics.orEmpty() + action.diagnostics.orEmpty()).distinct()
            if (relatedDiagnostics.isNotEmpty()) existing.setDiagnostics(relatedDiagnostics)
        }
        return actionsByKey.values.map { action -> Either.forRight(action) }
    }
}
