package org.cangjie.cfir.resolve

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.declarations.CfirClass
import org.cangjie.cfir.declarations.CfirClassKind
import org.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangjie.cfir.declarations.CfirFile
import org.cangjie.cfir.declarations.CfirFunction
import org.cangjie.cfir.declarations.CfirImport
import org.cangjie.cfir.declarations.CfirPackageDirective
import org.cangjie.cfir.declarations.CfirResolvePhase
import org.cangjie.cfir.diagnostics.CfirDiagnosticCollector
import org.cangjie.cfir.diagnostics.CfirDiagnosticReporter
import org.cangjie.cfir.providers.CfirEmptyExtendProvider
import org.cangjie.cfir.providers.CfirEmptyProvider
import org.cangjie.cfir.providers.CfirExtendProvider
import org.cangjie.cfir.providers.CfirSymbolProvider
import org.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.session.importBindingStore
import org.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangjie.cfir.symbols.CfirClassSymbol
import org.cangjie.cfir.types.CfirImplicitTypeRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

class CfirImportsResolveProcessorTest {
    @Test
    fun importsPhaseBindsPackageClassCallableAndAlias() {
        val moduleData = CfirModuleData(Name.identifier("resolve-imports-test"))
        val thingClass = CfirClass(
            origin = CfirDeclarationOrigin.Source,
            moduleData = moduleData,
            name = Name.identifier("Thing"),
            classKind = CfirClassKind.CLASS,
        )
        val classId = ClassId(FqName("demo.pkg"), thingClass.name)

        val callableDeclaration = CfirFunction(
            moduleData = moduleData,
            returnTypeRef = CfirImplicitTypeRef.INSTANCE,
            name = Name.identifier("makeThing"),
        )
        val callableSymbol = callableDeclaration.symbol
        val symbolProvider = InMemorySymbolProvider(
            existingPackages = setOf(FqName("demo.pkg")),
            classSymbols = mapOf(classId to thingClass.symbol),
            callableSymbols = mapOf(
                FqName("demo.pkg") to mapOf(Name.identifier("makeThing") to listOf(callableSymbol)),
            ),
        )
        val context = createContext(moduleData, symbolProvider)

        val file = CfirFile(
            moduleData = moduleData,
            name = "imports_test.cj",
            packageDirective = CfirPackageDirective(FqName("demo.consumer")),
            imports = listOf(
                CfirImport(
                    importedFqName = FqName("demo.pkg"),
                    isAllUnder = true,
                ),
                CfirImport(
                    importedFqName = FqName("demo.pkg.Thing"),
                    aliasName = Name.identifier("AliasThing"),
                ),
                CfirImport(
                    importedFqName = FqName("demo.pkg.makeThing"),
                ),
            ),
        )

        CfirTotalResolveProcessor(context.session, context.phaseRegistry).processFile(file)

        val bindings = context.session.importBindingStore.getBindings(file)
        assertNotNull(bindings)
        assertEquals(CfirResolvePhase.CHECKERS, file.resolvePhase)
        assertEquals(3, bindings!!.imports.size)

        val packageTargets = bindings.imports[0].targets.filterIsInstance<CfirResolvedImportTarget.Package>()
        assertEquals(1, packageTargets.size)
        assertEquals(FqName("demo.pkg"), packageTargets.first().fqName)

        val classBinding = bindings.imports[1]
        assertEquals(Name.identifier("AliasThing"), classBinding.effectiveName)
        val classTargets = classBinding.targets.filterIsInstance<CfirResolvedImportTarget.ClassLike>()
        assertEquals(1, classTargets.size)
        assertEquals(classId, classTargets.first().classId)

        val callableBinding = bindings.imports[2]
        assertEquals(Name.identifier("makeThing"), callableBinding.effectiveName)
        val callableTargets = callableBinding.targets.filterIsInstance<CfirResolvedImportTarget.Callable>()
        assertEquals(1, callableTargets.size)
        assertEquals(1, callableTargets.first().symbols.size)
        assertTrue(callableTargets.first().symbols.first() === callableSymbol)
        assertTrue(context.diagnostics.diagnostics.isEmpty())
    }

    @Test
    fun importsPhaseReportsMissingTargetImmediately() {
        val moduleData = CfirModuleData(Name.identifier("resolve-imports-missing-test"))
        val symbolProvider = InMemorySymbolProvider(
            existingPackages = emptySet(),
            classSymbols = emptyMap(),
            callableSymbols = emptyMap(),
        )
        val context = createContext(moduleData, symbolProvider)
        val file = CfirFile(
            moduleData = moduleData,
            name = "imports_missing_test.cj",
            packageDirective = CfirPackageDirective(FqName("demo.consumer")),
            imports = listOf(
                CfirImport(
                    importedFqName = FqName("demo.missing.Symbol"),
                ),
            ),
        )

        CfirTotalResolveProcessor(context.session, context.phaseRegistry).processFile(file)

        val bindings = context.session.importBindingStore.getBindings(file)
        assertNotNull(bindings)
        assertEquals(1, bindings!!.imports.size)
        assertTrue(bindings.imports.first().targets.isEmpty())
        assertTrue(
            context.diagnostics.diagnostics.any { it.factoryName == "CFIR_IMPORT_TARGET_NOT_FOUND" },
        )
    }

