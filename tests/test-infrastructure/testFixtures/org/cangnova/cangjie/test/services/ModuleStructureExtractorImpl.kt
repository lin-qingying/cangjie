package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.builders.LanguageVersionSettingsBuilder
import org.cangnova.cangjie.test.directives.CangjieTestDirectives
import org.cangnova.cangjie.test.directives.model.ComposedDirectivesContainer
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.model.ArtifactKind
import org.cangnova.cangjie.test.model.BackendKind
import org.cangnova.cangjie.test.model.FrontendKind
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.test.model.TestModuleStructureImpl
import org.cangnova.cangjie.test.services.impl.TestModuleStructureExtractorImpl as RawExtractor
import java.io.File
import java.nio.file.Paths

@OptIn(TestInfrastructureInternals::class)
class ModuleStructureExtractorImpl(
    testServices: TestServices,
    additionalSourceProviders: List<AdditionalSourceProvider>,
    moduleStructureTransformers: List<ModuleStructureTransformer>,
    @Suppress("UNUSED_PARAMETER") private val environmentConfigurators: List<AbstractEnvironmentConfigurator>,
) : ModuleStructureExtractor(testServices, additionalSourceProviders, moduleStructureTransformers) {
    companion object {
        fun parseModuleStructureWithoutService(testDataFile: File, vararg directivesContainers: DirectivesContainer): TestModuleStructure {
            val directivesContainer: DirectivesContainer = when (directivesContainers.size) {
                0 -> CangjieTestDirectives
                1 -> directivesContainers.single()
                else -> ComposedDirectivesContainer(*directivesContainers)
            }
            val testServices = TestServices().apply {
                register(AssertionsService::class, org.cangnova.cangjie.test.services.impl.JUnit5Assertions)
                register(
                    DefaultsProvider::class,
                    DefaultsProvider(
                        frontendKind = FrontendKind.NoFrontend,
                        backendKind = BackendKind.NoBackend,
                        defaultLanguageSettingsBuilder = LanguageVersionSettingsBuilder(),
                        artifactKind = ArtifactKind.NoArtifact,
                        defaultDependencyKind = DependencyKind.Regular
                    )
                )
                register(DefaultRegisteredDirectivesProvider::class, DefaultRegisteredDirectivesProvider(RegisteredDirectives.Empty))
            }
            return ModuleStructureExtractorImpl(
                testServices = testServices,
                additionalSourceProviders = emptyList(),
                moduleStructureTransformers = emptyList(),
                environmentConfigurators = emptyList()
            ).splitTestDataByModules(testDataFile.canonicalPath, directivesContainer)
        }
    }

    override fun splitTestDataByModules(testDataFileName: String, directivesContainer: DirectivesContainer): TestModuleStructure {
        var moduleStructure = RawExtractor(directivesContainer).extract(Paths.get(testDataFileName))
        for (transformer in moduleStructureTransformers) {
            moduleStructure = try {
                transformer.transformModuleStructure(moduleStructure, testServices.defaultsProvider)
            } catch (e: Throwable) {
                throw ExceptionFromModuleStructureTransformer(e, moduleStructure)
            }
        }

        if (additionalSourceProviders.isEmpty()) return moduleStructure

        val transformedModules = moduleStructure.modules.map { module ->
            val extraFiles = additionalSourceProviders.flatMap { provider ->
                provider.produceAdditionalFiles(
                    globalDirectives = moduleStructure.allDirectives,
                    module = module,
                    testModuleStructure = moduleStructure
                ).also { files ->
                    require(files.all(TestFile::isAdditional)) {
                        "Files produced by ${provider::class.qualifiedName} should have flag `isAdditional = true`"
                    }
                }
            }
            module.copy(files = module.files + extraFiles)
        }

        return TestModuleStructureImpl(
            modules = transformedModules,
            allDirectives = moduleStructure.allDirectives,
            originalTestDataFiles = moduleStructure.originalTestDataFiles
        )
    }
}
