

package org.cangnova.cangjie.analysis.low.level.api.cfir.state

import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase

interface LLModuleResolutionStrategyProvider {
    /**
     * Returns [LLModuleResolutionStrategy.STATIC] if the [module] is treated as a binary for the current session,
     * and [LLModuleResolutionStrategy.LAZY] otherwise.
     *
     * In some cases, modules of the same type might be treated differently by the session, and have a different [LLModuleResolutionStrategy].
     * For instance, for a resolvable library session, only the target library is considered resolvable, and its dependencies are binary.
     */
    fun getKind(module: CaModule): LLModuleResolutionStrategy
}

/**
 * Specifies the way declarations are loaded and handled in the module.
 */
enum class LLModuleResolutionStrategy {
    /**
     * When a module is analyzed with a [LAZY] resolution strategy, its declarations might be in an unresolved
     * (or partially resolved) state. Call [lazyResolveToPhase] on the declarations before usage if needed.
     *
     * Some modules, such as [CaSourceModule], are always analyzed as [LAZY].
     * [CaLibraryModule] can be analyzed both ways (different types of sessions will be created).
     */
    LAZY,

    /**
     * With a [STATIC] resolution strategy, all declarations in a module are always considered fully resolved. Typically, they are loaded
     * from a binary storage, such as a JAR file, or a klib, where the complete type information is present.
     * Normally, declarations inside [STATIC] modules do not change. On a backing binary storage change, the whole session is invalided.
     */
    STATIC
}

/**
 * A resolution strategy that treats all modules but the [useSiteModule] as [LLModuleResolutionStrategy.STATIC].
 */
internal class LLSimpleResolutionStrategyProvider(private val useSiteModule: CaModule) : LLModuleResolutionStrategyProvider {
    override fun getKind(module: CaModule): LLModuleResolutionStrategy {
        return when (module) {
            useSiteModule -> LLModuleResolutionStrategy.LAZY
            else -> LLModuleResolutionStrategy.STATIC
        }
    }
}
