/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.types

import org.cangnova.cangjie.type.model.*

abstract class TypeApproximatorConfiguration {
    enum class IntersectionStrategy {
        ALLOWED,
        TO_FIRST,
        TO_COMMON_SUPERTYPE,
    }

    protected abstract val approximateAllFlexible: Boolean

    val approximateFlexible: Boolean get() = approximateAllFlexible
    val approximateDynamic: Boolean get() = approximateAllFlexible
    val approximateRawTypes: Boolean get() = approximateAllFlexible

    open val approximateErrorTypes: Boolean get() = true

    open val approximateIntegerLiteralConstantTypes: Boolean get() = false
    open val approximateIntegerConstantOperatorTypes: Boolean get() = false
    open val expectedTypeForIntegerLiteralType: CangJieTypeMarker? get() = null

    open val approximateDefinitelyNotNullTypes: Boolean get() = false
    open val intersectionStrategy: IntersectionStrategy get() = IntersectionStrategy.TO_COMMON_SUPERTYPE
    open val approximateIntersectionTypesInContravariantPositions get() = false
    open val approximateLocalTypes get() = false

    open fun shouldApproximateLocalType(ctx: TypeSystemInferenceExtensionContext, type: CangJieTypeMarker): Boolean =
        true

    open val convertToNonRawVersionAfterApproximationInK2 get() = false

    open val approximateAnonymous get() = false

    internal open fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker): Boolean = true

    object LocalDeclaration : TypeApproximatorConfiguration() {
        override val approximateAllFlexible: Boolean get() = false
        override val intersectionStrategy: IntersectionStrategy get() = IntersectionStrategy.ALLOWED
        override val approximateErrorTypes: Boolean get() = false
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true

        override fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker): Boolean = false
    }

    abstract class PublicDeclaration(
        override val approximateLocalTypes: Boolean,
        override val approximateAnonymous: Boolean,
    ) : TypeApproximatorConfiguration() {
        override val approximateAllFlexible: Boolean get() = false
        override val approximateErrorTypes: Boolean get() = false
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true

        override fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker): Boolean = false

        object SaveAnonymousTypes : PublicDeclaration(approximateLocalTypes = false, approximateAnonymous = false)
        object ApproximateAnonymousTypes : PublicDeclaration(approximateLocalTypes = false, approximateAnonymous = true)
        object ApproximateLocalAndAnonymousTypes : PublicDeclaration(approximateLocalTypes = true, approximateAnonymous = true)
    }

    /**
     * 仓颉无 CapturedType，此配置族仅用于近似 ILT（整型字面量类型）和交叉类型。
     */
    sealed class AbstractILTApproximation : TypeApproximatorConfiguration() {
        override val approximateAllFlexible: Boolean get() = false
        override val approximateErrorTypes: Boolean get() = false
        override val intersectionStrategy: IntersectionStrategy get() = IntersectionStrategy.ALLOWED
        override fun shouldApproximateTypeVariableBasedType(marker: TypeVariableTypeConstructorMarker): Boolean = false
    }

    /**
     * 保留空对象以兼容约束合并流程中可能残留的配置引用。
     * 仓颉不再使用 CapturedType，此配置现在不做任何捕获类型近似。
     */
    object IncorporationConfiguration : AbstractILTApproximation()

    object SubtypeCapturedTypesApproximation : AbstractILTApproximation()

    class TopLevelIntegerLiteralTypeApproximationWithExpectedType(
        override val expectedTypeForIntegerLiteralType: CangJieTypeMarker?,
    ) : TypeApproximatorConfiguration() {
        override val approximateAllFlexible: Boolean get() = false
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
    }

    object InternalTypesApproximation : AbstractILTApproximation() {
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }

    object FinalApproximationAfterResolutionAndInference : AbstractILTApproximation() {
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }

    object IntermediateApproximationToSupertypeAfterCompletionInK2 : AbstractILTApproximation() {
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }

    object TypeArgumentApproximationAfterCompletionInK2 : AbstractILTApproximation() {
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }

    object FrontendToBackendTypesApproximation : TypeApproximatorConfiguration() {
        override val approximateAllFlexible: Boolean get() = false
        override val approximateErrorTypes: Boolean get() = false
        override val approximateIntegerLiteralConstantTypes: Boolean get() = true
        override val approximateIntegerConstantOperatorTypes: Boolean get() = true
        override val approximateIntersectionTypesInContravariantPositions: Boolean get() = true
    }
}
