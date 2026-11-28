@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.inference.ConstraintSystemTestHarness
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeInferenceContext
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 类型关系分类测试。
 *
 * 旧独立 [CfirTypeRelations] 已随 K2 架构移植删除，subtype/等值判定由
 * [AbstractTypeChecker] 承担；父类型关系通过覆盖
 * [org.cangnova.cangjie.type.model.TypeSystemContext.supertypes] 扩展提供。
 */
class CfirTypeRelationsTest {

    /**
     * 测试使用的 session。
     */
    private val session = ConstraintSystemTestHarness.newSession()

    /**
     * 测试使用的类型上下文，为 Child 提供 Parent 作为父类型。
     */
    private val context: ConeInferenceContext = object : ConeInferenceContext {
        /**
         * 测试上下文绑定真实测试 session。
         */
        override val session: CfirSession
            get() = this@CfirTypeRelationsTest.session

        /**
         * 按 primitive kind 或 class id 判断类型构造器是否相同。
         */
        override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean {
            if (a is ConePrimitiveType && b is ConePrimitiveType) return a.kind == b.kind
            if (a is ConeClassLikeType && b is ConeClassLikeType) return a.classId == b.classId
            return a == b
        }

        /**
         * 为 Child 提供 Parent 作为直接父类型。
         */
        override fun TypeConstructorMarker.supertypes(): Collection<CangJieTypeMarker> {
            if (this == TYPE_CHILD.typeConstructor()) {
                return listOf(TYPE_PARENT)
            }
            return emptyList()
        }
    }

    /**
     * 验证同一 primitive 类型判等。
     */
    @Test
    fun `same primitive type is identical`() {
        assertTrue(AbstractTypeChecker.equalTypes(context, ConePrimitiveType.INT32, ConePrimitiveType.INT32))
    }

    /**
     * 验证 Child 类型是 Parent 子类型。
     */
    @Test
    fun `child type is subtype of parent`() {
        assertTrue(AbstractTypeChecker.isSubtypeOf(context, TYPE_CHILD, TYPE_PARENT))
    }

    /**
     * 验证无关 primitive 类型互不兼容。
     */
    @Test
    fun `unrelated primitive types are incompatible`() {
        assertFalse(AbstractTypeChecker.isSubtypeOf(context, ConePrimitiveType.BOOLEAN, ConePrimitiveType.INT32))
        assertFalse(AbstractTypeChecker.isSubtypeOf(context, ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN))
    }

    /** native IsArraySubtype 同时比较维度与不变叶子，不能因相同 classifier/实参走捷径。 */
    @Test
    fun `raw array dimensions and leaf types are invariant`() {
        val one = rawArray(ConePrimitiveType.INT64)
        val two = rawArray(ConePrimitiveType.INT64, 2u)
        assertFalse(AbstractTypeChecker.equalTypes(context, one, two))
        assertFalse(AbstractTypeChecker.isSubtypeOf(context, one, two))
        assertFalse(AbstractTypeChecker.isSubtypeOf(context, two, one))
        assertFalse(AbstractTypeChecker.isSubtypeOf(context, rawArray(TYPE_CHILD), rawArray(TYPE_PARENT)))
    }

    @Test
    fun `nested source and flat loaded raw arrays share native type semantics`() {
        val nested = rawArray(rawArray(ConePrimitiveType.INT64))
        val loaded = rawArray(ConePrimitiveType.INT64, 2u)
        assertTrue(AbstractTypeChecker.equalTypes(context, nested, loaded))
        with(context) {
            assertEquals(1, nested.argumentsCount())
            assertSame(ConePrimitiveType.INT64, nested.getArgument(0))
            val replaced = nested.replaceArguments(listOf(ConePrimitiveType.BOOLEAN)) as ConeClassLikeType
            assertEquals(2u, requireNotNull(replaced.rawArrayLayout).dimensions)
            assertSame(ConePrimitiveType.BOOLEAN, requireNotNull(replaced.rawArrayLayout).element)
        }
    }

    /** native GetTypeArgs 与 Ty.typeArgs 分别提供降一维的声明视图和规范叶子。 */
    @Test
    fun `raw array declaration argument view is stable and dimension aware`() {
        val array = rawArray(ConePrimitiveType.INT64, 3u)
        val argument = array.rawArrayTypeArguments.single() as ConeClassLikeType
        assertSame(argument, array.rawArrayTypeArguments.single())
        assertEquals(2u, requireNotNull(argument.rawArrayLayout).dimensions)
        assertSame(ConePrimitiveType.INT64, requireNotNull(argument.rawArrayLayout).element)
        assertSame(ConePrimitiveType.INT64, rawArray(ConePrimitiveType.INT64).rawArrayTypeArguments.single())
    }

    @Test
    fun `raw array marker overloads and deep replacement use one leaf domain`() {
        val nested = rawArray(rawArray(ConePrimitiveType.INT64))
        val flat = rawArray(ConePrimitiveType.INT64, 2u)
        with(context) {
            for (input in listOf(nested, flat)) {
                val identity = input.replaceArguments { it } as ConeClassLikeType
                val deepIdentity = input.replaceArgumentsDeeply { it } as ConeClassLikeType
                assertEquals(2u, requireNotNull(identity.rawArrayLayout).dimensions)
                assertEquals(2u, requireNotNull(deepIdentity.rawArrayLayout).dimensions)
                assertTrue(AbstractTypeChecker.equalTypes(context, input, identity))
                val changed = input.replaceArgumentsDeeply { ConePrimitiveType.BOOLEAN } as ConeClassLikeType
                assertEquals(2u, requireNotNull(changed.rawArrayLayout).dimensions)
                assertSame(ConePrimitiveType.BOOLEAN, requireNotNull(changed.rawArrayLayout).element)
            }
        }
    }

    private fun rawArray(element: ConeCangJieType, dimensions: UInt = 1u): ConeClassLikeType =
        ConeClassLikeType(ConeClassLikeLookupTagImpl(StdlibClassIds.RawArray), listOf(element), rawArrayDimensions = dimensions)
}

/**
 * 测试中的父类类型。
 */
private val TYPE_PARENT =
    ConeClassLikeType(ConeClassLikeLookupTagImpl(ClassId(FqName("test"), Name.identifier("Parent"))))

/**
 * 测试中的子类类型。
 */
private val TYPE_CHILD =
    ConeClassLikeType(ConeClassLikeLookupTagImpl(ClassId(FqName("test"), Name.identifier("Child"))))
