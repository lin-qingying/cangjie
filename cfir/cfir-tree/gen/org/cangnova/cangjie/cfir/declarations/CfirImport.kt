

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirPureAbstractElement
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.importDirective]
 */
abstract class CfirImport : CfirPureAbstractElement(), CfirElement {
    abstract override val source: CjSourceElement?
    abstract val importedFqName: FqName?
    abstract val organizationName: Name?
    abstract val isAllUnder: Boolean
    abstract val aliasName: Name?
    abstract val aliasSource: CjSourceElement?
    abstract val condition: CfirExpression?

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitImport(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformImport(this, data) as E

    abstract fun replaceCondition(newCondition: CfirExpression?)

    abstract fun <D> transformCondition(transformer: CfirTransformer<D>, data: D): CfirImport
}
