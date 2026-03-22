package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.constraints.CfirConstraint
import org.cangnova.cangjie.cfir.constraints.CfirConstraintIssue
import org.cangnova.cangjie.cfir.constraints.CfirConstraintPosition
import org.cangnova.cangjie.cfir.constraints.CfirConstraintResult
import org.cangnova.cangjie.cfir.constraints.CfirConstraintSystem
import org.cangnova.cangjie.cfir.constraints.CfirTypeSubstitutor
import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceLogger
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType

class CfirConstraintSystemImpl(
    private val typeRelations: CfirTypeRelations,
    private val variableManager: CfirVariableManager = CfirVariableManager(),
    private val completion: CfirConstraintCompletion = CfirConstraintCompletion(typeRelations),
    private val resultBuilder: CfirConstraintResultBuilder = CfirConstraintResultBuilder(),
    private val inferenceLogger: CfirInferenceLogger? = null,
) : CfirConstraintSystem {

    val store = CfirConstraintStore()
    private val unifier = CfirUnifier(typeRelations, store)

    override val errors: List<CfirConstraintIssue>
        get() = store.issues

    fun nextFreshTypeId(): Int = variableManager.nextFreshTypeId()

    override fun registerTypeVariable(typeVariable: CfirTypeVariable) {
        store.registerTypeVariable(typeVariable)
        inferenceLogger?.logNewVariable(typeVariable, this)
    }

    override fun addConstraint(constraint: CfirConstraint) {
        store.addConstraint(constraint)
        inferenceLogger?.logConstraint(constraint, this)
        when (constraint) {
            is CfirConstraint.Equality -> {
                unifier.unify(constraint.left, constraint.right, constraint.position)
                unifier.unify(constraint.right, constraint.left, constraint.position)
            }

            is CfirConstraint.Subtype -> {
                unifier.unify(constraint.lower, constraint.upper, constraint.position)
            }

            is CfirConstraint.Compatibility -> {
                propagateCompatibilityConstraint(constraint.source, constraint.target, constraint.position)
            }

            is CfirConstraint.ExpectedType -> {
                propagateExpectedTypeConstraint(constraint.actual, constraint.expected, constraint.position)
            }

            is CfirConstraint.UpperBound -> {
                constraint.variable.upperBounds += constraint.bound
            }
        }
    }

    override fun addSubtypeConstraint(
        lower: ConeCangJieType,
        upper: ConeCangJieType,
        position: CfirConstraintPosition,
    ) {
        addConstraint(CfirConstraint.Subtype(lower, upper, position))
    }

    override fun addEqualityConstraint(
        left: ConeCangJieType,
        right: ConeCangJieType,
        position: CfirConstraintPosition,
    ) {
        addConstraint(CfirConstraint.Equality(left, right, position))
    }

    override fun addSubtypeConstraintIfCompatible(
        lower: ConeCangJieType,
        upper: ConeCangJieType,
        position: CfirConstraintPosition,
    ): Boolean {
        // 如果类型参数对应已注册的类型变量，直接当作可约束处理
        if (isRegisteredTypeVariable(lower) || isRegisteredTypeVariable(upper)) {
            addSubtypeConstraint(lower, upper, position)
            return true
        }

        val relation = typeRelations.compare(lower, upper)
        return when (relation) {
            CfirTypeRelationResult.INCOMPATIBLE -> {
                store.reportIssue(
                    CfirConstraintIssue.IncompatibleTypes(
                        message = "Incompatible subtype constraint: $lower <: $upper",
                        position = position,
                    ),
                )
                false
            }

            else -> {
                addSubtypeConstraint(lower, upper, position)
                true
            }
        }
    }

    /** 检查类型是否对应约束系统中已注册的类型变量 */
    private fun isRegisteredTypeVariable(type: ConeCangJieType): Boolean {
        if (type !is ConeTypeParameterType) return false
        return store.findTypeVariable(type.lookupTag.name) != null
    }

    override fun fixAllVariables() {
        completion.complete(store)
        store.typeVariables.forEach { variable ->
            variable.fixedType?.let { inferenceLogger?.logFixVariable(variable, it, this) }
        }
    }

    override fun buildResult(): CfirConstraintResult {
        return resultBuilder.build(store)
    }

    override fun buildResultingSubstitutor(): CfirTypeSubstitutor {
        return buildResult().substitutor
    }

    private fun propagateCompatibilityConstraint(
        source: ConeCangJieType,
        target: ConeCangJieType,
        position: CfirConstraintPosition,
    ) {
        when (typeRelations.compare(source, target)) {
            CfirTypeRelationResult.IDENTICAL,
            CfirTypeRelationResult.SUBTYPE,
            CfirTypeRelationResult.COMPATIBLE_WITH_CONVERSION,
            CfirTypeRelationResult.COMPATIBLE_WITH_QUEST_FALLBACK,
            CfirTypeRelationResult.CONSTRAINABLE_BY_VARIABLES,
            -> unifier.unify(source, target, position)

            CfirTypeRelationResult.INCOMPATIBLE -> {
                val issue = CfirConstraintIssue.IncompatibleTypes(
                    message = "Incompatible compatibility constraint: $source ~> $target",
                    position = position,
                )
                store.reportIssue(issue)
                inferenceLogger?.logError(issue, this)
            }
        }
    }

    private fun propagateExpectedTypeConstraint(
        actual: ConeCangJieType,
        expected: ConeCangJieType,
        position: CfirConstraintPosition,
    ) {
        when (typeRelations.compare(actual, expected)) {
            CfirTypeRelationResult.IDENTICAL,
            CfirTypeRelationResult.SUBTYPE,
            CfirTypeRelationResult.CONSTRAINABLE_BY_VARIABLES,
            -> unifier.unify(actual, expected, position)

            CfirTypeRelationResult.COMPATIBLE_WITH_CONVERSION,
            CfirTypeRelationResult.COMPATIBLE_WITH_QUEST_FALLBACK,
            -> store.addConstraint(CfirConstraint.Compatibility(actual, expected, position))

            CfirTypeRelationResult.INCOMPATIBLE -> {
                val issue = CfirConstraintIssue.IncompatibleTypes(
                    message = "Expected type mismatch: actual=$actual, expected=$expected",
                    position = position,
                )
                store.reportIssue(issue)
                inferenceLogger?.logError(issue, this)
            }
        }
    }
}
