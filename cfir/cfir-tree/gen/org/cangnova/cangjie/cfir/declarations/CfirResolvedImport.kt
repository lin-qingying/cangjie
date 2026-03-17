

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.resolvedImportDirective]
 */
abstract class CfirResolvedImport : CfirImport() {
    abstract override val source: CjSourceElement?
    abstract override val importedFqName: FqName?
    abstract override val isAllUnder: Boolean
    abstract override val aliasName: Name?
    abstract override val aliasSource: CjSourceElement?
    abstract val delegate: CfirImport
    abstract val packageFqName: FqName
    abstract val importedName: Name?

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitResolvedImport(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformResolvedImport(this, data) as E
}
