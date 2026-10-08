package org.cangnova.cangjie.lsp.completion

import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisRequestContext
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.server.LspRequestCancellation
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `$/cancelRequest` 的服务端侧：取消必须**到达管线**，并以 `RequestCancelled` 收场。
 *
 * 为什么需要专门的机制：客户端取消后 LSP4J 对请求 future 调 `cancel(true)`
 * （`RemoteEndpoint` 收到取消通知即如此），但语义工作跑在 `CangjieRequestExecutor`
 * 自己的线程上——`supplyAsync` 的取消**不中断**正在跑的 supplier。
 * 因此取消状态由 [CancellableRequest] 显式传进请求上下文，管线里的
 * `ProgressManager.checkCanceled()` 据此抛出，再映射成 `RequestCancelled`。
 *
 * 用例都**确定性**构造取消时机，不靠 sleep 抢窗口：
 * - 已取消的 token：整条管线从一开始就应当收场；
 * - 「第 N 次检查后取消」的 token：验证取消在**中途**被观察到（管线本来就反复检查取消）。
 */
class LspCompletionCancellationTest : AbstractLspIntegrationTest() {
    /**
     * 本类自建连接与工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 容器事实（**记录当前行为，不是期望行为**）：推入的已取消指示器不会被 `checkCanceled()` 读到。
     *
     * 实测：`ProgressManager.runProcess(..., 已取消的指示器)` 里调 `checkCanceled()` 不抛，
     * 因为容器的 `ProgressIndicatorProvider` 未接线（全局指示器恒为 `null`）。
     * 后果是「靠进度指示器在管线中途收场」这条路在 LSP 容器里**当前不通**，
     * 取消因此由 `LspRequestCancellation` 的**闸门**承担（管线前后各查一次）。
     *
     * 本用例断言的是「现在不抛」：容器哪天接上全局指示器，这条会失败——
     * 那时应当把这段说明改成「已生效」，并去掉闸门里多余的顾虑，而不是把测试改回去。
     */
    @Test
    fun containerDoesNotYetHonourPushedIndicatorCancellation() {
        withReadyServer(USE_SOURCE) { _, _, _ ->
            val delegate = com.intellij.openapi.progress.EmptyProgressIndicator()
            val cancelledIndicator = object : com.intellij.openapi.progress.ProgressIndicator by delegate {
                override fun isCanceled(): Boolean = true
            }

            val thrown = runCatching {
                com.intellij.openapi.progress.ProgressManager.getInstance().runProcess(
                    com.intellij.openapi.util.Computable {
                        com.intellij.openapi.progress.ProgressManager.checkCanceled()
                        "未取消"
                    },
                    cancelledIndicator,
                )
            }.exceptionOrNull()

            assertNull(
                thrown,
                "当前容器不读推入的指示器；这条断言锁的是**现状**。若它开始抛 ProcessCanceledException，" +
                    "说明容器已接上全局指示器，请把本用例的说明改写为「已生效」并同步 implementation-log",
            )
        }
    }

    /**
     * 请求开始前就已取消 → 立刻以 `RequestCancelled` 收场。
     */
    @Test
    fun alreadyCancelledRequestFailsWithRequestCancelled() {
        withReadyServer(USE_SOURCE) { serverContext, document, params ->
            val failure = runCatching {
                serverContext.analysisFacade.completion(
                    serverContext.requestContext(LspRequestCancellation { true }),
                    document,
                    params,
                )
            }.exceptionOrNull()

            assertRequestCancelled(failure)
        }
    }

    /**
     * 请求期间到达的取消 → 结果被**丢弃**（不是返回给用户）。
     *
     * 确定性做法：闸门在管线前后各查一次，因此「第一次说没取消、第二次说已取消」的 token
     * 正好模拟「取消在管线跑的过程中到达」——不靠 sleep 抢窗口。
     */
    @Test
    fun cancellationArrivingDuringTheRequestDiscardsTheResult() {
        withReadyServer(USE_SOURCE) { serverContext, document, params ->
            var remainingChecks = CHECKS_BEFORE_CANCELLATION
            val cancelAfterFirstCheck = LspRequestCancellation {
                if (remainingChecks-- <= 0) true else false
            }

            val failure = runCatching {
                serverContext.analysisFacade.completion(
                    serverContext.requestContext(cancelAfterFirstCheck),
                    document,
                    params,
                )
            }.exceptionOrNull()

            assertRequestCancelled(failure)
        }
    }

