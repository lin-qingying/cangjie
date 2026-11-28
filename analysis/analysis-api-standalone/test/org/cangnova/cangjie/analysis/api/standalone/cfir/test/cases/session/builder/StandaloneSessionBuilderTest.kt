@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.cfir.test.cases.session.builder

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.lifetime.CaInvalidLifetimeOwnerAccessException
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaContentScopeRefiner
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaCjmpCompilationMode
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.resolution.successfulFunctionCallOrNull
import org.cangnova.cangjie.analysis.api.resolution.symbol
import org.cangnova.cangjie.analysis.api.standalone.base.declarations.CangJieStandaloneSourceFileCollector
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.CaStandaloneSourceModule
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandalonePlatformState
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandaloneProjectStructureProvider
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
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.elementContext
import org.cangnova.cangjie.psi.psiUtil.findDescendantOfType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.test.services.TestServices
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import java.nio.file.Files
import java.nio.file.Path
import java.nio.charset.StandardCharsets
import java.util.Comparator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
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
    /**
     * 使用 CFIR standalone Analysis API 配置执行 session builder 行为测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 为直接构造 standalone session builder 的测试注册平台服务。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(StandaloneBuilderPlatformTestServiceRegistrar)

    /** 普通 cjnative 模块通过真实配置编译 common/specific，分析入口完成配对、调用和诊断。 */
    @Test
    fun explicitCjmpModuleModesResolveCounterpartAndCaller(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val files = testServices.cjTestModuleStructure.allCjFiles
        val commonFile = files.single { it.name == "common.cj" }
        val specificFile = files.single { it.name == "specific.cj" }
        val settings = (mainModule.caModule as CaSourceModule).languageVersionSettings
        val common = CaStandaloneSourceModule("cjmp-common", settings, mainFile.project, listOf(commonFile),
            cjmpCompilationMode = CaCjmpCompilationMode.COMMON)
        val specific = CaStandaloneSourceModule("cjmp-specific", settings, mainFile.project, listOf(specificFile),
            cjmpCompilationMode = CaCjmpCompilationMode.SPECIFIC)
        specific.directDependsOnDependencies += common
        val context = CaStandaloneSessionBuilder(mainFile.project).build(specific)
        assertSame(common, context.getUseSiteModule(commonFile))
        assertSame(specific, context.getUseSiteModule(specificFile))
        val diagnostics = context.analyzeFiles(listOf(commonFile, specificFile)) { file ->
            file.collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS).map { it.factoryName }
        }
        assertTrue(diagnostics.all { it.isEmpty() }, diagnostics.toString())
        val caller = specificFile.declarations.filterIsInstance<CjNamedFunction>().single { it.name == "use" }
        val call = requireNotNull(caller.bodyExpression?.findDescendantOfType<CjCallExpression>())
        val selected = context.analyze(call) { call.resolveToCall()?.successfulFunctionCallOrNull()?.symbol }
        requireNotNull(selected)
        assertSame(specific, selected.containingModule)
        assertEquals(Name.identifier("value"), selected.name)
    }

    /**
     * 验证根模块构建会收集可达模块闭包，并支持稳定模块名查询。
     */
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

    /**
     * 验证跨模块批量文件分析保持调用方输入顺序。
     */
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

    /**
     * 验证批量元素分析按 use-site module 分组复用 session 且结果顺序不漂移。
     */
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
        val projectStructure = CangJieProjectStructureProvider.getInstance(mainFile.project)
        assertSame(helperModule, projectStructure.getModule(helperClass, mainModule.caModule))
        assertSame(mainModule.caModule, projectStructure.getModule(consumeFunction, helperModule))

        val renderedDeclarations = context.analyzeElements(listOf(consumeFunction, helperClass)) { element ->
            when (element) {
                is CjNamedFunction -> "${element.name}:${element.symbol.containingModule.stableModuleName}"
                is CjTypeStatement -> "${element.name}:${element.symbol.containingModule.stableModuleName}"
                else -> error("Unexpected declaration type `${element::class.qualifiedName}`.")
            }
        }

        assertEquals(listOf("consume:main", "Helper:helper"), renderedDeclarations)
    }

    /** 两个源码依赖触发 LL combined provider，消费模块不能覆盖任一依赖文件的归属。 */
    @Test
    fun multipleSourceDependenciesKeepTheirDeclarationOwners(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val context = standaloneContext(mainFile, mainModule)
        val structure = CangJieProjectStructureProvider.getInstance(mainFile.project)
        for (module in testServices.cjTestModuleStructure.mainModules.filter { it !== mainModule }) {
            val sourceFile = module.cjFiles.single()
            assertSame(module.caModule, structure.getModule(sourceFile.declarations.single(), mainModule.caModule))
        }
        for (call in PsiTreeUtil.findChildrenOfType(mainFile, CjCallExpression::class.java)) {
            val symbol = context.analyze(call) { call.resolveToCall()?.successfulFunctionCallOrNull()?.symbol }
                ?: error("Imported call `${call.text}` must resolve through its source dependency")
            val expectedModule = when (call.calleeExpression?.text) {
                "fromLeft" -> "left"
                "fromRight" -> "right"
                else -> error("Unexpected imported call `${call.text}`")
            }
            assertEquals(expectedModule, symbol.containingModule.stableModuleName)
        }
    }

    /** G3：同一文件的两个等优先级真实 owner，只能由候选对象的身份消歧。 */
    @Test
    fun sameRootEqualPriorityOwner(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        val first = standaloneOwnerModule(mainModule, "owner-first", listOf(mainFile))
        val second = standaloneOwnerModule(mainModule, "owner-second", listOf(mainFile))
        assertNotSame(first, second)
        val virtualFile = requireNotNull(mainFile.virtualFile)
        assertTrue(first.contentScope.contains(virtualFile))
        assertTrue(second.contentScope.contains(virtualFile))

        for (modules in listOf(listOf(first, second), listOf(second, first))) {
            val context = CaStandaloneSessionBuilder(mainFile.project).build(modules)
            val provider = standaloneProductionProvider(context)
            for (element in listOf(mainFile, mainFile.declarations.single())) {
                assertSame(first, provider.getModule(element, first))
                assertSame(second, provider.getModule(element, second))
            }
            assertSame(first, context.getUseSiteModule(mainFile, first))
            assertSame(second, context.getUseSiteModule(mainFile, second))
        }
    }

    /** G3：空偏好、范围外模块及图外同名对象都不能替等优先级候选消歧。 */
    @Test
    fun sameRootEqualPriorityOwnerRejectsNonCandidatePreference(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        val peerFile = mainModule.cjFiles.single { it !== mainFile }
        val first = standaloneOwnerModule(mainModule, "owner-first", listOf(mainFile))
        val second = standaloneOwnerModule(mainModule, "owner-second", listOf(mainFile))
        val unrelated = standaloneOwnerModule(mainModule, "owner-peer", listOf(peerFile))
        val sameNameOutsideGraph = standaloneOwnerModule(mainModule, first.name, listOf(mainFile))
        assertNotSame(first, sameNameOutsideGraph)
        assertEquals(first.stableModuleName, sameNameOutsideGraph.stableModuleName)
        val virtualFile = requireNotNull(mainFile.virtualFile)
        assertFalse(unrelated.contentScope.contains(virtualFile))
        assertTrue(sameNameOutsideGraph.contentScope.contains(virtualFile))

        val context = CaStandaloneSessionBuilder(mainFile.project).build(first, second, unrelated)
        val provider = standaloneProductionProvider(context)
        assertSame(unrelated, provider.getModule(peerFile, first))
        for (preference in listOf(null, unrelated, sameNameOutsideGraph)) {
            assertThrows(IllegalStateException::class.java) { provider.getModule(mainFile, preference) }
            assertThrows(IllegalStateException::class.java) {
                context.getUseSiteModule(mainFile.declarations.single(), preference)
            }
        }
        // 失败查询不得改变后续合法偏好的真实 owner。
        assertSame(first, provider.getModule(mainFile, first))
        assertSame(second, provider.getModule(mainFile, second))
    }

    /** G3：真实 scope refiner 先裁剪候选，唯一存活 owner 不受被裁剪 use-site 覆盖。 */
    @Test
    fun contentScopeExcludedOwnerCannotOverrideUniqueOwner(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        val peerFile = mainModule.cjFiles.single { it !== mainFile }
        val excluded = standaloneOwnerModule(mainModule, "owner-excluded", listOf(mainFile, peerFile))
        val retained = standaloneOwnerModule(mainModule, "owner-retained", listOf(mainFile))
        restrictOwnerContentScope(excluded, mainFile)
        val virtualFile = requireNotNull(mainFile.virtualFile)
        assertTrue(excluded.baseContentScope.contains(virtualFile))
        assertFalse(excluded.contentScope.contains(virtualFile))
        assertTrue(retained.contentScope.contains(virtualFile))

        val context = CaStandaloneSessionBuilder(mainFile.project).build(excluded, retained)
        val provider = standaloneProductionProvider(context)
        for (preference in listOf(null, excluded, retained)) {
            assertSame(retained, provider.getModule(mainFile, preference))
            assertSame(retained, context.getUseSiteModule(mainFile.declarations.single(), preference))
        }
        // 仅排除目标文件，不把整个模块或它仍然拥有的其它文件一并排除。
        assertTrue(excluded.contentScope.contains(requireNotNull(peerFile.virtualFile)))
        assertSame(excluded, provider.getModule(peerFile, retained))
    }

    /** G3：裁剪后仍有两个真实 owner，被裁剪模块的偏好必须明确拒绝。 */
    @Test
    fun contentScopeExcludedOwnerCannotDisambiguateRemainingOwners(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        val excluded = standaloneOwnerModule(mainModule, "owner-excluded", listOf(mainFile))
        val first = standaloneOwnerModule(mainModule, "owner-first", listOf(mainFile))
        val second = standaloneOwnerModule(mainModule, "owner-second", listOf(mainFile))
        restrictOwnerContentScope(excluded, mainFile)
        val virtualFile = requireNotNull(mainFile.virtualFile)
        assertTrue(excluded.baseContentScope.contains(virtualFile))
        assertFalse(excluded.contentScope.contains(virtualFile))
        assertTrue(first.contentScope.contains(virtualFile))
        assertTrue(second.contentScope.contains(virtualFile))

        val context = CaStandaloneSessionBuilder(mainFile.project).build(excluded, first, second)
        val provider = standaloneProductionProvider(context)
        assertSame(first, provider.getModule(mainFile, first))
        assertSame(second, provider.getModule(mainFile, second))
        for (preference in listOf(null, excluded)) {
            assertThrows(IllegalStateException::class.java) { provider.getModule(mainFile, preference) }
            assertThrows(IllegalStateException::class.java) {
                context.getUseSiteModule(mainFile.declarations.single(), preference)
            }
        }
    }

    /** G3：物理目录根和更深文件根同时包含源码，较浅 use-site 不能改写深根 owner。 */
    @Test
    fun deeperSourceRootOwnerCannotBeOverriddenByUseSite(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        val temporaryRoot = Files.createTempDirectory("standalone-owner-depth")
        try {
            // 复用已确认的 fixture 正文，只为目录祖先关系建立真实磁盘输入。
            val filePath = Files.writeString(temporaryRoot.resolve("main.cj"), mainFile.text, StandardCharsets.UTF_8)
            val virtualFile = requireNotNull(
                StandardFileSystems.local().refreshAndFindFileByPath(filePath.toString().replace('\\', '/')),
            )
            val diskFile = requireNotNull(PsiManager.getInstance(mainFile.project).findFile(virtualFile)) as CjFile
            val directory = requireNotNull(diskFile.containingDirectory)
            val shallow = standaloneOwnerModule(mainModule, "owner-directory", listOf(directory))
            val deep = standaloneOwnerModule(mainModule, "owner-file", listOf(diskFile))
            assertTrue(shallow.contentScope.contains(virtualFile))
            assertTrue(deep.contentScope.contains(virtualFile))

            for (modules in listOf(listOf(shallow, deep), listOf(deep, shallow))) {
                val context = CaStandaloneSessionBuilder(mainFile.project).build(modules)
                val provider = standaloneProductionProvider(context)
                for (preference in listOf(null, shallow, deep)) {
                    assertSame(deep, provider.getModule(diskFile, preference))
                    assertSame(deep, context.getUseSiteModule(diskFile.declarations.single(), preference))
                }
            }
        } finally {
            Files.walk(temporaryRoot).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
            }
        }
    }

    /**
     * 验证更具体的文件 source root 会覆盖目录 root 并独立决定依赖解析边界。
     */
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

                    public class Helper {
                    }

                    public func helperFactory(): Helper {
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
                val calleeReference = specificCall.calleeExpression as? CjReferenceExpression
                val resolvedCalleeSymbols = calleeReference?.resolveToSymbols().orEmpty()
                assertEquals(listOf("helperFactory"), resolvedCalleeSymbols.map { it.name?.asString() })
                val resolvedCall = specificCall.resolveToCall()?.successfulFunctionCallOrNull()
                    ?: error(
                        "Specific file root must resolve helperFactory() through its own dependency edge; " +
                            "visible candidates=${getTopLevelCallableSymbols(FqName("sample.standalone.priority.helper"), Name.identifier("helperFactory"))
                                .map { it.callableId?.toString() }}",
                    )
                assertEquals("helperFactory", resolvedCall.partiallyAppliedSymbol.signature.symbol.name?.asString())
                assertSame(specificFileModule, specificFile.symbol.containingModule)
            }
        } finally {
            Files.walk(tempRoot)
                .sorted(Comparator.reverseOrder())
                .forEach { path -> Files.deleteIfExists(path) }
        }
    }

    /**
     * 验证同一 standalone 模块内的具体文件 root 能向目录 root 中源码贡献声明。
     */
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

    /**
     * 验证目录 source root 经过目录链接暴露的文件会进入内容作用域和声明收集闭包。
     */
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

    /**
     * 验证 standalone 上下文失效会推进 modification count 并使旧 session 产物失效。
     */
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

    /**
     * 验证代码片段会落到 dangling module，并能捕获上下文函数参数。
     */
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

    /**
     * 验证非物理文件继承上下文模块并能够解析上下文中的调用。
     */
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

    /** 使用 fixture 的真实项目、语言设置及 PSI 根构造独立 production 源码模块对象。 */
    private fun standaloneOwnerModule(
        mainModule: CjTestModule,
        name: String,
        roots: List<PsiFileSystemItem>,
    ): CaStandaloneSourceModule {
        val sourceModule = mainModule.caModule as CaSourceModule
        return CaStandaloneSourceModule(
            name = name,
            languageVersionSettings = sourceModule.languageVersionSettings,
            project = sourceModule.project,
            psiRoots = roots,
            targetPlatform = sourceModule.targetPlatform,
        )
    }

    /** 查询已安装模块图的真实平台服务，防止测试只覆盖 testFixtures provider。 */
    private fun standaloneProductionProvider(context: CaStandaloneAnalysisContext): CangJieProjectStructureProvider {
        val provider = CangJieProjectStructureProvider.getInstance(context.project)
        assertTrue(provider is CaStandaloneProjectStructureProvider)
        return provider
    }

    /** 对指定真实模块注册文件级 scope 裁剪；测试根 disposable 负责卸载扩展。 */
    private fun restrictOwnerContentScope(owner: CaModule, file: CjFile) {
        val restriction = GlobalSearchScope.notScope(GlobalSearchScope.fileScope(file))
        val refiner = object : CaContentScopeRefiner {
            override fun getRestrictionScopes(module: CaModule): List<GlobalSearchScope> =
                if (module === owner) listOf(restriction) else emptyList()
        }
        owner.project.extensionArea
            .getExtensionPoint<CaContentScopeRefiner>(CaContentScopeRefiner.EP_NAME.name)
            .registerExtension(refiner, disposable)
    }

    /**
     * 基于主测试模块创建 standalone 分析上下文。
     */
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
