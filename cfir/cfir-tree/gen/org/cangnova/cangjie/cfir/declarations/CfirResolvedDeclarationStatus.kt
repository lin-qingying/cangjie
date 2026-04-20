

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.resolvedDeclarationStatus]
 */
interface CfirResolvedDeclarationStatus : CfirDeclarationStatus {
    override val source: CjSourceElement?
    override val visibility: Visibility
    override val isVisibilityExplicit: Boolean
    override val isModalityExplicit: Boolean
    override val isOverride: Boolean
    override val isOperator: Boolean
    override val isStatic: Boolean
    override val isConst: Boolean
    override val isMut: Boolean
    override val isUnsafe: Boolean
    override val isForeign: Boolean
    override val isCommon: Boolean
    override val isSpecific: Boolean
    override val isRedef: Boolean
    override val isAbstract: Boolean
    override val isOpen: Boolean
    override val isSealed: Boolean
    override val modality: Modality

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitResolvedDeclarationStatus(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformResolvedDeclarationStatus(this, data) as E
}
