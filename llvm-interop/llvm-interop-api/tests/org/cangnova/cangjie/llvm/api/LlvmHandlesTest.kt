package org.cangnova.cangjie.llvm.api

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LlvmHandlesTest {
    @Test
    fun `all handles expose null sentinel and null check`() {
        assertTrue(LlvmContextRef.NULL.isNull)
        assertTrue(LlvmModuleRef.NULL.isNull)
        assertTrue(LlvmTypeRef.NULL.isNull)
        assertTrue(LlvmValueRef.NULL.isNull)
        assertTrue(LlvmBasicBlockRef.NULL.isNull)
        assertTrue(LlvmBuilderRef.NULL.isNull)
        assertTrue(LlvmTargetMachineRef.NULL.isNull)

        assertFalse(LlvmContextRef(1L).isNull)
        assertFalse(LlvmModuleRef(1L).isNull)
        assertFalse(LlvmTypeRef(1L).isNull)
        assertFalse(LlvmValueRef(1L).isNull)
        assertFalse(LlvmBasicBlockRef(1L).isNull)
        assertFalse(LlvmBuilderRef(1L).isNull)
        assertFalse(LlvmTargetMachineRef(1L).isNull)
    }
}
