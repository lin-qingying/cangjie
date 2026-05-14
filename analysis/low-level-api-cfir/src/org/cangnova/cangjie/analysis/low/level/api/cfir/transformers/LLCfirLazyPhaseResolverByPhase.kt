

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import java.util.*

/**
 * Maps [CfirResolvePhase] to the associated [LLCfirTargetResolver].
 */
internal object LLCfirLazyPhaseResolverByPhase {
    private val byPhase = EnumMap<CfirResolvePhase, LLCfirLazyResolver>(CfirResolvePhase::class.java).apply {
        this[CfirResolvePhase.SUPER_TYPES] = LLCfirSupertypeLazyResolver
        this[CfirResolvePhase.TYPES] = LLCfirTypeLazyResolver
        this[CfirResolvePhase.STATUS] = LLCfirStatusLazyResolver
        this[CfirResolvePhase.EXTENSIONS] = LLCfirExtensionsLazyResolver
        this[CfirResolvePhase.IMPLICIT_TYPES] = LLCfirImplicitTypesLazyResolver
        this[CfirResolvePhase.BODY_RESOLVE] = LLCfirBodyLazyResolver
    }

    fun getByPhase(phase: CfirResolvePhase): LLCfirLazyResolver = byPhase.getValue(phase)
}
