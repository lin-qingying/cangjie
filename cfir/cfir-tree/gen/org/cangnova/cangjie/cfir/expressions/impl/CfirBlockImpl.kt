

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirBlockImpl @CfirImplementationDetail constructor(
    override var coneTypeOrNull: ConeCangjieType?,
    override var statements: List<CfirElement>,
) : CfirBlock() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        statements.forEach { it.accept(visitor, data) }
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangjieType?)
     {
        this.coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformStatements(transformer: CfirTransformer<D>, data: D): CfirBlock
     {
        this.statements = statements.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirElement }
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirBlockImpl {
        transformStatements(transformer, data)
        return this
    }
}
