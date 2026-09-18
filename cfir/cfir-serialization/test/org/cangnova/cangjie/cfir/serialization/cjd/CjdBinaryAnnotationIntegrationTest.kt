package org.cangnova.cangjie.cfir.serialization.cjd

import PackageFormat.DeclKind
import PackageFormat.TypeKind
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.*
import org.cangnova.cangjie.cfir.serialization.deserialize.*
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
import org.cangnova.cangjie.name.*
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

/** 真正执行 binary -> CFIR publication；自包含 FlatBuffers 与磁盘 sidecar，不执行宏。 */
class CjdBinaryAnnotationIntegrationTest : CjParsingTestCase("", "cj.d", CangJieDeclarationFileType, CangJieParserDefinition()) {
    private lateinit var root: Path
    @BeforeEach fun setupFixture() { setUp(); root = Files.createTempDirectory("cjd-merge-") }
    @AfterEach fun teardownFixture() { root.toFile().deleteRecursively(); tearDown() }

    private fun context(text: String? = null): CfirDeserializationContext {
        val fixture = CjdBinaryFixture()
        val int = fixture.primitive(TypeKind.Int64)
        val param = fixture.declaration("x", DeclKind.FuncParam, int, topLevel = false)
        fixture.function("f", listOf(param))
        fixture.build()
        val path = root.resolve("actually-selected.cjo")
        Files.write(path, fixture.builder.sizedByteArray())
        val sidecar = CjdSidecarLocator.deriveCjdPath(path)
        if (text == null) Files.deleteIfExists(sidecar) else Files.writeString(sidecar, "package test.pkg\n$text")
        val manager = CjoManager(CjoSearchPath { if (it == "CANGJIE_LIBRARY") root.toString() else null })
        val loaded = kotlin.test.assertNotNull(manager.loadPackageSnapshot("test.pkg"))
        assertEquals(path.toAbsolutePath(), loaded.sourcePath)
        return CfirDeserializationContext(loaded.pkg, loaded.header, Module(), manager, loaded.sourcePath,
            setOf(FqName("ohos.labels.APILevel")))
    }

    @Test fun testMergedBeforePublicationAndCacheIsIdempotent() {
        val context = context("""
            @!APILevel[since: "22"]
            @!APILevel[since: "23"]
            @Frozen
            @OverflowWrapping
            @Deprecated[message: "old"]
            func f(@!APILevel[since: "24"] x: Int64): Unit
        """.trimIndent())
        val function = assertIs<CfirNamedFunction>(context.createDeclDeserializer().deserializeDecl(1))
        assertEquals(listOf("APILevel", "APILevel", "Frozen", "OverflowWrapping", "Deprecated"), function.annotations.map { it.annotationSourceName })
        assertEquals(CangjieAnnotationIdentity.SystemMacro(FqName("ohos.labels.APILevel"), "APILevel"), function.annotations.first().annotationIdentity)
        assertEquals("22", ((function.annotations.first() as CfirAnnotationCall).argumentMapping.mapping[Name.identifier("since")] as CfirLiteralExpression).value)
        assertEquals("22", CfirDeclarationAvailabilityProvider(context.moduleData.session).ownApiLevelInfo(function)?.since)
        assertTrue(function.interopInfo!!.isFrozen)
        assertEquals(CangjieOverflowStrategy.WRAPPING, function.interopInfo!!.overflowStrategy)
        assertNotSame(EmptyDeprecationsProvider, function.deprecationsProvider)
        val parameter = function.valueParameters.single()
        assertSame(parameter, context.declCache[0])
        assertEquals("APILevel", parameter.annotations.single().annotationSourceName)
        assertSame(parameter.symbol, (parameter.annotations.single() as CfirAnnotationCall).containingDeclarationSymbol)
        assertSame(function, context.createDeclDeserializer().deserializeDecl(1))
        assertEquals(5, function.annotations.size)
        assertTrue(context.sidecar!!.conversionDiagnostics.isEmpty(), context.sidecar!!.conversionDiagnostics.toString())
        assertTrue(context.sidecar!!.matchDiagnostics.isEmpty(), context.sidecar!!.matchDiagnostics.toString())
    }

