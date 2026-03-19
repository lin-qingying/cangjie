package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.types.ConeArrayType
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFlexibleType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType

internal class CfirConstraintIncorporator(
    private val storage: CfirConstraintStorage,
) {
    fun processSubtypeConstraint(
        subType: ConeCangjieType,
        superType: ConeCangjieType,
        onUpperBound: (CfirTypeVariable, ConeCangjieType) -> Unit,
        onLowerBound: (CfirTypeVariable, ConeCangjieType) -> Unit,
    ) {
        val subVariable = storage.findTypeVariable(subType)
        val superVariable = storage.findTypeVariable(superType)

        when {
            subVariable != null && superVariable != null -> {
                addUpperBound(subVariable, superType, onUpperBound, onLowerBound)
                addLowerBound(superVariable, subType, onUpperBound, onLowerBound)
            }

            subVariable != null -> addUpperBound(subVariable, superType, onUpperBound, onLowerBound)
            superVariable != null -> addLowerBound(superVariable, subType, onUpperBound, onLowerBound)
            else -> decomposeStructuralSubtypeConstraint(subType, superType, onUpperBound, onLowerBound)
        }
    }

    private fun decomposeStructuralSubtypeConstraint(
        subType: ConeCangjieType,
        superType: ConeCangjieType,
        onUpperBound: (CfirTypeVariable, ConeCangjieType) -> Unit,
        onLowerBound: (CfirTypeVariable, ConeCangjieType) -> Unit,
    ) {
        val normalizedSub = normalizeFlexibleTypeForSubtype(subType, useLowerBound = true)
        val normalizedSuper = normalizeFlexibleTypeForSubtype(superType, useLowerBound = false)

        if (normalizedSub is ConeUnionType) {
            normalizedSub.unionTypes.forEach { member ->
                processSubtypeConstraint(member, normalizedSuper, onUpperBound, onLowerBound)
            }
            return
        }

        if (normalizedSuper is ConeIntersectionType) {
            normalizedSuper.intersectedTypes.forEach { member ->
                processSubtypeConstraint(normalizedSub, member, onUpperBound, onLowerBound)
            }
            return
        }

        when {
            normalizedSub is ConeClassLikeType && normalizedSuper is ConeClassLikeType &&
                normalizedSub.classId == normalizedSuper.classId &&
                normalizedSub.typeArguments.size == normalizedSuper.typeArguments.size -> {
                normalizedSub.typeArguments.zip(normalizedSuper.typeArguments).forEach { (left, right) ->
                    processSubtypeConstraint(left, right, onUpperBound, onLowerBound)
                    processSubtypeConstraint(right, left, onUpperBound, onLowerBound)
                }
            }

            normalizedSub is ConeStructType && normalizedSuper is ConeStructType &&
                normalizedSub.classId == normalizedSuper.classId &&
                normalizedSub.typeArguments.size == normalizedSuper.typeArguments.size -> {
                normalizedSub.typeArguments.zip(normalizedSuper.typeArguments).forEach { (left, right) ->
                    processSubtypeConstraint(left, right, onUpperBound, onLowerBound)
                    processSubtypeConstraint(right, left, onUpperBound, onLowerBound)
                }
            }

            normalizedSub is ConeEnumType && normalizedSuper is ConeEnumType &&
                normalizedSub.classId == normalizedSuper.classId &&
                normalizedSub.typeArguments.size == normalizedSuper.typeArguments.size -> {
                normalizedSub.typeArguments.zip(normalizedSuper.typeArguments).forEach { (left, right) ->
                    processSubtypeConstraint(left, right, onUpperBound, onLowerBound)
                    processSubtypeConstraint(right, left, onUpperBound, onLowerBound)
                }
            }

            normalizedSub is ConeFuncType && normalizedSuper is ConeFuncType &&
                normalizedSub.parameterTypes.size == normalizedSuper.parameterTypes.size -> {
                normalizedSub.parameterTypes.zip(normalizedSuper.parameterTypes).forEach { (subParam, superParam) ->
                    processSubtypeConstraint(superParam, subParam, onUpperBound, onLowerBound)
                }
                processSubtypeConstraint(normalizedSub.returnType, normalizedSuper.returnType, onUpperBound, onLowerBound)
            }

            normalizedSub is ConeTupleType && normalizedSuper is ConeTupleType &&
                normalizedSub.elementTypes.size == normalizedSuper.elementTypes.size -> {
                normalizedSub.elementTypes.zip(normalizedSuper.elementTypes).forEach { (left, right) ->
                    processSubtypeConstraint(left, right, onUpperBound, onLowerBound)
                }
            }

            normalizedSub is ConeArrayType && normalizedSuper is ConeArrayType &&
                normalizedSub.dims == normalizedSuper.dims -> {
                processSubtypeConstraint(normalizedSub.elementType, normalizedSuper.elementType, onUpperBound, onLowerBound)
                processSubtypeConstraint(normalizedSuper.elementType, normalizedSub.elementType, onUpperBound, onLowerBound)
            }

            normalizedSub is ConeVArrayType && normalizedSuper is ConeVArrayType &&
                normalizedSub.size == normalizedSuper.size -> {
                processSubtypeConstraint(normalizedSub.elementType, normalizedSuper.elementType, onUpperBound, onLowerBound)
                processSubtypeConstraint(normalizedSuper.elementType, normalizedSub.elementType, onUpperBound, onLowerBound)
            }
        }
    }

    private fun normalizeFlexibleTypeForSubtype(type: ConeCangjieType, useLowerBound: Boolean): ConeCangjieType {
        if (type !is ConeFlexibleType) return type
        return if (useLowerBound) type.lowerBound else type.upperBound
    }

    private fun addUpperBound(
        variable: CfirTypeVariable,
        type: ConeCangjieType,
        onUpperBound: (CfirTypeVariable, ConeCangjieType) -> Unit,
        onLowerBound: (CfirTypeVariable, ConeCangjieType) -> Unit,
    ) {
        if (!storage.addUpperBound(variable, type)) return
        onUpperBound(variable, type)
        propagateBoundThroughOtherVariables(variable.name, type, onUpperBound, onLowerBound)
    }

    private fun addLowerBound(
        variable: CfirTypeVariable,
        type: ConeCangjieType,
        onUpperBound: (CfirTypeVariable, ConeCangjieType) -> Unit,
        onLowerBound: (CfirTypeVariable, ConeCangjieType) -> Unit,
    ) {
        if (!storage.addLowerBound(variable, type)) return
        onLowerBound(variable, type)
        propagateBoundThroughOtherVariables(variable.name, type, onUpperBound, onLowerBound)
    }

    private fun propagateBoundThroughOtherVariables(
        sourceVariableName: String,
        sourceBound: ConeCangjieType,
        onUpperBound: (CfirTypeVariable, ConeCangjieType) -> Unit,
        onLowerBound: (CfirTypeVariable, ConeCangjieType) -> Unit,
    ) {
        if (sourceBound is ConeTypeParameterType) return

        val dependencyOwners = storage.getVariablesDependingOn(sourceVariableName)
        val targetVariables = if (dependencyOwners.isEmpty()) storage.typeVariables else dependencyOwners

        targetVariables.forEach { variable ->
            if (variable.name == sourceVariableName || variable.isFixed) return@forEach

            variable.upperBounds.toList().forEach { upper ->
                if (!upper.containsTypeVariableName(sourceVariableName)) return@forEach
                addUpperBound(
                    variable,
                    upper.substituteTypeVariableName(sourceVariableName, sourceBound),
                    onUpperBound,
                    onLowerBound,
                )
            }

            variable.lowerBounds.toList().forEach { lower ->
                if (!lower.containsTypeVariableName(sourceVariableName)) return@forEach
                addLowerBound(
                    variable,
                    lower.substituteTypeVariableName(sourceVariableName, sourceBound),
                    onUpperBound,
                    onLowerBound,
                )
            }
        }
    }
}
