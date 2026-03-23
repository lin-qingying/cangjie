/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.type.model.*

class TrivialConstraintTypeInferenceOracle private constructor(context: TypeSystemInferenceExtensionContext) :
    TypeSystemInferenceExtensionContext by context {
    // This constructor is used for injection only in old FE
    constructor(context: TypeSystemInferenceExtensionContextDelegate) : this(context as TypeSystemInferenceExtensionContext)

    // The idea is to add knowledge that constraint `Nothing(?) <: T` is quite useless and
    // it's totally fine to go and resolve postponed argument without fixation T to Nothing(?).
    // In other words, constraint `Nothing(?) <: T` is *not* proper
    fun isNotInterestingConstraint(constraint: Constraint): Boolean {
        return constraint.kind == ConstraintKind.LOWER && constraint.type.typeConstructor().isNothingConstructor()
    }

    // This function controls the choice between sub and super result type
    // Even that Nothing(?) is the most specific type for subtype, it doesn't bring valuable information to the user,
    // therefore it is discriminated in favor of supertype
    fun isSuitableResultedType(
        resultType: CangJieTypeMarker
    ): Boolean {
        return !resultType.typeConstructor().isNothingConstructor() && !resultType.isError() && !resultType.isSpecial()
    }

    // It's possible to generate Nothing-like constraints inside incorporation mechanism:
    // For instance, when two type variables are in subtyping relation `T <: K`, after incorporation
    // there will be constraint `approximation(out K) <: K` => `Nothing <: K`, which is innocent
    // but can change result of the constraint system.
    // Therefore, here we avoid adding such trivial constraints to have stable constraint system
    fun isGeneratedConstraintTrivial(
        baseConstraint: Constraint,
        otherConstraint: Constraint,
        generatedConstraintType: CangJieTypeMarker,
        isSubtype: Boolean
    ): Boolean {
        if (isSubtype && generatedConstraintType.isNothing()) return true
        if (!isSubtype && generatedConstraintType.typeConstructor().isAnyConstructor()) return true

        // If types from constraints that will be used to generate new constraint already contains `Nothing(?)`,
        // then we can't decide that resulting constraint will be useless
        if (baseConstraint.type.contains { it.isNothingOrNullableNothing() }) return false
        if (otherConstraint.type.contains { it.isNothingOrNullableNothing() }) return false

        // It's important to preserve constraints with nullable Nothing: `Nothing? <: T` (see implicitNothingConstraintFromReturn.kt test)
        if (generatedConstraintType.containsOnlyNothing()) return true

        return false
    }


    private fun CangJieTypeMarker.isNothingOrNullableNothing(): Boolean =
        typeConstructor().isNothingConstructor()


    private fun CangJieTypeMarker.containsOnlyNothing(): Boolean =
        contains {
            it.isNothing()
        }


    companion object {
        fun create(context: TypeSystemInferenceExtensionContext) = TrivialConstraintTypeInferenceOracle(context)
    }
}
