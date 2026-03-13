package org.cangjie.cfir.resolve

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.declarations.CfirClass
import org.cangjie.cfir.declarations.CfirClassKind
import org.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangjie.cfir.declarations.CfirFile
import org.cangjie.cfir.declarations.CfirFunction
import org.cangjie.cfir.declarations.CfirPackageDirective
import org.cangjie.cfir.declarations.CfirProperty
import org.cangjie.cfir.declarations.CfirResolvePhase
import org.cangjie.cfir.declarations.CfirTypeParameter
import org.cangjie.cfir.declarations.CfirValueParameter
import org.cangjie.cfir.resolve.providers.CfirEmptyExtendProvider
import org.cangjie.cfir.resolve.providers.CfirEmptySymbolProvider
import org.cangjie.cfir.resolve.providers.CfirProvider
import org.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangjie.cfir.resolve.services.CfirLazyDeclarationResolver
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangjie.cfir.types.CfirUserTypeRef
import org.cangjie.cfir.types.ConeClassLikeType
import org.cangjie.cfir.types.ConeFuncType
import org.cangjie.cfir.types.ConeTypeParameterType
import org.cangjie.cfir.types.ConeTupleType
import org.cangjie.cfir.types.ConeVArrayType
import org.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangjie.cfir.types.CfirTupleTypeRef
import org.cangjie.cfir.types.CfirVArrayTypeRef
import org.cangjie.cfir.types.CfirErrorTypeRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

class CfirTypesResolveProcessorTest {
    @Test
    fun typesPhaseResolvesFunctionPropertyAndTypeParameterBounds() {
        val context = createContext(
            providedClasses = listOf(
                classDecl("Base", CfirClassKind.CLASS),
            ),
        )
        val typeParameter = CfirTypeParameter(
            moduleData = context.moduleData,
            name = Name.identifier("T"),
            bounds = mutableListOf(userTypeRef("Base")),
        )
        val function = CfirFunction(
            moduleData = context.moduleData,
            name = Name.identifier("foo"),
            returnTypeRef = userTypeRef("Base"),
            typeParameters = listOf(typeParameter),
            valueParameters = listOf(
                CfirValueParameter(
                    moduleData = context.moduleData,
                    name = Name.identifier("param"),
                    returnTypeRef = userTypeRef("T"),
                ),
            ),
        )
        val property = CfirProperty(
            moduleData = context.moduleData,
            name = Name.identifier("field"),
            returnTypeRef = userTypeRef("Base"),
        )

        CfirTotalResolveProcessor(context.session, context.phaseRegistry).processToPhase(function, CfirResolvePhase.TYPES)
        CfirTotalResolveProcessor(context.session, context.phaseRegistry).processToPhase(property, CfirResolvePhase.TYPES)

        val functionReturnType = function.returnTypeRef as? CfirResolvedTypeRef
        val parameterType = function.valueParameters.single().returnTypeRef as? CfirResolvedTypeRef
        val boundType = typeParameter.bounds.single() as? CfirResolvedTypeRef
        val propertyType = property.returnTypeRef as? CfirResolvedTypeRef

        assertTrue(functionReturnType?.coneType is ConeClassLikeType)
        assertTrue(parameterType?.coneType is ConeTypeParameterType)
        assertTrue(boundType?.coneType is ConeClassLikeType)
        assertTrue(propertyType?.coneType is ConeClassLikeType)
        assertEquals(CfirResolvePhase.TYPES, function.resolvePhase)
        assertEquals(CfirResolvePhase.TYPES, function.valueParameters.single().resolvePhase)
        assertEquals(CfirResolvePhase.TYPES, typeParameter.resolvePhase)
        assertEquals(CfirResolvePhase.TYPES, property.resolvePhase)
    }

