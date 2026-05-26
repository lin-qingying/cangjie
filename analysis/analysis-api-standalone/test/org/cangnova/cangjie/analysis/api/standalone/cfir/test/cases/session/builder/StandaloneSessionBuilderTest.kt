@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.cfir.test.cases.session.builder

import com.intellij.openapi.vfs.StandardFileSystems
import org.cangnova.cangjie.analysis.api.lifetime.CaInvalidLifetimeOwnerAccessException
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.api.resolution.successfulFunctionCallOrNull
import org.cangnova.cangjie.analysis.api.standalone.base.declarations.CangJieStandaloneSourceFileCollector
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.CaStandaloneSourceModule
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandalonePlatformState
import org.cangnova.cangjie.analysis.api.standalone.session.CaStandaloneAnalysisContext
import org.cangnova.cangjie.analysis.api.standalone.session.CaStandaloneSessionBuilder
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.api.symbols.symbol
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.elementContext
import org.cangnova.cangjie.psi.psiUtil.findDescendantOfType
import org.cangnova.cangjie.test.services.TestServices
import com.intellij.psi.PsiManager
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Standalone session builder 的手写行为测试。
 *
 * generated suite 已覆盖通用 Analysis API 组件；
 * 这里补 Kotlin standalone 专有但本地尚未锁定的几条契约：
 * 1. 根模块构建必须自动带上可达闭包；
 * 2. 批量文件分析必须保持调用方输入顺序；
 * 3. 批量元素分析必须按各自 use-site module 分组，但最终结果顺序不能漂移；
 * 4. `invalidate*()` 必须让旧 session 产物立刻失效，同时允许上下文继续重新分析；
 * 5. code fragment 必须落到 dangling module，并能捕获上下文局部变量；
 * 6. non-physical file 必须继承上下文模块并正常完成解析。
 * 7. 更具体的 source root 必须覆盖目录 root，并真实影响依赖解析边界。
 * 8. 单模块下的“具体文件 root + 目录 root”组合，必须让目录文件解析到具体文件 root 暴露的声明。
 * 9. 目录 source root 经过目录链接暴露的源码，也必须进入 standalone 声明收集闭包。
 */
class StandaloneSessionBuilderTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-standalone/testData/sessionBuilder",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(StandaloneBuilderPlatformTestServiceRegistrar)

    @Test
    fun collectReachableModulesAndFindByStableName(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val context = standaloneContext(mainFile, mainModule)
        val helperModule = testServices.cjTestModuleStructure.getModule("helper").caModule

        assertEquals(listOf("main", "helper"), context.allModules.mapNotNull { it.stableModuleName })
        assertSame(mainModule.caModule, context.findModuleByStableName("main"))
        assertSame(helperModule, context.findModuleByStableName("helper"))
        assertNull(context.findModuleByStableName("missing"))
    }

    @Test
    fun analyzeFilesPreservesInputOrderAcrossModules(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val context = standaloneContext(mainFile, mainModule)
        val requestedFiles = listOf("secondary.cj", "helper.cj", "entry.cj").map { fileName ->
            testServices.cjTestModuleStructure.allCjFiles.single { file -> file.name == fileName }
        }

        val declarationNames = context.analyzeFiles(requestedFiles) { file ->
            val firstDeclaration = file.declarations.filterIsInstance<CjDeclaration>().firstOrNull()
                ?: error("Test file `${file.name}` does not contain a declaration.")
            firstDeclaration.symbol.name?.asString()
                ?: error("Declaration `${firstDeclaration.text}` in `${file.name}` has no stable public name.")
        }

        assertEquals(listOf("Secondary", "Helper", "entry"), declarationNames)
    }

    @Test
    fun analyzeElementsResolvesUseSiteModulesAndPreservesInputOrder(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val context = standaloneContext(mainFile, mainModule)
        val helperModule = testServices.cjTestModuleStructure.getModule("helper").caModule
        val helperFile = testServices.cjTestModuleStructure.allCjFiles.single { file -> file.name == "helper.cj" }
        val helperClass = helperFile.declarations.filterIsInstance<CjTypeStatement>().single { declaration ->
            declaration.name == "Helper"
        }
        val consumeFunction = mainFile.declarations.filterIsInstance<CjNamedFunction>().single { function ->
            function.name == "consume"
        }

        assertSame(helperModule, context.getUseSiteModule(helperClass))
        assertSame(mainModule.caModule, context.getUseSiteModule(consumeFunction))

        val renderedDeclarations = context.analyzeElements(listOf(consumeFunction, helperClass)) { element ->
            when (element) {
                is CjNamedFunction -> "${element.name}:${element.symbol.containingModule.stableModuleName}"
                is CjTypeStatement -> "${element.name}:${element.symbol.containingModule.stableModuleName}"
                else -> error("Unexpected declaration type `${element::class.qualifiedName}`.")
            }
        }

        assertEquals(listOf("consume:main", "Helper:helper"), renderedDeclarations)
    }

    @Test
    fun specificSourceRootOverridesDirectoryRootAndItsDependencies(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        val languageVersionSettings = (mainModule.caModule as? CaSourceModule)?.languageVersionSettings
            ?: error("Standalone root-priority test expects a source module, got `${mainModule.caModule::class.qualifiedName}`.")
        val psiManager = PsiManager.getInstance(mainFile.project)
        val tempRoot = Files.createTempDirectory("standalone-root-priority")

        try {
            val mainDirPath = Files.createDirectories(tempRoot.resolve("main"))
            val helperDirPath = Files.createDirectories(tempRoot.resolve("helper"))
            val broadPath = mainDirPath.resolve("broad.cj")
            val specificPath = mainDirPath.resolve("specific.cj")
            val helperPath = helperDirPath.resolve("helper.cj")

            Files.writeString(
                helperPath,
                """
                    package sample.standalone.priority.helper

                    class Helper {
                    }

                    func helperFactory(): Helper {
                        return Helper()
                    }
                """.trimIndent(),
            )
            Files.writeString(
                broadPath,
                """
                    package sample.standalone.priority.main

                    import sample.standalone.priority.helper.helperFactory

                    func broadUsage() {
                        helperFactory()
                    }
                """.trimIndent(),
            )
            Files.writeString(
                specificPath,
                """
                    package sample.standalone.priority.main

                    import sample.standalone.priority.helper.helperFactory

                    func specificUsage(): sample.standalone.priority.helper.Helper {
                        return helperFactory()
                    }
                """.trimIndent(),
            )

            val normalizedTempRootPath = tempRoot.toString().replace('\\', '/')
            val localFileSystem = StandardFileSystems.local()
            val rootVirtualFile = localFileSystem.findFileByPath(normalizedTempRootPath)
                ?: localFileSystem.refreshAndFindFileByPath(normalizedTempRootPath)
                ?: error("Cannot refresh temporary root `${tempRoot}`.")
            val mainDirectory = psiManager.findDirectory(
                rootVirtualFile.findChild("main") ?: error("Temporary main directory was not created."),
            ) ?: error("Cannot restore PSI directory for `${mainDirPath}`.")
            val broadFile = psiManager.findFile(
                mainDirectory.virtualFile.findChild("broad.cj") ?: error("Temporary broad.cj was not created."),
            ) as? CjFile ?: error("Cannot restore PSI for `${broadPath}`.")
            val specificFile = psiManager.findFile(
                mainDirectory.virtualFile.findChild("specific.cj") ?: error("Temporary specific.cj was not created."),
            ) as? CjFile ?: error("Cannot restore PSI for `${specificPath}`.")
            val helperFile = psiManager.findFile(
                (rootVirtualFile.findChild("helper") ?: error("Temporary helper directory was not created."))
                    .findChild("helper.cj") ?: error("Temporary helper.cj was not created."),
            ) as? CjFile ?: error("Cannot restore PSI for `${helperPath}`.")

            val helperModule = CaStandaloneSourceModule(
                name = "helper",
                languageVersionSettings = languageVersionSettings,
                project = mainFile.project,
                psiRoots = listOf(helperFile),
            )
            val directoryModule = CaStandaloneSourceModule(
                name = "directory-root",
                languageVersionSettings = languageVersionSettings,
                project = mainFile.project,
                psiRoots = listOf(mainDirectory),
            )
            val specificFileModule = CaStandaloneSourceModule(
                name = "specific-file-root",
                languageVersionSettings = languageVersionSettings,
                project = mainFile.project,
                psiRoots = listOf(specificFile),
            ).apply {
                directRegularDependencies += helperModule
            }

            val context = CaStandaloneSessionBuilder(mainFile.project).build(directoryModule, specificFileModule)
            val directoryCall = broadFile.findDescendantOfType<CjCallExpression>()
                ?: error("Directory-root file `${broadFile.name}` must contain a call expression.")
            val specificCall = specificFile.findDescendantOfType<CjCallExpression>()
                ?: error("Specific-root file `${specificFile.name}` must contain a call expression.")

            assertSame(directoryModule, context.getUseSiteModule(broadFile))
            assertSame(specificFileModule, context.getUseSiteModule(specificFile))

            context.analyze(directoryCall) {
                assertNull(
                    directoryCall.resolveToCall()?.successfulFunctionCallOrNull(),
                    "目录 root 模块未声明 helper 依赖时，不应解析 helperFactory().",
                )
            }

            context.analyze(specificCall) {
                val resolvedCall = specificCall.resolveToCall()?.successfulFunctionCallOrNull()
                    ?: error("Specific file root must resolve helperFactory() through its own dependency edge.")
                assertEquals("helperFactory", resolvedCall.partiallyAppliedSymbol.signature.symbol.name?.asString())
                assertSame(specificFileModule, specificFile.symbol.containingModule)
            }
        } finally {
            Files.walk(tempRoot)
                .sorted(Comparator.reverseOrder())
                .forEach { path -> Files.deleteIfExists(path) }
        }
    }

    @Test
    fun specificFileSourceRootContributesDeclarationsToSameModule(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        val languageVersionSettings = (mainModule.caModule as? CaSourceModule)?.languageVersionSettings
            ?: error("Standalone same-module root test expects a source module, got `${mainModule.caModule::class.qualifiedName}`.")
        val psiManager = PsiManager.getInstance(mainFile.project)
        val tempRoot = Files.createTempDirectory("standalone-same-module-root")

        try {
            val mainDirPath = Files.createDirectories(tempRoot.resolve("main"))
            val dependentDirPath = Files.createDirectories(tempRoot.resolve("dependent"))
            val mainPath = mainDirPath.resolve("main.cj")
            val dependentPath = dependentDirPath.resolve("helper.cj")

            Files.writeString(
                dependentPath,
                """
                    package sample.standalone.same.module

                    func helperFactory(): Int64 {
                        return 7
                    }
                """.trimIndent(),
            )
            Files.writeString(
                mainPath,
                """
                    package sample.standalone.same.module

                    func mainUsage(): Int64 {
                        return helperFactory()
                    }
                """.trimIndent(),
            )

            val normalizedTempRootPath = tempRoot.toString().replace('\\', '/')
            val localFileSystem = StandardFileSystems.local()
            val rootVirtualFile = localFileSystem.findFileByPath(normalizedTempRootPath)
                ?: localFileSystem.refreshAndFindFileByPath(normalizedTempRootPath)
                ?: error("Cannot refresh temporary root `${tempRoot}`.")
            val mainDirectory = psiManager.findDirectory(
                rootVirtualFile.findChild("main") ?: error("Temporary main directory was not created."),
            ) ?: error("Cannot restore PSI directory for `${mainDirPath}`.")
            val dependentFile = psiManager.findFile(
                (rootVirtualFile.findChild("dependent") ?: error("Temporary dependent directory was not created."))
                    .findChild("helper.cj") ?: error("Temporary helper.cj was not created."),
            ) as? CjFile ?: error("Cannot restore PSI for `${dependentPath}`.")
            val entryFile = psiManager.findFile(
                mainDirectory.virtualFile.findChild("main.cj") ?: error("Temporary main.cj was not created."),
            ) as? CjFile ?: error("Cannot restore PSI for `${mainPath}`.")

            val sourceModule = CaStandaloneSourceModule(
                name = "single-module",
                languageVersionSettings = languageVersionSettings,
                project = mainFile.project,
                psiRoots = listOf(dependentFile, mainDirectory),
            )
            val context = CaStandaloneSessionBuilder(mainFile.project).build(sourceModule)
            val callExpression = entryFile.findDescendantOfType<CjCallExpression>()
                ?: error("Main file `${entryFile.name}` must contain a call expression.")

            assertEquals(listOf("helper.cj", "main"), context.projectStructure.allSourceFiles.map { it.name })
            assertSame(sourceModule, context.getUseSiteModule(entryFile))
            assertSame(sourceModule, context.getUseSiteModule(dependentFile))

            context.analyze(callExpression) {
                val resolvedCall = callExpression.resolveToCall()?.successfulFunctionCallOrNull()
                    ?: error("Directory-root file `${entryFile.name}` must resolve helperFactory() from the specific file root.")
                assertEquals("helperFactory", resolvedCall.partiallyAppliedSymbol.signature.symbol.name?.asString())
            }
        } finally {
            Files.walk(tempRoot)
                .sorted(Comparator.reverseOrder())
                .forEach { path -> Files.deleteIfExists(path) }
        }
    }

    @Test
    fun sourceRootFollowsDirectoryLinks(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        val languageVersionSettings = (mainModule.caModule as? CaSourceModule)?.languageVersionSettings
            ?: error("Standalone directory-link test expects a source module, got `${mainModule.caModule::class.qualifiedName}`.")
        val psiManager = PsiManager.getInstance(mainFile.project)
        val tempRoot = Files.createTempDirectory("standalone-source-link")

        try {
            val sourceDirPath = Files.createDirectories(tempRoot.resolve("src"))
            val linkedDirPath = Files.createDirectories(tempRoot.resolve("src2"))
            val entryPath = sourceDirPath.resolve("main.cj")
            val linkedPath = linkedDirPath.resolve("helper.cj")

            Files.writeString(
                entryPath,
                """
                    package sample.standalone.linked

                    func usage(): Int64 {
                        return helper()
                    }
                """.trimIndent(),
            )
            Files.writeString(
                linkedPath,
                """
                    package sample.standalone.linked

                    func helper(): Int64 {
                        return 42
                    }
                """.trimIndent(),
            )

            createDirectoryLink(sourceDirPath.resolve("src2"), linkedDirPath)

            val normalizedTempRootPath = tempRoot.toString().replace('\\', '/')
            val localFileSystem = StandardFileSystems.local()
            val rootVirtualFile = localFileSystem.findFileByPath(normalizedTempRootPath)
                ?: localFileSystem.refreshAndFindFileByPath(normalizedTempRootPath)
                ?: error("Cannot refresh temporary root `${tempRoot}`.")
            val sourceDirectory = psiManager.findDirectory(
                rootVirtualFile.findChild("src") ?: error("Temporary src directory was not created."),
            ) ?: error("Cannot restore PSI directory for `${sourceDirPath}`.")
            val entryFile = psiManager.findFile(
                sourceDirectory.virtualFile.findChild("main.cj") ?: error("Temporary main.cj was not created."),
            ) as? CjFile ?: error("Cannot restore PSI for `${entryPath}`.")

            val sourceModule = CaStandaloneSourceModule(
                name = "linked-root",
                languageVersionSettings = languageVersionSettings,
                project = mainFile.project,
                psiRoots = listOf(sourceDirectory),
            )
            val context = CaStandaloneSessionBuilder(mainFile.project).build(sourceModule)
            val callExpression = entryFile.findDescendantOfType<CjCallExpression>()
                ?: error("Linked-root file `${entryFile.name}` must contain a call expression.")
            val linkRoot = sourceDirectory.virtualFile.findChild("src2")
                ?: error("Directory link `src2` was not materialized under `${sourceDirectory.virtualFile.path}`.")
            val linkedVirtualFile = linkRoot.findChild("helper.cj")
                ?: error("Directory link `src2` does not expose helper.cj to VFS.")
            val collectedFiles = CangJieStandaloneSourceFileCollector(mainFile.project).collect(sourceModule.contentScope)

            assertSame(sourceModule, context.getUseSiteModule(entryFile))
            assertTrue(sourceModule.contentScope.contains(linkedVirtualFile))
            assertEquals(setOf("main.cj", "helper.cj"), collectedFiles.mapTo(linkedSetOf()) { it.name })

            context.analyze(callExpression) {
                val resolvedCall = callExpression.resolveToCall()?.successfulFunctionCallOrNull()
                    ?: error("Directory link under a source root must contribute helper() to standalone declaration collection.")
                assertEquals("helper", resolvedCall.partiallyAppliedSymbol.signature.symbol.name?.asString())
            }
        } finally {
            Files.walk(tempRoot)
                .sorted(Comparator.reverseOrder())
                .forEach { path -> Files.deleteIfExists(path) }
        }
    }

    @Test
    fun invalidateContextInvalidatesPreviousSessionObjects(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        val context = standaloneContext(mainFile, mainModule)
        val trackedFunction = mainFile.declarations.filterIsInstance<CjNamedFunction>().single { function ->
            function.name == "tracked"
        }

        lateinit var capturedSymbol: CaNamedFunctionSymbol
        context.analyze(trackedFunction) {
            capturedSymbol = trackedFunction.symbol
            assertEquals("tracked", capturedSymbol.name.asString())
        }

        val platformState = CaStandalonePlatformState.getInstance(mainFile.project)
        val modificationCountBeforeInvalidation = platformState.modificationCount

        context.invalidateAll()

        assertTrue(
            platformState.modificationCount > modificationCountBeforeInvalidation,
            "Standalone invalidation 必须推动平台级 modification count 前进。",
        )

        val exception = assertThrows(CaInvalidLifetimeOwnerAccessException::class.java) {
            capturedSymbol.name.asString()
        }
        assertTrue(exception.message.orEmpty().isNotBlank())

        context.analyze(trackedFunction) {
            assertEquals("tracked", trackedFunction.symbol.name.asString())
        }
    }

    @Test
    fun codeFragmentCapturesContextAndUsesDanglingModule(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        val context = standaloneContext(mainFile, mainModule)
        val contextElement = mainFile.findDescendantOfType<CjSimpleNameExpression> { expression ->
            expression.referencedName == "y"
        } ?: error("Test file `${mainFile.name}` must reference local variable `y` for code fragment context.")
        val codeFragment = CjPsiFactory(mainFile.project).createExpressionCodeFragment("x - 1", contextElement)

        val standaloneModule = CangJieProjectStructureProvider.getModule(mainFile.project, codeFragment, useSiteModule = null)
        assertTrue(standaloneModule is CaDanglingFileModule)
        standaloneModule as CaDanglingFileModule
        assertTrue(standaloneModule.isCodeFragment)
        assertSame(mainModule.caModule, standaloneModule.contextModule)

        val contentElement = codeFragment.getContentElement()
            ?: error("Code fragment `${codeFragment.name}` must expose a content element.")
        context.analyze(contentElement) {
            val fileModule = codeFragment.symbol.containingModule
            assertTrue(fileModule is CaDanglingFileModule)
            fileModule as CaDanglingFileModule
            assertTrue(fileModule.isCodeFragment)
            assertSame(mainModule.caModule, fileModule.contextModule)

            val referenceExpression = codeFragment.findDescendantOfType<CjSimpleNameExpression> { expression ->
                expression.referencedName == "x"
            } ?: error("Code fragment `${codeFragment.text}` must contain reference `x`.")
            val resolvedSymbol = referenceExpression.resolveToSymbol()
            assertTrue(resolvedSymbol is CaValueParameterSymbol, resolvedSymbol?.let { it::class.qualifiedName } ?: "null")
            assertEquals("x", resolvedSymbol?.name?.asString())
        }
    }

    @Test
    fun nonPhysicalFileUsesContextModuleAndResolvesCalls(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        val context = standaloneContext(mainFile, mainModule)
        val dummyFile = CjPsiFactory.contextual(mainFile, markGenerated = false).createFile(
            "dummy.cj",
            """
                package sample.standalone.nonphysical

                func usage(): Int64 {
                    return test()
                }
            """.trimIndent(),
        )

        assertSame(mainFile, dummyFile.elementContext)

        val standaloneModule = CangJieProjectStructureProvider.getModule(mainFile.project, dummyFile, useSiteModule = null)
        assertTrue(standaloneModule is CaDanglingFileModule)
        standaloneModule as CaDanglingFileModule
        assertSame(mainModule.caModule, standaloneModule.contextModule)

        val callExpression = dummyFile.findDescendantOfType<CjCallExpression>()
            ?: error("Dummy file must contain a call expression.")
        context.analyze(callExpression) {
            val fileModule = dummyFile.symbol.containingModule
            assertTrue(fileModule is CaDanglingFileModule)
            fileModule as CaDanglingFileModule
            assertSame(mainModule.caModule, fileModule.contextModule)

            val resolvedCall = callExpression.resolveToCall()?.successfulFunctionCallOrNull()
                ?: error("Call `${callExpression.text}` inside a non-physical file must resolve.")
            assertEquals("test", resolvedCall.partiallyAppliedSymbol.signature.symbol.name?.asString())
        }
    }

    private fun standaloneContext(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ): CaStandaloneAnalysisContext {
        return CaStandaloneSessionBuilder(mainFile.project).build(mainModule.caModule)
    }

    /**
     * Windows 上优先尝试真实符号链接；若当前权限不足，则退回目录 junction。
     * 其他平台直接使用符号链接，以保持与 Kotlin `symLinks` 场景相同的目录穿透语义。
     */
    private fun createDirectoryLink(
        linkPath: Path,
        targetPath: Path,
    ) {
        runCatching {
            Files.createSymbolicLink(linkPath, targetPath)
        }.onSuccess {
            return
        }

        if (!System.getProperty("os.name").lowercase().contains("win")) {
            error("Cannot create symbolic link `${linkPath}` -> `${targetPath}` on `${System.getProperty("os.name")}`.")
        }

        val process = ProcessBuilder(
            "cmd",
            "/c",
            "mklink",
            "/J",
            linkPath.toString(),
            targetPath.toString(),
        )
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { reader -> reader.readText() }
        val exitCode = process.waitFor()

        check(exitCode == 0 && Files.exists(linkPath)) {
            "Cannot create directory junction `${linkPath}` -> `${targetPath}`. mklink output: $output"
        }
    }
}
