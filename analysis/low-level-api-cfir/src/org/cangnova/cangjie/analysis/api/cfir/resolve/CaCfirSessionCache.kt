package org.cangnova.cangjie.analysis.api.cfir.resolve

import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryFallbackDependenciesModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.deserialization.SingleModuleDataProvider
import org.cangnova.cangjie.cfir.entrypoint.configuration.createForCfirFrontend
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.config.CompilerConfiguration
import java.util.concurrent.ConcurrentHashMap

/**
 * low-level CFIR session cache。
 *
 * 对齐 Kotlin `LLFirSessionCache` 的设计意图，这里缓存的是编译器底层 session，而不是 Analysis API session。
 * 它负责把 Analysis API 模块图转换成底层 `CfirSession` 依赖图，并维持如下边界：
 * 1. source-like 模块创建可解析的 source session；
 * 2. binary-like 模块创建依赖边界专用的 library session；
 * 3. dangling/original module 语义在 cache 层统一并入 regular dependencies。
 */
internal class CaCfirSessionCache(
    private val globalResolveComponents: CaCfirGlobalResolveComponents,
) {
    private val cache = ConcurrentHashMap<CaModule, CfirSession>()
    private val sessionFactoryContext: CfirDefaultSessionFactory.Context by lazy(LazyThreadSafetyMode.PUBLICATION) {
        createSessionFactoryContext()
    }

    fun getSession(
        module: CaModule,
        strategyProvider: CaCfirModuleResolutionStrategyProvider? = null,
    ): CfirSession {
        cache[module]?.let { return it }

        val newSession = createSession(
            module = module,
            strategy = strategyProvider?.getKind(module) ?: defaultStrategy(module),
            strategyProvider = strategyProvider,
        )
        val existing = cache.putIfAbsent(module, newSession)
        return existing ?: newSession
    }

    fun invalidate(modules: Set<CaModule>) {
        modules.forEach(cache::remove)
    }

    @OptIn(CompilerConfiguration.Internals::class)
    private fun createSession(
        module: CaModule,
        strategy: CaCfirModuleResolutionStrategy,
        strategyProvider: CaCfirModuleResolutionStrategyProvider?,
    ): CfirSession {
        return when (strategy) {
            CaCfirModuleResolutionStrategy.LAZY -> createLazySession(module, strategyProvider)
            CaCfirModuleResolutionStrategy.STATIC -> createStaticSession(module)
        }
    }

    @OptIn(CompilerConfiguration.Internals::class)
    private fun createLazySession(
        module: CaModule,
        strategyProvider: CaCfirModuleResolutionStrategyProvider?,
    ): CfirSession {
        require(isLazyCapableModule(module)) {
            "模块 `${module.moduleDescription}` 当前被请求为 LAZY 解析，但它不是可构建源码 session 的模块。"
        }

        val sessionFactory = CfirDefaultSessionFactory()
        val moduleName = globalResolveComponents.getModuleName(module)
        val configuration = CompilerConfiguration.createForCfirFrontend()
        val dependencySessions = collectDependencySessions(module, strategyProvider)
        val bootstrapLibraryModuleData = createBootstrapLibrarySession(
            sessionFactory = sessionFactory,
            module = module,
            moduleName = moduleName,
        )

        val moduleData = CfirSourceModuleData(
            name = moduleName,
            dependencies = buildList {
                add(bootstrapLibraryModuleData)
                dependencySessions.regular.forEach { add(it.moduleData) }
            },
            refinementDependencies = dependencySessions.dependsOn.map { it.moduleData },
            platform = module.toCfirPlatform(),
        )

        return sessionFactory.createSourceSession(
            moduleData = moduleData,
            extensionRegistrars = emptyList(),
            configuration = configuration,
            context = sessionFactoryContext,
        )
    }

    private fun createStaticSession(module: CaModule): CfirSession {
        require(isStaticCapableModule(module)) {
            "模块 `${module.moduleDescription}` 当前被请求为 STATIC 解析，但它没有可用的稳定依赖语义。"
        }

        val sessionFactory = CfirDefaultSessionFactory()
        val moduleName = globalResolveComponents.getModuleName(module)
        val languageVersionSettings = globalResolveComponents.getLanguageVersionSettings(module)
        val sharedLibrarySession = sessionFactory.createSharedLibrarySession(
            mainModuleName = moduleName,
            extensionRegistrars = emptyList(),
            languageVersionSettings = languageVersionSettings,
            context = sessionFactoryContext,
        )

        return sessionFactory.createLibrarySession(
            sharedLibrarySession = sharedLibrarySession,
            moduleDataProvider = SingleModuleDataProvider(
                CfirBinaryDependenciesModuleData(name = moduleName),
            ),
            extensionRegistrars = emptyList(),
            languageVersionSettings = languageVersionSettings,
            context = sessionFactoryContext,
        )
    }

    /**
     * 每个 source-like session 都显式持有一份“基础二进制依赖”模块数据。
     *
     * 这是当前 CFIR session 结构的真实要求：source session 在拼装依赖 provider 时，
     * 必须至少有一份 library-kind 依赖提供 shared provider。该语义应该归属于 low-level cache，
     * 不能再由 facade service 或 Analysis API 组件偷偷补全。
     */
    @OptIn(CompilerConfiguration.Internals::class)
    private fun createBootstrapLibrarySession(
        sessionFactory: CfirDefaultSessionFactory,
        module: CaModule,
        moduleName: org.cangnova.cangjie.name.Name,
    ): CfirModuleData {
        val dependencyList = DependencyListForCliModule.build(moduleName) {}
        val sharedLibrarySession = sessionFactory.createSharedLibrarySession(
            mainModuleName = moduleName,
            extensionRegistrars = emptyList(),
            languageVersionSettings = globalResolveComponents.getLanguageVersionSettings(module),
            context = sessionFactoryContext,
        )
        val librarySession = sessionFactory.createLibrarySession(
            sharedLibrarySession = sharedLibrarySession,
            moduleDataProvider = dependencyList.moduleDataProvider,
            extensionRegistrars = emptyList(),
            languageVersionSettings = globalResolveComponents.getLanguageVersionSettings(module),
            context = sessionFactoryContext,
        )
        return librarySession.moduleData
    }

    /**
     * 底层 CFIR 统一从系统属性/环境变量解析 `.cjo` 搜索路径。
     *
     * 这样 IDE、LSP、测试等不同宿主只需要把平台协商结果落到同一组键上，
     * session cache 不再感知具体宿主来源。
     */
    private fun createSessionFactoryContext(): CfirDefaultSessionFactory.Context {
        val cjoManager = CjoManager(
            CjoSearchPath { key ->
                when (key) {
                    "CANGJIE_STDLIB_MODULE" -> systemPropertyOrEnv("cangjie.stdlib.module", key)
                    "CANGJIE_LIBRARY" -> systemPropertyOrEnv("cangjie.library", key)
                    else -> System.getenv(key)
                }
            }
        )
        return CfirDefaultSessionFactory.Context(cjoManager = cjoManager)
    }

    private fun systemPropertyOrEnv(
        propertyKey: String,
        envKey: String,
    ): String? {
        return System.getProperty(propertyKey)
            ?.takeIf { it.isNotBlank() }
            ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }
    }

    /**
     * 将 Analysis API 模块依赖拓扑映射到底层 session 依赖集合。
     *
     * 仓颉没有 Kotlin 那套 friend module 机制，因此这里只保留：
     * - regular dependencies
     * - dependsOn dependencies
     * - dangling context / outside-root original module 的 use-site 语义依赖
     */
    private fun collectDependencySessions(
        module: CaModule,
        strategyProvider: CaCfirModuleResolutionStrategyProvider?,
    ): DependencySessions {
        val regularDependencies = linkedSetOf<CaModule>()
        regularDependencies += module.directRegularDependencies

        if (module is CaDanglingFileModule) {
            module.contextModule?.let(regularDependencies::add)
        }
        if (module is CaNotUnderContentRootModule) {
            module.originalModule?.let(regularDependencies::add)
        }

        return DependencySessions(
            regular = regularDependencies.map { dependency ->
                getSession(dependency, strategyProvider)
            },
            dependsOn = module.directDependsOnDependencies.map { dependency ->
                getSession(dependency, strategyProvider)
            },
        )
    }

    private fun defaultStrategy(module: CaModule): CaCfirModuleResolutionStrategy {
        return if (isStaticCapableModule(module) && !isLazyCapableModule(module)) {
            CaCfirModuleResolutionStrategy.STATIC
        } else {
            CaCfirModuleResolutionStrategy.LAZY
        }
    }

    private fun isLazyCapableModule(module: CaModule): Boolean {
        return module is CaSourceModule ||
            module is CaLibrarySourceModule ||
            module is CaNotUnderContentRootModule
    }

    private fun isStaticCapableModule(module: CaModule): Boolean {
        return module is CaLibraryModule ||
            module is CaBuiltinsModule ||
            module is CaLibraryFallbackDependenciesModule
    }

    private data class DependencySessions(
        val regular: List<CfirSession>,
        val dependsOn: List<CfirSession>,
    )
}

private fun CaModule.toCfirPlatform(): CfirPlatform = CfirPlatform.DEFAULT
