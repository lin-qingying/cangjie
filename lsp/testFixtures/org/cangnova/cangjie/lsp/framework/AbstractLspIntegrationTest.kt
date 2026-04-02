package org.cangnova.cangjie.lsp.framework

import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.capabilities.CangjieLanguageServerDescriptor
import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.cangnova.cangjie.lsp.testkit.ServiceLoadedLspTestEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * LSP 集成测试基类。
 *
 * 统一负责：
 * 1. 创建带服务加载的测试环境
 * 2. 启动语言服务器
 * 3. 建立真实 LSP 双端连接
 * 4. 自动执行 initialize / initialized
 *
 * 具体测试只保留交互动作和断言。
 */
abstract class AbstractLspIntegrationTest {
    protected lateinit var session: LspIntegrationTestSession
        private set

    @BeforeEach
    fun setUpLspSession() {
        session = createSession()
    }

    @AfterEach
    fun tearDownLspSession() {
        if (::session.isInitialized) {
            session.close()
        }
    }

    protected open fun createSession(
        options: CangjieLspServerOptions = defaultServerOptions(),
    ): LspIntegrationTestSession {
        val connection = LspIntegrationTestConnection.create(options)
        connection.initialize()
        connection.initialized()
        return LspIntegrationTestSession(connection)
    }

    protected open fun defaultServerOptions(): CangjieLspServerOptions {
        return CangjieLspServerOptions(
            descriptor = defaultDescriptor(),
            environmentMode = CangJieCoreEnvironmentMode.UnitTest,
            environmentFactory = {
                ServiceLoadedLspTestEnvironment.create()
            },
        )
    }

    protected open fun defaultDescriptor(): CangjieLanguageServerDescriptor = CangjieLanguageServerDescriptor()
}
