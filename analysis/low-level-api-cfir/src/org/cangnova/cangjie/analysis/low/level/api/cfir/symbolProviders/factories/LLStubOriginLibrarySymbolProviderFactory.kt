/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories

import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.BuiltinsDeserializedContainerSourceProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.JvmAndBuiltinsDeserializedContainerSourceProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.NullDeserializedContainerSourceProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.*
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLKotlinStubBasedLibrarySymbolProvider
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeCachedSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.kotlinScopeProvider
import org.cangnova.cangjie.load.kotlin.PackagePartProvider
import org.cangnova.cangjie.psi.CjFile

/**
 * [LLLibrarySymbolProviderFactory] for [KotlinDeserializedDeclarationsOrigin.STUBS][org.cangnova.cangjie.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.STUBS].
 */
internal object LLStubOriginLibrarySymbolProviderFactory : LLLibrarySymbolProviderFactory {
    override fun createJvmLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> {
        return listOf(
            LLKotlinStubBasedLibrarySymbolProvider(
                session,
                JvmAndBuiltinsDeserializedContainerSourceProvider,
                scope,
            )
        )
    }

    override fun createCommonLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> = listOf(
        LLKotlinStubBasedLibrarySymbolProvider(session, NullDeserializedContainerSourceProvider, scope),
    )

    override fun createBuiltinsSymbolProvider(session: LLCfirSession): List<CfirSymbolProvider> {
        return listOf(StubBasedBuiltInsSymbolProvider(session))
    }
}

private class StubBasedBuiltInsSymbolProvider(session: LLCfirSession) : LLKotlinStubBasedLibrarySymbolProvider(
    session,
    BuiltinsDeserializedContainerSourceProvider,
    BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(session.project),
) {
    override val symbolNamesProvider: CfirSymbolNamesProvider = CfirCompositeCachedSymbolNamesProvider(
        session,
        listOf(
            LLCfirKotlinSymbolNamesProvider(declarationProvider, allowKotlinPackage),
        ),
    )

    override fun getDeclarationOriginFor(file: CjFile): CfirDeclarationOrigin {
        // this provider operates only on builtins files, no need to check anything
        return CfirDeclarationOrigin.BuiltIns
    }
}
