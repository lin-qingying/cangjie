package org.cangnova.cangjie.analysis.test.framework.services

import org.cangnova.cangjie.analysis.test.framework.AnalysisApiTestDirectives
import org.cangnova.cangjie.analysis.test.framework.analysisApiModuleKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.model.DependencyDescription
import org.cangnova.cangjie.test.model.DependencyKind
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.test.model.TestModuleStructureImpl
import org.cangnova.cangjie.test.services.DefaultsProvider
import org.cangnova.cangjie.test.services.ModuleStructureTransformer

/**
 * 把 Analysis API 的 `MODULE_KIND` 显式依赖转换成正确的 `DependencyKind`。
 *
 * Kotlin 测试框架要求 source-like 测试在依赖 `LibraryBinaryDecompiled` / `LibraryBinary`
 * 模块时，把依赖边界降为 binary dependency；否则测试框架仍会按 source dependency
 * 连线，无法进入真实 compiled/decompiled library 路径。
 */
@OptIn(TestInfrastructureInternals::class)
object DependencyKindModuleStructureTransformer : ModuleStructureTransformer() {
    override fun transformModuleStructure(moduleStructure: TestModuleStructure, defaultsProvider: DefaultsProvider): TestModuleStructure {
        if (AnalysisApiTestDirectives.MODULE_KIND !in moduleStructure.allDirectives) return moduleStructure

        val moduleMapping = moduleStructure.modules.associateBy(TestModule::name)
        return TestModuleStructureImpl(
            moduleStructure.modules.map { module ->
                module.copy(
                    allDependencies = module.allDependencies.map { dependency ->
                        transformDependency(dependency, moduleMapping)
                    },
                )
            },
            moduleStructure.originalTestDataFiles,
        )
    }

    private fun transformDependency(
        dependency: DependencyDescription,
        moduleMapping: Map<String, TestModule>,
    ): DependencyDescription {
        val dependencyModule = moduleMapping.getValue(dependency.dependencyModuleName)
        val newKind = when (dependencyModule.analysisApiModuleKind) {
            TestModuleKind.Source,
            TestModuleKind.LibrarySource,
            TestModuleKind.ScriptSource,
            TestModuleKind.CodeFragment,
            -> DependencyKind.Source

            TestModuleKind.LibraryBinary,
            TestModuleKind.LibraryBinaryDecompiled,
            -> DependencyKind.Binary

            TestModuleKind.NotUnderContentRoot,
            TestModuleKind.NotUnderContentRootWithDependencies,
            -> error("A not-under-content-root module cannot be a dependency.")

            null -> return dependency
        }

        return dependency.copy(kind = newKind)
    }
}
