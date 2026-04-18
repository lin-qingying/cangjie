/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.projectStructure.*
import org.cangnova.cangjie.analysis.api.utils.errors.withCaModuleEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirLibraryOrLibrarySourceResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.analysis.low.level.api.cfir.state.*
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

@LLCfirInternals
class LLResolutionFacadeService(project: Project) {
    private val cache = LLCfirSessionCache.getInstance(project)

    fun getResolutionFacade(module: CaModule): LLResolutionFacade {
        return create(module, cache::getSession)
    }

    private fun create(module: CaModule, factory: (CaModule) -> LLCfirSession): LLResolutionFacade {
        val moduleProvider = LLModuleProvider(module)
        val sessionProvider = LLSessionProvider(module, factory)
        val resolutionStrategyProvider = createResolutionStrategyProvider(module, moduleProvider)
        val diagnosticProvider = createDiagnosticProvider(moduleProvider, sessionProvider)

        return LLResolutionFacade(moduleProvider, resolutionStrategyProvider, sessionProvider, diagnosticProvider)
    }

    private fun createResolutionStrategyProvider(module: CaModule, moduleProvider: LLModuleProvider): LLModuleResolutionStrategyProvider {
        return when (module) {
            is CaSourceModule -> LLSourceModuleResolutionStrategyProvider(module)
            is CaLibraryModule, is CaBuiltinsModule, is CaLibrarySourceModule -> LLBinaryModuleResolutionStrategyProvider(module)
            is CaDanglingFileModule -> {
                val contextModule = module.contextModule
                val contextResolutionStrategyProvider = createResolutionStrategyProvider(contextModule, moduleProvider)
                LLDanglingFileResolutionStrategyProvider(contextResolutionStrategyProvider)
            }
            is CaNotUnderContentRootModule -> LLSimpleResolutionStrategyProvider(module)
            else -> {
                errorWithCfirSpecificEntries(
                    "`${module::class.java}` does not have a corresponding resolution strategy (resolvable: ${module.isResolvable}).",
                ) {
                    withEntry("module", module) { it.moduleDescription }
                }
            }
        }
    }

    private fun createDiagnosticProvider(moduleProvider: LLModuleProvider, sessionProvider: LLSessionProvider): LLDiagnosticProvider {
        return when (moduleProvider.useSiteModule) {
            is CaSourceModule,
            is CaDanglingFileModule
                -> LLSourceDiagnosticProvider(moduleProvider, sessionProvider)
            else -> LLEmptyDiagnosticProvider
        }
    }

    companion object {
        fun getInstance(project: Project): LLResolutionFacadeService =
            project.getService(LLResolutionFacadeService::class.java)
    }
}

private class LLSourceModuleResolutionStrategyProvider(private val useSiteModule: CaModule) : LLModuleResolutionStrategyProvider {
    override fun getKind(module: CaModule): LLModuleResolutionStrategy {
        return when (module) {
            is CaSourceModule -> LLModuleResolutionStrategy.LAZY
            is CaBuiltinsModule, is CaLibraryModule -> LLModuleResolutionStrategy.STATIC
            else -> cannotProvideResolutionStrategy(module, useSiteModule)
        }
    }
}

private class LLBinaryModuleResolutionStrategyProvider(private val useSiteModule: CaModule) : LLModuleResolutionStrategyProvider {
    override fun getKind(module: CaModule): LLModuleResolutionStrategy {
        LLCfirLibraryOrLibrarySourceResolvableModuleSession.checkIsValidCjModule(module)
        // Providing `LLModuleResolutionStrategy.LAZY` strategy for `CaLibrarySourceModule` is a workaround,
        // as `CaLibrarySourceModule` should not be used as dependencies.
        // It was added after including the project library scope
        // in resolution scopes of all `CaLibrarySourceModule`s and `CaLibraryModule`s.
        // See KT-75838
        return if (module == useSiteModule || module is CaLibrarySourceModule) LLModuleResolutionStrategy.LAZY else LLModuleResolutionStrategy.STATIC
    }
}

private class LLDanglingFileResolutionStrategyProvider(private val delegate: LLModuleResolutionStrategyProvider) :
    LLModuleResolutionStrategyProvider {
    override fun getKind(module: CaModule): LLModuleResolutionStrategy {
        return when (module) {
            is CaDanglingFileModule -> LLModuleResolutionStrategy.LAZY
            else -> delegate.getKind(module)
        }
    }
}

private fun cannotProvideResolutionStrategy(module: CaModule, useSiteModule: CaModule): Nothing {
    errorWithAttachment("Cannot provide a resolution strategy for `${module::class.simpleName}`.") {
        withCaModuleEntry("module", module)
        withCaModuleEntry("useSiteModule", useSiteModule)
    }
}
