package org.cangnova.cangjie.cfir.entrypoint.session

import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.deserialization.ModuleDataProvider
import org.cangnova.cangjie.cfir.extensions.CfirExtensionRegistrar
import org.cangnova.cangjie.cfir.extensions.CfirSwitchableExtensionDeclarationsSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirBuiltinSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.resolve.transformers.MacroExpandAction
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.name.Name

/**
 * Default concrete implementation of [CfirAbstractSessionFactory].
 *
 * This class mirrors Kotlin's concrete FIR session factories structure:
 * - creates shared/library/source sessions via public entrypoints
 * - keeps platform-dependent wiring customizable through [Context]
 */
@OptIn(SessionConfiguration::class)
open class CfirDefaultSessionFactory : CfirAbstractSessionFactory<CfirDefaultSessionFactory.Context>() {
    /**
     * Extra wiring points for platform/environment-specific integrations.
     */
    class Context(
        val cjoManager: CjoManager? = null,
        val additionalSharedProviders: (CfirSession, CfirModuleData, CfirCangJieScopeProvider) -> List<CfirSymbolProvider> =
            { _, _, _ -> emptyList() },
        val additionalLibraryProviders: (CfirSession, ModuleDataProvider, CfirCangJieScopeProvider) -> List<CfirSymbolProvider> =
            { _, _, _ -> emptyList() },
        val additionalSourceProviders: (
            CfirSession,
            CfirCangJieScopeProvider,
            CfirSymbolProvider,
            CfirSwitchableExtensionDeclarationsSymbolProvider?,
        ) -> List<CfirSymbolProvider> = { _, _, mainProvider, generatedProvider ->
            listOfNotNull(mainProvider, generatedProvider)
        },
        val additionalOptionalAnnotationsProvider: (
            CfirSession,
            CfirCangJieScopeProvider,
            CfirSymbolProvider,
            CfirSwitchableExtensionDeclarationsSymbolProvider?,
        ) -> CfirSymbolProvider? = { _, _, _, _ -> null },
        val registerLibrarySessionComponents: CfirSession.() -> Unit = {},
        val registerSourceSessionComponents: CfirSession.() -> Unit = {},
    )

    fun createSharedLibrarySession(
        mainModuleName: Name,
        extensionRegistrars: List<CfirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        context: Context = Context(),
    ): CfirSession {
        return super.createSharedLibrarySession(
            mainModuleName,
            context,
            languageVersionSettings,
            extensionRegistrars,
        )
    }

    fun createLibrarySession(
        sharedLibrarySession: CfirSession,
        moduleDataProvider: ModuleDataProvider,
        extensionRegistrars: List<CfirExtensionRegistrar>,
        languageVersionSettings: LanguageVersionSettings,
        context: Context = Context(),
    ): CfirSession {
        return super.createLibrarySession(
            context,
            sharedLibrarySession,
            moduleDataProvider,
            languageVersionSettings,
            extensionRegistrars,
            createProviders = { session, cangjieScopeProvider ->
                buildList {
                    // Keep builtins reachable from library sessions by default.
                    add(CfirBuiltinSymbolProvider(session))

                    context.cjoManager?.let { manager ->
                        add(
                            CfirDeserializedSymbolProvider(
                                session = session,
                                cjoManager = manager,
                                cangjieScopeProvider = cangjieScopeProvider,
                                libraryModuleData = moduleDataProvider.regularDependenciesModuleData,
                            )
                        )
                    }

                    addAll(context.additionalLibraryProviders(session, moduleDataProvider, cangjieScopeProvider))
                }
            },
        )
    }

    fun createSourceSession(
        moduleData: CfirModuleData,
        extensionRegistrars: List<CfirExtensionRegistrar>,
        configuration: CompilerConfiguration,
        context: Context = Context(),
        macroExpandAction: MacroExpandAction? = null,
        init: CfirSessionConfigurator.() -> Unit = {},
    ): CfirSession {
        return super.createSourceSession(
            moduleData,
            context,
            extensionRegistrars,
            configuration,
            macroExpandAction,
            init,
            createProviders = { session, cangjieScopeProvider, symbolProvider, generatedSymbolsProvider ->
                val providers = context.additionalSourceProviders(
                    session,
                    cangjieScopeProvider,
                    symbolProvider,
                    generatedSymbolsProvider,
                )
                val optionalProvider = context.additionalOptionalAnnotationsProvider(
                    session,
                    cangjieScopeProvider,
                    symbolProvider,
                    generatedSymbolsProvider,
                )
                SourceProviders(providers, optionalProvider)
            },
        )
    }

    override fun createPlatformSpecificSharedProviders(
        session: CfirSession,
        moduleData: CfirModuleData,
        scopeProvider: CfirCangJieScopeProvider,
        context: Context,
    ): List<CfirSymbolProvider> {
        return buildList {
            add(CfirBuiltinSymbolProvider(session))
            addAll(context.additionalSharedProviders(session, moduleData, scopeProvider))
        }
    }

    override fun createCangJieScopeProviderForLibrarySession(): CfirCangJieScopeProvider {
        return CfirCangJieScopeProvider()
    }

    override fun CfirSession.registerLibrarySessionComponents(c: Context) {
        c.registerLibrarySessionComponents(this)
    }

    override fun createCangJieScopeProviderForSourceSession(
        moduleData: CfirModuleData,
        languageVersionSettings: LanguageVersionSettings,
    ): CfirCangJieScopeProvider {
        return CfirCangJieScopeProvider()
    }

    override fun CfirSessionConfigurator.registerPlatformCheckers() {
        // Default platform is no-op; platform modules can subclass and register their own checkers.
    }

    override fun CfirSessionConfigurator.registerExtraPlatformCheckers() {
        // Default platform is no-op; platform modules can subclass and register their own extra checkers.
    }

    override fun CfirSession.registerSourceSessionComponents(c: Context) {
        c.registerSourceSessionComponents(this)
    }
}
