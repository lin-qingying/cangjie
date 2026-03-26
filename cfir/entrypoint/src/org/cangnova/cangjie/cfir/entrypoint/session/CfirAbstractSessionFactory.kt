package org.cangnova.cangjie.cfir.entrypoint.session

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.PrivateSessionConstructor
import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.deserialization.ModuleDataProvider
import org.cangnova.cangjie.cfir.extensions.CfirExtensionRegistrar
import org.cangnova.cangjie.cfir.extensions.CfirSwitchableExtensionDeclarationsSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.CfirSessionExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.session.registerCliCompilerOnlyResolveComponents
import org.cangnova.cangjie.cfir.session.registerResolveComponents
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.entrypoint.checkers.registerCommonCheckers
import org.cangnova.cangjie.cfir.entrypoint.configuration.diagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceLogger

import org.cangnova.cangjie.cfir.resolve.providers.CfirLibrarySessionProvider
import org.cangnova.cangjie.cfir.resolve.CfirDefaultImportsProvider

import org.cangnova.cangjie.cfir.scopes.CfirDefaultImportsProviderHolder
import org.cangnova.cangjie.cfir.session.registerModuleData
import org.cangnova.cangjie.cfir.session.StructuredProviders
import org.cangnova.cangjie.cfir.session.DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY
import org.cangnova.cangjie.cfir.session.registerCliCompilerAndCommonComponents
import org.cangnova.cangjie.cfir.session.registerCommonComponentsAfterExtensionsAreConfigured
import org.cangnova.cangjie.cfir.session.extendIndexStore
import org.cangnova.cangjie.cfir.session.structuredProviders
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.config.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.addIfNotNull

/**
 * 会话工厂基类，用于创建编译阶段使用的多种会话。
 *
 * 会话类型：
 * - 源码会话：承载待分析源码及语义组件。
 * - 依赖会话：承载常规依赖（类路径）。
 * - 共享依赖会话：承载内建与合成依赖。
 */
@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
abstract class CfirAbstractSessionFactory<CONTEXT> {

    // ==================================== 共享库会话 ====================================

    /**
     * 创建共享库会话，注册内置与合成符号提供器。
     */
    protected fun createSharedLibrarySession(
        mainModuleName: Name,
        context: CONTEXT,
        languageVersionSettings: LanguageVersionSettings,
        extensionRegistrars: List<CfirExtensionRegistrar>
    ): CfirSession {
        return CfirDefaultSession(CfirSession.Kind.Library).apply session@{
            registerCliCompilerAndCommonComponents(languageVersionSettings)
            registerLibrarySessionComponents(context)

            val cangjieScopeProvider = createCangJieScopeProviderForLibrarySession()
            register(CfirCangJieScopeProvider::class, cangjieScopeProvider)

            val moduleData = CfirBinaryDependenciesModuleData(
                Name.special("<shared dependencies of ${mainModuleName.asString()}>")
            )
            moduleData.bindSession(this)
            register(CfirModuleData::class, moduleData)

            CfirSessionConfigurator(this).apply {
                for (extensionRegistrar in extensionRegistrars) {
                    registerExtensions(extensionRegistrar.configure())
                }
            }.configure()
            registerCommonComponentsAfterExtensionsAreConfigured()

            val providers = createSharedProviders(this, moduleData, cangjieScopeProvider, context)
            val symbolProvider = CfirCompositeSymbolProvider(this, providers)
            register(CfirSymbolProvider::class, symbolProvider)
            register(CfirProvider::class, CfirLibrarySessionProvider(symbolProvider))
        }
    }

    private fun createSharedProviders(
        session: CfirSession,
        moduleData: CfirModuleData,
        scopeProvider: CfirCangJieScopeProvider,
        context: CONTEXT,
    ): List<CfirSymbolProvider> {
        return buildList {
            addAll(createPlatformSpecificSharedProviders(session, moduleData, scopeProvider, context))
        }
    }

    protected abstract fun createPlatformSpecificSharedProviders(
        session: CfirSession,
        moduleData: CfirModuleData,
        scopeProvider: CfirCangJieScopeProvider,
        context: CONTEXT,
    ): List<CfirSymbolProvider>

    // ==================================== 普通库会话 ====================================

