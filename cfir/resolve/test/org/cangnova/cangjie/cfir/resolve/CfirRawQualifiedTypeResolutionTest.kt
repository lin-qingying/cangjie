@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals::class,
)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.builder.buildQualifierPart
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAvailabilityProvider
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotRefToPackageNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeNotATypeError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedTypeQualifierError
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityChecker
import org.cangnova.cangjie.cfir.resolve.providers.CfirImportNamespace
import org.cangnova.cangjie.cfir.resolve.providers.CfirImportNamespaceContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirLibrarySessionProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProviderWithoutCallables
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.scopes.CfirDefaultImportsProviderHolder
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.builder.buildUserTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/** 锁定源码裸限定类型通过 package namespace 解析并保留独立诊断通道。 */
class CfirRawQualifiedTypeResolutionTest {
    @Test
    fun `raw qualified type retains its class id with package name diagnostic`() {
        val fixture = Fixture()
        val typeRef = fixture.typeRef("std", "net", "SockAddr")

        val result = fixture.resolver.resolveType(
            typeRef = typeRef,
            configuration = TypeResolutionConfiguration(useSiteFile = fixture.file),
            areBareTypesAllowed = false,
            isOperandOfIsOperator = false,
            resolveDeprecations = true,
            supertypeSupplier = SupertypeSupplier.Default,
        )

        val resolvedType = assertInstanceOf(ConeClassLikeType::class.java, result.type)
        assertEquals(fixture.classId, resolvedType.lookupTag.classId)
        assertInstanceOf(ConeCannotRefToPackageNameError::class.java, result.diagnostic)
    }

    @Test
    fun `explicit first segment binding prevents raw qualified fallback`() {
        val fixture = Fixture(explicitlyBindStd = true)
        val result = fixture.resolve(fixture.typeRef("std", "net", "SockAddr"))

        assertInstanceOf(ConeErrorType::class.java, result.type)
        assertInstanceOf(ConeUnresolvedTypeQualifierError::class.java, result.diagnostic)
    }

    @Test
    fun `all under import does not bind the raw qualified first segment`() {
        val fixture = Fixture(allUnderStd = true)
        val result = fixture.resolve(fixture.typeRef("std", "net", "SockAddr"))

        val resolvedType = assertInstanceOf(ConeClassLikeType::class.java, result.type)
        assertEquals(fixture.classId, resolvedType.lookupTag.classId)
        assertInstanceOf(ConeCannotRefToPackageNameError::class.java, result.diagnostic)
    }

    @Test
    fun `missing package and missing classifier do not report package name diagnostic`() {
        val fixture = Fixture()
        val missingPackage = fixture.resolve(fixture.typeRef("missing", "package", "Type"))
        val missingClassifier = fixture.resolve(fixture.typeRef("std", "net", "MissingType"))

        assertInstanceOf(ConeUnresolvedTypeQualifierError::class.java, missingPackage.diagnostic)
        assertInstanceOf(ConeUnresolvedTypeQualifierError::class.java, missingClassifier.diagnostic)
    }

    @Test
    fun `ambiguous raw namespace candidates do not report package name diagnostic`() {
        val fixture = Fixture(ambiguousRawCandidates = true)
        val result = fixture.resolve(fixture.typeRef("std", "net", "SockAddr"))

        assertInstanceOf(ConeErrorType::class.java, result.type)
        assertInstanceOf(ConeAmbiguityError::class.java, result.diagnostic)
    }

    @Test
    fun `package callable used as a type reports not-a-type without package diagnostic`() {
        val fixture = Fixture(includeCorePrint = true)
        val result = fixture.resolve(fixture.typeRef("std", "core", "print"))

        assertInstanceOf(ConeErrorType::class.java, result.type)
        assertInstanceOf(ConeNotATypeError::class.java, result.diagnostic)
    }

    @Test
    fun `macro name used as type reports not-a-type without package diagnostic`() {
        val fixture = Fixture(includeNetMacro = true)
        val result = fixture.resolve(fixture.typeRef("std", "net", "MacroType"))

        assertInstanceOf(ConeErrorType::class.java, result.type)
        // 官方 PreCheck 对非注解类型位置的非类型声明报 not_a_type；这里同时确认没有
        // 被误判成“包名不能作为类型”诊断。
        assertInstanceOf(ConeNotATypeError::class.java, result.diagnostic)
    }

    @Test
    fun `package alias preserves organization context and reexport target class id`() {
        val targetClassId = ClassId(FqName("implementation.types"), Name.identifier("SockAddr"))
        val fixture = Fixture(
            resolvedClassId = targetClassId,
            organizationQualifiedPackageAlias = true,
        )

        val result = fixture.resolve(fixture.typeRef("network", "net", "SockAddr"))

        val resolvedType = assertInstanceOf(ConeClassLikeType::class.java, result.type)
        assertEquals(targetClassId, resolvedType.lookupTag.classId)
        assertEquals(null, result.diagnostic)
        assertEquals(
            CfirImportNamespaceContext(FqName("bridge.net"), Name.identifier("org1")),
            fixture.symbolProvider.lastImportNamespaceContext,
        )
    }

