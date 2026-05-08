/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.CaBuiltinsModuleImpl
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.LLCfirBuiltinsSessionProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirBuiltinsSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories.LLLibrarySymbolProviderFactory
import org.cangnova.cangjie.cfir.PrivateSessionConstructor
import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver

@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class, CaPlatformInterface::class)
@LLCfirInternals
class LLCfirBuiltinsSessionFactory(private val project: Project) {
    private val builtInTypes = CfirBuiltinTypes()
    private val builtinsModule: CaBuiltinsModule = CaBuiltinsModuleImpl(project)
    private val projectStructureProvider: CangJieProjectStructureProvider = CangJieProjectStructureProvider.getInstance(project)
    @Volatile
    private var builtinsSession: CachedValue<LLCfirBuiltinsSession>? = null

    /**
     * Returns the builtins [CaBuiltinsModule]. [getBuiltinsModule] should be used instead of [getBuiltinsSession] when a
     * [CaBuiltinsModule] is needed as a dependency for other [CaModule][org.cangnova.cangjie.analysis.api.projectStructure.CaModule]s. This
     * is because during project structure creation, we have to avoid the creation of the builtins *session*, as not all services might have
     * been registered at that point.
     */
    fun getBuiltinsModule(): CaBuiltinsModule = builtinsModule

    fun getBuiltinsSession(): LLCfirBuiltinsSession {
        builtinsSession?.let { return it.value }

        synchronized(this) {
            builtinsSession?.let { return it.value }

            val cachedValue = CachedValuesManager.getManager(project).createCachedValue {
                val session = createBuiltinsSession()
                CachedValueProvider.Result(session, session.createValidityTracker())
            }
            builtinsSession = cachedValue
            return cachedValue.value
        }
    }

    /**
     * Invalidates all builtins modules and sessions.
     *
     * [invalidateAll] should be called after [global module state modification][org.cangnova.cangjie.analysis.api.platform.modification.KotlinGlobalModuleStateModificationEvent],
     * as well as after [module state modification][org.cangnova.cangjie.analysis.api.platform.modification.KotlinModuleStateModificationEvent]
     * of a [CaBuiltinsModule]. Modification of builtins might affect any session, so in addition to the builtins sessions, all other
     * sessions should also be invalidated.
     *
     * Builtins cannot be affected by out-of-block modification.
     *
     * The method must be called in a write action, or alternatively when the caller can guarantee that no other threads can perform
     * invalidation or code analysis until the invalidation is complete.
     */
    internal fun invalidateAll() {
        builtinsSession = null
    }

    private fun createBuiltinsSession(): LLCfirBuiltinsSession {
        val session = LLCfirBuiltinsSession(builtinsModule, builtInTypes)
        val moduleData = LLCfirModuleData(session)

        return session.apply {
            val languageVersionSettings = projectStructureProvider.libraryLanguageVersionSettings
            registerIdeComponents(project, languageVersionSettings, builtinsModule.contentScope)
            register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
            registerCommonComponents(languageVersionSettings)
            registerCommonComponentsAfterExtensionsAreConfigured()
            registerModuleData(moduleData)
            val cangjieScopeProvider = CfirCangJieScopeProvider()
            register(CfirCangJieScopeProvider::class, cangjieScopeProvider)

            val symbolProvider = createCompositeSymbolProvider(this) {
                addAll(
                    LLLibrarySymbolProviderFactory
                        .fromSettings(project)
                        .createBuiltinsSymbolProvider(session)
                )
            }

            register(CfirSymbolProvider::class, symbolProvider)
            register(CfirProvider::class, LLCfirBuiltinsSessionProvider(symbolProvider))
        }
    }

    companion object {
        fun getInstance(project: Project): LLCfirBuiltinsSessionFactory = project.service()
    }
}