    @Test fun testWholeContextRebuildAndBinarySignaturePreserved() {
        val firstContext = context("@Frozen\nfunc f(x: Int64): Unit")
        val first = assertIs<CfirNamedFunction>(firstContext.createDeclDeserializer().deserializeDecl(1))
        val secondContext = context("@!APILevel[since: \"25\"]\nfunc f(x: Int64): Unit")
        val second = assertIs<CfirNamedFunction>(secondContext.createDeclDeserializer().deserializeDecl(1))
        assertNotSame(first, second)
        assertTrue(first.interopInfo!!.isFrozen)
        assertFalse(second.interopInfo!!.isFrozen)
        assertEquals(first.name, second.name)
        assertEquals(first.status.visibility, second.status.visibility)
        assertEquals(first.valueParameters.single().returnTypeRef.coneTypeOrNull, second.valueParameters.single().returnTypeRef.coneTypeOrNull)
        assertNull(first.body)
        assertNull(second.body)
        assertEquals("Frozen", first.annotations.single().annotationSourceName)
    }

    @Test fun testAmbiguityAndAbsentSidecarDoNotMerge() {
        val ambiguous = context("@Frozen\nfunc f(x: Int64): Unit\n@Frozen\nfunc f(x: Int64): Unit")
        val declaration = ambiguous.createDeclDeserializer().deserializeDecl(1)!!
        assertTrue(declaration.annotations.isEmpty())
        assertTrue(ambiguous.sidecar!!.matchDiagnostics.any { it.kind == CjdBinaryMatchDiagnosticKind.AMBIGUOUS })
        val absent = context()
        assertTrue(absent.createDeclDeserializer().deserializeDecl(1)!!.annotations.isEmpty())
        assertNull(absent.sidecar!!.snapshot)
    }

    @Test fun testProviderCachesRemainIsolatedAcrossSidecarReplacement() {
        val original = context("@Frozen\nfunc f(x: Int64): Unit")
        fun provider(context: CfirDeserializationContext) = CfirDeserializedSymbolProvider(
            context.moduleData.session, context.cjoManager, CfirCangJieScopeProvider(), context.moduleData,
        ).also { context.moduleData.session.register(org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider::class, it) }
        fun function(provider: CfirDeserializedSymbolProvider) = provider.getTopLevelFunctionSymbols(
            FqName("test.pkg"), Name.identifier("f"),
        ).single().cfir
        val firstProvider = provider(original)
        val first = function(firstProvider)
        assertTrue(first.interopInfo!!.isFrozen)
        assertSame(first, function(firstProvider))

        // 旧 provider 的符号、名称和 sidecar 快照不局部刷新；新一代使用独立节点。
        val replacement = context("@Deprecated[message: \"replacement\"]\nfunc f(x: Int64): Unit")
        val secondProvider = provider(replacement)
        val second = function(secondProvider)
        assertSame(first, function(firstProvider))
        assertNotSame(first, second)
        assertNotSame(first.symbol, second.symbol)
        assertEquals(listOf("Frozen"), first.annotations.map { it.annotationSourceName })
        assertEquals(listOf("Deprecated"), second.annotations.map { it.annotationSourceName })
        assertFalse(second.interopInfo!!.isFrozen)
        assertNotSame(EmptyDeprecationsProvider, second.deprecationsProvider)
        assertEquals(first.valueParameters.single().returnTypeRef.coneTypeOrNull,
            second.valueParameters.single().returnTypeRef.coneTypeOrNull)
    }

    @Test fun testMembersAndParametersMergeDuringTheirOwnPublication() {
        val fixture = CjdBinaryFixture()
        val int = fixture.primitive(TypeKind.Int64)
        val parameter = fixture.declaration("x", DeclKind.FuncParam, int, topLevel = false)
        val member = fixture.function("f", listOf(parameter), topLevel = false)
        val owner = fixture.classDecl("Box", listOf(member))
        fixture.build()
        val path = root.resolve("nested.cjo")
        Files.write(path, fixture.builder.sizedByteArray())
        Files.writeString(CjdSidecarLocator.deriveCjdPath(path), """
            package test.pkg
            @Frozen
            class Box {
                @Deprecated[message: "member"]
                func f(@Frozen x: Int64): Unit
            }
        """.trimIndent())
        val manager = CjoManager(CjoSearchPath { if (it == "CANGJIE_LIBRARY") root.toString() else null })
        val loaded = kotlin.test.assertNotNull(manager.loadPackageSnapshot("test.pkg"))
        val context = CfirDeserializationContext(loaded.pkg, loaded.header, Module(), manager, loaded.sourcePath)
        val box = assertIs<CfirClass>(context.createDeclDeserializer().deserializeDecl(owner.toInt() - 1))
        val function = assertIs<CfirNamedFunction>(box.declarations.single())
        assertTrue(box.interopInfo!!.isFrozen)
        assertSame(function, context.declCache[member.toInt() - 1])
        assertNotSame(EmptyDeprecationsProvider, function.deprecationsProvider)
        val valueParameter = function.valueParameters.single()
        assertSame(valueParameter, context.declCache[parameter.toInt() - 1])
        assertEquals("Frozen", valueParameter.annotations.single().annotationSourceName)
        assertSame(valueParameter.symbol, (valueParameter.annotations.single() as CfirAnnotationCall).containingDeclarationSymbol)
        assertEquals(emptyList<CjdBinaryMatchDiagnostic>(), context.sidecar!!.matchDiagnostics)
    }

