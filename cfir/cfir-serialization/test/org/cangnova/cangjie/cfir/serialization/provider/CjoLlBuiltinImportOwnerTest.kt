@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
    org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals::class,
)

package org.cangnova.cangjie.cfir.serialization.provider

import PackageFormat.ExprKind
import PackageFormat.LitConstKind
import PackageFormat.TypeKind
import PackageFormat.DeclKind
import PackageFormat.FullId
import com.google.flatbuffers.FlatBufferBuilder
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiDecompiledTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleBase
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.cases.session.builder.StandaloneBuilderPlatformTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandaloneModuleProvider
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandalonePlatformState
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.CaStandaloneProjectStructure
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.PluginStructureProvider
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirBuiltinsSessionFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.serialization.cjo.*
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

/** FullId 留在 serialization 的正式可见边界；project/session/provider 全部使用真实 LL 装配。 */
class CjoLlBuiltinImportOwnerTest : AbstractAnalysisApiBasedTest() {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(BuiltinImportServices)

    @Test fun originalLibraryNamedImportRejectsEveryActualBuiltinOwnerBeforeMaterializationInBothModes() =
        withProject { project, sdk ->
            val one = Builtin(project, "<builtin-one>")
            val two = Builtin(project, "<builtin-two>")
            assertNotSame(one, two)
            assertEquals(one.targetPlatform, two.targetPlatform)
            install(project, listOf(one, two))
            val sourceFile = writeSource(sdk.parent.resolve("competing-source.cjo"))
            val source = Library(project, one.targetPlatform, "competing-library", psiFile(project, sourceFile))
            install(project, listOf(one, two, source))
            val builtinSession = LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(one.targetPlatform)
            val librarySession = LLCfirSessionCache.getInstance(project).getSession(source, preferBinary = true)
            for (mode in CjoDeserializationMode.entries) {
                val target = assertNotNull(builtinSession.getOwnCjoPackageContext("std.core", mode))
                val index = target.header.topLevelClassifierNameToIndices.getValue("AnnotationKind").single()
                val key = assertNotNull(target.pkg.allDecls(index)?.exportId)
                assertTrue(key.isNotBlank())
                assertNotEquals("_CNat3AnyE", key)
                assertTrue(target.declCache.isEmpty())
                assertTrue(target.typeCache.isEmpty())
                val consumer = assertNotNull(librarySession.getOwnCjoPackageContext("owner.consumer", mode))
                assertEquals(setOf(FqName("owner.consumer")), consumer.binding.ownManager.getAvailablePackageNames())
                val slot = consumer.header.imports.indexOf("std.core")
                assertTrue(slot >= 0)
                val failure = assertFailsWith<IllegalStateException> {
                    consumer.fullIdResolver.resolveSymbol(reference(slot, key))
                }
                assertTrue(failure.message!!.contains("Competing CJO module owners"))
                assertTrue(target.declCache.isEmpty())
                assertTrue(target.typeCache.isEmpty())
            }
        }

    @Test fun singleActualBuiltinOwnerSharesItsNamedWireRecordAndPreservesFullAndSignatureInitializers() =
        withProject { project, sdk ->
            val owner = Builtin(project, "<single-builtin>")
            install(project, listOf(owner))
            val sourceFile = writeSource(sdk.parent.resolve("single-source.cjo"))
            val source = Library(project, owner.targetPlatform, "single-library", psiFile(project, sourceFile))
            install(project, listOf(owner, source))
            val builtinSession = LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(owner.targetPlatform)
            assertNotSame(owner, builtinSession.caModule)
            val librarySession = LLCfirSessionCache.getInstance(project).getSession(source, preferBinary = true)
            val results = CjoDeserializationMode.entries.map { mode ->
                val target = assertNotNull(builtinSession.getOwnCjoPackageContext("std.core", mode))
                val index = target.header.topLevelClassifierNameToIndices.getValue("AnnotationKind").single()
                val key = assertNotNull(target.pkg.allDecls(index)?.exportId)
                assertNotEquals("_CNat3AnyE", key)
                val consumer = assertNotNull(librarySession.getOwnCjoPackageContext("owner.consumer", mode))
                val slot = consumer.header.imports.indexOf("std.core")
                assertTrue(slot >= 0)
                val symbol = consumer.fullIdResolver.resolveSymbol(reference(slot, key))
                val named = target.binding.dependencyManager.deserializedSymbolLookups
                    .forBinding(target.binding, target.moduleData, mode)
                    .getClassLikeSymbolByClassId(ClassId(FqName("std.core"), Name.identifier("AnnotationKind")))
                assertSame<Any>(symbol, assertNotNull(named))
                val record = assertNotNull(target.headerRecordOrNull(index))
                assertSame(record.declarationRoot, target.declCache[index])
                assertSame<Any>(symbol, record.wireSymbol)
                assertSame(symbol.cfir, record.declarationRoot)
                assertSame(builtinSession.moduleData, symbol.cfir.moduleData)
                assertEquals(mode, target.mode)
                assertEquals(sdk.resolve("std/std.core.cjo").toAbsolutePath().normalize(), target.sourcePath)
                val sourceIndex = consumer.header.topLevelCallableNameToIndices.getValue("sourceValue").single()
                val sourceRoot = assertIs<CfirPatternVariable>(consumer.createDeclDeserializer().deserializeDecl(sourceIndex))
                if (mode == CjoDeserializationMode.SIGNATURE_ONLY) assertNull(sourceRoot.initializer)
                    else assertNotNull(sourceRoot.initializer)
                assertSame(librarySession.moduleData, sourceRoot.moduleData)
                symbol
            }
            assertNotSame(results[0], results[1])
        }

