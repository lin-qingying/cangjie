package org.cangnova.cangjie.cfir.serialization.deserialize

import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.serialization.provider.CfirClassLikeSymbolLookup
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.test.assertTrue

/**
 * `.cjo` 反序列化期间的 class-like 符号解析只能来自 `.cjo` 头部与 flatbuffer。
 *
 * `.cjo` 的 stub 树由 `StubUpdatingIndex` 在索引期构建，若解析器经 session 的符号提供器取符号，IDE 的 STUBS 来源下会再进 stub 索引，
 * 触发平台的 `Indexing process should not rely on non-indexed file data`。本测试的 session **不注册任何符号提供器**：
 * 一旦仍有任何一处类符号查找经过 session，就会抛 `No 'CfirSymbolProvider' in array`；同时用计数查找断言这条路径确实被走到，
 * 避免"因为从未查找而通过"。
 *
 * Kotlin 对应：`fir-deserialization` 全模块零 `com.intellij` 导入，库类解析只用元数据。
 */
class CjoDeserializationSymbolLookupIsolationTest :
    CjParsingTestCase("", "cj.d", CangJieDeclarationFileType, CangJieParserDefinition()) {

    private lateinit var fixtureRoot: Path

    @BeforeEach fun setupFixture() { setUp(); fixtureRoot = locateStdlibFixtureRoot() }
    @AfterEach fun teardownFixture() { tearDown() }

    @Test
    fun testStdCoreDeserializesWithoutSessionSymbolProvider() {
        assertDeserializesWithoutSessionProvider("std.core", minDeclarations = 100)
    }

    @Test
    fun testStdUnittestDeserializesWithoutSessionSymbolProvider() {
        assertDeserializesWithoutSessionProvider("std.unittest", minDeclarations = 10)
    }

    private fun assertDeserializesWithoutSessionProvider(packageName: String, minDeclarations: Int) {
        val manager = CjoManager(
            CjoSearchPath { key ->
                when (key) {
                    "CANGJIE_STDLIB_MODULE", "CANGJIE_LIBRARY" -> fixtureRoot.toString()
                    else -> null
                }
            },
        )
        val loaded = kotlin.test.assertNotNull(
            manager.loadPackageSnapshot(packageName),
            "fixture must contain $packageName",
        )
        val counting = CountingLookup(manager.deserializedSymbolLookups.forModuleData(IsolationModuleData))
        val context = CfirDeserializationContext(
            pkg = loaded.pkg,
            header = loaded.header,
            moduleData = IsolationModuleData,
            cjoManager = manager,
            classSymbolLookup = counting,
            sourcePath = loaded.sourcePath,
        )
        val indices = buildList {
            loaded.header.topLevelNameToIndices.values.forEach(::addAll)
            addAll(loaded.header.topLevelExtendIndices)
        }.distinct().sorted()
        val declDeserializer = context.createDeclDeserializer()
        val declarations = mutableListOf<CfirDeclaration>()
        for (index in indices) {
            val cached = context.declCache[index]
            if (cached != null) { declarations.add(cached); continue }
            declDeserializer.deserializeDecl(index)?.let { declarations.add(it) }
        }

        assertTrue(
            declarations.size >= minDeclarations,
            "$packageName must deserialize at least $minDeclarations declarations, got ${declarations.size}",
        )
        assertTrue(
            counting.count > 0,
            "$packageName must exercise the class-like symbol lookup path; the fixture no longer covers it",
        )
    }

    /** 统计注入的查找入口被调用的次数，委托到 manager 共享的反序列化 provider。 */
    private class CountingLookup(private val delegate: CfirClassLikeSymbolLookup) : CfirClassLikeSymbolLookup {
        var count = 0
        override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
            count++
            return delegate.getClassLikeSymbolByClassId(classId)
        }
    }

    /** 只注册语言设置与 scope provider，**不注册** `CfirSymbolProvider`。 */
    private object IsolationModuleData : CfirModuleData() {
        override val name = Name.identifier("cjo-isolation")
        override val dependencies = emptyList<CfirModuleData>()
        override val refinementDependencies = emptyList<CfirModuleData>()
        override val allRefinementDependencies = emptyList<CfirModuleData>()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "cjo-isolation"
        override val session = object : CfirSession(CfirSession.Kind.Library) {
            init {
                register(
                    CfirLanguageSettingsComponent::class,
                    CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT),
                )
                register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
            }
        }
        init { bindSession(session) }
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
