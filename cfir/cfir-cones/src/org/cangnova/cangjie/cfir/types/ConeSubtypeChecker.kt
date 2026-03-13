package org.cangnova.cangjie.cfir.types

/**
 * 仓颉子类型判定器。
 *
 * 基于 BFS 超类型图遍历实现子类型检查，参考 K2 AbstractTypeChecker。
 * 通过 [ConeTypeContext] 获取类型的超类型信息，与符号系统解耦。
 *
 * 子类型规则按优先级：
 * 1. 同类型 — T <: T（reflexive）
 * 2. Nothing — Nothing <: T（bottom type）
 * 3. Any — T <: Any（top type）
 * 4. Error — ErrorType <: T 且 T <: ErrorType（静默传播）
 * 5. IdealType — IdealInt <: 任何整数类型, IdealFloat <: 任何浮点类型
 * 6. 原始类型 — 仅同种 kind
 * 7. 类/接口 — BFS 超类型图 + 类型参数不变
 * 8. 结构体 — 同 classId + 类型参数相等
 * 9. 枚举 — 同 classId + 类型参数相等
 * 10. 函数类型 — 参数逆变 + 返回值协变
 * 11. 元组 — 同长度 + 元素逐一子类型
 * 12. 数组 — 不变（同元素类型 + 同维度/大小）
 * 13. 类型参数 — T <: UpperBound（检查所有上界）
 * 14. 交叉类型 — T <: (A & B) 当 T <: A 且 T <: B
 * 15. 联合类型 — (A | B) <: T 当 A <: T 且 B <: T
 * 16. 柔性类型 — Flexible(L, U) <: T 当 L <: T
 */
class ConeSubtypeChecker(private val context: ConeTypeContext) {

    /**
     * 判断 [subType] 是否为 [superType] 的子类型。
     */
    fun isSubtypeOf(subType: ConeCangjieType, superType: ConeCangjieType): Boolean {
        // 规则 1: 同类型（reflexive）
        if (subType == superType) return true

        // 规则 4: Error 类型静默传播
        if (subType.isError || superType.isError) return true

        // 规则 2: Nothing 是所有类型的子类型
        if (subType.isNothing) return true

        // 规则 3: 所有类型都是 Any 的子类型
        if (superType is ConeAnyType) return true
        if (subType is ConeAnyType) return false

        // 规则 16: 柔性类型
        if (subType is ConeFlexibleType) {
            return isSubtypeOf(subType.lowerBound, superType)
        }
        if (superType is ConeFlexibleType) {
            return isSubtypeOf(subType, superType.upperBound)
        }

        // 规则 15: 超类型为联合类型 — T <: (A | B) 当 T <: A 或 T <: B
        if (superType is ConeUnionType) {
            return superType.unionTypes.any { isSubtypeOf(subType, it) }
        }

        // 规则 15: 子类型为联合类型 — (A | B) <: T 当 A <: T 且 B <: T
        if (subType is ConeUnionType) {
            return subType.unionTypes.all { isSubtypeOf(it, superType) }
        }

        // 规则 14: 超类型为交叉类型 — T <: (A & B) 当 T <: A 且 T <: B
        if (superType is ConeIntersectionType) {
            return superType.intersectedTypes.all { isSubtypeOf(subType, it) }
        }

        // 规则 14: 子类型为交叉类型 — (A & B) <: T 当 A <: T 或 B <: T
        if (subType is ConeIntersectionType) {
            return subType.intersectedTypes.any { isSubtypeOf(it, superType) }
        }

        // 规则 13: 类型参数 — T <: UpperBound
        if (subType is ConeTypeParameterType) {
            if (subType.upperBounds.isNotEmpty()) {
                return subType.upperBounds.any { isSubtypeOf(it, superType) }
            }
            // 无上界约束时，隐式上界为 Any
            return superType is ConeAnyType
        }

        // 规则 5: IdealType
        if (subType is ConePrimitiveType && subType.kind.isIdeal) {
            return isIdealSubtypeOf(subType, superType)
        }

        // 规则 6: 原始类型 — 仅同种 kind
        if (subType is ConePrimitiveType && superType is ConePrimitiveType) {
            return subType.kind == superType.kind
        }

        // 规则 10: 函数类型 — 参数逆变 + 返回值协变
        if (subType is ConeFuncType && superType is ConeFuncType) {
            return isFuncSubtypeOf(subType, superType)
        }

        // 规则 11: 元组 — 同长度 + 元素逐一子类型
        if (subType is ConeTupleType && superType is ConeTupleType) {
            return isTupleSubtypeOf(subType, superType)
        }

        // 规则 12: 数组 — 不变
        if (subType is ConeArrayType && superType is ConeArrayType) {
            return subType.elementType == superType.elementType && subType.dims == superType.dims
        }
        if (subType is ConeVArrayType && superType is ConeVArrayType) {
            return subType.elementType == superType.elementType && subType.size == superType.size
        }

        // 规则 8: 结构体 — 同 classId + 类型参数相等
        if (subType is ConeStructType && superType is ConeStructType) {
            return subType.classId == superType.classId && subType.typeArguments == superType.typeArguments
        }

        // 规则 9: 枚举 — 同 classId + 类型参数相等
        if (subType is ConeEnumType && superType is ConeEnumType) {
            return subType.classId == superType.classId && subType.typeArguments == superType.typeArguments
        }

        // 规则 7: 类/接口 — BFS 超类型图遍历
        if (subType is ConeClassLikeType && superType is ConeClassLikeType) {
            return isClassSubtypeOf(subType, superType)
        }

        // 不同类型族之间的 BFS 超类型查找（例如 class <: interface）
        if (superType is ConeClassLikeType) {
            return isSubtypeBySupertypeTraversal(subType, superType)
        }

        return false
    }

