@file:OptIn(org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirBuiltInDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirBuiltInTypeKind
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.resolve.providers.*
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.CfirImportResolveTransformer
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitSimpleImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBuiltInTypeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.resolve.substitution.substitutorByMap
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** 官方 v1.1.3 PreCheck 的类型简单名选择：先确定 scope，再选择该层的 SPECIFIC。 */
class CfirCjmpTypeCandidateCollectorTest {
    @Test
    fun `reexport namespace retains common and specific with different actual class ids`() {
        val fixture = Fixture()
        val common = fixture.klass("common.A", common = true)
        val specific = fixture.klass("specific.B", specific = true)
        val exportName = Name.identifier("Shared")
        val namespace = CfirImportNamespaceContext(FqName("facade"), null)
        fixture.install(
            ExportingLibraryProvider(fixture.session, listOf(common), mapOf(namespace to mapOf(exportName to listOf(common)))),
            ExportingLibraryProvider(fixture.session, listOf(specific), mapOf(namespace to mapOf(exportName to listOf(specific)))),
        )

        assertEquals(listOf(common, specific), fixture.provider.getImportNamespace(namespace).classifiers(exportName))
        val binding = fixture.session.resolveImportBinding(buildImport {
            importedFqName = FqName("facade.Shared")
            isAllUnder = false
        }, CfirLookupOrigin.EXPLICIT_IMPORT)
        val scope = CfirExplicitSimpleImportingScope(listOf(binding), fixture.provider)
        val structural = mutableListOf<CfirClassLikeSymbol<*>>()
        scope.processClassifiersByName(exportName, structural::add)
        assertEquals(listOf(common, specific), structural)
        assertSame(specific, fixture.collector.candidatesFromScope(scope, exportName).single().symbol)
    }

    @Test
    fun `explicit class import is materialized by the consumer session provider`() {
        val owner = Fixture()
        val ownerSymbol = owner.klass("library.Original")
        val exportName = Name.identifier("Exported")
        val namespace = CfirImportNamespaceContext(FqName("facade"), null)
        owner.install(
            ExportingLibraryProvider(owner.session, listOf(ownerSymbol), mapOf(namespace to mapOf(exportName to listOf(ownerSymbol)))),
        )
        val ownerBinding = owner.session.resolveImportBinding(buildImport {
            importedFqName = FqName("facade.Exported")
            isAllUnder = false
        }, CfirLookupOrigin.EXPLICIT_IMPORT)

        val consumer = Fixture()
        val consumerSymbol = consumer.klass("library.Original")
        val unrelatedSymbol = consumer.klass("library.Unrelated")
        consumer.install(
            ExportingLibraryProvider(
                consumer.session,
                listOf(consumerSymbol, unrelatedSymbol),
                mapOf(namespace to mapOf(exportName to listOf(consumerSymbol, unrelatedSymbol))),
            ),
        )
        val scope = CfirExplicitSimpleImportingScope(listOf(ownerBinding), consumer.provider)
        val visibleSymbols = mutableListOf<CfirClassLikeSymbol<*>>()

        scope.processClassifiersByName(exportName, visibleSymbols::add)

        assertEquals(listOf(consumerSymbol), visibleSymbols)
        assertSame(consumerSymbol, visibleSymbols.single())
        assertNotSame(ownerSymbol, visibleSymbols.single(), "the file binding must not leak its owner-session symbol")
        assertEquals(
            listOf(ownerSymbol.classId),
            ownerBinding.targets.filterIsInstance<CfirResolvedImportTarget.ClassLike>().map { it.classId },
        )
    }

    @Test
    fun `foreign file is rejected before import transformer mutates declarations`() {
        val owner = Fixture()
        val child = owner.klass("owner.Child").cfir
        val file = ExtendTestFixtures.newFile(owner.module, FqName("owner"), listOf(child))
        child.replaceResolvePhase(CfirResolvePhase.RAW_CFIR)
        file.replaceResolvePhase(CfirResolvePhase.RAW_CFIR)

        val consumer = Fixture()
        val transformer = CfirImportResolveTransformer(consumer.session, CfirDiagnosticCollector())

        assertThrows(IllegalStateException::class.java) {
            transformer.transformDeclaration(file, null)
        }
        assertEquals(CfirResolvePhase.RAW_CFIR, file.resolvePhase)
        assertEquals(CfirResolvePhase.RAW_CFIR, child.resolvePhase)
    }

    @Test
    fun `class id provider retains common and specific structural declarations`() {
        val fixture = Fixture()
        val common = fixture.klass("library.Box", common = true)
        val specific = fixture.klass("library.Box", specific = true)
        fixture.install(
            ExportingLibraryProvider(fixture.session, listOf(common)),
            ExportingLibraryProvider(fixture.session, listOf(specific)),
        )

        assertEquals(listOf(common, specific), fixture.provider.getClassLikeSymbolsByClassId(common.classId))
    }

    @Test
    fun `materialized builtin removes only the synthetic declaration with the same actual class id`() {
        val fixture = Fixture()
        val materialized = fixture.builtin(CfirBuiltInTypeKind.CSTRING, synthetic = false)
        val duplicate = fixture.builtin(CfirBuiltInTypeKind.CSTRING)
        val otherBuiltin = fixture.builtin(CfirBuiltInTypeKind.CFUNC)
        val exportName = Name.identifier("Shared")
        val namespace = CfirImportNamespaceContext(FqName("facade"), null)
        fixture.install(
            ExportingLibraryProvider(fixture.session, listOf(materialized), mapOf(namespace to mapOf(exportName to listOf(materialized)))),
            ExportingLibraryProvider(fixture.session, listOf(duplicate, otherBuiltin), mapOf(namespace to mapOf(exportName to listOf(duplicate, otherBuiltin)))),
        )

        assertEquals(listOf(materialized, otherBuiltin), fixture.provider.getImportNamespace(namespace).classifiers(exportName))
    }

    @Test
    fun `synthetic builtin duplicates are normalized independently for each actual class id`() {
        val fixture = Fixture()
        val firstString = fixture.builtin(CfirBuiltInTypeKind.CSTRING)
        val secondString = fixture.builtin(CfirBuiltInTypeKind.CSTRING)
        val function = fixture.builtin(CfirBuiltInTypeKind.CFUNC)
        val exportName = Name.identifier("Shared")
        val namespace = CfirImportNamespaceContext(FqName("facade"), null)
        fixture.install(
            ExportingLibraryProvider(fixture.session, listOf(firstString), mapOf(namespace to mapOf(exportName to listOf(firstString)))),
            ExportingLibraryProvider(fixture.session, listOf(secondString, function), mapOf(namespace to mapOf(exportName to listOf(secondString, function)))),
        )

        assertEquals(listOf(firstString, function), fixture.provider.getImportNamespace(namespace).classifiers(exportName))
    }

    @Test
    fun `same import scope chooses specific without requiring cjmp pairing`() {
        val fixture = Fixture()
        val common = fixture.klass("common.Box", common = true)
        val specific = fixture.klass("specific.Box", specific = true)
        fixture.install(ExportingLibraryProvider(fixture.session, listOf(common, specific)))
        val scope = fixture.importScope(common, specific)

        assertSame(specific, fixture.collector.firstVisibleScopeCandidates(listOf(scope), boxName).single().symbol)
        val lookup = fixture.collector.lookupSimpleTypeName(listOf(scope), boxName, null)
            as CfirTypeCandidateCollector.SimpleTypeNameLookup.Classifiers
        assertSame(specific, lookup.candidates.single().symbol)
    }

    @Test
    fun `nearer scope common shadows specific from an outer scope`() {
        val fixture = Fixture()
        val common = fixture.klass("common.Box", common = true)
        val specific = fixture.klass("specific.Box", specific = true)
        fixture.install(ExportingLibraryProvider(fixture.session, listOf(common, specific)))
        val scopes = listOf(fixture.importScope(common), CfirPackageMemberScope(FqName("specific"), fixture.session))

        assertSame(common, fixture.collector.firstVisibleScopeCandidates(scopes, boxName).single().symbol)
        val lookup = fixture.collector.lookupSimpleTypeName(scopes, boxName, null)
            as CfirTypeCandidateCollector.SimpleTypeNameLookup.Classifiers
        assertSame(common, lookup.candidates.single().symbol)
    }

    @Test
    fun `disabled common specific language feature retains type ambiguity`() {
        val fixture = Fixture(enabled = false)
        val common = fixture.klass("common.Box", common = true)
        val specific = fixture.klass("specific.Box", specific = true)
        fixture.install(ExportingLibraryProvider(fixture.session, listOf(common, specific)))
        val scope = fixture.importScope(common, specific)

        assertEquals(listOf(common, specific), fixture.collector.candidatesFromScope(scope, boxName).map { it.symbol })
        val lookup = fixture.collector.lookupSimpleTypeName(listOf(scope), boxName, null)
            as CfirTypeCandidateCollector.SimpleTypeNameLookup.Classifiers
        assertEquals(listOf(common, specific), lookup.candidates.map { it.symbol })
    }

    @Test
    fun `type choice precedes declaration kind and generic matching`() {
        val fixture = Fixture()
        val commonParameter = ExtendTestFixtures.newTypeParameter(fixture.module, "T")
        val common = ExtendTestFixtures.newClass(
            fixture.module, "Box", ClassId(FqName("common"), boxName), typeParameters = listOf(commonParameter),
        ).apply { (status as CfirDeclarationStatusImpl).isCommon = true }.symbol
        val specific = ExtendTestFixtures.newInterface(
            fixture.module, "Box", ClassId(FqName("specific"), boxName),
        ).apply { (status as CfirDeclarationStatusImpl).isSpecific = true }.symbol
        fixture.install(ExportingLibraryProvider(fixture.session, listOf(common, specific)))

        assertSame(specific, fixture.collector.candidatesFromScope(fixture.importScope(common, specific), boxName).single().symbol)
    }

    @Test
    fun `specific choice retains the first specific candidate substitutor and lookup origin`() {
        val fixture = Fixture()
        val common = fixture.klass("common.Box", common = true)
        val firstSpecific = fixture.klass("specific.first.Box", specific = true)
        val secondSpecific = fixture.klass("specific.second.Box", specific = true)
        fixture.install(ExportingLibraryProvider(fixture.session, listOf(common, firstSpecific, secondSpecific)))
        val parameter = ExtendTestFixtures.newTypeParameter(fixture.module, "T")
        val substitutor = substitutorByMap(mapOf(parameter.symbol to ConePrimitiveType.INT64), fixture.session)
        val scope = ClassifierScope(
            listOf(common to ConeSubstitutor.Empty, firstSpecific to substitutor, secondSpecific to ConeSubstitutor.Empty),
            CfirLookupOrigin.MEMBER,
        )

        val candidate = fixture.collector.candidatesFromScope(scope, boxName).single()
        assertSame(firstSpecific, candidate.symbol)
        assertSame(substitutor, candidate.substitutor)
        assertEquals(CfirLookupOrigin.MEMBER, candidate.lookupOrigin)
        assertSame(ConePrimitiveType.INT64, candidate.substitutor!!.substituteOrSelf(ExtendTestFixtures.typeParameterType(parameter)))
    }

    /** 真实 session 组件与库 provider；不注册 CJMP 配对服务，使名字选择独立于家族匹配。 */
    private class Fixture(enabled: Boolean = true) {
        private val sessionAndModule = ExtendTestFixtures.newSessionAndModule("cjmp-type-name")
        val session = sessionAndModule.first
        val module = sessionAndModule.second
        lateinit var provider: CfirCompositeSymbolProvider
            private set
        val collector: CfirTypeCandidateCollector
            get() = CfirTypeCandidateCollector(session, CfirAccessContext(useSiteFile = null, kind = CfirAccessKind.TYPE))

        init {
            session.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(
                LanguageVersionSettingsImpl(
                    LanguageVersion.CANGJIE_1_1_3,
                    specificFeatures = mapOf(LanguageFeature.CommonSpecificDeclarations to if (enabled) {
                        LanguageFeature.State.ENABLED
                    } else {
                        LanguageFeature.State.DISABLED
                    }),
                ),
            ))
            session.register(CfirAccessibilityChecker::class, CfirAccessibilityChecker(session))
        }

        fun klass(fqName: String, common: Boolean = false, specific: Boolean = false): CfirClassLikeSymbol<*> {
            val name = FqName(fqName)
            return ExtendTestFixtures.newClass(module, name.shortName().asString(), ClassId(name.parent(), name.shortName()))
                .apply {
                    (status as CfirDeclarationStatusImpl).apply {
                        isCommon = common
                        isSpecific = specific
                    }
                }.symbol
        }

        fun builtin(kind: CfirBuiltInTypeKind, synthetic: Boolean = true): CfirBuiltInTypeSymbol =
            CfirBuiltInDeclaration(
                moduleData = module,
                symbol = CfirBuiltInTypeSymbol(kind.classId, kind),
                name = kind.classId.shortClassName,
                kind = kind,
                scopeProvider = CfirCangJieScopeProvider(),
                origin = if (synthetic) CfirDeclarationOrigin.Synthetic.Default else CfirDeclarationOrigin.Library,
                typeParameters = MutableList(kind.typeParameterCount) { ExtendTestFixtures.newTypeParameter(module, "T") },
            ).symbol

        fun install(vararg providers: CfirSymbolProvider) {
            provider = CfirCompositeSymbolProvider(session, providers.toList())
            session.register(CfirSymbolProvider::class, provider)
            session.register(CfirProvider::class, CfirLibrarySessionProvider(provider))
        }

        fun importScope(vararg symbols: CfirClassLikeSymbol<*>): CfirExplicitSimpleImportingScope =
            CfirExplicitSimpleImportingScope(symbols.map { symbol ->
                session.resolveImportBinding(buildImport {
                    importedFqName = symbol.classId.asSingleFqName()
                    isAllUnder = false
                }, CfirLookupOrigin.EXPLICIT_IMPORT)
            }, provider)
    }

    /** 库的声明表与导出表分别建模，允许导出名不同于真实 ClassId；不包含候选取舍逻辑。 */
    private class ExportingLibraryProvider(
        session: CfirSession,
        private val declarations: List<CfirClassLikeSymbol<*>>,
        private val exports: Map<CfirImportNamespaceContext, Map<Name, List<CfirClassLikeSymbol<*>>>> = emptyMap(),
    ) : CfirSymbolProvider(session) {
        private val packages = declarations.map { it.classId.packageFqName }.toSet() + exports.keys.map { it.packageFqName }
        override val symbolNamesProvider = object : CfirSymbolNamesProviderWithoutCallables() {
            override val hasSpecificClassifierPackageNamesComputation = false
            override fun getPackageNames(): Set<String> = packages.map { it.asString() }.toSet()
            override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> =
                declarations.filter { it.classId.packageFqName == packageFqName }.map { it.classId.shortClassName }.toSet() +
                    exports.filterKeys { it.packageFqName == packageFqName }.values.flatMap { it.keys }
        }

        override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
            declarations.firstOrNull { it.classId == classId }
        override fun getClassLikeSymbolsByClassId(classId: ClassId): List<CfirClassLikeSymbol<*>> =
            declarations.filter { it.classId == classId }
        override fun hasPackage(fqName: FqName): Boolean = fqName in packages
        override fun getImportNamespace(context: CfirImportNamespaceContext): CfirImportNamespace {
            val exported = exports[context] ?: return super.getImportNamespace(context)
            return object : CfirImportNamespace {
                override fun hasPackage(includeSubpackages: Boolean): Boolean = true
                override fun classifiers(name: Name): List<CfirClassLikeSymbol<*>> = exported[name].orEmpty()
                override fun callables(name: Name): List<CfirCallableSymbol<*>> = emptyList()
                override fun functions(name: Name): List<CfirNamedFunctionSymbol> = emptyList()
                override fun properties(name: Name): List<CfirPropertySymbol> = emptyList()
            }
        }
        override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {}
        override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {}
        override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {}
    }

    /** scope fixture 只传递符号、真实替换器和来源，让 collector 执行所有选择。 */
    private class ClassifierScope(
        private val candidates: List<Pair<CfirClassLikeSymbol<*>, ConeSubstitutor>>,
        override val lookupOrigin: CfirLookupOrigin,
    ) : CfirScope(), CfirLookupOriginScope {
        override fun processClassifiersByNameWithSubstitution(
            name: Name,
            processor: (CfirClassifierSymbol<*>, ConeSubstitutor) -> Unit,
        ) {
            candidates.filter { it.first.classId.shortClassName == name }.forEach { (symbol, substitutor) ->
                processor(symbol, substitutor)
            }
        }
        override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirScope = this
    }

    private companion object {
        val boxName = Name.identifier("Box")
    }
}