    /**
     * 创建普通库会话，注册常规依赖（类路径）的符号提供器。
     */
    protected fun createLibrarySession(
        context: CONTEXT,
        sharedLibrarySession: CfirSession,
        moduleDataProvider: ModuleDataProvider,
        languageVersionSettings: LanguageVersionSettings,
        extensionRegistrars: List<CfirExtensionRegistrar>,
        createProviders: (CfirSession, CfirCangJieScopeProvider) -> List<CfirSymbolProvider>
    ): CfirSession {
        return CfirDefaultSession(CfirSession.Kind.Library).apply session@{
            moduleDataProvider.allModuleData.forEach {
                it.bindSession(this)
            }

            registerCliCompilerAndCommonComponents(languageVersionSettings)
            registerLibrarySessionComponents(context)

            val cangjieScopeProvider = createCangJieScopeProviderForLibrarySession()
            register(CfirCangJieScopeProvider::class, cangjieScopeProvider)

            CfirSessionConfigurator(this).apply {
                for (extensionRegistrar in extensionRegistrars) {
                    registerExtensions(extensionRegistrar.configure())
                }
            }.configure()
            registerCommonComponentsAfterExtensionsAreConfigured()

            val providers = createProviders(this@session, cangjieScopeProvider)

            register(
                StructuredProviders::class,
                StructuredProviders(
                    sourceProviders = emptyList(),
                    dependencyProviders = providers,
                    sharedProvider = sharedLibrarySession.symbolProvider,
                )
            )

            val providersWithShared = providers + sharedLibrarySession.symbolProvider.flattenAndFilterOwnProviders()

            val symbolProvider = CfirCompositeSymbolProvider(this, providersWithShared)
            register(CfirSymbolProvider::class, symbolProvider)
            register(CfirProvider::class, CfirLibrarySessionProvider(symbolProvider))
        }
    }

    protected abstract fun createCangJieScopeProviderForLibrarySession(): CfirCangJieScopeProvider
    abstract fun CfirSession.registerLibrarySessionComponents(c: CONTEXT)

    // ==================================== 平台源码会话 ====================================

    /**
     * 创建源码会话，用于源文件语义分析。
     */
    protected fun createSourceSession(
        moduleData: CfirModuleData,
        context: CONTEXT,
        extensionRegistrars: List<CfirExtensionRegistrar>,
        configuration: CompilerConfiguration,
        init: CfirSessionConfigurator.() -> Unit,
        createProviders: (
            CfirSession, CfirCangJieScopeProvider, CfirSymbolProvider,
            CfirSwitchableExtensionDeclarationsSymbolProvider?,
        ) -> SourceProviders
    ): CfirSession {
        val languageVersionSettings = configuration.languageVersionSettings
        return CfirDefaultSession(CfirSession.Kind.Source).apply session@{
            moduleData.bindSession(this@session)
            registerModuleData(moduleData)
            if (configuration.dumpInferenceLogs) register(CfirInferenceLogger::class, CfirInferenceLogger())
            registerCliCompilerAndCommonComponents(languageVersionSettings)
            registerResolveComponents(
                configuration.diagnosticFactoriesStorage ?: error("diagnosticFactoriesStorage is not registered in the configuration"),
                configuration.lookupTracker,
                configuration.enumMatchTracker,
                configuration.importTracker,
                configuration.fileMappingTracker,
            )
            registerCliCompilerOnlyResolveComponents()
            register(CfirDefaultImportsProviderHolder::class, CfirDefaultImportsProviderHolder.of(CfirDefaultImportsProvider))

            registerSourceSessionComponents(context)

            val cangjieScopeProvider = createCangJieScopeProviderForSourceSession(moduleData, languageVersionSettings)
            register(CfirCangJieScopeProvider::class, cangjieScopeProvider)

            val cfirProvider = CfirProviderImpl(this, cangjieScopeProvider)
            register(CfirProvider::class, cfirProvider)

            // 注册 extend 声明提供器（惰性初始化，首次查询时才扫描所有文件）
            register(
                CfirExtendProvider::class,
                CfirSessionExtendProvider(extendIndexStore),
            )

            CfirSessionConfigurator(this).apply {
                registerCommonCheckers()
                registerPlatformCheckers()
                if (configuration.useCfirExtraCheckers) {
                    registerExtraPlatformCheckers()
                }

                for (extensionRegistrar in extensionRegistrars) {
                    registerExtensions(extensionRegistrar.configure())
                }
                init()
            }.configure()
            registerCommonComponentsAfterExtensionsAreConfigured()

            val structuredDependencyProvidersWithoutSource = computeDependencyProviderList(
                this,
                moduleData,
            )
            val generatedSymbolsProvider = CfirSwitchableExtensionDeclarationsSymbolProvider.createIfNeeded(this)

            val (sourceProviders, additionalOptionalAnnotationsProvider) = createProviders(
                this,
                cangjieScopeProvider,
                cfirProvider.symbolProvider,
                generatedSymbolsProvider,
            )

            val allLibrariesProviders = buildList {
                addAll(structuredDependencyProvidersWithoutSource.dependencyProviders)
                addIfNotNull(additionalOptionalAnnotationsProvider)
            }

            val structuredProvidersForModule = StructuredProviders(
                sourceProviders = sourceProviders,
                dependencyProviders = allLibrariesProviders,
                sharedProvider = structuredDependencyProvidersWithoutSource.sharedProvider,
            ).also {
                register(StructuredProviders::class, it)
            }

            val providersListWithoutSources = buildList {
                structuredProvidersForModule.dependencyProviders.flatMapTo(this) { it.flattenAndFilterOwnProviders() }
                addAll(structuredProvidersForModule.sharedProvider.flattenAndFilterOwnProviders())
            }.distinct()

            val providersList = structuredProvidersForModule.sourceProviders + providersListWithoutSources

            register(
                CfirSymbolProvider::class,
                CfirCompositeSymbolProvider(this, providersList)
            )

            generatedSymbolsProvider?.let { register(CfirSwitchableExtensionDeclarationsSymbolProvider::class, it) }
            register(
                DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY,
                CfirCompositeSymbolProvider(this, providersListWithoutSources)
            )
        }
    }

