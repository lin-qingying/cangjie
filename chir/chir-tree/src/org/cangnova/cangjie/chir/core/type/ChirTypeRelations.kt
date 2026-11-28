package org.cangnova.cangjie.chir.core.type

/**
 * CHIR 类型身份、约束和替换操作。
 *
 * 具名自定义类型的字段、父类型和枚举 case 描述声明布局，不参与一个实例化类型的名义身份；
 * 类型身份由稳定声明 ID、类型种类和实际类型参数决定。
 */
object ChirTypeRelations {
    /**
     * 由聚合布局路径解析出的字段类型事实。
     */
    data class FieldProjection(
        val resultType: ChirTypeRef,
        val isEnumSelector: Boolean,
    )

    /**
     * 判断两个类型引用是否指向相同的 CHIR 语义类型。
     */
    fun areEquivalent(first: ChirTypeRef, second: ChirTypeRef): Boolean =
        areEquivalent(first, second, emptyMap())

    /**
     * 在 matcher 已批准的类型参数映射下比较两个 CHIR 类型。
     *
     * 映射方向为 common 泛型身份到 specific 泛型身份；未列出的泛型仍要求语义身份完全相同。
     */
    fun areEquivalent(
        first: ChirTypeRef,
        second: ChirTypeRef,
        genericIdentityMappings: Map<String, String>,
    ): Boolean = when {
        first is ChirUnresolvedTypeRef && second is ChirUnresolvedTypeRef -> first.symbol == second.symbol
        first is ChirResolvedTypeRef && second is ChirResolvedTypeRef ->
            areEquivalent(first.type, second.type, genericIdentityMappings)
        else -> false
    }

    /**
     * 按官方 CHIR 检查器的规则比较表达式类型与操作数类型。
     *
     * 官方检查器允许首个类型为 Nothing，也允许任一侧为 ref<std.core/Any>；这条规则用于
     * 保真导入 CHIR 表达式，不替代语言层面的赋值兼容性判断。
     */
    fun isOfficialChirTypeCompatible(first: ChirTypeRef, second: ChirTypeRef): Boolean =
        areEquivalent(first, second) ||
            first.resolvedType() == ChirPrimitiveType.NOTHING ||
            first.isReferenceToCoreAny() ||
            second.isReferenceToCoreAny()

    private fun ChirTypeRef.resolvedType(): ChirType? = (this as? ChirResolvedTypeRef)?.type

    private fun ChirTypeRef.isReferenceToCoreAny(): Boolean {
        val reference = resolvedType() as? ChirRefType ?: return false
        val referencedType = reference.referencedType.resolvedType() as? ChirClassType ?: return false
        return referencedType.declarationId == ChirTypeDeclarationId.CORE_ANY
    }

    /**
     * 判断 [actual] 的值能否赋给 [expected] 类型的存储位置。
     *
     * 除同一语义类型外，Nothing 可赋给任意类型；泛型实参可沿声明上界赋值，类实例可沿直接父类型赋值。
     */
    fun isAssignable(actual: ChirTypeRef, expected: ChirTypeRef): Boolean =
        isAssignable(actual, expected, mutableSetOf())

    private fun isAssignable(
        actual: ChirTypeRef,
        expected: ChirTypeRef,
        visited: MutableSet<Pair<ChirTypeRef, ChirTypeRef>>,
    ): Boolean {
        if (areEquivalent(actual, expected)) return true
        if (!visited.add(actual to expected)) return false
        val actualType = (actual as? ChirResolvedTypeRef)?.type ?: return false
        val expectedType = (expected as? ChirResolvedTypeRef)?.type ?: return false
        return when {
            actualType == ChirPrimitiveType.NOTHING -> true
            actualType is ChirGenericType -> actualType.upperBounds.any { bound ->
                isAssignable(bound, expected, visited)
            }
            actualType is ChirClassType -> actualType.superTypes.any { superType ->
                isAssignable(superType, expected, visited)
            }
            actualType is ChirRefType && expectedType is ChirRefType ->
                actualType.mutable == expectedType.mutable &&
                    isAssignable(actualType.referencedType, expectedType.referencedType, visited)
            else -> false
        }
    }

    /**
     * 将 [formal] 与 [actual] 统一，并把求得的泛型绑定合并进 [substitutions]。
     *
     * 失败时不修改调用方传入的绑定集合。
     */
    fun constrain(
        formal: ChirTypeRef,
        actual: ChirTypeRef,
        substitutions: MutableMap<String, ChirTypeRef>,
    ): Boolean {
        val candidateSubstitutions = substitutions.toMutableMap()
        if (!constrainInto(formal, actual, candidateSubstitutions)) return false
        substitutions.clear()
        substitutions.putAll(candidateSubstitutions)
        return true
    }

