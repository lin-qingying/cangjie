package org.cangjie.cfir.types

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IdealTypeResolverTest {

    @Test
    fun `resolveIdealInt defaults to Int64 when no target`() {
        val result = IdealTypeResolver.resolveIdealInt()
        assertEquals(ConePrimitiveType.INT64, result)
    }

    @Test
    fun `resolveIdealInt returns target when target is integer`() {
        val result = IdealTypeResolver.resolveIdealInt(ConePrimitiveType.INT8)
        assertEquals(ConePrimitiveType.INT8, result)
    }

    @Test
    fun `resolveIdealInt returns target for unsigned integer`() {
        val result = IdealTypeResolver.resolveIdealInt(ConePrimitiveType.UINT32)
        assertEquals(ConePrimitiveType.UINT32, result)
    }

    @Test
    fun `resolveIdealInt ignores non-integer target`() {
        val result = IdealTypeResolver.resolveIdealInt(ConePrimitiveType.FLOAT64)
        assertEquals(ConePrimitiveType.INT64, result)
    }

    @Test
    fun `resolveIdealInt ignores IdealInt as target`() {
        val result = IdealTypeResolver.resolveIdealInt(ConePrimitiveType.IDEAL_INT)
        assertEquals(ConePrimitiveType.INT64, result)
    }

    @Test
    fun `resolveIdealFloat defaults to Float64 when no target`() {
        val result = IdealTypeResolver.resolveIdealFloat()
        assertEquals(ConePrimitiveType.FLOAT64, result)
    }

    @Test
    fun `resolveIdealFloat returns target when target is float`() {
        val result = IdealTypeResolver.resolveIdealFloat(ConePrimitiveType.FLOAT32)
        assertEquals(ConePrimitiveType.FLOAT32, result)
    }

    @Test
    fun `resolveIdealFloat ignores non-float target`() {
        val result = IdealTypeResolver.resolveIdealFloat(ConePrimitiveType.INT64)
        assertEquals(ConePrimitiveType.FLOAT64, result)
    }

    @Test
    fun `resolveIdealFloat ignores IdealFloat as target`() {
        val result = IdealTypeResolver.resolveIdealFloat(ConePrimitiveType.IDEAL_FLOAT)
        assertEquals(ConePrimitiveType.FLOAT64, result)
    }

    @Test
    fun `resolveIfIdeal resolves IdealInt`() {
        val result = IdealTypeResolver.resolveIfIdeal(ConePrimitiveType.IDEAL_INT)
        assertEquals(ConePrimitiveType.INT64, result)
    }

    @Test
    fun `resolveIfIdeal resolves IdealFloat`() {
        val result = IdealTypeResolver.resolveIfIdeal(ConePrimitiveType.IDEAL_FLOAT)
        assertEquals(ConePrimitiveType.FLOAT64, result)
    }

    @Test
    fun `resolveIfIdeal resolves IdealInt with target`() {
        val result = IdealTypeResolver.resolveIfIdeal(ConePrimitiveType.IDEAL_INT, ConePrimitiveType.INT16)
        assertEquals(ConePrimitiveType.INT16, result)
    }

    @Test
    fun `resolveIfIdeal passes through non-ideal types`() {
        assertSame(ConePrimitiveType.INT64, IdealTypeResolver.resolveIfIdeal(ConePrimitiveType.INT64))
        assertSame(ConePrimitiveType.BOOLEAN, IdealTypeResolver.resolveIfIdeal(ConePrimitiveType.BOOLEAN))
    }

    @Test
    fun `resolveIfIdeal passes through non-primitive types`() {
        val classType = ConeClassLikeType(
            ConeClassLookupTagImpl(StdlibClassIds.String),
        )
        assertSame(classType, IdealTypeResolver.resolveIfIdeal(classType))
    }
}
