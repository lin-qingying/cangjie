package org.cangnova.cangjie.llvm.jni

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class LlvmNativeIntegrationTest {
    companion object {
        private const val ENABLE_FLAG = "CANGJIE_LLVM_JNI_INTEGRATION"
        private const val LIB_PATH_FLAG = "CANGJIE_LLVM_JNI_LIBRARY_PATH"

        @JvmStatic
        @BeforeAll
        fun setupNativePath() {
            assumeTrue(
                System.getenv(ENABLE_FLAG).equals("true", ignoreCase = true),
                "integration test disabled, set $ENABLE_FLAG=true to enable",
            )
            val libraryPath = System.getenv(LIB_PATH_FLAG).orEmpty()
            assumeTrue(libraryPath.isNotBlank(), "missing $LIB_PATH_FLAG")
            System.setProperty("cangjie.llvm.native.library.path", libraryPath)
        }
    }

    @Test
    fun `can create and verify module through JNI bridge`() {
        assertTrue(LlvmNative.isAvailable, LlvmNative.loadDiagnostics)
        var context = 0L
        var module = 0L
        try {
            context = LlvmNative.contextCreate()
            assertNotEquals(0L, context)

            module = LlvmNative.moduleCreateInContext("integration_test_module", context)
            assertNotEquals(0L, module)

            val int32Type = LlvmNative.intTypeInContext(context, 32)
            val fnType = LlvmNative.functionType(int32Type, longArrayOf(), false)
            val function = LlvmNative.moduleAddFunction(module, "main", fnType)
            assertNotEquals(0L, function)

            LlvmNative.moduleVerify(module)
            val ir = LlvmNative.modulePrintToString(module)
            assertTrue(ir.contains("define i32 @main()"))

            val bitcode = LlvmNative.writeBitcodeToMemoryBuffer(module)
            assertFalse(bitcode.isEmpty())
            assertEquals("main", LlvmNative.valueGetName(function))
        } finally {
            if (module != 0L) {
                LlvmNative.moduleDispose(module)
            }
            if (context != 0L) {
                LlvmNative.contextDispose(context)
            }
        }
    }
}
