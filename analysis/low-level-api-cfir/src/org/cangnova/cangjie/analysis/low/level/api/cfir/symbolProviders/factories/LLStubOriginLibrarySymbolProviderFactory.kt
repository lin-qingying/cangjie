

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories

import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.BuiltinsDeserializedContainerSourceProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.NullDeserializedContainerSourceProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.StubAndBuiltinsDeserializedContainerSourceProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLCangJieStubBasedLibrarySymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider

/**
 * [LLLibrarySymbolProviderFactory] for [KotlinDeserializedDeclarationsOrigin.STUBS][org.cangnova.cangjie.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.STUBS].
 */
internal object LLStubOriginLibrarySymbolProviderFactory : LLLibrarySymbolProviderFactory {
    override fun createJvmLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: LLPackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> {
        return listOf(
            LLCangJieStubBasedLibrarySymbolProvider(
                session,
                StubAndBuiltinsDeserializedContainerSourceProvider,
                scope,
            )
        )
    }

    override fun createCommonLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: LLPackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> = listOf(
        LLCangJieStubBasedLibrarySymbolProvider(session, NullDeserializedContainerSourceProvider, scope),
    )

    override fun createBuiltinsSymbolProvider(session: LLCfirSession): List<CfirSymbolProvider> {
        return listOf(
            LLCangJieStubBasedLibrarySymbolProvider(
                session,
                BuiltinsDeserializedContainerSourceProvider,
                BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(session.project),
            ),
        )
    }
}