    /**
     * 接线：请求 future 被取消（LSP4J 收到 `$/cancelRequest` 后的动作）会翻转取消状态。
     *
     * 这条锁的是**服务端与执行器之间的联动**：工作跑在执行器自己的线程上，
     * `CompletableFuture.cancel(true)` 不会中断它，全靠这个回调把取消传进管线。
     */
    @Test
    fun cancellingTheRequestFutureFlipsTheCancellationState() {
        withReadyServer(USE_SOURCE) { serverContext, _, _ ->
            val cancellation = org.cangnova.cangjie.lsp.server.CancellableRequest()
            val future = serverContext.requestExecutor.compute {
                // 模拟一次仍在跑的请求：等的不是时间，而是取消到达。
                while (!cancellation.isCancelled()) Thread.onSpinWait()
                "未取消"
            }
            future.whenComplete { _, failure ->
                if (failure is java.util.concurrent.CancellationException) cancellation.cancel()
            }

            future.cancel(true)

            val deadline = System.nanoTime() + CANCELLATION_WIRING_TIMEOUT_NANOS
            while (!cancellation.isCancelled() && System.nanoTime() < deadline) Thread.onSpinWait()
            assertTrue(
                cancellation.isCancelled(),
                "请求 future 取消后，取消状态必须被翻转——否则管线永远不知道用户已经不要结果了",
            )
        }
    }

    /**
     * 断言失败是「已取消」而不是别的错误。
     *
     * 比较的是**码值**：lsp4j 的 `ResponseError.code` 是 `int`（构造函数收枚举，`getCode()`
     * 返回数值），拿枚举去比永远不会相等。
     */
    private fun assertRequestCancelled(failure: Throwable?) {
        val error = (failure as? ResponseErrorException)?.responseError
        assertEquals(
            ResponseErrorCode.RequestCancelled.value,
            error?.code,
            "取消必须以 RequestCancelled(-32800) 收场，实际 ${failure?.let { it::class.simpleName + ": " + it.message }}",
        )
        assertEquals("补全已取消", error?.message, "取消消息要能直接给用户看")
    }

    /**
     * 在真实容器里跑一次「带取消状态」的门面补全。
     */
    private fun withReadyServer(
        source: String,
        body: (
            serverContext: org.cangnova.cangjie.lsp.server.CangjieServerContext,
            document: LspTextDocument,
            params: CompletionParams,
        ) -> Unit,
    ) {
        val fixture = LspWorkspaceFixtureBuilder().source(USE_NAME, source).build()
        LspIntegrationTestConnection.create(defaultServerOptions()).use { connection ->
            connection.initialize(fixture.initializeParams())
            connection.initialized()
            val serverContext = connection.server.context
            val uri = fixture.uri("src/$USE_NAME")
            val text = fixture.text("src/$USE_NAME")
            connection.openDocument(uri, text)
            // 打开文档的副作用（PSI 快照与 use-site 模块绑定）要靠一次协议请求推到完成。
            connection.documentSymbol(connection.documentSymbolParams(uri))

            val document = LspTextDocument(uri = uri, languageId = "cangjie", version = 1, text = text)
            val params = CompletionParams(
                TextDocumentIdentifier(uri),
                positionAt(text, text.indexOf("let u =") + "let u =".length),
            )
            assertPositionYieldsCandidates(serverContext, document, params)

            body(serverContext, document, params)
        }
    }

    /**
     * 前置断言：该位置**本来有候选**。
     *
     * 取消用例若在「本来就没有候选」的位置上跑，断言「以取消收场」毫无意义——
     * 那可能只是因为管线根本没走到与取消有关的那一步。
     */
    private fun assertPositionYieldsCandidates(
        serverContext: org.cangnova.cangjie.lsp.server.CangjieServerContext,
        document: LspTextDocument,
        params: CompletionParams,
    ) {
        val items = serverContext.analysisFacade
            .completion(serverContext.requestContext(), document, params)
            .left
        assertTrue(
            items.isNotEmpty(),
            "前置条件不成立：位置 ${params.position} 本来就没有候选，取消断言将毫无意义",
        )
    }

    /**
     * 偏移 → LSP 位置（夹具源码只用 `\n`）。
     */
    private fun positionAt(text: String, offset: Int): Position {
        val prefix = text.substring(0, offset)
        return Position(prefix.count { it == '\n' }, offset - (prefix.lastIndexOf('\n') + 1))
    }

    private companion object {
        const val USE_NAME = "use.cj"

        /**
         * 几次取消检查之后翻转。
         *
         * 取一个小值即可：管线在一次补全里会做几十次取消检查（每个区段、每条候选路径）。
         */
        const val CHECKS_BEFORE_CANCELLATION = 1

        /** 等取消联动生效的上限（不靠 sleep，只做上界）。 */
        const val CANCELLATION_WIRING_TIMEOUT_NANOS = 5_000_000_000L

        /** 顶层函数可供补全，保证管线会真的跑起来（而不是在空候选处提前返回）。 */
        val USE_SOURCE = """
            package sample.cancel

            func buildUser(): Int64 {
                1
            }

            func consume(): Int64 {
                let u =
                return 1
            }
        """.trimIndent()
    }
}
