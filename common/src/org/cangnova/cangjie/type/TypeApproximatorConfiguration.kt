/*
 * 类型近似器配置
 *
 * 控制类型近似算法的行为策略，包括：
 * - 交叉类型近似策略
 * - 捕获类型近似条件
 * - 整型字面量类型近似
 * - 本地/匿名类型近似
 * - 类型变量近似
 *
 * 改造说明（相对于 Kotlin K2 原版）：
 * - 移除 approximateAllFlexible 及派生属性（仓颉无弹性类型）
 * - 移除 approximateDefinitelyNotNullTypes（仓颉无 DefinitelyNotNull 类型）
 * - 移除 convertToNonRawVersionAfterApproximationInK2（仓颉无 Raw 类型）
 * - 移除 IntersectionStrategy.TO_UPPER_BOUND_IF_SUPERTYPE（K1 遗留策略）
 * - 移除 @AllowedToUsedOnlyInK1、@K2Only 注解及相关配置
 * - 移除 shouldApproximateTypeVariableBasedType 的 isK2 参数
 * - 移除 IntegerLiteralsTypesApproximation（K1 专用配置）
 * - 移除 UpperBoundAwareIntersectionTypeApproximator（K1 专用配置）
 */

package org.cangnova.cangjie.type

import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.CaptureStatus
import org.cangnova.cangjie.type.model.CapturedTypeMarker
import org.cangnova.cangjie.type.model.TypeSystemInferenceExtensionContext
import org.cangnova.cangjie.type.model.TypeVariableTypeConstructorMarker

abstract class TypeApproximatorConfiguration {

    /**
     * 交叉类型近似策略
     */
    enum class IntersectionStrategy {
        /** 允许保留交叉类型：A <: A', B <: B' => A & B <: A' & B' */
        ALLOWED,
        /** 取交叉类型的第一个成员 */
        TO_FIRST,
        /** 取交叉类型所有成员的公共父类型 */
        TO_COMMON_SUPERTYPE,
    }

    /** 是否近似错误类型 */
    open val approximateErrorTypes: Boolean get() = true

    /** 是否近似整型字面量常量类型（如 IdealInt） */
    open val approximateIntegerLiteralConstantTypes: Boolean get() = false

    /** 是否近似整型常量运算类型 */
    open val approximateIntegerConstantOperatorTypes: Boolean get() = false

    /** 整型字面量类型近似时的期望类型（用于引导近似方向） */
    open val expectedTypeForIntegerLiteralType: CangJieTypeMarker? get() = null

    /** 交叉类型的近似策略 */
    open val intersectionStrategy: IntersectionStrategy get() = IntersectionStrategy.TO_COMMON_SUPERTYPE

    /** 是否近似位于逆变位置的交叉类型 */
    open val approximateIntersectionTypesInContravariantPositions get() = false

    /** 是否近似本地类型（函数内部定义的 class/struct） */
    open val approximateLocalTypes get() = false

    /**
     * 本地类型近似的额外条件。
     * 若返回 false，则当前本地类型视为最终近似结果，不再继续搜索父类型。
     * 注意 [approximateLocalTypes] 须为 true 此方法才有效。
     */
    open fun shouldApproximateLocalType(ctx: TypeSystemInferenceExtensionContext, type: CangJieTypeMarker): Boolean =
        true

    /** 是否近似匿名类型 */
    open val approximateAnonymous get() = false

