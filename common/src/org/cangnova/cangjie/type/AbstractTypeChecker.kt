/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.type

import org.cangnova.cangjie.type.model.*

/**
 * 抽象类型检查器，提供子类型检查和类型相等性判断。
 *
 * 参考 K2 AbstractTypeChecker，但融入仓颉 C++ 编译器的类型语义
 * （TypeManager::IsSubtype），针对仓颉语言特性做了以下调整：
 *
 * - 无弹性类型，所有类型都是刚性类型
 * - 无可空类型，使用 Option<T> 替代
 * - 泛型不变（invariant only），isSubtypeForSameConstructor 只检查参数相等
 * - 支持 IdealInt/IdealFloat 字面量类型
 * - 支持联合类型（A|B）和交叉类型（A&B）
 * - 支持函数类型（参数逆变+返回值协变）
     * - 支持元组类型（同长度+元素逐一子类型）
     * - 支持 VArray 类型（同长度+元素类型相等）
 * - Quest 类型作为通配符
 */
object AbstractTypeChecker {

    /**
     * 是否启用仅用于调试/一致性校验的慢断言。
     * 默认关闭，避免影响正常编译路径性能与行为。
     */
    var RUN_SLOW_ASSERTIONS: Boolean = false

    /**
     * 仓颉无弹性类型，超类型遍历时直接将类型转为刚性类型。
     */
    private val RIGID_SUPERTYPES_POLICY = TypeCheckerState.SupertypesPolicy.RigidOnly

    // =====================================================================
    // 公开入口
    // =====================================================================

    /**
     * 判断 [subType] 是否为 [superType] 的子类型。
     */
    fun isSubtypeOf(
        context: TypeCheckerProviderContext,
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
    ): Boolean {
        val state = context.newTypeCheckerState(
            errorTypesEqualToAnything = true,
            stubTypesEqualToAnything = true,
        )
        return isSubtypeOf(state, subType, superType)
    }

    /**
     * 带状态的子类型检查入口。
     */
    fun isSubtypeOf(
        state: TypeCheckerState,
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
    ): Boolean {
        return completeIsSubTypeOf(state, subType, superType)
    }

    /**
     * 按函数引用目标类型规则检查子类型关系。
     *
     * 官方函数引用候选筛选不把普通值到 `Option<T>` 的自动提升作为函数参数兼容关系；
     * 其他继承、泛型和函数方差规则仍复用同一套类型检查器。
     */
    fun isSubtypeOfForFunctionReference(
        context: TypeCheckerProviderContext,
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
    ): Boolean = isSubtypeOfWithoutOptionBoxing(context, subType, superType)

    /**
     * 在禁用 `T -> Option<T>` 自动提升的前提下检查普通子类型关系。
     *
     * 该入口用于语义上要求精确名义/结构关系的阶段，例如函数引用目标筛选和
     * type-pattern usefulness。它仍保留继承、泛型不变性、函数方差等通用规则，
     * 只关闭表达式目标定型阶段才允许的 Option 自动装箱。
     */
    fun isSubtypeOfWithoutOptionBoxing(
        context: TypeCheckerProviderContext,
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
    ): Boolean {
        val state = context.newTypeCheckerState(
            errorTypesEqualToAnything = true,
            stubTypesEqualToAnything = true,
        )
        return isSubtypeOfWithoutOptionBoxing(state, subType, superType)
    }

    /** 在现有类型检查状态中临时关闭 Option 自动装箱。 */
    fun isSubtypeOfWithoutOptionBoxing(
        state: TypeCheckerState,
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
    ): Boolean {
        return state.runWithOptionBoxing(allowed = false) {
            completeIsSubTypeOf(this, subType, superType)
        }
    }

    /**
     * 判断两个类型是否相等（双向子类型）。
     */
    fun equalTypes(
        context: TypeCheckerProviderContext,
        a: CangJieTypeMarker,
        b: CangJieTypeMarker,
    ): Boolean {
        val state = context.newTypeCheckerState(
            errorTypesEqualToAnything = true,
            stubTypesEqualToAnything = true,
        )
        return equalTypes(state, a, b)
    }

