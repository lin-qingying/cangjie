/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.types

import org.cangnova.cangjie.type.model.*

/**
 * 类型近似过程的策略配置。
 *
 * 该配置集中描述不同调用场景允许近似哪些内部类型形态，例如柔性类型、错误类型、
 * 整型字面量类型、交叉类型以及匿名类型。类型近似器只读取这些开关，不在配置对象中
 * 保存近似过程的中间状态，因此各个配置实例可以作为稳定的单例策略在推断、声明暴露
 * 与前后端边界中复用。
 */
abstract class TypeApproximatorConfiguration {
    /**
     * 交叉类型在近似时的处理策略。
     */
    enum class IntersectionStrategy {
        /**
         * 保留交叉类型本身，适用于内部推断仍需要精确保留所有约束分支的场景。
         */
        ALLOWED,

        /**
         * 使用交叉类型的第一个组成类型，适用于调用方只接受单一代表类型的快速近似路径。
         */
        TO_FIRST,

        /**
         * 计算交叉类型各组成类型的公共父类型，适用于对外暴露或最终落盘前的稳定化处理。
         */
        TO_COMMON_SUPERTYPE,
    }

    /**
     * 是否统一近似所有柔性相关类型形态。
     *
     * 子类通常只需要覆盖该根开关，派生的柔性、动态与原始类型开关会保持同一策略。
     */
    protected abstract val approximateAllFlexible: Boolean

    /**
     * 是否近似普通柔性类型。
     */
    val approximateFlexible: Boolean get() = approximateAllFlexible

    /**
     * 是否近似动态类型。
     */
    val approximateDynamic: Boolean get() = approximateAllFlexible

    /**
     * 是否近似原始类型。
     */
    val approximateRawTypes: Boolean get() = approximateAllFlexible

    /**
     * 是否允许错误类型参与近似。
     *
     * 默认保留可近似能力，使内部恢复流程可以继续推进；公开声明配置会覆盖为保留错误形态。
     */
    open val approximateErrorTypes: Boolean get() = true

    /**
     * 是否近似整型字面量常量类型。
     */
    open val approximateIntegerLiteralConstantTypes: Boolean get() = false

    /**
     * 是否近似整型常量运算结果类型。
     */
    open val approximateIntegerConstantOperatorTypes: Boolean get() = false

    /**
     * 整型字面量类型近似时可参考的期望类型。
     *
     * 为 `null` 时，近似器只能使用字面量自身的候选类型集合或默认目标类型。
     */
    open val expectedTypeForIntegerLiteralType: CangJieTypeMarker? get() = null

    /**
     * 是否近似 definitely-not-null 类型。
     */
    open val approximateDefinitelyNotNullTypes: Boolean get() = false

    /**
     * 交叉类型在当前策略下的近似方式。
     */
    open val intersectionStrategy: IntersectionStrategy get() = IntersectionStrategy.TO_COMMON_SUPERTYPE

    /**
     * 是否在逆变位置近似交叉类型。
     */
    open val approximateIntersectionTypesInContravariantPositions get() = false

    /**
     * K2 完成近似后是否把原始类型转换为非原始版本。
     */
    open val convertToNonRawVersionAfterApproximationInK2 get() = false

    /**
     * 是否近似匿名类型。
     *
     * 公开 API 暴露场景会按是否需要保留匿名结构分别选择不同配置对象。
     */
    open val approximateAnonymous get() = false

