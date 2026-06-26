package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * [CfirConstraintGraph] 变量依赖拓扑排序和替换传播测试。
 */
class CfirConstraintGraphTest {

    /**
     * 构造带固定 lookup name 的测试类型变量。
     */
    private fun makeVariable(name: String, id: Int = 1): CfirTypeVariable {
        return CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = id,
            lookupTag = ConeTypeParameterLookupTag(name),
        )
    }

    /**
     * 验证互不依赖的变量会在同一批次返回。
     */
    @Test
    fun `independent variables returned in single batch`() {
        val store = CfirConstraintStore()
        val t = makeVariable("T", 1)
        val u = makeVariable("U", 2)
        t.lowerBounds += ConePrimitiveType.INT32
        u.lowerBounds += ConePrimitiveType.BOOLEAN
        store.registerTypeVariable(t)
        store.registerTypeVariable(u)

        val graph = CfirConstraintGraph(listOf(t, u), store)
        val batch = graph.topoOnce()

        assertEquals(2, batch.size)
        assertFalse(graph.hasNext())
    }

    /**
     * 验证依赖其他变量的变量会在依赖变量求解后返回。
     */
    @Test
    fun `dependent variable returned after dependency`() {
        val store = CfirConstraintStore()
        val t = makeVariable("T", 1)
        val u = makeVariable("U", 2)
        // U 的下界引用了 T → U 依赖 T
        u.lowerBounds += ConeTypeParameterType(ConeTypeParameterLookupTag("T"), isPlaceholder = true)
        t.lowerBounds += ConePrimitiveType.INT32
        store.registerTypeVariable(t)
        store.registerTypeVariable(u)

        val graph = CfirConstraintGraph(listOf(t, u), store)

        val batch1 = graph.topoOnce()
        assertEquals(1, batch1.size)
        assertEquals("T", batch1.first().lookupTag.name)

        assertTrue(graph.hasNext())

        // 应用 T 的解
        graph.applySubstitution(mapOf("T" to ConePrimitiveType.INT32))

        val batch2 = graph.topoOnce()
        assertEquals(1, batch2.size)
        assertEquals("U", batch2.first().lookupTag.name)
    }

    /**
     * 验证链式依赖按 T、U、V 的顺序逐批求解。
     */
    @Test
    fun `chain dependency T-U-V solved in order`() {
        val store = CfirConstraintStore()
        val t = makeVariable("T", 1)
        val u = makeVariable("U", 2)
        val v = makeVariable("V", 3)

        t.lowerBounds += ConePrimitiveType.INT32
        u.lowerBounds += ConeTypeParameterType(ConeTypeParameterLookupTag("T"), isPlaceholder = true)
        v.lowerBounds += ConeTypeParameterType(ConeTypeParameterLookupTag("U"), isPlaceholder = true)

        store.registerTypeVariable(t)
        store.registerTypeVariable(u)
        store.registerTypeVariable(v)

        val graph = CfirConstraintGraph(listOf(t, u, v), store)

        val batch1 = graph.topoOnce()
        assertEquals(1, batch1.size)
        assertEquals("T", batch1.first().lookupTag.name)

        graph.applySubstitution(mapOf("T" to ConePrimitiveType.INT32))
        val batch2 = graph.topoOnce()
        assertEquals(1, batch2.size)
        assertEquals("U", batch2.first().lookupTag.name)

        graph.applySubstitution(mapOf("U" to ConePrimitiveType.INT32))
        val batch3 = graph.topoOnce()
        assertEquals(1, batch3.size)
        assertEquals("V", batch3.first().lookupTag.name)

        assertFalse(graph.hasNext())
    }

    /**
     * 验证嵌套类型实参中的类型变量引用可以被收集。
     */
    @Test
    fun `collectVariableNames finds nested references`() {
        val classType = ConeClassLikeType(
            ConeClassLookupTagImpl(
                org.cangnova.cangjie.name.ClassId(
                    org.cangnova.cangjie.name.FqName("test"),
                    org.cangnova.cangjie.name.Name.identifier("Box"),
                ),
            ),
            listOf(ConeTypeParameterType(ConeTypeParameterLookupTag("T"))),
        )

        val vars = classType.collectVariableNames(setOf("T", "U"))
        assertEquals(setOf("T"), vars)
    }

    /**
     * 验证类型变量替换能把 placeholder 替换为具体类型。
     */
    @Test
    fun `substituteVariables replaces type parameter`() {
        val original = ConeTypeParameterType(ConeTypeParameterLookupTag("T"))
        val result = original.substituteVariables(mapOf("T" to ConePrimitiveType.INT32))
        assertEquals(ConePrimitiveType.INT32, result)
    }
}
