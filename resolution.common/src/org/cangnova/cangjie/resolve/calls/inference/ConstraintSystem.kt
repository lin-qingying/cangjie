/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeInfo
import org.cangnova.cangjie.type.model.*

/**
 * 类型推断约束系统的只读入口。
 */
interface ConstraintSystem {
    /**
     * 当前约束系统是否已经存在矛盾。
     */
    val hasContradiction: Boolean
    /**
     * 当前约束系统收集到的错误。
     */
    val errors: List<ConstraintSystemError>

    /**
     * 返回可继续写入约束的 builder。
     */
    fun getBuilder(): ConstraintSystemBuilder

    // after this method we shouldn't mutate system via ConstraintSystemBuilder
    /**
     * 将当前系统冻结为只读约束存储。
     */
    fun asReadOnlyStorage(): ConstraintStorage

    /**
     * 返回约束系统完成阶段使用的上下文视图。
     */
    fun asConstraintSystemCompleterContext(): ConstraintSystemCompletionContext
    /**
     * 返回延迟参数分析阶段使用的上下文视图。
     */
    fun asPostponedArgumentsAnalyzerContext(): PostponedArgumentsAnalyzerContext
    /**
     * 解析 fork point 中积累的分支约束。
     */
    fun resolveForkPointsConstraints()

    /**
     * 判断一组类型形成空交叉类型时的具体原因。
     */
    fun getEmptyIntersectionTypeKind(types: Collection<CangJieTypeMarker>): EmptyIntersectionTypeInfo?
}

/**
 * In some cases we're not only adding constraints linearly to the system, but sometimes we need to consider several variants of constraints
 *
 * For example, from smartcast we've got a value of a type A<Int, String> & A<E, F> that we'd like to pass as an argument to the parameter
 * of type A<Xv, Yv> (where Xv and Yv are the type variables of the current call)
 *
 * So, we've got a subtyping constraint
 * A<Int, String> & A<E, F> <: A<Xv, Yv>
 *
 * And we might go with the first intersection component, having the following variables constraint set: {Xv=Int,Yv=String}
 * Or, if we'd consider the second component it would be {Xv=E, Yv=F}
 *
 * And all existing and future constraints might work differently depending on which option we've chosen.
 * Thus, ideally we need to create two versions of the constraint system and try to resolve each of them.
 * But that lead to exponential complexity, so we only use some set of heuristics for that
 *
 * Lately, we call such situation a "fork point" and each of the options a "fork point branch"
 * Each branch is defined by the set of constraints that need to be added to the system if we choose the particular branch.
 */
/**
 * fork point 的所有分支描述。
 */
typealias ForkPointData = List<ForkPointBranchDescription>
/**
 * 单个 fork point 分支需要追加的类型变量约束集合。
 */
typealias ForkPointBranchDescription = Set<Pair<TypeVariableMarker, Constraint>>
