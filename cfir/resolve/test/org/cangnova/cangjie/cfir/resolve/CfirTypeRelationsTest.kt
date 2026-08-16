@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.inference.ConstraintSystemTestHarness
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeInferenceContext
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.junit.jupiter.api.Assertions.assertFalse
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