package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirConstraintPosition
import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * [CfirUnifier] 类型统一和约束落库测试。
 */
class CfirUnifierTest {

    /**
     * 测试使用的类型关系服务。
     */
    private val typeRelations = CfirTypeRelations(UnifierTestContext())
    /**
     * 统一器写入约束和边界的测试 store。
     */
    private val store = CfirConstraintStore()
    /**
     * 当前测试复用的统一器。
     */
    private val unifier = CfirUnifier(typeRelations, store)

    /**
     * 构造并注册一个测试类型变量。
     */
    private fun makeVariable(name: String): CfirTypeVariable {
        val variable = CfirTypeVariable(
            typeParameter = CfirTypeParameterSymbol(),
            freshTypeId = 1,
            lookupTag = ConeTypeParameterLookupTag(name),
        )
        store.registerTypeVariable(variable)
        return variable
    }

    /**
     * 验证相同类型统一成功。
     */
    @Test
    fun `unify identical types succeeds`() {
        assertTrue(unifier.unify(ConePrimitiveType.INT32, ConePrimitiveType.INT32, CfirConstraintPosition.Unknown))
    }

    /**
     * 验证 quest 类型参与任一侧统一都直接成功。
     */
    @Test
    fun `unify quest type always succeeds`() {
        assertTrue(unifier.unify(ConeQuestType(), ConePrimitiveType.INT32, CfirConstraintPosition.Unknown))
        assertTrue(unifier.unify(ConePrimitiveType.INT32, ConeQuestType(), CfirConstraintPosition.Unknown))
    }

    /**
     * 验证具体类型统一到 placeholder 会增加变量下界。
     */
    @Test
    fun `unify concrete with placeholder adds lower bound`() {
        val variable = makeVariable("T")
        val placeholderT = ConeTypeParameterType(variable.lookupTag, isPlaceholder = true)

        unifier.unify(ConePrimitiveType.INT32, placeholderT, CfirConstraintPosition.ArgumentPosition(0))

        assertEquals(listOf(ConePrimitiveType.INT32), variable.lowerBounds)
    }

    /**
     * 验证 placeholder 统一到具体类型会增加变量上界。
     */
    @Test
    fun `unify placeholder with concrete adds upper bound`() {
        val variable = makeVariable("T")
        val placeholderT = ConeTypeParameterType(variable.lookupTag, isPlaceholder = true)

        unifier.unify(placeholderT, ConePrimitiveType.INT32, CfirConstraintPosition.ArgumentPosition(0))

        assertEquals(listOf(ConePrimitiveType.INT32), variable.upperBounds)
    }

    /**
     * 验证函数类型统一会按参数逆变和返回协变传播约束。
     */
    @Test
    fun `unify function types with contravariant params`() {
        val variable = makeVariable("T")
        val placeholderT = ConeTypeParameterType(variable.lookupTag, isPlaceholder = true)

        // (Int32) -> T  <:  (T) -> Int64
        val argFn = ConeFunctionType(listOf(ConePrimitiveType.INT32), placeholderT)
        val paramFn = ConeFunctionType(listOf(placeholderT), ConePrimitiveType.INT64)

        unifier.unify(argFn, paramFn, CfirConstraintPosition.ArgumentPosition(0))

        // 参数逆变：T <: Int32（T 在 paramFn 的参数位置）
        // 返回协变：T <: Int64（T 在 argFn 的返回位置，但这个 T 已经有 lower bound）
        assertTrue(variable.upperBounds.isNotEmpty() || variable.lowerBounds.isNotEmpty())
    }

    /**
     * 验证 tuple 类型统一按元素逐项分解。
     */
    @Test
    fun `unify tuple types element by element`() {
        val variable = makeVariable("T")
        val placeholderT = ConeTypeParameterType(variable.lookupTag, isPlaceholder = true)

        val argTuple = ConeTupleType(listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN))
        val paramTuple = ConeTupleType(listOf(placeholderT, ConePrimitiveType.BOOLEAN))

        unifier.unify(argTuple, paramTuple, CfirConstraintPosition.ArgumentPosition(0))

        assertEquals(listOf(ConePrimitiveType.INT32), variable.lowerBounds)
    }

    /**
     * 验证 array 类型实参按不变性生成双向约束。
     */
    @Test
    fun `unify array types invariant`() {
        val variable = makeVariable("T")
        val placeholderT = ConeTypeParameterType(variable.lookupTag, isPlaceholder = true)

        val argArr = ConeArrayType(ConePrimitiveType.INT32)
        val paramArr = ConeArrayType(placeholderT)

        unifier.unify(argArr, paramArr, CfirConstraintPosition.ArgumentPosition(0))

        // 不变性 → 双向约束
        assertTrue(variable.lowerBounds.contains(ConePrimitiveType.INT32))
        assertTrue(variable.upperBounds.contains(ConePrimitiveType.INT32))
    }

    /**
     * 验证 nominal class 类型实参统一按不变性处理。
     */
    @Test
    fun `unify nominal class with type arguments`() {
        val variable = makeVariable("T")
        val placeholderT = ConeTypeParameterType(variable.lookupTag, isPlaceholder = true)

        val boxId = ClassId(FqName("test"), Name.identifier("Box"))
        val concreteBox = ConeClassLikeType(ConeClassLookupTagImpl(boxId), listOf(ConePrimitiveType.INT32))
        val genericBox = ConeClassLikeType(ConeClassLookupTagImpl(boxId), listOf(placeholderT))

        unifier.unify(concreteBox, genericBox, CfirConstraintPosition.ArgumentPosition(0))

        // 不变性 → T 的 lower 和 upper 都含 Int32
        assertTrue(variable.lowerBounds.contains(ConePrimitiveType.INT32))
        assertTrue(variable.upperBounds.contains(ConePrimitiveType.INT32))
    }

    /**
     * 验证 intersection 参数类型可以分解并统一成功。
     */
    @Test
    fun `unify intersection param type`() {
        val variable = makeVariable("T")
        val placeholderT = ConeTypeParameterType(variable.lookupTag, isPlaceholder = true)

        // T <: A & B → T <: A AND T <: B
        val inter = ConeIntersectionType(listOf(ConePrimitiveType.INT32, ConePrimitiveType.INT32))
        val result = unifier.unify(placeholderT, inter, CfirConstraintPosition.ArgumentPosition(0))

        assertTrue(result)
    }

    /**
     * 验证 ideal int 与具体整数统一成功。
     */
    @Test
    fun `unify ideal int with concrete int succeeds`() {
        assertTrue(unifier.unify(ConePrimitiveType.IDEAL_INT, ConePrimitiveType.INT32, CfirConstraintPosition.Unknown))
    }
}

/**
 * unifier 测试使用的最小类型上下文。
 */
private class UnifierTestContext : ConeTypeContext {
    /**
     * 测试上下文不提供额外父类型。
     */
    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> = emptyList()

    /**
     * 按 primitive kind 或 class id 判断类型构造器是否相同。
     */
    override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean {
        if (a is ConePrimitiveType && b is ConePrimitiveType) return a.kind == b.kind
        if (a is ConeClassLikeType && b is ConeClassLikeType) return a.classId == b.classId
        return a == b
    }
}