    /**
     * 判断是否应近似基于类型变量的类型。
     *
     * @param marker 遇到的类型变量
     * @return true 表示应将类型变量近似掉，false 表示保留
     */
    internal open fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker): Boolean = true

    /**
     * 判断是否应近似给定的捕获类型。
     *
     * @return true 表示应近似，false 表示保留原样
     */
    context(ctx: TypeSystemInferenceExtensionContext)
    open fun shouldApproximateCapturedType(type: CapturedTypeMarker): Boolean {
        return true
    }

    // =================================================================
    // 预设配置
    // =================================================================

    /** 本地声明的类型近似配置 */
    object LocalDeclaration : TypeApproximatorConfiguration() {
        override val intersectionStrategy: IntersectionStrategy get() = IntersectionStrategy.ALLOWED
        override val approximateErrorTypes: Boolean get() = false
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true

        // 仓颉编译器不近似类型变量
        override fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker): Boolean = false
    }

    /**
     * 公开声明的类型近似配置
     *
     * 用于需要对外暴露的类型，确保不泄露内部实现细节。
     */
    abstract class PublicDeclaration(
        override val approximateLocalTypes: Boolean,
        override val approximateAnonymous: Boolean,
    ) : TypeApproximatorConfiguration() {
        override val approximateErrorTypes: Boolean get() = false
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true

        override fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker): Boolean = false

        /** 保留匿名类型 */
        object SaveAnonymousTypes : PublicDeclaration(approximateLocalTypes = false, approximateAnonymous = false)

        /** 近似匿名类型 */
        object ApproximateAnonymousTypes : PublicDeclaration(approximateLocalTypes = false, approximateAnonymous = true)

        /** 同时近似本地类型和匿名类型 */
        object ApproximateLocalAndAnonymousTypes : PublicDeclaration(approximateLocalTypes = true, approximateAnonymous = true)
    }

    /**
     * 仅近似特定状态的捕获类型和整型字面量类型的配置基类。
     * 不近似错误类型，交叉类型保持原样（ALLOWED 策略）。
     */
    sealed class AbstractCapturedTypesAndILTApproximation(
        private val approximatedCapturedStatus: CaptureStatus?,
    ) : TypeApproximatorConfiguration() {
        override val approximateErrorTypes: Boolean get() = false

        context(ctx: TypeSystemInferenceExtensionContext)
        override fun shouldApproximateCapturedType(type: CapturedTypeMarker): Boolean {
            return approximatedCapturedStatus != null &&
                    with(ctx) { type.captureStatus() } == approximatedCapturedStatus
        }

        override val intersectionStrategy: IntersectionStrategy get() = IntersectionStrategy.ALLOWED
        override fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker): Boolean = false
    }

    /** 约束合并阶段的近似配置：近似 FOR_INCORPORATION 状态的捕获类型 */
    object IncorporationConfiguration : AbstractCapturedTypesAndILTApproximation(CaptureStatus.FOR_INCORPORATION) {
        context(ctx: TypeSystemInferenceExtensionContext)
        override fun shouldApproximateCapturedType(type: CapturedTypeMarker): Boolean {
            if (super.shouldApproximateCapturedType(type)) return true

            // 递归检查嵌套的捕获类型
            return with(ctx) {
                type.contains { nested ->
                    nested is CapturedTypeMarker && super@IncorporationConfiguration.shouldApproximateCapturedType(nested)
                }
            }
        }
    }

    /** 子类型检查阶段的近似配置：近似 FOR_SUBTYPING 状态的捕获类型 */
    object SubtypeCapturedTypesApproximation : AbstractCapturedTypesAndILTApproximation(CaptureStatus.FOR_SUBTYPING)

    /** 带期望类型的顶层整型字面量类型近似配置 */
    class TopLevelIntegerLiteralTypeApproximationWithExpectedType(
        override val expectedTypeForIntegerLiteralType: CangJieTypeMarker?,
    ) : TypeApproximatorConfiguration() {
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
    }

    /** 内部类型近似配置：近似 FROM_EXPRESSION 状态的捕获类型及整型字面量 */
    object InternalTypesApproximation : AbstractCapturedTypesAndILTApproximation(CaptureStatus.FROM_EXPRESSION) {
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }

    /** 解析和推断完成后的最终近似配置 */
    object FinalApproximationAfterResolutionAndInference :
        AbstractCapturedTypesAndILTApproximation(CaptureStatus.FROM_EXPRESSION) {
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }

    /** 推断完成后对父类型方向的中间近似配置 */
    object IntermediateApproximationToSupertypeAfterCompletionInK2 :
        AbstractCapturedTypesAndILTApproximation(null) {
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true

        context(ctx: TypeSystemInferenceExtensionContext)
        override fun shouldApproximateCapturedType(type: CapturedTypeMarker): Boolean {
            return with(ctx) { type.captureStatus() } == CaptureStatus.FROM_EXPRESSION
        }
    }

    /** 推断完成后对类型实参的近似配置 */
    object TypeArgumentApproximationAfterCompletionInK2 : AbstractCapturedTypesAndILTApproximation(null) {
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }

    /** 前端到后端的类型近似配置 */
    object FrontendToBackendTypesApproximation : TypeApproximatorConfiguration() {
        override val approximateErrorTypes: Boolean get() = false
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }
}