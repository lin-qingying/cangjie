/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.inference.ForkPointBranchDescription
import org.cangnova.cangjie.resolve.calls.inference.ForkPointData
import org.cangnova.cangjie.resolve.calls.inference.extractAllContainingTypeVariables
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind.*
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.TypeCheckerState
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.types.AbstractTypeApproximator
import org.cangnova.cangjie.utils.SmartList
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.popLast
import kotlin.math.max

class ConstraintInjector(
    val constraintIncorporator: ConstraintIncorporator,
    val typeApproximator: AbstractTypeApproximator,

    private val languageVersionSettings: LanguageVersionSettings,
    inferenceLoggerParameter: InferenceLogger? = null,
) {
    private val inferenceLogger = inferenceLoggerParameter.takeIf { it !is InferenceLogger.Dummy }

    private val ALLOWED_DEPTH_DELTA_FOR_INCORPORATION = 1

    private val useMaxTypeDepthFromInitialConstraints: Boolean = true

    interface Context : TypeSystemInferenceExtensionContext, ConstraintSystemMarker {
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

        var maxTypeDepthFromInitialConstraints: Int
        val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints>
        val fixedTypeVariables: MutableMap<TypeConstructorMarker, CangJieTypeMarker>
        val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>

        /**
         * @see org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage.typeVariableDependencies
         */
        val typeVariableDependencies: Map<TypeConstructorMarker, Set<TypeConstructorMarker>>

        val atCompletionState: Boolean

        fun addInitialConstraint(initialConstraint: InitialConstraint)
        fun addError(error: ConstraintSystemError)

        fun resolveForkPointsConstraints()

        fun onNewConstraintOrForkPoint()

        fun recordTypeVariableReferenceInConstraint(
            constraintOwner: TypeConstructorMarker,
            referencedVariable: TypeConstructorMarker,
        )
    }

    context(c: Context)
    fun addInitialSubtypeConstraint(
        lowerType: CangJieTypeMarker,
        upperType: CangJieTypeMarker,
        position: ConstraintPosition,
    ) {
        val initialConstraint = InitialConstraint(lowerType, upperType, UPPER, position).also { c.addInitialConstraint(it) }
        inferenceLogger?.logInitial(initialConstraint, c)

        updateAllowedTypeDepth(lowerType)
        updateAllowedTypeDepth(upperType)

        inferenceLogger?.withOrigin(initialConstraint) {
            with(TypeCheckerStateForConstraintInjector(c, IncorporationConstraintPosition(initialConstraint))) {
                addSubTypeConstraintAndIncorporateIt(lowerType, upperType)
            }
        } ?: with(TypeCheckerStateForConstraintInjector(c, IncorporationConstraintPosition(initialConstraint))) {
            addSubTypeConstraintAndIncorporateIt(lowerType, upperType)
        }
    }

    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)
    private fun addInitialEqualityConstraintThroughSubtyping(a: CangJieTypeMarker, b: CangJieTypeMarker) {
        updateAllowedTypeDepth(a)
        updateAllowedTypeDepth(b)
        addSubTypeConstraintAndIncorporateIt(a, b)
        addSubTypeConstraintAndIncorporateIt(b, a)
    }

    context(c: Context)
    fun addInitialEqualityConstraint(a: CangJieTypeMarker, b: CangJieTypeMarker, position: ConstraintPosition) {
        val (typeVariable, equalType) = when {
            a.typeConstructor() is TypeVariableTypeConstructorMarker -> a to b
            b.typeConstructor() is TypeVariableTypeConstructorMarker -> b to a
            else -> return
        }
        val initialConstraint = InitialConstraint(typeVariable, equalType, EQUALITY, position).also { c.addInitialConstraint(it) }
        inferenceLogger?.logInitial(initialConstraint, c)

        with(TypeCheckerStateForConstraintInjector(c, IncorporationConstraintPosition(initialConstraint))) {
            if (!typeVariable.isRigidType()) {
                inferenceLogger?.withOrigin(initialConstraint) {
                    addInitialEqualityConstraintThroughSubtyping(typeVariable, equalType)
                } ?: addInitialEqualityConstraintThroughSubtyping(typeVariable, equalType)
                return
            }

            updateAllowedTypeDepth(equalType)
            inferenceLogger?.withOrigin(initialConstraint) {
                addEqualityConstraintAndIncorporateIt(typeVariable, equalType)
            } ?: addEqualityConstraintAndIncorporateIt(typeVariable, equalType)
        }
    }

    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)
    private fun addSubTypeConstraintAndIncorporateIt(lowerType: CangJieTypeMarker, upperType: CangJieTypeMarker) {
        typeCheckerState.setConstrainingTypesToPrintDebugInfo(lowerType, upperType)
        typeCheckerState.runIsSubtypeOf(lowerType, upperType)

        processConstraints()
    }

    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)
    private fun addEqualityConstraintAndIncorporateIt(typeVariable: CangJieTypeMarker, equalType: CangJieTypeMarker) {
        typeCheckerState.setConstrainingTypesToPrintDebugInfo(typeVariable, equalType)
        val typeSystemContext = c as TypeSystemContext
        typeCheckerState.addEqualityConstraint(with(typeSystemContext) { typeVariable.typeConstructor() }, equalType)

        processConstraints()
    }

    context(c: Context)
    fun processGivenForkPointBranchConstraints(
        constraintSet: Collection<Pair<TypeVariableMarker, Constraint>>,
        position: IncorporationConstraintPosition,
    ) {
        with(TypeCheckerStateForConstraintInjector(c, position)) {
            processGivenConstraints(constraintSet)
            processConstraintsIgnoringForksData()
        }
    }

    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)
    private fun processConstraints() {
        processConstraintsIgnoringForksData()
        typeCheckerState.extractForkPointsData()?.let { allForkPointsData ->
            allForkPointsData.mapTo(c.constraintsFromAllForkPoints) { forkPointData ->
                typeCheckerState.position to forkPointData
            }

            c.onNewConstraintOrForkPoint()

            // During completion, we start processing fork constrains immediately
            if (c.atCompletionState) {
                c.resolveForkPointsConstraints()
            }
        }
    }

    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)
    private fun processConstraintsIgnoringForksData() {
        while (typeCheckerState.hasConstraintsToProcess()) {
            processGivenConstraints(typeCheckerState.extractAllConstraints()!!)
        }
    }

    context(c: Context, typeCheckerState: TypeCheckerStateForConstraintInjector)
    private fun processGivenConstraints(constraintsToProcess: Collection<Pair<TypeVariableMarker, Constraint>>) {
        for ((typeVariable, constraint) in constraintsToProcess) {
            if (shouldWeSkipConstraint(typeVariable, constraint)) continue

            val inferenceContext = c as TypeSystemInferenceExtensionContext
            val typeVariableConstructor = with(inferenceContext) { typeVariable.freshTypeConstructor() }
            val constraints =
                c.notFixedTypeVariables[typeVariableConstructor] ?: typeCheckerState.fixedTypeVariable(typeVariable)

            // it is important, that we add constraint here(not inside TypeCheckerContext), because inside incorporation we read constraints
            val (addedOrNonRedundantExistedConstraint, wasAdded) = constraints.addConstraint(constraint, inferenceLogger)
            val positionFrom = constraint.position.from
            val constraintToIncorporate = when {
                wasAdded -> addedOrNonRedundantExistedConstraint
                positionFrom is FixVariableConstraintPosition<*> && positionFrom.variable == typeVariable && constraint.kind == EQUALITY ->
                    addedOrNonRedundantExistedConstraint
                else -> null
            }

            if (wasAdded) {
                inferenceLogger?.log(typeVariable, addedOrNonRedundantExistedConstraint, c)
                c.onNewConstraintOrForkPoint()
                recordReferencesOfOtherTypeVariableInConstraint(constraint, typeVariableConstructor)
            }

            if (constraintToIncorporate != null) {
                constraintIncorporator.incorporate(typeVariable, constraintToIncorporate)
            }
        }
    }

    context(c: Context)
    private fun recordReferencesOfOtherTypeVariableInConstraint(
        constraint: Constraint,
        constraintOwnerTypeVariableConstructor: TypeConstructorMarker,
    ) {
        for (referencedTypeVariableConstructor in constraint.type.extractAllContainingTypeVariables()) {
            c.recordTypeVariableReferenceInConstraint(constraintOwnerTypeVariableConstructor, referencedTypeVariableConstructor)
        }
    }

    context(c: Context)
    private fun updateAllowedTypeDepth(type: CangJieTypeMarker) {
        if (!useMaxTypeDepthFromInitialConstraints) return
        c.maxTypeDepthFromInitialConstraints = max(c.maxTypeDepthFromInitialConstraints, with(c) { type.typeDepth() })
    }

    context(c: Context)
    private fun shouldWeSkipConstraint(typeVariable: TypeVariableMarker, constraint: Constraint): Boolean {
        if (constraint.kind == EQUALITY)
            return false

        val constraintType = constraint.type

        if (constraintType.typeConstructor() == typeVariable.freshTypeConstructor()) {
            return true
        }

        return false
    }

    context(c: Context)
    private fun CangJieTypeMarker.isAllowedType(): Boolean {
        return with(c) { typeDepth() } <= c.maxTypeDepthFromInitialConstraints + ALLOWED_DEPTH_DELTA_FOR_INCORPORATION
    }

    private inner class TypeCheckerStateForConstraintInjector(
        baseState: TypeCheckerState,
        val c: Context,
        val position: IncorporationConstraintPosition
    ) : TypeCheckerStateForConstraintSystem(
        c,
        baseState.cangjieTypePreparator,
        baseState.cangjieTypeRefiner,
    ), ConstraintIncorporator.Context, TypeSystemInferenceExtensionContext by c {
        constructor(c: Context, position: IncorporationConstraintPosition) : this(
            c.newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = true),
            c,
            position
        )

        // We use `var` intentionally to avoid extra allocations as this property is quite "hot"
        private var possibleNewConstraints: MutableList<Pair<TypeVariableMarker, Constraint>>? = null

        private var forkPointsData: MutableList<ForkPointData>? = null
        private var stackForConstraintsSetsFromCurrentForkPoint: Stack<MutableList<ForkPointBranchDescription>>? = null
        private var stackForConstraintSetFromCurrentForkPointBranch: Stack<MutableList<Pair<TypeVariableMarker, Constraint>>>? = null

        override val languageVersionSettings: LanguageVersionSettings
            get() = this@ConstraintInjector.languageVersionSettings

        private val allowForking: Boolean
            get() = constraintIncorporator.utilContext.isForcedAllowForkingInferenceSystem

        private var baseLowerType = position.initialConstraint.a
        private var baseUpperType = position.initialConstraint.b

        private var isIncorporatingConstraintFromDeclaredUpperBound = false
        private var isIncorporatingConstraintFromNoInfer = false
        private var currentDerivedFromSet: Set<TypeVariableMarker> = emptySet()

        fun extractAllConstraints() = possibleNewConstraints.also { possibleNewConstraints = null }
        fun extractForkPointsData() = forkPointsData.also { forkPointsData = null }

        fun addPossibleNewConstraint(variable: TypeVariableMarker, constraint: Constraint) {
            val constraintsSetsFromCurrentFork = stackForConstraintsSetsFromCurrentForkPoint?.lastOrNull()
            if (constraintsSetsFromCurrentFork != null) {
                val currentConstraintSetForForkPointBranch = stackForConstraintSetFromCurrentForkPointBranch?.lastOrNull()
                require(currentConstraintSetForForkPointBranch != null) { "Constraint has been added not under fork {...} call " }
                currentConstraintSetForForkPointBranch.add(variable to constraint)
                return
            }

            if (possibleNewConstraints == null) {
                possibleNewConstraints = SmartList()
            }
            possibleNewConstraints!!.add(variable to constraint)
        }

        override fun runForkingPoint(block: ForkPointContext.() -> Unit): Boolean {
            if (!allowForking) {
                return super.runForkingPoint(block)
            }

            if (stackForConstraintsSetsFromCurrentForkPoint == null) {
                stackForConstraintsSetsFromCurrentForkPoint = SmartList()
            }

            stackForConstraintsSetsFromCurrentForkPoint!!.add(SmartList())
            val isThereSuccessfulFork = with(MyForkCreationContext()) {
                block()
                anyForkSuccessful
            }

            val constraintSets = stackForConstraintsSetsFromCurrentForkPoint?.popLast()

            when {
                // Just an optimization
                constraintSets.isNullOrEmpty() -> return isThereSuccessfulFork
                constraintSets.size > 1 -> {
                    if (forkPointsData == null) {
                        forkPointsData = SmartList()
                    }
                    forkPointsData!!.addIfNotNull(
                        constraintSets
                    )
                    return isThereSuccessfulFork
                }
                else -> {
                    // The emptiness case has been already handled above
                    with(c) {
                        processGivenForkPointBranchConstraints(
                            constraintSets.single(),
                            position,
                        )
                    }
                }
            }

            return isThereSuccessfulFork
        }

        private inner class MyForkCreationContext : ForkPointContext {
            var anyForkSuccessful = false

            override fun fork(block: () -> Boolean) {
                if (stackForConstraintSetFromCurrentForkPointBranch == null) {
                    stackForConstraintSetFromCurrentForkPointBranch = SmartList()
                }

                stackForConstraintSetFromCurrentForkPointBranch!!.add(SmartList())

                block().also { anyForkSuccessful = anyForkSuccessful || it }

                stackForConstraintsSetsFromCurrentForkPoint!!.last()
                    .addIfNotNull(
                        stackForConstraintSetFromCurrentForkPointBranch?.popLast()?.takeIf { it.isNotEmpty() }?.toSet()
                    )
            }
        }

        fun hasConstraintsToProcess() = possibleNewConstraints != null

        fun setConstrainingTypesToPrintDebugInfo(lowerType: CangJieTypeMarker, upperType: CangJieTypeMarker) {
            baseLowerType = lowerType
            baseUpperType = upperType
        }

        fun runIsSubtypeOf(
            lowerType: CangJieTypeMarker,
            upperType: CangJieTypeMarker,
        ) {
            fun isSubtypeOf(upperType: CangJieTypeMarker) =
                AbstractTypeChecker.isSubtypeOf(
                    this@TypeCheckerStateForConstraintInjector as TypeCheckerState,
                    lowerType,
                    upperType,
                )

            if (!isSubtypeOf(upperType)) {

                c.addError(ConstraintError(lowerType, upperType, position))
            }
        }

        // from AbstractTypeCheckerContextForConstraintSystem
        override fun isMyTypeVariable(type: RigidTypeMarker): Boolean =
            c.allTypeVariables.containsKey(type.typeConstructor().unwrapStubTypeVariableConstructor())

        override fun addUpperConstraint(typeVariable: TypeConstructorMarker, superType: CangJieTypeMarker, isNoInfer: Boolean) =
            addConstraint(
                typeVariable, superType, UPPER,
                isNoInfer = isNoInfer
            )

        override fun addLowerConstraint(
            typeVariable: TypeConstructorMarker,
            subType: CangJieTypeMarker,
            isFromNullabilityConstraint: Boolean,
            isNoInfer: Boolean,
        ) = addConstraint(typeVariable, subType, LOWER, isNoInfer)

        override fun addEqualityConstraint(typeVariable: TypeConstructorMarker, type: CangJieTypeMarker) =
            addConstraint(
                typeVariable, type, EQUALITY,
                isNoInfer = false
            )

        private fun addConstraint(
            typeVariableConstructor: TypeConstructorMarker,
            type: CangJieTypeMarker,
            kind: ConstraintKind,
            isNoInfer: Boolean,
        ) {
            val typeVariable = c.allTypeVariables[typeVariableConstructor.unwrapStubTypeVariableConstructor()]
                ?: error("Should by type variableConstructor: $typeVariableConstructor. ${c.allTypeVariables.values}")

            addNewIncorporatedConstraint(
                typeVariable,
                type,
                ConstraintContext(
                    kind = kind,
                    derivedFrom = currentDerivedFromSet,
                    isNoInfer = isNoInfer
                )
            )
        }

        // from ConstraintIncorporator.Context
        override fun processNewInitialConstraintFromIncorporation(
            lowerType: CangJieTypeMarker,
            upperType: CangJieTypeMarker,
            newDerivedFrom: Set<TypeVariableMarker>,
            isFromDeclaredUpperBound: Boolean,
            isNoInfer: Boolean
        ) = with(c) {
            // Avoid checking trivial incorporated constraints
            if (lowerType == upperType) return
            if (!useMaxTypeDepthFromInitialConstraints || (lowerType.isAllowedType() && upperType.isAllowedType())) {
                withNewConfigurationForIncorporationConstraints(
                    newDerivedFromSet = newDerivedFrom,
                    isFromDeclaredUpperBound = isFromDeclaredUpperBound,
                    isNoInfer = isNoInfer,
                ) {
                    runIsSubtypeOf(lowerType, upperType, )
                }
            }
        }

        private inline fun withNewConfigurationForIncorporationConstraints(
            newDerivedFromSet: Set<TypeVariableMarker>,
            isFromDeclaredUpperBound: Boolean,
            isNoInfer: Boolean,
            b: () -> Unit,
        ) {
            // No immediate recursive incorporation should happen, so `currentDerivedFromSet` would be reset at "finally"
            check(currentDerivedFromSet.isEmpty())

            try {
                currentDerivedFromSet = newDerivedFromSet
                isIncorporatingConstraintFromDeclaredUpperBound = isFromDeclaredUpperBound
                isIncorporatingConstraintFromNoInfer = isNoInfer
                b()
            } finally {
                // NB: `emptySet()` returns a singleton, so no excessive memory here
                currentDerivedFromSet = emptySet()
                isIncorporatingConstraintFromDeclaredUpperBound = false
                isIncorporatingConstraintFromNoInfer = false
            }
        }

        override fun addNewIncorporatedConstraint(
            typeVariable: TypeVariableMarker,
            type: CangJieTypeMarker,
            constraintContext: ConstraintContext
        ) {
            val (kind, derivedFrom, inputTypePosition, isNoInfer) = constraintContext

            var targetType = type
            if (targetType.isUninferredParameter()) {
                // there already should be an error, so there is no point in reporting one more
                return
            }

            if (targetType.isError()) {
                c.addError(ConstrainingTypeIsError(typeVariable, targetType, position))
                return
            }

            val position = if (isIncorporatingConstraintFromDeclaredUpperBound) position.copy(isFromDeclaredUpperBound = true) else position

            val newConstraint = Constraint(
                kind, targetType, position,
                derivedFrom = derivedFrom,
                isNoInfer = isNoInfer || isIncorporatingConstraintFromNoInfer,
                inputTypePositionBeforeIncorporation = inputTypePosition,
            )

            addPossibleNewConstraint(typeVariable, newConstraint)
        }

        override val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>
            get() = c.notFixedTypeVariables.values

        override fun getVariablesWithConstraintsContainingGivenTypeVariable(
            variableConstructorMarker: TypeConstructorMarker
        ): Collection<VariableWithConstraints> =
            c.typeVariableDependencies[variableConstructorMarker]?.mapNotNull { c.notFixedTypeVariables[it] }
                ?: emptyList()

        override fun getTypeVariable(typeConstructor: TypeConstructorMarker): TypeVariableMarker? {
            val typeVariable = c.allTypeVariables[typeConstructor]
            if (typeVariable != null && !c.notFixedTypeVariables.containsKey(typeConstructor)) {
                fixedTypeVariable(typeVariable)
            }
            return typeVariable
        }

        override fun getConstraintsForVariable(typeVariable: TypeVariableMarker) =
            c.notFixedTypeVariables[typeVariable.freshTypeConstructor()]?.constraints
                ?: fixedTypeVariable(typeVariable)

        fun fixedTypeVariable(variable: TypeVariableMarker): Nothing {
            error(
                "Type variable $variable should not be fixed!\n" +
                        renderBaseConstraint()
            )
        }

        private fun renderBaseConstraint() = "Base constraint: $baseLowerType <: $baseUpperType from position: $position"
    }
}

data class ConstraintContext(
    val kind: ConstraintKind,
    val derivedFrom: Set<TypeVariableMarker>,
    val inputTypePositionBeforeIncorporation: OnlyInputTypeConstraintPosition? = null,
    val isNoInfer: Boolean,
)

private typealias Stack<E> = MutableList<E>
