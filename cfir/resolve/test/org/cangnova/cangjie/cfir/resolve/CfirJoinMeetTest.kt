package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * [CfirJoinMeet] 上界 join 和下界 meet 计算测试。
 */
class CfirJoinMeetTest {

    /**
     * 测试使用的 join/meet 计算器。
     */
    private val joinMeet = CfirJoinMeet(CfirTypeRelations(JoinMeetTestContext()))

    /**
     * 验证单元素 join 返回该元素本身。
     */
    @Test
    fun `join single type returns itself`() {
        assertEquals(ConePrimitiveType.INT32, joinMeet.join(listOf(ConePrimitiveType.INT32)))
    }

    /**
     * 验证相同类型的 join 仍为同一类型。
     */
    @Test
    fun `join identical types returns same`() {
        assertEquals(
            ConePrimitiveType.INT32,
            joinMeet.join(listOf(ConePrimitiveType.INT32, ConePrimitiveType.INT32)),
        )
    }

    /**
     * 验证有符号整数 join 返回更宽类型。
     */
    @Test
    fun `join signed integers returns wider`() {
        val result = joinMeet.join(listOf(ConePrimitiveType.INT32, ConePrimitiveType.INT64))
        assertEquals(ConePrimitiveType.INT64, result)
    }

    /**
     * 验证 Int8 与 Int32 的 join 返回 Int32。
     */
    @Test
    fun `join int32 and int8 returns int32`() {
        val result = joinMeet.join(listOf(ConePrimitiveType.INT8, ConePrimitiveType.INT32))
        assertEquals(ConePrimitiveType.INT32, result)
    }

    /**
     * 验证 ideal int 与具体整数 join 返回具体整数。
     */
    @Test
    fun `join ideal int with concrete int returns concrete`() {
        val result = joinMeet.join(listOf(ConePrimitiveType.IDEAL_INT, ConePrimitiveType.INT32))
        assertEquals(ConePrimitiveType.INT32, result)
    }

    /**
     * 验证单元素 meet 返回该元素本身。
     */
    @Test
    fun `meet single type returns itself`() {
        assertEquals(ConePrimitiveType.INT32, joinMeet.meet(listOf(ConePrimitiveType.INT32)))
    }

    /**
     * 验证有符号整数 meet 返回更窄类型。
     */
    @Test
    fun `meet signed integers returns narrower`() {
        val result = joinMeet.meet(listOf(ConePrimitiveType.INT32, ConePrimitiveType.INT64))
        assertEquals(ConePrimitiveType.INT32, result)
    }

    /**
     * 验证 ideal int 与具体整数 meet 返回具体整数。
     */
    @Test
    fun `meet ideal int with concrete returns concrete`() {
        val result = joinMeet.meet(listOf(ConePrimitiveType.IDEAL_INT, ConePrimitiveType.INT32))
        assertEquals(ConePrimitiveType.INT32, result)
    }

    /**
     * 验证空 join 没有结果。
     */
    @Test
    fun `join empty list returns null`() {
        assertNull(joinMeet.join(emptyList()))
    }

    /**
     * 验证空 meet 没有结果。
     */
    @Test
    fun `meet empty list returns null`() {
        assertNull(joinMeet.meet(emptyList()))
    }

    /**
     * 验证 meetUpperBounds 能在上界集合中选择最窄非变量上界。
     */
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

/**
 * join/meet 测试使用的最小类型上下文。
 */
private class JoinMeetTestContext : ConeTypeContext {
    /**
     * 测试上下文不提供额外继承关系。
     */
    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> = emptyList()

    /**
     * 按 primitive kind 或 class id 结构判断类型构造器相等。
     */
    override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean {
        if (a is ConePrimitiveType && b is ConePrimitiveType) return a.kind == b.kind
        return a == b
    }
}
