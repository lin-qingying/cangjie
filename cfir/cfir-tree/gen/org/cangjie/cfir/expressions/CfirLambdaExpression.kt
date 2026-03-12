/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangjie.cfir.expressions

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.declarations.CfirFunction
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangjie.cfir.tree.generator.CfirTree.lambdaExpression]
 */
abstract class CfirLambdaExpression : CfirExpression() {
    abstract override val source: CfirSourceElement?
    abstract override val coneTypeOrNull: ConeCangjieType?
    abstract val anonymousFunction: CfirFunction

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitLambdaExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformLambdaExpression(this, data) as E
}
