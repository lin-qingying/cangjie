package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CfirExtendIndexStoreTest {
    @Test
    fun `rebuild indexes extends from multiple files`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val interfaceA = ClassId(packageFqName, Name.identifier("IA"))
        val interfaceB = ClassId(packageFqName, Name.identifier("IB"))

        val extend1 = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceA, isInterface = true)),
        )
        val extend2 = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceB, isInterface = true)),
        )
        val file1 = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extend1))
        val file2 = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extend2))

        val store = CfirExtendIndexStore()
        store.rebuild(listOf(file1, file2), NoopTypeResolver)

        assertEquals(2, store.modelsForClass(targetClassId).size)
        assertEquals(listOf(interfaceA), store.modelForDeclaration(extend1)?.inheritedInterfaceClassIds)
        assertEquals(listOf(interfaceB), store.modelForDeclaration(extend2)?.inheritedInterfaceClassIds)
    }

    @Test
    fun `query service excludes current declaration when collecting duplicates`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val interfaceA = ClassId(packageFqName, Name.identifier("IA"))
        val interfaceB = ClassId(packageFqName, Name.identifier("IB"))

        val extend1 = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceA, isInterface = true)),
        )
        val extend2 = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceB, isInterface = true)),
        )
        val file1 = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extend1))
        val file2 = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extend2))

        val store = CfirExtendIndexStore()
        store.rebuild(listOf(file1, file2), NoopTypeResolver)

        val query = CfirExtendRuleQueryServiceImpl(store)
        assertEquals(targetClassId, query.targetClassIdOf(extend1))
        assertNull(query.targetClassIdOf(Any()))
        assertEquals(
            listOf(interfaceB),
            query.inheritedInterfaceClassIdsForTarget(targetClassId, excludingDeclaration = extend1),
        )
        assertEquals(
            listOf(interfaceA, interfaceB),
            query.inheritedInterfaceClassIdsForTarget(targetClassId),
        )
        assertEquals(1, query.inheritedInterfacesOf(extend1).size)
        assertEquals(interfaceA, query.inheritedInterfacesOf(extend1).single().classId)
    }

    @Test
    fun `semantic keys normalize extend type parameter names`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val genericInterfaceId = ClassId(packageFqName, Name.identifier("IGeneric"))

        val extendT = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(ExtendTestFixtures.newTypeParameter(moduleData, "T")),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = genericInterfaceId,
                    typeArguments = listOf(ExtendTestFixtures.typeParameterType("T")),
                    isInterface = true,
                ),
            ),
        )
        val extendU = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(ExtendTestFixtures.newTypeParameter(moduleData, "U")),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = genericInterfaceId,
                    typeArguments = listOf(ExtendTestFixtures.typeParameterType("U")),
                    isInterface = true,
                ),
            ),
        )
        val file1 = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extendT))
        val file2 = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extendU))

        val store = CfirExtendIndexStore()
        store.rebuild(listOf(file1, file2), NoopTypeResolver)

        val query = CfirExtendRuleQueryServiceImpl(store)
        assertEquals(
            query.inheritedInterfaceSemanticKeysOf(extendT),
            query.inheritedInterfaceSemanticKeysOf(extendU),
        )
        assertEquals(
            query.inheritedInterfacesOf(extendT).single().semanticKey,
            query.inheritedInterfacesOf(extendU).single().semanticKey,
        )
    }

    @Test
    fun `models for same target are returned in stable order regardless of input file order`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val interfaceA = ClassId(packageFqName, Name.identifier("IA"))
        val interfaceB = ClassId(packageFqName, Name.identifier("IB"))

        val extendB = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceB, isInterface = true)),
        )
        val extendA = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceA, isInterface = true)),
        )
        val fileB = ExtendTestFixtures.newFile(
            moduleData = moduleData,
            packageFqName = packageFqName,
            declarations = listOf(extendB),
            fileName = "z_extend_b.cj",
        )
        val fileA = ExtendTestFixtures.newFile(
            moduleData = moduleData,
            packageFqName = packageFqName,
            declarations = listOf(extendA),
            fileName = "a_extend_a.cj",
        )

        val store = CfirExtendIndexStore()
        store.rebuild(listOf(fileB, fileA), NoopTypeResolver)

        val query = CfirExtendRuleQueryServiceImpl(store)
        val ordered = query.inheritedInterfaceClassIdsForTarget(targetClassId)
        assertEquals(listOf(interfaceA, interfaceB), ordered)
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

