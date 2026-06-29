/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.inference.model.VariableWithConstraints
import org.cangnova.cangjie.resolve.calls.model.CollectionLiteralAtomMarker
import org.cangnova.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.utils.SmartSet

/**
 * 类型变量依赖信息提供器。
 *
 * 该组件从未固定变量约束、postponed 参数、顶层期望类型和外层约束系统变量中构建依赖图，
 * 供变量固定策略判断某个类型变量是否依赖输出类型、集合字面量、顶层类型或外层变量。
 */
class TypeVariableDependencyInformationProvider(
    /**
     * 当前约束系统中尚未固定的类型变量及其约束。
     */
    private val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>,

    /**
     * 尚未完成分析的 postponed 调用原子。
     */
    private val postponedKtPrimitives: List<PostponedResolvedAtomMarker>,

    /**
     * 局部完成模式下的顶层类型；不存在顶层类型关联判断时为 `null`。
     */
    private val topLevelType: CangJieTypeMarker?,

    /**
     * 读取类型结构和外层变量信息的推断上下文。
     */
    private val typeSystemContext: VariableFixationFinder.Context,

    /**
     * 当前语言版本设置，保留给依赖规则的版本化分支使用。
     */
    private val languageVersionSettings: LanguageVersionSettings,
) {

    /**
     * 外层约束系统中的类型变量构造器集合。
     */
    private val outerTypeVariables: Set<TypeConstructorMarker>? =
        typeSystemContext.outerTypeVariables

    /*
     * Not oriented edges
     * TypeVariable(A) has UPPER(Function1<TypeVariable(B), R>) => A and B are related deeply
     */
    /**
     * 深依赖边集合。
     *
     * 当约束类型的任意嵌套位置引用另一个未固定变量时，两个变量之间建立无向深依赖边。
     */
    private val deepTypeVariableDependencies: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> = hashMapOf()

    /*
     * Not oriented edges
     * TypeVariable(A) has UPPER(TypeVariable(B)) => A and B are related shallowly
     */
    /**
     * 浅依赖边集合。
     *
     * 当约束类型的顶层构造器就是另一个未固定变量时，两个变量之间建立无向浅依赖边。
     */
    private val shallowTypeVariableDependencies: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> = hashMapOf()

    // Oriented edges
    /**
     * postponed 参数输入类型到输出类型变量的有向依赖边。
     */
    private val postponeArgumentsEdges: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> = hashMapOf()

    /**
     * 与任意 postponed 输出类型相关的类型变量集合。
     */
    private val relatedToAllOutputTypes: MutableSet<TypeConstructorMarker> = hashSetOf()

    /**
     * 与顶层类型相关的类型变量集合。
     */
    private val relatedToTopLevelType: MutableSet<TypeConstructorMarker> = hashSetOf()

    /**
     * 与集合字面量期望类型相关的类型变量集合。
     */
    private val relatedToCollectionLiteral: MutableSet<TypeConstructorMarker> = hashSetOf()

    /**
     * 与外层约束系统变量相关的类型变量集合。
     */
    private var relatedToOuterTypeVariables: MutableSet<TypeConstructorMarker>? = null

    init {
        computeConstraintEdges()
        computePostponeArgumentsEdges()
        computeRelatedToAllOutputTypes()
        computeRelatedToTopLevelType()
        computeRelatedToCollectionLiteral()
        computeRelatedToTopOuterTypeVariables()
    }

    /**
     * 判断 [variable] 是否与顶层类型相关。
     */
    fun isVariableRelatedToTopLevelType(variable: TypeConstructorMarker) =
        relatedToTopLevelType.contains(variable)

    /**
     * 判断 [variable] 是否与集合字面量期望类型相关。
     */
    fun isRelatedToCollectionLiteral(variable: TypeConstructorMarker) =
        relatedToCollectionLiteral.contains(variable)

    /**
     * 判断 [variable] 是否与外层约束系统变量相关。
     */
    fun isRelatedToOuterTypeVariable(variable: TypeConstructorMarker): Boolean =
        relatedToOuterTypeVariables?.contains(variable) == true

    // This one shall be removed together with LV 2.0.
    // The problem with this definition is that it doesn't consider Xv ~ Yv related if one of them is used inside an input type of
    // postponed atom and another is used as an output type.
    /**
     * 旧版外层变量相关性判断。
     *
     * 该算法只查看深依赖边，不考虑 postponed 输入到输出的有向边，保留用于兼容旧语义。
     */
    private fun oldIsRelatedToOuterTypeVariable(variable: TypeConstructorMarker): Boolean {
        val outerTypeVariables = outerTypeVariables ?: return false
        val myDependent = getDeeplyDependentVariables(variable) ?: return false
        return myDependent.any { it in outerTypeVariables }
    }

    /**
     * 判断 [variable] 是否与任意 postponed 输出类型相关。
     */
    fun isVariableRelatedToAnyOutputType(variable: TypeConstructorMarker) = relatedToAllOutputTypes.contains(variable)

    /**
     * 返回与 [variable] 存在深依赖关系的变量集合。
     */
    fun getDeeplyDependentVariables(variable: TypeConstructorMarker) = deepTypeVariableDependencies[variable]

    /**
     * 返回与 [variable] 存在浅依赖关系的变量集合。
     */
    fun getShallowlyDependentVariables(variable: TypeConstructorMarker) = shallowTypeVariableDependencies[variable]

    /**
     * 判断两个变量是否存在浅依赖关系，或是否共同出现在同一浅依赖连通组中。
     */
    fun areVariablesDependentShallowly(a: TypeConstructorMarker, b: TypeConstructorMarker): Boolean {
        if (a == b) return true

        val shallowDependencies = shallowTypeVariableDependencies[a] ?: return false

        return shallowDependencies.any { it == b } ||
                shallowTypeVariableDependencies.values.any { dependencies -> a in dependencies && b in dependencies }
    }

    /**
     * 根据未固定变量的约束构建深依赖边和浅依赖边。
     */
    private fun computeConstraintEdges() {
        fun addConstraintEdgeForDeepDependency(from: TypeConstructorMarker, to: TypeConstructorMarker) {
            deepTypeVariableDependencies.getOrPut(from) { linkedSetOf() }.add(to)
            deepTypeVariableDependencies.getOrPut(to) { linkedSetOf() }.add(from)
        }

        fun addConstraintEdgeForShallowDependency(from: TypeConstructorMarker, to: TypeConstructorMarker) {
            shallowTypeVariableDependencies.getOrPut(from) { linkedSetOf() }.add(to)
            shallowTypeVariableDependencies.getOrPut(to) { linkedSetOf() }.add(from)
        }

        for (variableWithConstraints in notFixedTypeVariables.values) {
            val from = with(typeSystemContext) { variableWithConstraints.typeVariable.freshTypeConstructor() }

            for (constraint in variableWithConstraints.constraints) {
                val constraintTypeConstructor = with(typeSystemContext) { constraint.type.typeConstructor() }

                constraint.type.forAllMyTypeVariables {
                    if (isMyTypeVariable(it)) {
                        addConstraintEdgeForDeepDependency(from, it)
                    }
                }
                if (isMyTypeVariable(constraintTypeConstructor)) {
                    addConstraintEdgeForShallowDependency(from, constraintTypeConstructor)
                }
            }
        }
    }

    /**
     * 根据 postponed 原子的输入类型和输出类型构建有向依赖边。
     */
    private fun computePostponeArgumentsEdges() {
        fun addPostponeArgumentsEdges(from: TypeConstructorMarker, to: TypeConstructorMarker) {
            postponeArgumentsEdges.getOrPut(from) { hashSetOf() }.add(to)
        }

        for (argument in postponedKtPrimitives) {
            if (argument.analyzed) continue

            val typeVariablesInOutputType = SmartSet.create<TypeConstructorMarker>()
            (argument.outputType ?: continue).forAllMyTypeVariables { typeVariablesInOutputType.add(it) }
            if (typeVariablesInOutputType.isEmpty()) continue

            for (inputType in argument.inputTypes) {
                inputType.forAllMyTypeVariables { from ->
                    for (to in typeVariablesInOutputType) {
                        addPostponeArgumentsEdges(from, to)
                    }
                }
            }
        }
    }

    /**
     * 计算与所有 postponed 输出类型相关的变量闭包。
     */
    private fun computeRelatedToAllOutputTypes() {
        for (argument in postponedKtPrimitives) {
            if (argument.analyzed) continue
            (argument.outputType ?: continue).forAllMyTypeVariables {
                addAllRelatedNodes(relatedToAllOutputTypes, it, includePostponedEdges = false)
            }
        }
    }

    /**
     * 计算与顶层类型相关的变量闭包。
     */
    private fun computeRelatedToTopLevelType() {
        if (topLevelType == null) return
        topLevelType.forAllMyTypeVariables {
            addAllRelatedNodes(relatedToTopLevelType, it, includePostponedEdges = true)
        }
    }

    /**
     * 计算与集合字面量期望类型相关的变量闭包。
     */
    private fun computeRelatedToCollectionLiteral() {
        for (argument in postponedKtPrimitives) {
            if (argument.analyzed || argument !is CollectionLiteralAtomMarker) continue
            val expectedType = argument.expectedType ?: continue
            val expectedTypeConstructor = with(typeSystemContext) { expectedType.typeConstructor() }
            if (isMyTypeVariable(expectedTypeConstructor)) {
                addAllRelatedNodes(relatedToCollectionLiteral, expectedTypeConstructor, includePostponedEdges = true)
            }
        }
    }

    /**
     * 计算与外层约束系统变量相关的变量闭包。
     */
    private fun computeRelatedToTopOuterTypeVariables() {
        val outerTypeVariables = outerTypeVariables ?: return
        relatedToOuterTypeVariables = mutableSetOf()
        for (outerTypeVariable in outerTypeVariables) {
            addAllRelatedNodes(relatedToOuterTypeVariables!!, outerTypeVariable, includePostponedEdges = true)
        }
    }

    /**
     * 判断 [typeConstructor] 是否属于当前系统的未固定类型变量。
     */
    private fun isMyTypeVariable(typeConstructor: TypeConstructorMarker) = notFixedTypeVariables.containsKey(typeConstructor)

    /**
     * 遍历当前类型中所有属于本约束系统的未固定类型变量。
     */
    private fun CangJieTypeMarker.forAllMyTypeVariables(action: (TypeConstructorMarker) -> Unit) =
        with(typeSystemContext) {
            contains {
                val typeConstructor = it.typeConstructor()
                if (isMyTypeVariable(typeConstructor)) action(typeConstructor)
                false
            }
        }


    /**
     * 获取 [from] 的深依赖邻接集合。
     */
    private fun getConstraintEdges(from: TypeConstructorMarker): Set<TypeConstructorMarker> = deepTypeVariableDependencies[from] ?: emptySet()

    /**
     * 获取 [from] 的 postponed 有向依赖邻接集合。
     */
    private fun getPostponeEdges(from: TypeConstructorMarker): Set<TypeConstructorMarker> = postponeArgumentsEdges[from] ?: emptySet()

    /**
     * 将 [node] 以及其可达依赖节点递归加入 [to]。
     */
    private fun addAllRelatedNodes(to: MutableSet<TypeConstructorMarker>, node: TypeConstructorMarker, includePostponedEdges: Boolean) {
        if (to.add(node)) {
            for (relatedNode in getConstraintEdges(node)) {
                addAllRelatedNodes(to, relatedNode, includePostponedEdges)
            }
            if (includePostponedEdges) {
                for (relatedNode in getPostponeEdges(node)) {
                    addAllRelatedNodes(to, relatedNode, includePostponedEdges)
                }
            }
        }
    }


}
