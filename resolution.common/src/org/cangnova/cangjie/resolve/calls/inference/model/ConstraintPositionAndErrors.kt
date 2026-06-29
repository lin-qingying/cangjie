/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference.model

import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability.*
import org.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeKind
import org.cangnova.cangjie.type.model.*

/**
 * 标记约束来源只允许作为输入类型参与推断。
 */
interface OnlyInputTypeConstraintPosition

/**
 * 类型推断约束的来源位置基类。
 *
 * 位置对象用于错误归因、推断日志和 OnlyInputTypes 等规则判定，不直接参与类型关系计算。
 */
sealed class ConstraintPosition

/**
 * 显式类型实参产生的约束位置。
 */
abstract class ExplicitTypeParameterConstraintPosition<T>(
    /**
     * 用户显式提供的类型实参。
     */
    val typeArgument: T,
) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    /**
     * 返回显式类型实参位置的调试文本。
     */
    override fun toString(): String = "TypeParameter $typeArgument"
}

/**
 * builder inference 中注入另一个 stub type 时产生的约束位置。
 */
abstract class InjectedAnotherStubTypeConstraintPosition<T>(
    /**
     * 触发 stub type 注入的 builder inference lambda。
     */
    private val builderInferenceLambdaOfInjectedStubType: T,
) : ConstraintPosition(),
    OnlyInputTypeConstraintPosition {
    /**
     * 返回 stub type 注入位置的调试文本。
     */
    override fun toString(): String = "Injected from $builderInferenceLambdaOfInjectedStubType builder inference call"
}

/**
 * builder inference 替换过程中产生的约束位置。
 */
abstract class BuilderInferenceSubstitutionConstraintPosition<L>(
    /**
     * 正在执行 builder inference 的 lambda。
     */
    private val builderInferenceLambda: L,

    /**
     * 被替换并 incorporation 的初始约束。
     */
    val initialConstraint: InitialConstraint,

    /**
     * 该约束是否来自尚未替换的声明上界。
     */
    val isFromNotSubstitutedDeclaredUpperBound: Boolean = false
) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    /**
     * 返回 builder inference 替换位置的调试文本。
     */
    override fun toString(): String = "Incorporated builder inference constraint $initialConstraint " +
            "into $builderInferenceLambda call"
}

/**
 * 顶层调用期望类型产生的约束位置。
 */
abstract class ExpectedTypeConstraintPosition<T>(
    /**
     * 产生期望类型约束的顶层调用。
     */
    val topLevelCall: T,
) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    /**
     * 返回期望类型位置的调试文本。
     */
    override fun toString(): String = "ExpectedType for call $topLevelCall"
}

/**
 * 类型参数声明上界产生的约束位置。
 */
abstract class DeclaredUpperBoundConstraintPosition<T>(
    /**
     * 约束对应的类型参数。
     */
    val typeParameter: T,
) : ConstraintPosition() {
    /**
     * 返回声明上界位置的调试文本。
     */
    override fun toString(): String = "DeclaredUpperBound $typeParameter"
}

/**
 * callable reference 产生的约束位置。
 */
abstract class CallableReferenceConstraintPosition<out T>(
    /**
     * callable reference 调用对象。
     */
    val call: T,
) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    /**
     * 返回 callable reference 位置的调试文本。
     */
    override fun toString(): String = "Callable reference $call"
}

/**
 * 接收者实参产生的约束位置。
 */
abstract class ReceiverConstraintPosition<T>(
    /**
     * 作为接收者的实参。
     */
    val argument: T,
) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    /**
     * 返回接收者位置的调试文本。
     */
    override fun toString(): String = "Receiver $argument"
}

/**
 * The idea of this position is that sometimes we want to reserve the variable type, but it's not yet the moment when we call
 * [org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionContext.fixVariable], for example, we need to take
 * a look into a member scope of a type variable, but it's too early for fixation time because current result type may still contain
 * some other not fixed type variables, like `List<OtherTv>`.
 *
 * Currently, only used inside PCLA
 */
abstract class SemiFixVariableConstraintPosition(
    /**
     * 被预固定的类型变量。
     */
    val variable: TypeVariableMarker,
) : ConstraintPosition() {
    /**
     * 返回预固定位置的调试文本。
     */
    override fun toString(): String = "Preliminary variable $variable fixation"
}

/**
 * 类型变量最终固定时产生的约束位置。
 */
abstract class FixVariableConstraintPosition<T>(
    /**
     * 被固定的类型变量。
     */
    val variable: TypeVariableMarker,

    /**
     * 触发固定的 resolved atom。
     */
    val resolvedAtom: T,
) : ConstraintPosition() {
    /**
     * 返回变量固定位置的调试文本。
     */
    override fun toString(): String = "Fix variable $variable"
}

/**
 * 已知类型形参实参产生的约束位置。
 */