    /** 只读真实 SDK，走生产 provider 的默认 sidecar 定位与系统注解识别。 */
    @Test fun testConfiguredSdkRoundingModeAvailability() {
        val directory = System.getenv("CANGJIE_CJD_SDK_DIR")
        org.junit.jupiter.api.Assumptions.assumeTrue(!directory.isNullOrBlank(),
            "Set CANGJIE_CJD_SDK_DIR to enable real SDK validation")
        val manager = CjoManager(CjoSearchPath {
            if (it == "CANGJIE_STDLIB_MODULE") directory else null
        })
        val module = Module()
        val provider = CfirDeserializedSymbolProvider(module.session, manager, CfirCangJieScopeProvider(), module)
        module.session.register(org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider::class, provider)
        val classId = ClassId.topLevel(FqName("std.math.RoundingMode"))
        val declaration = kotlin.test.assertNotNull(provider.getClassLikeSymbolByClassId(classId)).cfir
        val annotation = assertIs<CfirAnnotationCall>(declaration.annotations.single { it.annotationSourceName == "APILevel" })
        assertEquals(CangjieAnnotationIdentity.SystemMacro(FqName("ohos.labels.APILevel"), "APILevel"), annotation.annotationIdentity)
        assertEquals("22", CfirDeclarationAvailabilityProvider(module.session).ownApiLevelInfo(declaration)?.since)
        val enum = assertIs<CfirEnum>(declaration)
        val names = setOf("Ceiling", "Down", "Floor", "HalfEven", "HalfUp", "Up", "toString")
        val members = enum.declarations.filterIsInstance<CfirCallableDeclaration>().filter { it.symbol.callableId.callableName.asString() in names }
        assertEquals(names, members.mapTo(linkedSetOf()) { it.symbol.callableId.callableName.asString() })
        for (member in members) {
            kotlin.test.assertEquals("22", CfirDeclarationAvailabilityProvider(module.session).ownApiLevelInfo(member)?.since,
                member.symbol.callableId.toString())
        }
        assertSame(declaration, provider.getClassLikeSymbolByClassId(classId)!!.cfir)
    }