    @Test
    fun `resolving an error type ref preserves its resolved type and diagnostic`() {
        val fixture = Fixture()
        val original = fixture.resolve(fixture.typeRef("std", "net", "SockAddr"))
        val errorTypeRef = org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef {
            source = TestBinarySourceElement("std.net.SockAddr")
            coneType = original.type
            diagnostic = original.diagnostic!!
        }

        val rereresolved = fixture.resolve(errorTypeRef)

        assertSame(original.type, rereresolved.type)
        assertSame(original.diagnostic, rereresolved.diagnostic)
    }

    private class Fixture(
        explicitlyBindStd: Boolean = false,
        allUnderStd: Boolean = false,
        private val resolvedClassId: ClassId = ClassId(FqName("std.net"), Name.identifier("SockAddr")),
        organizationQualifiedPackageAlias: Boolean = false,
        ambiguousRawCandidates: Boolean = false,
        includeCorePrint: Boolean = false,
        includeNetMacro: Boolean = false,
    ) {
        private val sessionAndModule = ExtendTestFixtures.newSessionAndModule("raw-qualified-type")
        private val session = sessionAndModule.first
        private val moduleData = sessionAndModule.second
        val classId = resolvedClassId
        private val declaration = ExtendTestFixtures.newClass(
            moduleData = moduleData,
            name = classId.shortClassName.asString(),
            classId = classId,
        )
        private val secondDeclaration = if (ambiguousRawCandidates) {
            ExtendTestFixtures.newClass(
                moduleData = moduleData,
                name = classId.shortClassName.asString(),
                classId = classId,
            )
        } else {
            null
        }
        val symbolProvider = TestSymbolProvider(
            session = session,
            symbol = declaration.symbol,
            packageCallables = buildList {
                if (includeCorePrint) {
                    add(CfirNamedFunctionSymbol(CallableId(FqName("std.core"), Name.identifier("print"))))
                }
                if (includeNetMacro) {
                    add(CfirMacroDeclarationSymbol(CallableId(FqName("std.net"), Name.identifier("MacroType"))))
                }
            },
            additionalNamespaceSymbols = listOfNotNull(secondDeclaration?.symbol),
            organizationQualifiedNamespace = if (organizationQualifiedPackageAlias) {
                CfirImportNamespaceContext(FqName("bridge.net"), Name.identifier("org1"))
            } else {
                null
            },
        )
        val file = ExtendTestFixtures.newFile(moduleData, FqName("consumer"), emptyList())
        val resolver: CfirTypeResolverImpl

        init {
            session.register(
                CfirLanguageSettingsComponent::class,
                CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT),
            )
            session.register(CfirBuiltinTypes::class, CfirBuiltinTypes())
            session.register(
                CfirDeclarationAvailabilityProvider::class,
                CfirDeclarationAvailabilityProvider(session),
            )
            session.register(
                CfirDefaultImportsProviderHolder::class,
                CfirDefaultImportsProviderHolder.of(CfirDefaultImportsProvider),
            )
            session.register(CfirSymbolProvider::class, symbolProvider)
            session.register(CfirProvider::class, CfirLibrarySessionProvider(symbolProvider))
            session.register(CfirAccessibilityChecker::class, CfirAccessibilityChecker(session))
            val importBindings = CfirImportBindingStore()
            session.register(CfirImportBindingStore::class, importBindings)
            importBindings.record(
                file,
                if (organizationQualifiedPackageAlias) {
                    listOf(testOrganizationQualifiedPackageAlias())
                } else if (explicitlyBindStd || allUnderStd) {
                    listOf(testStdBinding(explicitlyBindStd, allUnderStd))
                } else {
                    emptyList()
                },
            )
            resolver = CfirTypeResolverImpl(session)
        }

        fun typeRef(vararg names: String) = buildUserTypeRef {
            val source = TestBinarySourceElement(names.joinToString("."))
            this.source = source
            qualifier += names.map { name ->
                buildQualifierPart {
                    this.source = source
                    this.name = Name.identifier(name)
                }
            }
        }

        fun resolve(typeRef: org.cangnova.cangjie.cfir.types.CfirTypeRef) = resolver.resolveType(
            typeRef = typeRef,
            configuration = TypeResolutionConfiguration(useSiteFile = file),
            areBareTypesAllowed = false,
            isOperandOfIsOperator = false,
            resolveDeprecations = true,
            supertypeSupplier = SupertypeSupplier.Default,
        )