abstract class KnownTypeParameterConstraintPosition<T : CangJieTypeMarker>(
    /**
     * 已知的类型实参。
     */
    val typeArgument: T,
) : ConstraintPosition() {
    /**
     * 返回已知类型实参位置的调试文本。
     */
    override fun toString(): String = "TypeArgument $typeArgument"
}


/**
 * 普通调用实参产生的约束位置基类。
 */
sealed class ArgumentConstraintPosition<out T>(
    /**
     * 产生约束的实参。
     */
    val argument: T,
) : ConstraintPosition()

/**
 * 非 lambda 普通实参产生的约束位置。
 */
abstract class RegularArgumentConstraintPosition<out T>(argument: T) : ArgumentConstraintPosition<T>(argument),
    OnlyInputTypeConstraintPosition {
    /**
     * 返回普通实参位置的调试文本。
     */
    override fun toString(): String = "Argument $argument"
}

/**
 * lambda 实参产生的约束位置。
 */
abstract class LambdaArgumentConstraintPosition<out T>(lambda: T) : ArgumentConstraintPosition<T>(lambda) {
    /**
     * 返回 lambda 实参位置的调试文本。
     */
    override fun toString(): String {
        return "LambdaArgument $argument"
    }
}

/**
 * 以 lambda 命名读取 [LambdaArgumentConstraintPosition.argument]。
 */
val <T> LambdaArgumentConstraintPosition<T>.lambda: T
    get() = argument

/**
 * delegated property 调用产生的约束位置。
 */
open class DelegatedPropertyConstraintPosition<T>(
    /**
     * 产生 delegated property 约束的顶层调用。
     */
    val topLevelCall: T,
) : ConstraintPosition() {
    /**
     * 返回 delegated property 位置的调试文本。
     */
    override fun toString(): String = "Constraint from call $topLevelCall for delegated property"
}

/**
 * incorporation 阶段派生约束的位置。
 */
data class IncorporationConstraintPosition(
    /**
     * incorporation 的原始初始约束。
     */
    val initialConstraint: InitialConstraint,

    /**
     * 该位置是否由声明上界传播而来。
     */
    var isFromDeclaredUpperBound: Boolean = false
) : ConstraintPosition() {
    /**
     * 初始约束的原始来源位置。
     */
    val from: ConstraintPosition get() = initialConstraint.position

    /**
     * 返回 incorporation 位置的调试文本。
     */
    override fun toString(): String = "Incorporate $initialConstraint from position $from"
}

/**
 * builder inference 调用整体对应的约束位置。
 */
object BuilderInferencePosition : ConstraintPosition() {
    /**
     * 返回 builder inference 位置的调试文本。
     */
    override fun toString(): String = "For builder inference call"
}

/**
 * provideDelegate 固定阶段使用的约束位置。
 */
data object ProvideDelegateFixationPosition : ConstraintPosition()

// TODO: should be used only in SimpleConstraintSystemImpl, KT-59675
/**
 * 简单约束系统内部使用的合成约束位置。
 */
object SimpleConstraintSystemConstraintPosition : ConstraintPosition()

// ------------------------------------------------ Errors ------------------------------------------------

/**
 * 约束系统错误基类。
 *
 * @property applicability 该错误映射到候选解析时的适用性等级。
 */
sealed class ConstraintSystemError(val applicability: CandidateApplicability)

/**
 * 约束不匹配错误和警告共享的类型信息。
 */
sealed interface  ConstraintMismatch {
    /**
     * 不匹配关系中的下界类型。
     */
    val lowerType: CangJieTypeMarker

    /**
     * 不匹配关系中的上界类型。
     */
    val upperType: CangJieTypeMarker

    /**
     * 产生不匹配的 incorporation 位置。
     */
    val position: IncorporationConstraintPosition
}

/**
 * 会使候选不可用的约束不匹配错误。
 */
class  ConstraintError(
    /**
     * 不满足子类型关系的下界类型。
     */
    override val lowerType: CangJieTypeMarker,

    /**
     * 不满足子类型关系的上界类型。
     */
    override val upperType: CangJieTypeMarker,

    /**
     * 错误产生的 incorporation 位置。
     */
    override val position: IncorporationConstraintPosition,
) : ConstraintSystemError(if (position.from is ReceiverConstraintPosition<*>) INAPPLICABLE_WRONG_RECEIVER else INAPPLICABLE),
    ConstraintMismatch {
    /**
     * 返回不匹配的子类型关系文本。
     */
    override fun toString(): String {
        return "$lowerType <: $upperType"
    }
}

/**
 * 不会使候选失败、但需要诊断或兼容性处理的约束不匹配警告。
 */
class ConstraintWarning(
    /**
     * 警告中的下界类型。
     */
    override val lowerType: CangJieTypeMarker,

    /**
     * 警告中的上界类型。
     */
    override val upperType: CangJieTypeMarker,

    /**
     * 警告产生的 incorporation 位置。
     */
    override val position: IncorporationConstraintPosition,
) : ConstraintSystemError(RESOLVED), ConstraintMismatch

