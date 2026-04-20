@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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

internal inline fun createCompositeSymbolProvider(
    session: CfirSession,
    createSubProviders: MutableList<CfirSymbolProvider>.() -> Unit
): CfirCompositeSymbolProvider =
    CfirCompositeSymbolProvider(session, buildList(createSubProviders))

@SessionConfiguration
internal fun CfirSession.registerCompilerPluginExtensions(project: Project, module: CaModule) {
    CfirSessionConfigurator(this).apply {
        this@apply.registerCompilerPluginExtensions(project, module)
    }.configure()
}

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

private fun CfirSessionConfigurator.applyExtensionRegistrar(registrar: CfirExtensionRegistrarAdapter) {
    val extensions = (registrar as CfirExtensionRegistrar).configure()
    registerExtensions(extensions)
}

@SessionConfiguration
internal fun LLCfirSession.registerCompilerPluginServices(project: Project, resolutionScope: GlobalSearchScope) {
    val annotationsResolver = project.createAnnotationResolver(resolutionScope)

    // We need CfirRegisteredPluginAnnotations and CfirPredicateBasedProvider during extensions' registration process
    register(CfirRegisteredPluginAnnotations::class, LLCfirIdeRegisteredPluginAnnotations(this, annotationsResolver))
    register(CfirPredicateBasedProvider::class, LLCfirIdePredicateBasedProvider(this, annotationsResolver))
}
