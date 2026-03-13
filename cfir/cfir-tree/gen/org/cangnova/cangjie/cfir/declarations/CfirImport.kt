

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangjie.cfir.declarations

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.CfirPureAbstractElement
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Generated from: [org.cangjie.cfir.tree.generator.CfirTree.importDirective]
 */
abstract class CfirImport : CfirPureAbstractElement(), CfirElement {
    abstract override val source: CfirSourceElement?
    abstract val importedFqName: FqName
    abstract val isAllUnder: Boolean
    abstract val aliasName: Name?

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitImport(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformImport(this, data) as E
}
