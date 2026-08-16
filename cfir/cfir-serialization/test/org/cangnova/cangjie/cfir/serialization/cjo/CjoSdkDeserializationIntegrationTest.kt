package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.Package
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.expandedExtendTargetKey
import java.nio.ByteBuffer
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * 使用随测试资源携带的 SDK CJO fixture 验证真实包格式可被当前反序列化入口读取。
 */
class CjoSdkDeserializationIntegrationTest {
    /**
     * 验证 SDK CJO fixture 具有合法标识符、包名和不高于当前支持范围的 CJO 版本。
     */
    @Test
    fun `sdk cjo fixtures can be deserialized with expected version`() {
        val fixtures = listOf(
            "cjo-sdk/windows_x86_64_cjnative/std.cjo",
            "cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo",
            "cjo-sdk/windows_x86_64_cjnative/std/std.objectpool.cjo",
        )

        for (fixture in fixtures) {
            val bytes = resourceBytes(fixture)
            val byteBuffer = ByteBuffer.wrap(bytes)
            assertTrue(Package.PackageBufferHasIdentifier(byteBuffer), "fixture should have CJOF identifier: $fixture")

            val pkg = Package.getRootAsPackage(byteBuffer)
            val fullPkgName = pkg.fullPkgName ?: fail("missing fullPkgName in fixture: $fixture")
            val cjoVersion = pkg.cjoVersion ?: fail("missing cjoVersion in fixture: $fixture")
            assertTrue(fullPkgName.isNotBlank(), "fullPkgName should not be blank: $fixture")
            val fixtureVersion = Version(
                major = cjoVersion.majorNum.toUInt().toInt(),
                minor = cjoVersion.minorNum.toUInt().toInt(),
                patch = cjoVersion.patchNum.toUInt().toInt(),
            )
            val supportedVersion = Version(
                major = CjoConstants.VERSION_MAJOR,
                minor = CjoConstants.VERSION_MINOR,
                patch = CjoConstants.VERSION_PATCH,
            )
            assertTrue(
                fixtureVersion <= supportedVersion,
                "fixture version $fixtureVersion should not be newer than supported $supportedVersion: $fixture",
            )
        }
    }

    /**
     * 验证 CJO manager 能从标准 SDK 搜索路径布局中加载指定包头和完整包。
     */
    @Test
    fun `cjo manager loads sdk package in canonical search path layout`() {
        val fixture = "cjo-sdk/windows_x86_64_cjnative/std/std.objectpool.cjo"
        val bytes = resourceBytes(fixture)
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        val fullPkgName = pkg.fullPkgName ?: fail("missing fullPkgName in fixture: $fixture")

        val tempDir = Files.createTempDirectory("cjo-sdk-deserialize-")
        try {
            val target = tempDir.resolve(CjoConstants.packageNameToPath(fullPkgName))
            target.parent?.createDirectories()
            target.outputStream().use { it.write(bytes) }

            val manager = CjoManager(
                searchPath = CjoSearchPath(
                    envProvider = { key ->
                        when (key) {
                            "CANGJIE_STDLIB_MODULE" -> tempDir.toString()
                            else -> null
                        }
                    }
                )
            )

            assertTrue(manager.hasPackage(org.cangnova.cangjie.name.FqName(fullPkgName)))
            val header = assertNotNull(manager.loadPackageHeader(fullPkgName), "should load package header for $fullPkgName")
            assertEquals(fullPkgName, header.fullPkgName)
            assertTrue(
                header.topLevelClassNames.isNotEmpty() || header.topLevelCallableNames.isNotEmpty(),
                "expected at least one top-level declaration in $fullPkgName",
            )
            val loadedPackage = assertNotNull(manager.loadPackage(fullPkgName), "should load full package for $fullPkgName")
            assertEquals(fullPkgName, loadedPackage.fullPkgName)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    /**
     * 诊断：dump 真实 SDK std.core 包中所有顶层声明的 kind/isTopLevel，检查 extend 提取链路。
     */
    @Test
    fun `diagnose top level extend extraction in real sdk package`() {
        val fixture = "cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo"
        val bytes = resourceBytes(fixture)
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes))

        val byKind = linkedMapOf<UShort, Int>()
        val kindNames = mapOf(
            PackageFormat.DeclKind.InvalidDecl to "InvalidDecl",
            PackageFormat.DeclKind.ClassDecl to "ClassDecl",
            PackageFormat.DeclKind.InterfaceDecl to "InterfaceDecl",
            PackageFormat.DeclKind.FuncDecl to "FuncDecl",
            PackageFormat.DeclKind.PropDecl to "PropDecl",
            PackageFormat.DeclKind.VarDecl to "VarDecl",
            PackageFormat.DeclKind.VarWithPatternDecl to "VarWithPatternDecl",
            PackageFormat.DeclKind.FuncParam to "FuncParam",
            PackageFormat.DeclKind.StructDecl to "StructDecl",
            PackageFormat.DeclKind.EnumDecl to "EnumDecl",
            PackageFormat.DeclKind.ExtendDecl to "ExtendDecl",
            PackageFormat.DeclKind.TypeAliasDecl to "TypeAliasDecl",
            PackageFormat.DeclKind.GenericParamDecl to "GenericParamDecl",
            PackageFormat.DeclKind.BuiltInDecl to "BuiltInDecl",
        )
        var extTopLevel = 0
        var extNonTopLevel = 0
        var extTopLevelIdentified = 0
        val extSamples = mutableListOf<String>()
        for (index in 0 until pkg.allDeclsLength) {
            val decl = pkg.allDecls(index) ?: continue
            val kind = decl.kind
            byKind[kind] = (byKind[kind] ?: 0) + 1
            if (kind == PackageFormat.DeclKind.ExtendDecl) {
                if (decl.isTopLevel) {
                    extTopLevel++
                    if (!decl.identifier.isNullOrBlank()) extTopLevelIdentified++
                    if (extSamples.size < 5) {
                        extSamples += "index=$index isTopLevel=${decl.isTopLevel} id=${decl.identifier ?: "<null>"} type=${decl.type}"
                    }
                } else {
                    extNonTopLevel++
                }
            }
        }
        println("=== std.core allDecls kind histogram ===")
        byKind.forEach { (kind, count) -> println("kind=$kind (${kindNames[kind] ?: "?"}): $count") }
        println("ExtendDecl total=${extTopLevel + extNonTopLevel} (topLevel=$extTopLevel, nested=$extNonTopLevel)")
        println("extend top-level samples: $extSamples")
        println("extend with identifier among top-level: $extTopLevelIdentified")

        val header = CjoPackageHeader.fromPackage(pkg)
        println("topLevelExtendIndices = ${header.topLevelExtendIndices}")
    }

