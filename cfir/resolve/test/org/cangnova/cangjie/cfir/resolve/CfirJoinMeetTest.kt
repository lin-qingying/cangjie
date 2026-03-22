package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CfirJoinMeetTest {

    private val joinMeet = CfirJoinMeet(CfirTypeRelations(JoinMeetTestContext()))

    @Test
    fun `join single type returns itself`() {
        assertEquals(ConePrimitiveType.INT32, joinMeet.join(listOf(ConePrimitiveType.INT32)))
    }

    @Test
    fun `join identical types returns same`() {
        assertEquals(
            ConePrimitiveType.INT32,
            joinMeet.join(listOf(ConePrimitiveType.INT32, ConePrimitiveType.INT32)),
        )
    }

    @Test
    fun `join signed integers returns wider`() {
        val result = joinMeet.join(listOf(ConePrimitiveType.INT32, ConePrimitiveType.INT64))
        assertEquals(ConePrimitiveType.INT64, result)
    }

    @Test
    fun `join int32 and int8 returns int32`() {
        val result = joinMeet.join(listOf(ConePrimitiveType.INT8, ConePrimitiveType.INT32))
        assertEquals(ConePrimitiveType.INT32, result)
    }

    @Test
    fun `join ideal int with concrete int returns concrete`() {
        val result = joinMeet.join(listOf(ConePrimitiveType.IDEAL_INT, ConePrimitiveType.INT32))
        assertEquals(ConePrimitiveType.INT32, result)
    }

    @Test
    fun `meet single type returns itself`() {
        assertEquals(ConePrimitiveType.INT32, joinMeet.meet(listOf(ConePrimitiveType.INT32)))
    }

    @Test
    fun `meet signed integers returns narrower`() {
        val result = joinMeet.meet(listOf(ConePrimitiveType.INT32, ConePrimitiveType.INT64))
        assertEquals(ConePrimitiveType.INT32, result)
    }

    @Test
    fun `meet ideal int with concrete returns concrete`() {
        val result = joinMeet.meet(listOf(ConePrimitiveType.IDEAL_INT, ConePrimitiveType.INT32))
        assertEquals(ConePrimitiveType.INT32, result)
    }

    @Test
    fun `join empty list returns null`() {
        assertNull(joinMeet.join(emptyList()))
    }

    @Test
    fun `meet empty list returns null`() {
        assertNull(joinMeet.meet(emptyList()))
    }

    @Test
    fun `meetUpperBounds separates variable and non-variable bounds`() {
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 1,
            lookupTag = ConeTypeParameterLookupTag("T"),
        )

        val result = joinMeet.meetUpperBounds(
            variable,
            listOf(ConePrimitiveType.INT64, ConePrimitiveType.INT32),
            emptySet(),
        )
        // meet of Int64 and Int32 → Int32 (narrower)
        assertEquals(ConePrimitiveType.INT32, result)
    }
}

private class JoinMeetTestContext : ConeTypeContext {
    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> = emptyList()

    override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean {
        if (a is ConePrimitiveType && b is ConePrimitiveType) return a.kind == b.kind
        return a == b
    }
}
