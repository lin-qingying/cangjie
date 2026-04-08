package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CfirExtensionsResolveProcessorTest {
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
