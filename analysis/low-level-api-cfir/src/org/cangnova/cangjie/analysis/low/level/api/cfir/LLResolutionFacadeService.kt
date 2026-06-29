/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.projectStructure.*
import org.cangnova.cangjie.analysis.api.util.withCaModuleEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirLibraryOrLibrarySourceResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.analysis.low.level.api.cfir.state.*
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

@LLCfirInternals
/**
 * 为 Analysis API 模块创建 low-level CFIR resolution facade 的 project 级服务。
 */
class LLResolutionFacadeService(project: Project) {
    /**
     * project 级 low-level CFIR session cache。
     */
    private val cache = LLCfirSessionCache.getInstance(project)

    /**
     * 返回指定 use-site 模块对应的 resolution facade。
     */
    fun getResolutionFacade(module: CaModule): LLResolutionFacade {
        return create(module, cache::getSession)
    }

    /**
     * 使用给定 session factory 装配 facade 需要的 module/session/strategy/diagnostic provider。
     */
    private fun create(module: CaModule, factory: (CaModule) -> LLCfirSession): LLResolutionFacade {
        val moduleProvider = LLModuleProvider(module)
        val sessionProvider = LLSessionProvider(module, factory)
        val resolutionStrategyProvider = createResolutionStrategyProvider(module, moduleProvider)
        val diagnosticProvider = createDiagnosticProvider(moduleProvider, sessionProvider)

        return LLResolutionFacade(moduleProvider, resolutionStrategyProvider, sessionProvider, diagnosticProvider)
    }

    /**
     * 根据 use-site 模块种类创建对应的模块解析策略 provider。
     */
    private fun createResolutionStrategyProvider(module: CaModule, moduleProvider: LLModuleProvider): LLModuleResolutionStrategyProvider {
        return when (module) {
            is CaDanglingFileModule -> {
                val contextModule = requireNotNull(module.contextModule) {
                    "Dangling file module must have a context module"
                }
                val contextResolutionStrategyProvider = createResolutionStrategyProvider(contextModule, moduleProvider)
                LLDanglingFileResolutionStrategyProvider(contextResolutionStrategyProvider)
            }
            is CaSourceModule -> LLSourceModuleResolutionStrategyProvider(module)
            is CaLibraryModule, is CaBuiltinsModule, is CaLibrarySourceModule -> LLBinaryModuleResolutionStrategyProvider(module)
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

    /**
     * 为源码或 dangling 文件模块创建 diagnostics provider，其它模块使用空 provider。
     */
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

/**
 * source use-site 模块的解析策略 provider。
 */
private class LLSourceModuleResolutionStrategyProvider(private val useSiteModule: CaModule) : LLModuleResolutionStrategyProvider {
    /**
     * source 模块走 lazy 解析，builtins/library 依赖走 static 解析。
     */
    override fun getKind(module: CaModule): LLModuleResolutionStrategy {
        return when (module) {
            is CaSourceModule -> LLModuleResolutionStrategy.LAZY
            is CaBuiltinsModule, is CaLibraryModule -> LLModuleResolutionStrategy.STATIC
            else -> cannotProvideResolutionStrategy(module, useSiteModule)
        }
    }
}

/**
 * binary/library use-site 模块的解析策略 provider。
 */
private class LLBinaryModuleResolutionStrategyProvider(private val useSiteModule: CaModule) : LLModuleResolutionStrategyProvider {
    /**
     * use-site library source 模块保持 lazy，其余 library/builtins 依赖走 static。
     */
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

/**
 * dangling 文件模块的解析策略 provider，dangling 自身走 lazy，其它模块委托上下文策略。
 */
private class LLDanglingFileResolutionStrategyProvider(private val delegate: LLModuleResolutionStrategyProvider) :
    LLModuleResolutionStrategyProvider {
    /**
     * dangling 模块本身按源码 lazy 解析，其上下文依赖沿用 delegate。
     */
    override fun getKind(module: CaModule): LLModuleResolutionStrategy {
        return when (module) {
            is CaDanglingFileModule -> LLModuleResolutionStrategy.LAZY
            else -> delegate.getKind(module)
        }
    }
}

/**
 * 抛出无法为模块组合提供解析策略的带附件错误。
 */
private fun cannotProvideResolutionStrategy(module: CaModule, useSiteModule: CaModule): Nothing {
    errorWithAttachment("Cannot provide a resolution strategy for `${module::class.simpleName}`.") {
        withCaModuleEntry("module", module)
        withCaModuleEntry("useSiteModule", useSiteModule)
    }
}
