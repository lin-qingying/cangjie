package org.cangnova.cangjie.analysis.decompiler.stub

import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.resolve.providers.CfirNullSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * `.cjo` 反序列化不得依赖 session 的符号提供器。
 *
 * `.cjo` 的 stub 树由 `StubUpdatingIndex` 在索引期构建，此时 session provider 在 IDE 的 STUBS 来源下会去读另一个 `.cjo` 的 green stub，
 * 触发平台的 `Indexing process should not rely on non-indexed file data`。本测试在 session 上注册一个只会在
 * `getClassLikeSymbolByClassId` 上抛异常的 provider；若反序列化器仍有任何一处类符号查找经过 session，测试会以具体 `ClassId` 失败。
 */
class CjoDeclarationLoaderSymbolProviderIsolationTest :
    CjParsingTestCase("", "cj.d", CangJieDeclarationFileType, CangJieParserDefinition()) {

    private lateinit var fixtureRoot: Path

    @BeforeEach
    fun setupFixture() {
        setUp()
        fixtureRoot = locateStdlibFixtureRoot()
        assertTrue(
            fixtureRoot.resolve("std").isDirectory(),
            "missing stdlib `.cjo` fixture root: $fixtureRoot",
        )
    }

    @AfterEach
    fun teardownFixture() {
        tearDown()
    }

    @Test
    fun testDeserializationNeverUsesSessionSymbolProvider() {
        val packageFqName = FqName("std.core")
        val manager = CjoManager(
            CjoSearchPath { key ->
                when (key) {
                    "CANGJIE_STDLIB_MODULE", "CANGJIE_LIBRARY" -> fixtureRoot.toString()
                    else -> null
                }
            },
        )
        val loaded = assertNotNull(
            manager.loadPackageSnapshot(packageFqName.asString()),
            "fixture must contain $packageFqName",
        )
        val input = LoadedCjoPackage(
            binaryFile = LightVirtualFile("std.core.cjo"),
            packageFqName = packageFqName,
            pkg = loaded.pkg,
            header = loaded.header,
            cjoManager = manager,
            isVersionSupported = true,
            sourcePath = loaded.sourcePath,
        )
        val module = Module()

        val declarations = CjoDeclarationLoader.loadDeclarations(input, module)
        assertTrue(declarations.isNotEmpty(), "fixture package must deserialize into declarations")

        val fileStub = CjoFileStubBuilder.buildFileStub(input, module)
        val kind = fileStub.kind
        assertTrue(
            kind !is CangJieFileStubKind.Invalid,
            "stub tree must be built from real declarations, but got: $kind",
        )
    }

    /** 会在被反序列化器查询时直接失败的 provider。 */
    @OptIn(CfirSymbolProviderInternals::class)
    private class TripwireSymbolProvider(session: CfirSession) : CfirSymbolProvider(session) {
        override val symbolNamesProvider: CfirSymbolNamesProvider = CfirNullSymbolNamesProvider

        override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*> =
            throw AssertionError("Deserialization must not use the session symbol provider for $classId")

        override fun getTopLevelCallableSymbolsTo(
            destination: MutableList<CfirCallableSymbol<*>>,
            packageFqName: FqName,
            name: Name,
        ) {
        }

        override fun getTopLevelFunctionSymbolsTo(
            destination: MutableList<CfirNamedFunctionSymbol>,
            packageFqName: FqName,
            name: Name,
        ) {
        }

        override fun getTopLevelPropertySymbolsTo(
            destination: MutableList<CfirPropertySymbol>,
            packageFqName: FqName,
            name: Name,
        ) {
        }

        override fun hasPackage(fqName: FqName): Boolean = false
    }

    private class Module : CfirModuleData() {
        override val name = Name.identifier("cjo-isolation-test")
        override val dependencies = emptyList<CfirModuleData>()
        override val refinementDependencies = emptyList<CfirModuleData>()
        override val allRefinementDependencies = emptyList<CfirModuleData>()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "cjo-isolation-test"
        override val session = object : CfirSession(CfirSession.Kind.Library) {
            init {
                register(
                    CfirLanguageSettingsComponent::class,
                    CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT),
                )
                register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
                register(CfirSymbolProvider::class, TripwireSymbolProvider(this))
            }
        }
        init {
            bindSession(session)
        }
    }

    private fun locateStdlibFixtureRoot(): Path {
        val start = Paths.get("").toAbsolutePath().normalize()
        val root = generateSequence(start) { current -> current.parent }
            .firstOrNull { candidate -> candidate.resolve("settings.gradle.kts").isRegularFile() }
            ?: error("Cannot locate repository root from $start")
        return root.resolve("cfir").resolve("cfir-serialization").resolve("testResources")
            .resolve("cjo-sdk").resolve("windows_x86_64_cjnative")
    }
}
