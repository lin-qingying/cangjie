package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeTypeProjection


/**
 * 对齐 Kotlin K2 FIR 的 `MapTypeArguments`。
 *
 * 本阶段从 [CallInfo.typeArguments] 构建 [TypeArgumentMapping] 并赋值给候选。
 * 在 [CfirCreateFreshTypeVariableSubstitutorStage] **之前**执行，
 * 确保后续创建新鲜类型变量时可以读取显式类型实参。
 */
object CfirMapTypeArguments : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        candidate.typeArgumentMapping = buildTypeArgumentMapping(candidate)
    }

    private fun buildTypeArgumentMapping(candidate: Candidate): TypeArgumentMapping {
        val explicitTypeArguments = candidate.callInfo.typeArguments
            .mapNotNull { it as? CfirResolvedTypeRef }
            .map { ConeTypeProjection(it.coneType) }

        if (explicitTypeArguments.isEmpty()) {
            return TypeArgumentMapping.NoExplicitArguments
        }

        return TypeArgumentMapping.Mapped(explicitTypeArguments)
    }
}
