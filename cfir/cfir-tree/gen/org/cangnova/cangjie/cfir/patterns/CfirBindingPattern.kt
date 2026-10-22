

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.bindingPattern]
 */
abstract class CfirBindingPattern : CfirPattern() {
    abstract override val source: CjSourceElement?
    /**
     * 该 pattern 节点所属的唯一模式树宿主；反向 owner 不属于 CFIR 子树。
     */
    abstract override val patternTreeOwner: CfirPatternTreeOwner?
    abstract val name: Name
    abstract val typeRef: CfirTypeRef?
    abstract val bindingVariable: CfirPatternBindingVariable?
    abstract val nestedPattern: CfirPattern?

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitBindingPattern(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformBindingPattern(this, data) as E

    abstract override fun replacePatternTreeOwner(newPatternTreeOwner: CfirPatternTreeOwner?)

    abstract fun replaceTypeRef(newTypeRef: CfirTypeRef?)

    abstract fun replaceBindingVariable(newBindingVariable: CfirPatternBindingVariable?)

    abstract fun replaceNestedPattern(newNestedPattern: CfirPattern?)

    abstract fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirBindingPattern

    abstract fun <D> transformBindingVariable(transformer: CfirTransformer<D>, data: D): CfirBindingPattern

    abstract fun <D> transformNestedPattern(transformer: CfirTransformer<D>, data: D): CfirBindingPattern
}
