package org.cangjie.cfir.resolve

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.declarations.CfirClass
import org.cangjie.cfir.declarations.CfirClassKind
import org.cangjie.cfir.declarations.CfirDeclaration
import org.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangjie.cfir.declarations.CfirExtend
import org.cangjie.cfir.declarations.CfirResolvePhase
import org.cangjie.cfir.resolve.providers.CfirEmptyExtendProvider
import org.cangjie.cfir.resolve.providers.CfirEmptySymbolProvider
import org.cangjie.cfir.resolve.providers.CfirProvider
import org.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangjie.cfir.resolve.services.CfirSuperTypeGraphStore
import org.cangjie.cfir.resolve.services.CfirLazyDeclarationResolver
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.session.superTypeGraphStore
import org.cangjie.cfir.types.CfirUserTypeRef
import org.cangjie.cfir.types.CfirBasicTypeRef
import org.cangjie.cfir.types.CfirTypeRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangjie.cfir.declarations.CfirFile

class CfirSuperExtensionsResolveProcessorTest {
    @Test
    fun cangjieExtendWithoutInterfacesIsAccepted() {
        val context = createContext(
            providedClasses = listOf(
                classDecl("Vec", CfirClassKind.CLASS),
            ),
        )
        val declaration = CfirExtend(
            moduleData = context.moduleData,
            extendedTypeRef = userTypeRef("Vec"),
            superTypeRefs = mutableListOf(),
        )

        processToCheckers(context, declaration)

        assertEquals(CfirResolvePhase.CHECKERS, declaration.resolvePhase)
        assertTrue(context.diagnostics.diagnostics.isEmpty())
    }

    @Test
    fun interfaceCannotInheritClassWhenResolved() {
        val context = createContext(
            providedClasses = listOf(
                classDecl("BaseClass", CfirClassKind.CLASS),
            ),
        )
        val declaration = CfirClass(
            moduleData = context.moduleData,
            name = Name.identifier("IMyApi"),
            classKind = CfirClassKind.INTERFACE,
            superTypeRefs = mutableListOf(userTypeRef("BaseClass")),
        )

        processToCheckers(context, declaration)

        assertEquals(CfirResolvePhase.CHECKERS, declaration.resolvePhase)
        assertTrue(context.diagnostics.diagnostics.any { it.factoryName == "CFIR_INTERFACE_CANNOT_INHERIT_CLASS" })
    }

    @Test
    fun extendSuperTypeMustBeInterfaceWhenResolved() {
        val context = createContext(
            providedClasses = listOf(
                classDecl("Vec", CfirClassKind.CLASS),
                classDecl("BaseClass", CfirClassKind.CLASS),
            ),
        )
        val declaration = CfirExtend(
            moduleData = context.moduleData,
            extendedTypeRef = userTypeRef("Vec"),
            superTypeRefs = mutableListOf(userTypeRef("BaseClass")),
        )

        processToCheckers(context, declaration)

        assertEquals(CfirResolvePhase.CHECKERS, declaration.resolvePhase)
        assertTrue(context.diagnostics.diagnostics.any { it.factoryName == "CFIR_EXTEND_NOT_INTERFACE" })
    }

    @Test
    fun extendTargetCannotBeInterfaceWhenResolved() {
        val context = createContext(
            providedClasses = listOf(
                classDecl("IService", CfirClassKind.INTERFACE),
                classDecl("IWorker", CfirClassKind.INTERFACE),
            ),
        )
        val declaration = CfirExtend(
            moduleData = context.moduleData,
            extendedTypeRef = userTypeRef("IService"),
            superTypeRefs = mutableListOf(userTypeRef("IWorker")),
        )

        processToCheckers(context, declaration)

        assertEquals(CfirResolvePhase.CHECKERS, declaration.resolvePhase)
        assertTrue(context.diagnostics.diagnostics.any { it.factoryName == "CFIR_ILLEGAL_EXTENDED_TYPE" })
    }

