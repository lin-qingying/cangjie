/*
 * Copyright 2010-2026 cangjie contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package org.cangnova.cangjie.resolve.calls

import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.TypeCheckerState
import org.cangnova.cangjie.type.model.asRigidType
import org.cangnova.cangjie.type.model.getArgumentOrNull
import org.cangnova.cangjie.type.model.getType
import org.cangnova.cangjie.type.model.parametersCount
import org.cangnova.cangjie.type.model.supertypes
import org.cangnova.cangjie.type.model.typeConstructor
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.RigidTypeMarker
import org.cangnova.cangjie.type.model.SimpleTypeMarker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeSystemCommonSuperTypesContext

object CommonSuperTypeCalculator {
    context(c: TypeSystemCommonSuperTypesContext)
    fun commonSuperType(types: List<CangJieTypeMarker>): CangJieTypeMarker {
        require(types.isNotEmpty()) { "Empty collection for common super type" }
        types.singleOrNull()?.let { return it }

        val rigidTypes = types.mapNotNull { it.asRigidType() }
        if (rigidTypes.size != types.size) {
            return c.createErrorType("CST(${types.joinToString()})", delegatedType = null)
        }

        val typeCheckerState = c.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = true,
        )

        val uniqueTypes = uniquify(rigidTypes, typeCheckerState)
        if (uniqueTypes.size == 1) return uniqueTypes.single()

        val filteredTypes = filterStrictSupertypes(uniqueTypes, typeCheckerState)
        if (filteredTypes.size == 1) return filteredTypes.single()

        c.findCommonIntegerLiteralTypesSuperType(filteredTypes)?.let {
            return it
        }

        val commonConstructors = allCommonSuperTypeConstructors(filteredTypes, typeCheckerState)
        if (commonConstructors.isEmpty()) {
            return c.anyType()
        }

        val candidateTypes = commonConstructors.map { constructor ->
            superTypeWithGivenConstructor(filteredTypes, constructor, typeCheckerState)
        }

        val result = when (candidateTypes.size) {
            1 -> candidateTypes.single()
            else -> c.intersectTypes(candidateTypes)
        }

        return result
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun uniquify(
        types: List<RigidTypeMarker>,
        state: TypeCheckerState,
    ): List<RigidTypeMarker> {
        val uniqueTypes = ArrayList<RigidTypeMarker>(types.size)
        for (type in types) {
            if (uniqueTypes.none { existing -> AbstractTypeChecker.equalTypes(state, existing, type) }) {
                uniqueTypes += type
            }
        }
        return uniqueTypes
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun filterStrictSupertypes(
        types: List<RigidTypeMarker>,
        state: TypeCheckerState,
    ): List<RigidTypeMarker> {
        if (types.size <= 1) return types

        return types.filterNot { potentialSubtype ->
            types.any { candidateSupertype ->
                candidateSupertype !== potentialSubtype &&
                    AbstractTypeChecker.isSubtypeOf(state, potentialSubtype, candidateSupertype)
            }
        }
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun allCommonSuperTypeConstructors(
        types: List<RigidTypeMarker>,
        state: TypeCheckerState,
    ): List<TypeConstructorMarker> {
        val commonConstructors = LinkedHashSet(collectAllSupertypes(types.first(), state))
        for (type in types.drop(1)) {
            commonConstructors.retainAll(collectAllSupertypes(type, state))
        }

        return commonConstructors.filterNot { target ->
            commonConstructors.any { other ->
                other != target && other.supertypes().any { supertype ->
                    supertype.typeConstructor() == target
                }
            }
        }
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun collectAllSupertypes(
        type: RigidTypeMarker,
        state: TypeCheckerState,
    ): Set<TypeConstructorMarker> {
        val result = LinkedHashSet<TypeConstructorMarker>()
        state.anySupertype(
            type,
            predicate = {
                result += it.typeConstructor()
                false
            },
            supertypesPolicy = { TypeCheckerState.SupertypesPolicy.Direct },
        )
        return result
    }

    context(c: TypeSystemCommonSuperTypesContext)
    private fun superTypeWithGivenConstructor(
        types: List<RigidTypeMarker>,
        constructor: TypeConstructorMarker,
        state: TypeCheckerState,
    ): SimpleTypeMarker {
        if (constructor.parametersCount() == 0) {
            return c.createSimpleType(constructor, emptyList())
        }

        val correspondingSupertypes = types.flatMap { type ->
            AbstractTypeChecker.findCorrespondingSupertypes(state, type, constructor)
        }
        if (correspondingSupertypes.isEmpty()) {
            return c.createSimpleType(constructor, emptyList())
        }

        val arguments = buildList(constructor.parametersCount()) {
            for (index in 0 until constructor.parametersCount()) {
                val candidateArguments = correspondingSupertypes.mapNotNull { it.getArgumentOrNull(index) }
                val candidateTypes = candidateArguments.mapNotNull { it.getType() }

                val argumentType = when {
                    candidateTypes.isEmpty() -> c.anyType()
                    candidateTypes.size == 1 -> candidateTypes.single()
                    else -> commonSuperType(candidateTypes)
                }

                add(c.createTypeArgument(argumentType))
            }
        }

        return c.createSimpleType(constructor, arguments)
    }
}
