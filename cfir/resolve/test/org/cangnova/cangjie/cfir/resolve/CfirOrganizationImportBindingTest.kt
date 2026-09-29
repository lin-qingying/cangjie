@file:OptIn(org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.resolve.providers.*
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangnova.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证组织限定查询不会混入另一组织或无组织库的同名声明。 */
class CfirOrganizationImportBindingTest {
    @Test
    fun `binding cache does not merge the same path across organizations`() {
        val (_, module) = ExtendTestFixtures.newSessionAndModule()
        val file = ExtendTestFixtures.newFile(module, FqName("consumer"), emptyList())
        val bindings = listOf("org1", "org2").map { organization ->
            val organizationName = Name.identifier(organization)
            CfirResolvedImportBinding(
                importDirective = buildImport {
                    importedFqName = FqName("shared")
                    this.organizationName = organizationName
                    isAllUnder = true
                },
                effectiveName = Name.identifier("shared"),
                targets = listOf(CfirResolvedImportTarget.Package(FqName("shared"), organizationName)),
                lookupOrigin = CfirLookupOrigin.EXPLICIT_IMPORT,
            )
        }
        val store = CfirImportBindingStore()
        store.record(file, bindings)
        assertEquals(bindings, store.requireBindings(file).imports)
    }

    @Test
    fun `organization qualifier isolates canonical library identities`() {
        val fixture = Fixture()
        for (organization in listOf("org1", "org2")) {
            val targets = fixture.resolve("shared.Box", organization)
            assertEquals(listOf("shared@$organization"), targets.filterIsInstance<CfirResolvedImportTarget.ClassLike>()
                .map { it.classId.packageFqName.asString() })
            assertTrue(targets.none { it is CfirResolvedImportTarget.Package }, "成员导入不能同时解析到父包")
        }
        assertTrue(fixture.resolve("shared.Box", "missing").isEmpty())
        assertEquals(listOf("shared"), fixture.resolve("shared.Box", null)
            .filterIsInstance<CfirResolvedImportTarget.ClassLike>().map { it.classId.packageFqName.asString() })
    }

    @Test
    fun `organization package prefix uses organization after full package name`() {
        val fixture = Fixture()
        assertTrue(fixture.resolve("nested", "org1", allUnder = true).isEmpty())
        assertEquals(listOf("nested@org1"), fixture.resolve("nested", "org1", allUnder = true, allowPackagePrefix = true)
            .filterIsInstance<CfirResolvedImportTarget.Package>().map { it.fqName.asString() })
        assertTrue(fixture.resolve("nested", "org2", allUnder = true).isEmpty())
    }

    /** 只提供已声明的库包和符号，源码 provider 为空，确保查询不借用普通包结果。 */
    private class Fixture {
        private val sessionAndModule = ExtendTestFixtures.newSessionAndModule()
        private val session = sessionAndModule.first
        private val classes = listOf("shared@org1", "shared@org2", "shared", "nested.deep@org1").associate { pkg ->
            val id = ClassId(FqName(pkg), Name.identifier("Box"))
            id to ExtendTestFixtures.newClass(sessionAndModule.second, "Box", id).symbol
        }
        private val packages = classes.keys.map { it.packageFqName }.toSet()
        private val symbols = object : CfirSymbolProvider(session) {
            override val symbolNamesProvider = object : CfirSymbolNamesProvider() {
                override val hasSpecificClassifierPackageNamesComputation = false
                override val hasSpecificCallablePackageNamesComputation = false
                override fun getPackageNames(): Set<String> = packages.map { it.asString() }.toSet()
                override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> =
                    classes.keys.filter { it.packageFqName == packageFqName }.map { it.shortClassName }.toSet()
                override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()
            }
            override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? = classes[classId]
            override fun hasPackage(fqName: FqName): Boolean = fqName in packages
            override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {}
            override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {}
            override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {}
        }
        init {
            session.register(CfirSymbolProvider::class, symbols)
            session.register(CfirProvider::class, object : CfirProvider() {
                override val symbolProvider: CfirSymbolProvider = symbols
                override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? = null
                override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile = error("Library symbols have no source file")
                override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? = null
                override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? = null
                override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> = emptyList()
                override fun getClassNamesInPackage(fqName: FqName): Set<Name> = emptySet()
            })
        }

        fun resolve(path: String, organization: String?, allUnder: Boolean = false, allowPackagePrefix: Boolean = false): List<CfirResolvedImportTarget> =
            session.resolveImportBinding(buildImport {
                importedFqName = FqName(path)
                organizationName = organization?.let(Name::identifier)
                isAllUnder = allUnder
            }, CfirLookupOrigin.EXPLICIT_IMPORT, allowPackagePrefix).targets
    }
}
