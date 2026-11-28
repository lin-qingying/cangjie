

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.errorTypeRef]
 */
abstract class CfirErrorTypeRef : CfirResolvedTypeRef(), CfirDiagnosticHolder {
    abstract override val source: CjSourceElement?
    abstract override val annotations: List<CfirAnnotation>
    abstract override val customRenderer: Boolean
    abstract override val typeUseOrigin: CfirTypeRefOrigin
    abstract override val coneType: ConeCangJieType
    abstract override val delegatedTypeRef: CfirTypeRef?
    abstract override val diagnostic: ConeDiagnostic
    abstract val partiallyResolvedTypeRef: CfirTypeRef?

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        delegatedTypeRef?.accept(visitor, data)
        partiallyResolvedTypeRef?.accept(visitor, data)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitErrorTypeRef(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformErrorTypeRef(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceTypeUseOrigin(newTypeUseOrigin: CfirTypeRefOrigin)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirErrorTypeRef

    abstract fun <D> transformPartiallyResolvedTypeRef(transformer: CfirTransformer<D>, data: D): CfirErrorTypeRef
}
