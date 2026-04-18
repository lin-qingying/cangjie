

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import java.util.*

/**
 * Maps [CfirResolvePhase] to the associated [LLCfirTargetResolver].
 */
internal object LLCfirLazyPhaseResolverByPhase {
    private val byPhase = EnumMap<CfirResolvePhase, LLCfirLazyResolver>(CfirResolvePhase::class.java).apply {
        this[CfirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS] = LLCfirCompilerAnnotationsLazyResolver
        this[CfirResolvePhase.SUPER_TYPES] = LLCfirSupertypeLazyResolver
        this[CfirResolvePhase.SEALED_CLASS_INHERITORS] = LLCfirSealedClassInheritorsLazyResolver
        this[CfirResolvePhase.TYPES] = LLCfirTypeLazyResolver
        this[CfirResolvePhase.STATUS] = LLCfirStatusLazyResolver
        this[CfirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE] = LLCfirImplicitTypesLazyResolver
        this[CfirResolvePhase.CONSTANT_EVALUATION] = LLCfirConstantEvaluationLazyResolver
        this[CfirResolvePhase.ANNOTATION_ARGUMENTS] = LLCfirAnnotationArgumentsLazyResolver
        this[CfirResolvePhase.BODY_RESOLVE] = LLCfirBodyLazyResolver
    }

    fun getByPhase(phase: CfirResolvePhase): LLCfirLazyResolver = byPhase.getValue(phase)
}