    /**
     * 按泛型语义身份替换类型参数，同时保留具名类型的声明布局信息。
     */
    fun substitute(type: ChirTypeRef, substitutions: Map<String, ChirTypeRef>): ChirTypeRef =
        substitute(type, substitutions, emptySet())

    /**
     * 按 nominal 声明的类型形参实例化字段布局。
     */
    fun instantiateMemberTypes(
        formalTypeParameters: List<ChirTypeRef>,
        actualTypeArguments: List<ChirTypeRef>,
        memberTypes: List<ChirTypeRef>,
    ): List<ChirTypeRef> {
        require(formalTypeParameters.size == actualTypeArguments.size) {
            "nominal type declares ${formalTypeParameters.size} type parameters but has " +
                "${actualTypeArguments.size} type arguments"
        }
        val substitutions = linkedMapOf<String, ChirTypeRef>()
        formalTypeParameters.zip(actualTypeArguments).forEachIndexed { index, (formal, actual) ->
            val parameter = (formal as? ChirResolvedTypeRef)?.type as? ChirGenericType
                ?: throw IllegalArgumentException("nominal type parameter #$index is not a generic CHIR type")
            require(substitutions.put(parameter.semanticIdentity, actual) == null) {
                "nominal type declares duplicate generic parameter identity ${parameter.semanticIdentity}"
            }
        }
        return memberTypes.map { memberType -> substitute(memberType, substitutions) }
    }

    /**
     * 读取结果类型对应的实例化结构体字段布局。
     */
    fun instantiatedStructFieldTypes(type: ChirStructType): List<ChirTypeRef> = type.fieldTypes

    /**
     * 读取结果类型中指定枚举构造器的实例化 payload 布局。
     */
    fun instantiatedEnumPayloadTypes(type: ChirEnumType, caseIndex: Int): List<ChirTypeRef>? =
        type.cases.getOrNull(caseIndex)?.payloadTypes

    /**
     * 沿官方 Field 路径解析元组、结构体、类或枚举选择器的结果类型。
     */
    fun projectFieldType(baseType: ChirTypeRef, path: List<ULong>): FieldProjection? {
        if (path.isEmpty()) return null
        var currentTypeRef = baseType
        path.forEachIndexed { pathIndex, fieldIndex ->
            val currentType = (currentTypeRef as? ChirResolvedTypeRef)?.type ?: return null
            if (currentType is ChirEnumType) {
                if (fieldIndex != 0uL || pathIndex != path.lastIndex) return null
                return FieldProjection(ChirResolvedTypeRef(currentType.selectorType), isEnumSelector = true)
            }
            val fieldTypes = when (currentType) {
                is ChirTupleType -> currentType.elementTypes
                is ChirStructType -> instantiatedStructFieldTypes(currentType)
                is ChirClassType -> currentType.fieldTypes
                else -> return null
            }
            val index = fieldIndex.takeIf { it <= Int.MAX_VALUE.toULong() }?.toInt() ?: return null
            currentTypeRef = fieldTypes.getOrNull(index) ?: return null
            if (pathIndex == path.lastIndex) return FieldProjection(currentTypeRef, isEnumSelector = false)
        }
        return null
    }

