

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.cfir.CfirPureAbstractElement
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.source.CjSourceElement

internal class CfirResolvedDeclarationStatusImpl(
    override val source: CjSourceElement?,
    override val visibility: Visibility,
    override val isVisibilityExplicit: Boolean,
    override val isModalityExplicit: Boolean,
    override val isAbstractExplicit: Boolean,
    override val isOverride: Boolean,
    override val isOperator: Boolean,
    override val isStatic: Boolean,
    override val isConst: Boolean,
    override val isMut: Boolean,
    override val isUnsafe: Boolean,
    override val isForeign: Boolean,
    override val isCommon: Boolean,
    override val isSpecific: Boolean,
    override val isRedef: Boolean,
    override val isDefault: Boolean,
    override val isAbstract: Boolean,
    override val isOpen: Boolean,
    override val isSealed: Boolean,
    override val modality: Modality,
) : CfirPureAbstractElement(), CfirResolvedDeclarationStatus {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirResolvedDeclarationStatusImpl {
        return this
    }
}
