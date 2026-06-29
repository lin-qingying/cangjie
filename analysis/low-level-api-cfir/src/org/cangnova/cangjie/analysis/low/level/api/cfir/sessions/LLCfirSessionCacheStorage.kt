

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.caches.cleanable.CleanableSoftValueReferenceCache
import org.cangnova.cangjie.analysis.low.level.api.cfir.caches.cleanable.CleanableValueReferenceCache
import org.cangnova.cangjie.analysis.low.level.api.cfir.caches.cleanable.CleanableWeakValueReferenceCache
import org.cangnova.cangjie.analysis.low.level.api.cfir.caches.cleanable.ValueReferenceCleaner

/**
 * A type of cache which is used by [LLCfirSessionCache] to store [LLCfirSession]s.
 *
 * Removal from the session storage invokes the [LLCfirSession]'s cleaner, which marks the session as invalid and disposes any disposables
 * registered with the session's disposable.
 */
internal typealias SessionStorage = CleanableValueReferenceCache<CaModule, LLCfirSession>

/**
 * Holds all the caches which are operated by [LLCfirSessionCache].
 */
@LLCfirInternals
class LLCfirSessionCacheStorage(
    /**
     * 源码和可解析 session 的缓存。
     */
    val sourceCache: SessionStorage,

    /**
     * 二进制库 session 的缓存。
     */
    val binaryCache: SessionStorage,

    /**
     * A cache for the binary sessions of [org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryFallbackDependenciesModule]s.
     *
     * We keep this cache separate from [binaryCache] for the following reasons:
     *
     * 1. We usually have to invalidate *all* fallback dependencies sessions at once. It's cheaper to clear a whole cache instead of
     *    traversing the binary cache.
     * 2. There is no sense in holding fallback dependencies on soft references, as they exist for a single use-site resolvable library
     *    session. Furthermore, such a session can grow arbitrarily large as it spans (almost) all libraries in the project.
     */
    val libraryFallbackDependenciesCache: SessionStorage,

    /**
     * stable dangling file session 的缓存。
     */
    val danglingFileSessionCache: SessionStorage,

    /**
     * unstable dangling file session 的缓存。
     */
    val unstableDanglingFileSessionCache: SessionStorage,

    /**
     * 为被移除或回收的 session 创建清理器的工厂。
     */
    val getCleaner: (LLCfirSession) -> ValueReferenceCleaner<LLCfirSession>,
) {

    /**
     * 创建共享清理器但复制所有底层缓存内容的 storage 副本。
     */
    fun createCopy(): LLCfirSessionCacheStorage {
        return LLCfirSessionCacheStorage(
            sourceCache = sourceCache.createCopy(),
            binaryCache = binaryCache.createCopy(),
            libraryFallbackDependenciesCache = libraryFallbackDependenciesCache.createCopy(),
            danglingFileSessionCache = danglingFileSessionCache.createCopy(),
            unstableDanglingFileSessionCache = unstableDanglingFileSessionCache.createCopy(),
            getCleaner = getCleaner,
        )
    }

    companion object {
        /**
         * 创建空 session cache storage。
         */
        fun createEmpty(
            getCleaner: (LLCfirSession) -> ValueReferenceCleaner<LLCfirSession>,
        ): LLCfirSessionCacheStorage {
            return LLCfirSessionCacheStorage(
                sourceCache = CleanableWeakValueReferenceCache(getCleaner = getCleaner),
                binaryCache = CleanableSoftValueReferenceCache(getCleaner = getCleaner),
                libraryFallbackDependenciesCache = CleanableWeakValueReferenceCache(getCleaner = getCleaner),
                danglingFileSessionCache = CleanableWeakValueReferenceCache(getCleaner = getCleaner),
                unstableDanglingFileSessionCache = CleanableWeakValueReferenceCache(getCleaner = getCleaner),
                getCleaner = getCleaner,
            )
        }
    }
}