    /**
     * 带状态的类型相等性检查。
     */
    fun equalTypes(
        state: TypeCheckerState,
        a: CangJieTypeMarker,
        b: CangJieTypeMarker,
    ): Boolean {
        if (a === b) return true
        return isSubtypeOf(state, a, b) && isSubtypeOf(state, b, a)
    }

    /**
     * 对外暴露与旧调用点兼容的预处理入口。
     * 仓颉语义下仅执行当前上下文定义的 prepare/refine 流程，不引入额外类型语义。
     */
    fun prepareType(
        context: TypeCheckerProviderContext,
        type: CangJieTypeMarker,
    ): CangJieTypeMarker {
        val state = context.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false,
        )
        return state.refineType(state.prepareType(type))
    }

    /**
     * 忽略泛型参数的类级别子类型检查。
     * 仅判断类型构造器之间的继承关系。
     */
    fun isSubtypeOfClass(
        state: TypeCheckerState,
        subType: CangJieTypeMarker,
        superConstructor: TypeConstructorMarker,
    ): Boolean {
        val ctx = state.typeSystemContext
        val subRigid = subType as? RigidTypeMarker ?: return false
        if (ctx.areEqualTypeConstructors(ctx.typeConstructor(subRigid), superConstructor)) return true
        return findCorrespondingSupertypes(state, subRigid, superConstructor).isNotEmpty()
    }

    /**
     * 查找子类型中与指定超类型构造器对应的所有超类型。
     * BFS 遍历超类型图，收集所有匹配的超类型。
     */
    fun findCorrespondingSupertypes(
        state: TypeCheckerState,
        subType: RigidTypeMarker,
        superConstructor: TypeConstructorMarker,
    ): List<RigidTypeMarker> {
        return collectAllSupertypesWithGivenTypeConstructor(state, subType, superConstructor)
    }

    // =====================================================================
    // 核心算法
    // =====================================================================

    /**
     * 完整的子类型检查流程。
     *
     * 仓颉无弹性类型，所有类型都是刚性类型，流程简化：
     * 1. prepareType + refineType 预处理
     * 2. 检查特殊情况（Error/Stub/Quest 类型）
     * 3. 约束传播（类型推断）
     * 4. 对刚性类型进行子类型判定
     */
    private fun completeIsSubTypeOf(
        state: TypeCheckerState,
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
    ): Boolean {
        val ctx = state.typeSystemContext

        // 预处理
        val preparedSubType = state.refineType(state.prepareType(subType))
        val preparedSuperType = state.refineType(state.prepareType(superType))

        // 引用相等
        if (preparedSubType === preparedSuperType) return true

        // 特殊情况快速路径
        checkSubtypeForSpecialCases(state, preparedSubType, preparedSuperType)?.let { return it }

        // 约束传播（类型推断时使用）
        state.addSubtypeConstraint(preparedSubType, preparedSuperType)?.let { return it }

        // 对齐 Kotlin AbstractTypeChecker：
        // 进入通用 subtype 算法前，必须经过类型系统上下文的刚性化视图，
        // 让上下文有机会把 typealias 等语义包装规约成真实类型头。
        val subRigid = ctx.asRigidType(preparedSubType) ?: return false
        val superRigid = ctx.asRigidType(preparedSuperType) ?: return false

        return isSubtypeOfForSingleClassifierType(state, subRigid, superRigid)
    }

    /**
     * 检查特殊类型的子类型关系。
     *
     * @return true/false 表示确定结果，null 表示需要继续检查
     */
    private fun checkSubtypeForSpecialCases(
        state: TypeCheckerState,
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
    ): Boolean? {
        val ctx = state.typeSystemContext

        // Error 类型：根据配置决定是否与任何类型兼容
        if (ctx.isError(subType) || ctx.isError(superType)) {
            if (state.isErrorTypeEqualsToAnything) return true
            if (ctx.isError(subType) && ctx.isError(superType)) return true
        }

        // 存根类型：推断阶段的占位类型
        val subRigid = subType as? RigidTypeMarker
        val superRigid = superType as? RigidTypeMarker
        if (subRigid != null && ctx.isStubType(subRigid) ||
            superRigid != null && ctx.isStubType(superRigid)
        ) {
            if (state.isStubTypeEqualsToAnything) return true
        }

        // 类型变量
        if (state.isAllowedTypeVariable(subType) || state.isAllowedTypeVariable(superType)) {
            return true
        }

        return null
    }

    /**
     * 对两个刚性类型进行子类型判定。
     *
     * 仓颉特有规则：
     * - IdealType：IdealInt <: 任何整数类型，IdealFloat <: 任何浮点类型
     * - 联合类型：(A|B) <: T ⟺ A <: T ∧ B <: T
     * - 交叉类型：(A&B) <: T ⟺ A <: T ∨ B <: T
     * - 不变泛型：同构造器时只检查参数相等
     */
    private fun isSubtypeOfForSingleClassifierType(
        state: TypeCheckerState,
        subType: RigidTypeMarker,
        superType: RigidTypeMarker,
    ): Boolean {
        val ctx = state.typeSystemContext

        // Nothing 是所有类型的子类型
        if (ctx.isNothing(subType)) return true

        /*
         * 普通 primitive 是 final 的值类型，不存在 primitive kind 之间的继承或 widening。
         * 在进入 corresponding-supertypes BFS 前按 constructor 常量时间判定，避免为大量
         * 明确不兼容的重载签名反复遍历父类型图。Nothing、ideal literal 以及
         * primitive-to-interface 装箱仍保留后续通用语义。
         */
        if (
            ctx.isPrimitiveType(subType) &&
            ctx.isPrimitiveType(superType) &&
            !ctx.isNothing(superType) &&
            !ctx.isIntegerLiteralType(subType) &&
            !ctx.isIntegerLiteralType(superType)
        ) {
            return ctx.areEqualTypeConstructors(ctx.typeConstructor(subType), ctx.typeConstructor(superType))
        }

        // 官方 TypeManager::IsSubtype 中，只有允许 implicitBoxed 时值类型才可装箱到 Any；
        // 禁止隐式装箱的函数类型子类型检查里，仅 class/interface 这类引用语义类型保留 Any 关系。
        if (ctx.isAnyConstructor(ctx.typeConstructor(superType))) {
            if (state.isImplicitBoxingAllowed || ctx.isClassLikeType(subType)) return true
        }

        val optionBoxedElementType = ctx.optionBoxedElementTypeOf(superType)
        if (
            state.isOptionBoxingAllowed &&
            optionBoxedElementType != null &&
            ctx.optionNestedLevelOf(subType) < ctx.optionNestedLevelOf(superType) &&
            completeIsSubTypeOf(state, subType, optionBoxedElementType)
        ) {
            return true
        }

        val subConstructor = ctx.typeConstructor(subType)
        val superConstructor = ctx.typeConstructor(superType)

        // 交叉类型（超类型）：T <: (A & B) 当 T <: A 且 T <: B
        if (ctx.isIntersection(superConstructor)) {
            return ctx.supertypes(superConstructor).all { completeIsSubTypeOf(state, subType, it) }
        }

        // 交叉类型（子类型）：(A & B) <: T 当 A <: T 或 B <: T
        if (ctx.isIntersection(subConstructor)) {
            return ctx.supertypes(subConstructor).any { completeIsSubTypeOf(state, it, superType) }
        }

        // 整型字面量类型处理
        if (ctx.isIntegerLiteralType(subType)) {
            return ctx.possibleIntegerTypes(subType).any { completeIsSubTypeOf(state, it, superType) }
        }
        if (ctx.isIntegerLiteralType(superType)) {
            if (ctx.possibleIntegerTypes(superType).any { completeIsSubTypeOf(state, subType, it) }) return true
        }

        // 仓颉函数/元组/VArray 是内置结构类型，不走普通用户泛型的不变实参规则。
        checkSubtypeForFunctionType(state, subType, superType)?.let { return it }
        checkSubtypeForTupleType(state, subType, superType)?.let { return it }
        checkSubtypeForVArrayType(state, subType, superType)?.let { return it }

        // 同构造器 + 无参数 → true
        if (ctx.areEqualTypeConstructors(subConstructor, superConstructor)) {
            if (ctx.argumentsCount(subType) == 0) return true
            return isSubtypeForSameConstructor(state, subType, superType)
        }

        // 类型参数：检查上界
        if (ctx.isTypeParameterTypeConstructor(subConstructor)) {
            val classifier = ctx.getTypeParameterClassifier(subConstructor)
            if (classifier != null) {
                val upperBoundCount = ctx.upperBoundCount(classifier)
                if (ctx.isAnyConstructor(superConstructor) && !state.isImplicitBoxingAllowed) {
                    // 对齐官方 TypeManager::IsGenericSubtype：
                    // 函数/元组等结构元素禁止隐式装箱时，T <: Any 只能由 T 的直接 class 上界满足。
                    return (0 until upperBoundCount).any { index ->
                        val upperBound = ctx.asRigidType(ctx.getUpperBound(classifier, index)) ?: return@any false
                        ctx.isClassTypeConstructor(ctx.typeConstructor(upperBound))
                    }
                }
                for (i in 0 until upperBoundCount) {
                    if (completeIsSubTypeOf(state, ctx.getUpperBound(classifier, i), superType)) return true
                }
            }
        }

        // 查找对应超类型
        val correspondingSupertypes = findCorrespondingSupertypes(state, subType, superConstructor)
        if (correspondingSupertypes.size > 1) {
            return state.runForkingPoint {
                for (correspondingSupertype in correspondingSupertypes) {
                    fork {
                        isSubtypeForSameConstructor(state, correspondingSupertype, superType)
                    }
                }
            }
        }
        for (correspondingSupertype in correspondingSupertypes) {
            if (isSubtypeForSameConstructor(state, correspondingSupertype, superType)) return true
        }

        return false
    }

    /**
     * 函数类型子类型规则。
     *
     * 对齐官方 TypeManager::IsFuncSubtype / LocalTypeArgumentSynthesis::UnifyFuncTy：
     * - 参数个数一致；
     * - 参数逆变：super.param <: sub.param；
     * - 返回协变：sub.ret <: super.ret；
     * - CFunc 与变长参数等函数类型头部信息一致。
     */
    private fun checkSubtypeForFunctionType(
        state: TypeCheckerState,
        subType: RigidTypeMarker,
        superType: RigidTypeMarker,
    ): Boolean? {
        val ctx = state.typeSystemContext
        val subIsFunction = ctx.isFunctionType(subType)
        val superIsFunction = ctx.isFunctionType(superType)
        if (!subIsFunction && !superIsFunction) return null
        if (!subIsFunction || !superIsFunction) return false
        if (!ctx.areEqualFunctionTypeKinds(subType, superType)) return false

        val subArguments = ctx.extractArgumentsForFunctionType(subType)
        val superArguments = ctx.extractArgumentsForFunctionType(superType)
        if (subArguments.size != superArguments.size || subArguments.isEmpty()) return false

        val returnIndex = subArguments.lastIndex
        for (index in 0 until returnIndex) {
            val subParameterType = subArguments[index]
            val superParameterType = superArguments[index]
            state.runWithArgumentsSettings(subParameterType) {
                if (!runWithImplicitBoxing(false) { isSubtypeOf(state, superParameterType, subParameterType) }) {
                    return false
                }
            }
        }

        return state.runWithArgumentsSettings(subArguments[returnIndex]) {
            runWithImplicitBoxing(false) {
                isSubtypeOf(state, subArguments[returnIndex], superArguments[returnIndex])
            }
        }
    }

    /**
     * 元组类型子类型规则。
     *
     * 对齐官方 TypeManager::IsTupleSubtype：同长度元组逐元素协变。
     */
    private fun checkSubtypeForTupleType(
        state: TypeCheckerState,
        subType: RigidTypeMarker,
        superType: RigidTypeMarker,
    ): Boolean? {
        val ctx = state.typeSystemContext
        val subIsTuple = ctx.isTupleType(subType)
        val superIsTuple = ctx.isTupleType(superType)
        if (!subIsTuple && !superIsTuple) return null
        if (!subIsTuple || !superIsTuple) return false

        val subElements = ctx.extractElementsForTupleType(subType)
        val superElements = ctx.extractElementsForTupleType(superType)
        if (subElements.size != superElements.size) return false

        for (index in subElements.indices) {
            val subElementType = subElements[index]
            val superElementType = superElements[index]
            state.runWithArgumentsSettings(subElementType) {
                if (!isSubtypeOf(state, subElementType, superElementType)) return false
            }
        }

        return true
    }

    /**
     * VArray 类型子类型规则。
     *
     * 对齐官方 TypeManager::IsVArraySubtype：两侧都必须是 VArray，尺寸一致，
     * 且元素类型 `IsTyEqual`。这不是协变数组规则。
     */
    private fun checkSubtypeForVArrayType(
        state: TypeCheckerState,
        subType: RigidTypeMarker,
        superType: RigidTypeMarker,
    ): Boolean? {
        val ctx = state.typeSystemContext
        val subIsVArray = ctx.isVArrayType(subType)
        val superIsVArray = ctx.isVArrayType(superType)
        if (!subIsVArray && !superIsVArray) return null
        if (!subIsVArray || !superIsVArray) return false
        if (ctx.extractSizeForVArrayType(subType) != ctx.extractSizeForVArrayType(superType)) return false

        val subElementType = ctx.extractElementTypeForVArrayType(subType)
        val superElementType = ctx.extractElementTypeForVArrayType(superType)
        return state.runWithArgumentsSettings(subElementType) {
            equalTypes(state, subElementType, superElementType)
        }
    }

    /**
     * 同构造器类型的参数子类型检查。
     *
     * 仓颉泛型是不变的（invariant），所以只检查参数相等。
     */
    private fun isSubtypeForSameConstructor(
        state: TypeCheckerState,
        subType: RigidTypeMarker,
        superType: RigidTypeMarker,
    ): Boolean {
        val ctx = state.typeSystemContext

        // 优化：参数引用相同
        if (ctx.identicalArguments(subType, superType)) return true

        val count = ctx.argumentsCount(subType)
        if (count != ctx.argumentsCount(superType)) return false

        // 仓颉泛型不变，所有参数必须相等
        for (i in 0 until count) {
            val subArgument = ctx.getArgument(subType, i)
            val superArgument = ctx.getArgument(superType, i)
            val subArgType = ctx.getType(subArgument) ?: continue
            val superArgType = ctx.getType(superArgument) ?: continue

            state.runWithArgumentsSettings(subArgType) {
                if (!equalTypes(state, subArgType, superArgType)) return false
            }
        }

        return true
    }

    // =====================================================================
    // 超类型收集
    // =====================================================================

    /**
     * BFS 遍历超类型图，收集所有与指定构造器匹配的超类型。
     */
    private fun collectAllSupertypesWithGivenTypeConstructor(
        state: TypeCheckerState,
        subType: RigidTypeMarker,
        superConstructor: TypeConstructorMarker,
    ): List<RigidTypeMarker> {
        val ctx = state.typeSystemContext

        if (
            !state.isImplicitBoxingAllowed &&
            ctx.isInterface(superConstructor) &&
            ctx.isPrimitiveType(subType)
        ) {
            return emptyList()
        }

        ctx.fastCorrespondingSupertypes(subType, superConstructor)?.let { return it }

        if (!ctx.isClassTypeConstructor(superConstructor) && !ctx.isInterface(superConstructor) && ctx.isClassType(subType)) {
            return emptyList()
        }

        if (ctx.isCommonFinalClassConstructor(superConstructor)) {
            return if (ctx.areEqualTypeConstructors(ctx.typeConstructor(subType), superConstructor)) {
                listOf(subType)
            } else {
                emptyList()
            }
        }

        val result = mutableListOf<RigidTypeMarker>()
        state.anySupertype(subType, { false }) { current ->
            when {
                ctx.areEqualTypeConstructors(ctx.typeConstructor(current), superConstructor) -> {
                    result.add(current)
                    TypeCheckerState.SupertypesPolicy.None
                }
                ctx.argumentsCount(current) == 0 -> RIGID_SUPERTYPES_POLICY
                else -> ctx.substitutionSupertypePolicy(current)
            }
        }
        return result
    }
}

