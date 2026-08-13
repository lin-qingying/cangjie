/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.entrypoint.session

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.PrivateSessionConstructor
import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.deserialization.ModuleDataProvider
import org.cangnova.cangjie.cfir.entrypoint.checkers.registerCommonCheckers
import org.cangnova.cangjie.cfir.entrypoint.configuration.checkProgramEntry
import org.cangnova.cangjie.cfir.entrypoint.configuration.diagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.entrypoint.configuration.noPrelude
import org.cangnova.cangjie.cfir.entrypoint.configuration.noSubPackage
import org.cangnova.cangjie.cfir.extensions.CfirExtensionRegistrar
import org.cangnova.cangjie.cfir.extensions.CfirSwitchableExtensionDeclarationsSymbolProvider
import org.cangnova.cangjie.cfir.resolve.CfirDefaultImportsProvider
import org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceLogger
import org.cangnova.cangjie.cfir.resolve.providers.*
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.scopes.CfirDefaultImportsProviderHolder
import org.cangnova.cangjie.cfir.serialization.provider.AbstractCfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedExtendProvider
import org.cangnova.cangjie.cfir.serialization.provider.flattenDeserializedProviders
import org.cangnova.cangjie.cfir.session.*
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
            register(CfirExtendProvider::class, createLibraryExtendProvider(providers))
        }
    }

    /**
     * 汇总共享库会话的 provider 列表。
     *
     * 当前基类只委托平台钩子创建 provider，保留独立方法是为了把“共享 provider 汇总”与
     * “平台 provider 选择”两个职责分开，后续新增公共共享 provider 时不会影响平台实现。
     */
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

    /**
     * 创建共享库会话的宿主平台符号提供器。
     *
     * 共享库会话只承载内建、合成或平台级全局符号，不绑定具体源码模块；平台实现可在这里追加
     * 标准库、内建类型或工具链合成声明的 provider。
     *
     * @param session 正在构造的共享库会话。
     * @param moduleData 共享依赖模块数据。
     * @param scopeProvider 共享库会话使用的仓颉作用域提供器。
     * @param context 宿主工厂传入的平台上下文。
     * @return 需要注册到共享库会话的符号 provider 列表。
     */
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
            // 1. 绑定所有模块数据
            moduleDataProvider.allModuleData.forEach {
                it.bindSession(this)
            }
            // 2. 注册主模块数据（修复 Analysis API 启动崩溃的关键点）
            registerModuleData(moduleDataProvider.regularDependenciesModuleData)

            // 3. 注册公共组件
            registerCliCompilerAndCommonComponents(languageVersionSettings)
            registerLibrarySessionComponents(context)

            // 4. 注册 ScopeProvider
            val cangjieScopeProvider = createCangJieScopeProviderForLibrarySession()
            register(CfirCangJieScopeProvider::class, cangjieScopeProvider)

            // 5. 配置扩展点
            CfirSessionConfigurator(this).apply {
                for (extensionRegistrar in extensionRegistrars) {
                    registerExtensions(extensionRegistrar.configure())
                }
            }.configure()
            registerCommonComponentsAfterExtensionsAreConfigured()

            // 6. 构建 Providers 列表
            val providers = createProviders(this@session, cangjieScopeProvider)

            // 7. 注册结构化 Providers
            register(
                StructuredProviders::class,
                StructuredProviders(
                    sourceProviders = emptyList(),
                    dependencyProviders = providers,
                    sharedProvider = sharedLibrarySession.symbolProvider,
                )
            )

            // 8. 最终符号提供器合成（常规库依赖 + 共享库符号）
            val providersWithShared = providers + sharedLibrarySession.symbolProvider.flattenAndFilterOwnProviders()

            // 9. 注册核心 Provider 服务
            val symbolProvider = CfirCompositeSymbolProvider(this, providersWithShared)
            register(CfirSymbolProvider::class, symbolProvider)
            register(CfirProvider::class, CfirLibrarySessionProvider(symbolProvider))

            // 10. 注册 Extend Provider (仓颉特有)
            register(
                CfirExtendProvider::class,
                combineExtendProviders(
                    ownProvider = createLibraryExtendProvider(providers),
                    dependencyProviders = listOfNotNull(sharedLibrarySession.extendProviderOrNull),
                ),
            )
        }
    }

    /**
     * 为普通库会话创建仓颉作用域提供器。
     *
     * 库会话负责反序列化依赖声明并提供跨模块符号查询，因此它的 scope provider 必须与
     * deserialized provider 使用同一套作用域模型。
     */
    protected abstract fun createCangJieScopeProviderForLibrarySession(): CfirCangJieScopeProvider

    /**
     * 注册普通库会话的宿主平台组件。
     *
     * 该钩子运行在公共组件注册之后、provider 装配之前，用于平台实现注入库解析需要的额外
     * session component。
     *
     * @param c 宿主工厂上下文。
     */
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
            register(CfirPreludeSettingsComponent::class, CfirPreludeSettingsComponent(configuration.noPrelude))
            register(
                CfirPackageCompilationSettingsComponent::class,
                CfirPackageCompilationSettingsComponent(configuration.noSubPackage),
            )
            register(
                CfirProgramEntrySettingsComponent::class,
                CfirProgramEntrySettingsComponent(configuration.checkProgramEntry)
            )
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
            val sourceExtendProvider = CfirSessionExtendProvider(this, extendIndexStore)

            // 注册 extend 声明提供器（惰性初始化，首次查询时才扫描所有文件）
            register(
                CfirExtendProvider::class,
                sourceExtendProvider,
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

            register(
                CfirExtendProvider::class,
                combineExtendProviders(
                    ownProvider = sourceExtendProvider,
                    dependencyProviders = moduleData.dependencies
                        .distinctBy { it.session }
                        .mapNotNull { it.session.extendProviderOrNull },
                ),
            )

            generatedSymbolsProvider?.let { register(CfirSwitchableExtensionDeclarationsSymbolProvider::class, it) }
            register(
                DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY,
                CfirCompositeSymbolProvider(this, providersListWithoutSources)
            )
        }
    }

    /**
     * 源码会话 provider 构造结果。
     *
     * @property sourceProviders 当前源码模块自身声明的 provider，包括源码 provider 与插件生成 provider。
     * @property additionalOptionalAnnotationsProvider 额外可选注解 provider；为空表示没有平台补充注解来源。
     */
    protected data class SourceProviders(
        /**
         * 当前源码模块自身声明的 provider 列表。
         */
        val sourceProviders: List<CfirSymbolProvider>,
        /**
         * 平台或宿主额外提供的可选注解 provider。
         */
        val additionalOptionalAnnotationsProvider: CfirSymbolProvider? = null,
    )

    /**
     * 为源码会话创建仓颉作用域提供器。
     *
     * 源码会话的作用域提供器参与源码声明构建、import resolve 和后续成员查询，必须绑定当前
     * [moduleData] 与语言版本配置的语义。
     */
    protected abstract fun createCangJieScopeProviderForSourceSession(
        moduleData: CfirModuleData, languageVersionSettings: LanguageVersionSettings
    ): CfirCangJieScopeProvider

    /**
     * 注册平台默认 checker。
     *
     * 该钩子在公共 checker 注册之后执行，用于 JVM、Native 或 IDE 等宿主追加平台语义检查。
     */
    abstract fun CfirSessionConfigurator.registerPlatformCheckers()

    /**
     * 注册平台额外 checker。
     *
     * 仅当配置开启 extra checkers 时调用，适合放置成本较高或非默认启用的诊断规则。
     */
    abstract fun CfirSessionConfigurator.registerExtraPlatformCheckers()

    /**
     * 注册源码会话的宿主平台组件。
     *
     * 该钩子在公共 session 组件注册之后执行，用于平台实现注入源码 resolve/checker 阶段需要的
     * 自定义服务。
     *
     * @param c 宿主工厂上下文。
     */
    abstract fun CfirSession.registerSourceSessionComponents(c: CONTEXT)

    // ==================================== 工具方法 ====================================

    /**
     * 根据当前模块依赖计算源码会话可见的结构化 provider。
     *
     * 依赖 session 分为库 session 与源码 session：库依赖贡献 dependency provider，源码依赖贡献
     * source provider。共享 provider 从第一个库依赖继承，保持所有源码模块共用同一套 builtins 与
     * 合成声明来源。
     */
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

    /**
     * 展开组合 provider，并按当前顶层 provider 的会话边界保留本会话可见 provider。
     *
     * 源码会话只保留同一源码 session 的 provider；库会话只保留库 provider。这样可以避免依赖
     * provider 在组合 provider 中重复出现，也避免跨源码模块错误地共享当前模块声明。
     */
    fun CfirSymbolProvider.flattenAndFilterOwnProviders(): List<CfirSymbolProvider> {
        val originalSession = session.takeIf { it.kind == CfirSession.Kind.Source }
        return flatten { provider ->
            originalSession != null && provider.session.kind == CfirSession.Kind.Source && provider.session == originalSession ||
                    originalSession == null && provider.session.kind == CfirSession.Kind.Library
        }
    }

    /**
     * 递归展开 [CfirCompositeSymbolProvider]。
     *
     * @param predicate 过滤最终叶子 provider 的谓词。
     * @return 按原组合顺序收集的叶子 provider 列表。
     */
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

