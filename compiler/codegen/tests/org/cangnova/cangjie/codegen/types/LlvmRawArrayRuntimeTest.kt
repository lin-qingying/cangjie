package org.cangnova.cangjie.codegen.types

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirBoxType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirTypeDeclarationId
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.type.ChirVArrayType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证 RawArray TypeInfo 的 LLVM 布局与仓颉 v1.1.3 官方 ABI 一致。 */
class LlvmRawArrayRuntimeTest {
    @Test
    fun `emits official typeinfo layout and rawarray initializer fields`() {
        val arrayType = ChirRawArrayType(ChirResolvedTypeRef(ChirPrimitiveType.UINT8))
        val plan = LlvmRawArrayRuntime.declarations(
            allocationExpressions = listOf(arrayType to ChirSemanticId("test:rawarray-uint8")),
        )

        assertTrue(
            "%TypeInfo = type { ptr, i8, i8, i16, i32, ptr, i32, i8, i8, i16, ptr, ptr, ptr, ptr, ptr, ptr, ptr, ptr }" in plan.typeDeclarations,
            plan.typeDeclarations.toString(),
        )
        assertTrue(
            "%TypeTemplate = type { ptr, i8, i8, i16, i16, ptr, ptr, ptr, ptr, ptr, i16 }" in plan.typeDeclarations,
            plan.typeDeclarations.toString(),
        )

        val rawArrayTypeInfo = plan.globalDeclarations.single { it.startsWith("@\"RawArray<UInt8>.ti\"") }
        assertTrue(
            rawArrayTypeInfo.contains(
                "i8 -126, i8 0, i16 0, i32 0, ptr null, i32 0, i8 0, i8 0, i16 -32768, " +
                    "ptr null, ptr @RawArray.tt, ptr null, ptr null, ptr @\"UInt8.ti\", ptr null, ptr null, ptr null",
            ),
            rawArrayTypeInfo,
        )
    }

    @Test
    fun `uses official qualified nominal names in rawarray typeinfo`() {
        val arrayType = ChirRawArrayType(
            ChirResolvedTypeRef(ChirStructType(ChirTypeDeclarationId("std.core", "Widget"))),
        )
        val sourceId = ChirSemanticId("test:rawarray-widget")
        val plan = LlvmRawArrayRuntime.declarations(
            allocationExpressions = listOf(arrayType to sourceId),
        )

        assertTrue(
            plan.globalDeclarations.any { it == "@\"std.core:Widget.ti\" = external global %TypeInfo" },
            plan.globalDeclarations.toString(),
        )
        assertTrue(plan.globalDeclarations.any { it.startsWith("@\"RawArray<std.core:Widget>.ti\" = internal global %TypeInfo") })
        assertTrue(plan.globalDeclarations.any { "c\"RawArray<std.core:Widget>\\00\"" in it })
        assertTrue(plan.globalDeclarations.none { "std.core:std.core/Widget" in it })
    }

    @Test
    fun `omits box wrapper from rawarray typeinfo name field but preserves typeinfo symbol`() {
        val arrayType = ChirRawArrayType(
            ChirResolvedTypeRef(ChirBoxType(ChirResolvedTypeRef(ChirPrimitiveType.INT32))),
        )
        val plan = LlvmRawArrayRuntime.declarations(
            allocationExpressions = listOf(arrayType to ChirSemanticId("test:rawarray-box-int32")),
        )

        assertTrue(plan.globalDeclarations.any { it.startsWith("@\"RawArray<Box<Int32>>.ti\" = internal global %TypeInfo") })
        assertTrue(plan.globalDeclarations.any { "c\"RawArray<Int32>\\00\"" in it })
    }

    @Test
    fun `declares dynamic element typeinfo dependencies for rawarray allocation`() {
        val typeParameter = ChirResolvedTypeRef(
            ChirGenericType(
                identifier = "T",
                declarationIndex = 0,
                semanticIdentity = "fn:rawarray-pair:T",
            ),
        )
        val int64Type = ChirResolvedTypeRef(ChirPrimitiveType.INT64)
        val pairType = ChirResolvedTypeRef(
            ChirStructType(
                declarationId = ChirTypeDeclarationId("pkg", "Pair"),
                typeArguments = listOf(typeParameter, int64Type),
            ),
        )
        val sourceId = ChirSemanticId("fn:rawarray-pair:allocate")
        val plan = LlvmRawArrayRuntime.declarations(
            allocationExpressions = listOf(ChirRawArrayType(pairType) to sourceId),
        )

        assertTrue(plan.globalDeclarations.contains("@\"pkg:Pair.tt\" = external global %TypeTemplate"), plan.globalDeclarations.toString())
        assertTrue(plan.globalDeclarations.contains("@\"Int64.ti\" = external global %TypeInfo"), plan.globalDeclarations.toString())
        assertTrue(plan.globalDeclarations.contains("@\"RawArray.tt\" = external global %TypeTemplate"), plan.globalDeclarations.toString())
    }

    @Test
    fun `repeats VArray element typeinfo by its declared length`() {
        val typeParameter = ChirResolvedTypeRef(
            ChirGenericType(
                identifier = "T",
                declarationIndex = 0,
                semanticIdentity = "fn:varray-typeinfo:T",
            ),
        )
        val varrayType = ChirResolvedTypeRef(ChirVArrayType(typeParameter, size = 4))
        val template = LlvmRuntimeTypeInfo.dynamicTemplateReference(varrayType)

        assertEquals("VArray.tt", template?.symbolName)
        assertEquals(4, template?.typeArgumentCount)
        val arguments = mutableListOf<ChirTypeRef>()
        template?.forEachTypeArgument(arguments::add)
        assertEquals(List(4) { typeParameter }, arguments)
    }

    @Test
    fun `retains dynamic VArray template for zero length`() {
        val typeParameter = ChirResolvedTypeRef(
            ChirGenericType(
                identifier = "T",
                declarationIndex = 0,
                semanticIdentity = "fn:zero-varray-typeinfo:T",
            ),
        )
        val template = LlvmRuntimeTypeInfo.dynamicTemplateReference(
            ChirResolvedTypeRef(ChirVArrayType(typeParameter, size = 0)),
        )

        assertEquals("VArray.tt", template?.symbolName)
        assertEquals(0, template?.typeArgumentCount)
    }
}
