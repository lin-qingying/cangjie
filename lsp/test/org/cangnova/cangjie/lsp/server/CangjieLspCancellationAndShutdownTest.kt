package org.cangnova.cangjie.lsp.server

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspClientCapabilitiesBuilder
import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.cangnova.cangjie.lsp.testkit.ProtocolContractAnalysisFacadeFactory
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 请求取消、shutdown/exit 幂等与资源释放。
 *
 * 这里固定的是「失败不得静默」的边界：取消要能被管线看到、重复 exit 不能重复退出、
 * 未初始化时关闭不能反向拉起环境、已初始化时释放必须且只能发生一次。
 */
class CangjieLspCancellationAndShutdownTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：每个用例自行决定初始化时机（未初始化路径必须能单独观测）。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 取消闸门的语义：未取消放行，取消后一律抛 `RequestCancelled`。
     */
    @Test
    fun `cancellable request throws request cancelled only after cancel`() {
        val request = CancellableRequest()
        assertFalse(request.isCancelled())
        request.throwIfCancelled()

        request.cancel()
        request.cancel() // 重复取消必须无副作用：取消通知可能重复到达

        assertTrue(request.isCancelled())
        val failure = assertThrows(ResponseErrorException::class.java) { request.throwIfCancelled() }
        assertEquals(ResponseErrorCode.RequestCancelled.value, failure.responseError.code)
    }

    /**
     * 测试/非请求路径使用 [LspRequestCancellation.NONE]，任何时候都不应抛取消错误。
     */
    @Test
    fun `none cancellation never throws`() {
        LspRequestCancellation.NONE.throwIfCancelled()
        assertFalse(LspRequestCancellation.NONE.isCancelled())
    }

    /**
     * 取消未知请求 id 是无害噪音：会话必须继续服务后续请求。
     */
    @Test
    fun `cancelling an unknown request keeps the session serving`() {
        val factory = ProtocolContractAnalysisFacadeFactory()
        val connection = LspIntegrationTestConnection.create(
            defaultServerOptions().copy(analysisFacadeFactory = factory::create),
        )
        try {
            connection.initialize(
                LspClientCapabilitiesBuilder.initializeParams(capabilities = LspClientCapabilitiesBuilder.fullFeatured()),
            )
            connection.initialized()

            connection.cancelProgress("unknown-token")
            connection.cancelProgress("another-token")

            val uri = "file:///workspace/after-cancel.cj"
            connection.openDocument(uri, "contract body")
            val hover = connection.hover(connection.hoverParams(uri, 0, 0))
            assertTrue(hover.toString().contains("contractHover"), "取消未知 id 不应影响后续请求")
        } finally {
            connection.close()
        }
    }

    /**
     * shutdown 必须把状态写进 workspaceState，且重复调用幂等。
     */
    @Test
    fun `shutdown marks workspace state and is idempotent`() {
        val factory = ProtocolContractAnalysisFacadeFactory()
        val connection = LspIntegrationTestConnection.create(
            defaultServerOptions().copy(analysisFacadeFactory = factory::create),
        )
        try {
            connection.initialize(
                LspClientCapabilitiesBuilder.initializeParams(capabilities = LspClientCapabilitiesBuilder.fullFeatured()),
            )
            connection.initialized()

            val workspaceState = connection.server.context.workspaceState
            assertFalse(workspaceState.isShutdownRequested())

            connection.server.shutdown().get()
            assertTrue(workspaceState.isShutdownRequested(), "shutdown 必须落库，供后续路径判断退出语义")

            connection.server.shutdown().get()
            assertTrue(workspaceState.isShutdownRequested())
        } finally {
            connection.close()
        }
    }

    /**
     * 未初始化就 close/exit：不得触发惰性环境初始化。
     *
     * 这条**不能**走 JSON-RPC：LSP4J 在首个请求上会构建嵌套服务分派表，
     * 顺带调 `getTextDocumentService()`，环境就会被创建——那是协议层的既定行为，
     * 不是服务端「为读状态而反向初始化」。所以这里直接对服务端对象断言。
     */
    @Test
    fun `closing an uninitialized server never creates the environment`() {
        val environmentCreations = AtomicInteger(0)
        val exitCodes = CopyOnWriteArrayList<Int>()
        val baseOptions = defaultServerOptions()
        val options = baseOptions.copy(
            environmentFactory = {
                environmentCreations.incrementAndGet()
                baseOptions.environmentFactory()
            },
            exitHandler = exitCodes::add,
        )

        val server = CangjieLanguageServer(options)
        server.close()
        server.close()
        server.exit()

        assertEquals(0, environmentCreations.get(), "close/exit 不得反向触发惰性环境初始化")
        assertEquals(listOf(1), exitCodes, "未初始化且未 shutdown 的 exit 仍应报告异常退出码")
    }

    /**
     * exit 重复到达只能产生一次退出码；资源释放也只能发生一次。
     */
    @Test
    fun `repeated exit reports one exit code and releases resources once`() {
        val exitCodes = CopyOnWriteArrayList<Int>()
        val factory = ProtocolContractAnalysisFacadeFactory()
        val options = CangjieLspServerOptions(
            descriptor = defaultServerOptions().descriptor,
            environmentMode = defaultServerOptions().environmentMode,
            environmentFactory = defaultServerOptions().environmentFactory,
            analysisFacadeFactory = factory::create,
            exitHandler = exitCodes::add,
        )

        val connection = LspIntegrationTestConnection.create(options)
        try {
            connection.initialize(
                LspClientCapabilitiesBuilder.initializeParams(capabilities = LspClientCapabilitiesBuilder.fullFeatured()),
            )
            connection.initialized()
            connection.server.shutdown().get()

            connection.server.exit()
            connection.server.exit()
        } finally {
            connection.close()
        }

        assertEquals(listOf(0), exitCodes, "重复 exit 只允许产生一次退出码")
        assertEquals(1, factory.requireFacade().closeCount, "facade 只允许被释放一次")
    }
}
