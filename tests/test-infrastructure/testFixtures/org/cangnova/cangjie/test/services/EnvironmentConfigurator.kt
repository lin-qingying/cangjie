package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.config.AnalysisFlag
import org.cangnova.cangjie.config.LanguageVersion
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.model.ServicesAndDirectivesContainer

@DslMarker
annotation class DefaultsDsl

abstract class AbstractEnvironmentConfigurator(
    protected val testServices: TestServices,
) : TestService, ServicesAndDirectivesContainer {
    open fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion,
    ): Map<AnalysisFlag<*>, Any?> = emptyMap()
}
