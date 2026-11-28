

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirControlFlowGraphOwner
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.functionBodyOwner]
 */
interface CfirFunctionBodyOwner : CfirTargetElement, CfirControlFlowGraphOwner {
    override val source: CjSourceElement?
    override val controlFlowGraphReference: CfirControlFlowGraphReference?
    val contentSource: CfirContentSource
    val symbol: CfirCallableSymbol<*>
    val returnTypeRef: CfirTypeRef
    val valueParameters: List<CfirValueParameter>
    val body: CfirBlock?

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        controlFlowGraphReference?.accept(visitor, data)
        returnTypeRef.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitFunctionBodyOwner(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformFunctionBodyOwner(this, data) as E

    @CfirImplementationDetail
    override fun replaceSource(newSource: CjSourceElement?)

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: CfirControlFlowGraphReference?)

    fun replaceReturnTypeRef(newReturnTypeRef: CfirTypeRef)

    fun replaceValueParameters(newValueParameters: List<CfirValueParameter>)

    fun replaceBody(newBody: CfirBlock?)

    fun <D> transformReturnTypeRef(transformer: CfirTransformer<D>, data: D): CfirFunctionBodyOwner

    fun <D> transformValueParameters(transformer: CfirTransformer<D>, data: D): CfirFunctionBodyOwner

    fun <D> transformBody(transformer: CfirTransformer<D>, data: D): CfirFunctionBodyOwner
}