    @Test
    fun importsPhaseReportsAliasConflict() {
        val moduleData = CfirModuleData(Name.identifier("resolve-imports-alias-conflict-test"))
        val firstClass = CfirClass(
            origin = CfirDeclarationOrigin.Source,
            moduleData = moduleData,
            name = Name.identifier("Item"),
            classKind = CfirClassKind.CLASS,
        )
        val secondClass = CfirClass(
            origin = CfirDeclarationOrigin.Source,
            moduleData = moduleData,
            name = Name.identifier("Item"),
            classKind = CfirClassKind.CLASS,
        )
        val symbolProvider = InMemorySymbolProvider(
            existingPackages = setOf(FqName("demo.one"), FqName("demo.two")),
            classSymbols = mapOf(
                ClassId(FqName("demo.one"), Name.identifier("Item")) to firstClass.symbol,
                ClassId(FqName("demo.two"), Name.identifier("Item")) to secondClass.symbol,
            ),
            callableSymbols = emptyMap(),
        )
        val context = createContext(moduleData, symbolProvider)
        val file = CfirFile(
            moduleData = moduleData,
            name = "imports_alias_conflict_test.cj",
            packageDirective = CfirPackageDirective(FqName("demo.consumer")),
            imports = listOf(
                CfirImport(
                    importedFqName = FqName("demo.one.Item"),
                    aliasName = Name.identifier("AliasItem"),
                ),
                CfirImport(
                    importedFqName = FqName("demo.two.Item"),
                    aliasName = Name.identifier("AliasItem"),
                ),
            ),
        )

        CfirTotalResolveProcessor(context.session, context.phaseRegistry).processFile(file)

        assertTrue(
            context.diagnostics.diagnostics.any { it.factoryName == "CFIR_IMPORT_ALIAS_CONFLICT" },
        )
    }

    @Test
    fun importsPhaseReportsNameConflict() {
        val moduleData = CfirModuleData(Name.identifier("resolve-imports-name-conflict-test"))
        val firstClass = CfirClass(
            origin = CfirDeclarationOrigin.Source,
            moduleData = moduleData,
            name = Name.identifier("Item"),
            classKind = CfirClassKind.CLASS,
        )
        val secondClass = CfirClass(
            origin = CfirDeclarationOrigin.Source,
            moduleData = moduleData,
            name = Name.identifier("Item"),
            classKind = CfirClassKind.CLASS,
        )
        val symbolProvider = InMemorySymbolProvider(
            existingPackages = setOf(FqName("demo.one"), FqName("demo.two")),
            classSymbols = mapOf(
                ClassId(FqName("demo.one"), Name.identifier("Item")) to firstClass.symbol,
                ClassId(FqName("demo.two"), Name.identifier("Item")) to secondClass.symbol,
            ),
            callableSymbols = emptyMap(),
        )
        val context = createContext(moduleData, symbolProvider)
        val file = CfirFile(
            moduleData = moduleData,
            name = "imports_name_conflict_test.cj",
            packageDirective = CfirPackageDirective(FqName("demo.consumer")),
            imports = listOf(
                CfirImport(
                    importedFqName = FqName("demo.one.Item"),
                ),
                CfirImport(
                    importedFqName = FqName("demo.two.Item"),
                ),
            ),
        )

        CfirTotalResolveProcessor(context.session, context.phaseRegistry).processFile(file)

        assertTrue(
            context.diagnostics.diagnostics.any { it.factoryName == "CFIR_IMPORT_CONFLICT" },
        )
    }

