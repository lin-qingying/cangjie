

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.references

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirPureAbstractElement
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.namedReferenceWithCandidateBase]
 */
abstract class CfirNamedReferenceWithCandidateBase : CfirPureAbstractElement(), CfirNamedReference {
    abstract override val source: CjSourceElement?
    abstract override val name: Name
    abstract val candidateSymbol: CfirBasedSymbol<*>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitNamedReferenceWithCandidateBase(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformNamedReferenceWithCandidateBase(this, data) as E
}
