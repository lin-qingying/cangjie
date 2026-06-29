package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspClientCapabilitiesBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutionException

/**
 * 校验语言服务器初始化、关闭和退出码的生命周期协议。
 *
 * 该测试使用可替换退出处理器避免测试进程退出，同时固定 shutdown/exit 的协议语义。
 */
class CangjieLanguageServerLifecycleTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话，测试按场景手动创建服务器或会话。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 校验未连接客户端时 initialize 会快速失败。
     *
     * 该用例确保服务端不会在缺失远端代理时进入部分初始化状态。
     */
    @Test
    fun `initialize fails fast when client is not connected`() {
        val server = CangjieLanguageServer(defaultServerOptions())
        try {
            val exception = assertThrows<ExecutionException> {
                server.initialize(org.eclipse.lsp4j.InitializeParams()).get()
            }
            assertTrue(exception.cause is IllegalStateException)
        } finally {
            server.close()
        }
    }

    /**
     * 校验正常关闭的实时会话会请求成功退出码。
     *
     * 该用例通过替换 exit handler 观察退出码，不触发真实 JVM 退出。
     */
    @Test
    fun `closing a live session exits with success code`() {
        val exitCodes = CopyOnWriteArrayList<Int>()
        val options = defaultServerOptions().copy(
            exitHandler = exitCodes::add,
        )

        val testSession = createSession(
            rootUri = "file:///workspace",
            capabilities = LspClientCapabilitiesBuilder.fullFeatured(),
            options = options,
        )
        testSession.close()

        assertEquals(listOf(0), exitCodes)
    }

    /**
     * 校验未收到 shutdown 就调用 exit 会报告非零退出码。
     *
     * 该用例固定 LSP 生命周期协议中的异常退出语义。
     */
    @Test
    fun `exit without shutdown reports non zero exit code`() {
        val exitCodes = CopyOnWriteArrayList<Int>()
        val server = CangjieLanguageServer(
            CangjieLspServerOptions(
                environmentMode = defaultServerOptions().environmentMode,
                environmentFactory = defaultServerOptions().environmentFactory,
                analysisFacadeFactory = defaultServerOptions().analysisFacadeFactory,
                descriptor = defaultServerOptions().descriptor,
                exitHandler = exitCodes::add,
            ),
        )

        try {
            server.exit()
        } finally {
            server.close()
        }

        assertEquals(listOf(1), exitCodes)
    }
}
