package org.cangnova.cangjie.resolve.checkers

import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.TypeCheckerState
import org.cangnova.cangjie.type.model.*

/**
 * 空交叉类型检查器
 *
 * 仓颉支持交叉类型（A & B），用于多接口约束。
 * 本检查器用于在编译期检测不可能存在公共子类型的交叉，提前报错。
 *
 * 仓颉类型系统特点（影响本检查器的部分）：
 * - 无可空类型，Option<T> 是标准库类型，不参与交叉类型检查
 * - 无星号投影/型变注解，泛型实参就是具体类型
 * - 无弹性类型，所有类型均为刚性类型
 * - 区分值类型（struct）和引用类型（class），两者都可实现接口
 * - class-like 声明只参与顶层公开语义
 */
internal object EmptyIntersectionTypeChecker {

    /**
     * 计算给定类型集合的交叉是否为空
     *
     * 对集合中所有类型两两配对，判断是否存在不可能有公共子类型的组合。
     * - 若发现**确定为空**的组合，立即返回（如两个互不相关的 class）
     * - 若发现**可能为空**的组合（如 final class & interface），记录后返回
     *
     * @param types 参与交叉的类型集合（如 A & B & C 中的 A、B、C）
     * @return 空交叉的详细信息；若交叉不为空则返回 null
     */
    context(c: TypeSystemInferenceExtensionContext)
    fun computeEmptyIntersectionEmptiness(types: Collection<CangJieTypeMarker>): EmptyIntersectionTypeInfo? {
        if (types.isEmpty()) return null

        @Suppress("NAME_SHADOWING")
        val types = types.toList()
        var possibleEmptyIntersectionTypeInfo: EmptyIntersectionTypeInfo? = null

        for (i in types.indices) {
            val firstType = types[i]

            if (!mayCauseEmptyIntersection(firstType)) continue

            val firstSubstitutedType by lazy { firstType.eraseContainingTypeParameters() }

            for (j in i + 1 until types.size) {
                val secondType = types[j]

                if (!mayCauseEmptyIntersection(secondType)) continue

                val secondSubstitutedType = secondType.eraseContainingTypeParameters()

                if (!mayCauseEmptyIntersection(secondSubstitutedType) &&
                    !mayCauseEmptyIntersection(firstSubstitutedType)
                ) continue

                val typeInfo = computeByHavingCommonSubtype(firstSubstitutedType, secondSubstitutedType)
                    ?: continue

                if (typeInfo.kind.isDefinitelyEmpty) {
                    return typeInfo
                }

                possibleEmptyIntersectionTypeInfo = typeInfo
            }
        }

        return possibleEmptyIntersectionTypeInfo
    }

    /**
     * 对两个具体类型判断是否可能存在公共子类型
     *
     * 处理流程：
     * 1. 若任一类型本身是交叉类型（A & B），先将其展开为组成部分
     * 2. 对展开后的所有类型两两比较，按以下规则判断：
     *    - 构造器相同 → 跳过
     *    - 一方是另一方父类型（忽略泛型实参）→ 跳过
     *    - 两者都不是 interface → MULTIPLE_CLASSES（确定为空）
     *    - 有一方是 final class/struct → FINAL_CLASS_AND_INTERFACE（可能为空）
     *
     * 注意：即使两者都是 interface，也可能因泛型实参不同而为空交叉，
     * 例如：interface Inv<K>; interface B : Inv<Int>
     *       Inv<String> & B  →  等价于  Inv<String> & Inv<Int>  →  空交叉
     * 因此不对 interface 做提前过滤。
     */
    context(c: TypeSystemInferenceExtensionContext)
    private fun computeByHavingCommonSubtype(
        first: CangJieTypeMarker,
        second: CangJieTypeMarker
    ): EmptyIntersectionTypeInfo? {

        fun extractIntersectionComponentsIfNeeded(type: CangJieTypeMarker) =
            if (type.typeConstructor() is IntersectionTypeConstructorMarker) {
                type.typeConstructor().supertypes().toList()
            } else listOf(type)

        val expandedTypes = extractIntersectionComponentsIfNeeded(first) +
                extractIntersectionComponentsIfNeeded(second)

        val typeCheckerState by lazy {
            c.newTypeCheckerState(
                errorTypesEqualToAnything = true,
                stubTypesEqualToAnything = true
            )
        }

        var possibleEmptyIntersectionKind: EmptyIntersectionTypeInfo? = null

        for (i in expandedTypes.indices) {
            // 仓颉所有类型均为刚性类型，直接转型，无需取弹性类型下界
            val firstType = expandedTypes[i].asRigidType()!!
            val firstTypeConstructor = firstType.typeConstructor()

            if (!mayCauseEmptyIntersection(firstType)) continue

            for (j in i + 1 until expandedTypes.size) {
                val secondType = expandedTypes[j].asRigidType()!!
                val secondTypeConstructor = secondType.typeConstructor()

                when {
                    !mayCauseEmptyIntersection(secondType) -> {
                    }

                    c.areEqualTypeConstructors(firstTypeConstructor, secondTypeConstructor) -> {
                    }

                    firstType.isSubtypeOfIgnoringArguments(typeCheckerState, secondTypeConstructor) ||
                            secondType.isSubtypeOfIgnoringArguments(typeCheckerState, firstTypeConstructor) -> {
                    }

                    // 两个互不相关的 class 或 struct，不可能有公共子类型
                    !firstTypeConstructor.isInterface() && !secondTypeConstructor.isInterface() -> {
                        return EmptyIntersectionTypeInfo(
                            EmptyIntersectionTypeKind.MULTIPLE_CLASSES,
                            firstType, secondType
                        )
                    }

                    // final class/struct 与 interface，可能无公共子类型
                    firstTypeConstructor.isFinalClassConstructor() ||
                            secondTypeConstructor.isFinalClassConstructor() -> {
                        possibleEmptyIntersectionKind = EmptyIntersectionTypeInfo(
                            EmptyIntersectionTypeKind.FINAL_CLASS_AND_INTERFACE,
                            firstType, secondType
                        )
                    }
                }
            }
        }

        return possibleEmptyIntersectionKind
    }

