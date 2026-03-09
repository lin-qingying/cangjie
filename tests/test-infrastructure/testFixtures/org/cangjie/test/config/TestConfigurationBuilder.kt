package org.cangjie.test.config

import org.cangjie.test.directives.model.Directive
import org.cangjie.test.services.TestServices

class TestConfigurationBuilder {
    private val facadeFactories = mutableListOf<(TestServices) -> TestFacade>()
    private val handlerFactories = mutableListOf<(TestServices) -> AnalysisHandler>()
    private val defaultDirectives = mutableListOf<Directive>()

    fun useFrontendFacades(vararg facades: (TestServices) -> TestFacade) {
        facadeFactories += facades
    }

    fun useHandlers(vararg handlers: (TestServices) -> AnalysisHandler) {
        handlerFactories += handlers
    }

    fun defaultDirectives(configure: DefaultDirectivesBuilder.() -> Unit) {
        DefaultDirectivesBuilder(defaultDirectives).configure()
    }

    fun build(): TestConfiguration {
        return TestConfiguration(
            facadeFactories = facadeFactories.toList(),
            handlerFactories = handlerFactories.toList(),
            defaultDirectives = defaultDirectives.toList(),
        )
    }

    class DefaultDirectivesBuilder(
        private val directives: MutableList<Directive>,
    ) {
        operator fun Directive.unaryPlus() {
            directives += this
        }
    }
}