    /**
     * 诊断：对真实 SDK 包的顶层 extend 执行完整反序列化，检查 convertExtend 产出与 targetKey。
     */
    @Test
    fun `diagnose extend deserialization in real sdk package`() {
        val fixture = "cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo"
        val bytes = resourceBytes(fixture)
        val tempDir = Files.createTempDirectory("cjo-extend-diag-")
        try {
            val target = tempDir.resolve(CjoConstants.packageNameToPath("std.core"))
            target.parent?.createDirectories()
            target.outputStream().use { it.write(bytes) }

            val manager = CjoManager(
                CjoSearchPath { key ->
                    when (key) {
                        "CANGJIE_STDLIB_MODULE" -> tempDir.toString()
                        else -> null
                    }
                }
            )
            val header = assertNotNull(manager.loadPackageHeader("std.core"), "header")
            val pkg = assertNotNull(manager.loadPackage("std.core"), "pkg")
            val context = org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext(
                pkg = pkg,
                header = header,
                moduleData = DiagModuleData,
                cjoManager = manager,
            )

            val indices = header.topLevelExtendIndices
            var ok = 0
            var fail = 0
            val targetKeyCounts = linkedMapOf<String, Int>()
            for (index in indices) {
                try {
                    val decl = context.createDeclDeserializer().deserializeDecl(index)
                    if (decl is org.cangnova.cangjie.cfir.declarations.CfirExtend) {
                        ok++
                        val cone = decl.extendedTypeRef.coneTypeOrNull
                        val key = cone?.expandedExtendTargetKey
                        val keyText = key?.toString() ?: "<null>"
                        targetKeyCounts[keyText] = (targetKeyCounts[keyText] ?: 0) + 1
                        if (ok <= 5) {
                            println(
                                "EXTEND[$index] -> CfirExtend typeRefCone=$cone " +
                                    "targetKey=$key superTypes=${decl.superTypeRefs.size} members=${decl.declarations.size}",
                            )
                        }
                    } else {
                        fail++
                        println("EXTEND[$index] -> ${decl?.let { it::class.simpleName }} NOT CfirExtend")
                    }
                } catch (e: Throwable) {
                    fail++
                    println("EXTEND[$index] -> EXCEPTION ${e::class.simpleName}: ${e.message}")
                }
            }
            println("extend deserialize result: ok=$ok fail=$fail (total ${indices.size})")
            println("targetKey histogram:")
            targetKeyCounts.forEach { (key, count) -> println("  $key: $count") }
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    /**
     * 诊断测试使用的库模块数据 mock。
     */
    private object DiagModuleData : org.cangnova.cangjie.cfir.common.CfirModuleData() {
        override val name = org.cangnova.cangjie.name.Name.identifier("diag")
        override val dependencies: List<org.cangnova.cangjie.cfir.common.CfirModuleData> = emptyList()
        override val refinementDependencies: List<org.cangnova.cangjie.cfir.common.CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<org.cangnova.cangjie.cfir.common.CfirModuleData> = emptyList()
        override val targetPlatform = org.cangnova.cangjie.platform.CangJiePlatforms.defaultCangJiePlatform
        override val platform: org.cangnova.cangjie.cfir.common.CfirPlatform = org.cangnova.cangjie.cfir.common.CfirPlatform.DEFAULT
        override val isCommon: Boolean = false
        override val stableModuleName: String = "diag"
        override val session: org.cangnova.cangjie.cfir.session.CfirSession
            get() = DiagSession

        init {
            bindSession(DiagSession)
        }
    }

    /**
     * 诊断测试使用的库 session mock。
     */
    private object DiagSession : org.cangnova.cangjie.cfir.session.CfirSession(org.cangnova.cangjie.cfir.session.CfirSession.Kind.Library)

    /**
     * 诊断：验证 LLT 同构场景——deserialized provider + deserialized extend provider 查询 Int64。
     */
    @Test
    fun `diagnose deserialized extend provider query for Int64`() {
        val fixture = "cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo"
        val bytes = resourceBytes(fixture)
        val tempDir = Files.createTempDirectory("cjo-extend-diag-")
        try {
            // 拷贝真实 SDK 全部 cjo 到 tempDir，复现 LLT 多包场景
            val sdkRootUrl = javaClass.classLoader.getResource("cjo-sdk/windows_x86_64_cjnative")
                ?: fail("cjo-sdk root not on classpath")
            val sdkRoot = java.io.File(sdkRootUrl.toURI())
            if (sdkRoot.isDirectory) {
                sdkRoot.walkTopDown().filter { it.isFile && it.extension == "cjo" }.forEach { f ->
                    val rel = f.relativeTo(sdkRoot)
                    val target = tempDir.resolve(rel.path)
                    target.parent?.createDirectories()
                    target.outputStream().use { it.write(f.readBytes()) }
                }
            } else {
                // 单包 fallback
                val target = tempDir.resolve(CjoConstants.packageNameToPath("std.core"))
                target.parent?.createDirectories()
                target.outputStream().use { it.write(bytes) }
            }

            val manager = CjoManager(
                CjoSearchPath { key ->
                    when (key) {
                        "CANGJIE_STDLIB_MODULE" -> tempDir.toString()
                        else -> null
                    }
                }
            )
            val provider = org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider(
                session = DiagSession,
                cjoManager = manager,
                cangjieScopeProvider = org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider(),
                libraryModuleData = DiagModuleData,
            )
            DiagSession.register(org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider::class, provider)
            val extendProvider = org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedExtendProvider(listOf(provider))

            val packageNames = provider.symbolNamesProvider.getPackageNames()
            println("getPackageNames = $packageNames")

            val extends = extendProvider.getExtendsForBuiltinType(org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64)
            println("getExtendsForBuiltinType(INT64) size=${extends.size}")
            extends.take(10).forEach { e ->
                val cone = e.extendedTypeRef.coneTypeOrNull
                println("  extend: cone=${cone?.let { "${it::class.simpleName}: $it" }} " +
                    "key=${cone?.expandedExtendTargetKey} superTypes=${e.superTypeRefs.map { it.toString() }}")
            }

            val hashableClassId = org.cangnova.cangjie.name.ClassId(
                org.cangnova.cangjie.name.FqName("std.core"),
                org.cangnova.cangjie.name.Name.identifier("Hashable"),
            )
            val byClass = extendProvider.getExtendsForClass(hashableClassId)
            println("getExtendsForClass(std.core.Hashable) size=${byClass.size}")
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    /**
     * 验证共享装配入口 [CfirExtendProviderComposer] 的三种组合语义。
     *
     * 主编译器与 LL/IDE 侧共用该 composer 构造 session 级 extend 视图；这里用真实 SDK CJO
     * 证明：组合后的视图包含库中 extend 元数据，且惰性版本只在首次查询时求值一次。
     */
    @Test
    fun `extend provider composer combines own and deserialized extend sources`() {
        val fixture = "cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo"
        val bytes = resourceBytes(fixture)
        val tempDir = Files.createTempDirectory("cjo-composer-")
        try {
            // 拷贝真实 SDK 全部 cjo 到 tempDir，复现 LLT 多包场景（std.core 的 extend 目标类型依赖其他包）
            val sdkRootUrl = javaClass.classLoader.getResource("cjo-sdk/windows_x86_64_cjnative")
                ?: fail("cjo-sdk root not on classpath")
            val sdkRoot = java.io.File(sdkRootUrl.toURI())
            if (sdkRoot.isDirectory) {
                sdkRoot.walkTopDown().filter { it.isFile && it.extension == "cjo" }.forEach { f ->
                    val rel = f.relativeTo(sdkRoot)
                    val target = tempDir.resolve(rel.path)
                    target.parent?.createDirectories()
                    target.outputStream().use { it.write(f.readBytes()) }
                }
            } else {
                val target = tempDir.resolve(CjoConstants.packageNameToPath("std.core"))
                target.parent?.createDirectories()
                target.outputStream().use { it.write(bytes) }
            }

            val manager = CjoManager(
                CjoSearchPath { key ->
                    when (key) {
                        "CANGJIE_STDLIB_MODULE" -> tempDir.toString()
                        else -> null
                    }
                }
            )
            val provider = org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider(
                session = DiagSession,
                cjoManager = manager,
                cangjieScopeProvider = org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider(),
                libraryModuleData = DiagModuleData,
            )
            DiagSession.register(org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider::class, provider)
            val empty = org.cangnova.cangjie.cfir.resolve.providers.CfirEmptyExtendProvider()

            // 1. fromSymbolProviders：从符号 provider 树提取真实 SDK 的 extend 元数据
            val library = org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer
                .fromSymbolProviders(listOf(provider))
            val int64Extends = library.getExtendsForBuiltinType(org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64)
            assertTrue(int64Extends.isNotEmpty(), "std.core Int64 extends should be visible through composer")

            // 2. combine：仅 own 自身时直接返回原实例，不引入多余包装
            val same = org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer
                .combine(empty, listOf(empty))
            assertTrue(same === empty, "combine with only own provider should return it as is")

            // 3. combine：own 与依赖 provider 合并后查询结果等于依赖视图
            val combined = org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer
                .combine(empty, listOf(library))
            assertEquals(int64Extends, combined.getExtendsForBuiltinType(org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64))

            // 4. lazy 版本：providersRef 惰性求值、只求值一次、结果缓存
            var providerRefInvocations = 0
            val lazy = org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer
                .lazyFromSymbolProviders {
                    providerRefInvocations++
                    listOf(provider)
                }
            assertEquals(0, providerRefInvocations, "providersRef should be lazy until first query")
            assertEquals(int64Extends, lazy.getExtendsForBuiltinType(org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64))
            assertEquals(1, providerRefInvocations, "providersRef should be evaluated exactly once")
            assertEquals(int64Extends, lazy.getExtendsForBuiltinType(org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64))
            assertEquals(1, providerRefInvocations, "lazy result should be cached after first query")

            // 5. lazy 空降级：无 deserialized provider 时查询返回空结果
            val lazyEmpty = org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer
                .lazyFromSymbolProviders { emptyList() }
            assertTrue(lazyEmpty.getExtendsForBuiltinType(org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64).isEmpty())
            assertTrue(lazyEmpty.isExtendAccessible(int64Extends.first()), "empty provider should keep default accessibility")
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    /**
     * 从测试 classpath 读取指定 CJO fixture 字节。
     */
    private fun resourceBytes(path: String): ByteArray {
        return javaClass.classLoader.getResourceAsStream(path)?.use { it.readBytes() }
            ?: fail("fixture not found in test resources: $path (classpath lookup failed)")
    }

    /**
     * 用于比较 CJO fixture 版本和当前支持版本的语义化版本值。
     */
    private data class Version(
        /**
         * 主版本号。
         */
        val major: Int,
        /**
         * 次版本号。
         */
        val minor: Int,
        /**
         * 补丁版本号。
         */
        val patch: Int,
    ) : Comparable<Version> {
        /**
         * 按 major、minor、patch 顺序比较版本大小。
         */
        override fun compareTo(other: Version): Int {
            return compareValuesBy(this, other, Version::major, Version::minor, Version::patch)
        }
    }
}
