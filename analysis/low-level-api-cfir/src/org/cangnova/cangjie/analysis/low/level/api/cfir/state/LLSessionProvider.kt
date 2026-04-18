

package org.cangnova.cangjie.analysis.low.level.api.cfir.state

import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

class LLSessionProvider(
    val useSiteModule: CaModule,
    private val useSiteSessionFactory: (CaModule) -> LLCfirSession
) {
    /**
     * The [LLCfirSession] must be strongly reachable from the resolvable session and ultimately the `CaCfirSession` so that soft
     * reference garbage collection doesn't collect the [LLCfirSession] without collecting its dependent `CaCfirSession`. See
     * [LLCfirSession] for more details.
     */
    val useSiteSession: LLCfirSession by lazy(LazyThreadSafetyMode.PUBLICATION) { useSiteSessionFactory(useSiteModule) }

    /**
     * Returns an [LLCfirSession] for the [module].
     * For a binary module, the resulting session will be a binary (non-resolvable) one.
     */
    fun getSession(module: CaModule): LLCfirSession {
        return getSession(module, preferBinary = true)
    }

    /**
     * Returns an analyzable [LLCfirSession] for the module.
     * For a binary module, the resulting session will still be a resolvable one.
     *
     * Note: prefer using [getSession] unless you need to perform resolution actively.
     * Resolvable sessions for libraries are much less performant.
     */
    fun getResolvableSession(module: CaModule): LLCfirResolvableModuleSession {
        return getSession(module, preferBinary = false) as LLCfirResolvableModuleSession
    }

    private fun getSession(module: CaModule, preferBinary: Boolean): LLCfirSession {
        if (module == useSiteModule) {
            return useSiteSession
        }

        val cache = LLCfirSessionCache.getInstance(module.project)
        return cache.getSession(module, preferBinary)
    }

    /**
     * Returns the [LLCfirSession] for [module], to be used as a *dependency*, or `null` if it doesn't make sense to create such a session as
     * a dependency. This is an optimization for [CaModule]s of certain kinds, like empty modules.
     */
    fun getDependencySession(module: CaModule): LLCfirSession? {
        if (module == useSiteModule) return useSiteSession

        val cache = LLCfirSessionCache.getInstance(module.project)
        return cache.getDependencySession(module)
    }
}
