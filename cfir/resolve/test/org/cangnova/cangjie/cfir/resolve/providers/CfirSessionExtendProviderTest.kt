package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CfirSessionExtendProviderTest {
    @Test
    fun `provider does not rebuild index implicitly before extensions phase`() {
        val (session, _) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))

        val store = CfirExtendIndexStore()
        val provider = CfirSessionExtendProvider(session, store)

        assertEquals(emptyList<CfirExtend>(), provider.getExtendsForClass(targetClassId))
    }

    @Test
    fun `provider reads extends for class and package from index store`() {
        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val otherClassId = ClassId(packageFqName, Name.identifier("Other"))

        val targetExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = emptyList(),
        )
        val otherExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(otherClassId),
            superTypeRefs = emptyList(),
        )
        val file = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(targetExtend, otherExtend))

        val store = CfirExtendIndexStore().also { it.rebuild(listOf(file), NoopTypeResolver) }
        val provider = CfirSessionExtendProvider(session, store)

        assertEquals(listOf(targetExtend), provider.getExtendsForClass(targetClassId))
        assertEquals(listOf(targetExtend, otherExtend), provider.getExtendsInPackage(packageFqName))
    }

    @Test
    fun `provider resolves builtin extends by builtin short name`() {
        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("std.core")
        val int64ClassId = ClassId(packageFqName, Name.identifier("Int64"))

        val int64Extend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(int64ClassId),
            superTypeRefs = emptyList(),
        )
        val file = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(int64Extend))

        val store = CfirExtendIndexStore().also { it.rebuild(listOf(file), NoopTypeResolver) }
        val provider = CfirSessionExtendProvider(session, store)

        assertEquals(listOf(int64Extend), provider.getExtendsForBuiltinType(org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64))
    }
}

private object NoopTypeResolver : CfirTypeResolver() {
    override fun resolveType(
        typeRef: org.cangnova.cangjie.cfir.types.CfirTypeRef,
        configuration: org.cangnova.cangjie.cfir.resolve.TypeResolutionConfiguration,
        areBareTypesAllowed: Boolean,
        isOperandOfIsOperator: Boolean,
        resolveDeprecations: Boolean,
        supertypeSupplier: org.cangnova.cangjie.cfir.resolve.SupertypeSupplier,
        expandTypeAliases: Boolean,
    ): org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionResult {
        return org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionResult(
            type = org.cangnova.cangjie.cfir.types.ConeErrorType("NoopTypeResolver"),
            diagnostic = null,
        )
    }

    override fun resolveClass(typeRef: org.cangnova.cangjie.cfir.types.CfirTypeRef): org.cangnova.cangjie.cfir.declarations.CfirClass? = null

    override fun resolveClass(classId: ClassId): org.cangnova.cangjie.cfir.declarations.CfirClass? = null
}