    private fun writeSource(path: Path): Path = path.also {
        CjoPackageWriter.write(it, CjoPackageMetadata(
            fullPackageName = "owner.consumer", moduleName = "owner-gate", imports = listOf("std.core"),
            types = listOf(CjoTypeMetadata(kind = TypeKind.Int64)),
            declarations = listOf(CjoPackageDeclaration("sourceValue", kind = DeclKind.VarDecl,
                type = 1u, info = CjoVariableInfo(initializer = 1u))),
            expressions = listOf(CjoExpressionMetadata(kind = ExprKind.LitConstExpr, type = 1u,
                literal = CjoLiteralExpressionMetadata("1", LitConstKind.Integer))),
        ))
    }

    private fun reference(slot: Int, key: String): FullId {
        val builder = FlatBufferBuilder()
        val id = FullId.createFullId(builder, slot, builder.createString(key), 0u)
        builder.finish(id)
        return FullId.getRootAsFullId(builder.dataBuffer())
    }

    private fun withProject(action: (Project, Path) -> Unit) {
        val repo = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isRegularFile(it.resolve("settings.gradle.kts")) }
        val root = Files.createTempDirectory("cjo-ll-owner-import")
        val sdk = Files.createDirectory(root.resolve("sdk"))
        val previous = System.getProperty("cangjie.stdlib.module")
        try {
            val fixture = repo.resolve("cfir/cfir-serialization/testResources/cjo-sdk/windows_x86_64_cjnative")
            for (name in listOf("std.cjo", "std/std.core.cjo", "std/std.objectpool.cjo")) {
                val target = sdk.resolve(name)
                Files.createDirectories(target.parent)
                Files.copy(fixture.resolve(name), target)
            }
            System.setProperty("cangjie.stdlib.module", sdk.toString())
            runTest(repo.resolve("analysis/stubs/testData/compiled/std.objectpool.cj").toString()) { services ->
                action(services.cjTestModuleStructure.project, sdk)
            }
        } finally {
            if (previous == null) System.clearProperty("cangjie.stdlib.module")
                else System.setProperty("cangjie.stdlib.module", previous)
            root.toFile().deleteRecursively()
        }
    }

    /** 安装后直接核对正式 provider 的快照与输入 module identity，防止测试写入另一份 state。 */
    private fun install(project: Project, modules: List<CaModule>) {
        val structure = CaStandaloneProjectStructure(modules)
        val state = CaStandalonePlatformState.getInstance(project)
        state.install(structure)
        val provider = assertIs<CaStandaloneModuleProvider>(CaModuleProvider.getInstance(project))
        val snapshot = provider.snapshot
        assertSame(structure.snapshot, snapshot)
        assertSame(state.snapshot, snapshot)
        assertEquals(modules.size, snapshot.allModules.size)
        modules.forEachIndexed { index, module -> assertSame(module, snapshot.allModules[index]) }
    }

    private fun psiFile(project: Project, path: Path): PsiFileSystemItem {
        val file = assertNotNull(StandardFileSystems.local().refreshAndFindFileByPath(path.toAbsolutePath().toString().replace('\\', '/')))
        return assertNotNull(PsiManager.getInstance(project).findFile(file))
    }

    private class Builtin(override val project: Project, override val builtinsName: String) : CaModuleBase(), CaBuiltinsModule {
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val baseContentScope get() = BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)
    }

    private class Library(override val project: Project, override val targetPlatform: TargetPlatform,
        override val libraryName: String, root: PsiFileSystemItem) : CaLibraryModule {
        override val binaryRoots = listOf(root)
        override val directRegularDependencies = emptyList<CaModule>()
        override val baseContentScope = GlobalSearchScope.fileScope(project, root.virtualFile)
        override val contentScope get() = baseContentScope
    }

    /** 与现有 compiled stubs 测试相同的正式平台服务注册，不提供 owner/metadata 替身。 */
    private object BuiltinImportServices : AnalysisApiTestServiceRegistrar() {
        private val xmls = listOf("META-INF/analysis-api/cangjie-low-level-api-cfir.xml", "META-INF/analysis-api/cangjie-analysis-stubs.xml")
        override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
            xmls.forEach { PluginStructureProvider.registerApplicationServices(application, it) }
            CaAnalysisApiDecompiledTestServiceRegistrar.registerApplicationServices(application)
        }
        override fun registerProjectServices(project: MockProject, testServices: TestServices) {
            // base 已注册真实测试模块；先接正式平台图，再注册 CJO reader，避免两份 state 分离。
            StandaloneBuilderPlatformTestServiceRegistrar.registerProjectServices(project, testServices)
            val structure = CaStandaloneProjectStructure(testServices.cjTestModuleStructure.allCaModules)
            val state = CaStandalonePlatformState.getInstance(project)
            state.install(structure)
            assertSame(structure.snapshot, state.snapshot)
            assertSame(state.snapshot, CaModuleProvider.getInstance(project).snapshot)
            xmls.forEach { PluginStructureProvider.registerProjectServices(project, it) }
            CaAnalysisApiDecompiledTestServiceRegistrar.registerProjectServices(project)
        }
    }
}