/**
 * 从库符号 provider 列表中构造 extend provider。
 *
 * 只有反序列化 provider 能提供二进制依赖中的 extend 元数据；当依赖中没有这类 provider 时返回
 * 空实现，避免库会话暴露不存在的 extend 查询能力。
 */
private fun createLibraryExtendProvider(providers: List<CfirSymbolProvider>): CfirExtendProvider {
    val deserializedProviders = providers
        .flatMap(CfirSymbolProvider::flattenDeserializedProviders)
        .distinct()
    return if (deserializedProviders.isEmpty()) {
        CfirEmptyExtendProvider()
    } else {
        CfirDeserializedExtendProvider(deserializedProviders)
    }
}

/**
 * 合并当前会话与依赖会话的 extend provider。
 *
 * 当前会话 provider 始终位于第一位，依赖 provider 去重后追加，保证源码 extend 查询优先使用
 * 当前模块索引，再回退到依赖模块。
 */
private fun combineExtendProviders(
    ownProvider: CfirExtendProvider,
    dependencyProviders: List<CfirExtendProvider>,
): CfirExtendProvider {
    val providers = buildList {
        add(ownProvider)
        addAll(dependencyProviders.filter { it !== ownProvider })
    }.distinct()
    return if (providers.size == 1) providers.single() else CfirCompositeExtendProvider(providers)
}
