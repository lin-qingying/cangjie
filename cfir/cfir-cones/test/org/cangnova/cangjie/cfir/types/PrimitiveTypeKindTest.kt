package org.cangnova.cangjie.cfir.types

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 验证 CFIR 原始类型分类标记与仓颉内建数值类型集合保持一致。
 */
class PrimitiveTypeKindTest {

    /**
     * 验证所有有符号、无符号、平台相关整数和 IdealInt 都被标记为整数类型。
     */
    @Test
    fun `integer kinds include signed, unsigned, native, and IdealInt`() {
        val expectedIntegers = setOf(
            PrimitiveTypeKind.INT8, PrimitiveTypeKind.INT16,
            PrimitiveTypeKind.INT32, PrimitiveTypeKind.INT64,
            PrimitiveTypeKind.INT_NATIVE,
            PrimitiveTypeKind.UINT8, PrimitiveTypeKind.UINT16,
            PrimitiveTypeKind.UINT32, PrimitiveTypeKind.UINT64,
            PrimitiveTypeKind.UINT_NATIVE,
            PrimitiveTypeKind.IDEAL_INT,
        )
        for (kind in PrimitiveTypeKind.entries) {
            assertEquals(kind in expectedIntegers, kind.isInteger, "$kind.isInteger")
        }
    }

    /**
     * 验证 Float16、Float32、Float64 和 IdealFloat 都被标记为浮点类型。
     */
    @Test
    fun `float kinds include Float16, Float32, Float64, and IdealFloat`() {
        val expectedFloats = setOf(
            PrimitiveTypeKind.FLOAT16, PrimitiveTypeKind.FLOAT32,
            PrimitiveTypeKind.FLOAT64, PrimitiveTypeKind.IDEAL_FLOAT,
        )
        for (kind in PrimitiveTypeKind.entries) {
            assertEquals(kind in expectedFloats, kind.isFloat, "$kind.isFloat")
        }
    }

    /**
     * 验证数值类型集合正好等于整数类型和浮点类型的并集。
     */
    @Test
    fun `numeric is union of integer and float`() {
        for (kind in PrimitiveTypeKind.entries) {
            assertEquals(kind.isInteger || kind.isFloat, kind.isNumeric, "$kind.isNumeric")
        }
    }

    /**
     * 验证理想字面量类型只包含 IdealInt 和 IdealFloat。
     */
    @Test
    fun `ideal types are only IdealInt and IdealFloat`() {
        for (kind in PrimitiveTypeKind.entries) {
            val expected = kind == PrimitiveTypeKind.IDEAL_INT || kind == PrimitiveTypeKind.IDEAL_FLOAT
            assertEquals(expected, kind.isIdeal, "$kind.isIdeal")
        }
    }

    /**
     * 验证 Unit、Boolean、Rune 和 Nothing 不参与数值类型分类。
     */
    @Test
    fun `non-numeric types are Unit, Boolean, Rune, Nothing`() {
        val nonNumeric = setOf(
            PrimitiveTypeKind.UNIT, PrimitiveTypeKind.BOOLEAN,
            PrimitiveTypeKind.RUNE, PrimitiveTypeKind.NOTHING,
        )
        for (kind in nonNumeric) {
            assertFalse(kind.isNumeric, "$kind should not be numeric")
            assertFalse(kind.isInteger, "$kind should not be integer")
            assertFalse(kind.isFloat, "$kind should not be float")
        }
    }

    /**
     * 验证原始类型对外渲染名称与仓颉标准类型名一致。
     */
    @Test
    fun `typeName matches expected strings`() {
        assertEquals("Bool", PrimitiveTypeKind.BOOLEAN.typeName)
        assertEquals("Int64", PrimitiveTypeKind.INT64.typeName)
        assertEquals("Float64", PrimitiveTypeKind.FLOAT64.typeName)
        assertEquals("Unit", PrimitiveTypeKind.UNIT.typeName)
        assertEquals("Nothing", PrimitiveTypeKind.NOTHING.typeName)
        assertEquals("Rune", PrimitiveTypeKind.RUNE.typeName)
        assertEquals("IdealInt", PrimitiveTypeKind.IDEAL_INT.typeName)
        assertEquals("IdealFloat", PrimitiveTypeKind.IDEAL_FLOAT.typeName)
    }
}