// =====================================================================
// TypeSystemContext 方法的非成员扩展调用桥接
// 避免 with(context) 导致的 context receiver 歧义
// =====================================================================

/** 桥接 [TypeSystemContext.isError] */
private fun TypeSystemContext.isError(type: CangJieTypeMarker): Boolean = type.isError()

/** 桥接 [TypeSystemContext.asRigidType] */
private fun TypeSystemContext.asRigidType(type: CangJieTypeMarker): RigidTypeMarker? = with(type) { asRigidType() }

/** 桥接 [TypeSystemContext.isStubType] */
private fun TypeSystemContext.isStubType(type: RigidTypeMarker): Boolean = type.isStubType()

/** 桥接 [TypeSystemContext.isNothing] */
private fun TypeSystemContext.isNothing(type: CangJieTypeMarker): Boolean = type.isNothing()

/** 桥接 [TypeSystemContext.optionBoxedElementType] */
private fun TypeSystemContext.optionBoxedElementTypeOf(type: CangJieTypeMarker): CangJieTypeMarker? =
    with(this) { type.optionBoxedElementType() }

/** 桥接 [TypeSystemContext.optionNestedLevel] */
private fun TypeSystemContext.optionNestedLevelOf(type: CangJieTypeMarker): Int = with(this) { type.optionNestedLevel() }

