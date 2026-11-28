package org.cangnova.cangjie.codegen.types

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证模块级 String 字面量池保持官方 UTF-8 字节布局。 */
class LlvmStringLiteralRuntimeTest {
    @Test
    fun `packs UTF-8 literals and produces String aggregate constants`() {
        val stringType = ChirResolvedTypeRef(ChirStructType(LlvmStringLiteralRuntime.STRING_TYPE_ID))
        val literals = listOf("alpha", "pha", "你好").mapIndexed { index, literal ->
            ChirConstantValue(ChirSemanticId("const:string:$index"), stringType, literal)
        }

        val plan = LlvmStringLiteralRuntime.plan(literals)

        assertEquals(
            listOf("%struct.std.core_String = type { ptr addrspace(1), i32, i32 }"),
            plan.typeDeclarations,
        )
        assertNotNull(plan.requiredByteArrayTypeInfo)
        val backingData = plan.globalDeclarations.single { it.contains("= private constant { ptr, i64,") }
        assertTrue("i64 11, [11 x i8]" in backingData, backingData)
        assertTrue("c\"\\E4\\BD\\A0\\E5\\A5\\BDalpha\"" in backingData, backingData)

        val alphaGlobal = plan.globalDeclarations.single { it.startsWith(LlvmStringLiteralRuntime.literalReference("alpha")) }
        val substringGlobal = plan.globalDeclarations.single { it.startsWith(LlvmStringLiteralRuntime.literalReference("pha")) }
        val unicodeGlobal = plan.globalDeclarations.single { it.startsWith(LlvmStringLiteralRuntime.literalReference("你好")) }
        assertTrue("i32 6, i32 5" in alphaGlobal, alphaGlobal)
        assertTrue("i32 8, i32 3" in substringGlobal, substringGlobal)
        assertTrue("i32 0, i32 6" in unicodeGlobal, unicodeGlobal)
        assertTrue(plan.literalValues.getValue("alpha").contains("i32 6, i32 5"))
        assertTrue(plan.literalValues.getValue("pha").contains("i32 8, i32 3"))
        assertTrue(plan.literalValues.getValue("你好").contains("i32 0, i32 6"))
    }
}
