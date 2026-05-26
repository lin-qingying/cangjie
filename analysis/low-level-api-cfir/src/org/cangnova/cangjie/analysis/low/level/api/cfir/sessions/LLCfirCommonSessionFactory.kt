/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleWithDependenciesSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories.LLLibrarySymbolProviderFactory
import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.resolve.CfirDefaultImportsProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirDefaultImportsProviderHolder
import org.cangnova.cangjie.cfir.session.cfirProvider

@OptIn(SessionConfiguration::class)
internal class LLCfirCommonSessionFactory(project: Project) : LLCfirAbstractSessionFactory(project) {
    override fun createSourcesSession(module: CaSourceModule): LLCfirSourcesSession {
        return doCreateSourcesSession(module) { context ->
            register(
                CfirSymbolProvider::class,
                LLModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOfNotNull(
                        context.cfirProvider.symbolProvider,
                    ),
                    context.dependencyProvider,
                )
            )

            registerCommonComponents()
        }
    }

    override fun createResolvableLibrarySession(module: CaModule): LLCfirLibraryOrLibrarySourceResolvableModuleSession {
        return doCreateResolvableLibrarySession(module) { context ->
            register(
                CfirSymbolProvider::class,
                LLModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        context.cfirProvider.symbolProvider,
                    ),
                    context.dependencyProvider,
                )
            )

            registerCommonComponents()
        }
    }

    override fun createBinaryLibrarySession(module: CaModule): LLCfirLibrarySession {
        return doCreateBinaryLibrarySession(module) {
            registerCommonComponents()
        }
    }

    override fun createDanglingFileSession(module: CaDanglingFileModule, contextSession: LLCfirSession): LLCfirSession {
        return doCreateDanglingFileSession(module, contextSession) { context ->
            register(
                CfirSymbolProvider::class,
                LLModuleWithDependenciesSymbolProvider(
                    this,
                    providers = listOf(
                        cfirProvider.symbolProvider,
                    ),
                    context.dependencyProvider,
                )
            )

            registerCommonComponents()
        }
    }

    override fun createProjectLibraryProvidersForScope(
        session: LLCfirSession,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> {
        /**
         * 对齐 Kotlin `LLFirCommonSessionFactory`：
         * binary library session 必须通过统一的 library provider factory 产出真实库符号 provider，
         * 否则依赖侧既拿不到库声明，也无法触发后续 combined-package-delegation merge。
         *
         * 仓颉当前没有 Kotlin/JVM 的 package-part provider 语义，这里沿用本地 factory 约定传入占位对象即可。
         */
        return LLLibrarySymbolProviderFactory
            .fromSettings(project)
            .createCommonLibrarySymbolProvider(
                session = session,
                packagePartProvider = Any(),
                scope = scope,
            )
    }

    private fun LLCfirSession.registerCommonComponents() {
        register(CfirDefaultImportsProviderHolder::class, CfirDefaultImportsProviderHolder.of(CfirDefaultImportsProvider))
    }
}
