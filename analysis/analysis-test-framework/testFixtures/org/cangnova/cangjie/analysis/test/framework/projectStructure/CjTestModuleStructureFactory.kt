package org.cangnova.cangjie.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.test.model.DependencyKind
import org.cangnova.cangjie.test.model.DependencyRelation
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
 * 该工厂复用现有 `tests:test-infrastructure` 的模块解析逻辑，
 * 然后把 `TestModuleStructure` 映射成 Analysis API 可直接消费的 `CjTestModuleStructure`。
 */
object CjTestModuleStructureFactory {
    fun createFromTestDataFile(
        testDataPath: Path,
        testServices: TestServices,
        project: Project,
    ): CjTestModuleStructure {
        require(testDataPath.isRegularFile()) {
            "Analysis API 测试当前要求传入单个测试数据文件，实际得到：$testDataPath"
        }

        val testModuleStructure = ModuleStructureExtractorImpl.parseModuleStructureWithoutService(testDataPath.toFile())
        return createProjectStructureByTestStructure(testModuleStructure, testServices, project)
    }

    fun createProjectStructureByTestStructure(
        testModuleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): CjTestModuleStructure {
        val modulesByName = linkedMapOf<String, CjTestModule>()

        for (testModule in testModuleStructure.modules) {
            val psiFiles = testServices.sourceFileProvider
                .getCjFilesForSourceFiles(testModule.files, project)
                .values
                .toList()

            val caModule = CaSourceModuleImpl(
                name = testModule.name,
                languageVersionSettings = testModule.languageVersionSettings ?: LanguageVersionSettings.DEFAULT,
                project = project,
                psiRoots = psiFiles,
            )

            modulesByName[testModule.name] = CjTestModule(
                testModule = testModule,
                moduleKind = inferModuleKind(testModule),
                caModule = caModule,
                psiFiles = psiFiles,
            )
        }

        for (cjTestModule in modulesByName.values) {
            val sourceModule = cjTestModule.caModule as CaSourceModuleImpl
            for (dependency in cjTestModule.testModule.allDependencies) {
                require(dependency.kind == DependencyKind.Source) {
                    "Analysis API 测试框架当前仅支持源码模块依赖，" +
                        "模块 `${cjTestModule.name}` 包含未对齐的二进制依赖 `${dependency.dependencyModuleName}`。"
                }

                val dependencyModule = modulesByName.getValue(dependency.dependencyModuleName).caModule
                when (dependency.relation) {
                    DependencyRelation.RegularDependency -> sourceModule.directRegularDependencies += dependencyModule
                    DependencyRelation.FriendDependency -> sourceModule.directFriendDependencies += dependencyModule
                }
            }
        }

        return CjTestModuleStructure(
            testModuleStructure = testModuleStructure,
            mainModules = modulesByName.values.toList(),
        )
    }

    private fun inferModuleKind(testModule: TestModule): TestModuleKind {
        val scriptFiles = testModule.files.filter { it.name.endsWith(".cjs") }
        return if (scriptFiles.isNotEmpty() && scriptFiles.size == testModule.files.size) {
            TestModuleKind.ScriptSource
        } else {
            TestModuleKind.Source
        }
    }
}