/** 桥接 [TypeSystemContext.typeConstructor] (RigidTypeMarker) */
private fun TypeSystemContext.typeConstructor(type: RigidTypeMarker): TypeConstructorMarker = type.typeConstructor()

/** 桥接 [TypeSystemContext.typeConstructor] (CangJieTypeMarker) */
private fun TypeSystemContext.typeConstructor(type: CangJieTypeMarker): TypeConstructorMarker = type.typeConstructor()

/** 桥接 [TypeSystemContext.isAnyConstructor] */
private fun TypeSystemContext.isAnyConstructor(ctor: TypeConstructorMarker): Boolean = ctor.isAnyConstructor()

/** 桥接 [TypeSystemContext.isClassLikeType] */
private fun TypeSystemContext.isClassLikeType(type: CangJieTypeMarker): Boolean =
    with(this) { type.isClassLikeType() }

/** 桥接 [TypeSystemContext.isIntersection] */
private fun TypeSystemContext.isIntersection(ctor: TypeConstructorMarker): Boolean = ctor.isIntersection()

/** 桥接 [TypeSystemContext.supertypes] */
private fun TypeSystemContext.supertypes(ctor: TypeConstructorMarker): Collection<CangJieTypeMarker> = ctor.supertypes()

/** 桥接 [TypeSystemContext.isIntegerLiteralType] */
private fun TypeSystemContext.isIntegerLiteralType(type: RigidTypeMarker): Boolean = type.isIntegerLiteralType()