    /** 只读真实 CJO 参数元数据，固化 Byte 别名对应的未知类型边界。 */
    @Test fun testConfiguredSdkConsoleByteAliasBoundary() {
        val directory = System.getenv("CANGJIE_CJD_SDK_DIR")
        org.junit.jupiter.api.Assumptions.assumeTrue(!directory.isNullOrBlank(),
            "Set CANGJIE_CJD_SDK_DIR to enable real SDK validation")
        val manager = CjoManager(CjoSearchPath { if (it == "CANGJIE_STDLIB_MODULE") directory else null })
        val loaded = kotlin.test.assertNotNull(manager.loadPackageSnapshot("std.console"))
        val context = CfirDeserializationContext(loaded.pkg, loaded.header, Module(), manager, loaded.sourcePath)
        val sidecar = CjdSidecarParser.create().parse(CjdSidecarLocator.deriveCjdPath(loaded.sourcePath))
        assertTrue(sidecar.isUsable)
        val matcher = CjdBinaryDeclarationMatcher.create(context, sidecar)
        val missing = matcher.diagnostics.filter { it.sourceRange != null }
        assertEquals(3, missing.size)
        assertTrue(missing.all { it.kind == CjdBinaryMatchDiagnosticKind.MISSING })
        val missingRanges = missing.map { it.sourceRange }.toSet()
        val aliasArray = TypeKey.Ref("Array", listOf(TypeKey.Ref("Byte")))
        val entries = buildList {
            fun collect(entry: CjdDeclarationEntry) {
                if (entry.range in missingRanges) add(entry)
                entry.members.forEach(::collect)
            }
            sidecar.declarations.forEach(::collect)
        }
        assertEquals(setOf("read", "write", "writeln"), entries.map { it.key.identifier }.toSet())
        entries.forEach { entry ->
            assertEquals(aliasArray, assertIs<DeclarationMatchKey.Function>(entry.key).parameters.single().type)
        }
        val types = CjdBinaryTypeAdapter.create(context)
        val binaryNames = mutableListOf<String>()
        for (index in 0 until loaded.pkg.allDeclsLength) {
            val declaration = loaded.pkg.allDecls(index) ?: continue
            if (declaration.identifier !in entries.map { it.key.identifier } ||
                declaration.infoType != PackageFormat.DeclInfo.FuncInfo) continue
            val info = declaration.info(PackageFormat.FuncInfo()) as PackageFormat.FuncInfo
            val parameters = info.funcBody?.paramLists(0) ?: continue
            if (parameters.paramsLength != 1) continue
            val parameter = loaded.pkg.allDecls(parameters.params(0).toInt() - 1)!!
            if (parameter.identifier !in setOf("arr", "buffer")) continue
            val rawArray = loaded.pkg.allTypes(parameter.type.toInt() - 1)!!
            assertEquals(1, rawArray.typeArgsLength)
            val rawElement = loaded.pkg.allTypes(rawArray.typeArgs(0).toInt() - 1)!!
            // 实际 SDK 保留 Type 元数据，不是展开后的 UInt8；未知类型不能当作通配符。
            assertEquals(TypeKind.Type, rawElement.kind)
            val array = assertIs<CjdResolvedType.Named>(types.typeFromField(parameter.type))
            assertEquals("Array", array.name)
            assertIs<CjdResolvedType.Unknown>(array.arguments.single())
            assertFalse(aliasArray.matchesResolved(array))
            assertFalse(TypeKey.Ref("Array", listOf(TypeKey.primitive("UInt8"))).matchesResolved(array))
            assertNull(matcher.selection(index))
            binaryNames += declaration.identifier!!
        }
        assertEquals(listOf("read", "write", "writeln"), binaryNames)
    }

    /** 全量只读匹配统计；源码缺项单独报告，不把二进制内部声明无 sidecar 当作覆盖失败。 */
    @Test fun testConfiguredSdkMatchCoverage() {
        val directory = System.getenv("CANGJIE_CJD_SDK_DIR")
        org.junit.jupiter.api.Assumptions.assumeTrue(!directory.isNullOrBlank(),
            "Set CANGJIE_CJD_SDK_DIR to enable real SDK validation")
        val manager = CjoManager(CjoSearchPath { if (it == "CANGJIE_STDLIB_MODULE") directory else null })
        val paths = Files.list(Path.of(directory!!)).use { stream ->
            stream.filter { it.toString().endsWith(".cj.d") }.sorted().toList()
        }
        assertEquals(38, paths.size)
        var matchedAnnotations = 0
        for (path in paths) {
            val sidecar = CjdSidecarParser.create().parse(path)
            assertTrue(sidecar.isUsable, "$path: ${sidecar.diagnostics}")
            val loaded = kotlin.test.assertNotNull(manager.loadPackageSnapshot(sidecar.annotationContext.packageFqName))
            val context = CfirDeserializationContext(loaded.pkg, loaded.header, Module(), manager, loaded.sourcePath)
            val matcher = CjdBinaryDeclarationMatcher.create(context, sidecar)
            val selections = (0 until loaded.pkg.allDeclsLength).mapNotNull(matcher::selection)
            val count = selections.sumOf { it.annotations.count { annotation -> annotation.name == "APILevel" } }
            matchedAnnotations += count
            val sourceIssues = matcher.diagnostics.filter { it.sourceRange != null }
            assertTrue(matcher.diagnostics.none { it.kind == CjdBinaryMatchDiagnosticKind.AMBIGUOUS },
                "$path: ${matcher.diagnostics.filter { it.kind == CjdBinaryMatchDiagnosticKind.AMBIGUOUS }}")
            println("${path.fileName}: APILevel=$count, sourceIssues=${sourceIssues.groupingBy { it.kind }.eachCount()}")
            sourceIssues.take(12).forEach { println("  $it") }
        }
        println("SDK matched APILevel=$matchedAnnotations")
        assertTrue(matchedAnnotations > 0)
    }

    private class Module : CfirModuleData() {
        override val name = Name.identifier("cjd-test")
        override val dependencies = emptyList<CfirModuleData>()
        override val refinementDependencies = emptyList<CfirModuleData>()
        override val allRefinementDependencies = emptyList<CfirModuleData>()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "cjd-test"
        override val session = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        }
        init { bindSession(session) }
    }
}