    protected data class SourceProviders(
        val sourceProviders: List<CfirSymbolProvider>,
        val additionalOptionalAnnotationsProvider: CfirSymbolProvider? = null,
    )

    protected abstract fun createCangJieScopeProviderForSourceSession(
        moduleData: CfirModuleData, languageVersionSettings: LanguageVersionSettings
    ): CfirCangJieScopeProvider

    abstract fun CfirSessionConfigurator.registerPlatformCheckers()
    abstract fun CfirSessionConfigurator.registerExtraPlatformCheckers()
    abstract fun CfirSession.registerSourceSessionComponents(c: CONTEXT)

    // ==================================== 工具方法 ====================================

    private fun computeDependencyProviderList(
        session: CfirSession,
        moduleData: CfirModuleData,
    ): StructuredProviders {
        val providersFromDependencies = moduleData.dependencies
            .distinctBy { it.session }
            .sortedBy { it.session.kind }
            .map { it to it.session.structuredProviders }

        val dependencyProviders = providersFromDependencies.flatMap { (dependencyModuleData, providers) ->
            when (dependencyModuleData.session.kind) {
                CfirSession.Kind.Library -> providers.dependencyProviders.also { check(providers.sourceProviders.isEmpty()) }
                CfirSession.Kind.Source -> providers.sourceProviders
            }
        }

        // 取第一个二进制依赖作为共享提供器，用于支持常规模块间的源码到源码依赖。
        val sharedProvider = providersFromDependencies
            .first { (moduleData, _) -> moduleData.session.kind == CfirSession.Kind.Library }
            .second.sharedProvider

        return StructuredProviders(
            sourceProviders = emptyList(),
            dependencyProviders,
            sharedProvider
        )
    }

    /* 该逻辑会在扁平化时剔除多余依赖/组合提供器，避免重复与解析错误。
    *  扁平化过程中会根据顶层提供器的会话类型过滤掉其他模块的库/源码提供器。 */
    fun CfirSymbolProvider.flattenAndFilterOwnProviders(): List<CfirSymbolProvider> {
        val originalSession = session.takeIf { it.kind == CfirSession.Kind.Source }
        return flatten { provider ->
            originalSession != null && provider.session.kind == CfirSession.Kind.Source && provider.session == originalSession ||
                    originalSession == null && provider.session.kind == CfirSession.Kind.Library
        }
    }

    private fun CfirSymbolProvider.flatten(predicate: (CfirSymbolProvider) -> Boolean): List<CfirSymbolProvider> {
        val result = mutableListOf<CfirSymbolProvider>()

        fun CfirSymbolProvider.collectProviders() {
            when {
                // 当前为组合提供器时，展开内部提供器并递归处理。
                this is CfirCompositeSymbolProvider -> {
                    for (provider in providers) {
                        provider.collectProviders()
                    }
                }

                predicate(this) -> {
                    result.add(this)
                }
            }
        }

        collectProviders()

        return result
    }

}