/** 桥接 [TypeSystemContext.possibleIntegerTypes] */
private fun TypeSystemContext.possibleIntegerTypes(type: RigidTypeMarker): Collection<CangJieTypeMarker> = type.possibleIntegerTypes()

/** 桥接 [TypeSystemContext.isFunctionType] */
private fun TypeSystemContext.isFunctionType(type: CangJieTypeMarker): Boolean = with(this) { type.isFunctionType() }

/** 桥接 [TypeSystemContext.extractArgumentsForFunctionType] */
private fun TypeSystemContext.extractArgumentsForFunctionType(type: CangJieTypeMarker): List<CangJieTypeMarker> =
    with(this) { type.extractArgumentsForFunctionType() }

/** 桥接 [TypeSystemContext.areEqualFunctionTypeKinds] */
private fun TypeSystemContext.areEqualFunctionTypeKinds(
    subType: CangJieTypeMarker,
    superType: CangJieTypeMarker,
): Boolean = with(this) { areEqualFunctionTypeKinds(subType, superType) }

/** 桥接 [TypeSystemContext.isTupleType] */
private fun TypeSystemContext.isTupleType(type: CangJieTypeMarker): Boolean = with(this) { type.isTupleType() }

/** 桥接 [TypeSystemContext.extractElementsForTupleType] */
private fun TypeSystemContext.extractElementsForTupleType(type: CangJieTypeMarker): List<CangJieTypeMarker> =
    with(this) { type.extractElementsForTupleType() }