    @Test
    fun totalAndLazyResolveAreConsistentAtImportsPhase() {
        val totalModuleData = CfirModuleData(Name.identifier("resolve-imports-total-test"))
        val lazyModuleData = CfirModuleData(Name.identifier("resolve-imports-lazy-test"))

        val itemInPkg = CfirClass(
            origin = CfirDeclarationOrigin.Source,
            moduleData = totalModuleData,
            name = Name.identifier("Item"),
            classKind = CfirClassKind.CLASS,
        )
        val callableDeclaration = CfirFunction(
            moduleData = totalModuleData,
            returnTypeRef = CfirImplicitTypeRef.INSTANCE,
            name = Name.identifier("makeItem"),
        )
        val symbolProvider = InMemorySymbolProvider(
            existingPackages = setOf(FqName("demo.pkg")),
            classSymbols = mapOf(
                ClassId(FqName("demo.pkg"), Name.identifier("Item")) to itemInPkg.symbol,
            ),
            callableSymbols = mapOf(
                FqName("demo.pkg") to mapOf(Name.identifier("makeItem") to listOf(callableDeclaration.symbol)),
            ),
        )

        val totalContext = createContext(totalModuleData, symbolProvider)
        val lazyContext = createContext(lazyModuleData, symbolProvider)
        val totalFile = CfirFile(
            moduleData = totalModuleData,
            name = "imports_consistency_total.cj",
            packageDirective = CfirPackageDirective(FqName("demo.consumer")),
            imports = listOf(
                CfirImport(importedFqName = FqName("demo.pkg"), isAllUnder = true),
                CfirImport(importedFqName = FqName("demo.pkg.Item"), aliasName = Name.identifier("AliasItem")),
                CfirImport(importedFqName = FqName("demo.pkg.makeItem")),
                CfirImport(importedFqName = FqName("demo.missing.Unknown")),
            ),
        )
        val lazyFile = CfirFile(
            moduleData = lazyModuleData,
            name = "imports_consistency_lazy.cj",
            packageDirective = CfirPackageDirective(FqName("demo.consumer")),
            imports = listOf(
                CfirImport(importedFqName = FqName("demo.pkg"), isAllUnder = true),
                CfirImport(importedFqName = FqName("demo.pkg.Item"), aliasName = Name.identifier("AliasItem")),
                CfirImport(importedFqName = FqName("demo.pkg.makeItem")),
                CfirImport(importedFqName = FqName("demo.missing.Unknown")),
            ),
        )

        CfirTotalResolveProcessor(totalContext.session, totalContext.phaseRegistry).processFile(totalFile)
        CfirTotalResolveProcessor(lazyContext.session, lazyContext.phaseRegistry)
            .processToPhase(lazyFile, CfirResolvePhase.IMPORTS)

        val totalBindings = totalContext.session.importBindingStore.getBindings(totalFile)
        val lazyBindings = lazyContext.session.importBindingStore.getBindings(lazyFile)
        assertNotNull(totalBindings)
        assertNotNull(lazyBindings)
        assertEquals(totalBindings!!.imports.map { it.stableSignature() }, lazyBindings!!.imports.map { it.stableSignature() })
        assertEquals(
            totalContext.diagnostics.diagnostics.map { "${it.factoryName}:${it.message}" },
            lazyContext.diagnostics.diagnostics.map { "${it.factoryName}:${it.message}" },
        )
    }

    private fun createContext(
        moduleData: CfirModuleData,
        symbolProvider: CfirSymbolProvider,
    ): TestContext {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val diagnostics = CfirDiagnosticCollector()
        val phaseRegistry = CfirPhaseResolverRegistry()

        session.register(CfirModuleData::class, moduleData)
        session.register(CfirPhaseResolverRegistry::class, phaseRegistry)
        session.register(CfirDiagnosticReporter::class, diagnostics)
        session.register(CfirDiagnosticCollector::class, diagnostics)
        session.register(CfirSymbolProvider::class, symbolProvider)
        session.register(org.cangjie.cfir.providers.CfirProvider::class, CfirEmptyProvider())
        session.register(CfirExtendProvider::class, CfirEmptyExtendProvider())
        session.register(CfirImportBindingStore::class, CfirImportBindingStore())

        registerResolveProcessors(phaseRegistry, diagnostics)

        return TestContext(
            session = session,
            phaseRegistry = phaseRegistry,
            diagnostics = diagnostics,
        )
    }

    private class InMemorySymbolProvider(
        private val existingPackages: Set<FqName>,
        private val classSymbols: Map<ClassId, CfirClassSymbol>,
        private val callableSymbols: Map<FqName, Map<Name, List<CfirCallableSymbol<*>>>>,
    ) : CfirSymbolProvider() {
        override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassSymbol? = classSymbols[classId]

        override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> =
            callableSymbols[packageFqName]?.get(name).orEmpty()

        override fun hasPackage(fqName: FqName): Boolean = fqName in existingPackages
    }

    private data class TestContext(
        val session: CfirSession,
        val phaseRegistry: CfirPhaseResolverRegistry,
        val diagnostics: CfirDiagnosticCollector,
    )
}

private fun CfirResolvedImportBinding.stableSignature(): String {
    val targetSignature = targets.map { target ->
        when (target) {
            is CfirResolvedImportTarget.Package -> "pkg:${target.fqName.asString()}"
            is CfirResolvedImportTarget.ClassLike -> "class:${target.classId.asString()}"
            is CfirResolvedImportTarget.Callable -> "callable:${target.packageFqName.asString()}.${target.name.asString()}#${target.symbols.size}"
        }
    }.sorted().joinToString(";")
    return "${importDirective.importedFqName.asString()}|${importDirective.isAllUnder}|${effectiveName.asString()}|$targetSignature"
}