    /**
     * 判断基于类型变量构造器的类型是否需要近似。
     *
     * @param marker 待判断的类型变量构造器标记。
     * @return `true` 表示当前策略允许把该类型变量相关类型近似为稳定类型。
     */
    internal open fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker): Boolean = true

    /**
     * 局部声明使用的近似配置。
     *
     * 局部声明仍处在推断内部边界内，因此保留柔性类型和错误类型，允许交叉类型存在，
     * 同时对整型字面量与逆变交叉类型执行必要稳定化。
     */
    object LocalDeclaration : TypeApproximatorConfiguration() {
        /**
         * 局部声明不统一近似柔性相关类型。
         */
        override val approximateAllFlexible: Boolean get() = false

        /**
         * 局部声明允许保留交叉类型。
         */
        override val intersectionStrategy: IntersectionStrategy get() = IntersectionStrategy.ALLOWED

        /**
         * 局部声明保留错误类型，避免隐藏前序诊断信息。
         */
        override val approximateErrorTypes: Boolean get() = false

        /**
         * 局部声明会稳定化整型字面量常量类型。
         */
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true

        /**
         * 局部声明允许在逆变位置近似交叉类型。
         */
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true

        /**
         * 局部声明保留类型变量相关类型，交由后续推断阶段继续处理。
         */
        override fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker): Boolean = false
    }

    /**
     * 公开声明使用的近似配置基类。
     *
     * 公开声明需要把内部推断类型稳定化为可暴露的签名类型，但仍保留错误类型和类型变量
     * 信息，以便诊断与符号恢复阶段能够看到真实问题来源。
     *
     * @property approximateAnonymous 是否把匿名类型近似为可公开暴露的上界或父类型。
     */
    abstract class PublicDeclaration(
        /**
         * 是否近似匿名类型。
         */
        override val approximateAnonymous: Boolean,
    ) : TypeApproximatorConfiguration() {
        /**
         * 公开声明不统一近似柔性相关类型。
         */
        override val approximateAllFlexible: Boolean get() = false

        /**
         * 公开声明保留错误类型，避免对外签名掩盖已有诊断。
         */
        override val approximateErrorTypes: Boolean get() = false

        /**
         * 公开声明会稳定化整型字面量常量类型。
         */
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true

        /**
         * 公开声明允许在逆变位置近似交叉类型。
         */
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true

        /**
         * 公开声明保留类型变量相关类型，避免提前把泛型约束折叠为错误的公开签名。
         */
        override fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker): Boolean = false

        /**
         * 保留匿名类型的公开声明配置。
         */
        object SaveAnonymousTypes : PublicDeclaration(approximateAnonymous = false)

        /**
         * 将匿名类型近似为可公开表达类型的公开声明配置。
         */
        object ApproximateAnonymousTypes : PublicDeclaration(approximateAnonymous = true)
    }

    /**
     * 仓颉无 CapturedType，此配置族仅用于近似 ILT（整型字面量类型）和交叉类型。
     */
    sealed class AbstractILTApproximation : TypeApproximatorConfiguration() {
        /**
         * 整型字面量近似配置不统一近似柔性相关类型。
         */
        override val approximateAllFlexible: Boolean get() = false

        /**
         * 整型字面量近似配置保留错误类型。
         */
        override val approximateErrorTypes: Boolean get() = false

        /**
         * 整型字面量近似配置允许交叉类型继续存在。
         */
        override val intersectionStrategy: IntersectionStrategy get() = IntersectionStrategy.ALLOWED

        /**
         * 整型字面量近似配置保留类型变量相关类型。
         */
        override fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker): Boolean = false
    }

    /**
     * 保留空对象以兼容约束合并流程中可能残留的配置引用。
     * 仓颉不再使用 CapturedType，此配置现在不做任何捕获类型近似。
     */
    object IncorporationConfiguration : AbstractILTApproximation()

    /**
     * 子类型方向的捕获类型近似兼容配置。
     *
     * 仓颉当前没有 CapturedType，该对象保留为调用点可识别的策略名，实际行为继承
     * [AbstractILTApproximation] 的整型字面量与交叉类型处理。
     */
    object SubtypeCapturedTypesApproximation : AbstractILTApproximation()

    /**
     * 顶层整型字面量类型近似配置。
     *
     * 该配置在表达式顶层存在期望类型时使用，使整型字面量和整型常量运算可以优先向
     * 期望类型稳定化。
     *
     * @property expectedTypeForIntegerLiteralType 整型字面量近似可参考的期望类型。
     */
    class TopLevelIntegerLiteralTypeApproximationWithExpectedType(
        /**
         * 整型字面量近似可参考的期望类型。
         */
        override val expectedTypeForIntegerLiteralType: CangJieTypeMarker?,
    ) : TypeApproximatorConfiguration() {
        /**
         * 顶层整型字面量近似不统一近似柔性相关类型。
         */
        override val approximateAllFlexible: Boolean get() = false

        /**
         * 顶层整型字面量近似会稳定化字面量常量类型。
         */
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true

        /**
         * 顶层整型字面量近似会稳定化整型常量运算结果类型。
         */
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
    }

    /**
     * 内部类型稳定化使用的近似配置。
     */
    object InternalTypesApproximation : AbstractILTApproximation() {
        /**
         * 内部类型稳定化会近似整型字面量常量类型。
         */
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true

        /**
         * 内部类型稳定化会近似整型常量运算结果类型。
         */
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true

        /**
         * 内部类型稳定化允许在逆变位置近似交叉类型。
         */
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }

    /**
     * 解析与推断完成后的最终近似配置。
     */
    object FinalApproximationAfterResolutionAndInference : AbstractILTApproximation() {
        /**
         * 最终近似会稳定化整型字面量常量类型。
         */
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true

        /**
         * 最终近似会稳定化整型常量运算结果类型。
         */
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true

        /**
         * 最终近似允许在逆变位置近似交叉类型。
         */
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }

    /**
     * K2 完成阶段后向父类型执行中间近似的配置。
     */
    object IntermediateApproximationToSupertypeAfterCompletionInK2 : AbstractILTApproximation() {
        /**
         * 中间父类型近似会稳定化整型字面量常量类型。
         */
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true

        /**
         * 中间父类型近似会稳定化整型常量运算结果类型。
         */
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true

        /**
         * 中间父类型近似允许在逆变位置近似交叉类型。
         */
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }

    /**
     * K2 完成阶段后用于类型实参的近似配置。
     */
    object TypeArgumentApproximationAfterCompletionInK2 : AbstractILTApproximation() {
        /**
         * 类型实参近似会稳定化整型字面量常量类型。
         */
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true

        /**
         * 类型实参近似会稳定化整型常量运算结果类型。
         */
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true

        /**
         * 类型实参近似允许在逆变位置近似交叉类型。
         */
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }

    /**
     * 前端类型交付给后端前使用的近似配置。
     *
     * 该配置把后端不应直接消费的内部类型形态稳定化，同时保留错误类型供诊断链路识别。
     */
    object FrontendToBackendTypesApproximation : TypeApproximatorConfiguration() {
        /**
         * 前后端边界不统一近似柔性相关类型。
         */
        override val approximateAllFlexible: Boolean get() = false

        /**
         * 前后端边界保留错误类型。
         */
        override val approximateErrorTypes: Boolean get() = false

        /**
         * 前后端边界会稳定化整型字面量常量类型。
         */
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true

        /**
         * 前后端边界会稳定化整型常量运算结果类型。
         */
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true

        /**
         * 前后端边界允许在逆变位置近似交叉类型。
         */
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }
}
