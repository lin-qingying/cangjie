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
import org.cangjie.cfir.expressions.CfirBlock
import org.cangjie.cfir.expressions.CfirCatch
import org.cangjie.cfir.expressions.CfirTryExpression
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

class CfirTryExpressionImpl @CfirImplementationDetail constructor(
    override val coneTypeOrNull: ConeCangjieType?,
    override val tryBlock: CfirBlock,
    override val catches: List<CfirCatch>,
    override val finallyBlock: CfirBlock?,
) : CfirTryExpression() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        tryBlock.accept(visitor, data)
        catches.forEach { it.accept(visitor, data) }
        finallyBlock?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTryExpressionImpl {
        tryBlock.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        catches.forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }
        finallyBlock?.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        return this
    }
}