    private fun areEquivalent(
        first: ChirType,
        second: ChirType,
        genericIdentityMappings: Map<String, String>,
    ): Boolean = when {
        first is ChirPrimitiveType && second is ChirPrimitiveType -> first == second
        first is ChirNamedType && second is ChirNamedType ->
            first.name == second.name && areEquivalentLists(first.typeArguments, second.typeArguments, genericIdentityMappings)

        first is ChirTupleType && second is ChirTupleType ->
            areEquivalentLists(first.elementTypes, second.elementTypes, genericIdentityMappings)
        first is ChirFunctionType && second is ChirFunctionType ->
            first.throws == second.throws &&
                first.isCFunctionType == second.isCFunctionType &&
                first.hasVarArg == second.hasVarArg &&
                areEquivalentLists(first.parameterTypes, second.parameterTypes, genericIdentityMappings) &&
                areEquivalent(first.returnType, second.returnType, genericIdentityMappings) &&
                areNullableEquivalent(first.receiverType, second.receiverType, genericIdentityMappings)

        first is ChirStructType && second is ChirStructType ->
            first.declarationId == second.declarationId &&
                areEquivalentLists(first.typeArguments, second.typeArguments, genericIdentityMappings)

        first is ChirClassType && second is ChirClassType ->
            first.declarationId == second.declarationId &&
                areEquivalentLists(first.typeArguments, second.typeArguments, genericIdentityMappings)

        first is ChirEnumType && second is ChirEnumType ->
            first.declarationId == second.declarationId &&
                areEquivalentLists(first.typeArguments, second.typeArguments, genericIdentityMappings)

        first is ChirRawArrayType && second is ChirRawArrayType ->
            first.dimensions == second.dimensions && areEquivalent(first.elementType, second.elementType, genericIdentityMappings)

        first is ChirVArrayType && second is ChirVArrayType ->
            first.size == second.size && areEquivalent(first.elementType, second.elementType, genericIdentityMappings)

        first is ChirCPointerType && second is ChirCPointerType ->
            areEquivalent(first.pointeeType, second.pointeeType, genericIdentityMappings)
        first is ChirCStringType && second is ChirCStringType -> true
        first is ChirGenericType && second is ChirGenericType ->
            first.semanticIdentity == second.semanticIdentity ||
                genericIdentityMappings[first.semanticIdentity] == second.semanticIdentity
        first is ChirRefType && second is ChirRefType ->
            first.mutable == second.mutable &&
                areEquivalent(first.referencedType, second.referencedType, genericIdentityMappings)

        first is ChirBoxType && second is ChirBoxType ->
            areEquivalent(first.boxedType, second.boxedType, genericIdentityMappings)
        first is ChirThisType && second is ChirThisType -> first.ownerTypeName == second.ownerTypeName
        else -> false
    }

    private fun constrainInto(
        formal: ChirTypeRef,
        actual: ChirTypeRef,
        substitutions: MutableMap<String, ChirTypeRef>,
    ): Boolean {
        val formalType = (formal as? ChirResolvedTypeRef)?.type
        if (formalType is ChirGenericType) {
            val actualType = (actual as? ChirResolvedTypeRef)?.type
            if (actualType is ChirGenericType && actualType.semanticIdentity == formalType.semanticIdentity) {
                return true
            }
            val previous = substitutions[formalType.semanticIdentity]
            if (previous != null) return areEquivalent(previous, actual)
            substitutions[formalType.semanticIdentity] = actual
            return true
        }

        val formalResolved = formal as? ChirResolvedTypeRef ?: return areEquivalent(formal, actual)
        val actualResolved = actual as? ChirResolvedTypeRef ?: return false
        val left = formalResolved.type
        val right = actualResolved.type
        return when {
            left is ChirPrimitiveType && right is ChirPrimitiveType -> left == right
            left is ChirNamedType && right is ChirNamedType ->
                left.name == right.name && constrainLists(left.typeArguments, right.typeArguments, substitutions)

            left is ChirTupleType && right is ChirTupleType -> constrainLists(left.elementTypes, right.elementTypes, substitutions)
            left is ChirFunctionType && right is ChirFunctionType ->
                left.throws == right.throws &&
                    left.isCFunctionType == right.isCFunctionType &&
                    left.hasVarArg == right.hasVarArg &&
                    constrainLists(left.parameterTypes, right.parameterTypes, substitutions) &&
                    constrainInto(left.returnType, right.returnType, substitutions) &&
                    constrainNullable(left.receiverType, right.receiverType, substitutions)

            left is ChirStructType && right is ChirStructType ->
                left.declarationId == right.declarationId && constrainLists(left.typeArguments, right.typeArguments, substitutions)

            left is ChirClassType && right is ChirClassType ->
                left.declarationId == right.declarationId && constrainLists(left.typeArguments, right.typeArguments, substitutions)

            left is ChirEnumType && right is ChirEnumType ->
                left.declarationId == right.declarationId && constrainLists(left.typeArguments, right.typeArguments, substitutions)

            left is ChirRawArrayType && right is ChirRawArrayType ->
                left.dimensions == right.dimensions && constrainInto(left.elementType, right.elementType, substitutions)

            left is ChirVArrayType && right is ChirVArrayType ->
                left.size == right.size && constrainInto(left.elementType, right.elementType, substitutions)

            left is ChirCPointerType && right is ChirCPointerType -> constrainInto(left.pointeeType, right.pointeeType, substitutions)
            left is ChirCStringType && right is ChirCStringType -> true
            left is ChirGenericType && right is ChirGenericType -> left.semanticIdentity == right.semanticIdentity
            left is ChirRefType && right is ChirRefType ->
                left.mutable == right.mutable && constrainInto(left.referencedType, right.referencedType, substitutions)

            left is ChirBoxType && right is ChirBoxType -> constrainInto(left.boxedType, right.boxedType, substitutions)
            left is ChirThisType && right is ChirThisType -> left.ownerTypeName == right.ownerTypeName
            else -> false
        }
    }

