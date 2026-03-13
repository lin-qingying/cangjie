package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * Resolve provider registration pipeline.
 *
 * Provider instances are created from session-aware factories so builtin providers
 * can be composed safely at registration time.
 */
data class CfirResolveProviderPipeline(
    val symbolProviderFactory: (CfirSession) -> CfirSymbolProvider,
    val cfirProviderFactory: (CfirSession) -> CfirProvider,
    val extendProviderFactory: (CfirSession) -> CfirExtendProvider,
) {
    fun registerInto(session: CfirSession) {
        session.register(CfirSymbolProvider::class, symbolProviderFactory(session))
        session.register(CfirProvider::class, cfirProviderFactory(session))
        session.register(CfirExtendProvider::class, extendProviderFactory(session))
    }

    companion object {
        /**
         * Formal default registration chain.
         *
         * Current stage keeps source/import layers as empty implementations,
         * but already composes builtin symbol provider through composite wrapper.
         */
        fun formalDefaults(): CfirResolveProviderPipeline = CfirResolveProviderPipeline(
            symbolProviderFactory = { session ->
                CfirCompositeSymbolProvider(
                    listOf(
                        CfirBuiltinSymbolProvider(session),
                        CfirEmptySymbolProvider(),
                    ),
                )
            },
            cfirProviderFactory = {
                CfirCompositeProvider(
                    listOf(CfirEmptyProvider()),
                )
            },
            extendProviderFactory = {
                CfirCompositeExtendProvider(
                    listOf(CfirEmptyExtendProvider()),
                )
            },
        )

        /**
         * Backward-compatible defaults used by legacy entry points.
         */
        fun legacyCompatibleDefaults(): CfirResolveProviderPipeline = CfirResolveProviderPipeline(
            symbolProviderFactory = { CfirEmptySymbolProvider() },
            cfirProviderFactory = { CfirEmptyProvider() },
            extendProviderFactory = { CfirEmptyExtendProvider() },
        )
    }
}
