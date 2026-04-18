/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.cfir.deserialization.SingleModuleDataProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.impl.CfirFallbackBuiltinSymbolProvider
import org.cangnova.cangjie.cfir.scopes.kotlinScopeProvider
import org.cangnova.cangjie.cfir.session.KlibBasedSymbolProvider
import org.cangnova.cangjie.cfir.session.MetadataSymbolProvider
import org.cangnova.cangjie.library.KlibConstants.KLIB_FILE_EXTENSION
import org.cangnova.cangjie.library.KotlinLibrary
import org.cangnova.cangjie.library.loader.KlibLoader
import org.cangnova.cangjie.load.kotlin.PackageAndMetadataPartProvider
import org.cangnova.cangjie.load.kotlin.PackagePartProvider
import org.cangnova.cangjie.load.kotlin.VirtualFileFinderFactory
import org.cangnova.cangjie.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import org.cangnova.cangjie.util.Logger as KLogger

/**
 * [LLLibrarySymbolProviderFactory] for [KotlinDeserializedDeclarationsOrigin.BINARIES][org.cangnova.cangjie.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.BINARIES].
 */
internal object LLBinaryOriginLibrarySymbolProviderFactory : LLLibrarySymbolProviderFactory {
    override fun createJvmLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> = emptyList()

    override fun createCommonLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> {
        val moduleData = session.moduleData
        val moduleDataProvider = SingleModuleDataProvider(moduleData)
        val kotlinScopeProvider = session.kotlinScopeProvider
        return buildList {
            add(
                MetadataSymbolProvider(
                    session,
                    moduleDataProvider,
                    kotlinScopeProvider,
                    packagePartProvider as PackageAndMetadataPartProvider,
                    VirtualFileFinderFactory.getInstance(session.project).create(scope),
                )
            )

            val kLibs = moduleData.getLibraryKLibs()
            if (kLibs.isNotEmpty()) {
                add(KlibBasedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, kLibs))
            }
        }
    }

    override fun createBuiltinsSymbolProvider(session: LLCfirSession): List<CfirSymbolProvider> =
        listOf(
            createFallbackBuiltinsSymbolProvider(session),
        )

    private fun LLCfirModuleData.getLibraryKLibs(): List<KotlinLibrary> {
        val ktLibraryModule = ktModule as? CaLibraryModule ?: return emptyList()

        return ktLibraryModule.binaryRoots
            .filter { it.isDirectory() || it.extension == KLIB_FILE_EXTENSION }
            .mapNotNull { it.tryResolveAsKLib() }
    }

    private fun Path.tryResolveAsKLib(): KotlinLibrary? {
        return try {
            KlibLoader { libraryPaths(absolutePathString()) }.load().librariesStdlibCfirst.singleOrNull()
        } catch (e: Exception) {
            rethrowIntellijPlatformExceptionIfNeeded(e)
            LOG.warn("Cannot resolve a KLib $this", e)
            null
        }
    }

    private val LOG = Logger.getInstance(LLBinaryOriginLibrarySymbolProviderFactory::class.java)

    private object IntellijLogBasedLogger : KLogger {
        override fun log(message: String) {
            LOG.info(message)
        }

        override fun error(message: String) {
            LOG.error(message)
        }

        override fun warning(message: String) {
            LOG.warn(message)
        }

        @Deprecated(KLogger.FATAL_DEPRECATION_MESSAGE, ReplaceWith(KLogger.FATAL_REPLACEMENT))
        override fun fatal(message: String): Nothing {
            throw IllegalStateException(message)
        }
    }
}

/**
 * 仓颉在 low-level API 中不保留多平台特化 builtins 分支，统一使用基础 builtins provider。
 */
private fun createFallbackBuiltinsSymbolProvider(session: LLCfirSession): CfirSymbolProvider =
    CfirFallbackBuiltinSymbolProvider(session, session.moduleData, session.kotlinScopeProvider)