/** 桥接 [TypeSystemContext.isVArrayType] */
private fun TypeSystemContext.isVArrayType(type: CangJieTypeMarker): Boolean = with(this) { type.isVArrayType() }

/** 桥接 [TypeSystemContext.extractElementTypeForVArrayType] */
private fun TypeSystemContext.extractElementTypeForVArrayType(type: CangJieTypeMarker): CangJieTypeMarker =
    with(this) { type.extractElementTypeForVArrayType() }

/** 桥接 [TypeSystemContext.extractSizeForVArrayType] */
private fun TypeSystemContext.extractSizeForVArrayType(type: CangJieTypeMarker): Long =
    with(this) { type.extractSizeForVArrayType() }

/** 桥接 [TypeSystemContext.argumentsCount] */
private fun TypeSystemContext.argumentsCount(type: CangJieTypeMarker): Int = type.argumentsCount()

/** 桥接 [TypeSystemContext.getArgument] */
private fun TypeSystemContext.getArgument(type: CangJieTypeMarker, index: Int): TypeArgumentMarker = type.getArgument(index)

/** 桥接 [TypeSystemContext.getType] */
private fun TypeSystemContext.getType(arg: TypeArgumentMarker): CangJieTypeMarker? = arg.getType()

/** 桥接 [TypeSystemContext.isTypeParameterTypeConstructor] */
private fun TypeSystemContext.isTypeParameterTypeConstructor(ctor: TypeConstructorMarker): Boolean = ctor.isTypeParameterTypeConstructor()

