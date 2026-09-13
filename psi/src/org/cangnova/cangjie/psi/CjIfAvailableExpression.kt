/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.cangnova.cangjie.psi

import com.intellij.lang.ASTNode

/**
 * 官方 `IfAvailableExpr` 的 PSI 表示。
 *
 * 该节点与普通 annotation、macro expression 分离，保证两个 lambda 分支
 * 在 source/raw CFIR lowering 时保留为真正的表达式树，而不是 token surface。
 */
class CjIfAvailableExpression(node: ASTNode) : CjExpressionImpl(node) {
    /** 条件实参，例如 `level: 19` 或 `syscap: "camera"`。 */
    val conditionArgument: CjValueArgument?
        get() = findChildByClass(CjValueArgument::class.java)

    /** 条件参数的名字。 */
    val conditionName: String?
        get() = conditionArgument?.getArgumentName()?.asName?.asString()

    /** 条件参数值。 */
    val condition: CjExpression?
        get() = conditionArgument?.getArgumentExpression()

    /** 两个分支的直接表达式；合法输入是 lambda，非法输入仍保留原始表达式。 */
    val branchExpressions: List<CjExpression>
        get() = children.filterIsInstance<CjExpression>()

    /** 可用分支。 */
    val thenBranch: CjLambdaExpression?
        get() = branchExpressions.getOrNull(0) as? CjLambdaExpression

    /** 不可用分支。 */
    val elseBranch: CjLambdaExpression?
        get() = branchExpressions.getOrNull(1) as? CjLambdaExpression

    /** 访问特殊表达式节点。 */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? =
        visitor.visitIfAvailableExpression(this, data)
}
