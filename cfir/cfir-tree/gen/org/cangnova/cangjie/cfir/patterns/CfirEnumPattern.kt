

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.enumPattern]
 */
abstract class CfirEnumPattern : CfirPattern() {
    abstract override val source: CjSourceElement?
    abstract val constructorReference: CfirReference
    abstract val arguments: List<CfirPattern>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitEnumPattern(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformEnumPattern(this, data) as E

    abstract fun <D> transformConstructorReference(transformer: CfirTransformer<D>, data: D): CfirEnumPattern


    abstract fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirEnumPattern

}
