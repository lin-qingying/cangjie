package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.inference.CfirConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.CfirConstraintSystem
import org.cangnova.cangjie.cfir.resolve.inference.CfirTypeVariable
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType

/**
 * 泛型类型参数推断阶段。
 *
 * 对泛型候选执行类型推断：
 * 1. 若调用方提供了显式类型参数 → 直接构建 Map 替换器（沿用 Phase 3 路径）
 * 2. 若需要推断 → 为每个类型参数注册类型变量 → 收集约束 → 固定 → 构建替换器
 *
 * 该阶段在 CheckArguments 之前执行，确保 CheckArguments 使用推断后的替换器。
 *
 * 对齐 K2 CheckCallArguments + inference 部分。
 */
object CfirInferTypeArguments : CfirResolutionStage() {

    override fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    ) {
        val typeParameters = extractTypeParameters(candidate) ?: return
        if (typeParameters.isEmpty()) return // 非泛型函数，跳过

        val explicitTypeArgs = candidate.callInfo.typeArguments

        if (explicitTypeArgs.isNotEmpty()) {
            // 显式类型参数：直接构建 Map 替换器
            buildExplicitSubstitutor(candidate, typeParameters, explicitTypeArgs)
            return
        }

        // 需要推断：创建约束系统并收集约束
        inferTypeArguments(candidate, typeParameters, context)
    }

    /** 从候选符号中提取类型参数列表 */
    private fun extractTypeParameters(candidate: CfirCandidate): List<CfirTypeParameter>? {
        if (!candidate.symbol.isBound) return null
        return when (val decl = candidate.symbol.cfir) {
            is CfirFunction -> decl.typeParameters
            is CfirConstructor -> decl.typeParameters
            else -> null
        }
    }

    /**
     * 显式类型参数路径：从类型参数和显式类型实参构建 Map 替换器。
     */
    private fun buildExplicitSubstitutor(
        candidate: CfirCandidate,
        typeParameters: List<CfirTypeParameter>,
        explicitTypeArgs: List<org.cangnova.cangjie.cfir.types.CfirTypeRef>,
    ) {
        if (explicitTypeArgs.size != typeParameters.size) return

        val substitution = mutableMapOf<String, ConeCangjieType>()
        for (i in typeParameters.indices) {
            val paramName = typeParameters[i].name.asString()
            val argTypeRef = explicitTypeArgs[i]
            val argType = (argTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            substitution[paramName] = argType
        }
        candidate.substitutor = CfirTypeSubstitutorByMap(substitution)
    }

    /**
     * 推断路径：创建约束系统 → 注册类型变量 → 收集参数约束 → 固定 → 构建替换器。
     */
    private fun inferTypeArguments(
        candidate: CfirCandidate,
        typeParameters: List<CfirTypeParameter>,
        context: CfirResolutionContext,
    ) {
        val inferenceComponents = context.inferenceComponents ?: return
        val constraintSystem = inferenceComponents.createConstraintSystem()

        // 1. 为每个类型参数注册类型变量
        val typeVariableMap = mutableMapOf<String, CfirTypeVariable>()
        for (typeParam in typeParameters) {
            val paramName = typeParam.name.asString()
            val lookupTag = ConeTypeParameterLookupTag(paramName)
            val symbol = typeParam.symbol as? CfirTypeParameterSymbol ?: continue
            val variable = CfirTypeVariable(
                typeParameter = symbol,
                freshTypeId = constraintSystem.nextFreshTypeId(),
                lookupTag = lookupTag,
            )
            constraintSystem.registerTypeVariable(variable)
            typeVariableMap[paramName] = variable

            // 注册声明的上界约束
            for (bound in typeParam.bounds) {
                val boundType = (bound as? CfirResolvedTypeRef)?.coneType ?: continue
                variable.upperBounds.add(boundType)
            }
        }

        // 保存约束系统到候选
        candidate.constraintSystem = constraintSystem

        // 2. 收集参数约束：对每对 (argType, paramType) 添加 argType <: substitute(paramType)
        val valueParameters = extractValueParameters(candidate)
        val arguments = candidate.callInfo.arguments
        val mapping = candidate.argumentMapping

        for ((argIndex, paramIndex) in mapping) {
            val argument = arguments.getOrNull(argIndex) ?: continue
            val parameter = valueParameters?.getOrNull(paramIndex) ?: continue

            val argType = argument.coneTypeOrNull ?: continue
            val paramType = extractParameterType(parameter) ?: continue

            if (argType is ConeErrorType || paramType is ConeErrorType) continue

            constraintSystem.addSubtypeConstraint(
                argType,
                paramType,
                CfirConstraintPosition.ArgumentPosition(argIndex),
            )
        }

        // 3. 固定所有类型变量
        constraintSystem.fixAllVariables()

        // 4. 构建替换器
        candidate.substitutor = constraintSystem.buildResultingSubstitutor()
    }

    private fun extractValueParameters(
        candidate: CfirCandidate,
    ): List<org.cangnova.cangjie.cfir.declarations.CfirValueParameter>? {
        if (!candidate.symbol.isBound) return null
        return when (val decl = candidate.symbol.cfir) {
            is CfirFunction -> decl.valueParameters
            is CfirConstructor -> decl.valueParameters
            else -> null
        }
    }

    private fun extractParameterType(
        parameter: org.cangnova.cangjie.cfir.declarations.CfirValueParameter,
    ): ConeCangjieType? {
        return (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType
    }
}
