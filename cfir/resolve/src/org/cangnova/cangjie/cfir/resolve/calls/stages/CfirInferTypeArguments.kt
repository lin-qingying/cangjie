package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccess
import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.InferenceConstraintError
import org.cangnova.cangjie.cfir.resolve.inference.CfirConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.CfirTypeVariable
import org.cangnova.cangjie.cfir.resolve.inference.collectTypeVariableNames
import org.cangnova.cangjie.cfir.resolve.inference.inferenceLogger
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef

/**
 * 泛型类型参数推断阶段。
 * 对泛型候选的处理分为两条路径：
 * 1. 如果调用方显式给出了类型实参，直接构建 map 形式的替换器
 * 2. 否则进入推断流程：注册类型变量 -> 收集约束 -> 固定变量 -> 构建替换器
 * 该阶段在 `CheckArguments` 之前执行，确保后续阶段使用的是推断后的替换器。
 * 对齐 K2 调用检查中的 inference 部分。
 */
object CfirInferTypeArguments : CfirResolutionStage() {

    override fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    ) {
        val typeParameters = extractTypeParameters(candidate) ?: return
        if (typeParameters.isEmpty()) return // 非泛型函数，直接跳过

        val explicitTypeArgs = candidate.callInfo.typeArguments

        if (explicitTypeArgs.isNotEmpty()) {
            // 显式类型实参：直接构建 map 替换器
            buildExplicitSubstitutor(candidate, typeParameters, explicitTypeArgs)
            return
        }

        if (bindEnumTypeParametersFromExpectedType(candidate, typeParameters, context.expectedType)) {
            return
        }

        // 需要推断：创建约束系统并收集约束
        inferTypeArguments(candidate, typeParameters, sink, context)
    }

    /** 从候选符号中提取类型参数列表。 */
    private fun extractTypeParameters(candidate: CfirCandidate): List<CfirTypeParameter>? {
        if (!candidate.symbol.isBound) return null
        return when (val decl = candidate.symbol.cfir) {
            is CfirFunction -> decl.typeParameters
            is CfirConstructor -> decl.typeParameters
            is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor -> decl.typeParameters
            else -> null
        }
    }

    /**
     * 显式类型实参路径：从类型参数与显式类型实参直接构建 map 替换器。
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

    private fun bindEnumTypeParametersFromExpectedType(
        candidate: CfirCandidate,
        typeParameters: List<CfirTypeParameter>,
        expectedType: ConeCangjieType?,
    ): Boolean {
        if (!candidate.symbol.isBound) return false
        val enumDecl = candidate.symbol.cfir as? org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor ?: return false
        val enumSymbol = enumDecl.symbol as? org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol ?: return false
        val ownerClassId = candidate.callInfo.session.symbolProvider.getEnumConstructorOwnerClassId(enumSymbol)
            ?: candidate.callInfo.session.cfirProvider.getEnumConstructorOwnerClassId(enumSymbol)
            ?: return false

        val expectedArgs = when (expectedType) {
            is ConeEnumType -> if (expectedType.classId == ownerClassId) expectedType.typeArguments else return false
            is ConeClassLikeType -> if (expectedType.classId == ownerClassId) expectedType.typeArguments else return false
            else -> return false
        }
        if (expectedArgs.size != typeParameters.size) return false

        val substitution = buildMap {
            for (i in typeParameters.indices) {
                put(typeParameters[i].name.asString(), expectedArgs[i])
            }
        }
        candidate.substitutor = CfirTypeSubstitutorByMap(substitution)
        return true
    }

    /**
     * 推断路径：创建约束系统 -> 注册类型变量 -> 收集参数约束 -> 固定变量 -> 构建替换器。
     */
    private fun inferTypeArguments(
        candidate: CfirCandidate,
        typeParameters: List<CfirTypeParameter>,
        sink: CfirCheckerSink,
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

            // 注册声明上的上界约束：T <: UpperBound
            val variableType = ConeTypeParameterType(lookupTag)
            for (bound in typeParam.bounds) {
                val boundType = (bound as? CfirResolvedTypeRef)?.coneType ?: continue
                constraintSystem.addSubtypeConstraint(
                    variableType,
                    boundType,
                    CfirConstraintPosition.UpperBound,
                )
            }
        }

        // 保存约束系统到候选上
        candidate.constraintSystem = constraintSystem
        context.session.inferenceLogger?.apply {
            logCandidate(candidate)
            logStage("InferTypeArguments", constraintSystem)
        }

        // 2. 收集参数约束：对每对 (argType, paramType) 添加 argType <: substitute(paramType)
        val parameterTypes = extractParameterTypes(candidate)
        val arguments = candidate.callInfo.arguments
        val mapping = candidate.argumentMapping

        for ((argIndex, paramIndex) in mapping) {
            val argument = arguments.getOrNull(argIndex) ?: continue
            val paramType = parameterTypes?.getOrNull(paramIndex) ?: continue
            val argType = argument.coneTypeOrNull ?: continue

            if (argType is ConeErrorType || paramType is ConeErrorType) continue

            constraintSystem.addSubtypeConstraint(
                argType,
                paramType,
                CfirConstraintPosition.ArgumentPosition(argIndex),
            )
        }

        // 2.5. 返回值约束：当调用处有期望类型时，添加 returnType <: expectedType
        val expectedReturnType = context.expectedType
        if (expectedReturnType != null) {
            val returnType = extractReturnType(candidate)
            if (returnType != null &&
                returnType !is ConeErrorType &&
                shouldAddExpectedTypeConstraint(returnType, typeVariableMap)
            ) {
                constraintSystem.addSubtypeConstraint(
                    returnType,
                    expectedReturnType,
                    CfirConstraintPosition.ReturnType,
                )
            }
        }

        // 3. 固定所有类型变量
        constraintSystem.fixAllVariables()
        if (constraintSystem.hasErrors) {
            constraintSystem.errors.forEach { message ->
                sink.reportDiagnostic(InferenceConstraintError(message))
            }
        }

        // 4. 构建替换器
        candidate.substitutor = constraintSystem.buildResultingSubstitutor()
        applyInferredTypeArgumentsToCallSite(candidate, typeParameters, typeVariableMap)
    }

    private fun extractParameterTypes(candidate: CfirCandidate): List<ConeCangjieType>? {
        if (!candidate.symbol.isBound) return null
        return when (val decl = candidate.symbol.cfir) {
            is CfirFunction -> decl.valueParameters.mapNotNull {
                (it.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            }
            is CfirConstructor -> decl.valueParameters.mapNotNull {
                (it.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            }
            is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor -> {
                val payloadType = (decl.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return emptyList()
                when (payloadType) {
                    is ConeTupleType -> payloadType.elementTypes
                    is ConeErrorType -> emptyList()
                    else -> listOf(payloadType)
                }
            }
            else -> null
        }
    }

    /** 从候选符号中提取返回类型。 */
    private fun extractReturnType(candidate: CfirCandidate): ConeCangjieType? {
        if (!candidate.symbol.isBound) return null
        val typeRef = when (val decl = candidate.symbol.cfir) {
            is CfirFunction -> decl.returnTypeRef
            is CfirConstructor -> decl.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor -> {
                val symbol = decl.symbol as? org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
                    ?: return null
                val classId = candidate.callInfo.session.symbolProvider.getEnumConstructorOwnerClassId(symbol)
                    ?: candidate.callInfo.session.cfirProvider.getEnumConstructorOwnerClassId(symbol)
                    ?: return null
                val typeArguments = decl.typeParameters.map {
                    ConeTypeParameterType(ConeTypeParameterLookupTag(it.name.asString()))
                }
                return ConeEnumType(ConeClassLookupTagImpl(classId), typeArguments)
            }
            else -> return null
        }
        return (typeRef as? CfirResolvedTypeRef)?.coneType
    }

    /**
     * 与仓颉官方行为对齐：
     * 当返回类型中的类型变量已由实参提供有效下界信息时，不再把 expected type 反向注入约束系统，
     * 以避免把调用点类型强推回参数检查阶段（导致 ARGUMENT_TYPE_MISMATCH 抢占诊断）。
     */
    private fun shouldAddExpectedTypeConstraint(
        returnType: ConeCangjieType,
        typeVariableMap: Map<String, CfirTypeVariable>,
    ): Boolean {
        val namesInReturnType = mutableSetOf<String>()
        returnType.collectTypeVariableNames(namesInReturnType)
        if (namesInReturnType.isEmpty()) return true

        return namesInReturnType.none { variableName ->
            val variable = typeVariableMap[variableName] ?: return@none false
            variable.lowerBounds.isNotEmpty()
        }
    }

    private fun applyInferredTypeArgumentsToCallSite(
        candidate: CfirCandidate,
        typeParameters: List<CfirTypeParameter>,
        typeVariableMap: Map<String, CfirTypeVariable>,
    ) {
        if (candidate.callInfo.typeArguments.isNotEmpty()) return

        val callSite = candidate.callInfo.callSite
        val callSiteSource = callSite.source

        val inferredTypeArguments = typeParameters.map { typeParameter ->
            val parameterName = typeParameter.name.asString()
            val parameterSymbol = typeParameter.symbol as? CfirTypeParameterSymbol
            check(parameterSymbol != null) {
                "Expected CfirTypeParameterSymbol for type parameter '$parameterName', got: ${typeParameter.symbol::class.simpleName}"
            }
            val variable = typeVariableMap[parameterName]
            val inferredType = variable?.fixedType
                ?: candidate.substitutor.substituteOrSelf(
                    ConeTypeParameterType(ConeTypeParameterLookupTag(parameterName)),
                )

            val finalType = when {
                variable?.fixedType == null ->
                    ConeErrorType(ConeCannotInferTypeParameterType(parameterSymbol))

                inferredType is ConeTypeParameterType &&
                        inferredType.lookupTag.name == parameterName ->
                    ConeErrorType(ConeCannotInferTypeParameterType(parameterSymbol))

                else -> inferredType
            }

            buildResolvedTypeRef {
                source = callSiteSource
                coneType = finalType
                delegatedTypeRef = null
            }
        }

        when (callSite) {
            is CfirFunctionCall -> callSite.replaceTypeArguments(inferredTypeArguments)
            is CfirQualifiedAccess -> callSite.replaceTypeArguments(inferredTypeArguments)
        }
    }

}
