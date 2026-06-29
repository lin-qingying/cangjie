package org.cangnova.cangjie.lsp.framework

import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.capabilities.CangjieLanguageServerDescriptor
import org.cangnova.cangjie.lsp.testkit.LspIntegrationTestConnection
import org.cangnova.cangjie.lsp.testkit.LspClientCapabilitiesBuilder
import org.cangnova.cangjie.lsp.testkit.ServiceLoadedLspTestEnvironment
import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.InitializeParams
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
    /**
     * 默认创建的 LSP 集成测试会话。
     */
    protected lateinit var session: LspIntegrationTestSession
        private set

    /**
     * 是否在每个测试前自动创建默认会话。
     */
    protected open val autoCreateDefaultSession: Boolean = true

    /**
     * 在测试前创建默认 LSP 会话。
     */
    @BeforeEach
    fun setUpLspSession() {
        if (autoCreateDefaultSession) {
            session = createSession()
        }
    }

    /**
     * 在测试后关闭自动创建的默认 LSP 会话。
     */
    @AfterEach
    fun tearDownLspSession() {
        if (autoCreateDefaultSession && ::session.isInitialized) {
            session.close()
        }
    }

    /**
     * 使用给定服务端选项创建并初始化 LSP 会话。
     */
    protected open fun createSession(
        options: CangjieLspServerOptions = defaultServerOptions(),
    ): LspIntegrationTestSession {
        val connection = LspIntegrationTestConnection.create(options)
        connection.initialize()
        connection.initialized()
        return LspIntegrationTestSession(connection)
    }

    /**
     * 使用完整 initialize 参数创建并初始化 LSP 会话。
     */
    protected open fun createSession(
        params: InitializeParams,
        options: CangjieLspServerOptions = defaultServerOptions(),
    ): LspIntegrationTestSession {
        val connection = LspIntegrationTestConnection.create(options)
        connection.initialize(params)
        connection.initialized()
        return LspIntegrationTestSession(connection)
    }

    /**
     * 使用根 URI 和客户端能力创建并初始化 LSP 会话。
     */
    protected fun createSession(
        rootUri: String,
        capabilities: ClientCapabilities,
        options: CangjieLspServerOptions = defaultServerOptions(),
    ): LspIntegrationTestSession {
        return createSession(
            InitializeParams().apply {
                this.rootUri = rootUri
                this.capabilities = capabilities
            },
            options,
        )
    }

    /**
     * 返回全功能客户端能力配置。
     */
    protected fun fullFeaturedCapabilities(): ClientCapabilities = LspClientCapabilitiesBuilder.fullFeatured()

    /**
     * 构造测试默认服务端选项。
     */
    protected open fun defaultServerOptions(): CangjieLspServerOptions {
        return CangjieLspServerOptions(
            descriptor = defaultDescriptor(),
            environmentMode = CangJieCoreEnvironmentMode.UnitTest,
            environmentFactory = {
                ServiceLoadedLspTestEnvironment.create()
            },
            exitHandler = {},
        )
    }

    /**
     * 构造测试默认服务端静态能力描述。
     */
    protected open fun defaultDescriptor(): CangjieLanguageServerDescriptor = CangjieLanguageServerDescriptor()
}
