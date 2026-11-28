package org.cangnova.cangjie.resolve.calls.mpp

/** 平台中立的 CJMP 声明与有序候选兼容性计算。 */
object AbstractCjmpMatcher {
    /** 元数相同才建立本声明的位置映射；不同时保留父映射，不推断共同前缀映射。 */
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

    /**
     * 候选发现已保证名称、角色、依赖与已配对 owner；这里检查声明种类、名义泛型性和可调用声明的精确泛型数量。
     * 不读取值参数、返回类型、名字或默认值，不能产生参数程序的失败结果。
     */
    fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Type> matchEligibility(
        specific: D,
        common: D,
        context: CjmpMatchingContext<D, T, Type>,
    ): CjmpMatchResult {
        val specificClassKind = context.classLikeKind(specific)
        val commonClassKind = context.classLikeKind(common)
        val nominal = specificClassKind != null || commonClassKind != null
        if (nominal) {
            if (specificClassKind == null || specificClassKind != commonClassKind) {
                return CjmpMatchResult.Mismatched(CjmpMismatchKind.CLASS_KIND)
            }
        } else if (context.isExtend(specific) || context.isExtend(common)) {
            if (!context.isExtend(specific) || !context.isExtend(common)) {
                return CjmpMatchResult.Mismatched(CjmpMismatchKind.CLASS_KIND)
            }
        } else {
            val specificCallableKind = context.callableKind(specific)
            val commonCallableKind = context.callableKind(common)
            if (specificCallableKind == null || commonCallableKind == null || specificCallableKind != commonCallableKind) {
                return CjmpMatchResult.Mismatched(CjmpMismatchKind.CALLABLE_KIND)
            }
        }
        val specificTypeParameterCount = context.typeParameters(specific).size
        val commonTypeParameterCount = context.typeParameters(common).size
        if (nominal) {
            if ((specificTypeParameterCount == 0) != (commonTypeParameterCount == 0)) {
                return CjmpMatchResult.Mismatched(CjmpMismatchKind.NOMINAL_GENERICNESS)
            }
        } else if (specificTypeParameterCount != commonTypeParameterCount) {
            return CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE_PARAMETER_COUNT)
        }
        return CjmpMatchResult.Matched
    }

    /** 为有序计划一次性冻结资格、签名和绑定所需事实；参数程序仅由求值器执行。 */
    fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Type> prepareCandidate(
        specific: D,
        common: D,
        context: CjmpMatchingContext<D, T, Type>,
        parentTypeParameterMapping: Map<T, T> = emptyMap(),
    ): CjmpPreparedCandidate<T> {
        val eligibility = matchEligibility(specific, common, context)
        if (eligibility != CjmpMatchResult.Matched) {
            return CjmpPreparedCandidate(eligibility, null, emptyMap(), false)
        }
        val mapping = typeParameterMapping(specific, common, parentTypeParameterMapping, context).toMap()
        val signature = matchEligibleSignature(specific, common, context, mapping)
        return CjmpPreparedCandidate(
            eligibility, signature, mapping,
            signature == CjmpMatchResult.Matched && context.needToReportMissingBody(common, specific),
        )
    }

    /** 独立签名查询同样经过结构资格，且不执行参数名字/default 程序。 */
    fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Type> matchSignature(
        specific: D,
        common: D,
        context: CjmpMatchingContext<D, T, Type>,
        parentTypeParameterMapping: Map<T, T> = emptyMap(),
    ): CjmpMatchResult {
        val eligibility = matchEligibility(specific, common, context)
        if (eligibility != CjmpMatchResult.Matched) return eligibility
        val mapping = typeParameterMapping(specific, common, parentTypeParameterMapping, context)
        return matchEligibleSignature(specific, common, context, mapping)
    }

    private fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Type> matchEligibleSignature(
        specific: D,
        common: D,
        context: CjmpMatchingContext<D, T, Type>,
        typeParameterMapping: Map<T, T>,
    ): CjmpMatchResult {
        if (context.classLikeKind(specific) != null) return CjmpMatchResult.Matched
        if (context.isExtend(specific)) {
            return when (context.haveSameExtendKey(specific, common)) {
                CjmpTypeCompatibility.COMPATIBLE -> CjmpMatchResult.Matched
                CjmpTypeCompatibility.INCOMPATIBLE -> CjmpMatchResult.Mismatched(CjmpMismatchKind.CLASS_KIND)
                CjmpTypeCompatibility.UNRESOLVED -> CjmpMatchResult.TypeNotResolved
            }
        }
        if (context.areEnumConstructors(specific, common)) {
            return when (context.areEnumConstructorTypesEquivalent(specific, common, typeParameterMapping)) {
                CjmpTypeCompatibility.COMPATIBLE -> CjmpMatchResult.Matched
                CjmpTypeCompatibility.INCOMPATIBLE -> CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE)
                CjmpTypeCompatibility.UNRESOLVED -> CjmpMatchResult.TypeNotResolved
            }
        }
        if (!context.isFunction(specific) || !context.isFunction(common)) return CjmpMatchResult.Matched
        val specificParameters = context.valueParameters(specific)
        val commonParameters = context.valueParameters(common)
        if (specificParameters.size != commonParameters.size) {
            return CjmpMatchResult.Mismatched(CjmpMismatchKind.PARAMETER_COUNT)
        }

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
            val returnCompatibility = context.areReturnTypesCompatible(
                specific,
                common,
                typeParameterMapping,
            )
            when {
                returnCompatibility == CjmpTypeCompatibility.UNRESOLVED ->
                    return CjmpMatchResult.TypeNotResolved

                returnCompatibility == CjmpTypeCompatibility.INCOMPATIBLE ->
                    return CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE)
            }
        }

        return CjmpMatchResult.Matched
    }

    /** 对单个候选执行完整的只读兼容性检查；签名与参数检查共用各自唯一的判定入口。 */
    fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Type> match(
        specific: D,
        common: D,
        context: CjmpMatchingContext<D, T, Type>,
        parentTypeParameterMapping: Map<T, T> = emptyMap(),
    ): CjmpMatchResult {
        val signature = matchSignature(specific, common, context, parentTypeParameterMapping)
        if (signature != CjmpMatchResult.Matched || !context.isFunction(specific) || !context.isFunction(common)) {
            return signature
        }
        val specificParameters = context.valueParameters(specific)
        val commonParameters = context.valueParameters(common)
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
            if (context.checkParameterDefaultValueConflict() &&
                specificParameter.hasDefaultValue && commonParameter.hasDefaultValue
            ) {
                return CjmpMatchResult.ParameterMismatched(
                    CjmpParameterMismatch(CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES, index),
                )
            }
        }

        return CjmpMatchResult.Matched
    }

    /**
     * 计算全部有序候选的兼容性，不把绑定胜者或即时诊断写回 context。
     *
     * Kotlin 的共享 matcher 同样只判定声明兼容性；仓颉额外保留候选顺序和实现体要求，
     * 供前端按照官方 first-fit、占用优先级和不可撤销的候选诊断规则消费。
     */
    fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Type> matchSpecificAgainstPotentialCommon(
        specific: D,
        commonCandidates: List<D>,
        context: CjmpMatchingContext<D, T, Type>,
        parentTypeParameterMapping: Map<T, T> = emptyMap(),
    ): List<CjmpCandidateMatch<D, T>> = commonCandidates.map { common ->
        val compatibility = match(specific, common, context, parentTypeParameterMapping)
        CjmpCandidateMatch(
            common = common,
            compatibility = compatibility,
            typeParameterMapping = typeParameterMapping(specific, common, parentTypeParameterMapping, context).toMap(),
            requiresImplementation = compatibility == CjmpMatchResult.Matched &&
                    context.needToReportMissingBody(common, specific),
        )
    }
}