        private fun testStdBinding(explicitlyBind: Boolean, allUnder: Boolean) = CfirResolvedImportBinding(
            importDirective = org.cangnova.cangjie.cfir.declarations.builder.buildImport {
                importedFqName = if (explicitlyBind) FqName("std.net.SockAddr") else FqName("std")
                isAllUnder = allUnder
                if (explicitlyBind) aliasName = Name.identifier("std")
            },
            effectiveName = Name.identifier("std"),
            targets = if (explicitlyBind) {
                listOf(CfirResolvedImportTarget.ClassLike(classId, declaration.symbol))
            } else {
                emptyList()
            },
            lookupOrigin = CfirLookupOrigin.EXPLICIT_IMPORT,
        )

        private fun testOrganizationQualifiedPackageAlias() = CfirResolvedImportBinding(
            importDirective = org.cangnova.cangjie.cfir.declarations.builder.buildImport {
                importedFqName = FqName("bridge")
                organizationName = Name.identifier("org1")
                aliasName = Name.identifier("network")
                isAllUnder = false
            },
            effectiveName = Name.identifier("network"),
            targets = listOf(
                CfirResolvedImportTarget.Package(
                    fqName = FqName("bridge"),
                    organizationName = Name.identifier("org1"),
                ),
            ),
            lookupOrigin = CfirLookupOrigin.EXPLICIT_IMPORT,
        )
    }

    private class TestSymbolProvider(
        session: org.cangnova.cangjie.cfir.session.CfirSession,
        private val symbol: CfirClassLikeSymbol<*>,
        private val packageCallables: List<CfirCallableSymbol<*>>,
        private val additionalNamespaceSymbols: List<CfirClassLikeSymbol<*>>,
        private val organizationQualifiedNamespace: CfirImportNamespaceContext?,
    ) : CfirSymbolProvider(session) {
        var lastImportNamespaceContext: CfirImportNamespaceContext? = null
            private set

        override fun getImportNamespace(context: CfirImportNamespaceContext): CfirImportNamespace {
            lastImportNamespaceContext = context
            val directNamespace = super.getImportNamespace(context)
            val namespaceSymbols = if (
                context.organizationName == null && context.packageFqName == symbol.classId.packageFqName
            ) {
                listOf(symbol) + additionalNamespaceSymbols
            } else {
                emptyList()
            }
            if (context != organizationQualifiedNamespace && namespaceSymbols.size < 2) return directNamespace
            return object : CfirImportNamespace by directNamespace {
                override fun hasPackage(includeSubpackages: Boolean): Boolean =
                    if (context == organizationQualifiedNamespace) true else directNamespace.hasPackage(includeSubpackages)

                override fun classifiers(name: Name): List<CfirClassLikeSymbol<*>> =
                    when {
                        context == organizationQualifiedNamespace && name == symbol.classId.shortClassName -> listOf(symbol)
                        context == organizationQualifiedNamespace -> emptyList()
                        namespaceSymbols.isNotEmpty() && name == symbol.classId.shortClassName -> namespaceSymbols
                        namespaceSymbols.isNotEmpty() -> emptyList()
                        else -> directNamespace.classifiers(name)
                    }
            }
        }

        override val symbolNamesProvider = object : CfirSymbolNamesProviderWithoutCallables() {
            override fun getPackageNames(): Set<String> =
                setOf(symbol.classId.packageFqName.asString()) + packageCallables.map { it.callableId.packageName.asString() }
            override val hasSpecificClassifierPackageNamesComputation: Boolean get() = true
            override fun getPackageNamesWithTopLevelClassifiers(): Set<String> =
                setOf(symbol.classId.packageFqName.asString())
            override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> =
                if (packageFqName == symbol.classId.packageFqName) setOf(symbol.classId.shortClassName) else emptySet()
        }

        override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
            symbol.takeIf { it.classId == classId }

        override fun getTopLevelCallableSymbolsTo(
            destination: MutableList<CfirCallableSymbol<*>>,
            packageFqName: FqName,
            name: Name,
        ) {
            destination += packageCallables.filter { it.callableId.packageName == packageFqName && it.name == name }
        }

        override fun getTopLevelFunctionSymbolsTo(
            destination: MutableList<CfirNamedFunctionSymbol>,
            packageFqName: FqName,
            name: Name,
        ) = Unit

        override fun getTopLevelPropertySymbolsTo(
            destination: MutableList<CfirPropertySymbol>,
            packageFqName: FqName,
            name: Name,
        ) = Unit

        override fun hasPackage(fqName: FqName): Boolean =
            fqName == symbol.classId.packageFqName || packageCallables.any { it.callableId.packageName == fqName }
    }

    private class TestBinarySourceElement(identity: String) : CjBinarySourceElement(
        debugText = identity,
        binaryFilePath = null,
        stableIdentity = identity,
    )
}
