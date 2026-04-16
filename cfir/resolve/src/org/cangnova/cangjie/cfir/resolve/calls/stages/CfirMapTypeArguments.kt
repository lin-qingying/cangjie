package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.diagnostic.WrongArgumentCount
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
 *
 * 同时检查显式类型参数个数是否与声明的泛型参数个数匹配。
 */
object CfirMapTypeArguments : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        val mapping = buildTypeArgumentMapping(candidate)
        candidate.typeArgumentMapping = mapping

        checkTypeArgumentCount(candidate)
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

    /**
     * 当显式类型参数个数与声明泛型参数个数不匹配时报告诊断。
     *
     * 复用已有的 [WrongArgumentCount] ResolutionDiagnostic，
     * 它在 `ConeInapplicableCandidateError` 路径中被映射。
     */
    context(sink: CheckerSink)
    private fun checkTypeArgumentCount(candidate: Candidate) {
        val explicitCount = candidate.callInfo.typeArguments
            .count { it is CfirResolvedTypeRef }
        if (explicitCount == 0) return

        val declaration = candidate.symbol.takeIf { it.isBound }?.cfir ?: return
        val declaredTypeParams = when (declaration) {
            is org.cangnova.cangjie.cfir.declarations.CfirFunction -> declaration.typeParameters
            is org.cangnova.cangjie.cfir.declarations.CfirClass -> declaration.typeParameters
            is org.cangnova.cangjie.cfir.declarations.CfirStruct -> declaration.typeParameters
            is org.cangnova.cangjie.cfir.declarations.CfirEnum -> declaration.typeParameters
            is org.cangnova.cangjie.cfir.declarations.CfirInterface -> declaration.typeParameters
            else -> return
        }

        val expected = declaredTypeParams.size
        if (expected != explicitCount) {
            sink.reportDiagnostic(WrongArgumentCount(expected, explicitCount))
        }
    }
}
