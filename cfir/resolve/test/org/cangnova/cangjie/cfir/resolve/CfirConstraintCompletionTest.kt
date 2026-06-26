package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirConstraintIssue
import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeContext
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [CfirConstraintCompletion] 变量固定和边界合并测试。
 */
class CfirConstraintCompletionTest {

    /**
     * 测试使用的 completion 实例。
     */
    private val completion = CfirConstraintCompletion(CfirTypeRelations(CompletionTypeContext()))

    /**
     * 验证只有下界时变量会固定为该下界。
     */
    @Test
    fun `completion fixes variable from lower bounds`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 1,
            lookupTag = ConeTypeParameterLookupTag("T"),
            lowerBounds = mutableListOf(ConePrimitiveType.INT32),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        assertEquals(ConePrimitiveType.INT32, variable.fixedType)
        assertTrue(store.issues.isEmpty())
    }

    /**
     * 验证下界满足上界时优先采用下界作为固定类型。
     */
    @Test
    fun `completion prefers lower bound when it fits upper bound`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 2,
            lookupTag = ConeTypeParameterLookupTag("U"),
            lowerBounds = mutableListOf(ConePrimitiveType.INT32),
            upperBounds = mutableListOf(ConePrimitiveType.INT32),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        assertEquals(ConePrimitiveType.INT32, variable.fixedType)
    }

    /**
     * 验证冲突边界会先报告 issue 并阻止变量固定。
     */
    @Test
    fun `completion reports conflict before fixing`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 3,
            lookupTag = ConeTypeParameterLookupTag("V"),
            lowerBounds = mutableListOf(ConePrimitiveType.INT32),
            upperBounds = mutableListOf(ConePrimitiveType.BOOLEAN),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        assertTrue(store.issues.any { it is CfirConstraintIssue.ConflictingBounds })
        assertNull(variable.fixedType)
    }

    /**
     * 验证 ideal int 会终结为 Int64。
     */
    @Test
    fun `completion finalizes ideal int to Int64`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 4,
            lookupTag = ConeTypeParameterLookupTag("T"),
            lowerBounds = mutableListOf(ConePrimitiveType.IDEAL_INT),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        assertEquals(ConePrimitiveType.INT64, variable.fixedType)
    }

    /**
     * 验证 ideal float 会终结为 Float64。
     */
    @Test
    fun `completion finalizes ideal float to Float64`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 5,
            lookupTag = ConeTypeParameterLookupTag("T"),
            lowerBounds = mutableListOf(ConePrimitiveType.IDEAL_FLOAT),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        assertEquals(ConePrimitiveType.FLOAT64, variable.fixedType)
    }

    /**
     * 验证多个下界通过 join 合并后固定变量。
     */
    @Test
    fun `completion joins multiple lower bounds`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 6,
            lookupTag = ConeTypeParameterLookupTag("T"),
            lowerBounds = mutableListOf(ConePrimitiveType.INT32, ConePrimitiveType.INT64),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        // join(Int32, Int64) = Int64
        assertEquals(ConePrimitiveType.INT64, variable.fixedType)
    }

    /**
     * 验证多个上界通过 meet 合并后固定变量。
     */
    @Test
    fun `completion meets multiple upper bounds`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 7,
            lookupTag = ConeTypeParameterLookupTag("T"),
            upperBounds = mutableListOf(ConePrimitiveType.INT32, ConePrimitiveType.INT64),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        // meet(Int32, Int64) = Int32
        assertEquals(ConePrimitiveType.INT32, variable.fixedType)
    }

    /**
     * 验证 ideal 下界和具体上界共存时采用非 ideal 具体类型。
     */
    @Test
    fun `completion prefers non-ideal join over ideal`() {
        val store = CfirConstraintStore()
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 8,
            lookupTag = ConeTypeParameterLookupTag("T"),
            lowerBounds = mutableListOf(ConePrimitiveType.IDEAL_INT),
            upperBounds = mutableListOf(ConePrimitiveType.INT32),
        )
        store.registerTypeVariable(variable)

        completion.complete(store)

        // ideal int lower + Int32 upper → meet gives Int32
        assertEquals(ConePrimitiveType.INT32, variable.fixedType)
    }
}

/**
 * completion 测试使用的最小类型上下文。
 */
private class CompletionTypeContext : ConeTypeContext {
    /**
     * 测试上下文不提供额外继承关系。
     */
    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> = emptyList()

    /**
     * 使用结构相等判断类型构造器是否相同。
     */
    override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean = a == b
}
