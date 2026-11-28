package org.cangnova.cangjie.lsp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CangjieLspServerOptionsTest {
    @Test
    fun `no sdk home keeps macro expansion disabled`() {
        val options = lspServerOptionsFromArgs(emptyArray(), emptyMap())
        assertNull(options.macroExpansion)
    }

    @Test
    fun `sdk home assembles host provided handle`() {
        val sdk = lspSdkHandleFromArgs(
            arrayOf(
                "--sdk-home", "/opt/cangjie",
                "--stdlib", "/opt/cangjie/modules",
                "--macro-server=/opt/cangjie/tools/bin/LSPMacroServer",
            ),
            mapOf("PATH" to "/usr/bin"),
        )
        assertNotNull(sdk)
        assertEquals("lsp:/opt/cangjie", sdk!!.id)
        assertEquals("/opt/cangjie", sdk.homePath)
        assertEquals("/opt/cangjie/modules", sdk.stdlibPath)
        assertEquals("/opt/cangjie/tools/bin/LSPMacroServer", sdk.macroServerPath)
        assertEquals("/opt/cangjie", sdk.environment["CANGJIE_HOME"])
        assertEquals("/usr/bin", sdk.environment["PATH"])
    }

    @Test
    fun `sdk home produces macro expansion options`() {
        val options = lspServerOptionsFromArgs(arrayOf("--sdk-home=/opt/cangjie"), emptyMap())
        assertNotNull(options.macroExpansion)
    }
}
