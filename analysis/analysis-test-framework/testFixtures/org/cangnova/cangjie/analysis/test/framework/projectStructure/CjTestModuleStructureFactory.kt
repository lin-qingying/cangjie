package org.cangnova.cangjie.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaTargetPlatform
import org.cangnova.cangjie.analysis.test.framework.AnalysisApiTestDirectives
import org.cangnova.cangjie.analysis.test.framework.analysisApiModuleKind
import org.cangnova.cangjie.analysis.test.framework.hasAnalysisApiFallbackDependencies
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.model.DependencyDescription
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.getCjFilesForSourceFiles
import org.cangnova.cangjie.test.services.impl.ModuleStructureExtractorImpl
import org.cangnova.cangjie.test.services.sourceFileProvider
import java.nio.file.Path
import kotlin.io.path.isRegularFile

/**
 * Analysis API 测试模块结构工厂。
 *
 * 它负责把 tests:test-infrastructure 的 [TestModuleStructure] 提升成 Analysis API 可直接消费的模块图。
 * 这里不会把测试模块简化成“源码模块 + binary 视图”，而是完整建模 builtins、script dependencies、
 * fallback dependencies、dangling file 和 not-under-content-root 等分析边界。
 */
object CjTestModuleStructureFactory {
    fun createFromTestDataFile(
        testDataPath: Path,
        testServices: TestServices,
        project: Project,
        targetPlatform: CaTargetPlatform = CaTargetPlatform.STANDALONE,
        additionalDirectives: List<DirectivesContainer> = emptyList(),
    ): CjTestModuleStructure {
        require(testDataPath.isRegularFile()) {
            "Analysis API 测试当前要求传入单个测试数据文件，实际得到：$testDataPath"
        }

        val testModuleStructure = ModuleStructureExtractorImpl.parseModuleStructureWithoutService(
            testDataPath.toFile(),
            AnalysisApiTestDirectives,
            *additionalDirectives.toTypedArray(),
        )
        return createProjectStructureByTestStructure(testModuleStructure, testServices, project, targetPlatform)
    }

    fun createProjectStructureByTestStructure(
        testModuleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
        targetPlatform: CaTargetPlatform = CaTargetPlatform.STANDALONE,
    ): CjTestModuleStructure {
        val modulesByName = linkedMapOf<String, CjTestModule>()
        val builtinsModule = CaBuiltinsModuleImpl(project, targetPlatform = targetPlatform)

        testModuleStructure.modules.forEach { testModule ->
            val psiFiles = testServices.sourceFileProvider
                .getCjFilesForSourceFiles(testModule.files, project)
                .values
                .toList()

            val moduleKind = inferModuleKind(testModule)
            val moduleSet = createModuleSet(
                testModule = testModule,
                moduleKind = moduleKind,
                project = project,
                psiFiles = psiFiles,
                builtinsModule = builtinsModule,
                targetPlatform = targetPlatform,
            )

            modulesByName[testModule.name] = CjTestModule(
                testModule = testModule,
                moduleKind = moduleKind,
                caModule = moduleSet.primaryModule,
                binaryArtifactModule = moduleSet.binaryArtifactModule,
                auxiliaryModules = moduleSet.auxiliaryModules,
                psiFiles = psiFiles,
            )
        }

        modulesByName.values.forEach { cjTestModule ->
            wireDependencies(
                cjTestModule = cjTestModule,
                modulesByName = modulesByName,
                builtinsModule = builtinsModule,
            )
        }

        return CjTestModuleStructure(
            testModuleStructure = testModuleStructure,
            mainModules = modulesByName.values.toList(),
        )
    }

