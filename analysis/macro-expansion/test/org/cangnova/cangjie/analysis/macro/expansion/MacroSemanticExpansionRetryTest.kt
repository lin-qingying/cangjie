package org.cangnova.cangjie.analysis.macro.expansion

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * 锁定"输入变化时重新捕获、其余结果立即结束"的构造重试语义。
 */
class MacroSemanticExpansionRetryTest {
    @Test
    fun changedInputIsRecapturedUntilOneAttemptCompletes() {
        var attempts = 0
        var stabilizationWaits = 0

        runMacroSemanticExpansionUntilStable(
            shouldContinue = { true },
            waitForInputStabilization = { stabilizationWaits++ },
        ) {
            attempts++
            if (attempts < 3) {
                MacroExpansionAttempt.INPUT_CHANGED
            } else {
                MacroExpansionAttempt.COMPLETED
            }
        }

        assertEquals(3, attempts)
        assertEquals(2, stabilizationWaits)
    }

    @Test
    fun invalidatedSessionStopsAfterTheCurrentAttempt() {
        var sessionActive = true
        var attempts = 0
        var stabilizationWaits = 0

        runMacroSemanticExpansionUntilStable(
            shouldContinue = { sessionActive },
            waitForInputStabilization = {
                stabilizationWaits++
                sessionActive = false
            },
        ) {
            attempts++
            MacroExpansionAttempt.INPUT_CHANGED
        }

        assertEquals(1, attempts)
        assertEquals(1, stabilizationWaits)
    }

    @Test
    fun successfulFirstAttemptDoesNotWaitForStabilization() {
        var attempts = 0
        var stabilizationWaits = 0

        runMacroSemanticExpansionUntilStable(
            shouldContinue = { true },
            waitForInputStabilization = { stabilizationWaits++ },
        ) {
            attempts++
            MacroExpansionAttempt.COMPLETED
        }

        assertEquals(1, attempts)
        assertEquals(0, stabilizationWaits)
    }

    @Test
    fun alreadyInvalidatedSessionSkipsConstructionEntirely() {
        var attempts = 0

        runMacroSemanticExpansionUntilStable(
            shouldContinue = { false },
            waitForInputStabilization = { error("不应等待输入稳定") },
        ) {
            attempts++
            MacroExpansionAttempt.COMPLETED
        }

        assertEquals(0, attempts)
    }
}
