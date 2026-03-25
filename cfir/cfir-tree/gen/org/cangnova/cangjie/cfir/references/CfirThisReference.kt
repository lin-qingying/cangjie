

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.references

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirPureAbstractElement
import org.cangnova.cangjie.cfir.symbols.CfirThisOwnerSymbol
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.thisReference]
 */
abstract class CfirThisReference : CfirPureAbstractElement(), CfirReference {
    abstract override val source: CjSourceElement?
    abstract val boundSymbol: CfirThisOwnerSymbol<*>?
    abstract val isImplicit: Boolean
    abstract val diagnostic: ConeDiagnostic?

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitThisReference(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformThisReference(this, data) as E

    abstract fun replaceBoundSymbol(newBoundSymbol: CfirThisOwnerSymbol<*>?)


    abstract fun replaceDiagnostic(newDiagnostic: ConeDiagnostic?)

}
