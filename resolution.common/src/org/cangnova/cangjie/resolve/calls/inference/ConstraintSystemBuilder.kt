/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.cangnova.cangjie.type.model.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * 约束系统构建阶段允许执行的可变操作集合。
 *
 * 该接口抽象出注册类型变量、添加约束、管理 postponed 变量与合并其他系统的能力，
 * 使调用解析和推断组件可以通过统一入口操作当前约束系统。
 */
interface ConstraintSystemOperation {
    /**
     * 当前约束系统是否已经包含不可满足的约束矛盾。
     */
    val hasContradiction: Boolean

    /**
     * 向约束系统注册新的类型变量。
     */
    fun registerVariable(variable: TypeVariableMarker)

    /**
     * 标记 [variable] 为 postponed 变量，推迟其固定时机。
     */
    fun markPostponedVariable(variable: TypeVariableMarker)

    /**
     * 标记当前系统可使用非受限 builder inference 继续解析。
     */
    fun markCouldBeResolvedWithUnrestrictedBuilderInference()

    /**
     * 取消 [variable] 的 postponed 标记。
     */
    fun unmarkPostponedVariable(variable: TypeVariableMarker)

    /**
     * 清除当前系统中所有 postponed 变量标记。
     */
    fun removePostponedVariables()

    /**
     * 使用 [substitutor] 替换已经固定的类型变量。
     */
    fun substituteFixedVariables(substitutor: TypeSubstitutorMarker)

    /**
     * 按顶层类型变量与期望类型路径读取 postponed 参数已经构建出的函数期望类型。
     */
    fun getBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>
    ): CangJieTypeMarker?

    /**
     * 按期望类型变量读取 postponed 参数已经构建出的函数期望类型。
     */
    fun getBuiltFunctionalExpectedTypeForPostponedArgument(expectedTypeVariable: TypeConstructorMarker): CangJieTypeMarker?

    /**
     * 记录由顶层类型变量和期望类型路径定位的 postponed 参数函数期望类型。
     */
    fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>,
        builtFunctionalType: CangJieTypeMarker
    )

    /**
     * 记录由期望类型变量定位的 postponed 参数函数期望类型。
     */
    fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        expectedTypeVariable: TypeConstructorMarker,
        builtFunctionalType: CangJieTypeMarker
    )

    /**
     * 添加 [lowerType] <: [upperType] 子类型约束。
     */
    fun addSubtypeConstraint(lowerType: CangJieTypeMarker, upperType: CangJieTypeMarker, position: ConstraintPosition)

    /**
     * 添加 [a] == [b] 等价约束。
     */
    fun addEqualityConstraint(a: CangJieTypeMarker, b: CangJieTypeMarker, position: ConstraintPosition)

    /**
     * 判断 [type] 是否为不依赖未固定变量的 proper type。
     */
    fun isProperType(type: CangJieTypeMarker): Boolean

    /**
     * 判断 [type] 是否表示当前约束系统中的类型变量。
     */
    fun isTypeVariable(type: CangJieTypeMarker): Boolean

    /**
     * 判断 [typeVariable] 是否仍处于 postponed 状态。
     */
    fun isPostponedTypeVariable(typeVariable: TypeVariableMarker): Boolean


    /**
     * 将 [otherSystem] 的约束、变量和错误信息加入当前系统。
     */
    fun addOtherSystem(otherSystem: ConstraintStorage)

    /**
     * 当前约束系统收集到的错误列表。
     */
    val errors: List<ConstraintSystemError>
}

/**
 * 约束系统事务句柄。
 *
 * 事务用于临时尝试添加约束；调用方必须显式关闭以提交，或回滚以恢复事务开始前状态。
 */
abstract class ConstraintSystemTransaction {
    /**
     * 提交事务中记录的约束系统变更。
     */
    abstract fun closeTransaction()

    /**
     * 回滚事务中记录的约束系统变更。
     */
    abstract fun rollbackTransaction()
}

