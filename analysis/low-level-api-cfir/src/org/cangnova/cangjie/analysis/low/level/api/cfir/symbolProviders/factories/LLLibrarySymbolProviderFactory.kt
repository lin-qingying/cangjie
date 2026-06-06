

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider

internal typealias LLPackagePartProvider = Any

/**
 * [LLLibrarySymbolProviderFactory] creates symbol providers in accordance with [CaPlatformSettings.deserializedDeclarationsOrigin].
 * Its implementations should be lightweight as the factory is neither a service nor cached.
 */
internal interface LLLibrarySymbolProviderFactory {
    fun createJvmLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: LLPackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider>

    fun createCommonLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: LLPackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider>

    /**
     * Creates a symbol provider for a [fallback builtins module][org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule].
     *
     * Since fallback builtins don't have any class ID ambiguities, their symbol providers don't have to implement
     * [LLPsiAwareSymbolProvider][org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLPsiAwareSymbolProvider].
     */
    fun createBuiltinsSymbolProvider(session: LLCfirSession): List<CfirSymbolProvider>

    companion object {
        fun fromSettings(project: Project): LLLibrarySymbolProviderFactory {
            val platformSettings = CaPlatformSettings.getInstance(project)
            return when (platformSettings.deserializedDeclarationsOrigin) {
                CaDeserializedDeclarationsOrigin.BINARIES -> LLBinaryOriginLibrarySymbolProviderFactory
                CaDeserializedDeclarationsOrigin.STUBS -> LLStubOriginLibrarySymbolProviderFactory
            }
        }
    }
}
