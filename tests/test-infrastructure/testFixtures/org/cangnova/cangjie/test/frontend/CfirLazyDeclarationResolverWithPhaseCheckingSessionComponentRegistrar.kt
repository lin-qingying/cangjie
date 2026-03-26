package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.entrypoint.session.CfirSessionConfigurator
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.TestService

class CfirLazyDeclarationResolverWithPhaseCheckingSessionComponentRegistrar(
    @Suppress("UNUSED_PARAMETER") testServices: TestServices,
) : TestService {
    private val lazyResolver = CfirCompilerLazyDeclarationResolverWithPhaseChecking()

    fun registerAdditionalComponent(configurator: CfirSessionConfigurator) {
        configurator.registerComponent(CfirLazyDeclarationResolver::class, lazyResolver)
    }
}

val TestServices.cfirLazyDeclarationResolverWithPhaseCheckingSessionComponentRegistrar: CfirLazyDeclarationResolverWithPhaseCheckingSessionComponentRegistrar?
    by TestServices.nullableTestServiceAccessor()