/**
 * 约束系统构建器。
 *
 * 构建器在 [ConstraintSystemOperation] 基础上提供事务、当前替换器和只读存储快照，
 * 是调用解析向类型推断系统注入约束的主要入口。
 */
interface ConstraintSystemBuilder : ConstraintSystemOperation {
    /**
     * 开启一个可提交或回滚的约束系统事务。
     */
    fun prepareTransaction(): ConstraintSystemTransaction

    /**
     * 构建当前已固定类型变量对应的替换器。
     */
    fun buildCurrentSubstitutor(): TypeSubstitutorMarker

    /**
     * 返回当前约束系统的只读存储视图。
     */
    fun currentStorage(): ConstraintStorage
}

/**
 * 在事务中执行 [runOperations]。
 *
 * 当 [runOperations] 返回 `true` 时提交事务并返回 `true`；返回 `false` 时回滚事务并
 * 返回 `false`。
 */
@OptIn(ExperimentalContracts::class)
inline fun ConstraintSystemBuilder.runTransaction(crossinline runOperations: ConstraintSystemOperation.() -> Boolean): Boolean {
    contract {
        callsInPlace(runOperations, InvocationKind.EXACTLY_ONCE)
    }
    val transactionState = prepareTransaction()

    // typeVariablesTransaction is clear
    if (runOperations()) {
        transactionState.closeTransaction()
        return true
    }

    transactionState.rollbackTransaction()
    return false
}

/**
 * 尝试添加子类型约束，并仅在不会产生矛盾时保留该约束。
 */
fun ConstraintSystemBuilder.addSubtypeConstraintIfCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition
): Boolean = addConstraintIfCompatible(lowerType, upperType, position, ConstraintKind.LOWER)

/**
 * 尝试添加等价约束，并仅在不会产生矛盾时保留该约束。
 */
fun ConstraintSystemBuilder.addEqualityConstraintIfCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition
): Boolean = addConstraintIfCompatible(lowerType, upperType, position, ConstraintKind.EQUALITY)

/**
 * 在事务中尝试添加指定 [kind] 的约束，并按兼容性决定提交或回滚。
 */
private fun ConstraintSystemBuilder.addConstraintIfCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    position: ConstraintPosition,
    kind: ConstraintKind
): Boolean = runTransaction {
    if (!hasContradiction) {
        when (kind) {
            ConstraintKind.LOWER -> addSubtypeConstraint(lowerType, upperType, position)
            ConstraintKind.UPPER -> addSubtypeConstraint(upperType, lowerType, position)
            ConstraintKind.EQUALITY -> addEqualityConstraint(lowerType, upperType, position)
        }
    }
    !hasContradiction
}

/**
 * 判断添加 [lowerType] <: [upperType] 约束是否会保持系统兼容。
 */
fun ConstraintSystemBuilder.isSubtypeConstraintCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
): Boolean = isConstraintCompatible(lowerType, upperType, ConstraintKind.LOWER)

/**
 * 判断添加 [lowerType] == [upperType] 约束是否会保持系统兼容。
 */
fun ConstraintSystemBuilder.isEqualityConstraintCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
): Boolean = isConstraintCompatible(lowerType, upperType, ConstraintKind.EQUALITY)

/**
 * 通过回滚事务探测指定 [kind] 的约束是否与当前系统兼容。
 */
private fun ConstraintSystemBuilder.isConstraintCompatible(
    lowerType: CangJieTypeMarker,
    upperType: CangJieTypeMarker,
    kind: ConstraintKind
): Boolean {
    var isCompatible = false
    runTransaction {
        if (!hasContradiction) {
            // the type of position is irrelevant since the constraint is always rolled back.
            val position = SimpleConstraintSystemConstraintPosition
            when (kind) {
                ConstraintKind.LOWER -> addSubtypeConstraint(lowerType, upperType, position)
                ConstraintKind.UPPER -> addSubtypeConstraint(upperType, lowerType, position)
                ConstraintKind.EQUALITY -> addEqualityConstraint(lowerType, upperType, position)
            }
        }
        isCompatible = !hasContradiction
        false
    }
    return isCompatible
}
