/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.UnstableSystemMergeMode
import org.cangnova.cangjie.resolve.calls.inference.model.VariableWithConstraints
import org.cangnova.cangjie.type.model.*

interface PostponedArgumentsAnalyzerContext : TypeSystemInferenceExtensionContext {
    val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>

    fun buildCurrentSubstitutor(additionalBinding: Pair<TypeConstructorMarker, CangJieTypeMarker>?): TypeSubstitutorMarker =
        buildCurrentSubstitutor(if (additionalBinding == null) emptyMap() else mapOf(additionalBinding))

    fun buildCurrentSubstitutor(additionalBindings: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker
    fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker
    fun bindingStubsForPostponedVariables(): Map<TypeVariableMarker, StubTypeMarker>

    // type can be proper if it not contains not fixed type variables
    fun canBeProper(type: CangJieTypeMarker): Boolean

    fun hasUpperOrEqualUnitConstraint(type: CangJieTypeMarker): Boolean

    fun removePostponedTypeVariablesFromConstraints(postponedTypeVariables: Set<TypeConstructorMarker>)

    // mutable operations
    fun addOtherSystem(otherSystem: ConstraintStorage)

    @UnstableSystemMergeMode
    fun mergeOtherSystem(otherSystem: ConstraintStorage)

    fun getBuilder(): ConstraintSystemBuilder
    fun resolveForkPointsConstraints()
}
