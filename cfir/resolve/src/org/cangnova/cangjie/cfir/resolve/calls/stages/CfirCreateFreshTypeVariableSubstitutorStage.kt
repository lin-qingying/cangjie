package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.constraints.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.diagnostic.InferenceConstraintError
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeDeclaredUpperBoundConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePlaceholderType
import org.cangnova.cangjie.cfir.types.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.ConeTypeVariable
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemOperation

/**
 * 对齐 Kotlin K2 FIR 的 `CreateFreshTypeVariableSubstitutorStage`。
 *
 * 本阶段负责初始化候选的约束系统：
 * - 为声明的类型参数创建新鲜推断类型变量
 * - 在约束系统中注册这些变量
 * - 添加类型参数的上界约束（declared upper bounds）
 * - 处理显式类型实参的相等约束
 *
 * 后续阶段（如 CfirCheckArguments）可在此基础上添加参数/期望类型约束并完成推断。
 */
object CfirCreateFreshTypeVariableSubstitutorStage : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        val declaration = candidate.symbol.cfir
        if (declaration !is CfirTypeParameterRefsOwner || declaration.typeParameters.isEmpty()) {
            candidate.initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
            return
        }

        val csBuilder = candidate.system.getBuilder()
        val (substitutor, freshVariables) =
            createToFreshVariableSubstitutorAndAddInitialConstraints(declaration, csBuilder)
        candidate.initializeSubstitutorAndVariables(substitutor, freshVariables)

        // 声明侧存在矛盾（如上界冲突）——直接标记不可用
        if (csBuilder.hasContradiction) {
            sink.reportDiagnostic(InferenceConstraintError("declaration has contradicting upper bounds"))
            return
        }

        // 无显式类型实参时可提前返回
        if (candidate.typeArgumentMapping == TypeArgumentMapping.NoExplicitArguments) {
            return
        }

        // 处理显式类型实参：添加相等约束
        val typeParameters = declaration.typeParameters
        for (index in typeParameters.indices) {
            val freshVariable = freshVariables[index]
            val typeArgument = candidate.typeArgumentMapping[index]
            val argumentType = typeArgument.type

            if (argumentType is ConePlaceholderType) {
                // 占位符投影——不添加约束，等待推断
                continue
            }

            csBuilder.addEqualityConstraint(
                freshVariable.defaultType,
                argumentType,
                ConeExplicitTypeParameterConstraintPosition(typeArgument),
            )
        }

        if (csBuilder.hasContradiction) {
            for (error in csBuilder.errors) {
                sink.reportDiagnostic(InferenceConstraintError(error.toString()))
            }
        }
    }

    /**
     * 为声明的类型参数创建新鲜类型变量，构建替代器，并添加上界约束。
     */
    private fun createToFreshVariableSubstitutorAndAddInitialConstraints(
        declaration: CfirTypeParameterRefsOwner,
        csBuilder: ConstraintSystemOperation,
    ): Pair<ConeSubstitutor, List<ConeTypeVariable>> {
        val typeParameters = declaration.typeParameters
        val freshTypeVariables = typeParameters.map { ConeTypeParameterBasedTypeVariable(it.symbol) }

        // 构建替代器：类型参数名 → 新鲜类型变量的默认类型
        val replacements = freshTypeVariables.associate {
            it.typeParameterSymbol.name.asString() to (it.defaultType as ConeCangJieType)
        }
        val toFreshVariables = CfirTypeSubstitutorByMap(replacements)

        // 在约束系统中注册所有新鲜变量
        for (freshVariable in freshTypeVariables) {
            csBuilder.registerVariable(freshVariable)
        }

        // 添加上界约束：freshVariable <: substituted(bound)
        for (freshVariable in freshTypeVariables) {
            for (bound in freshVariable.typeParameterSymbol.resolvedBounds) {
                val substitutedBound = toFreshVariables.substituteOrSelf(bound.coneType)
                csBuilder.addSubtypeConstraint(
                    freshVariable.defaultType,
                    substitutedBound,
                    ConeDeclaredUpperBoundConstraintPosition,
                )
            }
        }

        return toFreshVariables to freshTypeVariables
    }
}
