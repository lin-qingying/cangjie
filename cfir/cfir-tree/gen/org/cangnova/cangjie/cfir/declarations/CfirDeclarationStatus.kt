

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangjie.cfir.declarations

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility

/**
 * Generated from: [org.cangjie.cfir.tree.generator.CfirTree.declarationStatus]
 */
interface CfirDeclarationStatus : CfirElement {
    override val source: CfirSourceElement?
    val visibility: Visibility
    val modality: Modality?
    val isOverride: Boolean
    val isOperator: Boolean
    val isStatic: Boolean
    val isConst: Boolean
    val isMut: Boolean
    val isUnsafe: Boolean
    val isForeign: Boolean
    val isCommon: Boolean
    val isSpecific: Boolean
    val isRedef: Boolean
    val isAbstract: Boolean
    val isOpen: Boolean
    val isSealed: Boolean

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitDeclarationStatus(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformDeclarationStatus(this, data) as E
}
