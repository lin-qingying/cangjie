@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.buildQualifierPart
import org.cangnova.cangjie.cfir.render.ConeTypeRendererForDebugInfo
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.cfir.types.impl.CfirUserTypeRefImpl
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CFIR type reference 稳定 key 渲染测试。
 *
 * 稳定 key 委托 [ConeTypeRendererForDebugInfo] 渲染 Cone 类型，因此
 * typealias 的 expandedType、userType 的类型实参和 union 成员顺序都会
 * 体现在 key 中；这些测试锁定该渲染行为，防止后续回归。
 */
class CfirTypeRefExtensionsTest {
    /**
     * 构造已解析的 type ref。
     */
    private fun resolvedTypeRef(coneType: ConeCangJieType): CfirResolvedTypeRefImpl =
        CfirResolvedTypeRefImpl(
            source = null,
            annotations = MutableOrEmptyList.empty(),
            customRenderer = false,
            coneType = coneType,
            delegatedTypeRef = null,
        )

    /**
     * 验证 typealias 的 expandedType 会体现在稳定 key 中。
     */
    @Test
    fun `renderStableKey distinguishes typealias expandedType details`() {
        val aliasClassId = ClassId(FqName("sample.pkg"), Name.identifier("Alias"))
        val typeWithIntExpanded = resolvedTypeRef(
            ConeTypeAliasType(aliasClassId, expandedType = ConePrimitiveType.INT32),
        )
        val typeWithFloatExpanded = resolvedTypeRef(
            ConeTypeAliasType(aliasClassId, expandedType = ConePrimitiveType.FLOAT64),
        )

        val intKey = typeWithIntExpanded.renderStableKey()
        val floatKey = typeWithFloatExpanded.renderStableKey()

        assertNotEquals(intKey, floatKey)
        assertTrue(intKey.contains("Int32"))
        assertTrue(floatKey.contains("Float64"))
    }

    /**
     * 验证 userType 的类型实参按渲染区分。
     */
    @Test
    fun `renderStableKey distinguishes userType type arguments`() {
        val aliasClassId = ClassId(FqName("sample.pkg"), Name.identifier("Alias"))
        val argRef1 = resolvedTypeRef(
            ConeTypeAliasType(aliasClassId, expandedType = ConePrimitiveType.INT32),
        )
        val argRef2 = resolvedTypeRef(
            ConeTypeAliasType(aliasClassId, expandedType = ConePrimitiveType.FLOAT64),
        )

        val userTypeRef1 = CfirUserTypeRefImpl(
            annotations = MutableOrEmptyList.empty(),
            customRenderer = false,
            source = TestBinarySourceElement("sample.pkg.Box"),
qualifier = listOf(
                buildQualifierPart {
                    name = Name.identifier("pkg")
                },
                buildQualifierPart {
                    name = Name.identifier("Box")
                    typeArguments += argRef1
                },
            ).toMutableOrEmpty(),
        )
        val userTypeRef2 = CfirUserTypeRefImpl(
            annotations = MutableOrEmptyList.empty(),
            customRenderer = false,
            source = TestBinarySourceElement("sample.pkg.Box"),
            qualifier = listOf(
                buildQualifierPart {
                    name = Name.identifier("pkg")
                },
                buildQualifierPart {
                    name = Name.identifier("Box")
                    typeArguments += argRef2
                },
            ).toMutableOrEmpty(),
        )

        assertNotEquals(userTypeRef1.renderStableKey(), userTypeRef2.renderStableKey())
    }

    /**
     * 验证 union type 的稳定 key 保留成员集合顺序。
     */
    @Test
    fun `renderStableKey preserves union type member order`() {
        val unionA = resolvedTypeRef(
            ConeUnionType(linkedSetOf(ConePrimitiveType.INT32, ConePrimitiveType.FLOAT64)),
        )
        val unionB = resolvedTypeRef(
            ConeUnionType(linkedSetOf(ConePrimitiveType.FLOAT64, ConePrimitiveType.INT32)),
        )

        assertNotEquals(unionA.renderStableKey(), unionB.renderStableKey())
    }

    /**
     * 带稳定 debug identity 的二进制 source element。
     */
    private class TestBinarySourceElement(identity: String) : CjBinarySourceElement(
        debugText = identity,
        binaryFilePath = null,
        stableIdentity = identity,
    )
}
