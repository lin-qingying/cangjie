

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

class CfirLazyBlockImpl : CfirLazyBlock() {
    override val source: CjSourceElement?
        get() = error("CfirLazyBlock should be resolved before accessing")
    override val annotations: List<CfirAnnotation>
        get() = error("CfirLazyBlock should be resolved before accessing")
    override val coneTypeOrNull: ConeCangJieType?
        get() = error("CfirLazyBlock should be resolved before accessing")
    override val statements: List<CfirElement>
        get() = error("CfirLazyBlock should be resolved before accessing")

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)
     {
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)
     {
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirLazyBlock
     {
        return this
    }

    override fun <D> transformStatements(transformer: CfirTransformer<D>, data: D): CfirLazyBlock
     {
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirLazyBlockImpl {
        return this
    }
}
