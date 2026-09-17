/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.psi.stubs.CangJieFeaturesDirectiveStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * 仓颉官方 `FeaturesDirective` 的 PSI 表示。
 *
 * `features` 是文件前导的一部分，不是普通声明；其注解和 feature id
 * 必须由同一个节点持有，避免 `@NonProduct` 被错误挂到后续声明。
 */
class CjFeaturesDirective : CjElementImplStub<CangJieFeaturesDirectiveStub>, CjAnnotated {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieFeaturesDirectiveStub) : super(stub, CjStubElementTypes.FEATURES_DIRECTIVE)
    /** 该 directive 的注解容器。 */
    override val annotations: CjAnnotations?
        get() = getStubOrPsiChild(CjStubElementTypes.ANNOTATIONS)

    /** 该 directive 上的完整注解条目。 */
    override val annotationEntries: List<CjAnnotation>
        get() = annotations?.entries ?: emptyList()

    /** feature id 集合。 */
    val features: CjFeaturesSet?
        get() = findChildByType(CjNodeTypes.FEATURES_SET)

    /** 该文件声明的 feature id。 */
    val featureIdElements: List<CjFeatureId>
        get() = features?.featureIds ?: emptyList()

    /** 按源码顺序返回 feature id；compiled PSI 从 stub 恢复文本。 */
    val featureIds: List<String>
        get() = stub?.featureIds ?: featureIdElements.map(CjFeatureId::qualifiedName)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? =
        visitor.visitFeaturesDirective(this, data)
}

/** `features { id, qualified.id }` 的 feature 集合。 */
class CjFeaturesSet(node: ASTNode) : CjElementImpl(node) {
    /** 按源码顺序返回 feature id。 */
    val featureIds: List<CjFeatureId>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, CjFeatureId::class.java)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? =
        visitor.visitFeaturesSet(this, data)
}

/** 一个由点分隔的 feature id。 */
class CjFeatureId(node: ASTNode) : CjElementImpl(node) {
    /** 规范化后的 feature id 文本；保留源码段顺序，不引入符号解析。 */
    val qualifiedName: String
        get() = text.filterNot(Char::isWhitespace)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? =
        visitor.visitFeatureId(this, data)
}
