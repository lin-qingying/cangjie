package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.DeclaredUpperBoundConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.ExplicitTypeParameterConstraintPosition
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.contains

/**
 * 仓颉局部类型实参求解的有向依赖图，对位官方 TyVarConstraintGraph::TopoOnce。
 * 上下界中出现的待解变量先于当前变量求解；先选零入度组，没有零入度时才从入度为一
 * 的结点寻找递归组。没有可选组表示依赖尚不能求解，不能只凭局部 proper 下界强行固定。
 */
internal class CangjieTypeVariableDependencyGraph(
    private val context: VariableFixationFinder.Context,
    private val variables: List<TypeConstructorMarker>,
) {
    /** 返回当前允许进入固定阶段的一组变量；空列表表示该依赖图尚无可解组。 */
    fun nextGroup(): List<TypeConstructorMarker> = with(context) {
        // 显式类型实参是用户已给定的绑定，不属于官方局部推断的待求解集合。
        // 只认该变量自己的初始显式等式，不能把其它显式实参派生的等式混为直接绑定。
        val explicitBindings = initialConstraints.asSequence()
            .filter { it.position is ExplicitTypeParameterConstraintPosition<*> && it.constraintKind == ConstraintKind.EQUALITY }
            .map { it.a.typeConstructor() }
            .toSet()
        val explicitVariables = variables.filter { it in explicitBindings }
        if (explicitVariables.isNotEmpty()) return explicitVariables

        val variableSet = variables.toSet()
        val outgoing = variables.associateWith { linkedSetOf<TypeConstructorMarker>() }
        val indegree = variables.associateWith { 0 }.toMutableMap()
        val substitutor = typeSubstitutorByTypeConstructor(fixedTypeVariables)
        fun addDependency(dependency: TypeConstructorMarker, variable: TypeConstructorMarker) {
            if (dependency in variableSet && outgoing.getValue(dependency).add(variable)) {
                indegree[variable] = indegree.getValue(variable) + 1
            }
        }
        fun addDependencies(variable: TypeConstructorMarker, type: CangJieTypeMarker) {
            substitutor.safeSubstitute(type).contains { nestedType ->
                addDependency(nestedType.typeConstructor(), variable)
                false
            }
        }

        val declaredBounds = linkedMapOf<TypeConstructorMarker, MutableList<CangJieTypeMarker>>()
        for (initial in initialConstraints) {
            if (initial.position !is DeclaredUpperBoundConstraintPosition<*>) continue
            if (initial.constraintKind != ConstraintKind.UPPER) continue
            val variable = initial.a.typeConstructor()
            if (variable in variableSet) declaredBounds.getOrPut(variable) { mutableListOf() }.add(initial.b)
        }

        for (variable in variables) {
            // ExposeGenericUpperBounds只传递名义/结构上界，保留原有参数边；不会把X <: Y
            // 复制成Y的下界或把所有传递参数边加入图。FIR的incorporation存储包含这些派生边。
            val pending = ArrayDeque<TypeConstructorMarker>()
            val visited = mutableSetOf<TypeConstructorMarker>()
            pending.add(variable)
            while (pending.isNotEmpty()) {
                val current = pending.removeFirst()
                if (!visited.add(current)) continue
                for (bound in declaredBounds[current].orEmpty()) {
                    val expandedBound = substitutor.safeSubstitute(bound)
                    val dependency = expandedBound.typeConstructor()
                    if (dependency in variableSet) {
                        if (current == variable && dependency != variable) addDependency(dependency, variable)
                        pending.add(dependency)
                    } else {
                        addDependencies(variable, expandedBound)
                    }
                }
            }

            for (constraint in notFixedTypeVariables.getValue(variable).constraints) {
                if (!constraint.isLocalToInferenceScope) continue
                val initial = constraint.position.initialConstraint
                if (initial.position is DeclaredUpperBoundConstraintPosition<*> &&
                    initial.a.typeConstructor() in allTypeVariables
                ) continue
                addDependencies(variable, constraint.type)
            }
        }

        val independent = variables.filter { indegree.getValue(it) == 0 }
        if (independent.isNotEmpty()) return independent

        // 与官方 FindLoopConstraints / HasLoop 一致，递归组保留从可进入结点到环的路径。
        fun findLoop(start: TypeConstructorMarker): List<TypeConstructorMarker>? {
            val path = mutableListOf<TypeConstructorMarker>()
            val visiting = mutableSetOf<TypeConstructorMarker>()
            fun visit(variable: TypeConstructorMarker): Boolean {
                if (!visiting.add(variable)) return true
                path += variable
                for (next in outgoing.getValue(variable)) {
                    if (visit(next)) return true
                }
                path.removeAt(path.lastIndex)
                visiting.remove(variable)
                return false
            }
            return if (visit(start)) path.toList() else null
        }

        for (variable in variables) {
            if (indegree.getValue(variable) != 1) continue
            findLoop(variable)?.let { return it }
        }
        emptyList()
    }
}
