package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.combined

import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.AbstractSymbolProviderTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider

/**
 * @see LLCombinedPackageDelegationSymbolProvider
 */
abstract class AbstractCombinedPackageDelegationSymbolProviderTest : AbstractSymbolProviderTest() {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    override fun findTestSymbolProvider(mainModule: CjTestModule): CfirSymbolProvider {
        val providers = mainModule.caModule.findSymbolProvidersOfType<LLCombinedPackageDelegationSymbolProvider>()
        val availableProviders = mainModule.caModule.allTopLevelSymbolProviders()
        return providers.singleOrNull()
            ?: error(
                "Expected a single `${LLCombinedPackageDelegationSymbolProvider::class.simpleName}` " +
                        "(candidates: $providers, available: ${availableProviders.map { it::class.simpleName }})"
            )
    }
}