    @Test
    fun superTypeGraphIsRecordedDuringSuperTypesPhase() {
        val context = createContext(
            providedClasses = listOf(
                classDecl("BaseClass", CfirClassKind.CLASS),
            ),
        )
        val declaration = CfirClass(
            moduleData = context.moduleData,
            name = Name.identifier("DerivedClass"),
            classKind = CfirClassKind.CLASS,
            superTypeRefs = mutableListOf(userTypeRef("BaseClass")),
        )

        CfirTotalResolveProcessor(context.session, context.phaseRegistry)
            .processToPhase(declaration, CfirResolvePhase.SUPER_TYPES)

        val graphNode = context.session.superTypeGraphStore.getNode(declaration)
        assertEquals(CfirResolvePhase.SUPER_TYPES, declaration.resolvePhase)
        assertTrue(graphNode != null)
        assertEquals(1, graphNode!!.superTypes.size)
        assertEquals("BaseClass", graphNode.superTypes.single().renderedType)
        assertTrue(graphNode.superTypes.single().resolvedClassSymbol != null)
    }

    @Test
    fun duplicateInterfaceIsDetectedAfterTypeInstantiation() {
        val context = createContext(
            providedClasses = listOf(
                classDecl("IBase", CfirClassKind.INTERFACE),
            ),
        )
        val declaration = CfirClass(
            moduleData = context.moduleData,
            name = Name.identifier("Derived"),
            classKind = CfirClassKind.CLASS,
            superTypeRefs = mutableListOf(
                userTypeRefWithArguments("IBase", "Int64"),
                userTypeRefWithArguments("IBase", "Bool"),
            ),
        )

        processToCheckers(context, declaration)

        assertEquals(CfirResolvePhase.CHECKERS, declaration.resolvePhase)
        assertTrue(context.diagnostics.diagnostics.any { it.factoryName == "CFIR_SUPER_TYPES_DUPLICATE" })
    }

    @Test
    fun superTypesErrorInOneDeclarationDoesNotBlockOtherDeclarations() {
        val context = createContext(
            providedClasses = listOf(
                classDecl("BaseClass", CfirClassKind.CLASS),
                classDecl("Iface", CfirClassKind.INTERFACE),
            ),
        )
        val invalidInterface = CfirClass(
            moduleData = context.moduleData,
            name = Name.identifier("InvalidIface"),
            classKind = CfirClassKind.INTERFACE,
            superTypeRefs = mutableListOf(userTypeRef("BaseClass")),
        )
        val validClass = CfirClass(
            moduleData = context.moduleData,
            name = Name.identifier("ValidClass"),
            classKind = CfirClassKind.CLASS,
            superTypeRefs = mutableListOf(userTypeRef("Iface")),
        )
        val file = CfirFile(
            moduleData = context.moduleData,
            name = "super_types_recovery_test.cj",
            packageDirective = org.cangjie.cfir.declarations.CfirPackageDirective(FqName("demo.recovery")),
            declarations = mutableListOf(invalidInterface, validClass),
        )

        CfirTotalResolveProcessor(context.session, context.phaseRegistry).processFile(file)

        assertEquals(CfirResolvePhase.CHECKERS, invalidInterface.resolvePhase)
        assertEquals(CfirResolvePhase.CHECKERS, validClass.resolvePhase)
        assertTrue(context.diagnostics.diagnostics.any { it.factoryName == "CFIR_INTERFACE_CANNOT_INHERIT_CLASS" })
    }