    /**
     * 判断当前刚性类型是否是给定类型构造器的子类型（忽略泛型实参）
     */
    private fun RigidTypeMarker.isSubtypeOfIgnoringArguments(
        typeCheckerState: TypeCheckerState,
        otherConstructorMarker: TypeConstructorMarker
    ): Boolean = AbstractTypeChecker.findCorrespondingSupertypes(
        typeCheckerState, this, otherConstructorMarker
    ).isNotEmpty()

    /**
     * 快速判断给定类型是否可能引发空交叉
     *
     * 以下类型不参与空交叉检测（直接返回 false）：
     * - 存根类型（推断中间占位，语义未确定）
     * - 错误类型（已经报错，不重复报）
     * - 非 class/struct/interface 的类型构造器（如函数类型）
     * - Any（顶层类型，任何类型都是其子类型，不造成冲突）
     * - Nothing（底层类型，是所有类型的子类型，不造成冲突）
     *
     * 注意：interface 不在过滤列表中，因为带不同泛型实参的 interface 可能形成空交叉。
     */
    context(context: TypeSystemInferenceExtensionContext)
    private fun mayCauseEmptyIntersection(type: CangJieTypeMarker): Boolean {
        // 仓颉所有类型均为刚性类型，直接转型取存根/错误状态
        val rigidType = type.asRigidType()!!
        if (rigidType.isStubType() || type.isError()) {
            return false
        }

        val typeConstructor = type.typeConstructor()

        if (!typeConstructor.isClassTypeConstructor() && !typeConstructor.isTypeParameterTypeConstructor()) {
            return false
        }

        return !typeConstructor.isAnyConstructor() && !typeConstructor.isNothingConstructor()
    }
}

// =====================================================================
// 空交叉类型信息
// =====================================================================

/**
 * 空交叉类型的种类
 */
enum class EmptyIntersectionTypeKind(
    /** 是否确定为空（true = 一定报错，false = 警告/可能为空） */
    val isDefinitelyEmpty: Boolean
) {
    /** 两个互不相关的 class 或 struct，不可能有公共子类型，确定为空 */
    MULTIPLE_CLASSES(isDefinitelyEmpty = true),

    /** final class/struct 与 interface 组合，可能无公共子类型 */
    FINAL_CLASS_AND_INTERFACE(isDefinitelyEmpty = false),
}

/**
 * 空交叉类型的详细信息
 *
 * @param kind        空交叉的原因种类
 * @param casingTypes 造成冲突的两个刚性类型（用于错误报告）
 */
class EmptyIntersectionTypeInfo(
    /**
     * 空交叉的原因种类。
     */
    val kind: EmptyIntersectionTypeKind,
    /**
     * 造成空交叉判断的刚性类型集合。
     */
    vararg val casingTypes: RigidTypeMarker  // 仓颉所有类型均为刚性类型
)
