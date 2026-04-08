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

class CangjieLanguageServerLifecycleTest : AbstractLspIntegrationTest() {
    override val autoCreateDefaultSession: Boolean = false

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
