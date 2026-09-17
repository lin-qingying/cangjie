/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.psi

import com.intellij.lang.ASTNode
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * 注解修饰的表达式，主要承载官方 annotation-lambda 语法。
 *
 * 注解表达式不能伪装成声明注解或普通 macro；容器保留 annotation 与真正
 * 的 base expression 的所有权，raw CFIR 再把注解语义附着到对应匿名函数。
 */
class CjAnnotatedExpression(node: ASTNode) : CjExpressionImpl(node), CjAnnotated {
    /** 表达式上的注解容器。 */
    override val annotations: CjAnnotations?
        get() = findChildByType(CjStubElementTypes.ANNOTATIONS)

    /** 表达式上的完整注解条目。 */
    override val annotationEntries: List<CjAnnotation>
        get() = annotations?.entries ?: emptyList()

    /** 被注解修饰的基础表达式。 */
    val baseExpression: CjExpression?
        get() = children.filterIsInstance<CjExpression>().firstOrNull()

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? =
        visitor.visitAnnotatedExpression(this, data)
}
