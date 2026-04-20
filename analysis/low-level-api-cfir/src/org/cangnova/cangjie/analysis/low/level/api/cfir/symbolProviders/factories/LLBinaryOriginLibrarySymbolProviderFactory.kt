/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories

import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirBuiltinSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider

/**
 * [LLLibrarySymbolProviderFactory] for [KotlinDeserializedDeclarationsOrigin.BINARIES][org.cangnova.cangjie.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.BINARIES].
 */
internal object LLBinaryOriginLibrarySymbolProviderFactory : LLLibrarySymbolProviderFactory {
    override fun createJvmLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: LLPackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> =
        createCommonLibrarySymbolProvider(session, packagePartProvider, scope)

    override fun createCommonLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: LLPackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> =
        listOf(createDeserializedLibrarySymbolProvider(session))

    override fun createBuiltinsSymbolProvider(session: LLCfirSession): List<CfirSymbolProvider> =
        listOf(
            CfirBuiltinSymbolProvider(session),
        )

    private fun createDeserializedLibrarySymbolProvider(session: LLCfirSession): CfirSymbolProvider =
        CfirDeserializedSymbolProvider(
            session = session,
            cjoManager = CjoManager(CjoSearchPath()),
            cangjieScopeProvider = session.cangjieScopeProvider,
            libraryModuleData = session.moduleData,
        )
}
