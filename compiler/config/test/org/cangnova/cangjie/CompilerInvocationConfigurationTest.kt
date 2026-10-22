package org.cangnova.cangjie

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.compileMacro
import org.cangnova.cangjie.config.diagnosticFormat
import org.cangnova.cangjie.config.dumpMacro
import org.cangnova.cangjie.config.errorCountLimit
import org.cangnova.cangjie.config.parallelMacroExpansion
import org.cangnova.cangjie.config.warningGroupsEnabled
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证 invocation 选项通过配置键隔离并可清除，避免前端复用时污染下一次调用。 */
@OptIn(CompilerConfiguration.Internals::class)
class CompilerInvocationConfigurationTest {
    @Test
    fun booleanAndCollectionOptionsHaveTypedDefaultsAndCopyWithConfiguration() {
        val configuration = CompilerConfiguration()
        assertFalse(configuration.compileMacro)
        assertFalse(configuration.parallelMacroExpansion)
        assertFalse(configuration.dumpMacro)
        assertTrue(configuration.warningGroupsEnabled.isEmpty())

        configuration.compileMacro = true
        configuration.parallelMacroExpansion = true
        configuration.dumpMacro = true
        configuration.warningGroupsEnabled = setOf("unused", "deprecated")

        val copy = configuration.copy()
        assertTrue(copy.compileMacro)
        assertTrue(copy.parallelMacroExpansion)
        assertTrue(copy.dumpMacro)
        assertEquals(setOf("unused", "deprecated"), copy.warningGroupsEnabled)
    }

    @Test
    fun nullableInvocationOptionsClearPreviousValues() {
        val configuration = CompilerConfiguration()
        configuration.diagnosticFormat = "json"
        configuration.errorCountLimit = "10"

        configuration.diagnosticFormat = null
        configuration.errorCountLimit = null

        assertNull(configuration.diagnosticFormat)
        assertNull(configuration.errorCountLimit)
    }
}
