

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.rootElement]
 */
interface CfirElement {
    val source: CjSourceElement?

    /**
     * Runs the provided [visitor] on the CFIR subtree with the root at this node.
     *
     * @param visitor The visitor to accept.
     * @param data An arbitrary context to pass to each invocation of [visitor]'s methods.
     * @return The value returned by the topmost `visit*` invocation.
     */
    fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitElement(this, data)

    /**
     * Runs the provided [transformer] on the CFIR subtree with the root at this node.
     *
     * @param transformer The transformer to use.
     * @param data An arbitrary context to pass to each invocation of [transformer]'s methods.
     * @return The transformed node.
     */
    @Suppress("UNCHECKED_CAST")
    fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformElement(this, data) as E

    /**
     * Runs the provided [visitor] on the CFIR subtree with the root at this node.
     *
     * @param visitor The visitor to accept.
     */
    fun accept(visitor: CfirVisitorVoid) {
        accept(visitor, null)
    }

    /**
     * Runs the provided [visitor] on subtrees with roots in this node's children.
     *
     * Basically, calls `accept(visitor, data)` on each child of this node.
     *
     * Does **not** run [visitor] on this node itself.
     *
     * @param visitor The visitor for children to accept.
     * @param data An arbitrary context to pass to each invocation of [visitor]'s methods.
     */
    fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D)
    /**
     * Runs the provided [visitor] on subtrees with roots in this node's children.
     *
     * Basically, calls `accept(visitor)` on each child of this node.
     *
     * Does **not** run [visitor] on this node itself.
     *
     * @param visitor The visitor for children to accept.
     */
    fun acceptChildren(visitor: CfirVisitorVoid) {
        acceptChildren(visitor, null)
    }


    /**
     * Recursively transforms this node's children *in place* using [transformer].
     *
     * Basically, executes `this.child = this.child.transform(transformer, data)` for each child of this node.
     *
     * Does **not** run [transformer] on this node itself.
     *
     * @param transformer The transformer to use for transforming the children.
     * @param data An arbitrary context to pass to each invocation of [transformer]'s methods.
     * @return `this`
     */
    fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement
}