    private fun wireDependencies(
        cjTestModule: CjTestModule,
        modulesByName: Map<String, CjTestModule>,
        builtinsModule: CaBuiltinsModuleImpl,
    ) {
        val primaryModule = cjTestModule.caModule
        val dependencyOwner = primaryModule as? CaMutableTestModule
            ?: error("Test module `${cjTestModule.name}` does not support dependency wiring: ${primaryModule::class.simpleName}")

        val resolvedRegularDependencies = cjTestModule.testModule.regularDependencies.map { dependency ->
            resolveDependencyModule(modulesByName, dependency)
        }
        val resolvedFriendDependencies = cjTestModule.testModule.friendDependencies.map { dependency ->
            resolveDependencyModule(modulesByName, dependency)
        }

        dependencyOwner.addRegularDependencyIfAbsent(builtinsModule)

        if (cjTestModule.testModule.hasAnalysisApiFallbackDependencies) {
            require(cjTestModule.moduleKind == TestModuleKind.LibraryBinary || cjTestModule.moduleKind == TestModuleKind.LibrarySource) {
                "FALLBACK_DEPENDENCIES 仅允许用于库模块：`${cjTestModule.name}` 当前是 ${cjTestModule.moduleKind}。"
            }
            require(resolvedRegularDependencies.isEmpty() && resolvedFriendDependencies.isEmpty()) {
                "声明 FALLBACK_DEPENDENCIES 的测试模块 `${cjTestModule.name}` 不能再声明显式 regular/friend dependencies。"
            }
        } else {
            resolvedRegularDependencies.forEach(dependencyOwner::addRegularDependencyIfAbsent)
            resolvedFriendDependencies.forEach(dependencyOwner::addFriendDependencyIfAbsent)
        }

        cjTestModule.auxiliaryModules
            .filterIsInstance<CaLibraryFallbackDependenciesModuleImpl>()
            .singleOrNull()
            ?.also { fallbackModule ->
                dependencyOwner.addRegularDependencyIfAbsent(fallbackModule)
                fallbackModule.addRegularDependencyIfAbsent(builtinsModule)
                resolvedRegularDependencies.forEach(fallbackModule::addRegularDependencyIfAbsent)
                resolvedFriendDependencies.forEach(fallbackModule::addFriendDependencyIfAbsent)
            }

        if (primaryModule is CaDanglingFileModuleImpl) {
            primaryModule.contextModule = resolvedRegularDependencies.firstOrNull()
        }

        if (primaryModule is CaNotUnderContentRootModuleImpl) {
            primaryModule.originalModule = resolvedRegularDependencies.firstOrNull()
        }
    }

    private fun resolveDependencyModule(
        modulesByName: Map<String, CjTestModule>,
        dependency: DependencyDescription,
    ): CaModule {
        return modulesByName.getValue(dependency.dependencyModuleName).moduleForDependency(dependency.kind)
    }