/** 桥接 [TypeSystemContext.getTypeParameterClassifier] */
private fun TypeSystemContext.getTypeParameterClassifier(ctor: TypeConstructorMarker): TypeParameterMarker? = ctor.getTypeParameterClassifier()

/** 桥接 [TypeSystemContext.upperBoundCount] */
private fun TypeSystemContext.upperBoundCount(param: TypeParameterMarker): Int = param.upperBoundCount()

/** 桥接 [TypeSystemContext.getUpperBound] */
private fun TypeSystemContext.getUpperBound(param: TypeParameterMarker, index: Int): CangJieTypeMarker = param.getUpperBound(index)

/** 桥接 [TypeSystemContext.isClassTypeConstructor] */
private fun TypeSystemContext.isClassTypeConstructor(ctor: TypeConstructorMarker): Boolean = ctor.isClassTypeConstructor()

/** 桥接 [TypeSystemContext.isInterface] */
private fun TypeSystemContext.isInterface(ctor: TypeConstructorMarker): Boolean = ctor.isInterface()

/** 桥接 [TypeSystemContext.isClassType] */
private fun TypeSystemContext.isClassType(type: RigidTypeMarker): Boolean = type.isClassType()

/** 调用 [TypeSystemContext.isPrimitiveType] */
private fun TypeSystemContext.isPrimitiveType(type: RigidTypeMarker): Boolean = type.isPrimitiveType()

/** 桥接 [TypeSystemContext.isCommonFinalClassConstructor] */
private fun TypeSystemContext.isCommonFinalClassConstructor(ctor: TypeConstructorMarker): Boolean = ctor.isCommonFinalClassConstructor()

/** 桥接 [TypeSystemContext.fastCorrespondingSupertypes] */
private fun TypeSystemContext.fastCorrespondingSupertypes(
    type: RigidTypeMarker, ctor: TypeConstructorMarker
): List<SimpleTypeMarker>? = type.fastCorrespondingSupertypes(ctor)