/**
 * 类型参数缺少足够推断信息时产生的错误。
 */
open class NotEnoughInformationForTypeParameter<T>(
    /**
     * 信息不足的类型变量。
     */
    val typeVariable: TypeVariableMarker,

    /**
     * 触发该错误的 resolved atom。
     */
    val resolvedAtom: T,

    /**
     * 是否仍可能通过非受限 builder inference 解析。
     */
    val couldBeResolvedWithUnrestrictedBuilderInference: Boolean
) : ConstraintSystemError(INAPPLICABLE)

/**
 * 类型变量被推断到声明上界内时产生的兼容性信息。
 */
class InferredIntoDeclaredUpperBounds(
    /**
     * 被推断进声明上界的类型变量。
     */
    val typeVariable: TypeVariableMarker,
) : ConstraintSystemError(RESOLVED)

/**
 * 约束类型本身是错误类型时产生的错误。
 */
class ConstrainingTypeIsError(
    /**
     * 被约束的类型变量。
     */
    val typeVariable: TypeVariableMarker,

    /**
     * 作为约束来源的错误类型。
     */
    val constraintType: CangJieTypeMarker,

    /**
     * 错误产生的 incorporation 位置。
     */
    val position: IncorporationConstraintPosition
) : ConstraintSystemError(INAPPLICABLE)

/**
 * 推断出空交叉类型时共享的诊断信息。
 */
sealed interface InferredEmptyIntersection {
    /**
     * 被判定不兼容的类型集合。
     */
    val incompatibleTypes: List<CangJieTypeMarker>

    /**
     * 导致空交叉的来源类型集合。
     */
    val causingTypes: List<CangJieTypeMarker>

    /**
     * 产生空交叉结果的类型变量。
     */
    val typeVariable: TypeVariableMarker

    /**
     * 空交叉类型的分类。
     */
    val kind: EmptyIntersectionTypeKind
}

/**
 * 推断出空交叉类型但只作为警告处理。
 */
class InferredEmptyIntersectionWarning(
    /**
     * 被判定不兼容的类型集合。
     */
    override val incompatibleTypes: List<CangJieTypeMarker>,

    /**
     * 导致空交叉的来源类型集合。
     */
    override val causingTypes: List<CangJieTypeMarker>,

    /**
     * 产生空交叉结果的类型变量。
     */
    override val typeVariable: TypeVariableMarker,

    /**
     * 空交叉类型的分类。
     */
    override val kind: EmptyIntersectionTypeKind,
) : ConstraintSystemError(RESOLVED), InferredEmptyIntersection

/**
 * 推断出空交叉类型并作为不可用错误处理。
 */
class InferredEmptyIntersectionError(
    /**
     * 被判定不兼容的类型集合。
     */
    override val incompatibleTypes: List<CangJieTypeMarker>,

    /**
     * 导致空交叉的来源类型集合。
     */
    override val causingTypes: List<CangJieTypeMarker>,

    /**
     * 产生空交叉结果的类型变量。
     */
    override val typeVariable: TypeVariableMarker,

    /**
     * 空交叉类型的分类。
     */
    override val kind: EmptyIntersectionTypeKind,
) : ConstraintSystemError(INAPPLICABLE), InferredEmptyIntersection

/**
 * 违反 OnlyInputTypes 限制时产生的诊断。
 */
class OnlyInputTypesDiagnostic(
    /**
     * 违反限制的类型变量。
     */
    val typeVariable: TypeVariableMarker,
) : ConstraintSystemError(INAPPLICABLE)

/**
 * 为保持兼容性而降低候选优先级的诊断。
 */
class LowerPriorityToPreserveCompatibility(
    /**
     * 是否需要把兼容性降级报告为警告。
     */
    val needToReportWarning: Boolean,
) :
    ConstraintSystemError(RESOLVED_NEED_PRESERVE_COMPATIBILITY)

/**
 * 多 lambda builder inference 限制触发时产生的错误。
 */
open class MultiLambdaBuilderInferenceRestriction<T>(
    /**
     * 受限制的匿名函数或 lambda。
     */
    val anonymous: T,

    /**
     * 触发限制的类型参数。
     */
    val typeParameter: TypeParameterMarker
) : ConstraintSystemError(RESOLVED_WITH_ERROR)

/**
 * 判断约束是否来自期望类型或 delegated property 位置。
 */
fun Constraint.isExpectedTypePosition() =
    position.from is ExpectedTypeConstraintPosition<*> || position.from is DelegatedPropertyConstraintPosition<*>

/**
 * 将不可用的约束错误降级为已解析警告。
 */
fun ConstraintError.transformToWarning() = ConstraintWarning(lowerType, upperType, position)