    @Test
    fun typesPhaseResolvesTupleFunctionAndVArrayForms() {
        val context = createContext(
            providedClasses = listOf(
                classDecl("Base", CfirClassKind.CLASS),
            ),
        )
        val tupleProperty = CfirProperty(
            moduleData = context.moduleData,
            name = Name.identifier("tupleField"),
            returnTypeRef = CfirTupleTypeRef(
                elementTypeRefs = listOf(userTypeRef("Base"), userTypeRef("Base")),
            ),
        )
        val functionTypeProperty = CfirProperty(
            moduleData = context.moduleData,
            name = Name.identifier("funcField"),
            returnTypeRef = CfirFunctionTypeRef(
                parameterTypeRefs = listOf(userTypeRef("Base")),
                returnTypeRef = userTypeRef("Base"),
            ),
        )
        val varrayProperty = CfirProperty(
            moduleData = context.moduleData,
            name = Name.identifier("varrayField"),
            returnTypeRef = CfirVArrayTypeRef(
                elementTypeRef = userTypeRef("Base"),
                sizeLiteral = "4",
            ),
        )

        val resolver = CfirTotalResolveProcessor(context.session, context.phaseRegistry)
        resolver.processToPhase(tupleProperty, CfirResolvePhase.TYPES)
        resolver.processToPhase(functionTypeProperty, CfirResolvePhase.TYPES)
        resolver.processToPhase(varrayProperty, CfirResolvePhase.TYPES)

        val tupleType = (tupleProperty.returnTypeRef as? CfirResolvedTypeRef)?.coneType
        val functionType = (functionTypeProperty.returnTypeRef as? CfirResolvedTypeRef)?.coneType
        val varrayType = (varrayProperty.returnTypeRef as? CfirResolvedTypeRef)?.coneType

        assertTrue(tupleType is ConeTupleType)
        assertTrue(functionType is ConeFuncType)
        assertTrue(varrayType is ConeVArrayType)
    }

    @Test
    fun typesPhaseUsesErrorTypePlaceholderAndKeepsRecovery() {
        val context = createContext(
            providedClasses = listOf(
                classDecl("Base", CfirClassKind.CLASS),
            ),
        )
        val invalidFunction = CfirFunction(
            moduleData = context.moduleData,
            name = Name.identifier("bad"),
            returnTypeRef = userTypeRef("UnknownType"),
        )
        val validProperty = CfirProperty(
            moduleData = context.moduleData,
            name = Name.identifier("good"),
            returnTypeRef = userTypeRef("Base"),
        )
        val file = CfirFile(
            moduleData = context.moduleData,
            name = "types_recovery_test.cj",
            packageDirective = CfirPackageDirective(FqName("demo.types")),
            declarations = mutableListOf(invalidFunction, validProperty),
        )

        CfirTotalResolveProcessor(context.session, context.phaseRegistry).processFile(file)

        assertTrue(invalidFunction.returnTypeRef is CfirErrorTypeRef)
        assertTrue((validProperty.returnTypeRef as? CfirResolvedTypeRef)?.coneType is ConeClassLikeType)
        assertEquals(CfirResolvePhase.CHECKERS, invalidFunction.resolvePhase)
        assertEquals(CfirResolvePhase.CHECKERS, validProperty.resolvePhase)
        assertTrue(context.diagnostics.diagnostics.any { it.factoryName == "CFIR_TYPES_ERROR_RECOVERY" })
    }

    private fun createContext(providedClasses: List<CfirClass>): TestContext {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val diagnostics = CfirDiagnosticCollector()
        val moduleData = CfirModuleData(Name.identifier("types-test-module"))
        val phaseRegistry = CfirPhaseResolverRegistry()

        session.register(CfirModuleData::class, moduleData)
        session.register(CfirPhaseResolverRegistry::class, phaseRegistry)
        session.register(CfirDiagnosticReporter::class, diagnostics)
        session.register(CfirDiagnosticCollector::class, diagnostics)
        session.register(CfirSymbolProvider::class, CfirEmptySymbolProvider())
        session.register(CfirProvider::class, InMemoryProvider(providedClasses))
        session.register(org.cangjie.cfir.providers.CfirExtendProvider::class, CfirEmptyExtendProvider())
        session.register(CfirLazyDeclarationResolver::class, CfirLazyDeclarationResolver())

        registerResolveProcessors(phaseRegistry, diagnostics, session)

        return TestContext(
            session = session,
            moduleData = moduleData,
            phaseRegistry = phaseRegistry,
            diagnostics = diagnostics,
        )
    }

    private fun classDecl(name: String, kind: CfirClassKind): CfirClass = CfirClass(
        origin = CfirDeclarationOrigin.Source,
        moduleData = CfirModuleData(Name.identifier("provided-module")),
        name = Name.identifier(name),
        classKind = kind,
    )

    private fun userTypeRef(name: String) =
        CfirUserTypeRef(qualifier = listOf(Name.identifier(name)))

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