    @Test
    fun superTypesMatrixCoversPositiveAndNegativeCases() {
        val positiveContext = createContext(
            providedClasses = listOf(
                classDecl("BaseClass", CfirClassKind.CLASS),
                classDecl("Iface", CfirClassKind.INTERFACE),
            ),
        )
        val validClass = CfirClass(
            moduleData = positiveContext.moduleData,
            name = Name.identifier("ValidClass"),
            classKind = CfirClassKind.CLASS,
            superTypeRefs = mutableListOf(userTypeRef("BaseClass"), userTypeRef("Iface")),
        )
        processToCheckers(positiveContext, validClass)
        assertEquals(CfirResolvePhase.CHECKERS, validClass.resolvePhase)
        assertTrue(positiveContext.diagnostics.diagnostics.none { it.factoryName == "CFIR_MULTIPLE_CLASS_SUPER_TYPES" })

        val negativeContext = createContext(
            providedClasses = listOf(
                classDecl("BaseA", CfirClassKind.CLASS),
                classDecl("BaseB", CfirClassKind.CLASS),
            ),
        )
        val invalidClass = CfirClass(
            moduleData = negativeContext.moduleData,
            name = Name.identifier("InvalidClass"),
            classKind = CfirClassKind.CLASS,
            superTypeRefs = mutableListOf(userTypeRef("BaseA"), userTypeRef("BaseB")),
        )
        processToCheckers(negativeContext, invalidClass)
        assertEquals(CfirResolvePhase.CHECKERS, invalidClass.resolvePhase)
        assertTrue(negativeContext.diagnostics.diagnostics.any { it.factoryName == "CFIR_MULTIPLE_CLASS_SUPER_TYPES" })
    }

    private fun processToCheckers(context: TestContext, declaration: CfirDeclaration) {
        CfirTotalResolveProcessor(context.session, context.phaseRegistry)
            .processToPhase(declaration, CfirResolvePhase.CHECKERS)
    }

    private fun classDecl(name: String, kind: CfirClassKind): CfirClass = CfirClass(
        origin = CfirDeclarationOrigin.Source,
        moduleData = CfirModuleData(Name.identifier("provided-module")),
        name = Name.identifier(name),
        classKind = kind,
    )

    private fun userTypeRef(name: String): CfirTypeRef =
        CfirUserTypeRef(qualifier = listOf(Name.identifier(name)))

    private fun userTypeRefWithArguments(name: String, argumentName: String): CfirTypeRef =
        CfirUserTypeRef(
            qualifier = listOf(Name.identifier(name)),
            typeArguments = listOf(
                CfirBasicTypeRef(name = Name.identifier(argumentName)),
            ),
        )

    private fun createContext(providedClasses: List<CfirClass>): TestContext {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val diagnostics = CfirDiagnosticCollector()
        val moduleData = CfirModuleData(Name.identifier("resolve-test-module"))
        val phaseRegistry = CfirPhaseResolverRegistry()

        session.register(CfirModuleData::class, moduleData)
        session.register(CfirPhaseResolverRegistry::class, phaseRegistry)
        session.register(CfirDiagnosticReporter::class, diagnostics)
        session.register(CfirDiagnosticCollector::class, diagnostics)
        session.register(CfirSymbolProvider::class, CfirEmptySymbolProvider())
        session.register(CfirProvider::class, InMemoryProvider(providedClasses))
        session.register(org.cangjie.cfir.providers.CfirExtendProvider::class, CfirEmptyExtendProvider())
        session.register(CfirSuperTypeGraphStore::class, CfirSuperTypeGraphStore())
        session.register(CfirLazyDeclarationResolver::class, CfirLazyDeclarationResolver())

        registerResolveProcessors(phaseRegistry, diagnostics, session)

        return TestContext(
            session = session,
            moduleData = moduleData,
            phaseRegistry = phaseRegistry,
            diagnostics = diagnostics,
        )
    }

    private class InMemoryProvider(
        classes: List<CfirClass>,
    ) : CfirProvider {
        private val byClassId: Map<ClassId, CfirClass> = classes.associateBy {
            ClassId(FqName.ROOT, it.name)
        }

        override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> = emptyList()

        override fun getClassByClassId(classId: ClassId): CfirClass? = byClassId[classId]
    }

    private data class TestContext(
        val session: CfirSession,
        val moduleData: CfirModuleData,
        val phaseRegistry: CfirPhaseResolverRegistry,
        val diagnostics: CfirDiagnosticCollector,
    )
}

