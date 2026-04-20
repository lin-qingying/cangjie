@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.platform.CaCachedService
import org.cangnova.cangjie.analysis.api.platform.declarations.*
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaDanglingFileModuleImpl
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScopeProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieAnchorModuleProvider
import org.cangnova.cangjie.analysis.api.platform.utils.mergeInto
import org.cangnova.cangjie.analysis.api.projectStructure.*
import org.cangnova.cangjie.analysis.api.util.withCaModuleEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirGlobalResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirLazyDeclarationResolver
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.*
import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.LLCfirIdeRegisteredPluginAnnotations
import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.LLCfirLibrarySessionProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.LLCfirProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.LLNameConflictsTracker
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.*
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.combined.LLCombinedCangJieSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.combined.LLCombinedPackageDelegationSymbolProvider
import org.cangnova.cangjie.cfir.ScopeSession

import org.cangnova.cangjie.cfir.CfirNameConflictsTracker
import org.cangnova.cangjie.cfir.PrivateSessionConstructor
import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.diagnostics.CjRegisteredDiagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.extensions.*
import org.cangnova.cangjie.cfir.resolve.providers.*
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
internal abstract class LLCfirAbstractSessionFactory(protected val project: Project) {
    @CaCachedService
    private val globalResolveComponents: LLCfirGlobalResolveComponents by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLCfirGlobalResolveComponents.getInstance(project)
    }

    @CaCachedService
    private val resolutionScopeProvider: CaResolutionScopeProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CaResolutionScopeProvider.getInstance(project)
    }

    abstract fun createSourcesSession(module: CaSourceModule): LLCfirSourcesSession

    abstract fun createResolvableLibrarySession(module: CaModule): LLCfirLibraryOrLibrarySourceResolvableModuleSession

    /**
     * Creates a binary [LLCfirLibrarySession] for a [CaLibraryModule] or [CaLibraryFallbackDependenciesModule].
     *
     * Both regular libraries and library fallback dependencies can be treated from the same point of view of a binary session. Hence, it
     * doesn't make practical sense to have separate session creation machinery for [CaLibraryFallbackDependenciesModule].
     */
    abstract fun createBinaryLibrarySession(module: CaModule): LLCfirLibrarySession

    abstract fun createProjectLibraryProvidersForScope(
        session: LLCfirSession,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider>

    fun createNotUnderContentRootResolvableSession(module: CaNotUnderContentRootModule): LLCfirNotUnderContentRootResolvableModuleSession {
        val builtinsSession = LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession()
        val languageVersionSettings = LanguageVersionSettings.DEFAULT
        val scopeProvider = CfirCangJieScopeProvider()
        val components = LLCfirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val session = LLCfirNotUnderContentRootResolvableModuleSession(module, components, builtinsSession.builtinTypes)
        components.session = session

        val moduleData = createModuleData(session)
        val resolutionScope = resolutionScopeProvider.getResolutionScope(module)

        return session.apply {
            registerModuleData(moduleData)
            register(CfirCangJieScopeProvider::class, scopeProvider)

            registerAllCommonComponents(languageVersionSettings, module, resolutionScope)
            registerSourceLikeComponents()

            registerCommonComponentsAfterExtensionsAreConfigured()


            val provider = LLCfirProvider(
                this,
                components,
            ) { scope ->
                project.createDeclarationProvider(scope, module)
            }

            register(CfirProvider::class, provider)
            register(CfirLazyDeclarationResolver::class, LLCfirLazyDeclarationResolver())

            val dependencyProvider = LLDependenciesSymbolProvider(this) {
                buildList {
                    addMerged(session, computeDependencySymbolProviders(module))
                    add(builtinsSession.symbolProvider)
                }
            }

            register(
                CfirSymbolProvider::class,
                LLModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        provider.symbolProvider,
                    ),
                    dependencyProvider,
                )
            )

            register(CfirPredicateBasedProvider::class, CfirEmptyPredicateBasedProvider)
            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)
            register(CfirRegisteredPluginAnnotations::class, CfirRegisteredPluginAnnotations.Empty)

            LLCfirSessionConfigurator.configure(this)
        }
    }

    protected class SourceSessionCreationContext(
        val contentScope: GlobalSearchScope,
        val cfirProvider: LLCfirProvider,
        val dependencyProvider: LLDependenciesSymbolProvider,
    )

    protected fun doCreateSourcesSession(
        module: CaSourceModule,
        scopeProvider: CfirCangJieScopeProvider = CfirCangJieScopeProvider(),
        additionalSessionConfiguration: LLCfirSourcesSession.(context: SourceSessionCreationContext) -> Unit,
    ): LLCfirSourcesSession {
        val builtinsSession = LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession()
        val languageVersionSettings = wrapLanguageVersionSettings(module.languageVersionSettings)

        val components = LLCfirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val session = LLCfirSourcesSession(module, components, builtinsSession.builtinTypes) {
            computeDependencySessions(module)
        }

        components.session = session

        val moduleData = createModuleData(session)
        val resolutionScope = resolutionScopeProvider.getResolutionScope(module)

        return session.apply {
            registerModuleData(moduleData)
            register(CfirCangJieScopeProvider::class, scopeProvider)

            registerAllCommonComponents(languageVersionSettings, module, resolutionScope)
            registerSourceLikeComponents()

            val cfirProvider = LLCfirProvider(
                this,
                components,
            ) { scope ->
                project.createDeclarationProvider(scope, module)
            }

            register(CfirProvider::class, cfirProvider)
            register(CfirLazyDeclarationResolver::class, LLCfirLazyDeclarationResolver())

            registerCompilerPluginServices(project, resolutionScope)
            registerCompilerPluginExtensions(project, module)
            registerCommonComponentsAfterExtensionsAreConfigured()

            val dependencyProvider = LLDependenciesSymbolProvider(this) {
                buildList {
                    addMerged(session, computeDependencySymbolProviders(session.dependencies))
                    add(builtinsSession.symbolProvider)
                }
            }

            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)

            LLCfirSessionConfigurator.configure(this)

            val context = SourceSessionCreationContext(
                module.contentScope,
                cfirProvider,
                dependencyProvider,
            )

            additionalSessionConfiguration(context)
        }
    }

    protected class LibrarySessionCreationContext(
        val contentScope: GlobalSearchScope,
        val cfirProvider: LLCfirProvider,
        val dependencyProvider: LLDependenciesSymbolProvider,
    )

    protected fun doCreateResolvableLibrarySession(
        module: CaModule,
        additionalSessionConfiguration: LLCfirLibraryOrLibrarySourceResolvableModuleSession.(context: LibrarySessionCreationContext) -> Unit,
    ): LLCfirLibraryOrLibrarySourceResolvableModuleSession {
        val binaryModule = when (module) {
            is CaLibraryModule, is CaBuiltinsModule -> module
            is CaLibrarySourceModule -> module.binaryLibraryModule
            else -> errorWithAttachment("Unexpected module ${module::class.simpleName}") {
                withCaModuleEntry("module", module)
            }
        }

        val builtinsSession = LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession()
        val languageVersionSettings = LanguageVersionSettings.DEFAULT

        val scopeProvider = CfirCangJieScopeProvider()
        val components = LLCfirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val session = LLCfirLibraryOrLibrarySourceResolvableModuleSession(module, components, builtinsSession.builtinTypes)
        components.session = session

        val moduleData = createModuleData(session)
        val binaryContentScope = binaryModule.contentScope

        return session.apply {
            registerModuleData(moduleData)
            register(CfirCangJieScopeProvider::class, scopeProvider)

            registerAllCommonComponents(languageVersionSettings, module, binaryContentScope)
            registerCommonComponentsAfterExtensionsAreConfigured()

            val cfirProvider = LLCfirProvider(
                this,
                components,
            ) { scope ->
                project.createDeclarationProvider(scope, module)
            }

            register(CfirProvider::class, cfirProvider)

            register(CfirLazyDeclarationResolver::class, LLCfirLazyDeclarationResolver())

            // We need CfirRegisteredPluginAnnotations during extensions' registration process
            val annotationsResolver = project.createAnnotationResolver(binaryContentScope)
            register(CfirRegisteredPluginAnnotations::class, LLCfirIdeRegisteredPluginAnnotations(this, annotationsResolver))
            register(CfirPredicateBasedProvider::class, CfirEmptyPredicateBasedProvider)

            val dependencyProvider = LLDependenciesSymbolProvider(this) {
                buildList {
                    if (module !is CaBuiltinsModule) {
                        add(builtinsSession.symbolProvider)
                    }

                    // The library (source) module will usually have a `CaLibraryFallbackDependenciesModule`, which will be added here, but
                    // this also works when the library (source) module has precise dependencies.
                    addMerged(session, computeDependencySymbolProviders(binaryModule))

                    if (binaryModule is CaLibraryModule) {
                        CangJieAnchorModuleProvider.getInstance(project)?.getAnchorModule(binaryModule)?.let { anchorModule ->
                            val anchorModuleSession = LLCfirSessionCache.getInstance(project).getSession(anchorModule)
                            val anchorModuleSymbolProvider =
                                anchorModuleSession.symbolProvider as LLModuleWithDependenciesSymbolProvider

                            addAll(anchorModuleSymbolProvider.providers)
                            addAll(anchorModuleSymbolProvider.dependencyProvider.providers)
                        }
                    }
                }
            }

            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)

            val context = LibrarySessionCreationContext(binaryContentScope, cfirProvider, dependencyProvider)
            additionalSessionConfiguration(context)

            LLCfirSessionConfigurator.configure(this)
        }
    }

    protected class BinaryLibrarySessionCreationContext

    protected fun doCreateBinaryLibrarySession(
        module: CaModule,
        additionalSessionConfiguration: LLCfirLibrarySession.(context: BinaryLibrarySessionCreationContext) -> Unit,
    ): LLCfirLibrarySession {
        require(module is CaLibraryModule || module is CaLibraryFallbackDependenciesModule) {
            "A binary library session can only be created for a `${CaLibraryModule::class.simpleName}` or a " +
                    "`${CaLibraryFallbackDependenciesModule::class.simpleName}`. Instead got: `${module::class.simpleName}`."
        }

        val builtinsSession = LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession()

        val session = LLCfirLibrarySession(module, builtinsSession.builtinTypes)

        val moduleData = createModuleData(session)
        val contentScope = module.contentScope

        return session.apply {
            val languageVersionSettings = LanguageVersionSettings.DEFAULT
            registerModuleData(moduleData)
            registerIdeComponents(project, languageVersionSettings, contentScope)
            register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
            registerCommonComponents(languageVersionSettings)
            registerCommonComponentsAfterExtensionsAreConfigured()

            val kotlinScopeProvider = CfirCangJieScopeProvider()

            register(CfirCangJieScopeProvider::class, kotlinScopeProvider)

            val symbolProvider = LLModuleWithDependenciesSymbolProvider(
                this,
                providers = createProjectLibraryProvidersForScope(this, contentScope),
                LLDependenciesSymbolProvider(this) {
                    // A binary library session should not have any dependencies (apart from fallback builtins), as library module
                    // dependencies only apply to *resolvable* sessions, including fallback dependencies.
                    listOf(builtinsSession.symbolProvider)
                },
            )

            register(CfirProvider::class, LLCfirLibrarySessionProvider(symbolProvider))
            register(CfirSymbolProvider::class, symbolProvider)

            val context = BinaryLibrarySessionCreationContext()
            additionalSessionConfiguration(context)
            LLCfirSessionConfigurator.configure(this)
        }
    }

    abstract fun createDanglingFileSession(module: CaDanglingFileModule, contextSession: LLCfirSession): LLCfirSession

    protected fun doCreateDanglingFileSession(
        module: CaDanglingFileModule,
        contextSession: LLCfirSession,
        additionalSessionConfiguration: LLCfirDanglingFileSession.(DanglingFileSessionCreationContext) -> Unit,
    ): LLCfirSession {
        val builtinsSession = LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession()
        val languageVersionSettings = wrapLanguageVersionSettings(contextSession.languageVersionSettings)
        val scopeProvider = CfirCangJieScopeProvider()

        val components = LLCfirModuleResolveComponents(module, globalResolveComponents, scopeProvider)

        val session = LLCfirDanglingFileSession(module, components, builtinsSession.builtinTypes)
        components.session = session

        val moduleData = createModuleData(session)
        val resolutionScope = resolutionScopeProvider.getResolutionScope(module)

        return session.apply {
            registerModuleData(moduleData)
            register(CfirCangJieScopeProvider::class, scopeProvider)

            registerAllCommonComponents(languageVersionSettings, module, resolutionScope)
            registerSourceLikeComponents()

            val cfirProvider = LLCfirProvider(
                this,
                components,
                disregardSelfDeclarations = module.resolutionMode == CaDanglingFileResolutionMode.IGNORE_SELF,
                declarationProviderFactory = { scope -> createScopedDeclarationProviderForFiles(scope, module.files) }
            )

            register(CfirProvider::class, cfirProvider)
            register(CfirLazyDeclarationResolver::class, LLCfirLazyDeclarationResolver())

            register(CfirRegisteredPluginAnnotations::class, CfirRegisteredPluginAnnotationsImpl(session))
            register(CfirPredicateBasedProvider::class, CfirEmptyPredicateBasedProvider)

            val contextModule = module.contextModule
            when (contextModule) {
                is CaSourceModule -> {
                    registerCompilerPluginServices(project, resolutionScope)
                    registerCompilerPluginExtensions(project, contextModule)
                }
            }

            registerCommonComponentsAfterExtensionsAreConfigured()

            val dependencyProvider = LLDependenciesSymbolProvider(this) {
                buildList {
                    // The default implementation must have no extra dependencies (so we can delegate to the context module dependencies).
                    // For other implementations, we need to at least perform the check.
                    if (module !is CaDanglingFileModuleImpl) {
                        val allDependencies = computeAggregatedModuleDependencies(module)
                        val contextDependencies = computeAggregatedModuleDependencies(contextModule)

                        val hasAllContextDependencies = contextDependencies.all { it in allDependencies }
                        if (hasAllContextDependencies) {
                            // Exclude dependencies of the context module as they are submitted below
                            val ownDependencies = allDependencies - contextDependencies
                            if (ownDependencies.isNotEmpty()) {
                                val dependencySessions = computeDependencySessionsFromDependencyModules(ownDependencies, module)
                                addMerged(session, computeDependencySymbolProviders(dependencySessions))
                            }
                            // Share symbol providers (and their caches) with the context session
                            addMerged(session, computeDependencySymbolProviders(listOf(contextSession)))
                        } else {
                            // Dependencies are original, so we need a separate set of providers
                            val dependencySessions = computeDependencySessionsFromDependencyModules(allDependencies, module)
                            addMerged(session, computeDependencySymbolProviders(dependencySessions))
                        }
                    } else {
                        addMerged(session, computeDependencySymbolProviders(listOf(contextSession)))
                    }

                    when (contextSession.caModule) {
                        is CaLibraryModule, is CaLibrarySourceModule -> {
                            // Wrap library dependencies into a single classpath-filtering provider
                            // Also see 'LLDanglingFileDependenciesSymbolProvider.filterSymbols()'
                            add(LLDanglingFileDependenciesSymbolProvider(contextSession.dependenciesSymbolProvider))
                        }
                        else -> add(contextSession.dependenciesSymbolProvider)
                    }

                    add(builtinsSession.symbolProvider)
                }
            }

            register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencyProvider)

            LLCfirSessionConfigurator.configure(this)

            val context = DanglingFileSessionCreationContext(
                moduleData,
                dependencyProvider,
            )

            additionalSessionConfiguration(this, context)
        }
    }

    protected class DanglingFileSessionCreationContext(
        val moduleData: LLCfirModuleData,
        val dependencyProvider: LLDependenciesSymbolProvider,
    )

    private fun wrapLanguageVersionSettings(original: LanguageVersionSettings): LanguageVersionSettings {
        return if (original.supportsFeature(LanguageFeature.EnableDfaWarnings)) {
            original
        } else {
            original.copy(enabledFeatures = original.enabledFeatures + LanguageFeature.EnableDfaWarnings)
        }
    }

    private fun computeDependencySessions(module: CaModule): List<LLCfirSession> {
        val dependencyModules = computeAggregatedModuleDependencies(module)
        return computeDependencySessionsFromDependencyModules(dependencyModules, module)
    }

    private fun computeAggregatedModuleDependencies(module: CaModule): Set<CaModule> {
        // Please update KmpModuleSorterTest#buildDependenciesToTest if the logic of collecting dependencies changes
        return buildSet {
            addAll(module.directRegularDependencies)
            addAll(module.directFriendDependencies)

            // The dependency provider needs to have access to all direct and indirect `dependsOn` dependencies, as `dependsOn`
            // dependencies are transitive.
            addAll(module.transitiveDependsOnDependencies)
        }
    }

    private fun computeDependencySessionsFromDependencyModules(dependencyModules: Set<CaModule>, module: CaModule): List<LLCfirSession> {
        val sessionCache = LLCfirSessionCache.getInstance(project)

        fun getOrCreateSessionForDependency(dependency: CaModule): LLCfirSession? = when (dependency) {
            is CaBuiltinsModule -> null // Built-ins are already added

            is CaLibraryModule, is CaLibraryFallbackDependenciesModule -> sessionCache.getDependencySession(dependency)

            is CaSourceModule -> sessionCache.getDependencySession(dependency)

            is CaDanglingFileModule -> {
                requireWithAttachment(dependency.isStable, message = { "Unstable dangling modules cannot be used as a dependency" }) {
                    withCaModuleEntry("module", module)
                    withCaModuleEntry("dependency", dependency)
                    dependency.files.forEachIndexed { index, file -> withPsiEntry("dependencyFile$index", file) }
                }
                sessionCache.getDependencySession(dependency)
            }

            else -> {
                errorWithAttachment("Module ${module::class} cannot depend on ${dependency::class}") {
                    withCaModuleEntry("module", module)
                    withCaModuleEntry("dependency", dependency)
                }
            }
        }

        val orderedDependencyModules = dependencyModules.toList()

        return orderedDependencyModules.mapNotNull(::getOrCreateSessionForDependency)
    }

    private fun computeDependencySymbolProviders(module: CaModule): List<CfirSymbolProvider> =
        computeDependencySymbolProviders(computeDependencySessions(module))

    private fun computeDependencySymbolProviders(dependencySessions: List<LLCfirSession>): List<CfirSymbolProvider> =
        buildList {
            dependencySessions.forEach { session ->
                when (val dependencyProvider = session.symbolProvider) {
                    is LLModuleWithDependenciesSymbolProvider -> dependencyProvider.providers.forEach { it.flattenTo(this) }
                    else -> dependencyProvider.flattenTo(this)
                }
            }
        }

    private fun CfirSymbolProvider.flattenTo(destination: MutableList<CfirSymbolProvider>) {
        when (this) {
            is CfirCompositeSymbolProvider -> providers.forEach { it.flattenTo(destination) }
            else -> destination.add(this)
        }
    }

    private fun createModuleData(session: LLCfirSession): LLCfirModuleData {
        return LLCfirModuleData(session)
    }

    private fun LLCfirSession.registerAllCommonComponents(
        languageVersionSettings: LanguageVersionSettings,
        module: CaModule,
        annotationSearchScope: GlobalSearchScope,
    ) {
        registerIdeComponents(project, languageVersionSettings, annotationSearchScope)
        registerCommonComponents(languageVersionSettings)
        registerResolveComponents(CjRegisteredDiagnosticFactoriesStorage())
    }

    private fun LLCfirSession.registerSourceLikeComponents() {
        register(CfirNameConflictsTracker::class, LLNameConflictsTracker(this))
    }

    /**
     * Merges dependency symbol providers of the same kind, and adds the result to the receiver [MutableList].
     * See [mergeDependencySymbolProvidersInto] for more information on symbol provider merging.
     */
    private fun MutableList<CfirSymbolProvider>.addMerged(session: LLCfirSession, dependencies: List<CfirSymbolProvider>) {
        dependencies.mergeDependencySymbolProvidersInto(session, this)
    }

    /**
     * Merges dependency symbol providers of the same kind if possible. The merged symbol provider usually delegates to its subordinate
     * symbol providers to preserve session semantics, but it will have some form of advantage over individual symbol providers (such as
     * querying an index once instead of N times).
     *
     * [session] should be the session of the dependent module. Because all symbol providers are tied to a session, we need a session to
     * create a combined symbol provider.
     */
    private fun List<CfirSymbolProvider>.mergeDependencySymbolProvidersInto(
        session: LLCfirSession,
        destination: MutableList<CfirSymbolProvider>,
    ) {
        mergeInto(destination) {
            merge<LLCangJieSourceSymbolProvider> { LLCombinedCangJieSymbolProvider.merge(session, project, it) }

            // We place the combined CangJie library symbol provider before the combined Java symbol provider because the former is generally
            // faster due to package and name set checks.
            merge<LLCangJieStubBasedLibrarySymbolProvider> { LLCombinedPackageDelegationSymbolProvider.merge(session, it) }
        }
    }

    /**
     * Creates a [CangJieDeclarationProvider] for the provided files if they are in the search [scope].
     *
     * Otherwise, returns `null`.
     */
    private fun createScopedDeclarationProviderForFiles(scope: GlobalSearchScope, files: List<CjFile>): CangJieDeclarationProvider? {
        if (files.isEmpty()) {
            return null
        }

        val fileProviders = buildList {
            for (file in files) {
                if (file is CjCodeFragment) {
                    // All declarations inside code fragments are local
                    continue
                }

                val virtualFile = file.virtualFile

                // 'CjFile's without a backing 'VirtualFile' can't be covered by a shadow scope, and are thus assumed in-scope.
                if (virtualFile == null || scope.contains(virtualFile)) {
                    add(CangJieFileBasedDeclarationProvider(file))
                }
            }
        }

        return CangJieCompositeDeclarationProvider.create(fileProviders)
    }
}
