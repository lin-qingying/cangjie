package org.cangnova.cangjie.analysis.decompiled.psi.text

import PackageFormat.Package as CjoPackage
import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.analysis.decompiler.stub.CjoDeclarationLoader
import org.cangnova.cangjie.analysis.decompiler.stub.CjoFileStubBuilder
import org.cangnova.cangjie.analysis.decompiler.stub.LoadedCjoPackage
import org.cangnova.cangjie.cfir.analysis.tests.AbstractCfirAnalysisResolveTest
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.serialization.cjo.CfirCjoPackageMetadataProducer
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

/**
 * typealias 在 `.cjo` 写入 → 读入 → 反编译三个环节都必须保持别名身份。
 *
 * 锁定的框架契约：
 * 1. 写出：`ConeTypeAliasType` 写成 `TypeKind.Type`（官方 `SaveNominalTy`），不再 `unsupported`；
 * 2. 读入：`TypeKind.Type` 恢复为 `ConeTypeAliasType`（别名 ClassId + use-site 实参），
 *    同包非泛型别名的 `expandedType` 内联为 RHS；泛型/跨包由 use-site 惰性展开；
 * 3. 反编译：文本渲染为别名限定名，而不是 `typealias std.core.X` 或 `Any`。
 */
class TypeAliasCjoRoundTripTest : AbstractCfirAnalysisResolveTest() {
    /** 是否关闭 typealias 展开（用于分别覆盖解析期保留别名 / 解析期展开两条流水线）。 */
    private var disableAliasExpansion: Boolean = true

    /**
     * 关闭展开时声明签名直接保留 `ConeTypeAliasType`；默认设置下解析会展开为底层类型
     * （并保留 `AbbreviatedTypeAttribute`），写出侧仍须还原别名身份。
     */
    override val sessionLanguageVersionSettings: LanguageVersionSettings
        get() = if (disableAliasExpansion) {
            LanguageVersionSettingsImpl(
                languageVersion = LanguageVersion.LATEST_STABLE,
                analysisFlags = mapOf(AnalysisFlags.expandTypeAliasesInTypeResolution to false),
            )
        } else {
            LanguageVersionSettingsImpl.DEFAULT
        }

    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    /**
     * 关闭展开时：声明签名保留 `ConeTypeAliasType`，经 `.cjo` 写出、读入、反编译后仍是别名类型。
     */
    @Test
    fun typeAliasSurvivesCjoRoundTripAndDecompilesToItsName() {
        disableAliasExpansion = true
        assertAliasSurvivesRoundTrip()
    }

    /**
     * 默认流水线（解析期展开、保留 `AbbreviatedTypeAttribute`）下，写出侧仍必须写回别名身份。
     *
     * 官方 `ASTWriter` 写的是 `ObtainsAliasType(&node)`，解析展开只是中间态；若不看缩写属性，
     * `Array<Byte>` 会落盘成 `Array<UInt8>`，与官方 `.cjo` 不等价。
     */
    @Test
    fun typeAliasSurvivesCjoRoundTripUnderDefaultExpansionPipeline() {
        disableAliasExpansion = false
        assertAliasSurvivesRoundTrip()
    }

    /** 两条流水线共用的往返断言。 */
    private fun assertAliasSurvivesRoundTrip() {
        val source = """
            package roundtrip
            type MyInt = Int64
            public class Holder {
                public func take(x: MyInt): MyInt {
                    return x
                }
            }
        """.trimIndent()
        val file = createCjFile("roundTrip", source)

        val (cfirFile, loaded) = serializeAndLoad(file)

        // 写出侧：别名声明本身可序列化，且源文件里别名类型被识别为 alias 类型。
        val sourceAlias = cfirFile.declarations.filterIsInstance<CfirTypeAlias>().single()
        assertEquals("MyInt", sourceAlias.name.asString())

        // 读入侧：别名声明的 RHS 与别名类型身份都恢复。
        val decoded = CjoDeclarationLoader.loadDeclarations(loaded, cfirFile.moduleData)
        val decodedAlias = decoded.filterIsInstance<CfirTypeAlias>().single()
        assertEquals("MyInt", decodedAlias.name.asString())
        assertEquals(ConePrimitiveType.INT64, decodedAlias.expandedTypeRef.let {
            (it as? CfirResolvedTypeRef)?.coneType
        })

        val decodedFunction = decoded.filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration>()
            .flatMap { it.declarations }
            .filterIsInstance<CfirNamedFunction>()
            .single { it.name.asString() == "take" }
        val parameterType = (decodedFunction.valueParameters.single().returnTypeRef as CfirResolvedTypeRef).coneType
        val aliasType = assertInstanceOf(
            ConeTypeAliasType::class.java,
            parameterType,
            "库签名里的 typealias 必须恢复成 ConeTypeAliasType，而不是错误类型或展开类型",
        )
        assertEquals(FqName("roundtrip"), aliasType.classId.packageFqName)
        assertEquals("MyInt", aliasType.classId.shortClassName.asString())
        assertTrue(aliasType.typeArguments.isEmpty())
        assertEquals(
            ConePrimitiveType.INT64,
            aliasType.expandedType,
            "同包非泛型别名的 expandedType 应内联为 RHS",
        )

        // 反编译侧：别名渲染为它自己的名字，且文本里不能出现 `typealias ` 关键字或 `Any` 占位。
        val text = buildDecompiledText(CjoFileStubBuilder.buildFileStub(loaded, cfirFile.moduleData))
        assertTrue(text.contains("MyInt"), "反编译文本应保留别名名：\n$text")
        assertFalse(text.contains("typealias "), "反编译文本不得出现 `typealias ` 关键字：\n$text")
        assertFalse(text.contains("std.core.Any"), "反编译文本不得退化成 Any 占位：\n$text")
    }

    private fun serializeAndLoad(file: CjFile): Pair<CfirFile, LoadedCjoPackage> {
        val cfirFile = resolveSource(file)
        return cfirFile to loadMetadata(CfirCjoPackageMetadataProducer.produce(listOf(cfirFile)))
    }

    private fun resolveSource(file: CjFile): CfirFile {
        val session = createTestSession()
        val cfirFile = file.toCfirFile(session)
        resolveToPhase(cfirFile, session, targetPhase)
        return cfirFile
    }

    private fun loadMetadata(metadata: CjoPackageMetadata): LoadedCjoPackage {
        val bytes = CjoPackageWriter.toByteArray(metadata)
        val pkg = CjoPackage.getRootAsPackage(ByteBuffer.wrap(bytes))
        return LoadedCjoPackage(
            binaryFile = LightVirtualFile("roundtrip.cjo"),
            packageFqName = FqName("roundtrip"),
            pkg = pkg,
            header = CjoPackageHeader.fromPackage(pkg),
            cjoManager = CjoManager(CjoSearchPath { null }),
            isVersionSupported = true,
        )
    }
}





