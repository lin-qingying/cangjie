package org.cangnova.cangjie.test.config

import org.cangnova.cangjie.test.Constructor
import org.cangnova.cangjie.test.model.DependencyKind
import org.cangnova.cangjie.test.model.FrontendFacade
import org.cangnova.cangjie.test.model.FrontendKind
import org.cangnova.cangjie.test.model.ResultingArtifact
import org.cangnova.cangjie.test.services.AdditionalSourceProvider
import org.cangnova.cangjie.test.services.CommonEnvironmentConfigurator

fun <F : ResultingArtifact.FrontendOutput<F>, B : ResultingArtifact.BackendInput<B>> TestConfigurationBuilder.commonConfigurationForTest(
    targetFrontend: FrontendKind<F>,
    frontendFacade: Constructor<FrontendFacade<F>>,
    additionalSourceProvider: Constructor<AdditionalSourceProvider>? = null,
) {
    commonServicesConfigurationForCodegenAndDebugTest(targetFrontend)

    facadeStep(frontendFacade)

}
fun TestConfigurationBuilder.commonServicesConfigurationForCodegenAndDebugTest(targetFrontend: FrontendKind<*>) {
    globalDefaults {
        frontend = targetFrontend
        dependencyKind = DependencyKind.Binary
    }

    defaultDirectives {
    }

    useConfigurators(
        ::CommonEnvironmentConfigurator,
    )

    useAdditionalSourceProviders(
    )

//    useMetaTestConfigurators(::CfirSpecificParserSuppressor)
}
