package org.cangnova.cangjie.cli

import org.cangnova.cangjie.codegen.api.CodegenOptimizationLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CjmpCodegenOptimizationTest {
    @Test
    fun `maps every accepted cjmp optimization level to llvm`() {
        val cases = listOf(
            null to (false to CodegenOptimizationLevel.NONE),
            "0" to (false to CodegenOptimizationLevel.NONE),
            "1" to (true to CodegenOptimizationLevel.LESS),
            "2" to (true to CodegenOptimizationLevel.DEFAULT),
            "3" to (true to CodegenOptimizationLevel.AGGRESSIVE),
            "s" to (true to CodegenOptimizationLevel.SIZE),
            "z" to (true to CodegenOptimizationLevel.SIZE_AGGRESSIVE),
            "9" to (true to CodegenOptimizationLevel.AGGRESSIVE),
        )

        cases.forEach { (input, expected) ->
            val actual = cjmpCodegenOptimization(input)
            assertEquals(expected.first, actual.optimizeLlvmModule, "-O${input.orEmpty()}")
            assertEquals(expected.second, actual.level, "-O${input.orEmpty()}")
        }
    }

    @Test
    fun `rejects optimization levels not accepted by the frontend`() {
        assertTrue(runCatching { cjmpCodegenOptimization("invalid") }.isFailure)
        assertFalse(cjmpCodegenOptimization("0").optimizeLlvmModule)
    }
}
