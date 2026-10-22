

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.patterns.CfirCatchPattern
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeOwner
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.catchClause]
 */
abstract class CfirCatch : CfirExpression(), CfirPatternTreeOwner {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val coneTypeOrNull: ConeCangJieType?
    /**
     * 异常模式子树具有唯一 owner；浅拷贝必须由调用方显式提供独立模式树。
     */
    abstract val pattern: CfirCatchPattern
    abstract val body: CfirBlock

    abstract override fun patternTreeRoot(): org.cangnova.cangjie.cfir.CfirElement
    abstract override fun validatePatternTreeRoot(newPatternTreeRoot: org.cangnova.cangjie.cfir.CfirElement)
    abstract override fun replacePatternTreeRoot(newPatternTreeRoot: org.cangnova.cangjie.cfir.CfirElement)

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitCatch(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformCatch(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?)

    abstract fun replacePattern(newPattern: CfirCatchPattern)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirCatch

    abstract fun <D> transformPattern(transformer: CfirTransformer<D>, data: D): CfirCatch

    abstract fun <D> transformBody(transformer: CfirTransformer<D>, data: D): CfirCatch
}
