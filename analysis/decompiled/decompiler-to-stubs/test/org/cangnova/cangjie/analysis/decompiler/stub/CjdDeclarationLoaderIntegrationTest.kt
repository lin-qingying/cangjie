package org.cangnova.cangjie.analysis.decompiler.stub

import PackageFormat.Decl
import PackageFormat.DeclKind
import PackageFormat.Package
import com.google.flatbuffers.FlatBufferBuilder
import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjd.CjdSidecarLocator
import org.cangnova.cangjie.cfir.serialization.cjd.cjdAnnotationProvenance
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirSession
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
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

/** 两个生产入口各自物化声明，比较有序注解语义，而不是拿两个裸 context 代替接线测试。 */
class CjdDeclarationLoaderIntegrationTest : CjParsingTestCase("", "cj.d", CangJieDeclarationFileType, CangJieParserDefinition()) {
    private lateinit var root: Path
    @BeforeEach fun setupFixture() { setUp(); root = Files.createTempDirectory("cjd-two-paths-") }
    @AfterEach fun teardownFixture() { root.toFile().deleteRecursively(); tearDown() }

    @Test fun testProviderAndDecompilerUseSelectedBinarySidecar() {
        val builder = FlatBufferBuilder(256)
        val name = builder.createString("Box")
        Decl.startDecl(builder)
        Decl.addIdentifier(builder, name)
        Decl.addKind(builder, DeclKind.ClassDecl)
        Decl.addIsTopLevel(builder, true)
        val declaration = Decl.endDecl(builder)
        val declarations = Package.createAllDeclsVector(builder, intArrayOf(declaration))
        val packageName = builder.createString("test.pkg")
        Package.startPackage(builder)
        Package.addFullPkgName(builder, packageName)
        Package.addAllDecls(builder, declarations)
        Package.finishPackageBuffer(builder, Package.endPackage(builder))
        val binaryPath = root.resolve("selected.cjo")
        Files.write(binaryPath, builder.sizedByteArray())
        Files.writeString(CjdSidecarLocator.deriveCjdPath(binaryPath), """
            package test.pkg
            @Frozen
            @Deprecated[message: "first"]
            @Deprecated[message: "second"]
            class Box {}
        """.trimIndent())
        val manager = CjoManager(CjoSearchPath { if (it == "CANGJIE_LIBRARY") root.toString() else null })
        val loaded = kotlin.test.assertNotNull(manager.loadPackageSnapshot("test.pkg"))
        val module = Module()
        val provider = CfirDeserializedSymbolProvider(module.session, manager, CfirCangJieScopeProvider(), module)
        module.session.register(org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider::class, provider)
        val classId = ClassId.topLevel(FqName("test.pkg.Box"))
        val fromProvider = assertIs<CfirClass>(kotlin.test.assertNotNull(provider.getClassLikeSymbolByClassId(classId)).cfir)
        // 触发用 VirtualFile 故意不同名，sidecar 必须取实际选中 CJO 的 sibling。
        val input = LoadedCjoPackage(LightVirtualFile("unrelated.cjo"), FqName("test.pkg"), loaded.pkg,
            loaded.header, listOf(root.toFile()), true, loaded.sourcePath)
        val fromDecompiler = assertIs<CfirClass>(CjoDeclarationLoader.loadDeclarations(input, Module()).single())
        fun annotationValues(declaration: CfirClass) = declaration.annotations.map { annotation ->
            val call = assertIs<CfirAnnotationCall>(annotation)
            Triple(call.annotationSourceName,
                call.argumentMapping.mapping.map { (name, expression) -> name to (expression as? CfirLiteralExpression)?.value },
                call.source!!.cjdAnnotationProvenance!!.rawText)
        }
        assertEquals(listOf("Frozen", "Deprecated", "Deprecated"), fromProvider.annotations.map { it.annotationSourceName })
        assertEquals(annotationValues(fromProvider), annotationValues(fromDecompiler))
        assertNotSame(fromProvider, fromDecompiler)
        fromProvider.annotations.zip(fromDecompiler.annotations).forEach { (left, right) -> assertNotSame(left, right) }
        assertSame(fromProvider, provider.getClassLikeSymbolByClassId(classId)!!.cfir)
        assertEquals(3, fromProvider.annotations.size)
        // 旧的纯内存入口不凭虚拟文件名推导磁盘路径。
        val withoutPath = assertIs<CfirClass>(CjoDeclarationLoader.loadDeclarations(input.copy(sourcePath = null), Module()).single())
        assertTrue(withoutPath.annotations.isEmpty())

        // 验证生产 CJO → stub 的结果，不借用源码 PSI 或仍持有 AST 的 source stub。
        val fileStub = CjoFileStubBuilder.buildFileStub(input, Module())
        val classStub = fileStub.findChildStubByType(
            org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.CLASS,
        )
        val compiledClass = kotlin.test.assertNotNull(classStub).psi
        assertEquals(listOf("Frozen", "Deprecated", "Deprecated"),
            compiledClass.annotationEntries.map { it.shortName?.asString() })
        val deprecated = compiledClass.annotationEntries[1]
        val argument = kotlin.test.assertNotNull(deprecated.valueArgumentList,
            "CJO annotation stub must contain its argument subtree").arguments.single()
        assertEquals("message", argument.getArgumentName()?.asName?.asString())
        kotlin.test.assertNotNull(argument.getArgumentExpression(), "CJO annotation argument must retain its expression")
    }

    private class Module : CfirModuleData() {
        override val name = Name.identifier("cjd-decompiler-test")
        override val dependencies = emptyList<CfirModuleData>()
        override val refinementDependencies = emptyList<CfirModuleData>()
        override val allRefinementDependencies = emptyList<CfirModuleData>()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "cjd-decompiler-test"
        override val session = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        }
        init { bindSession(session) }
    }
}
