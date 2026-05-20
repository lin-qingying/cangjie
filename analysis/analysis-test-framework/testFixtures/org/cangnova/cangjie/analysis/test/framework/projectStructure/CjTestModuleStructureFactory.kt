package org.cangnova.cangjie.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryFallbackDependenciesModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.explicitModule
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
 * 这里对齐 Kotlin `TestModuleStructureFactory` 的 owner：
 * builtins 由环境层安装，fallback dependencies 只作为库模块的直接依赖注入，
 * 不再把它们物化成测试矩阵里的额外模块。
 */
object CjTestModuleStructureFactory {
    fun createFromTestDataFile(
        testDataPath: Path,
        testServices: TestServices,
        project: Project,
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
        return createProjectStructureByTestStructure(testModuleStructure, testServices, project)
    }

    fun createProjectStructureByTestStructure(
        testModuleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): CjTestModuleStructure {
        val modulesByName = linkedMapOf<String, CjTestModule>()
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
            )

            modulesByName[testModule.name] = CjTestModule(
                testModule = testModule,
                moduleKind = moduleKind,
                caModule = moduleSet.primaryModule,
                binaryArtifactModule = moduleSet.binaryArtifactModule,
                psiFiles = psiFiles,
            )
        }

        modulesByName.values.forEach { cjTestModule ->
            wireDependencies(
                cjTestModule = cjTestModule,
                modulesByName = modulesByName,
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
    ) {
        val primaryModule = cjTestModule.caModule
        val dependencyOwner = primaryModule as? CaMutableTestModule
            ?: error("Test module `${cjTestModule.name}` does not support dependency wiring: ${primaryModule::class.simpleName}")
        val binaryArtifactOwner = cjTestModule.binaryArtifactModule as? CaMutableTestModule

        val resolvedRegularDependencies = cjTestModule.testModule.regularDependencies.map { dependency ->
            resolveDependencyModule(modulesByName, dependency)
        }
        val resolvedFriendDependencies = cjTestModule.testModule.friendDependencies.map { dependency ->
            resolveDependencyModule(modulesByName, dependency)
        }

        if (cjTestModule.testModule.hasAnalysisApiFallbackDependencies) {
            require(cjTestModule.moduleKind == TestModuleKind.LibraryBinary || cjTestModule.moduleKind == TestModuleKind.LibrarySource) {
                "FALLBACK_DEPENDENCIES 仅允许用于库模块：`${cjTestModule.name}` 当前是 ${cjTestModule.moduleKind}。"
            }
            require(resolvedRegularDependencies.isEmpty() && resolvedFriendDependencies.isEmpty()) {
                "声明 FALLBACK_DEPENDENCIES 的测试模块 `${cjTestModule.name}` 不能再声明显式 regular/friend dependencies。"
            }
            val libraryModule = (cjTestModule.binaryArtifactModule ?: primaryModule) as? CaLibraryModule
                ?: error("FALLBACK_DEPENDENCIES 仅允许绑定到库模块：`${cjTestModule.name}` 当前主模块为 ${primaryModule::class.simpleName}")
            val fallbackModule = CaLibraryFallbackDependenciesModuleImpl(libraryModule)
            dependencyOwner.addRegularDependencyIfAbsent(fallbackModule)
            binaryArtifactOwner?.addRegularDependencyIfAbsent(fallbackModule)
        } else {
            resolvedRegularDependencies.forEach(dependencyOwner::addRegularDependencyIfAbsent)
            resolvedFriendDependencies.forEach(dependencyOwner::addFriendDependencyIfAbsent)
            binaryArtifactOwner?.let { binaryOwner ->
                resolvedRegularDependencies.forEach(binaryOwner::addRegularDependencyIfAbsent)
                resolvedFriendDependencies.forEach(binaryOwner::addFriendDependencyIfAbsent)
            }
        }

        if (primaryModule is CaDanglingFileModuleImpl) {
            primaryModule.contextModule = requireNotNull(resolvedRegularDependencies.firstOrNull()) {
                "Code fragment 测试模块 `${cjTestModule.name}` 必须显式绑定 context module。"
            }
            primaryModule.files.forEach { file ->
                @OptIn(CaExperimentalApi::class)
                file.explicitModule = primaryModule
            }
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
    ): TestModuleSet {
        val languageVersionSettings = testModule.languageVersionSettings ?: LanguageVersionSettings.DEFAULT
        val moduleName = testModule.name

        return when (moduleKind) {
            TestModuleKind.Source -> {
                val sourceModule = CaSourceModuleImpl(
                    name = moduleName,
                    languageVersionSettings = languageVersionSettings,
                    project = project,
                    psiRoots = psiFiles,
                )
                TestModuleSet(
                    primaryModule = sourceModule,
                    binaryArtifactModule = null,
                )
            }

            TestModuleKind.LibraryBinary -> TestModuleSet(
                primaryModule = CaLibraryModuleImpl(
                    libraryName = moduleName,
                    project = project,
                    binaryRoots = psiFiles,
                ),
                binaryArtifactModule = null,
            )

            TestModuleKind.LibraryBinaryDecompiled -> {
                error("Test module kind `${TestModuleKind.LibraryBinaryDecompiled}` is not implemented in Cangjie analysis test framework yet.")
            }

            TestModuleKind.LibrarySource -> {
                val binaryModule = CaLibraryModuleImpl(
                    libraryName = "$moduleName.binary",
                    project = project,
                    binaryRoots = psiFiles,
                )
                TestModuleSet(
                    primaryModule = CaLibrarySourceModuleImpl(
                        libraryName = moduleName,
                        binaryLibraryModule = binaryModule,
                        project = project,
                        sourceRoots = psiFiles,
                    ),
                    binaryArtifactModule = binaryModule,
                )
            }

            TestModuleKind.ScriptSource -> {
                error("Test module kind `${TestModuleKind.ScriptSource}` is not implemented in Cangjie analysis test framework yet.")
            }

            TestModuleKind.CodeFragment -> {
                val danglingFileModule = CaDanglingFileModuleImpl(
                    name = moduleName,
                    languageVersionSettings = languageVersionSettings,
                    project = project,
                    psiRoots = psiFiles,
                )
                TestModuleSet(
                    primaryModule = danglingFileModule,
                    binaryArtifactModule = null,
                )
            }

            TestModuleKind.NotUnderContentRoot -> {
                error("Test module kind `${TestModuleKind.NotUnderContentRoot}` is not supported by the generic Cangjie analysis test framework module factory.")
            }

            TestModuleKind.NotUnderContentRootWithDependencies -> {
                error("Test module kind `${TestModuleKind.NotUnderContentRootWithDependencies}` is not supported by the generic Cangjie analysis test framework module factory.")
            }
        }
    }

    private fun inferModuleKind(testModule: TestModule): TestModuleKind {
        findExplicitModuleKind(testModule)?.let { return it }

        val fileNames = testModule.files.map { it.name }
        if (fileNames.any { it.endsWith(".fragment.cj") }) {
            return TestModuleKind.CodeFragment
        }
        return TestModuleKind.Source
    }

    private fun findExplicitModuleKind(testModule: TestModule): TestModuleKind? {
        return testModule.analysisApiModuleKind
    }

    private data class TestModuleSet(
        val primaryModule: CaModule,
        val binaryArtifactModule: CaLibraryModule?,
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
