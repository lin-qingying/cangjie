

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.typePattern]
 */
abstract class CfirTypePattern : CfirPattern() {
    abstract override val source: CjSourceElement?
    abstract override val semanticTypeRef: CfirTypeRef?
    abstract override val constantValues: List<CfirConstantValue>
    abstract override val contextExpression: CfirExpression?
    abstract val matchingKind: CfirTypePatternMatchingKind
    abstract val typeRef: CfirTypeRef
    abstract val nestedPattern: CfirPattern
    abstract val matchBeforeRuntime: Boolean
    abstract val needRuntimeTypeCheck: Boolean

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        semanticTypeRef?.accept(visitor, data)
        typeRef.accept(visitor, data)
        nestedPattern.accept(visitor, data)
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitTypePattern(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformTypePattern(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract override fun replaceSemanticTypeRef(newSemanticTypeRef: CfirTypeRef?)

    abstract override fun replaceConstantValues(newConstantValues: List<CfirConstantValue>)

    abstract override fun replaceContextExpression(newContextExpression: CfirExpression?)

    abstract fun replaceMatchingKind(newMatchingKind: CfirTypePatternMatchingKind)

    abstract fun replaceTypeRef(newTypeRef: CfirTypeRef)

    abstract fun replaceNestedPattern(newNestedPattern: CfirPattern)

    abstract fun replaceMatchBeforeRuntime(newMatchBeforeRuntime: Boolean)

    abstract fun replaceNeedRuntimeTypeCheck(newNeedRuntimeTypeCheck: Boolean)

    abstract override fun <D> transformSemanticTypeRef(transformer: CfirTransformer<D>, data: D): CfirTypePattern

    abstract fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirTypePattern

    abstract fun <D> transformNestedPattern(transformer: CfirTransformer<D>, data: D): CfirTypePattern
}
