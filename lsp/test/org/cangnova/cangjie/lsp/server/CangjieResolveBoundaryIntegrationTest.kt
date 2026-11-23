package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspClientCapabilitiesBuilder
import org.cangnova.cangjie.lsp.testkit.ProtocolContractAnalysisFacadeFactory
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.InlayHint
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `resolve` 类入口的边界行为。
 *
 * `completionItem/resolve` 与 `inlayHint/resolve` 不带文档 URI，`codeAction/resolve`
 * 只带 `data`；三者共同的要求是：**定位信息失效时原样返回**，让候选/动作/提示继续可用，
 * 而不是抛错或返回空对象。这些路径在真实客户端上就是「点了没反应但也不报错」。
 */
class CangjieResolveBoundaryIntegrationTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：需要注入可记录的契约 facade 才能断言「是否进入 facade」。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * codeAction 的 `data` 缺失时，服务端必须原样返回动作且不进入 facade。
     */
    @Test
    fun `code action resolve without data keeps the action untouched`() {
        val factory = ProtocolContractAnalysisFacadeFactory()
        contractSession(factory).use { session ->
            val action = CodeAction().apply { title = "contractCodeAction" }

            val resolved = session.resolveCodeAction(action)

            assertEquals("contractCodeAction", resolved.title)
            assertNull(resolved.edit, "缺少 data 时无法定位文档，只能原样返回")
            assertFalse(factory.requireFacade().wasInvoked("resolveCodeAction"))
        }
    }

    /**
     * `data` 指向已经关闭的文档时，同样只能原样返回。
     */
    @Test
    fun `code action resolve for closed document keeps the action untouched`() {
        val factory = ProtocolContractAnalysisFacadeFactory()
        contractSession(factory).use { session ->
            val uri = "file:///workspace/closed.cj"
            session.openDocument(uri, "contract body")
            session.closeDocument(uri)

            val resolved = session.resolveCodeAction(
                CodeAction().apply {
                    title = "contractCodeAction"
                    data = mapOf(
                        "documentUri" to uri,
                        "diagnosticFactoryName" to "contract",
                        "startLine" to 0,
                        "startCharacter" to 0,
                        "endLine" to 0,
                        "endCharacter" to 4,
                    )
                },
            )

            assertEquals("contractCodeAction", resolved.title)
            assertNull(resolved.edit, "文档已关闭时 resolve 不得凭空造编辑")
            assertFalse(factory.requireFacade().wasInvoked("resolveCodeAction"))
        }
    }

    /**
     * `data` 字段残缺（例如缺少诊断标识）时按「无法定位」处理，不抛协议错误。
     */
    @Test
    fun `code action resolve with incomplete data degrades to untouched action`() {
        val factory = ProtocolContractAnalysisFacadeFactory()
        contractSession(factory).use { session ->
            val uri = "file:///workspace/partial.cj"
            session.openDocument(uri, "contract body")

            val resolved = session.resolveCodeAction(
                CodeAction().apply {
                    title = "contractCodeAction"
                    data = mapOf("documentUri" to uri)
                },
            )

            assertEquals("contractCodeAction", resolved.title)
            assertNull(resolved.edit)
            assertFalse(factory.requireFacade().wasInvoked("resolveCodeAction"))
        }
    }

    /**
     * `data` 结构完整且文档仍在存储中时，resolve 必须进入 facade 并补回编辑。
     *
     * 这条与上面三条构成同一入口的正反对照：原样返回只应发生在定位失效时。
     */
    @Test
    fun `code action resolve with valid data reaches the facade`() {
        val factory = ProtocolContractAnalysisFacadeFactory()
        contractSession(factory).use { session ->
            val uri = "file:///workspace/open.cj"
            session.openDocument(uri, "contract body")

            val resolved = session.resolveCodeAction(
                CodeAction().apply {
                    title = "contractCodeAction"
                    data = mapOf(
                        "documentUri" to uri,
                        "diagnosticFactoryName" to "contract",
                        "startLine" to 0,
                        "startCharacter" to 0,
                        "endLine" to 0,
                        "endCharacter" to 4,
                    )
                },
            )

            assertEquals("contractResolved", resolved.edit.changes.getValue(uri).single().newText)
            assertTrue(factory.requireFacade().wasInvoked("resolveCodeAction"))
        }
    }

    /**
     * inlayHint 的 resolve 不依赖文档定位：即使没有任何上下文，也必须原样返回提示。
     */
    @Test
    fun `inlay hint resolve without data keeps the hint untouched`() {
        val factory = ProtocolContractAnalysisFacadeFactory()
        contractSession(factory).use { session ->
            val hint = InlayHint().apply {
                position = Position(0, 0)
                label = Either.forLeft("contractHint")
                tooltip = null
            }

            val resolved = session.resolveInlayHint(hint)

            assertEquals("contractHint", resolved.label.left)
            assertTrue(
                factory.requireFacade().wasInvoked("resolveInlayHint"),
                "能力开启时 resolve 仍应经过 facade（缺 data 只是不补内容）",
            )
        }
    }

    /**
     * 用契约 facade 创建会话。
     */
    private fun contractSession(factory: ProtocolContractAnalysisFacadeFactory) = createSession(
        rootUri = "file:///workspace",
        capabilities = LspClientCapabilitiesBuilder.fullFeatured(),
        options = defaultServerOptions().copy(analysisFacadeFactory = factory::create),
    )
}
