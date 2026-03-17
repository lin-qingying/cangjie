@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.cfir.types.impl.CfirUserTypeRefImpl
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CfirTypeRefExtensionsTest {
    @Test
    fun `renderStableKey ignores typealias expandedType details`() {
        val aliasClassId = ClassId(FqName("sample.pkg"), Name.identifier("Alias"))
        val typeWithIntExpanded = CfirResolvedTypeRefImpl(
            source = null,
            annotations = emptyList(),
            coneType = ConeTypeAliasType(aliasClassId, expandedType = ConePrimitiveType.INT32),
            delegatedTypeRef = null,
        )
        val typeWithFloatExpanded = CfirResolvedTypeRefImpl(
            source = null,
            annotations = emptyList(),
            coneType = ConeTypeAliasType(aliasClassId, expandedType = ConePrimitiveType.FLOAT64),
            delegatedTypeRef = null,
        )

        assertEquals(typeWithIntExpanded.renderStableKey(), typeWithFloatExpanded.renderStableKey())
    }

    @Test
    fun `renderStableKey normalizes userType arguments semantically`() {
        val aliasClassId = ClassId(FqName("sample.pkg"), Name.identifier("Alias"))
        val argRef1 = CfirResolvedTypeRefImpl(
            source = null,
            annotations = emptyList(),
            coneType = ConeTypeAliasType(aliasClassId, expandedType = ConePrimitiveType.INT32),
            delegatedTypeRef = null,
        )
        val argRef2 = CfirResolvedTypeRefImpl(
            source = null,
            annotations = emptyList(),
            coneType = ConeTypeAliasType(aliasClassId, expandedType = ConePrimitiveType.FLOAT64),
            delegatedTypeRef = null,
        )

        val userTypeRef1 = CfirUserTypeRefImpl(
            source = null,
            annotations = emptyList(),
            qualifier = listOf(Name.identifier("pkg"), Name.identifier("Box")),
            typeArguments = listOf(argRef1),
        )
        val userTypeRef2 = CfirUserTypeRefImpl(
            source = null,
            annotations = emptyList(),
            qualifier = listOf(Name.identifier("pkg"), Name.identifier("Box")),
            typeArguments = listOf(argRef2),
        )

        assertEquals(userTypeRef1.renderStableKey(), userTypeRef2.renderStableKey())
    }

    @Test
    fun `renderStableKey of union type is deterministic regardless set order`() {
        val unionA = CfirResolvedTypeRefImpl(
            source = null,
            annotations = emptyList(),
            coneType = ConeUnionType(linkedSetOf(ConePrimitiveType.INT32, ConePrimitiveType.FLOAT64)),
            delegatedTypeRef = null,
        )
        val unionB = CfirResolvedTypeRefImpl(
            source = null,
            annotations = emptyList(),
            coneType = ConeUnionType(linkedSetOf(ConePrimitiveType.FLOAT64, ConePrimitiveType.INT32)),
            delegatedTypeRef = null,
        )

        assertEquals(unionA.renderStableKey(), unionB.renderStableKey())
    }
}

