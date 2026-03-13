package org.cangnova.cangjie.cfir.types

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PrimitiveTypeKindTest {

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

    @Test
    fun `numeric is union of integer and float`() {
        for (kind in PrimitiveTypeKind.entries) {
            assertEquals(kind.isInteger || kind.isFloat, kind.isNumeric, "$kind.isNumeric")
        }
    }

    @Test
    fun `ideal types are only IdealInt and IdealFloat`() {
        for (kind in PrimitiveTypeKind.entries) {
            val expected = kind == PrimitiveTypeKind.IDEAL_INT || kind == PrimitiveTypeKind.IDEAL_FLOAT
            assertEquals(expected, kind.isIdeal, "$kind.isIdeal")
        }
    }

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
