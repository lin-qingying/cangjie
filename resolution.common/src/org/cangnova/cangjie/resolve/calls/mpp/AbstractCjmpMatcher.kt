package org.cangnova.cangjie.resolve.calls.mpp

/** 平台中立的 CJMP 候选兼容性与 first-fit 配对算法。 */
object AbstractCjmpMatcher {
    fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Type> typeParameterMapping(
        specific: D,
        common: D,
        parentMapping: Map<T, T> = emptyMap(),
        context: CjmpMatchingContext<D, T, Type>,
    ): Map<T, T> {
        val specificParameters = context.typeParameters(specific)
        val commonParameters = context.typeParameters(common)
        if (specificParameters.size != commonParameters.size) return parentMapping
        return parentMapping + commonParameters.zip(specificParameters)
    }

    fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Type> match(
        specific: D,
        common: D,
        context: CjmpMatchingContext<D, T, Type>,
        parentTypeParameterMapping: Map<T, T> = emptyMap(),
    ): CjmpMatchResult {
        val specificClassKind = context.classLikeKind(specific)
        val commonClassKind = context.classLikeKind(common)
        if (specificClassKind != null || commonClassKind != null) {
            if (specificClassKind == null || commonClassKind == null || specificClassKind != commonClassKind) {
                return CjmpMatchResult.Mismatched(CjmpMismatchKind.CLASS_KIND)
            }
            if (context.typeParameters(specific).size != context.typeParameters(common).size) {
                return CjmpMatchResult.Mismatched(CjmpMismatchKind.NOMINAL_TYPE_PARAMETER_COUNT)
            }
            return CjmpMatchResult.Matched
        }

        val specificIsExtend = context.isExtend(specific)
        val commonIsExtend = context.isExtend(common)
        if (specificIsExtend || commonIsExtend) {
            if (!specificIsExtend || !commonIsExtend) {
                return CjmpMatchResult.Mismatched(CjmpMismatchKind.CLASS_KIND)
            }
            return when (context.haveSameExtendKey(specific, common)) {
                CjmpTypeCompatibility.COMPATIBLE -> CjmpMatchResult.Matched
                CjmpTypeCompatibility.INCOMPATIBLE -> CjmpMatchResult.Mismatched(CjmpMismatchKind.CLASS_KIND)
                CjmpTypeCompatibility.UNRESOLVED -> CjmpMatchResult.TypeNotResolved
            }
        }

        if (context.callableKind(specific) != context.callableKind(common)) {
            return CjmpMatchResult.Mismatched(CjmpMismatchKind.CALLABLE_KIND)
        }
        if (!context.isFunction(specific) || !context.isFunction(common)) return CjmpMatchResult.Matched

        val specificParameters = context.valueParameters(specific)
        val commonParameters = context.valueParameters(common)
        if (context.typeParameters(specific).size != context.typeParameters(common).size) {
            return CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE_PARAMETER_COUNT)
        }
        if (specificParameters.size != commonParameters.size) {
            return CjmpMatchResult.Mismatched(CjmpMismatchKind.PARAMETER_COUNT)
        }

        val typeParameterMapping = typeParameterMapping(specific, common, parentTypeParameterMapping, context)
        for (index in specificParameters.indices) {
            when (context.areTypesEquivalent(
                    specificParameters[index].type,
                    commonParameters[index].type,
                    typeParameterMapping,
                )
            ) {
                CjmpTypeCompatibility.COMPATIBLE -> Unit
                CjmpTypeCompatibility.INCOMPATIBLE ->
                    return CjmpMatchResult.Mismatched(CjmpMismatchKind.PARAMETER_TYPES)

                CjmpTypeCompatibility.UNRESOLVED -> return CjmpMatchResult.TypeNotResolved
            }
        }

        if (!context.isConstructor(specific)) {
            val returnCompatibility = context.isSubtypeOf(
                context.returnType(specific),
                context.returnType(common),
                typeParameterMapping,
            )
            when {
                returnCompatibility == CjmpTypeCompatibility.UNRESOLVED ->
                    return CjmpMatchResult.TypeNotResolved

                returnCompatibility == CjmpTypeCompatibility.INCOMPATIBLE &&
                        !context.hasInferredReturnType(specific) ->
                    return CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE)
            }
        }

        for (index in specificParameters.indices) {
            val specificParameter = specificParameters[index]
            val commonParameter = commonParameters[index]
            if (specificParameter.isNamed != commonParameter.isNamed ||
                (specificParameter.isNamed && specificParameter.name != commonParameter.name)
            ) {
                return CjmpMatchResult.ParameterMismatched(
                    CjmpParameterMismatch(CjmpMismatchKind.PARAMETER_NAMES, index),
                )
            }
            if (specificParameter.hasDefaultValue && commonParameter.hasDefaultValue) {
                return CjmpMatchResult.ParameterMismatched(
                    CjmpParameterMismatch(CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES, index),
                )
            }
        }

        return CjmpMatchResult.Matched
    }

    /** 逐 common 候选执行匹配、失配记录与存储回调，并绑定首个成功候选。 */
    fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Type> matchSpecificAgainstPotentialCommon(
        specific: D,
        commonCandidates: List<D>,
        context: CjmpMatchingContext<D, T, Type>,
        parentTypeParameterMapping: Map<T, T> = emptyMap(),
    ): D? {
        for (common in commonCandidates) {
            when (val result = match(specific, common, context, parentTypeParameterMapping)) {
                CjmpMatchResult.Matched -> {
                    if (context.needToReportMissingBody(common, specific)) {
                        context.recordMismatch(specific, CjmpMismatchKind.MISSING_BODY, common)
                        continue
                    }
                    val mapping = typeParameterMapping(specific, common, parentTypeParameterMapping, context)
                    if (context.trySetSpecificImplementation(specific, common, mapping)) return common
                }

                is CjmpMatchResult.Mismatched -> context.recordMismatch(specific, result.kind, common)
                is CjmpMatchResult.ParameterMismatched ->
                    context.recordParameterMismatch(specific, result.mismatch, common)

                CjmpMatchResult.TypeNotResolved ->
                    context.recordMismatch(specific, CjmpMismatchKind.TYPE_NOT_RESOLVED, common)
            }
        }
        return null
    }
}