    private fun createModuleSet(
        testModule: TestModule,
        moduleKind: TestModuleKind,
        project: Project,
        psiFiles: List<PsiFile>,
        builtinsModule: CaBuiltinsModuleImpl,
        targetPlatform: CaTargetPlatform,
    ): TestModuleSet {
        val languageVersionSettings = testModule.languageVersionSettings ?: LanguageVersionSettings.DEFAULT
        val moduleName = testModule.name
        val includeFallbackDependencies = testModule.hasAnalysisApiFallbackDependencies

        return when (moduleKind) {
            TestModuleKind.Source -> {
                val sourceModule = CaSourceModuleImpl(
                    name = moduleName,
                    languageVersionSettings = languageVersionSettings,
                    project = project,
                    psiRoots = psiFiles,
                    targetPlatform = targetPlatform,
                )
                TestModuleSet(
                    primaryModule = sourceModule,
                    binaryArtifactModule = CaLibraryModuleImpl(
                        libraryName = "$moduleName.binary",
                        project = project,
                        binaryRoots = psiFiles,
                        targetPlatform = targetPlatform,
                    ),
                    auxiliaryModules = listOf(builtinsModule),
                )
            }

            TestModuleKind.LibraryBinary -> TestModuleSet(
                primaryModule = CaLibraryModuleImpl(
                    libraryName = moduleName,
                    project = project,
                    binaryRoots = psiFiles,
                    targetPlatform = targetPlatform,
                ),
                binaryArtifactModule = null,
                auxiliaryModules = buildList {
                    add(builtinsModule)
                    if (includeFallbackDependencies) {
                        add(
                            CaLibraryFallbackDependenciesModuleImpl(
                                dependencyOwnerName = moduleName,
                                project = project,
                                scopeRoots = psiFiles,
                                targetPlatform = targetPlatform,
                            ),
                        )
                    }
                },
            )

            TestModuleKind.LibrarySource -> {
                val binaryModule = CaLibraryModuleImpl(
                    libraryName = "$moduleName.binary",
                    project = project,
                    binaryRoots = psiFiles,
                    targetPlatform = targetPlatform,
                )
                TestModuleSet(
                    primaryModule = CaLibrarySourceModuleImpl(
                        libraryName = moduleName,
                        binaryLibraryModule = binaryModule,
                        project = project,
                        sourceRoots = psiFiles,
                        targetPlatform = targetPlatform,
                    ),
                    binaryArtifactModule = binaryModule,
                    auxiliaryModules = buildList {
                        add(builtinsModule)
                        if (includeFallbackDependencies) {
                            add(
                                CaLibraryFallbackDependenciesModuleImpl(
                                    dependencyOwnerName = moduleName,
                                    project = project,
                                    scopeRoots = psiFiles,
                                    targetPlatform = targetPlatform,
                                ),
                            )
                        }
                    },
                )
            }

            TestModuleKind.CodeFragment -> {
                val danglingFileModule = CaDanglingFileModuleImpl(
                    name = moduleName,
                    languageVersionSettings = languageVersionSettings,
                    project = project,
                    psiRoots = psiFiles,
                    targetPlatform = targetPlatform,
                )
                TestModuleSet(
                    primaryModule = danglingFileModule,
                    binaryArtifactModule = CaLibraryModuleImpl(
                        libraryName = "$moduleName.binary",
                        project = project,
                        binaryRoots = psiFiles,
                        targetPlatform = targetPlatform,
                    ),
                    auxiliaryModules = listOf(
                        builtinsModule,
                        CaLibraryFallbackDependenciesModuleImpl(
                            dependencyOwnerName = moduleName,
                            project = project,
                            scopeRoots = psiFiles,
                            targetPlatform = targetPlatform,
                        ),
                    ),
                )
            }

            TestModuleKind.NotUnderContentRoot -> {
                val outsideRootModule = CaNotUnderContentRootModuleImpl(
                    name = moduleName,
                    project = project,
                    scopeRoots = psiFiles,
                    targetPlatform = targetPlatform,
                )
                TestModuleSet(
                    primaryModule = outsideRootModule,
                    binaryArtifactModule = CaLibraryModuleImpl(
                        libraryName = "$moduleName.binary",
                        project = project,
                        binaryRoots = psiFiles,
                        targetPlatform = targetPlatform,
                    ),
                    auxiliaryModules = listOf(
                        builtinsModule,
                        CaLibraryFallbackDependenciesModuleImpl(
                            dependencyOwnerName = moduleName,
                            project = project,
                            scopeRoots = psiFiles,
                            targetPlatform = targetPlatform,
                        ),
                    ),
                )
            }

            TestModuleKind.Builtins -> TestModuleSet(
                primaryModule = CaBuiltinsModuleImpl(
                    project = project,
                    scopeRoots = psiFiles,
                    targetPlatform = targetPlatform,
                ),
                binaryArtifactModule = null,
                auxiliaryModules = emptyList(),
            )

            TestModuleKind.LibraryFallbackDependencies -> TestModuleSet(
                primaryModule = CaLibraryFallbackDependenciesModuleImpl(
                    dependencyOwnerName = moduleName,
                    project = project,
                    scopeRoots = psiFiles,
                    targetPlatform = targetPlatform,
                ),
                binaryArtifactModule = null,
                auxiliaryModules = listOf(builtinsModule),
            )

        }
    }

    private fun inferModuleKind(testModule: TestModule): TestModuleKind {
        findExplicitModuleKind(testModule)?.let { return it }

        val fileNames = testModule.files.map { it.name }
        if (fileNames.any { it.endsWith(".fragment.cj") }) {
            return TestModuleKind.CodeFragment
        }
        if (testModule.files.any { it.relativePath.contains("/outsideRoot/") || it.name.contains(".outsideRoot.") }) {
            return TestModuleKind.NotUnderContentRoot
        }
        return TestModuleKind.Source
    }

    private fun findExplicitModuleKind(testModule: TestModule): TestModuleKind? {
        return testModule.analysisApiModuleKind
    }

    private data class TestModuleSet(
        val primaryModule: CaModule,
        val binaryArtifactModule: CaLibraryModule?,
        val auxiliaryModules: List<CaModule>,
    )
}

private fun CaMutableTestModule.addRegularDependencyIfAbsent(module: CaModule) {
    if (module !== this && module !in directRegularDependencies) {
        directRegularDependencies += module
    }
}

private fun CaMutableTestModule.addFriendDependencyIfAbsent(module: CaModule) {
    if (module !== this && module !in directFriendDependencies) {
        directFriendDependencies += module
    }
}
