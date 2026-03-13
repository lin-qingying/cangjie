package org.cangnova.cangjie.cfir.types

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ConeBuiltinTypeUtilsTest {

    // ---- Builtin primitive type checks ----

    @Test
    fun `isBoolean returns true only for ConePrimitiveType BOOLEAN`() {
        assertTrue(ConePrimitiveType.BOOLEAN.isBoolean)
        assertFalse(ConePrimitiveType.INT64.isBoolean)
        assertFalse(ConePrimitiveType.UNIT.isBoolean)
    }

    @Test
    fun `isInt64 returns true only for ConePrimitiveType INT64`() {
        assertTrue(ConePrimitiveType.INT64.isInt64)
        assertFalse(ConePrimitiveType.INT32.isInt64)
        assertFalse(ConePrimitiveType.FLOAT64.isInt64)
    }

    @Test
    fun `isIntegerType matches all integer ConePrimitiveTypes`() {
        assertTrue(ConePrimitiveType.INT8.isIntegerType)
        assertTrue(ConePrimitiveType.INT16.isIntegerType)
        assertTrue(ConePrimitiveType.INT32.isIntegerType)
        assertTrue(ConePrimitiveType.INT64.isIntegerType)
        assertTrue(ConePrimitiveType.UINT8.isIntegerType)
        assertTrue(ConePrimitiveType.UINT64.isIntegerType)
        assertTrue(ConePrimitiveType.IDEAL_INT.isIntegerType)
        assertFalse(ConePrimitiveType.FLOAT64.isIntegerType)
        assertFalse(ConePrimitiveType.BOOLEAN.isIntegerType)
    }

    @Test
    fun `isFloatType matches float ConePrimitiveTypes`() {
        assertTrue(ConePrimitiveType.FLOAT16.isFloatType)
        assertTrue(ConePrimitiveType.FLOAT32.isFloatType)
        assertTrue(ConePrimitiveType.FLOAT64.isFloatType)
        assertTrue(ConePrimitiveType.IDEAL_FLOAT.isFloatType)
        assertFalse(ConePrimitiveType.INT64.isFloatType)
    }

    @Test
    fun `isNumericType is union of integer and float`() {
        assertTrue(ConePrimitiveType.INT64.isNumericType)
        assertTrue(ConePrimitiveType.FLOAT64.isNumericType)
        assertFalse(ConePrimitiveType.BOOLEAN.isNumericType)
        assertFalse(ConePrimitiveType.RUNE.isNumericType)
        assertFalse(ConePrimitiveType.UNIT.isNumericType)
    }

    @Test
    fun `isPrimitiveType returns true for all ConePrimitiveType instances`() {
        assertTrue(ConePrimitiveType.INT64.isPrimitiveType)
        assertTrue(ConePrimitiveType.UNIT.isPrimitiveType)
        assertTrue(ConePrimitiveType.NOTHING.isPrimitiveType)
    }

    @Test
    fun `isPrimitiveType returns false for non-primitive types`() {
        val classType = ConeClassLikeType(
            ConeClassLookupTagImpl(StdlibClassIds.String),
        )
        assertFalse(classType.isPrimitiveType)
    }

    // ---- IdealType checks ----

    @Test
    fun `isIdealInt and isIdealFloat are exclusive`() {
        assertTrue(ConePrimitiveType.IDEAL_INT.isIdealInt)
        assertFalse(ConePrimitiveType.IDEAL_INT.isIdealFloat)
        assertTrue(ConePrimitiveType.IDEAL_FLOAT.isIdealFloat)
        assertFalse(ConePrimitiveType.IDEAL_FLOAT.isIdealInt)
    }

    @Test
    fun `isIdealType covers both ideal types`() {
        assertTrue(ConePrimitiveType.IDEAL_INT.isIdealType)
        assertTrue(ConePrimitiveType.IDEAL_FLOAT.isIdealType)
        assertFalse(ConePrimitiveType.INT64.isIdealType)
        assertFalse(ConePrimitiveType.FLOAT64.isIdealType)
    }

    // ---- Stdlib type checks (ClassId-based) ----

    @Test
    fun `classId returns ClassId for ConeClassLikeType`() {
        val stringType = ConeClassLikeType(
            ConeClassLookupTagImpl(StdlibClassIds.String),
        )
        assertEquals(StdlibClassIds.String, stringType.classId)
    }

    @Test
    fun `classId returns null for ConePrimitiveType`() {
        assertNull(ConePrimitiveType.INT64.classId)
    }

    @Test
    fun `isString checks ClassId`() {
        val stringType = ConeClassLikeType(
            ConeClassLookupTagImpl(StdlibClassIds.String),
        )
        assertTrue(stringType.isString)
        assertFalse(ConePrimitiveType.INT64.isString)
    }

    @Test
    fun `isArray checks ClassId`() {
        val arrayType = ConeClassLikeType(
            ConeClassLookupTagImpl(StdlibClassIds.Array),
        )
        assertTrue(arrayType.isArray)
    }

    @Test
    fun `isOption checks ClassId`() {
        val optionType = ConeClassLikeType(
            ConeClassLookupTagImpl(StdlibClassIds.Option),
        )
        assertTrue(optionType.isOption)
    }
}
