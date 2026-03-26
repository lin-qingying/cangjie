/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.type.AbstractTypePreparator
import org.cangnova.cangjie.type.AbstractTypeRefiner
import org.cangnova.cangjie.type.TypeCheckerState
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.*

abstract class TypeCheckerStateForConstraintSystem(
    val extensionTypeContext: TypeSystemInferenceExtensionContext,
    cangjieTypePreparator: AbstractTypePreparator,
    cangjieTypeRefiner: AbstractTypeRefiner
) : TypeCheckerState(
    isErrorTypeEqualsToAnything = true,
    isStubTypeEqualsToAnything = true,
    allowedTypeVariable = false,
    typeSystemContext = extensionTypeContext,
    cangjieTypePreparator = cangjieTypePreparator,
    cangjieTypeRefiner = cangjieTypeRefiner,
) {
    abstract val languageVersionSettings: LanguageVersionSettings

    abstract fun isMyTypeVariable(type: RigidTypeMarker): Boolean

    // super and sub type isSingleClassifierType
    abstract fun addUpperConstraint(typeVariable: TypeConstructorMarker, superType: CangJieTypeMarker, isNoInfer: Boolean)

    abstract fun addLowerConstraint(
        typeVariable: TypeConstructorMarker,
        subType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean = false,
        isNoInfer: Boolean,
    )

    abstract fun addEqualityConstraint(typeVariable: TypeConstructorMarker, type: CangJieTypeMarker)

    /**
     * todo: possible we should override this method, because otherwise OR in subtyping transformed to AND in constraint system
     * Now we cannot do this, because sometimes we have proper intersection type as lower type and if we first supertype,
     * then we can get wrong result.
     * override val sameConstructorPolicy get() = SeveralSupertypesWithSameConstructorPolicy.TAKE_FIRST_FOR_SUBTYPING
     */
    final override fun addSubtypeConstraint(
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
    ): Boolean? {
        return internalAddSubtypeConstraint(subType, superType, isNoInfer = false)
    }

    private fun internalAddSubtypeConstraint(
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        isNoInfer: Boolean,
    ): Boolean? {
        assertInputTypes(subType, superType)

        var answer: Boolean? = null

        if (superType.anyBound(this::isMyTypeVariable)) {
            answer = simplifyLowerConstraint(superType, subType, isNoInfer)
        }

        return if (subType.anyBound(this::isMyTypeVariable)) {
            simplifyUpperConstraint(subType, superType, isNoInfer) && (answer ?: true)
        } else {
            simplifyConstraintForPossibleIntersectionSubType(subType, superType, isNoInfer) ?: answer
        }
    }

    /**
     * Foo <: T -- leave as is
     *
     * T?
     *
     * Foo <: T? -- Foo & Any <: T
     * Foo? <: T? -- Foo? & Any <: T -- Foo & Any <: T
     * (Foo..Bar) <: T? -- (Foo..Bar) & Any <: T
     *
     * T!
     *
     * Foo <: T! --
     * assert T! == (T..T?)
     *  Foo <: T?
     *  Foo <: T (optional constraint, needs to preserve nullability)
     * =>
     *  Foo & Any <: T
     *  Foo <: T
     * =>
     *  (Foo & Any .. Foo) <: T -- (Foo!! .. Foo) <: T
     *
     * => Foo <: T! -- (Foo!! .. Foo) <: T
     *
     * Foo? <: T! -- Foo? <: T
     *
     *
     * (Foo..Bar) <: T! --
     * assert T! == (T..T?)
     *  (Foo..Bar) <: (T..T?)
     * =>
     *  Foo <: T?
     *  Bar <: T (optional constraint, needs to preserve nullability)
     * =>
     *  (Foo & Any .. Bar) <: T -- (Foo!! .. Bar) <: T
     *
     * => (Foo..Bar) <: T! -- (Foo!! .. Bar) <: T
     *
     * T & Any
     *
     * Foo? <: T & Any => ERROR (for K2 only)
     *
     * Foo..Bar? <: T & Any => Foo..Bar? <: T
     * Foo <: T & Any  => Foo <: T
     */
    private fun simplifyLowerConstraint(
        typeVariable: CangJieTypeMarker,
        subType: CangJieTypeMarker,
        isNoInfer: Boolean,
        isFromNullabilityConstraint: Boolean = false,
    ): Boolean = with(extensionTypeContext) {
        val rigidTypeVariable = typeVariable.asRigidType() ?: return false
        if (!isMyTypeVariable(rigidTypeVariable)) return true

        when {
            subType.isError() -> return false
            subType.isUninferredParameter() -> return true
        }

        addLowerConstraint(rigidTypeVariable.typeConstructor(), subType, isFromNullabilityConstraint, isNoInfer)

        return true
    }

    /**
     * T! <: Foo <=> T <: Foo!
     * T? <: Foo <=> T <: Foo && Nothing? <: Foo
     * T  <: Foo -- leave as is
     * T & Any <: Foo <=> T <: Foo?
     */
    private fun simplifyUpperConstraint(
        typeVariable: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        isNoInfer: Boolean
    ): Boolean = with(extensionTypeContext) {
        val rigidTypeVariable = typeVariable.asRigidType() ?: return false
        if (!isMyTypeVariable(rigidTypeVariable)) return true

        addUpperConstraint(rigidTypeVariable.typeConstructor(), superType, isNoInfer)
        return true
    }

    private fun simplifyConstraintForPossibleIntersectionSubType(subType: CangJieTypeMarker, superType: CangJieTypeMarker, isNoInfer: Boolean): Boolean? =
        with(extensionTypeContext) {
            @Suppress("NAME_SHADOWING")
            val subType = subType.asRigidType() ?: return null

            if (!subType.typeConstructor().isIntersection()) return null

            val subIntersectionTypes = subType.typeConstructor().supertypes().mapNotNull { it.asRigidType() }

            val typeVariables = subIntersectionTypes.filter(::isMyTypeVariable).takeIf { it.isNotEmpty() } ?: return null
            val notTypeVariables = subIntersectionTypes.filterNot(::isMyTypeVariable)

            // todo: may be we can do better then that.
            if (notTypeVariables.isNotEmpty() &&
                AbstractTypeChecker.isSubtypeOf(
                    this@TypeCheckerStateForConstraintSystem,
                    intersectTypes(notTypeVariables),
                    superType
                )
            ) {
                return true
            }

            return typeVariables.all { simplifyUpperConstraint(it, superType, isNoInfer) }
        }

    private fun isSubtypeOfByTypeChecker(subType: CangJieTypeMarker, superType: CangJieTypeMarker) =
        AbstractTypeChecker.isSubtypeOf(this as TypeCheckerState, subType, superType)

    private fun assertInputTypes(subType: CangJieTypeMarker, superType: CangJieTypeMarker): Unit = with(typeSystemContext) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        fun correctSubType(subType: RigidTypeMarker) =
            subType.isSingleClassifierType() || subType.typeConstructor()
                .isIntersection() || isMyTypeVariable(subType) || subType.isError() || subType.isIntegerLiteralType()

        fun correctSuperType(superType: RigidTypeMarker) =
            superType.isSingleClassifierType() || superType.typeConstructor()
                .isIntersection() || isMyTypeVariable(superType) || superType.isError() || superType.isIntegerLiteralType()

        assert(subType.bothBounds(::correctSubType)) {
            "Not singleClassifierType and not intersection subType: $subType"
        }
        assert(superType.bothBounds(::correctSuperType)) {
            "Not singleClassifierType superType: $superType"
        }
    }

    private inline fun CangJieTypeMarker.bothBounds(f: (RigidTypeMarker) -> Boolean): Boolean {
        val rigidType = with(extensionTypeContext) { asRigidType() } ?: return false
        return f(rigidType)
    }

    private inline fun CangJieTypeMarker.anyBound(f: (RigidTypeMarker) -> Boolean): Boolean {
        val rigidType = with(extensionTypeContext) { asRigidType() } ?: return false
        return f(rigidType)
    }
}
