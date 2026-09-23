package org.cangnova.cangjie

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.cjoOutputFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** 验证可复用编译配置不会泄漏上一轮 `.cj.d` 输出路径。 */
@OptIn(CompilerConfiguration.Internals::class)
class CompilerConfigurationTest {
    @Test
    fun nullableCjoOutputPropertiesRemovePreviousInvocationState() {
        val configuration = CompilerConfiguration()
        configuration.cjoOutputDirectory = "build/first"
        configuration.cjoOutputFile = "build/first/product"

        configuration.cjoOutputDirectory = null
        configuration.cjoOutputFile = null

        assertNull(configuration.cjoOutputDirectory)
        assertNull(configuration.cjoOutputFile)

        configuration.cjoOutputDirectory = "build/second"
        assertEquals("build/second", configuration.cjoOutputDirectory)
        assertNull(configuration.cjoOutputFile)
    }
}
