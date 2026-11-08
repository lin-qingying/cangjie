package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * `ConeTypeAliasType` 的等值语义。
 *
 * 与 `ConeLookupTagBasedType` 一致：别名身份（classId）、use-site 实参、**属性**共同决定相等；
 * `expandedType` 不参与（官方 `TypeAliasTy` 只按 declPtr + typeArgs 判等，展开是缓存）。
 *
 * 属性必须参与：别名类型会从 `CfirTypeReference`/替换/推断各路径携带 attributes（注解投影等），
 * 若忽略属性，带注解与不带注解的同一别名会被 `typeIndex`/`Set<ConeCangJieType>` 合并。
 */
class ConeTypeAliasTypeEqualityTest {
    private val classId = ClassId(FqName("sample"), Name.identifier("Alias"))

    @Test
    fun `alias types with different attributes are not equal`() {
        val plain = ConeTypeAliasType(classId)
        val annotated = ConeTypeAliasType(
            classId,
            attributes = ConeAttributes.create(listOf(AbbreviatedTypeAttribute(plain))),
        )

        assertNotEquals(plain, annotated, "属性不同的别名类型不能被判等，否则序列化/重入保护会合并它们")
        assertEquals(plain, ConeTypeAliasType(classId), "无属性、同 classId 的别名类型应相等")
        assertEquals(plain.hashCode(), ConeTypeAliasType(classId).hashCode(), "相等对象必须有相同 hashCode")
    }

    @Test
    fun `attributes comparison follows ConeAttributes own equality`() {
        val firstAttrs = ConeAttributes.create(listOf(AbbreviatedTypeAttribute(ConePrimitiveType.INT64)))
        val secondAttrs = ConeAttributes.create(listOf(AbbreviatedTypeAttribute(ConePrimitiveType.INT64)))
        val alias = ConeTypeAliasType(classId)

        assertEquals(
            alias.withAttributesForTest(firstAttrs),
            alias.withAttributesForTest(firstAttrs),
            "同一 attributes 实例上构造的别名类型必须相等且 hash 一致",
        )
        assertEquals(
            alias.withAttributesForTest(firstAttrs).hashCode(),
            alias.withAttributesForTest(firstAttrs).hashCode(),
        )
        assertNotEquals(
            firstAttrs,
            secondAttrs,
            "现状：ConeAttributes 无值等值 override（实例等值），两个等值内容实例不相等",
        )
    }

    private fun ConeTypeAliasType.withAttributesForTest(attributes: ConeAttributes): ConeTypeAliasType =
        ConeTypeAliasType(classId, expandedType, typeArguments, attributes)

    @Test
    fun `expanded type does not participate in alias equality`() {
        val intAlias = ConeTypeAliasType(classId, expandedType = ConePrimitiveType.INT64)
        val floatAlias = ConeTypeAliasType(classId, expandedType = ConePrimitiveType.FLOAT64)

        assertEquals(intAlias, floatAlias, "官方 TypeAliasTy 只按别名身份 + 实参判等，展开类型只是缓存")
    }
}