    /** IdealInt <: 任何整数类型, IdealFloat <: 任何浮点类型 */
    private fun isIdealSubtypeOf(subType: ConePrimitiveType, superType: ConeCangjieType): Boolean {
        if (superType !is ConePrimitiveType) return false
        return when (subType.kind) {
            PrimitiveTypeKind.IDEAL_INT -> superType.kind.isInteger
            PrimitiveTypeKind.IDEAL_FLOAT -> superType.kind.isFloat
            else -> false
        }
    }

    /** 函数类型子类型：参数逆变 + 返回值协变 */
    private fun isFuncSubtypeOf(sub: ConeFuncType, sup: ConeFuncType): Boolean {
        if (sub.parameterTypes.size != sup.parameterTypes.size) return false
        // 参数逆变: sup.params <: sub.params
        for (i in sub.parameterTypes.indices) {
            if (!isSubtypeOf(sup.parameterTypes[i], sub.parameterTypes[i])) return false
        }
        // 返回值协变: sub.return <: sup.return
        return isSubtypeOf(sub.returnType, sup.returnType)
    }

    /** 元组子类型：同长度 + 元素逐一子类型 */
    private fun isTupleSubtypeOf(sub: ConeTupleType, sup: ConeTupleType): Boolean {
        if (sub.elementTypes.size != sup.elementTypes.size) return false
        return sub.elementTypes.zip(sup.elementTypes).all { (s, t) -> isSubtypeOf(s, t) }
    }

    /** 类/接口子类型：同构造器 + 类型参数相等，或 BFS 超类型图遍历 */
    private fun isClassSubtypeOf(sub: ConeClassLikeType, sup: ConeClassLikeType): Boolean {
        // 同一类型构造器，检查类型参数（不变泛型）
        if (context.isSameTypeConstructor(sub, sup)) {
            return sub.typeArguments == sup.typeArguments
        }
        return isSubtypeBySupertypeTraversal(sub, sup)
    }

    /** BFS 超类型图遍历：检查 superType 是否在 subType 的超类型传递闭包中 */
    private fun isSubtypeBySupertypeTraversal(
        subType: ConeCangjieType,
        superType: ConeCangjieType,
    ): Boolean {
        val visited = mutableSetOf<ConeCangjieType>()
        val queue = ArrayDeque<ConeCangjieType>()
        queue.addAll(context.supertypes(subType))

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current)) continue

            if (current == superType) return true

            if (context.isSameTypeConstructor(current, superType)) {
                // 同一构造器，检查类型参数
                if (current.typeArguments == superType.typeArguments) return true
            }

            queue.addAll(context.supertypes(current))
        }
        return false
    }
}
