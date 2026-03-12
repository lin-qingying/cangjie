/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.expressions.impl

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

class CfirSubscriptExpressionImpl @CfirImplementationDetail constructor(
    override val coneTypeOrNull: ConeCangjieType?,
    override val receiver: CfirExpression,
    override val indices: List<CfirExpression>,
) : CfirSubscriptExpression() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        receiver.accept(visitor, data)
        indices.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirSubscriptExpressionImpl {
        receiver.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        indices.forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }
        return this
    }
}
