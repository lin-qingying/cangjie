@file:OptIn(org.cangnova.cangjie.config.CompilerConfiguration.Internals::class)

package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.macro.expansion.MacroConstructionReadAccess
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** frontend 的实际读取/写入接口必须消费宿主安装的同一 read-access capability。 */
class MacroConstructionReadAccessConfigurationTest {
    @Test
    fun actualFrontendLookupAndSetterPreserveTheSharedHostBarrier() {
        val configuration = CompilerConfiguration()
        var inReadAccess = false
        var retired = false
        var consumerEntered = false
        val host = object : MacroConstructionReadAccess {
            override fun <T> compute(action: () -> T): T {
                check(!retired)
                inReadAccess = true
                return try { action() } finally { inReadAccess = false }
            }
        }
        assertSame(MacroConstructionReadAccess.CONFIGURATION_KEY, FrontendMacroConfigurationKeys.MACRO_CONSTRUCTION_READ_ACCESS)
        configuration.put(MacroConstructionReadAccess.CONFIGURATION_KEY, host)
        assertSame(host, configuration.macroConstructionReadAccess)
        assertEquals(42, configuration.macroConstructionReadAccess.compute {
            assertTrue(inReadAccess)
            42
        })
        configuration.macroConstructionReadAccess = host
        assertSame(host, configuration.get(MacroConstructionReadAccess.CONFIGURATION_KEY))
        retired = true
        assertThrows(IllegalStateException::class.java) {
            configuration.macroConstructionReadAccess.compute { consumerEntered = true }
        }
        assertFalse(consumerEntered)
    }
}
