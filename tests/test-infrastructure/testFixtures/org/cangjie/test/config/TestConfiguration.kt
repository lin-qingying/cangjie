package org.cangjie.test.config

import org.cangjie.test.directives.model.Directive
import org.cangjie.test.model.TestModule
import org.cangjie.test.services.TestServices

fun interface TestFacade {
    fun transform(module: TestModule, inputArtifact: Any?): Any?
}

interface AnalysisHandler {
    fun processModule(module: TestModule, artifact: Any?, testServices: TestServices)

    fun processAfterAllModules(testServices: TestServices) {}
}

class TestConfiguration(
    val facadeFactories: List<(TestServices) -> TestFacade>,
    val handlerFactories: List<(TestServices) -> AnalysisHandler>,
    val defaultDirectives: List<Directive>,
)
