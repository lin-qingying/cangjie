package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CfirExtensionsResolveProcessorTest {
    @Test
    fun `beforePhase builds extend index from all provider files`() {
        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
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

        val provider = CfirProviderImpl(session).apply {
            recordFile(file1)
            recordFile(file2)
        }
        val indexStore = CfirExtendIndexStore()
        session.register(CfirProvider::class, provider)
        session.register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
        session.register(CfirTypeResolver::class, NoopTypeResolver)
        session.register(CfirExtendIndexStore::class, indexStore)

        val processor = CfirExtensionsResolveProcessor(session, ScopeSession())
        processor.beforePhase()
        processor.afterPhase()

        assertEquals(2, indexStore.modelsForClass(targetClassId).size)
    }

    @Test
    fun `transformDeclaration advances STATUS declarations to EXTENSIONS`() {
        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val targetClassId = ClassId(FqName("sample.pkg"), Name.identifier("Target"))
        val interfaceA = ClassId(FqName("sample.pkg"), Name.identifier("IA"))

        val shouldAdvance = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceA, isInterface = true)),
        ).also {
            it.replaceResolvePhase(CfirResolvePhase.STATUS)
        }
        val shouldStayRaw = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceA, isInterface = true)),
        )

        val transformer = CfirExtensionsResolveTransformer(session)
        transformer.transformDeclaration(shouldAdvance, null)
        transformer.transformDeclaration(shouldStayRaw, null)

        assertEquals(CfirResolvePhase.EXTENSIONS, shouldAdvance.resolvePhase)
        assertEquals(CfirResolvePhase.RAW_CFIR, shouldStayRaw.resolvePhase)
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
