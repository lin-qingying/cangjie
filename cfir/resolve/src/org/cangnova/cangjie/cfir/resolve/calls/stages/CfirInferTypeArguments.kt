package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.resolve.calls.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.inference.CfirConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.CfirTypeVariable
import org.cangnova.cangjie.cfir.resolve.inference.inferenceLogger
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag

/**
 * 娉涘瀷绫诲瀷鍙傛暟鎺ㄦ柇闃舵銆? *
 * 瀵规硾鍨嬪€欓€夋墽琛岀被鍨嬫帹鏂細
 * 1. 鑻ヨ皟鐢ㄦ柟鎻愪緵浜嗘樉寮忕被鍨嬪弬鏁?鈫?鐩存帴鏋勫缓 Map 鏇挎崲鍣紙娌跨敤 Phase 3 璺緞锛? * 2. 鑻ラ渶瑕佹帹鏂?鈫?涓烘瘡涓被鍨嬪弬鏁版敞鍐岀被鍨嬪彉閲?鈫?鏀堕泦绾︽潫 鈫?鍥哄畾 鈫?鏋勫缓鏇挎崲鍣? *
 * 璇ラ樁娈靛湪 CheckArguments 涔嬪墠鎵ц锛岀‘淇?CheckArguments 浣跨敤鎺ㄦ柇鍚庣殑鏇挎崲鍣ㄣ€? *
 * 瀵归綈 K2 CheckCallArguments + inference 閮ㄥ垎銆? */
object CfirInferTypeArguments : CfirResolutionStage() {

    override fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    ) {
        val typeParameters = extractTypeParameters(candidate) ?: return
        if (typeParameters.isEmpty()) return // 闈炴硾鍨嬪嚱鏁帮紝璺宠繃

        val explicitTypeArgs = candidate.callInfo.typeArguments

        if (explicitTypeArgs.isNotEmpty()) {
            // 鏄惧紡绫诲瀷鍙傛暟锛氱洿鎺ユ瀯寤?Map 鏇挎崲鍣?
            buildExplicitSubstitutor(candidate, typeParameters, explicitTypeArgs)
            return
        }

        // 闇€瑕佹帹鏂細鍒涘缓绾︽潫绯荤粺骞舵敹闆嗙害鏉?
        inferTypeArguments(candidate, typeParameters, context)
    }

    /** 浠庡€欓€夌鍙蜂腑鎻愬彇绫诲瀷鍙傛暟鍒楄〃 */
    private fun extractTypeParameters(candidate: CfirCandidate): List<CfirTypeParameter>? {
        if (!candidate.symbol.isBound) return null
        return when (val decl = candidate.symbol.cfir) {
            is CfirFunction -> decl.typeParameters
            is CfirConstructor -> decl.typeParameters
            else -> null
        }
    }

    /**
     * 鏄惧紡绫诲瀷鍙傛暟璺緞锛氫粠绫诲瀷鍙傛暟鍜屾樉寮忕被鍨嬪疄鍙傛瀯寤?Map 鏇挎崲鍣ㄣ€?     */
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
     * 鎺ㄦ柇璺緞锛氬垱寤虹害鏉熺郴缁?鈫?娉ㄥ唽绫诲瀷鍙橀噺 鈫?鏀堕泦鍙傛暟绾︽潫 鈫?鍥哄畾 鈫?鏋勫缓鏇挎崲鍣ㄣ€?     */
    private fun inferTypeArguments(
        candidate: CfirCandidate,
        typeParameters: List<CfirTypeParameter>,
        context: CfirResolutionContext,
    ) {
        val inferenceComponents = context.inferenceComponents ?: return
        val constraintSystem = inferenceComponents.createConstraintSystem()

        // 1. 涓烘瘡涓被鍨嬪弬鏁版敞鍐岀被鍨嬪彉閲?
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

            // 娉ㄥ唽澹版槑鐨勪笂鐣岀害鏉?
            for (bound in typeParam.bounds) {
                val boundType = (bound as? CfirResolvedTypeRef)?.coneType ?: continue
                variable.upperBounds.add(boundType)
            }
        }

        // 淇濆瓨绾︽潫绯荤粺鍒板€欓€?
        candidate.constraintSystem = constraintSystem
        context.session.inferenceLogger?.apply {
            logCandidate(candidate)
            logStage("InferTypeArguments", constraintSystem)
        }

        // 2. 鏀堕泦鍙傛暟绾︽潫锛氬姣忓 (argType, paramType) 娣诲姞 argType <: substitute(paramType)
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

        // 3. 鍥哄畾鎵€鏈夌被鍨嬪彉閲?
        constraintSystem.fixAllVariables()

        // 4. 鏋勫缓鏇挎崲鍣?
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