    private fun substitute(
        typeRef: ChirTypeRef,
        substitutions: Map<String, ChirTypeRef>,
        resolving: Set<String>,
    ): ChirTypeRef {
        val type = (typeRef as? ChirResolvedTypeRef)?.type ?: return typeRef
        if (type is ChirGenericType) {
            if (type.semanticIdentity in resolving) return typeRef
            val replacement = substitutions[type.semanticIdentity]
            if (replacement != null && replacement != typeRef) {
                return substitute(replacement, substitutions, resolving + type.semanticIdentity)
            }
            if (type.upperBounds.isEmpty()) return typeRef
            val upperBounds = type.upperBounds.map { substitute(it, substitutions, resolving + type.semanticIdentity) }
            return if (upperBounds == type.upperBounds) typeRef else ChirResolvedTypeRef(type.copy(upperBounds = upperBounds))
        }
        val substitutedType = when (type) {
            is ChirPrimitiveType, ChirCStringType -> type
            is ChirNamedType -> type.copy(typeArguments = type.typeArguments.map { substitute(it, substitutions, resolving) })

            is ChirTupleType -> type.copy(elementTypes = type.elementTypes.map { substitute(it, substitutions, resolving) })
            is ChirFunctionType -> type.copy(
                parameterTypes = type.parameterTypes.map { substitute(it, substitutions, resolving) },
                returnType = substitute(type.returnType, substitutions, resolving),
                receiverType = type.receiverType?.let { substitute(it, substitutions, resolving) },
            )

            is ChirStructType -> type.copy(
                fieldTypes = type.fieldTypes.map { substitute(it, substitutions, resolving) },
                typeArguments = type.typeArguments.map { substitute(it, substitutions, resolving) },
            )
            is ChirClassType -> type.copy(
                fieldTypes = type.fieldTypes.map { substitute(it, substitutions, resolving) },
                superTypes = type.superTypes.map { substitute(it, substitutions, resolving) },
                typeArguments = type.typeArguments.map { substitute(it, substitutions, resolving) },
            )
            is ChirEnumType -> type.copy(
                cases = type.cases.map { enumCase ->
                    enumCase.copy(payloadTypes = enumCase.payloadTypes.map { substitute(it, substitutions, resolving) })
                },
                typeArguments = type.typeArguments.map { substitute(it, substitutions, resolving) },
            )
            is ChirRawArrayType -> type.copy(elementType = substitute(type.elementType, substitutions, resolving))
            is ChirVArrayType -> type.copy(elementType = substitute(type.elementType, substitutions, resolving))
            is ChirCPointerType -> type.copy(pointeeType = substitute(type.pointeeType, substitutions, resolving))
            is ChirGenericType -> type
            is ChirRefType -> type.copy(referencedType = substitute(type.referencedType, substitutions, resolving))
            is ChirBoxType -> type.copy(boxedType = substitute(type.boxedType, substitutions, resolving))
            is ChirThisType -> type
        }
        return ChirResolvedTypeRef(substitutedType)
    }

    private fun constrainLists(
        formal: List<ChirTypeRef>,
        actual: List<ChirTypeRef>,
        substitutions: MutableMap<String, ChirTypeRef>,
    ): Boolean = formal.size == actual.size && formal.zip(actual).all { (left, right) -> constrainInto(left, right, substitutions) }

    private fun areEquivalentLists(
        first: List<ChirTypeRef>,
        second: List<ChirTypeRef>,
        genericIdentityMappings: Map<String, String>,
    ): Boolean = first.size == second.size && first.zip(second).all { (left, right) ->
        areEquivalent(left, right, genericIdentityMappings)
    }

    private fun constrainNullable(
        formal: ChirTypeRef?,
        actual: ChirTypeRef?,
        substitutions: MutableMap<String, ChirTypeRef>,
    ): Boolean = when {
        formal == null -> actual == null
        actual == null -> false
        else -> constrainInto(formal, actual, substitutions)
    }

    private fun areNullableEquivalent(
        first: ChirTypeRef?,
        second: ChirTypeRef?,
        genericIdentityMappings: Map<String, String>,
    ): Boolean = when {
        first == null -> second == null
        second == null -> false
        else -> areEquivalent(first, second, genericIdentityMappings)
    }

}
