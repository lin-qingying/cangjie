@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
    org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals::class,
)

package org.cangnova.cangjie.analysis.stubs

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleBase
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandalonePlatformState
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.CaStandaloneProjectStructure
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.decompiled.psi.CangJieBuiltInMetadataStubBuilder
import org.cangnova.cangjie.analysis.decompiler.stub.file.CangJieMetadataStubBuilder
import org.cangnova.cangjie.analysis.decompiler.stub.file.CjoModuleDataProvider
import org.cangnova.cangjie.analysis.decompiler.stub.file.DecompiledPackageDataFinder
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirBuiltinsSessionFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.cfir.common.DetachedCjoModuleData
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeclDeserializer
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirTypeDeserializer
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Base64
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

/** 实际 VFS/原服务/LL library producer；单文件 owner 不扩大到 parent，generation 不由每读 new 定义。 */
class CjoActualFileProducerScopeTest : AbstractAnalysisApiBasedTest() {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(CjoCompiledStubsTestServiceRegistrar)

    @Test fun twoPublicReadersAndBothModesKeepOneDetachedProducerInEitherOrder() = withProject { project, sdk ->
        for (ownerFirst in listOf(true, false)) {
            val root = Files.createTempDirectory("cjo-detached-reader-order")
            try {
                val path = root.resolve("std.objectpool.cjo")
                Files.copy(sdk.resolve("std/std.objectpool.cjo"), path, StandardCopyOption.REPLACE_EXISTING)
                val file = file(path)
                val service = CjoModuleDataProvider.getInstance(project)
                val before = if (ownerFirst) service.getModuleData(file) else
                    service.getPackageContext(file, file.contentsToByteArray(false), CjoDeserializationMode.FULL).moduleData
                val signature = service.getPackageContext(file, file.contentsToByteArray(false), CjoDeserializationMode.SIGNATURE_ONLY)
                val full = service.getPackageContext(file, file.contentsToByteArray(false), CjoDeserializationMode.FULL)
                assertIs<DetachedCjoModuleData>(before)
                assertSame(before, signature.moduleData)
                assertSame(before, full.moduleData)
                assertSame(before, service.getModuleData(file))
                assertSame(signature.binding, full.binding)
                assertSame(signature.snapshotIdentity, full.snapshotIdentity)
                assertNotSame(signature, full)
                assertSame(signature, service.getPackageContext(file, file.contentsToByteArray(false), signature.mode))
                assertSame(full, service.getPackageContext(file, file.contentsToByteArray(false), full.mode))
                assertEquals(path.toAbsolutePath().normalize(), signature.sourcePath)
                val metadata = assertIs<CangJieMetadataStubBuilder.FileWithMetadata.Compatible>(
                    CangJieBuiltInMetadataStubBuilder.readFileSafely(file, null, project))
                assertSame(before, metadata.moduleData)
            } finally { root.toFile().deleteRecursively() }
        }
    }

    @Test fun concurrentColdActualReadersShareThePublishedDetachedModuleAndSnapshot() = withProject { project, sdk ->
        val root = Files.createTempDirectory("cjo-detached-concurrent")
        val callers = Executors.newFixedThreadPool(8)
        try {
            val path = root.resolve("std.objectpool.cjo")
            Files.copy(sdk.resolve("std/std.objectpool.cjo"), path)
            val file = file(path)
            val bytes = file.contentsToByteArray(false)
            val service = CjoModuleDataProvider.getInstance(project)
            val barrier = CyclicBarrier(8)
            val reads = (0 until 8).map { n ->
                callers.submit<CfirDeserializationContext> {
                    barrier.await(10, TimeUnit.SECONDS)
                    ApplicationManager.getApplication().runReadAction<CfirDeserializationContext> {
                        service.getPackageContext(file, bytes,
                            if (n % 2 == 0) CjoDeserializationMode.FULL else CjoDeserializationMode.SIGNATURE_ONLY)
                    }
                }
            }.map { it.get(30, TimeUnit.SECONDS) }
            assertIs<DetachedCjoModuleData>(reads.first().moduleData)
            reads.forEach {
                assertSame(reads.first().moduleData, it.moduleData)
                assertSame(reads.first().binding, it.binding)
                assertSame(reads.first().snapshotIdentity, it.snapshotIdentity)
                assertSame(reads.first { other -> other.mode == it.mode }, it)
            }
        } finally { callers.shutdownNow(); root.toFile().deleteRecursively() }
    }

