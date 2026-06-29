

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import java.util.*

/**
 * 维护 CFIR 解析阶段到低阶懒解析器的映射。
 *
 * 调用方通过该对象把 [CfirResolvePhase] 分派到对应的 [LLCfirLazyResolver]，从而复用统一的阶段解析入口。
 */
internal object LLCfirLazyPhaseResolverByPhase {
    /**
     * 各个支持懒解析的阶段与解析器实例之间的映射表。
     */
    private val byPhase = EnumMap<CfirResolvePhase, LLCfirLazyResolver>(CfirResolvePhase::class.java).apply {
        this[CfirResolvePhase.SUPER_TYPES] = LLCfirSupertypeLazyResolver
        this[CfirResolvePhase.TYPES] = LLCfirTypeLazyResolver
        this[CfirResolvePhase.STATUS] = LLCfirStatusLazyResolver
        this[CfirResolvePhase.EXTENSIONS] = LLCfirExtensionsLazyResolver
        this[CfirResolvePhase.IMPLICIT_TYPES] = LLCfirImplicitTypesLazyResolver
        this[CfirResolvePhase.BODY_RESOLVE] = LLCfirBodyLazyResolver
    }

    /**
     * 返回 [phase] 对应的低阶懒解析器。
     */
    fun getByPhase(phase: CfirResolvePhase): LLCfirLazyResolver = byPhase.getValue(phase)
}
