

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirResolveState
import org.cangnova.cangjie.cfir.declarations.ResolveStateAccess
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.elementWithResolveState]
 */
abstract class CfirElementWithResolveState : CfirPureAbstractElement(), CfirElement {
    abstract override val source: CjSourceElement?
    abstract val moduleData: CfirModuleData
    @kotlin.concurrent.Volatile
    @ResolveStateAccess
    lateinit var resolveState: CfirResolveState

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitElementWithResolveState(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformElementWithResolveState(this, data) as E
}
