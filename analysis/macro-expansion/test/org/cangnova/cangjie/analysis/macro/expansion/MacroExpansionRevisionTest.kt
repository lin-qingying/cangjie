package org.cangnova.cangjie.analysis.macro.expansion

import org.cangnova.cangjie.macro.expansion.MacroExpansionRevision
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals

/**
 * revision 的相等语义：任一输入维度变化都必须被视为"输入已变"。
 *
 * 框架只做相等比较，因此这些用例同时锁定了"旧结果被丢弃、新结果重新构造"的触发条件。
 */
class MacroExpansionRevisionTest {
    private val captured = MacroExpansionRevision(
        moduleKey = "module-a",
        psiModificationCount = 12,
        projectRootModificationCount = 7,
        compilerSettingsModificationCount = 4,
        sdkKey = "sdk-a",
    )

    @Test
    fun identicalInputsMatch() {
        assertEquals(
            captured,
            MacroExpansionRevision(
                moduleKey = "module-a",
                psiModificationCount = 12,
                projectRootModificationCount = 7,
                compilerSettingsModificationCount = 4,
                sdkKey = "sdk-a",
            ),
        )
    }

    @Test
    fun psiModificationInvalidatesSnapshot() {
        assertNotEquals(captured, captured.copy(psiModificationCount = 13))
    }

    @Test
    fun projectRootModificationInvalidatesSnapshot() {
        assertNotEquals(captured, captured.copy(projectRootModificationCount = 8))
    }

    @Test
    fun compilerSettingsModificationInvalidatesSnapshot() {
        assertNotEquals(captured, captured.copy(compilerSettingsModificationCount = 5))
    }

    @Test
    fun sdkReplacementInvalidatesSnapshot() {
        assertNotEquals(captured, captured.copy(sdkKey = "sdk-b"))
        assertNotEquals(captured, captured.copy(sdkKey = null))
    }

    @Test
    fun macroArtifactFingerprintInvalidatesSnapshot() {
        assertNotEquals(
            captured,
            captured.copy(artifactFingerprint = "macro-artifacts:changed"),
        )
    }
}
