/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.FixVariableConstraintPosition
import org.cangnova.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.cangnova.cangjie.type.model.*

/*
 * Functions from this context can not be moved to TypeSystemInferenceExtensionContext, because
 *   it's classic implementation, ClassicTypeSystemContext lays in :core:descriptors,
 *   but we need access classes from :compiler:resolution for this function implementation
 */
interface ConstraintSystemUtilContext {
    /**
     * DNN-related hack that softens slightly restrictions in
     * [org.cangnova.cangjie.resolve.calls.inference.components.ConstraintInjector.TypeCheckerStateForConstraintInjector.runIsSubtypeOf]
     */
    fun TypeVariableMarker.shouldBeFlexible(): Boolean
    fun TypeVariableMarker.hasOnlyInputTypesAttribute(): Boolean
    fun CangJieTypeMarker.unCapture(): CangJieTypeMarker
    fun TypeVariableMarker.isReified(): Boolean
    fun CangJieTypeMarker.refineType(): CangJieTypeMarker
    fun CangJieTypeMarker.isBuiltinFunctionType(): Boolean
    fun CangJieTypeMarker.extractBuiltinFunctionArgumentTypes(): List<CangJieTypeMarker>
    fun getBuiltinFunctionTypeConstructor(parametersNumber: Int): TypeConstructorMarker

    // PostponedArgumentInputTypesResolver
    fun createArgumentConstraintPosition(argument: PostponedAtomWithRevisableExpectedType): ConstraintPosition

    fun <T> createFixVariableConstraintPosition(variable: TypeVariableMarker, atom: T): FixVariableConstraintPosition<T>
    fun extractLambdaParameterTypesFromDeclaration(declaration: PostponedAtomWithRevisableExpectedType): List<CangJieTypeMarker?>?
    fun PostponedAtomWithRevisableExpectedType.isFunctionExpression(): Boolean
    fun PostponedAtomWithRevisableExpectedType.isFunctionExpressionWithReceiver(): Boolean
    fun PostponedAtomWithRevisableExpectedType.contextParameterCountOfFunctionExpression(): Int
    fun PostponedAtomWithRevisableExpectedType.isLambda(): Boolean

    /**
     * Only implemented in K2.
     */
    fun PostponedAtomWithRevisableExpectedType.isSuspend(): Boolean
    fun createTypeVariableForLambdaReturnType(): TypeVariableMarker
    fun createTypeVariableForLambdaParameterType(argument: PostponedAtomWithRevisableExpectedType, index: Int): TypeVariableMarker
    fun createTypeVariableForCallableReferenceReturnType(): TypeVariableMarker
    fun createTypeVariableForCallableReferenceParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker

    val isForcedAllowForkingInferenceSystem get() = false
}
