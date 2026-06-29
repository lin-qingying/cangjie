@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)



package org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.declarations.createAnnotationResolver
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieCompilerPluginsProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.low.level.api.cfir.caches.CfirThreadSafeCachesFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.compile.CodeFragmentScopeProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.LLCheckersFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.*
import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.LLCfirPrivateVisibleFromDifferentModuleExtension
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.CfirElementFinder
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.LLCfirExceptionHandler
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.caches.CfirCachesFactory
import org.cangnova.cangjie.cfir.declarations.CfirHiddenDeprecationProvider
import org.cangnova.cangjie.cfir.entrypoint.session.configure
import org.cangnova.cangjie.cfir.extensions.CfirExtensionRegistrar
import org.cangnova.cangjie.cfir.extensions.CfirExtensionRegistrarAdapter
import org.cangnova.cangjie.cfir.extensions.CfirPredicateBasedProvider
import org.cangnova.cangjie.cfir.extensions.CfirRegisteredPluginAnnotations
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirLookupDefaultStarImportsInSourcesSettingHolder
import org.cangnova.cangjie.cfir.entrypoint.session.CfirSessionConfigurator
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 向低阶 CFIR session 注册 IDE 分析环境需要的公共组件。
 *
 * 这些组件覆盖线程安全缓存、异常处理、代码片段作用域、元素查找、可见性扩展、默认导入策略和诊断 checker。
 */
@SessionConfiguration
internal fun LLCfirSession.registerIdeComponents(
    project: Project,
    languageVersionSettings: LanguageVersionSettings,
    annotationSearchScope: GlobalSearchScope
) {
    register(CfirCachesFactory::class, CfirThreadSafeCachesFactory(project))
    register(CfirExceptionHandler::class, LLCfirExceptionHandler)
    register(CodeFragmentScopeProvider::class, CodeFragmentScopeProvider(this))
    register(CfirElementFinder::class, CfirElementFinder())
    register(CfirPrivateVisibleFromDifferentModuleExtension::class, LLCfirPrivateVisibleFromDifferentModuleExtension(this))
    register(
        CfirLookupDefaultStarImportsInSourcesSettingHolder::class,
        createLookupDefaultStarImportsInSourcesSettingHolder(languageVersionSettings)
    )
    register(LLCheckersFactory::class, LLCheckersFactory(this))
    register(CfirHiddenDeprecationProvider::class, LLHiddenDeprecationProvider(this))
}

/**
 * 创建由多个子 provider 组成的 [CfirCompositeSymbolProvider]。
 *
 * [createSubProviders] 负责按调用方 session 类型添加源码、库、builtins 或其他 symbol provider。
 */
internal inline fun createCompositeSymbolProvider(
    session: CfirSession,
    createSubProviders: MutableList<CfirSymbolProvider>.() -> Unit
): CfirCompositeSymbolProvider =
    CfirCompositeSymbolProvider(session, buildList(createSubProviders))

/**
 * 在普通 [CfirSession] 上创建配置器并注册当前模块可见的编译器插件扩展。
 */
@SessionConfiguration
internal fun CfirSession.registerCompilerPluginExtensions(project: Project, module: CaModule) {
    CfirSessionConfigurator(this).apply {
        this@apply.registerCompilerPluginExtensions(project, module)
    }.configure()
}

/**
 * 向 [CfirSessionConfigurator] 注册 IDE 全局和模块级编译器插件扩展。
 *
 * 全局扩展来自 IntelliJ extension point；模块级扩展来自 [CangJieCompilerPluginsProvider]。
 */
@SessionConfiguration
internal fun CfirSessionConfigurator.registerCompilerPluginExtensions(project: Project, module: CaModule) {
    project.extensionArea.getExtensionPoint<CfirExtensionRegistrarAdapter>(CfirExtensionRegistrarAdapter.name)
        .extensionList
        .forEach(::applyExtensionRegistrar)

    val pluginsProvider = CangJieCompilerPluginsProvider.getInstance(project) ?: return
    pluginsProvider
        .getRegisteredExtensions(module, CfirExtensionRegistrarAdapter)
        .forEach(::applyExtensionRegistrar)
}

/**
 * 应用单个 [registrar] 产生的 CFIR 扩展集合。
 */
private fun CfirSessionConfigurator.applyExtensionRegistrar(registrar: CfirExtensionRegistrarAdapter) {
    val extensions = (registrar as CfirExtensionRegistrar).configure()
    registerExtensions(extensions)
}

/**
 * 注册编译器插件扩展在配置阶段依赖的 IDE 服务。
 *
 * 插件注解注册表和 predicate provider 必须先于扩展配置存在，扩展才能查询用户注解和声明谓词。
 */
@SessionConfiguration
internal fun LLCfirSession.registerCompilerPluginServices(project: Project, resolutionScope: GlobalSearchScope) {
    val annotationsResolver = project.createAnnotationResolver(resolutionScope)

    // We need CfirRegisteredPluginAnnotations and CfirPredicateBasedProvider during extensions' registration process
    register(CfirRegisteredPluginAnnotations::class, LLCfirIdeRegisteredPluginAnnotations(this, annotationsResolver))
    register(CfirPredicateBasedProvider::class, LLCfirIdePredicateBasedProvider(this, annotationsResolver))
}
