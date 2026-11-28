package org.cangnova.cangjie.cfir.serialization.deserialize

import org.cangnova.cangjie.test.bindCjoFixture
import PackageFormat.FullId
import PackageFormat.DeclKind
import com.google.flatbuffers.FlatBufferBuilder
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirBuiltInDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.serialization.cjo.PackageIndex
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
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
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull as assertValue
import kotlin.test.assertSame

/**
 * `.cjo` 反序列化期间的 class-like 符号解析只能来自 `.cjo` 头部与 flatbuffer。
 *
 * `.cjo` 的 stub 树由 `StubUpdatingIndex` 在索引期构建，若解析器经 session 的符号提供器取符号，IDE 的 STUBS 来源下会再进 stub 索引，
 * 触发平台的 `Indexing process should not rely on non-indexed file data`。本测试的 session **不注册任何符号提供器**：
 * 一旦仍有任何一处类符号查找经过 session，就会抛 `No 'CfirSymbolProvider' in array`。具名声明通过正式 CURRENT FullId 入口读取，
 * 只统计实际返回的 class-like 符号，并核对 canonical record、ClassId、模块和完成阶段，避免"因为从未查找而通过"。
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
        val metadataBinding = bindCjoFixture(IsolationModuleData, manager, setOf(fixtureRoot))
        val provider = manager.deserializedSymbolLookups.forBinding(metadataBinding, IsolationModuleData, CjoDeserializationMode.FULL)
        val context = assertValue(provider.getPackageContext(packageName, CjoDeserializationMode.FULL))
        assertSame(context, context.canonical())
        assertSame(IsolationModuleData, context.moduleData)
        assertEquals(loaded.sourcePath, context.sourcePath)
        val counting = CountingLookup(context)
        val indices = buildList {
            loaded.header.topLevelNameToIndices.values.forEach(::addAll)
            addAll(loaded.header.topLevelExtendIndices)
        }.distinct().sorted()
        val declarations = mutableListOf<CfirDeclaration>()
        for (index in indices) {
            declarations.add(counting.deserializeDecl(index))
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

    /**
     * 正式 FullId 读取就是 fixture 的声明读取工作，不另发 class-id 请求制造覆盖。
     * count 是该工作实际返回的 class-like 符号数；缓存命中同样核对规范 wire record。
     */
    private class CountingLookup(private val context: CfirDeserializationContext) {
        var count = 0
            private set

        fun deserializeDecl(declIndex: Int): CfirDeclaration {
            val declaration = assertValue(context.pkg.allDecls(declIndex))
            val symbol = if (declaration.kind == DeclKind.ExtendDecl) {
                // extend 沿原声明树入口读取，不能把可能无名称的容器改成 named FullId 查询。
                assertValue(context.createDeclDeserializer().deserializeDecl(declIndex)).symbol
            } else {
                val builder = FlatBufferBuilder()
                val fullIdOffset = FullId.createFullId(builder, PackageIndex.CURRENT.value, 0, (declIndex + 1).toUInt())
                builder.finish(fullIdOffset)
                val target = FullId.getRootAsFullId(builder.dataBuffer())
                context.fullIdResolver.resolveSymbol(target)
            }
            val record = assertValue(context.declCache[declIndex], "FullId must return a complete canonical declaration")
            assertSame(record.wireSymbol, symbol, "FullId must keep the exact wire symbol")
            assertSame(context.moduleData, symbol.cfir.moduleData, "FullId must keep the actual module owner")
            assertEquals(CfirResolvePhase.BODY_RESOLVE, record.declarationRoot.resolvePhase)
            assertEquals(CfirResolvePhase.BODY_RESOLVE, symbol.cfir.resolvePhase)
            if (declaration.kind in setOf(DeclKind.ClassDecl, DeclKind.InterfaceDecl, DeclKind.StructDecl,
                    DeclKind.EnumDecl, DeclKind.TypeAliasDecl, DeclKind.BuiltInDecl)) {
                assertIs<CfirClassLikeDeclaration>(record.declarationRoot)
                val classLike = assertIs<CfirClassLikeSymbol<*>>(symbol)
                assertTrue(classLike.isBound)
                assertSame(record.declarationRoot, classLike.cfir)
                val expectedClassId = if (declaration.kind == DeclKind.BuiltInDecl) {
                    assertIs<CfirBuiltInDeclaration>(record.declarationRoot).kind.classId
                } else {
                    ClassId(FqName(context.header.fullPkgName), Name.identifier(assertValue(declaration.identifier)))
                }
                assertEquals(
                    expectedClassId,
                    classLike.classId,
                    "Class-like identity must come from this exact CURRENT wire declaration",
                )
                count++
            }
            return record.declarationRoot
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
