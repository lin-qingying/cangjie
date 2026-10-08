package org.cangnova.cangjie.lsp.completion

import com.google.gson.JsonPrimitive
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.lsp.state.LspDocumentStore
import org.eclipse.lsp4j.ApplyWorkspaceEditParams
import org.eclipse.lsp4j.WorkspaceEdit
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import org.eclipse.lsp4j.services.LanguageClient
import java.util.concurrent.CompletableFuture

/**
 * 「接受候选并插入」的服务端实现：`workspace/executeCommand` 的 `applyCompletion` 分支。
 *
 * 服务端**只做两件事**：算出编辑、把编辑经 `workspace/applyEdit` 发给客户端。
 * 文本由客户端落盘——服务端不写文档，因此这里不涉及 EDT 写事务，也不做撤销分组
 * （撤销由客户端按一次编辑处理）。
 *
 * 每条失败路径都返回**明确的错误**（而不是静默返回成功或抛未捕获异常）：
 * 会话过期、候选下标越界、文档已关闭、文档版本已变、编辑区间越界。
 * 共同的理由是——这些情况都意味着「按旧会话算出的区间可能已经不对」，
 * 此时写下去会改坏用户代码；宁可让客户端提示用户重新触发补全。
 */
class LspCompletionCommandHandler(
    /**
     * 补全会话存储。
     */
    private val sessions: LspCompletionSessionStore,
    /**
     * 文档存储：应用时必须拿到**当前文本**，而不是会话创建时的文本。
     */
    private val documentStore: LspDocumentStore,
    /**
     * 当前工程，用于导入编辑的 PSI 计算。
     */
    private val project: Project,
    /**
     * 取当前连接的客户端；服务端在 `initialize` 时才拿到它。
     */
    private val clientProvider: () -> LanguageClient?,
    /**
     * 客户端是否声明了 `workspace/applyEdit`。
     *
     * **LSP 的服务端能力里没有 applyEdit 字段**（核对 lsp4j 1.0.0：`WorkspaceServerCapabilities`
     * 只有 workspaceFolders/fileOperations/textDocumentContent），因此这件事只能从
     * **客户端能力**得知，服务端不额外声明什么。客户端没声明时不能发该请求——
     * 发了没人处理，表现是「接受候选后没有反应」，不如直接报错。
     */
    private val supportsApplyEdit: () -> Boolean,
) {
    /**
     * 应用一个候选。
     *
     * @param arguments 命令实参，按约定是 `[会话键, 候选下标]`。
     */
    fun applyCompletion(arguments: List<Any>): CompletableFuture<Any> {
        val sessionKey = sessionKeyOf(arguments.getOrNull(0))
            ?: return failed("applyCompletion 缺少会话键：实参 ${arguments.map { describe(it) }}")
        val index = indexOf(arguments.getOrNull(LspCompletionCommands.ARG_INDEX_OFFSET))
            ?: return failed("applyCompletion 缺少候选下标：实参 ${arguments.map { describe(it) }}")

        val session = sessions.find(sessionKey)
            ?: return failed("补全会话已过期，请重新触发补全（key=$sessionKey）")
        val item = session.itemAt(index)
            ?: return failed("补全会话里没有第 $index 个候选，请重新触发补全")

        val document = documentStore.get(session.uri)
            ?: return failed("文档已关闭，无法插入候选（${session.uri}）")
        if (document.version != session.version) {
            return failed(
                "文档已变（会话版本 ${session.version}，当前 ${document.version}），请重新触发补全",
            )
        }

        val edits = try {
            editsFor(session, item, document.text)
        } catch (failure: Exception) {
            // 区间越界/导入冲突等都在这里变成明确失败：绝不带着可疑区间去写文档。
            return failed("计算插入编辑失败：${failure.message}")
        }
        val client = clientProvider() ?: return failed("客户端未连接，无法应用编辑")
        if (!supportsApplyEdit()) {
            return failed("客户端未声明 workspace/applyEdit，无法插入候选——补全插入靠客户端应用编辑")
        }
        return client.applyEdit(
            ApplyWorkspaceEditParams(WorkspaceEdit().apply { changes = mapOf(session.uri to edits) }, LABEL),
        ).thenApply { response -> response as Any }
    }

    /**
     * 算出这次插入的全部编辑，坐标全部相对**当前文档文本**。
     *
     * 顺序按起点升序：LSP 只要求编辑互不重叠，但固定顺序能让日志与用例断言稳定。
     */
    private fun editsFor(
        session: LspCompletionSessionStore.Session,
        item: CompletionSnapshot,
        text: String,
    ): List<org.eclipse.lsp4j.TextEdit> = buildList {
        addAll(
            LspImportEditCalculator.importEdits(
                project = project,
                documentUri = session.uri,
                fileName = fileNameOf(session.uri),
                text = text,
                snapshot = item,
            ),
        )
        addAll(
            LspCompletionEditCalculator.candidateEdits(
                text = text,
                replaceStart = session.replaceStart,
                replaceEnd = session.replaceEnd,
                snapshot = item,
            ),
        )
    }.sortedWith(compareBy({ it.range.start.line }, { it.range.start.character }))

    /**
     * 从 URI 取文件名；取不到时给一个稳定占位名（导入计算只把它当副本名用）。
     */
    private fun fileNameOf(uri: String): String = uri.substringAfterLast('/').substringBefore('?').ifEmpty { "document.cj" }

    /**
     * 明确的失败结果。
     *
     * 用 [ResponseErrorException] 而不是普通异常：后者经 LSP4J 只会在客户端侧得到
     * 一句通用的 `Internal error.`（实测），失败原因留在服务端日志里——用户看到的就是
     * 「接受候选后弹一个没头没尾的错误」。这里的消息因此必须是**给用户看的**那一句。
     */
    private fun failed(message: String): CompletableFuture<Any> =
        CompletableFuture.failedFuture(
            ResponseErrorException(ResponseError(ResponseErrorCode.InternalError, message, null)),
        )

    /**
     * 取会话键。
     *
     * 实参经 JSON-RPC 往返后是 **`JsonPrimitive`**（实测：直接强转 `String` 会失败），
     * 但也接受本进程内直接构造的 `String`，便于单元级调用。
     */
    private fun sessionKeyOf(argument: Any?): String? = when (argument) {
        is JsonPrimitive -> argument.takeIf { it.isString }?.asString
        is String -> argument
        else -> null
    }

    /**
     * 取候选下标；同样兼容 `JsonPrimitive` 与进程内直接构造的数字。
     */
    private fun indexOf(argument: Any?): Int? = when (argument) {
        is JsonPrimitive -> argument.takeIf { it.isNumber }?.asInt
        is Number -> argument.toInt()
        else -> null
    }

    /**
     * 实参的日志描述：只记类型与是否可解析，不把内容整段打进日志。
     */
    private fun describe(argument: Any?): String = when (argument) {
        null -> "null"
        is JsonPrimitive -> "JsonPrimitive(${if (argument.isString) "string" else if (argument.isNumber) "number" else "other"})"
        else -> argument::class.simpleName ?: "unknown"
    }

    private companion object {
        /** `applyEdit` 请求的标签，出现在客户端的撤销栈里。 */
        const val LABEL = "Cangjie completion"
    }
}