    @Test fun exactFileOwnScopeLeavesItsSiblingToTheActualLibraryProducer() = withProject { project, _ ->
        val root = Files.createTempDirectory("cjo-exact-file-sibling")
        try {
            val sourcePath = copyWire("wire.imported", root)
            val targetPath = copyWire("wire.current", root)
            val builtin = CjoCompiledTestEnvironment.installBuiltinsProjectStructure(project)
            val targetModule = Library(project, builtin.targetPlatform, "actual-sibling", psiFile(project, targetPath))
            install(project, listOf(builtin, targetModule))
            val sourceFile = file(sourcePath)
            val service = CjoModuleDataProvider.getInstance(project)
            val source = service.getPackageContext(sourceFile, sourceFile.contentsToByteArray(false), CjoDeserializationMode.FULL)
            val targetFile = file(targetPath)
            val target = service.getPackageContext(targetFile, targetFile.contentsToByteArray(false), CjoDeserializationMode.FULL, targetModule)
            assertIs<DetachedCjoModuleData>(source.moduleData)
            assertSame(LLCfirSessionCache.getInstance(project).getSession(targetModule, preferBinary = true).moduleData, target.moduleData)
            assertEquals(setOf(FqName("wire.imported")), source.binding.ownManager.getAvailablePackageNames())
            assertEquals(setOf(FqName("wire.current")), target.binding.ownManager.getAvailablePackageNames())
            val rootVariable = assertIs<CfirPatternVariable>(CfirDeclDeserializer(source, CfirTypeDeserializer(source)).deserializeDecl(0))
            val reference = assertIs<CfirResolvedNamedReference>(assertIs<CfirNamedAccessExpression>(rootVariable.initializer).calleeReference)
            val named = target.binding.dependencyManager.deserializedSymbolLookups.forBinding(
                target.binding, target.moduleData, target.mode).getTopLevelCallableSymbols(
                FqName("wire.current"), Name.identifier("first")).single()
            assertSame(named, reference.resolvedSymbol)
            assertSame(target.moduleData, named.cfir.moduleData)
            assertEquals(targetPath.toAbsolutePath().normalize(), target.sourcePath)
            assertSame(source.moduleData, service.getModuleData(sourceFile))
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun changedIngressCannotCreateAnotherDetachedManagerInsideTheSameGeneration() = withProject { project, sdk ->
        val root = Files.createTempDirectory("cjo-detached-stable-ingress")
        try {
            val path = root.resolve("std.objectpool.cjo")
            Files.copy(sdk.resolve("std/std.objectpool.cjo"), path)
            val file = file(path)
            val service = CjoModuleDataProvider.getInstance(project)
            val bytes = file.contentsToByteArray(false)
            val original = service.getPackageContext(file, bytes, CjoDeserializationMode.SIGNATURE_ONLY)
            val changed = bytes.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
            assertFailsWith<IllegalStateException> {
                service.getPackageContext(file, changed, CjoDeserializationMode.SIGNATURE_ONLY)
            }
            assertSame(original, service.getPackageContext(file, bytes, CjoDeserializationMode.SIGNATURE_ONLY))
            assertSame(original.moduleData, service.getModuleData(file))
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun actualFileModificationRetiresTheWholeDetachedGeneration() = withProject { project, sdk ->
        val root = Files.createTempDirectory("cjo-detached-file-generation")
        try {
            val path = root.resolve("std.objectpool.cjo")
            Files.copy(sdk.resolve("std/std.objectpool.cjo"), path)
            val file = file(path)
            val service = CjoModuleDataProvider.getInstance(project)
            val bytes = file.contentsToByteArray(false)
            val old = service.getPackageContext(file, bytes, CjoDeserializationMode.FULL)
            ApplicationManager.getApplication().runWriteAction { file.setBinaryContent(bytes) }
            val fresh = service.getPackageContext(file, file.contentsToByteArray(false), CjoDeserializationMode.FULL)
            assertNotSame(old.moduleData, fresh.moduleData)
            assertNotSame(old.binding, fresh.binding)
            assertNotSame(old.snapshotIdentity, fresh.snapshotIdentity)
            assertSame(fresh.moduleData, service.getModuleData(file))
            assertEquals(old.sourcePath, fresh.sourcePath)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun installingAnActualLibraryOwnerRetiresTheFilesDetachedProducer() = withProject { project, sdk ->
        val root = Files.createTempDirectory("cjo-detached-owner-generation")
        try {
            val path = root.resolve("std.objectpool.cjo")
            Files.copy(sdk.resolve("std/std.objectpool.cjo"), path)
            val file = file(path)
            val service = CjoModuleDataProvider.getInstance(project)
            val old = service.getPackageContext(file, file.contentsToByteArray(false), CjoDeserializationMode.SIGNATURE_ONLY)
            assertIs<DetachedCjoModuleData>(old.moduleData)
            val builtin = CjoCompiledTestEnvironment.installBuiltinsProjectStructure(project)
            val owner = Library(project, builtin.targetPlatform, "installed-file-owner", psiFile(project, path))
            install(project, listOf(builtin, owner))
            val actual = service.getPackageContext(file, file.contentsToByteArray(false), CjoDeserializationMode.SIGNATURE_ONLY)
            assertSame(LLCfirSessionCache.getInstance(project).getSession(owner, preferBinary = true).moduleData, actual.moduleData)
            assertNotSame(old.moduleData, actual.moduleData)
            assertNotSame(old.binding, actual.binding)
            assertSame(actual.moduleData, service.getModuleData(file))
            assertEquals(path.toAbsolutePath().normalize(), actual.sourcePath)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun differentBuiltinModuleIdentitiesOnTheSamePlatformRemainAmbiguous() = withProject { project, sdk ->
        val original = CjoCompiledTestEnvironment.installBuiltinsProjectStructure(project)
        val another = object : CaModuleBase(), CaBuiltinsModule {
            override val project = project
            override val targetPlatform = original.targetPlatform
            override val builtinsName = "<another-builtin-owner>"
            override val baseContentScope get() = BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(project)
        }
        install(project, listOf(original, another))
        val file = file(sdk.resolve("std/std.core.cjo"))
        assertFailsWith<IllegalStateException> { CaDecompiledBinaryIndex.getInstance(project).findOwningModule(file) }
        assertFailsWith<IllegalStateException> { CjoModuleDataProvider.getInstance(project).getModuleData(file) }
        val service = CjoModuleDataProvider.getInstance(project)
        for (mode in CjoDeserializationMode.entries) {
            for (owner in listOf(original, another)) {
                val failure = assertFailsWith<IllegalStateException> {
                    service.getPackageContext(file, file.contentsToByteArray(false), mode, owner)
                }
                assertTrue(failure.message!!.contains("Competing CJO builtin module owners"))
            }
        }
        val finder = DecompiledPackageDataFinder(project)
        for (owner in listOf(original, another)) {
            val failure = assertFailsWith<IllegalStateException> { finder.loadPackageData(owner, FqName("std.core")) }
            assertTrue(failure.message!!.contains("Competing CJO builtin module owners"))
        }
    }

    @Test fun singleHostBuiltinOwnerUsesTheCanonicalFactoryThroughActualReaderAndDataFinder() = withProject { project, sdk ->
        val owner = CjoCompiledTestEnvironment.installBuiltinsProjectStructure(project)
        val session = LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(owner.targetPlatform)
        assertNotSame(owner, session.caModule)
        val actualFile = file(sdk.resolve("std/std.core.cjo"))
        val service = CjoModuleDataProvider.getInstance(project)
        val contexts = CjoDeserializationMode.entries.map { mode ->
            val actual = service.getPackageContext(actualFile, actualFile.contentsToByteArray(false), mode, owner)
            val named = assertNotNull(session.getOwnCjoPackageContext("std.core", mode))
            assertSame(named, actual)
            assertSame(session.moduleData, actual.moduleData)
            val id = ClassId(FqName("std.core"), Name.identifier("AnnotationKind"))
            val symbol = assertNotNull(actual.classSymbolLookup.getClassLikeSymbolByClassId(id))
            val index = actual.header.topLevelClassifierNameToIndices.getValue("AnnotationKind").single()
            val record = actual.declCache.getValue(index)
            assertSame<Any>(symbol, record.wireSymbol)
            assertSame(symbol.cfir, record.declarationRoot)
            assertEquals(mode, actual.mode)
            assertEquals(sdk.resolve("std/std.core.cjo").toAbsolutePath().normalize(), actual.sourcePath)
            actual
        }
        assertSame(contexts[0].moduleData, contexts[1].moduleData)
        assertSame(contexts[0].binding, contexts[1].binding)
        assertSame(contexts[0].snapshotIdentity, contexts[1].snapshotIdentity)
        assertNotSame(contexts[0], contexts[1])
        val loaded = assertNotNull(DecompiledPackageDataFinder(project).loadPackageData(owner, FqName("std.core")))
        assertSame(contexts.single { it.mode == CjoDeserializationMode.SIGNATURE_ONLY }, loaded.deserializationContext)
    }

    @Test fun sdkFileWithoutAnInstalledBuiltinOwnerUsesAnExplicitFileScope() = withProject { project, sdk ->
        val emptyRoot = Files.createTempDirectory("cjo-no-builtin-owner")
        try {
            val directoryFile = file(emptyRoot)
            val root = assertNotNull(PsiManager.getInstance(project).findDirectory(directoryFile))
            val module = Library(project, CangJiePlatforms.defaultCangJiePlatform, "empty-actual-library", root)
            install(project, listOf(module))
            val file = file(sdk.resolve("std/std.core.cjo"))
            assertNull(CaDecompiledBinaryIndex.getInstance(project).findOwningModule(file))
            val service = CjoModuleDataProvider.getInstance(project)
            val context = service.getPackageContext(file, file.contentsToByteArray(false), CjoDeserializationMode.SIGNATURE_ONLY)
            assertIs<DetachedCjoModuleData>(context.moduleData)
            assertSame(context.moduleData, service.getModuleData(file))
            assertEquals(setOf(FqName("std.core")), context.binding.ownManager.getAvailablePackageNames())
        } finally { emptyRoot.toFile().deleteRecursively() }
    }

    private fun withProject(action: (Project, Path) -> Unit) {
        CjoCompiledTestEnvironment.withSlimStdlibFixture("std.cjo", "std/std.core.cjo", "std/std.objectpool.cjo") { sdk ->
            val data = CjoCompiledTestEnvironment.locateRepositoryRoot().resolve("analysis/stubs/testData/compiled/std.objectpool.cj")
            runTest(data.toString()) { services ->
                val project = services.cjTestModuleStructure.project
                CjoCompiledTestEnvironment.installBuiltinsProjectStructure(project)
                action(project, sdk)
            }
        }
    }

    private fun install(project: Project, modules: List<CaModule>) =
        project.getService(CaStandalonePlatformState::class.java).install(CaStandaloneProjectStructure(modules))
    private fun file(path: Path): VirtualFile = assertNotNull(StandardFileSystems.local()
        .refreshAndFindFileByPath(path.toAbsolutePath().toString().replace('\\', '/')))
    private fun psiFile(project: Project, path: Path): PsiFileSystemItem =
        assertNotNull(PsiManager.getInstance(project).findFile(file(path)))
    private fun copyWire(name: String, directory: Path): Path {
        val path = CjoCompiledTestEnvironment.locateRepositoryRoot()
            .resolve("cfir/cfir-serialization/testResources/cjo-wire/" + name + ".cjo.b64")
        return Files.write(directory.resolve(name + ".cjo"), Base64.getDecoder().decode(Files.readString(path).trim()))
    }

    private class Library(override val project: Project, override val targetPlatform: TargetPlatform,
        override val libraryName: String, root: PsiFileSystemItem) : CaLibraryModule {
        override val binaryRoots = listOf(root)
        override val directRegularDependencies = emptyList<CaModule>()
        override val baseContentScope = if (root.virtualFile.isDirectory)
            com.intellij.psi.search.GlobalSearchScopesCore.directoryScope(project, root.virtualFile, true)
            else GlobalSearchScope.fileScope(project, root.virtualFile)
        override val contentScope get() = baseContentScope
    }
}
