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
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.name.Name

/**
 * [CfirAbstractSessionFactory] 的默认具体实现。
 *
 * 该实现对齐 Kotlin FIR session factory 的结构：公开入口分别创建共享库、普通库和源码会话；
 * 平台相关的 provider 与组件注入通过 [Context] 保持可替换。
 */
@OptIn(SessionConfiguration::class)
open class CfirDefaultSessionFactory : CfirAbstractSessionFactory<CfirDefaultSessionFactory.Context>() {
    /**
     * 默认工厂的宿主上下文。
     *
     * @property cjoManager CJO 依赖管理器；为空时库会话不会注册反序列化符号 provider。
     * @property additionalSharedProviders 共享库会话的额外 provider 注入点。
     * @property additionalLibraryProviders 普通库会话的额外 provider 注入点。
     * @property additionalSourceProviders 源码会话的 source provider 注入点。
     * @property additionalOptionalAnnotationsProvider 源码会话的可选注解 provider 注入点。
     * @property registerLibrarySessionComponents 普通库会话的额外 session component 注册回调。
     * @property registerSourceSessionComponents 源码会话的额外 session component 注册回调。
     */
    class Context(
        /**
         * 当前 session factory 使用的 CJO 依赖管理器。
         */
        val cjoManager: CjoManager? = null,
        /**
         * 共享库会话创建时追加的符号 provider 工厂。
         */
        val additionalSharedProviders: (CfirSession, CfirModuleData, CfirCangJieScopeProvider) -> List<CfirSymbolProvider> =
            { _, _, _ -> emptyList() },
        /**
         * 普通库会话创建时追加的符号 provider 工厂。
         */
        val additionalLibraryProviders: (CfirSession, ModuleDataProvider, CfirCangJieScopeProvider) -> List<CfirSymbolProvider> =
            { _, _, _ -> emptyList() },
        /**
         * 源码会话创建时追加或重排 source provider 的工厂。
         */
        val additionalSourceProviders: (
            CfirSession,
            CfirCangJieScopeProvider,
            CfirSymbolProvider,
            CfirSwitchableExtensionDeclarationsSymbolProvider?,
        ) -> List<CfirSymbolProvider> = { _, _, mainProvider, generatedProvider ->
            listOfNotNull(mainProvider, generatedProvider)
        },
        /**
         * 源码会话创建时追加的可选注解 provider 工厂。
         */
        val additionalOptionalAnnotationsProvider: (
            CfirSession,
            CfirCangJieScopeProvider,
            CfirSymbolProvider,
            CfirSwitchableExtensionDeclarationsSymbolProvider?,
        ) -> CfirSymbolProvider? = { _, _, _, _ -> null },
        /**
         * 普通库会话组件注册完成后的宿主扩展回调。
         */
        val registerLibrarySessionComponents: CfirSession.() -> Unit = {},
        /**
         * 源码会话组件注册完成后的宿主扩展回调。
         */
        val registerSourceSessionComponents: CfirSession.() -> Unit = {},
    )

    /**
     * 创建默认共享库会话。
     *
     * 共享库会话承载 builtins 与平台额外共享 provider，作为所有普通库/源码会话的 fallback
     * 符号来源。
     */
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

    /**
     * 创建默认普通库会话。
     *
     * 默认实现始终注册 [CfirBuiltinSymbolProvider]；当 [Context.cjoManager] 存在时，同时注册
     * [CfirDeserializedSymbolProvider] 用于读取 CJO 依赖符号。
     */
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
                    // Prefer declarations from CJO; the builtin provider is synthetic fallback.
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

                    add(CfirBuiltinSymbolProvider(session))

                    addAll(context.additionalLibraryProviders(session, moduleDataProvider, cangjieScopeProvider))
                }
            },
        )
    }

    /**
     * 创建默认源码会话。
     *
     * 源码会话通过基类完成公共 resolve/checker 组件注册，并由 [Context.additionalSourceProviders]
     * 决定最终参与组合的源码 provider 列表。
     */
    fun createSourceSession(
        moduleData: CfirModuleData,
        extensionRegistrars: List<CfirExtensionRegistrar>,
        configuration: CompilerConfiguration,
        context: Context = Context(),
        init: CfirSessionConfigurator.() -> Unit = {},
    ): CfirSession {
        return super.createSourceSession(
            moduleData,
            context,
            extensionRegistrars,
            configuration,
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

    /**
     * 创建默认共享库 provider。
     *
     * 共享 provider 中的真实声明优先于 synthetic builtin 声明；builtin provider 只补齐缺失项。
     */
    override fun createPlatformSpecificSharedProviders(
        session: CfirSession,
        moduleData: CfirModuleData,
        scopeProvider: CfirCangJieScopeProvider,
        context: Context,
    ): List<CfirSymbolProvider> {
        return buildList {
            addAll(context.additionalSharedProviders(session, moduleData, scopeProvider))
            add(CfirBuiltinSymbolProvider(session))
        }
    }

    /**
     * 创建库会话使用的仓颉作用域提供器。
     */
    override fun createCangJieScopeProviderForLibrarySession(): CfirCangJieScopeProvider {
        return CfirCangJieScopeProvider()
    }

    /**
     * 执行上下文提供的库会话组件注册回调。
     */
    override fun CfirSession.registerLibrarySessionComponents(c: Context) {
        c.registerLibrarySessionComponents(this)
    }

    /**
     * 创建源码会话使用的仓颉作用域提供器。
     */
    override fun createCangJieScopeProviderForSourceSession(
        moduleData: CfirModuleData,
        languageVersionSettings: LanguageVersionSettings,
    ): CfirCangJieScopeProvider {
        return CfirCangJieScopeProvider()
    }

    /**
     * 注册默认平台 checker。
     *
     * 默认平台没有额外 checker；子类可覆盖该方法补充目标平台语义检查。
     */
    override fun CfirSessionConfigurator.registerPlatformCheckers() {
        // 默认平台无额外 checker；平台模块通过子类覆盖该钩子。
    }

    /**
     * 注册默认平台 extra checker。
     *
     * 默认平台没有额外 checker；子类可覆盖该方法补充非默认启用的诊断。
     */
    override fun CfirSessionConfigurator.registerExtraPlatformCheckers() {
        // 默认平台无 extra checker；平台模块通过子类覆盖该钩子。
    }

    /**
     * 执行上下文提供的源码会话组件注册回调。
     */
    override fun CfirSession.registerSourceSessionComponents(c: Context) {
        c.registerSourceSessionComponents(this)
    }
}
