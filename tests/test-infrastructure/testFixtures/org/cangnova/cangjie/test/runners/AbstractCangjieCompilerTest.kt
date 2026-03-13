package org.cangnova.cangjie.test.runners

import junit.framework.TestCase
import org.cangnova.cangjie.test.config.TestConfigurationBuilder
import org.cangnova.cangjie.test.directives.CangjieTestDirectives
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.test.services.DefaultDirectivesService
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.impl.TestModuleStructureExtractorImpl
import java.nio.file.Path
import java.nio.file.Paths

abstract class AbstractCangjieCompilerTest : TestCase() {
    protected abstract fun TestConfigurationBuilder.configuration()

    protected open val directivesContainer: DirectivesContainer = CangjieTestDirectives

    protected fun runTest(testDataFilePath: String) {
        val testDataPath = Paths.get(testDataFilePath)
        runTest(testDataPath)
    }

    protected fun runTest(testDataPath: Path) {
        val testServices = TestServices()
        val moduleStructure = TestModuleStructureExtractorImpl(directivesContainer).extract(testDataPath)
        testServices.register(TestModuleStructure::class, moduleStructure)

        val configurationBuilder = TestConfigurationBuilder().apply { configuration() }
        val configuration = configurationBuilder.build()
        testServices.register(
            DefaultDirectivesService::class,
            DefaultDirectivesService(configuration.defaultDirectives.toSet()),
        )

        val facades = configuration.facadeFactories.map { it(testServices) }
        val handlers = configuration.handlerFactories.map { it(testServices) }

        moduleStructure.modules.forEach { module ->
            val artifact = facades.fold<_, Any?>(null) { currentArtifact, facade ->
                facade.transform(module, currentArtifact)
            }
            handlers.forEach { handler ->
                handler.processModule(module, artifact, testServices)
            }
        }

        handlers.forEach { it.processAfterAllModules(testServices) }
    }
}
